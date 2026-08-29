package com.cr.tunnel.root

import android.content.Context
import android.os.Process
import com.cr.tunnel.AppConfig
import com.cr.tunnel.handler.MmkvManager
import com.cr.tunnel.handler.SettingsManager
import com.cr.tunnel.root.RootProxyManager.TABLE
import com.cr.tunnel.root.RootProxyManager.TUN
import com.cr.tunnel.root.RootProxyManager.teardown
import com.cr.tunnel.util.LogUtil
import com.cr.tunnel.util.PackageUidResolver
import com.cr.tunnel.util.Utils
import java.io.File

/**
 * Installs and removes the iptables / ip-rule routing that pushes system-wide traffic
 * into a tun device (root mode, and LAN/tethering sharing for VPN mode).
 *
 * A bundled hev-socks5-tunnel binary (run as root) creates the tun and forwards it to
 * the in-process core's SOCKS inbound; a mangle MARK chain plus a dedicated routing
 * table and ip rule steer traffic into the tun. Rules live in dedicated chains/tables
 * so teardown is a bounded flush, and teardown runs before every setup and on every
 * stop path so no stale rule can break connectivity.
 */
object RootProxyManager {

    private const val CHAIN = AppConfig.ROOT_IPTABLES_CHAIN
    private const val TUN = AppConfig.ROOT_TUN_NAME
    private const val TABLE = AppConfig.ROOT_ROUTE_TABLE
    private const val PRIORITY = AppConfig.ROOT_RULE_PRIORITY
    private const val FWMARK = AppConfig.ROOT_FWMARK
    private const val MARK = AppConfig.ROOT_MARK_ROUTE

    // Local / private / multicast destinations that must never be proxied.
    private val bypassCidrs = listOf(
        "0.0.0.0/8", "10.0.0.0/8", "127.0.0.0/8", "169.254.0.0/16",
        "172.16.0.0/12", "192.168.0.0/16", "224.0.0.0/4", "240.0.0.0/4"
    )

    // IPv6 equivalents (loopback, link-local, ULA/private, multicast).
    // ip6tables needs its own list; the v4 list fails there silently.
    private val bypassCidrsV6 = listOf(
        "::1/128", "fe80::/10", "fc00::/7", "ff00::/8"
    )

    fun start(context: Context): Boolean {
        teardown(context)
        val script = buildTun2socksSetup(context) ?: return false
        val result = RootShell.runScript(context, "setup_rules.sh", script)
        if (!result.success) {
            LogUtil.e(AppConfig.TAG, "RootProxyManager: setup failed, rolling back:\n${result.output}")
            teardown(context)
            return false
        }
        return true
    }

    /**
     * LAN/tethering sharing while the device itself stays on VPN mode: a client tun2socks
     * forwards tethered clients into the core's SOCKS inbound without touching the
     * device's own traffic. Requires root.
     */
    fun startClientSharing(context: Context): Boolean {
        teardown(context)
        val script = buildTun2socksSetup(context, captureDeviceTraffic = false, forceLanShare = true)
            ?: return false
        val result = RootShell.runScript(context, "setup_rules.sh", script)
        if (!result.success) {
            LogUtil.e(AppConfig.TAG, "RootProxyManager: client sharing setup failed:\n${result.output}")
            teardown(context)
            return false
        }
        LogUtil.i(AppConfig.TAG, "RootProxyManager: LAN client sharing installed")
        return true
    }

    /** Remove all rules and stop helper processes. Safe to call repeatedly. */
    fun stop(context: Context) {
        teardown(context)
        LogUtil.i(AppConfig.TAG, "RootProxyManager: rules removed")
    }

    private fun teardown(context: Context) {
        RootShell.runScript(context, "teardown_rules.sh", buildTeardown(context))
    }

    // --------------------------------------------------------------- TUN2SOCKS

    /**
     * @param captureDeviceTraffic Root mode: capture the device's own OUTPUT traffic.
     *   VPN-mode sharing: only forwarded clients are routed into the tun.
     * @param forceLanShare Force the LAN forward rules regardless of the preference.
     */
    private fun buildTun2socksSetup(
        context: Context,
        captureDeviceTraffic: Boolean = true,
        forceLanShare: Boolean = false,
    ): String? {
        val bin = File(context.applicationInfo.nativeLibraryDir, AppConfig.ROOT_TUN2SOCKS_BIN)
        if (!bin.exists()) {
            LogUtil.e(AppConfig.TAG, "RootProxyManager: hev-socks5-tunnel binary missing at ${bin.absolutePath}")
            return null
        }
        val appUid = context.applicationInfo.uid
        val socksUsername = SettingsManager.getSocksUsername()
        val socksPassword = SettingsManager.getSocksPassword()
        val port = SettingsManager.getSocksPort()
        val runDir = File(context.filesDir, AppConfig.ROOT_RUNTIME_DIR).apply { mkdirs() }
        val pidFile = File(runDir, "tun2socks.pid").absolutePath
        val logFile = File(runDir, "tun2socks.log").absolutePath
        val cfgFile = File(runDir, "tun2socks.yml").absolutePath
        val oomGuardPid = File(runDir, "oomguard.pid").absolutePath
        val ipv6 = MmkvManager.decodeSettingsBool(AppConfig.PREF_IPV6_ENABLED)
        val lanShare = forceLanShare || MmkvManager.decodeSettingsBool(AppConfig.PREF_ROOT_LAN_SHARING)
        val corePid = Process.myPid()

        // Per-app proxy/bypass selection. Proxy and Direct sets are per-app modes:
        // proxy-mode default captures the Proxy apps, bypass-mode default keeps the
        // Direct apps on their own network and captures everything else.
        val perAppEnabled = MmkvManager.decodeSettingsBool(AppConfig.PREF_PER_APP_PROXY)
        val bypassApps = MmkvManager.decodeSettingsBool(AppConfig.PREF_BYPASS_APPS)
        val selectedUids = if (perAppEnabled) {
            val pkgs = if (bypassApps) {
                MmkvManager.decodeSettingsStringSet(AppConfig.PREF_PER_APP_PROXY_DIRECT)
            } else {
                MmkvManager.decodeSettingsStringSet(AppConfig.PREF_PER_APP_PROXY_SET)
            }?.toList().orEmpty()
            if (pkgs.isNotEmpty()) PackageUidResolver.packageNamesToUids(context, pkgs) else emptyList()
        } else {
            emptyList()
        }

        return buildString {
            appendLine("set -e")
            appendLine("BIN='${bin.absolutePath}'")
            // Keep the core process alive: system_server keeps recomputing oom_score_adj,
            // so re-pin it from a small root loop instead of a one-shot write.
            appendLine("nohup sh -c 'while true; do echo ${AppConfig.ROOT_OOM_SCORE} > /proc/$corePid/oom_score_adj 2>/dev/null; sleep 5; done' >/dev/null 2>&1 &")
            appendLine("echo \$! > '$oomGuardPid'")
            // tun device node
            appendLine("if [ ! -e /dev/net/tun ]; then mkdir -p /dev/net; mknod /dev/net/tun c 10 200; chmod 666 /dev/net/tun; fi")
            // hev creates the tun ($TUN) itself and forwards it to the in-process core's SOCKS
            // inbound on loopback. MTU comes from the existing VPN MTU setting.
            appendLine("cat > '$cfgFile' <<'HEVCFG'")
            append(buildHevConfig(socksUsername = socksUsername, socksPassword = socksPassword, socksPort = port, ipv6 = ipv6))
            appendLine("HEVCFG")
            appendLine("nohup \"\$BIN\" '$cfgFile' >'$logFile' 2>&1 &")
            appendLine("T2S_PID=\$!")
            appendLine("echo \$T2S_PID > '$pidFile'")
            appendLine("echo ${AppConfig.ROOT_OOM_SCORE} > /proc/\$T2S_PID/oom_score_adj 2>/dev/null || true")
            // wait for the interface hev creates to appear
            appendLine("i=0; while [ \$i -lt 20 ]; do ip link show $TUN >/dev/null 2>&1 && break; sleep 0.3; i=\$((i+1)); done")
            appendLine("ip link show $TUN >/dev/null 2>&1 || { echo 'tun device did not come up'; cat '$logFile' 2>/dev/null; exit 1; }")
            // relax reverse-path filtering for the tun
            appendLine("echo 0 > /proc/sys/net/ipv4/conf/$TUN/rp_filter 2>/dev/null || true")
            appendLine("echo 0 > /proc/sys/net/ipv4/conf/all/rp_filter 2>/dev/null || true")
            // address + default route in a dedicated table
            appendLine("ip addr add ${AppConfig.ROOT_TUN_ADDR_V4} dev $TUN 2>/dev/null || true")
            appendLine("ip link set dev $TUN up")
            appendLine("ip route replace default dev $TUN table $TABLE")
            appendLine("ip rule add fwmark $MARK table $TABLE priority $PRIORITY")
            // mark the device's own packets into the tun (Root mode only)
            if (captureDeviceTraffic) {
                append(buildMangleMarking("iptables", appUid, perAppEnabled, bypassApps, selectedUids))
            }
            // optionally route hotspot / USB-tethered clients through the tun too
            if (lanShare) {
                append(buildLanShareSetup(captureDeviceTraffic, ipv6))
            }
            if (captureDeviceTraffic) {
                // IPv6 is best-effort: never fail the (working) IPv4 setup over it.
                appendLine("set +e")
                if (ipv6) {
                    // route the device's v6 into the tun, same as v4
                    appendLine("ip -6 addr add ${AppConfig.ROOT_TUN_ADDR_V6} dev $TUN 2>/dev/null || true")
                    appendLine("ip -6 route replace default dev $TUN table $TABLE 2>/dev/null || true")
                    appendLine("ip -6 rule add fwmark $MARK table $TABLE priority $PRIORITY 2>/dev/null || true")
                    append(buildMangleMarking("ip6tables", appUid, perAppEnabled, bypassApps, selectedUids))
                } else {
                    // v6 disabled: reject the captured apps' native v6 so it falls back to
                    // v4-through-proxy, matching a v4-only VpnService.
                    append(buildV6Blackhole(appUid, perAppEnabled, bypassApps, selectedUids))
                }
            }
        }
    }

    /**
     * hev-socks5-tunnel YAML config. hev creates the tun named [TUN], assigns it the
     * addresses, and forwards everything it receives to the core's SOCKS inbound on
     * loopback (TCP + UDP). MTU comes from the existing VPN MTU setting.
     */
    private fun buildHevConfig(socksUsername: String?, socksPassword: String?, socksPort: Int, ipv6: Boolean): String {
        val v4 = AppConfig.ROOT_TUN_ADDR_V4.substringBefore("/")
        val v6 = AppConfig.ROOT_TUN_ADDR_V6.substringBefore("/")
        return buildString {
            appendLine("tunnel:")
            appendLine("  name: '$TUN'")
            appendLine("  mtu: ${SettingsManager.getVpnMtu()}")
            appendLine("  multi-queue: true")
            appendLine("  ipv4: '$v4'")
            if (ipv6) appendLine("  ipv6: '$v6'")
            appendLine("socks5:")
            appendLine("  port: $socksPort")
            appendLine("  address: '${AppConfig.LOOPBACK}'")
            appendLine("  udp: 'udp'")
            if (socksUsername != null && socksPassword != null) {
                appendLine("  username: '$socksUsername'")
                appendLine("  password: '$socksPassword'")
            }
            appendLine("  tcp-fastopen: true")
        }
    }

    /**
     * mangle OUTPUT marking chain (ipv4/ipv6), mirroring VpnService capture behavior:
     * all-apps mode marks every remaining uid; bypass mode keeps the selected apps
     * direct and captures everything else; proxy mode captures only the selected apps.
     */
    private fun buildMangleMarking(
        cmd: String,
        appUid: Int,
        perAppEnabled: Boolean,
        bypassApps: Boolean,
        selectedUids: List<String>,
    ): String {
        val allowMode = perAppEnabled && !bypassApps
        val bypassSelected = perAppEnabled && bypassApps && selectedUids.isNotEmpty()
        return buildString {
            appendLine("$cmd -t mangle -N $CHAIN 2>/dev/null || true")
            appendLine("$cmd -t mangle -F $CHAIN")
            // The core's own outbound and the helper's loopback traffic must not loop into the tun.
            appendLine("$cmd -t mangle -A $CHAIN -m mark --mark $FWMARK -j RETURN")
            appendLine("$cmd -t mangle -A $CHAIN -m owner --uid-owner $appUid -j RETURN")
            // bypass mode: selected apps go fully direct (incl their DNS)
            if (bypassSelected) {
                selectedUids.forEach { appendLine("$cmd -t mangle -A $CHAIN -m owner --uid-owner $it -j RETURN") }
            }
            // Route DNS through the core for all modes, without a uid filter: on Android the
            // query is sent by netd (a shared uid), not under the app's uid. Also runs
            // before the LAN-bypass RETURNs so a LAN/router-resolver query is not
            // resolved direct (DNS leak / CDN mis-resolution). The MARK survives RETURN.
            appendLine("$cmd -t mangle -A $CHAIN -p udp --dport 53 -j MARK --set-xmark $MARK")
            appendLine("$cmd -t mangle -A $CHAIN -p tcp --dport 53 -j MARK --set-xmark $MARK")
            // keep LAN / private destinations direct (per-family CIDR list)
            val cidrs = if (cmd == "ip6tables") bypassCidrsV6 else bypassCidrs
            cidrs.forEach { appendLine("$cmd -t mangle -A $CHAIN -d $it -j RETURN") }
            if (allowMode) {
                // Proxy only the explicitly selected apps. If none resolved (early-boot uid lookup
                // failure), mark nothing instead of falling through to the catch-all below,
                // which would tunnel every unselected app.
                selectedUids.forEach { appendLine("$cmd -t mangle -A $CHAIN -m owner --uid-owner $it -j MARK --set-xmark $MARK") }
            } else {
                // all-apps / bypass mode: capture every remaining uid (incl 0 + system uids)
                appendLine("$cmd -t mangle -A $CHAIN -j MARK --set-xmark $MARK")
            }
            appendLine("$cmd -t mangle -D OUTPUT -j $CHAIN 2>/dev/null || true")
            appendLine("$cmd -t mangle -A OUTPUT -j $CHAIN")
        }
    }

    /**
     * Reject native IPv6 egress for the captured apps when IPv6 is not routed into the
     * tun. A v4-only VpnService has no v6 route, so the kernel rejects apps' v6 and they
     * fall back to IPv4; root mode must reproduce that explicitly or v6-capable apps
     * reach destinations natively, bypassing the proxy. REJECT (not DROP) fails fast so
     * happy-eyeballs falls back to v4 without a timeout.
     *
     * Exemptions mirror the v4 chain (helper fwmark, app core uid, loopback,
     * link-local/multicast, ULA/LAN) and per-app selection is honored.
     */
    private fun buildV6Blackhole(
        appUid: Int,
        perAppEnabled: Boolean,
        bypassApps: Boolean,
        selectedUids: List<String>,
    ): String {
        val chain = AppConfig.ROOT_V6_CHAIN
        val allowMode = perAppEnabled && !bypassApps
        val bypassSelected = perAppEnabled && bypassApps && selectedUids.isNotEmpty()
        val reject = "-j REJECT --reject-with icmp6-adm-prohibited"
        return buildString {
            appendLine("ip6tables -t filter -N $chain 2>/dev/null || true")
            appendLine("ip6tables -t filter -F $chain")
            // never touch the helper, the core, loopback, NDP/link-local/multicast or LAN
            appendLine("ip6tables -t filter -A $chain -m mark --mark $FWMARK -j RETURN")
            appendLine("ip6tables -t filter -A $chain -m owner --uid-owner $appUid -j RETURN")
            appendLine("ip6tables -t filter -A $chain -o lo -j RETURN")
            bypassCidrsV6.forEach { appendLine("ip6tables -t filter -A $chain -d $it -j RETURN") }
            // bypass mode: bypassed apps keep their native v6
            if (bypassSelected) {
                selectedUids.forEach { appendLine("ip6tables -t filter -A $chain -m owner --uid-owner $it -j RETURN") }
            }
            if (allowMode) {
                // proxy mode: only the selected apps lose v6 (so they fall back to v4-via-proxy).
                // None resolved -> reject nothing, mirroring the v4 chain's fail-closed handling.
                selectedUids.forEach { appendLine("ip6tables -t filter -A $chain -m owner --uid-owner $it $reject") }
            } else {
                // all-apps / bypass: reject everyone left
                appendLine("ip6tables -t filter -A $chain $reject")
            }
            appendLine("ip6tables -t filter -D OUTPUT -j $chain 2>/dev/null || true")
            appendLine("ip6tables -t filter -A OUTPUT -j $chain")
        }
    }

    // -------------------------------------------------- LAN / tethering sharing

    /**
     * Route Wi-Fi-hotspot / USB-tethered clients through the tun (ipv4). Best-effort:
     * wrapped in `set +e` so a failure here never breaks the working proxy. Mirrors
     * Magic_V2Ray's hotspot rules (FORWARD accept, DNS DNAT, source-based policy
     * routing for private client ranges, MSS clamp).
     */
    private fun buildLanShareSetup(captureDeviceTraffic: Boolean, ipv6: Boolean): String {
        val fwd = AppConfig.ROOT_FWD_CHAIN
        val dnsChain = AppConfig.ROOT_DNS_CHAIN
        val v6fwd = AppConfig.ROOT_V6_FWD_CHAIN
        val v6pre = AppConfig.ROOT_V6_PRE_CHAIN
        // DNAT target for tethered clients: the configured remote DNS when it is a
        // plain IPv4 address, else the fallback resolver.
        val dns = SettingsManager.getRemoteDnsServers()
            .firstOrNull { Utils.isPureIpAddress(it) && !it.contains(":") }
            ?: AppConfig.ROOT_LAN_DNS
        val lanCidrs = listOf("10.0.0.0/8", "172.16.0.0/12", "192.168.0.0/16")
        return buildString {
            appendLine("set +e")
            appendLine("echo 1 > /proc/sys/net/ipv4/ip_forward 2>/dev/null || true")
            // forward traffic to/from the tun
            appendLine("iptables -N $fwd 2>/dev/null || true")
            appendLine("iptables -F $fwd")
            appendLine("iptables -A $fwd -i $TUN -j ACCEPT")
            appendLine("iptables -A $fwd -o $TUN -j ACCEPT")
            appendLine("iptables -D FORWARD -j $fwd 2>/dev/null || true")
            appendLine("iptables -I FORWARD -j $fwd")
            // clamp MSS to avoid TLS fragmentation overhead through the tunnel
            appendLine("iptables -t mangle -D FORWARD -o $TUN -p tcp --tcp-flags SYN,RST SYN -j TCPMSS --set-mss 1350 2>/dev/null || true")
            appendLine("iptables -t mangle -A FORWARD -o $TUN -p tcp --tcp-flags SYN,RST SYN -j TCPMSS --set-mss 1350")
            // Hijack tethered clients' DNS into a dedicated chain (teardown stays
            // independent of the resolver IP).
            appendLine("iptables -t nat -N $dnsChain 2>/dev/null || true")
            appendLine("iptables -t nat -F $dnsChain")
            lanCidrs.forEach {
                appendLine("iptables -t nat -A $dnsChain ! -i $TUN -d $it -p udp --dport 53 -j DNAT --to $dns")
            }
            appendLine("iptables -t nat -D PREROUTING -j $dnsChain 2>/dev/null || true")
            appendLine("iptables -t nat -A PREROUTING -j $dnsChain")
            // policy routing: return-path via main, LAN direct, the rest via the tun table
            appendLine("ip rule add iif lo goto 6000 pref 5000 2>/dev/null || true")
            appendLine("ip rule add iif $TUN lookup main suppress_prefixlength 0 pref 5010 2>/dev/null || true")
            appendLine("ip rule add iif $TUN goto 6000 pref 5020 2>/dev/null || true")
            appendLine("ip rule add to 10.0.0.0/8 lookup main pref 5025 2>/dev/null || true")
            appendLine("ip rule add to 172.16.0.0/12 lookup main pref 5026 2>/dev/null || true")
            appendLine("ip rule add to 192.168.0.0/16 lookup main pref 5027 2>/dev/null || true")
            appendLine("ip rule add from 10.0.0.0/8 lookup $TABLE pref 5030 2>/dev/null || true")
            appendLine("ip rule add from 172.16.0.0/12 lookup $TABLE pref 5040 2>/dev/null || true")
            appendLine("ip rule add from 192.168.0.0/16 lookup $TABLE pref 5050 2>/dev/null || true")
            appendLine("ip rule add nop pref 6000 2>/dev/null || true")

            // Tethered clients get a native (RA-assigned) global IPv6 that the IPv4 rules
            // don't touch, so it would egress upstream directly, bypassing the proxy.
            // Route it through the tun when v6 is enabled, reject it when it isn't.
            appendLine("ip6tables -N $v6fwd 2>/dev/null || true")
            appendLine("ip6tables -F $v6fwd")
            appendLine("ip6tables -D FORWARD -j $v6fwd 2>/dev/null || true")
            appendLine("ip6tables -I FORWARD -j $v6fwd")
            if (ipv6) {
                // VPN-mode sharing: the tun table has no v6 default and the tun has no v6
                // address while the device itself isn't capturing v6 — add them so marked
                // client v6 has somewhere to go.
                if (!captureDeviceTraffic) {
                    appendLine("ip -6 addr add ${AppConfig.ROOT_TUN_ADDR_V6} dev $TUN 2>/dev/null || true")
                    appendLine("ip -6 route replace default dev $TUN table $TABLE 2>/dev/null || true")
                    appendLine("ip -6 rule add fwmark $MARK table $TABLE priority $PRIORITY 2>/dev/null || true")
                }
                // allow forwarding to/from the tun
                appendLine("ip6tables -A $v6fwd -i $TUN -j ACCEPT")
                appendLine("ip6tables -A $v6fwd -o $TUN -j ACCEPT")
                // Mark forwarded (non-locally-sourced) client v6 into the tun table. DNS
                // first so a query to a LAN/router resolver is still tunneled (the MARK
                // survives RETURN); loopback, link-local (NDP/RA) and ULA/multicast stay direct.
                appendLine("ip6tables -t mangle -N $v6pre 2>/dev/null || true")
                appendLine("ip6tables -t mangle -F $v6pre")
                appendLine("ip6tables -t mangle -A $v6pre ! -i $TUN -p udp --dport 53 -j MARK --set-xmark $MARK")
                appendLine("ip6tables -t mangle -A $v6pre ! -i $TUN -p tcp --dport 53 -j MARK --set-xmark $MARK")
                bypassCidrsV6.forEach { appendLine("ip6tables -t mangle -A $v6pre ! -i $TUN -d $it -j RETURN") }
                appendLine("ip6tables -t mangle -A $v6pre ! -i $TUN -j MARK --set-xmark $MARK")
                appendLine("ip6tables -t mangle -D PREROUTING -j $v6pre 2>/dev/null || true")
                appendLine("ip6tables -t mangle -A PREROUTING -j $v6pre")
                // Fail closed: forwarded v6 that was not marked into the tun is rejected
                // rather than leaked out the upstream interface.
                appendLine("ip6tables -A $v6fwd -j REJECT --reject-with icmp6-no-route")
            } else {
                // v6 disabled: reject forwarded clients' native v6 so it can't leak past
                // the proxy (the device's own v6 is handled in OUTPUT).
                appendLine("ip6tables -A $v6fwd -j REJECT --reject-with icmp6-no-route")
            }
        }
    }

    // ---------------------------------------------------------------- teardown

    private fun buildTeardown(context: Context): String {
        val runDir = File(context.filesDir, AppConfig.ROOT_RUNTIME_DIR)
        val pidFile = File(runDir, "tun2socks.pid").absolutePath
        val oomGuardPid = File(runDir, "oomguard.pid").absolutePath
        val corePid = Process.myPid()
        return buildString {
            // mangle (TUN2SOCKS), both families
            for (cmd in listOf("iptables", "ip6tables")) {
                appendLine("$cmd -t mangle -D OUTPUT -j $CHAIN 2>/dev/null || true")
                appendLine("$cmd -t mangle -F $CHAIN 2>/dev/null || true")
                appendLine("$cmd -t mangle -X $CHAIN 2>/dev/null || true")
            }
            // IPv6 blackhole chain (only set up when v6 is disabled; harmless if absent)
            appendLine("ip6tables -t filter -D OUTPUT -j ${AppConfig.ROOT_V6_CHAIN} 2>/dev/null || true")
            appendLine("ip6tables -t filter -F ${AppConfig.ROOT_V6_CHAIN} 2>/dev/null || true")
            appendLine("ip6tables -t filter -X ${AppConfig.ROOT_V6_CHAIN} 2>/dev/null || true")
            // routing rule + table
            appendLine("ip rule del fwmark $MARK table $TABLE priority $PRIORITY 2>/dev/null || true")
            appendLine("ip -6 rule del fwmark $MARK table $TABLE priority $PRIORITY 2>/dev/null || true")
            appendLine("ip route flush table $TABLE 2>/dev/null || true")
            appendLine("ip -6 route flush table $TABLE 2>/dev/null || true")
            // LAN / tethering sharing (always cleaned, harmless if it was never set up)
            appendLine("iptables -D FORWARD -j ${AppConfig.ROOT_FWD_CHAIN} 2>/dev/null || true")
            appendLine("iptables -F ${AppConfig.ROOT_FWD_CHAIN} 2>/dev/null || true")
            appendLine("iptables -X ${AppConfig.ROOT_FWD_CHAIN} 2>/dev/null || true")
            appendLine("iptables -t mangle -D FORWARD -o $TUN -p tcp --tcp-flags SYN,RST SYN -j TCPMSS --set-mss 1350 2>/dev/null || true")
            appendLine("iptables -t nat -D PREROUTING -j ${AppConfig.ROOT_DNS_CHAIN} 2>/dev/null || true")
            appendLine("iptables -t nat -F ${AppConfig.ROOT_DNS_CHAIN} 2>/dev/null || true")
            appendLine("iptables -t nat -X ${AppConfig.ROOT_DNS_CHAIN} 2>/dev/null || true")
            // IPv6 LAN-sharing chains (forward accept/reject + forwarded-client marking)
            appendLine("ip6tables -D FORWARD -j ${AppConfig.ROOT_V6_FWD_CHAIN} 2>/dev/null || true")
            appendLine("ip6tables -F ${AppConfig.ROOT_V6_FWD_CHAIN} 2>/dev/null || true")
            appendLine("ip6tables -X ${AppConfig.ROOT_V6_FWD_CHAIN} 2>/dev/null || true")
            appendLine("ip6tables -t mangle -D PREROUTING -j ${AppConfig.ROOT_V6_PRE_CHAIN} 2>/dev/null || true")
            appendLine("ip6tables -t mangle -F ${AppConfig.ROOT_V6_PRE_CHAIN} 2>/dev/null || true")
            appendLine("ip6tables -t mangle -X ${AppConfig.ROOT_V6_PRE_CHAIN} 2>/dev/null || true")
            for (pref in listOf(5000, 5010, 5020, 5025, 5026, 5027, 5030, 5040, 5050, 6000)) {
                appendLine("ip rule del pref $pref 2>/dev/null || true")
            }
            // tun device down + helper process
            appendLine("ip link set dev $TUN down 2>/dev/null || true")
            appendLine("[ -f '$pidFile' ] && kill \$(cat '$pidFile') 2>/dev/null || true")
            appendLine("rm -f '$pidFile'")
            // stop the OOM re-pin loop and restore the core process's LMK priority
            appendLine("[ -f '$oomGuardPid' ] && kill \$(cat '$oomGuardPid') 2>/dev/null || true")
            appendLine("rm -f '$oomGuardPid'")
            appendLine("echo 0 > /proc/$corePid/oom_score_adj 2>/dev/null || true")
        }
    }
}
