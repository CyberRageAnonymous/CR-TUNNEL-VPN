package com.cr.tunnel.handler

import android.util.Base64
import com.cr.tunnel.AppConfig
import com.cr.tunnel.util.LogUtil
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.URLEncoder
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object NetModVault {

    private const val KEY = "_netsyna_netmod_"
    private val NM_LINK = Regex("(?i)nm-[a-z]+://[^\\s\"'<>\\[\\]{},;)]+")
    private val NM_PREFIX = Regex("(?i)^nm-([a-z]+)://")
    private val WS = Regex("\\s+")

    fun isNmContent(content: String?): Boolean {
        if (content.isNullOrBlank()) return false
        return NM_PREFIX.containsMatchIn(content.trim())
    }

    fun decryptPayload(payload: String): String? {
        return try {
            val cleaned = payload.replace(WS, "")
            val bytes = decodeBase64(cleaned) ?: return null
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(KEY.toByteArray(Charsets.UTF_8), "AES")
            )
            String(cipher.doFinal(bytes), Charsets.UTF_8)
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to decrypt netmod payload", e)
            null
        }
    }

    fun toStandardLinks(content: String): List<String> {
        val links = mutableListOf<String>()
        NM_LINK.findAll(content).forEach { match ->
            val link = convertLink(match.value) ?: return@forEach
            links.add(link)
        }
        return links
    }

    fun decryptBlobLink(content: String): String? {
        val trimmed = content.trim()
        if (trimmed.isEmpty() || NM_PREFIX.containsMatchIn(trimmed)) return null
        if (trimmed.length < 40 || trimmed.length > 400_000) return null
        val plain = decryptPayload(trimmed)?.trim().orEmpty()
        if (plain.isEmpty()) return null
        if (plain.contains("://")) return plain
        if (plain.startsWith("{")) return buildFromJson("vmess", plain) ?: botLink("vmess", plain)
        return botLink("vmess", plain)
    }

    private fun convertLink(link: String): String? {
        val scheme = NM_PREFIX.find(link)?.groupValues?.get(1)?.lowercase(Locale.ROOT)
            ?: return null
        val payload = link.substring(link.indexOf("://") + 3)
        val plain = decryptPayload(payload)?.trim().orEmpty()
        if (plain.isEmpty()) return null
        if (plain.contains("://")) return plain
        if (plain.startsWith("{")) return buildFromJson(scheme, plain) ?: botLink(scheme, plain)
        return botLink(scheme, plain)
    }

    private fun botLink(scheme: String, plain: String): String = "$scheme://${encodeBase64(plain)}"

    private fun buildFromJson(scheme: String, plain: String): String? {
        val json = runCatching { JsonParser.parseString(plain).asJsonObject }.getOrNull()
            ?: return null
        if (scheme == "vmess") return botLink(scheme, plain)

        val server = json.first("add", "address", "server", "host") ?: return null
        val port = json.first("port") ?: return null
        val uuid = json.first("id", "uuid", "password", "pass") ?: return null
        val remarks = json.first("ps", "remarks", "name", "remark").orEmpty().ifBlank { "NetMod" }
        val network = json.first("net", "network", "transport").orEmpty()
        val security = json.first("tls", "security").orEmpty()
        val sni = json.first("sni", "peer").orEmpty()
        val host = json.first("host", "sniHost").orEmpty()
        val path = json.first("path").orEmpty()
        val fingerprint = json.first("fp", "fingerprint").orEmpty()
        val alpn = json.first("alpn").orEmpty()
        val flow = json.first("flow").orEmpty()
        val method = json.first("security", "method", "cipher", "scy").orEmpty()

        return when (scheme) {
            "vless", "trojan" -> {
                val query = buildString {
                    if (scheme == "vless") append("encryption=none")
                    if (flow.isNotBlank()) appendQuery("flow", flow)
                    if (security.isNotBlank()) {
                        appendQuery("security", if (security == "tls") "tls" else security)
                    }
                    if (sni.isNotBlank()) appendQuery("sni", sni)
                    if (fingerprint.isNotBlank()) appendQuery("fp", fingerprint)
                    if (alpn.isNotBlank()) appendQuery("alpn", alpn)
                    if (network.isNotBlank()) appendQuery("type", network)
                    if (host.isNotBlank()) appendQuery("host", host)
                    if (path.isNotBlank()) appendQuery("path", path)
                }
                "$scheme://$uuid@$server:$port?$query#${encodeComponent(remarks)}"
            }

            "ss" -> {
                val userInfo = if (method.isNotBlank()) "$method:$uuid" else uuid
                val encoded = encodeBase64(userInfo).trimEnd('=')
                "$scheme://$encoded@$server:$port#${encodeComponent(remarks)}"
            }

            "hy2", "hysteria2" -> {
                val query = buildString {
                    appendQuery("sni", sni.ifBlank { host })
                    appendQuery("insecure", json.first("allowInsecure", "insecure") ?: "1")
                }
                "$scheme://$uuid@$server:$port?$query#${encodeComponent(remarks)}"
            }

            else -> null
        }
    }

    private fun StringBuilder.appendQuery(key: String, value: String) {
        if (value.isBlank()) return
        if (isNotEmpty()) append('&')
        append(key).append('=').append(encodeComponent(value))
    }

    private fun JsonObject.first(vararg keys: String): String? {
        for (key in keys) {
            if (!has(key)) continue
            val element = get(key)
            if (element == null || element.isJsonNull) continue
            val text = runCatching { element.asString }.getOrNull()
            if (!text.isNullOrBlank()) return text
        }
        return null
    }

    private fun encodeComponent(value: String): String =
        URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    private fun encodeBase64(text: String): String =
        Base64.encodeToString(text.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

    private fun decodeBase64(text: String): ByteArray? {
        return runCatching { Base64.decode(text, Base64.NO_WRAP) }.getOrNull()
            ?: runCatching { Base64.decode(text, Base64.NO_WRAP or Base64.URL_SAFE) }.getOrNull()
            ?: runCatching {
                Base64.decode(text + "=".repeat((4 - text.length % 4) % 4), Base64.NO_WRAP)
            }.getOrNull()
    }
}
