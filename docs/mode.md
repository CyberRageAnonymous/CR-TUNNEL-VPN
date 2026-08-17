# CR TUNNEL — Modes

CR TUNNEL can run in three modes. You can switch between them in **Settings → Mode**.

## 1. VPN Mode (recommended)

The default mode. Uses Android's standard **VpnService** API.

- Works on any device, no root required.
- On first start, Android shows the VPN permission dialog — grant it and the app remembers the choice.
- Traffic is routed through a local `tun2socks` tunnel into the Xray-core engine.

## 2. Root Mode

For devices that already have root access.

- Runs the tunnel as a standalone native process — no VPN permission dialog at all.
- Routes the **whole device's traffic**, including raw DNS, into the core.
- Requires root privileges; the app will prompt for them when enabling this mode.

## 3. Proxy-Only Mode

Runs the proxy engine **without** creating a VPN tunnel.

- The Xray core listens on a local **SOCKS5 proxy** (`127.0.0.1:10808` by default).
- Use it together with tools that support SOCKS5 (browsers, apps, `curl --socks5 ...`, etc.).
- Does not affect other apps on the device.

## Per-App Proxy

In VPN mode you can restrict the tunnel to a specific set of apps:

1. Open **Settings → Per-App Proxy**.
2. Toggle **Bypass mode** to invert the selection (proxy everything *except* the chosen apps).
3. The default list ships with 400+ common app package names (`proxy.txt`) so you rarely need to type package IDs yourself.

## Tips

- If a site doesn't load, try **Auto Optimize** (⚡) to switch to the fastest config.
- Make sure the selected config passes **TCPing** before connecting.
- Private DNS (Android) should be left as **Off** while connected.
