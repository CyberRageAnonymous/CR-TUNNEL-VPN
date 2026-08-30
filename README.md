<div align="center">

# CR TUNNEL

<a id="top"></a>

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:0D1117,100:39FF14&height=200&section=header&text=CR%20TUNNEL&fontSize=68&fontColor=FFFFFF&fontAlignY=38&animation=fadeIn&desc=Powered%20by%20Xray-core&descAlignY=62&descSize=18&descColor=FFFFFF" width="100%"/>

[![Stars](https://img.shields.io/github/stars/CyberRageAnonymous/CR-TUNNEL-VPN?style=for-the-badge&color=39FF14&labelColor=0d1117)](https://github.com/CyberRageAnonymous/CR-TUNNEL-VPN/stargazers)
[![License](https://img.shields.io/github/license/CyberRageAnonymous/CR-TUNNEL-VPN?style=for-the-badge&color=39FF14&labelColor=0d1117)](LICENSE)
[![Last Commit](https://img.shields.io/github/last-commit/CyberRageAnonymous/CR-TUNNEL-VPN?style=for-the-badge&color=39FF14&labelColor=0d1117)](https://github.com/CyberRageAnonymous/CR-TUNNEL-VPN/commits/main)
[![Platform](https://img.shields.io/badge/platform-Android-39FF14?style=for-the-badge&labelColor=0d1117&logo=android&logoColor=39FF14)](#download)
[![Core](https://img.shields.io/badge/core-Xray--core-39FF14?style=for-the-badge&labelColor=0d1117)](https://github.com/XTLS/Xray-core)

### [English](#english) · [فارسی](#فارسی)

</div>

---

# English

CR TUNNEL is an Android VPN client for VLESS, XHTTP, VMess, Trojan, Shadowsocks, SOCKS and Hysteria2, built on Xray-core. Open source under GPL-3.0, no ads, no tracking, no in-app purchases.

## Contents

- [Features](#features)
- [How it works](#how-it-works)
- [Download](#download)
- [Per-app proxy](#per-app-proxy)
- [Custom DNS](#custom-dns)
- [Build from source](#build-from-source)
- [Project layout](#project-layout)
- [Privacy](#privacy)
- [Credits](#credits)
- [License](#license)
- [Contact](#contact)
- [Disclaimer](#disclaimer)

## Features

- XHTTP support (stream-up and packet-up) plus the usual VLESS transports (WebSocket, TCP, Reality, gRPC), VMess, Trojan, Shadowsocks, SOCKS and Hysteria2
- Auto optimize: every saved config is ping-tested for real latency, the fastest one is selected and connected automatically
- Import configs from a subscription link or a QR code
- Per-app proxy with 400+ package names pre-loaded
- Auto-update for subscriptions
- VpnService mode and a standalone Root mode

## How it works

Xray-core handles the proxy protocols and routing, wired in through the AndroidLibXrayLite bridge. A native C tunnel (hev-socks5-tunnel) built with the Android NDK turns the local proxy into a device-wide VPN.

The tunnel source is compiled twice: once as a JNI shared library that runs inside the app for normal VpnService mode, and once as a standalone executable for Root mode, which does not need the Android VPN permission dialog.

## Download

The latest build is on the [releases page](https://github.com/CyberRageAnonymous/CR-TUNNEL-VPN/releases/latest). Direct downloads, no login required. Fresh builds are also produced on every push and published under the Actions tab.

| File | Use |
|---|---|
| `CRTunnel_*-arm64-v8a.apk` | Modern 64-bit devices (recommended) |
| `CRTunnel_*-armeabi-v7a.apk` | Older 32-bit devices |
| `CRTunnel_*-x86` / `x86_64` | Emulators and x86 devices |
| `CRTunnel_*-universal.apk` | Works everywhere, larger file |

Minimum requirement: Android 7.0 (API 24).

## Per-app proxy

`proxy.txt` ships with 400+ common package names (browsers, messengers, wallets, circumvention tools), so routing per app takes seconds instead of looking up package IDs by hand. Apps can be set to Proxy, Direct or Default, added to favourites, and selected in bulk.

## Custom DNS

Separate server lists for remote and domestic DNS, plus custom per-domain host mappings, are all configurable from Settings > DNS.

## Build from source

```bash
git clone --recursive https://github.com/CyberRageAnonymous/CR-TUNNEL-VPN.git
cd CR-TUNNEL-VPN

# Play Store build
./gradlew assemblePlaystoreRelease

# F-Droid build
./gradlew assembleFdroidRelease
```

Rebuilding the native `hev-socks5-tunnel` libraries requires the Android NDK with `$NDK_HOME` set:

```bash
NDK_HOME=/path/to/android-ndk ./compile-hevtun.sh
```

## Project layout

```
.
├── app/                              # Main Android app (Kotlin)
├── AndroidLibXrayLite/               # Xray-core bridge (submodule)
├── hev-socks5-tunnel/                # Native tun2socks engine (submodule)
├── docs/                             # Additional documentation
├── fastlane/metadata/android/en-US/  # Store listing metadata
├── compile-hevtun.sh                 # NDK build script for the native tunnel
├── proxy.txt                         # Default per-app proxy list
├── CR.md                             # Privacy policy
└── LICENSE                           # GPL-3.0
```

## Privacy

Configs, test results and settings stay on the device. No telemetry, no ad SDKs, no trackers. The full policy is in [CR.md](CR.md).

## Credits

- [Xray-core](https://github.com/XTLS/Xray-core) by XTLS, the proxy engine
- [AndroidLibXrayLite](https://github.com/2dust/AndroidLibXrayLite) by 2dust, the Xray-core Android bridge
- [hev-socks5-tunnel](https://github.com/heiher/hev-socks5-tunnel) by heiher, the native tun2socks engine

## License

GPL-3.0. See [LICENSE](LICENSE).

## Contact

Developed by Cyber-Rage.

- Telegram: [t.me/R4G3_2024](https://t.me/R4G3_2024)
- Issues and PRs: [GitHub Issues](https://github.com/CyberRageAnonymous/CR-TUNNEL-VPN/issues)

## Disclaimer

CR TUNNEL is a client-side privacy tool. It does not target or attack any system; users apply it to their own traffic. Use it in accordance with the laws that apply to you.

<div align="right"><a href="#top">back to top</a></div>

---

# فارسی

CR TUNNEL یک کلاینت VPN برای اندرویده که از VLESS، XHTTP، VMess، Trojan، Shadowsocks، SOCKS و Hysteria2 پشتیبانی میکنه و روی هسته Xray-core ساخته شده. متنباز با لایسنس GPL-3.0، بدون تبلیغ، بدون ردیابی و بدون خرید داخلبرنامهای.

## فهرست

- [امکانات](#امکانات)
- [نحوه کار](#نحوه-کار)
- [دانلود](#دانلود)
- [پروکسی اختصاصی اپلیکیشنها](#پروکسی-اختصاصی-اپلیکیشنها)
- [DNS سفارشی](#dns-سفارشی)
- [ساخت از سورس](#ساخت-از-سورس)
- [ساختار پروژه](#ساختار-پروژه)
- [حریم خصوصی](#حریم-خصوصی)
- [تشکر و قدردانی](#تشکر-و-قدردانی)
- [لایسنس](#لایسنس)
- [ارتباط با ما](#ارتباط-با-ما)
- [سلب مسئولیت](#سلب-مسئولیت)

## امکانات

- پشتیبانی از XHTTP ( stream-up و packet-up ) بهعلاوه ترنسپورتهای معمول VLESS (WebSocket، TCP، Reality، gRPC)، VMess، Trojan، Shadowsocks، SOCKS و Hysteria2
- بهینهسازی خودکار: همه کانفیگهای ذخیرهشده با پینگ واقعی تست میشن، سریعترین انتخاب و اتصال خودکار برقرار میشه
- ایمپورت کانفیگ با لینک ساباسکریپشن یا اسکن QR
- پروکسی اختصاصی هر اپ با بیش از ۴۰۰ پکیجنیم آماده
- آپدیت خودکار ساباسکریپشنها
- حالت VpnService و حالت مستقل Root

## نحوه کار

Xray-core از طریق پل AndroidLibXrayLite به اپ وصله و پروتکلهای پروکسی و مسیریابی را مدیریت میکنه. یک تانل نیتیو به زبان C (hev-socks5-tunnel) که با Android NDK ساخته میشه، پروکسی محلی را به VPN تمامدستگاه تبدیل میکنه.

سورس تانل دو بار کامپایل میشه: یک بار بهصورت کتابخانه JNI که داخل خود اپ در حالت معمول VpnService اجرا میشه، و یک بار بهصورت فایل اجرایی مستقل برای حالت Root که نیازی به دیالوگ مجوز VPN اندروید نداره.

## دانلود

آخرین نسخه در [صفحه ریلیز](https://github.com/CyberRageAnonymous/CR-TUNNEL-VPN/releases/latest) قرار داره. دانلود مستقیم، بدون نیاز به ورود. روی هر پوش هم یک نسخه جدید ساخته و در تب Actions منتشر میشه.

| فایل | کاربرد |
|---|---|
| `CRTunnel_*-arm64-v8a.apk` | گوشیهای ۶۴ بیتی جدید (پیشنهادی) |
| `CRTunnel_*-armeabi-v7a.apk` | گوشیهای قدیمیتر ۳۲ بیتی |
| `CRTunnel_*-x86` / `x86_64` | شبیهسازها و دستگاههای x86 |
| `CRTunnel_*-universal.apk` | همهجا کار میکنه، حجم بیشتر |

حداقل نسخه موردنیاز: اندروید ۷.۰ (API 24).

## پروکسی اختصاصی اپلیکیشنها

فایل `proxy.txt` از قبل با بیش از ۴۰۰ پکیجنیم رایج (مرورگرها، پیامرسانها، کیفپولها و ابزارهای دور زدن سانسور) پر شده، پس تنظیم مسیریابی هر اپ چند ثانیه طول میکشه. هر اپ را میتوان Proxy، Direct یا پیشفرض کرد، به علاقهمندیها اضافه کرد یا بهصورت گروهی انتخاب کرد.

## DNS سفارشی

از تنظیمات > DNS میتوان فهرست جداگانه برای DNS ریموت و داخلی تعریف کرد و نگاشت دامنه به هاست را سفارشی کرد.

## ساخت از سورس

```bash
git clone --recursive https://github.com/CyberRageAnonymous/CR-TUNNEL-VPN.git
cd CR-TUNNEL-VPN

# ساخت نسخه پلیاستور
./gradlew assemblePlaystoreRelease

# ساخت نسخه اف-درید
./gradlew assembleFdroidRelease
```

برای بازسازی کتابخانههای نیتیو `hev-socks5-tunnel` به Android NDK و متغیر `$NDK_HOME` نیازه:

```bash
NDK_HOME=/path/to/android-ndk ./compile-hevtun.sh
```

## ساختار پروژه

```
.
├── app/                              # اپ اصلی اندروید (Kotlin)
├── AndroidLibXrayLite/               # پل Xray-core (سابماژول)
├── hev-socks5-tunnel/                # موتور نیتیو تانل (سابماژول)
├── docs/                             # مستندات بیشتر
├── fastlane/metadata/android/en-US/  # متادیتای فروشگاه
├── compile-hevtun.sh                 # اسکریپت ساخت NDK برای تانل نیتیو
├── proxy.txt                         # فهرست پیشفرض پروکسی اختصاصی
├── CR.md                             # سیاست حریم خصوصی
└── LICENSE                           # GPL-3.0
```

## حریم خصوصی

کانفیگها، نتایج تست و تنظیمات فقط روی خود دستگاه میمونن. بدون تله متری، بدون SDK تبلیغاتی، بدون ردیاب. سیاست کامل در [CR.md](CR.md).

## تشکر و قدردانی

- [Xray-core](https://github.com/XTLS/Xray-core) از XTLS، موتور پروکسی
- [AndroidLibXrayLite](https://github.com/2dust/AndroidLibXrayLite) از 2dust، پل اندروید Xray-core
- [hev-socks5-tunnel](https://github.com/heiher/hev-socks5-tunnel) از heiher، موتور نیتیو تانل

## لایسنس

GPL-3.0. فایل [LICENSE](LICENSE) را ببینید.

## ارتباط با ما

توسعه توسط Cyber-Rage.

- تلگرام: [t.me/R4G3_2024](https://t.me/R4G3_2024)
- ایشو و پولریکوئست: [GitHub Issues](https://github.com/CyberRageAnonymous/CR-TUNNEL-VPN/issues)

## سلب مسئولیت

CR TUNNEL ابزاری برای حفظ حریم خصوصی در سمت کاربره. هیچ سیستمی را هدف قرار نمیده و بهش حمله نمیکنه؛ کاربران برای محافظت از ترافیک خودشون استفاده میکنن. استفاده از آن مطابق قوانینی که شامل حالت میشه انجام بشه.

<div align="left"><a href="#top">بازگشت به بالا</a></div>

---

<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:39FF14,100:0D1117&height=120&section=footer&animation=fadeIn&reversal=true" width="100%"/>

</div>