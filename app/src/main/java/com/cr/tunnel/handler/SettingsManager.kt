package com.cr.tunnel.handler

import android.content.Context
import android.content.res.AssetManager
import android.os.Build
import android.text.TextUtils
import com.cr.tunnel.AppConfig
import com.cr.tunnel.AppConfig.ANG_PACKAGE
import com.cr.tunnel.AppConfig.DEFAULT_SUBSCRIPTION_ID
import com.cr.tunnel.AppConfig.GEOIP_PRIVATE
import com.cr.tunnel.AppConfig.GEOSITE_PRIVATE
import com.cr.tunnel.AppConfig.TAG_DIRECT
import com.cr.tunnel.AppConfig.VPN
import com.cr.tunnel.dto.V2rayConfig
import com.cr.tunnel.dto.entities.ProfileItem
import com.cr.tunnel.dto.entities.RulesetItem
import com.cr.tunnel.dto.entities.SubscriptionItem
import com.cr.tunnel.enums.EConfigType
import com.cr.tunnel.enums.RoutingType
import com.cr.tunnel.enums.VpnInterfaceAddressConfig
import com.cr.tunnel.extension.moveItem
import com.cr.tunnel.handler.MmkvManager.decodeAllServerList
import com.cr.tunnel.handler.MmkvManager.decodeServerConfig
import com.cr.tunnel.handler.MmkvManager.decodeSubsList
import com.cr.tunnel.handler.MmkvManager.decodeSubscription
import com.cr.tunnel.handler.MmkvManager.encodeSubscription
import com.cr.tunnel.handler.MmkvManager.removeSubscription
import com.cr.tunnel.util.JsonUtil
import com.cr.tunnel.util.LogUtil
import com.cr.tunnel.util.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import kotlin.random.Random

object SettingsManager {

    @Volatile
    private var runtimeSocksPort: Int? = null

    fun initApp(context: Context) {
        ensureDefaultSettings()
        initRoutingRulesets(context)
        CoroutineScope(Dispatchers.IO).launch {
            migrateServerListToSubscriptions()
            migrateHysteria2PinSHA256()
        }
    }

    private fun initRoutingRulesets(context: Context) {
        val exist = MmkvManager.decodeRoutingRulesets()
        if (exist.isNullOrEmpty()) {
            val rulesetList = getPresetRoutingRulesets(context)
            MmkvManager.encodeRoutingRulesets(rulesetList)
        }
    }

    private fun getPresetRoutingRulesets(context: Context, type: RoutingType = RoutingType.WHITE): MutableList<RulesetItem>? {
        val assets = Utils.readTextFromAssets(context, type.fileName)
        if (TextUtils.isEmpty(assets)) {
            return null
        }

        return JsonUtil.fromJsonSafe(assets, Array<RulesetItem>::class.java)?.toMutableList()
    }

    fun resetRoutingRulesetsFromPresets(context: Context, type: RoutingType) {
        val rulesetList = getPresetRoutingRulesets(context, type) ?: return
        resetRoutingRulesetsCommon(rulesetList)
    }

    fun resetRoutingRulesets(content: String?): Boolean {
        if (content.isNullOrEmpty()) {
            return false
        }

        try {
            val rulesetList = JsonUtil.fromJsonSafe(content, Array<RulesetItem>::class.java)?.toMutableList()
            if (rulesetList.isNullOrEmpty()) {
                return false
            }

            resetRoutingRulesetsCommon(rulesetList)
            return true
        } catch (e: Exception) {
            LogUtil.e(ANG_PACKAGE, "Failed to reset routing rulesets", e)
            return false
        }
    }

    private fun resetRoutingRulesetsCommon(rulesetList: MutableList<RulesetItem>) {
        val rulesetNew: MutableList<RulesetItem> = mutableListOf()
        MmkvManager.decodeRoutingRulesets()?.forEach { key ->
            if (key.locked == true) {
                rulesetNew.add(key)
            }
        }

        rulesetNew.addAll(rulesetList)
        MmkvManager.encodeRoutingRulesets(rulesetNew)
    }

    fun getRoutingRuleset(index: Int): RulesetItem? {
        if (index < 0) return null

        val rulesetList = MmkvManager.decodeRoutingRulesets()
        if (rulesetList.isNullOrEmpty()) return null

        return rulesetList[index]
    }

    fun saveRoutingRuleset(index: Int, ruleset: RulesetItem?) {
        if (ruleset == null) return

        var rulesetList = MmkvManager.decodeRoutingRulesets()
        if (rulesetList.isNullOrEmpty()) {
            rulesetList = mutableListOf()
        }

        if (index < 0 || index >= rulesetList.count()) {
            rulesetList.add(0, ruleset)
        } else {
            rulesetList[index] = ruleset
        }
        MmkvManager.encodeRoutingRulesets(rulesetList)
    }

    fun removeRoutingRuleset(index: Int) {
        if (index < 0) return

        val rulesetList = MmkvManager.decodeRoutingRulesets()
        if (rulesetList.isNullOrEmpty()) return

        rulesetList.removeAt(index)
        MmkvManager.encodeRoutingRulesets(rulesetList)
    }

    fun routingRulesetsBypassLan(): Boolean {
        val vpnBypassLan = MmkvManager.decodeSettingsString(AppConfig.PREF_VPN_BYPASS_LAN) ?: "1"
        if (vpnBypassLan == "1") {
            return true
        } else if (vpnBypassLan == "2") {
            return false
        }

        val guid = MmkvManager.getSelectServer() ?: return false
        val config = decodeServerConfig(guid) ?: return false
        if (config.configType == EConfigType.CUSTOM) {
            val raw = MmkvManager.decodeServerRaw(guid) ?: return false
            val v2rayConfig = JsonUtil.fromJsonSafe(raw, V2rayConfig::class.java)
            val exist = v2rayConfig?.routing?.rules?.filter { it.outboundTag == TAG_DIRECT }?.any {
                it.domain?.contains(GEOSITE_PRIVATE) == true || it.ip?.contains(GEOIP_PRIVATE) == true
            }
            return exist == true
        }

        val rulesetItems = MmkvManager.decodeRoutingRulesets()
        val exist = rulesetItems?.filter { it.enabled && it.outboundTag == TAG_DIRECT }?.any {
            it.domain?.contains(GEOSITE_PRIVATE) == true || it.ip?.contains(GEOIP_PRIVATE) == true
        }
        return exist == true
    }

    fun getServerViaRemarks(remarks: String?): ProfileItem? {
        if (remarks.isNullOrEmpty()) {
            return null
        }
        val serverList = decodeAllServerList()
        return serverList
            .mapNotNull { guid -> decodeServerConfig(guid) }
            .firstOrNull { it.remarks == remarks }
    }

    fun getProfileRemarks(excludeConfigTypes: Set<EConfigType> = setOf(EConfigType.CUSTOM)): List<String> {
        return decodeAllServerList()
            .asSequence()
            .mapNotNull { guid -> decodeServerConfig(guid) }
            .filter { profile -> profile.configType !in excludeConfigTypes }
            .map { it.remarks.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .toList()
    }

    fun removeSubscriptionWithDefault(subid: String) {
        SubscriptionUpdater.cancelOne(subId = subid)
        // Remove the subscription
        removeSubscription(subid)

        // After removal, check if there are any subscriptions left. If not, create a default subscription.
        val subsList2 = decodeSubsList()
        if (subsList2.isNotEmpty()) {
            return
        }

        val defaultSub = SubscriptionItem(
            remarks = "Default",
        )
        encodeSubscription(DEFAULT_SUBSCRIPTION_ID, defaultSub)
    }

    fun getSocksPort(): Int {
        val port =
            if (IsDynamicSocksPort()) {
                runtimeSocksPort ?: refreshRuntimeSocksPort()
            } else {
                Utils.parseInt(MmkvManager.decodeSettingsString(AppConfig.PREF_SOCKS_PORT), AppConfig.PORT_SOCKS.toInt())
            }
        return port ?: AppConfig.PORT_SOCKS.toInt()
    }

    @Synchronized
    fun refreshRuntimeSocksPort(): Int? {
        if (IsDynamicSocksPort()) {
            runtimeSocksPort = generateRandomSocksPort()
            return runtimeSocksPort
        }
        return null
    }

    fun getSocksUsername(): String? {
        return MmkvManager.decodeSettingsString(AppConfig.PREF_SOCKS_USERNAME)?.trim()?.takeIf { it.isNotEmpty() }
    }

    fun getSocksPassword(): String? {
        return MmkvManager.decodeSettingsString(AppConfig.PREF_SOCKS_PASSWORD)?.trim()?.takeIf { it.isNotEmpty() }
    }

    fun getHttpPort(): Int {
        return getSocksPort() + if (Utils.isXray()) 0 else 1
    }

    private fun IsDynamicSocksPort(): Boolean {
        return MmkvManager.decodeSettingsBool(AppConfig.PREF_DYNAMIC_SOCKS_PORT, false)
    }

    private fun generateRandomSocksPort(): Int {
        return Random.nextInt(10000, 65535)
    }

    fun initAssets(context: Context, assets: AssetManager) {
        val extFolder = Utils.userAssetPath(context)

        try {
            val geo = arrayOf(AppConfig.GEOSITE_DAT, AppConfig.GEOIP_DAT, AppConfig.GEOIP_ONLY_CN_PRIVATE_DAT)
            assets.list("")
                ?.filter { geo.contains(it) }
                ?.filter { !File(extFolder, it).exists() }
                ?.forEach {
                    val target = File(extFolder, it)
                    assets.open(it).use { input ->
                        FileOutputStream(target).use { output ->
                            input.copyTo(output)
                        }
                    }
                    LogUtil.i(AppConfig.TAG, "Copied from apk assets folder to ${target.absolutePath}")
                }
        } catch (e: Exception) {
            LogUtil.e(ANG_PACKAGE, "asset copy failed", e)
        }
    }

    fun getDomesticDnsServers(): List<String> {
        val domesticDns =
            MmkvManager.decodeSettingsString(AppConfig.PREF_DOMESTIC_DNS) ?: AppConfig.DNS_DIRECT
        val ret = domesticDns.split(",").filter { Utils.isPureIpAddress(it) || Utils.isCoreDNSAddress(it) }
        if (ret.isEmpty()) {
            return listOf(AppConfig.DNS_DIRECT)
        }
        return ret
    }

    fun getRemoteDnsServers(): List<String> {
        val remoteDns =
            MmkvManager.decodeSettingsString(AppConfig.PREF_REMOTE_DNS) ?: AppConfig.DNS_PROXY
        val ret = remoteDns.split(",").filter { Utils.isPureIpAddress(it) || Utils.isCoreDNSAddress(it) }
        if (ret.isEmpty()) {
            return listOf(AppConfig.DNS_PROXY)
        }
        return ret
    }

    fun getVpnDnsServers(): List<String> {
        val vpnDns = MmkvManager.decodeSettingsString(AppConfig.PREF_VPN_DNS) ?: AppConfig.DNS_VPN
        return vpnDns.split(",").filter { Utils.isPureIpAddress(it) }
    }

    fun getDelayTestUrl(second: Boolean = false): String {
        return if (second) {
            AppConfig.DELAY_TEST_URL2
        } else {
            MmkvManager.decodeSettingsString(AppConfig.PREF_DELAY_TEST_URL)
                ?: AppConfig.DELAY_TEST_URL
        }
    }

    fun getRealPingConcurrency(): Int {
        val value = MmkvManager.decodeSettingsString(AppConfig.PREF_REAL_PING_CONCURRENCY)?.toIntOrNull() ?: 16
        return value.coerceIn(1, 128)
    }

    fun getCurrentVpnInterfaceAddressConfig(): VpnInterfaceAddressConfig {
        val selectedIndex = MmkvManager.decodeSettingsString(AppConfig.PREF_VPN_INTERFACE_ADDRESS_CONFIG_INDEX, "0")?.toInt()
        return VpnInterfaceAddressConfig.getConfigByIndex(selectedIndex ?: 0)
    }

    fun getVpnMtu(): Int {
        return Utils.parseInt(MmkvManager.decodeSettingsString(AppConfig.PREF_VPN_MTU), AppConfig.VPN_MTU)
    }

    fun isUsingHevTun(): Boolean {
        return MmkvManager.decodeSettingsBool(AppConfig.PREF_USE_HEV_TUNNEL, false) &&
            isHevTunLibraryAvailable()
    }

    @Volatile
    private var hevTunAvailable: Boolean? = null

    private fun isHevTunLibraryAvailable(): Boolean {
        hevTunAvailable?.let { return it }
        val result = try {
            System.loadLibrary("hev-socks5-tunnel")
            true
        } catch (e: Throwable) {
            LogUtil.w(AppConfig.TAG, "hev-socks5-tunnel library unavailable, falling back to Xray TUN: ${e.message}")
            false
        }
        hevTunAvailable = result
        return result
    }

    fun isVpnMode(): Boolean {
        val mode = MmkvManager.decodeSettingsString(AppConfig.PREF_MODE)
        return mode == null || mode == VPN
    }

    fun isRootMode(): Boolean {
        return MmkvManager.decodeSettingsBool(AppConfig.PREF_ROOT_MODE_ENABLE, false)
    }

    fun isKillSwitchEnabled(): Boolean {
        return MmkvManager.decodeSettingsBool(AppConfig.PREF_KILL_SWITCH, false)
    }

    fun canUseProcessRouting(): Boolean {
        // Android 10+
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return false
        }

        // Must xray tun
        if (isUsingHevTun()) {
            return false
        }

        // Must have route only enabled
        if (MmkvManager.decodeSettingsBool(AppConfig.PREF_ROUTE_ONLY_ENABLED, false) == false) {
            return false
        }

        return true
    }

    private fun ensureDefaultSettings() {
        // Write defaults in the exact order requested by the user
        ensureDefaultValue(AppConfig.PREF_MODE, VPN)
        ensureDefaultValue(AppConfig.PREF_VPN_DNS, AppConfig.DNS_VPN)
        ensureDefaultValue(AppConfig.PREF_VPN_MTU, AppConfig.VPN_MTU.toString())
        ensureDefaultValue(AppConfig.PREF_SOCKS_PORT, AppConfig.PORT_SOCKS)
        ensureDefaultValue(AppConfig.PREF_REMOTE_DNS, AppConfig.DNS_PROXY)
        ensureDefaultValue(AppConfig.PREF_DOMESTIC_DNS, AppConfig.DNS_DIRECT)
        ensureDefaultValue(AppConfig.PREF_DELAY_TEST_URL, AppConfig.DELAY_TEST_URL)
        ensureDefaultValue(AppConfig.PREF_IP_API_URL, AppConfig.IP_API_URL)
        ensureDefaultValue(AppConfig.PREF_HEV_TUNNEL_RW_TIMEOUT, AppConfig.HEVTUN_RW_TIMEOUT)
        ensureDefaultValue(AppConfig.PREF_MUX_CONCURRENCY, "8")
        ensureDefaultValue(AppConfig.PREF_MUX_XUDP_CONCURRENCY, "8")
        ensureDefaultValue(AppConfig.PREF_FRAGMENT_LENGTH, "50-100")
        ensureDefaultValue(AppConfig.PREF_FRAGMENT_INTERVAL, "10-20")
        ensureDefaultValue(AppConfig.PREF_FRAGMENT_MAXSPLIT, "10")
        ensureDefaultValue(AppConfig.PREF_OBSERVATORY_LEAST_PING_INTERVAL, AppConfig.OBSERVATORY_LEAST_PING_INTERVAL)
        ensureDefaultValue(AppConfig.PREF_OBSERVATORY_LEAST_LOAD_INTERVAL, AppConfig.OBSERVATORY_LEAST_LOAD_INTERVAL)
        ensureDefaultValue(AppConfig.PREF_OBSERVATORY_LEAST_LOAD_METHOD, AppConfig.OBSERVATORY_LEAST_LOAD_METHOD)
        ensureDefaultValue(AppConfig.PREF_OBSERVATORY_LEAST_LOAD_SAMPLING, AppConfig.OBSERVATORY_LEAST_LOAD_SAMPLING)
        ensureDefaultValue(AppConfig.PREF_OBSERVATORY_LEAST_LOAD_TIMEOUT, AppConfig.OBSERVATORY_LEAST_LOAD_TIMEOUT)
    }

    private fun ensureDefaultValue(key: String, default: String) {
        if (MmkvManager.decodeSettingsString(key).isNullOrEmpty()) {
            MmkvManager.encodeSettings(key, default)
        }
    }

    private fun migrateHysteria2PinSHA256() {
        // Check if migration has already been done
        val migrationKey = "hysteria2_pin_sha256_migrated"
        if (MmkvManager.decodeSettingsBool(migrationKey, false)) {
            return
        }

        val serverList = decodeAllServerList()

        for (guid in serverList) {
            val profile = decodeServerConfig(guid) ?: continue
            if (profile.configType != EConfigType.HYSTERIA2) {
                continue
            }
            if (profile.pinSHA256.isNullOrEmpty() || !profile.pinnedCA256.isNullOrEmpty()) {
                continue
            }
            profile.pinnedCA256 = profile.pinSHA256
            profile.pinSHA256 = null
            MmkvManager.encodeServerConfig(guid, profile)
        }

        MmkvManager.encodeSettings(migrationKey, true)
    }

    private fun migrateServerListToSubscriptions() {
        // Check if migration has already been done
        val migrationKey = "server_list_to_subscriptions_migrated"
        if (MmkvManager.decodeSettingsBool(migrationKey, false)) {
            return
        }

        // Ensure default subscription exists before migration
        ensureDefaultSubscription()

        // Read existing server list from legacy KEY_ANG_CONFIGS
        val oldJson = MmkvManager.readLegacyServerList()
        if (oldJson.isNullOrBlank()) {
            // No data to migrate, mark as done
            MmkvManager.encodeSettings(migrationKey, true)
            return
        }

        val guids = JsonUtil.fromJsonSafe(oldJson, Array<String>::class.java) ?: run {
            MmkvManager.encodeSettings(migrationKey, true)
            return
        }

        val subscriptionServerMap = mutableMapOf<String, MutableList<String>>()

        // Group servers by subscription (use default subscription for empty subscriptionId)
        guids.forEach { guid ->
            val config = decodeServerConfig(guid) ?: return@forEach
            val subId = config.subscriptionId.ifEmpty { DEFAULT_SUBSCRIPTION_ID }

            subscriptionServerMap.getOrPut(subId) { mutableListOf() }.add(guid)
        }

        // Update each subscription's serverList (including default subscription)
        subscriptionServerMap.forEach { (subId, serverGuids) ->
            MmkvManager.encodeServerList(serverGuids, subId)
        }

        // Mark migration as complete
        MmkvManager.encodeSettings(migrationKey, true)
    }

    private fun ensureDefaultSubscription() {
        if (decodeSubscription(DEFAULT_SUBSCRIPTION_ID) == null) {
            val defaultSub = SubscriptionItem(
                remarks = "Default",
            )
            encodeSubscription(DEFAULT_SUBSCRIPTION_ID, defaultSub)

            // Move to the top
            val subsList = decodeSubsList()
            if (subsList.moveItem(subsList.lastIndex, 0)) {
                MmkvManager.encodeSubsList(subsList)
            }
        }
    }

}
