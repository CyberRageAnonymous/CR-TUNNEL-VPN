package com.cr.tunnel.handler

import com.cr.tunnel.dto.entities.ProfileItem
import com.cr.tunnel.util.HttpUtil
import com.cr.tunnel.util.JsonUtil
import com.cr.tunnel.util.LogUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress

/**
 * Resolves the country code of a server from its address and caches it per GUID.
 */
object CountryResolver {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun getCountryCode(guid: String): String? {
        return MmkvManager.getServerCountry(guid)?.takeIf { it.isNotBlank() }
    }

    fun resolve(guid: String, profile: ProfileItem, onResolved: (String) -> Unit = {}) {
        val cached = getCountryCode(guid)
        if (cached != null) {
            onResolved(cached)
            return
        }
        val server = profile.server?.takeIf { it.isNotBlank() } ?: return
        scope.launch {
            val code = lookupCountry(server)
            if (code != null) {
                MmkvManager.setServerCountry(guid, code)
                withContext(Dispatchers.Main) { onResolved(code) }
            }
        }
    }

    private fun lookupCountry(server: String): String? {
        try {
            val host = server.trim()
                .removePrefix("http://").removePrefix("https://")
                .substringBefore('/').substringBefore(':')
            val ip = runCatching { InetAddress.getAllByName(host).firstOrNull()?.hostAddress }
                .getOrNull() ?: host
            val content = HttpUtil.getUrlContent(
                com.cr.tunnel.dto.UrlContentRequest(
                    url = "https://api.ip.sb/geoip/$ip",
                    timeout = 5000
                )
            ) ?: return null
            val json = JsonUtil.parseString(content)
            val code = json?.get("country_code")?.asString
                ?: json?.get("country")?.asString
                ?: json?.get("countryCode")?.asString
            return code?.takeIf { it.isNotBlank() }?.uppercase()
        } catch (e: Exception) {
            LogUtil.e("CountryResolver", "lookup failed for $server", e)
            return null
        }
    }

    fun flagEmoji(countryCode: String?): String {
        if (countryCode.isNullOrBlank()) return ""
        val code = countryCode.uppercase()
        if (code.length == 2 && code.all { it in 'A'..'Z' }) {
            val base = 0x1F1E6
            val sb = StringBuilder()
            for (c in code) sb.append((base + (c.code - 'A'.code)).toChar())
            return sb.toString()
        }
        return ""
    }
}