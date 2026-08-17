<div align="center">

# ⚡ CR TUNNEL

<a id="top"></a>

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:0D1117,100:39FF14&height=200&section=header&text=CR%20TUNNEL&fontSize=68&fontColor=FFFFFF&fontAlignY=38&animation=fadeIn&desc=Powered%20by%20Xray-core&descAlignY=62&descSize=18&descColor=FFFFFF" width="100%"/>

[![Stars](https://img.shields.io/github/stars/CyebRageAnonymuos/CR-TUNNEL-VPN?style=for-the-badge&color=39FF14&labelColor=0d1117)](https://github.com/CyebRageAnonymuos/CR-TUNNEL-VPN/stargazers)
[![License](https://img.shields.io/github/license/CyebRageAnonymuos/CR-TUNNEL-VPN?style=for-the-badge&color=39FF14&labelColor=0d1117)](LICENSE)
[![Last Commit](https://img.shields.io/github/last-commit/CyebRageAnonymuos/CR-TUNNEL-VPN?style=for-the-badge&color=39FF14&labelColor=0d1117)](https://github.com/CyebRageAnonymuos/CR-TUNNEL-VPN/commits/main)
[![Platform](https://img.shields.io/badge/platform-Android-39FF14?style=for-the-badge&labelColor=0d1117&logo=android&logoColor=39FF14)](#-download)
[![Core](https://img.shields.io/badge/core-Xray--core-39FF14?style=for-the-badge&labelColor=0d1117)](https://github.com/XTLS/Xray-core)

### **[🇬🇧 English](#-english)** &nbsp;·&nbsp; **[ فارسی](#-فارسی)**

</div>

---
---

<div id="-english"></div>

# 🇬🇧 English

<div align="center">

[![Typing SVG](https://readme-typing-svg.demolab.com?font=Fira+Code&weight=600&size=22&pause=1000&color=39FF14&center=true&vCenter=true&width=600&lines=Fast.+Free.+Open+Source.;VLESS%2C+VMess%2C+Trojan%2C+Shadowsocks;Powered+by+Xray-core;Zero+Ads.+Zero+Tracking.)](https://github.com/CyebRageAnonymuos/CR-TUNNEL-VPN)

</div>

CR TUNNEL is a free, fully open-source Android VPN client for VLESS, XHTTP, VMess, Trojan, Shadowsocks, SOCKS, and Hysteria2 — built on the Xray-core engine and wrapped in a neon Cyber-Rage shell. No ads. No tracking. No catch.

## 📋 Table of Contents

- [Features](#features)
- [How It Works](#how-it-works)
- [Download](#download)
- [Auto Optimize](#auto-optimize)
- [Supported Protocols](#supported-protocols)
- [Per-App Proxy](#per-app-proxy)
- [Build From Source](#build-from-source)
- [Project Structure](#project-structure)
- [Privacy](#privacy)
- [Credits](#credits)
- [License](#license)
- [Contact](#contact)
- [Disclaimer](#disclaimer)

## ✨ Features

| | Feature |
|---|---|
| ⚡ | **Auto Optimize** — real-ping-tests every saved config and auto-connects to the fastest |
| 🎨 | Neon Cyber-Rage dark UI |
| 🆓 | 100% free, no ads, no in-app purchases |
| 🔓 | Fully open source (GPL-3.0) |
| 🔌 | VLESS with full XHTTP support (stream-up, packet-up, WS, TCP, Reality, gRPC) |
| 📄 | VMess, Trojan, Shadowsocks, SOCKS, Hysteria2 |
| 📥 | Import via subscription link or QR code |
| 🔍 | Real latency testing per config |
| 🌍 | Per-app proxy with 400+ apps pre-listed |
| 🔄 | Auto subscription updates |
| 🛡️ | VpnService mode **and** standalone Root mode |

## 🧠 How It Works

CR TUNNEL runs on two engines working together:

1. **Xray-core**, wired into the app through the `AndroidLibXrayLite` bridge, handles every proxy protocol — encryption, routing rules, and talking to your server.
2. **hev-socks5-tunnel**, a native C tunnel compiled with the Android NDK, turns that local proxy into a real device-wide VPN. It's built twice from the same source: once as a JNI library that runs inside the app for standard **VpnService** mode, and once as a standalone executable for an alternative **Root mode** that doesn't need Android's VPN permission dialog at all.

## 📥 Download

> Direct download (no login required):

```
https://github.com/CyebRageAnonymuos/CR-TUNNEL-VPN/releases/tag/v1.0.0
```

> Or grab the latest build from the **Actions** tab of this repo:

```
https://github.com/CyebRageAnonymuos/CR-TUNNEL-VPN/actions
```

| File | Use |
|---|---|
| `CRTunnel_*-arm64-v8a.apk` | Modern 64-bit devices (recommended) |
| `CRTunnel_*-armeabi-v7a.apk` | Older 32-bit devices |
| `CRTunnel_*-x86` / `x86_64` | Emulators & x86 devices |
| `CRTunnel_*-universal.apk` | Works everywhere, bigger file |

Minimum: **Android 7.0 (API 24)**.

## 🚀 Auto Optimize

Tap the **⚡ button** on the main screen and CR TUNNEL will:

1. Real-ping-test **every** config you've saved.
2. Pick the one with the **lowest latency**.
3. **Auto-connect** — no manual switching.

## 🌍 Supported Protocols

`VLESS` (XHTTP · WebSocket · TCP · Reality · gRPC) · `VMess` · `Trojan` · `Shadowsocks` · `SOCKS` · `Hysteria2`

## 🎯 Per-App Proxy

`proxy.txt` ships with **400+ common app package names** pre-loaded — browsers, messengers, wallets, and censorship-circumvention tools — so setting up per-app routing takes seconds instead of hunting down package IDs yourself.

## 🛠 Build From Source

```bash
git clone --recursive https://github.com/CyebRageAnonymuos/CR-TUNNEL-VPN.git
cd CR-TUNNEL-VPN

# Open the CR-TUNNEL/ folder in Android Studio, or build headlessly:
cd CR-TUNNEL && ./gradlew assembleRelease
```

To rebuild the native `hev-socks5-tunnel` libraries, you'll need the **Android NDK** with `$NDK_HOME` set:

```bash
NDK_HOME=/path/to/android-ndk ./compile-hevtun.sh
```

GitHub Actions also builds a fresh APK on every push — check the **Actions** tab.

## 📁 Project Structure

```
CR-TUNNEL-VPN/
├── CR-TUNNEL/                          # Main Android app (Kotlin/Java)
├── AndroidLibXrayLite/                 # Xray-core ↔ Android bridge (submodule)
├── hev-socks5-tunnel/                  # Native tun2socks engine (submodule)
├── docs/                               # Project documentation
├── fastlane/metadata/android/en-US/    # Store-listing metadata
├── compile-hevtun.sh                   # NDK build script for the native tunnel
├── proxy.txt                           # Default per-app proxy list
├── README.md
├── CR.md                               # Privacy policy
└── LICENSE                             # GPL-3.0
```

## 🔒 Privacy

No telemetry, no ad SDKs, no trackers — every config, test result, and setting stays on your device. Full policy in [`CR.md`](CR.md).

## 🙏 Credits

CR TUNNEL stands on the shoulders of some excellent open-source work:

- **[Xray-core](https://github.com/XTLS/Xray-core)** by [XTLS](https://github.com/XTLS) — the proxy engine
- **[AndroidLibXrayLite](https://github.com/2dust/AndroidLibXrayLite)** by [2dust](https://github.com/2dust) — Xray-core's Android bridge
- **[hev-socks5-tunnel](https://github.com/heiher/hev-socks5-tunnel)** by [heiher](https://github.com/heiher) — the native tun2socks engine

## ⚖️ License

Released under **GPL-3.0**. See [`LICENSE`](LICENSE).

## 📡 Contact

Built by **Cyber-Rage** — an open-source security & dev crew.

- Telegram: [t.me/R4G3_2024](https://t.me/R4G3_2024)
- WhatsApp: **CyberRageAnonymuos**
- Session:
  ```
  05fd51ac639edc257133f9364529eff3af1d69c5c18b31f321ba466b3823a0a805
  ```
- Issues & PRs: [GitHub Issues](https://github.com/CyebRageAnonymuos/CR-TUNNEL-VPN/issues)

## ⚠️ Disclaimer

CR TUNNEL is a client-side privacy and anti-censorship tool. It doesn't target, exploit, or attack any system — it's used voluntarily by end users to protect their own traffic. Use it in accordance with the laws that apply to you.
<div align="right"><a href="#top">↑ back to top</a></div>

---

---

<div id="-فارسی"></div>

#  فارسی

<div align="center">

<img src="https://capsule-render.vercel.app/api?type=soft&color=0:39FF14,100:0D1117&height=150&section=header&text=CR%20TUNNEL&fontSize=55&fontColor=FFFFFF&fontAlignY=50&animation=fadeIn" width="100%"/>

</div>

CR TUNNEL یک کلاینت VPN رایگان و کاملاً متن‌باز برای اندرویده که از VLESS، XHTTP، VMess، Trojan، Shadowsocks، SOCKS و Hysteria2 پشتیبانی می‌کنه — روی هسته‌ی Xray-core ساخته شده و توی یک پوسته‌ی نئونی از Cyber-Rage پیچیده شده. بدون تبلیغ، بدون ردیابی، بدون هیچ حقه‌ای.

## 📋 فهرست

- [امکانات](#امکانات)
- [نحوه کارکرد](#نحوه-کارکرد)
- [دانلود](#دانلود)
- [بهینه‌سازی خودکار](#بهینه‌سازی-خودکار)
- [پروتکل‌های پشتیبانی‌شده](#پروتکل‌های-پشتیبانی‌شده)
- [پروکسی اختصاصی اپ‌ها](#پروکسی-اختصاصی-اپها)
- [ساخت از سورس](#ساخت-از-سورس)
- [ساختار پروژه](#ساختار-پروژه)
- [حریم خصوصی](#حریم-خصوصی)
- [تشکر و قدردانی](#تشکر-و-قدردانی)
- [لایسنس](#لایسنس)
- [ارتباط با ما](#ارتباط-با-ما)
- [سلب مسئولیت](#سلب-مسئولیت)

## ✨ امکانات

| | امکان |
|---|---|
| ⚡ | **بهینه‌سازی خودکار** — همه‌ی کانفیگ‌های ذخیره‌شده رو با پینگ واقعی تست و به سریع‌ترین وصل می‌شه |
| 🎨 | رابط کاربری تیره و نئونی با امضای Cyber-Rage |
| 🆓 | صد در صد رایگان، بدون تبلیغ و بدون خرید داخل‌برنامه‌ای |
| 🔓 | کاملاً متن‌باز با لایسنس GPL-3.0 |
| 🔌 | پشتیبانی کامل VLESS با XHTTP (استریم‌آپ، پکت‌آپ، WS، TCP، Reality، gRPC) |
| 📄 | پشتیبانی از VMess، Trojan، Shadowsocks، SOCKS و Hysteria2 |
| 📥 | وارد کردن کانفیگ با لینک ساب‌اسکریپشن یا اسکن QR |
| 🔍 | تست تأخیر واقعی برای تک‌تک کانفیگ‌ها |
| 🌍 | پروکسی اختصاصی هر اپ، با فهرست آماده‌ی بیش از ۴۰۰ اپلیکیشن |
| 🔄 | آپدیت خودکار ساب‌اسکریپشن‌ها |
| 🛡️ | هم حالت VpnService **و** هم حالت روت مستقل |

## 🧠 نحوه کارکرد

CR TUNNEL روی دو موتور کار می‌کنه:

۱. **Xray-core** که از طریق پل `AndroidLibXrayLite` به اپ وصل شده، مسئول همه‌ی پروتکل‌های پروکسی، رمزنگاری، قوانین مسیریابی و ارتباط با سرورته.

۲. **hev-socks5-tunnel**، یک تانل نیتیو به زبان C که با Android NDK کامپایل می‌شه و همون پروکسی محلی رو به یک VPN واقعیِ سراسر دستگاه تبدیل می‌کنه. این بخش از یک سورس، دو بار ساخته می‌شه: یک بار به‌صورت کتابخانه‌ی JNI که داخل خود اپ برای حالت استاندارد **VpnService** اجرا می‌شه، و یک بار به‌صورت یک فایل اجرایی مستقل برای حالت جایگزین **Root** که اصلاً نیازی به دیالوگ مجوز VPN اندروید نداره.

## 📥 دانلود

> دانلود مستقیم (بدون نیاز به ورود):

```
https://github.com/CyebRageAnonymuos/CR-TUNNEL-VPN/releases/tag/v1.0.0
```

> یا آخرین نسخه رو از تب **Actions** همین ریپو بردار:

```
https://github.com/CyebRageAnonymuos/CR-TUNNEL-VPN/actions
```

| فایل | کاربرد |
|---|---|
| `CRTunnel_*-arm64-v8a.apk` | گوشی‌های ۶۴ بیتی جدید (پیشنهادی) |
| `CRTunnel_*-armeabi-v7a.apk` | گوشی‌های قدیمی‌تر ۳۲ بیتی |
| `CRTunnel_*-x86` / `x86_64` | شبیه‌سازها و دستگاه‌های x86 |
| `CRTunnel_*-universal.apk` | همه‌جا کار می‌کنه، حجمش بیشتره |

حداقل نسخه‌ی موردنیاز: **اندروید ۷.۰ (API 24)**.

## 🚀 بهینه‌سازی خودکار

دکمه‌ی **⚡** رو بزن، همین سه اتفاق می‌افته: تست پینگ واقعی روی همه‌ی کانفیگ‌ها، انتخاب کم‌تأخیرترین گزینه، و اتصال خودکار بهش — بدون این‌که خودت دستی چیزی رو عوض کنی.

## 🌍 پروتکل‌های پشتیبانی‌شده

`VLESS` (XHTTP · WebSocket · TCP · Reality · gRPC) · `VMess` · `Trojan` · `Shadowsocks` · `SOCKS` · `Hysteria2`

## 🎯 پروکسی اختصاصی اپ‌ها

فایل `proxy.txt` از قبل با **بیش از ۴۰۰ پکیج‌نیم اپلیکیشن رایج** پر شده — مرورگرها، پیام‌رسان‌ها، کیف‌پول‌ها و ابزارهای دور زدن سانسور — یعنی تنظیم پروکسی اختصاصیِ هر اپ چند ثانیه طول می‌کشه، نه این‌که خودت دنبال package ID بگردی.

## 🛠 ساخت از سورس

```bash
git clone --recursive https://github.com/CyebRageAnonymuos/CR-TUNNEL-VPN.git
cd CR-TUNNEL-VPN

# پوشه‌ی CR-TUNNEL/ رو تو Android Studio باز کن، یا بدون رابط گرافیکی بساز:
cd CR-TUNNEL && ./gradlew assembleRelease
```

برای بازسازی کتابخانه‌های نیتیو `hev-socks5-tunnel` به **Android NDK** و متغیر محیطی `$NDK_HOME` نیاز داری:

```bash
NDK_HOME=/path/to/android-ndk ./compile-hevtun.sh
```

ضمناً روی هر پوش، GitHub Actions هم به‌صورت خودکار یک APK جدید می‌سازه — تب **Actions** رو چک کن.

## 📁 ساختار پروژه

```
CR-TUNNEL-VPN/
├── CR-TUNNEL/                          # اپ اصلی اندروید (Kotlin/Java)
├── AndroidLibXrayLite/                 # پل Xray-core به اندروید (ساب‌ماژول)
├── hev-socks5-tunnel/                  # موتور نیتیو تانل (ساب‌ماژول)
├── docs/                               # مستندات پروژه
├── fastlane/metadata/android/en-US/    # متادیتای فروشگاه اپ
├── compile-hevtun.sh                   # اسکریپت ساخت NDK برای تانل نیتیو
├── proxy.txt                           # فهرست پیش‌فرض پروکسی اختصاصی
├── README.md
├── CR.md                               # سیاست حریم خصوصی
└── LICENSE                             # GPL-3.0
```

## 🔒 حریم خصوصی

بدون تله‌متری، بدون SDK تبلیغاتی، بدون ردیاب — همه‌ی کانفیگ‌ها، نتایج تست و تنظیمات فقط روی خود دستگاهت می‌مونن. سیاست کامل در [`CR.md`](CR.md).

## 🙏 تشکر و قدردانی

CR TUNNEL روی شونه‌ی چند تا پروژه‌ی متن‌باز عالی ایستاده:

- **[Xray-core](https://github.com/XTLS/Xray-core)** از [XTLS](https://github.com/XTLS) — موتور پروکسی
- **[AndroidLibXrayLite](https://github.com/2dust/AndroidLibXrayLite)** از [2dust](https://github.com/2dust) — پل اندرویدِ Xray-core
- **[hev-socks5-tunnel](https://github.com/heiher/hev-socks5-tunnel)** از [heiher](https://github.com/heiher) — موتور نیتیو تانل

## ⚖️ لایسنس

منتشرشده تحت لایسنس **GPL-3.0**. فایل [`LICENSE`](LICENSE) رو ببین.

## 📡 ارتباط با ما

ساخته‌شده توسط **Cyber-Rage** — یک تیم متن‌باز امنیت و توسعه.

- تلگرام: [t.me/R4G3_2024](https://t.me/R4G3_2024)
- واتساپ: **CyberRageAnonymuos**
- Session:
  ```
  05fd51ac639edc257133f9364529eff3af1d69c5c18b31f321ba466b3823a0a805
  ```
- ایشو و پول‌ریکوئست: [GitHub Issues](https://github.com/CyebRageAnonymuos/CR-TUNNEL-VPN/issues)

## ⚠️ سلب مسئولیت

CR TUNNEL یک ابزار سمت‌کاربر برای حریم خصوصی و دور زدن سانسوره. هیچ سیستمی رو هدف قرار نمی‌ده یا بهش حمله نمی‌کنه — کاملاً داوطلبانه توسط کاربر نهایی برای محافظت از ترافیک خودش استفاده می‌شه. استفاده از اون رو مطابق قوانینی که شامل حالت می‌شه انجام بده.

<div align="left"><a href="#top">بازگشت به بالا ↑</a></div>

---

<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:39FF14,100:0D1117&height=120&section=footer&animation=fadeIn&reversal=true" width="100%"/>

Made with ⚡ by **Cyber-Rage**

</div>
