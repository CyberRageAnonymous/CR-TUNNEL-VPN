package com.cr.tunnel.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.LocaleList
import android.provider.Settings
import android.util.Base64
import android.util.Patterns
import android.webkit.URLUtil
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.cr.tunnel.AppConfig
import com.cr.tunnel.AppConfig.LOOPBACK
import com.cr.tunnel.BuildConfig
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object Utils {

    private val IPV4_REGEX =
        Regex("^([01]?[0-9]?[0-9]|2[0-4][0-9]|25[0-5])\\.([01]?[0-9]?[0-9]|2[0-4][0-9]|25[0-5])\\.([01]?[0-9]?[0-9]|2[0-4][0-9]|25[0-5])\\.([01]?[0-9]?[0-9]|2[0-4][0-9]|25[0-5])$")
    private val IPV6_REGEX = Regex("^((?:[0-9A-Fa-f]{1,4}))?((?::[0-9A-Fa-f]{1,4}))*::((?:[0-9A-Fa-f]{1,4}))?((?::[0-9A-Fa-f]{1,4}))*|((?:[0-9A-Fa-f]{1,4}))((?::[0-9A-Fa-f]{1,4})){7}$")

    fun parseInt(str: String?, default: Int = 0): Int {
        return str?.toIntOrNull() ?: default
    }

    fun getClipboard(context: Context): String {
        return try {
            val cmb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cmb.primaryClip?.getItemAt(0)?.text.toString()
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to get clipboard content", e)
            ""
        }
    }

    fun setClipboard(context: Context, content: String) {
        try {
            val cmb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText(null, content)
            cmb.setPrimaryClip(clipData)
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to set clipboard content", e)
        }
    }

    fun decode(text: String?): String {
        return tryDecodeBase64(text) ?: text?.trimEnd('=')?.let { tryDecodeBase64(it) }.orEmpty()
    }

    private fun tryDecodeBase64(text: String?): String? {
        if (text.isNullOrEmpty()) return null

        try {
            return Base64.decode(text, Base64.NO_WRAP).toString(Charsets.UTF_8)
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to decode standard base64", e)
        }
        try {
            return Base64.decode(text, Base64.NO_WRAP.or(Base64.URL_SAFE)).toString(Charsets.UTF_8)
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to decode URL-safe base64", e)
        }
        return null
    }

    fun encode(text: String, removePadding: Boolean = false): String {
        return try {
            var encoded = Base64.encodeToString(text.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            if (removePadding) {
                encoded = encoded.trimEnd('=')
            }
            encoded
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to encode text to base64", e)
            ""
        }
    }

    fun isIpAddress(value: String?): Boolean {
        if (value.isNullOrEmpty()) return false

        try {
            var addr = value.trim()
            if (addr.isEmpty()) return false

            //CIDR
            if (addr.contains("/")) {
                val arr = addr.split("/")
                if (arr.size == 2 && arr[1].toIntOrNull() != null && arr[1].toInt() > -1) {
                    addr = arr[0]
                }
            }

            // Handle IPv4-mapped IPv6 addresses
            if (addr.startsWith("::ffff:") && '.' in addr) {
                addr = addr.drop(7)
            } else if (addr.startsWith("[::ffff:") && '.' in addr) {
                addr = addr.drop(8).replace("]", "")
            }

            val octets = addr.split('.')
            if (octets.size == 4) {
                if (octets[3].contains(":")) {
                    addr = addr.substring(0, addr.indexOf(":"))
                }
                return isIpv4Address(addr)
            }

            return isIpv6Address(addr)
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to validate IP address", e)
            return false
        }
    }

    fun isPureIpAddress(value: String): Boolean {
        return isIpv4Address(value) || isIpv6Address(value)
    }

    fun isDomainName(input: String?): Boolean {
        if (input.isNullOrEmpty()) return false

        // Must not be an IP address and must be a valid URL format
        return !isPureIpAddress(input) && isValidUrl(input)
    }

    private fun isIpv4Address(value: String): Boolean {
        return IPV4_REGEX.matches(value)
    }

    private fun isIpv6Address(value: String): Boolean {
        var addr = value
        if (addr.startsWith("[") && addr.endsWith("]")) {
            addr = addr.drop(1).dropLast(1)
        }
        return IPV6_REGEX.matches(addr)
    }

    fun isCoreDNSAddress(s: String): Boolean {
        return s.startsWith("https") ||
                s.startsWith("tcp") ||
                s.startsWith("udp") ||
                s.startsWith("tls") ||
                s.startsWith("quic") ||
                s == "localhost"
    }

    fun isValidUrl(value: String?): Boolean {
        if (value.isNullOrEmpty()) return false

        return try {
            Patterns.WEB_URL.matcher(value).matches() ||
                    Patterns.DOMAIN_NAME.matcher(value).matches() ||
                    URLUtil.isValidUrl(value)
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to validate URL", e)
            false
        }
    }

    fun openUri(context: Context, uriString: String) {
        try {
            val uri = uriString.toUri()
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to open URI", e)
        }
    }

    fun getUuid(): String {
        return try {
            UUID.randomUUID().toString().replace("-", "")
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to generate UUID", e)
            ""
        }
    }

    fun decodeURIComponent(url: String): String {
        return try {
            // Decode strictly according to RFC 3986 / encodeURIComponent semantics.
            // '+' is a literal plus and MUST NOT be interpreted as space.
            // Inputs using '+' for spaces are non-conforming and rejected deliberately
            // to avoid cross-language interoperability issues.
            URLDecoder.decode(url.replace("+", "%2B"), Charsets.UTF_8.toString())
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to decode encodeURIComponent", e)
            url
        }
    }

    fun encodeURIComponent(url: String): String {
        return try {
            // Replace '+' with '%20' to conform to encodeURIComponent semantics.
            URLEncoder.encode(url, Charsets.UTF_8.toString()).replace("+", "%20")
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to encode encodeURIComponent", e)
            url
        }
    }

    fun readTextFromAssets(context: Context?, fileName: String): String {
        if (context == null) return ""

        return try {
            context.assets.open(fileName).use { inputStream ->
                inputStream.bufferedReader().use { reader ->
                    reader.readText()
                }
            }
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to read asset file: $fileName", e)
            ""
        }
    }

    fun userAssetPath(context: Context?): String {
        if (context == null) return ""

        return try {
            context.getDir(AppConfig.DIR_ASSETS, Context.MODE_PRIVATE).absolutePath
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to get user asset path", e)
            ""
        }
    }

    fun getDeviceIdForXUDPBaseKey(): String {
        return try {
            val androidId = Settings.Secure.ANDROID_ID.toByteArray(Charsets.UTF_8)
            Base64.encodeToString(androidId.copyOf(32), Base64.NO_PADDING.or(Base64.URL_SAFE))
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to generate device ID", e)
            ""
        }
    }

    fun getIpv6Address(address: String?): String {
        if (address.isNullOrEmpty()) return ""

        return if (isIpv6Address(address) && !address.contains('[') && !address.contains(']')) {
            "[$address]"
        } else {
            address
        }
    }

    fun getSysLocale(): Locale = LocaleList.getDefault().get(0) ?: Locale.getDefault()

    fun fixIllegalUrl(str: String): String {
        return str.replace(" ", "%20")
            .replace("|", "%7C")
    }

    fun findRandomFreePort(): Int {
        return ServerSocket(0).use { it.localPort }
    }

    fun isValidSubUrl(value: String?): Boolean {
        if (value.isNullOrEmpty()) return false

        try {
            if (URLUtil.isHttpsUrl(value)) return true
            if (URLUtil.isHttpUrl(value)) {
                if (value.contains(LOOPBACK)) return true

                //Check private ip address
                val uri = URI(fixIllegalUrl(value))
                if (isIpAddress(uri.host)) {
                    AppConfig.PRIVATE_IP_LIST.forEach {
                        if (isIpInCidr(uri.host, it)) return true
                    }
                }
            }
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to validate subscription URL", e)
        }
        return false
    }

    fun receiverFlags(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.RECEIVER_EXPORTED
    } else {
        ContextCompat.RECEIVER_NOT_EXPORTED
    }

    fun isXray(): Boolean = BuildConfig.APPLICATION_ID.startsWith("com.cr.tunnel")

    private fun inetAddressToLong(ip: InetAddress): Long {
        val bytes = ip.address
        var result: Long = 0
        for (i in bytes.indices) {
            result = result shl 8 or (bytes[i].toInt() and 0xff).toLong()
        }
        return result
    }

    fun isIpInCidr(ip: String, cidr: String): Boolean {
        try {
            if (!isIpAddress(ip)) return false

            // Parse CIDR (e.g., "192.168.1.0/24")
            val (cidrIp, prefixLen) = cidr.split("/")
            val prefixLength = prefixLen.toInt()

            // Convert IP and CIDR's IP portion to Long
            val ipLong = inetAddressToLong(InetAddress.getByName(ip))
            val cidrIpLong = inetAddressToLong(InetAddress.getByName(cidrIp))

            // Calculate subnet mask (e.g., /24 → 0xFFFFFF00)
            val mask = if (prefixLength == 0) 0L else (-1L shl (32 - prefixLength))

            // Check if they're in the same subnet
            return (ipLong and mask) == (cidrIpLong and mask)
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to check if IP is in CIDR", e)
            return false
        }
    }

    fun formatTimestamp(ts: Long?, pattern: String = "yyyy-MM-dd HH:mm", locale: Locale = Locale.getDefault()): String {
        if (ts == null || ts <= 0L) return ""
        return try {
            val sdf = SimpleDateFormat(pattern, locale)
            sdf.format(Date(ts))
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to format timestamp", e)
            ""
        }
    }
}
