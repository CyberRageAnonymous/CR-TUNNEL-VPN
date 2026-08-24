package com.cr.tunnel.handler

import android.util.Base64
import com.cr.tunnel.AppConfig
import com.cr.tunnel.BuildConfig
import com.cr.tunnel.dto.CommunityConfigItem
import com.cr.tunnel.dto.GitHubContentResponse
import com.cr.tunnel.dto.UrlContentRequest
import com.cr.tunnel.util.HttpUtil
import com.cr.tunnel.util.JsonUtil
import com.cr.tunnel.util.Utils
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID
import java.util.concurrent.TimeUnit

object CommunityConfigManager {

    private const val MAX_ENTRIES = 200
    private const val MAX_LINK_LENGTH = 8000

    fun isSharingEnabled(): Boolean = Utils.decode(AppConfig.COMMUNITY_TOKEN.reversed()).isNotBlank()

    fun fetchConfigs(): List<CommunityConfigItem> {
        parseConfigList(fetchViaApi())?.let { return it }
        return parseConfigList(fetchViaRaw()).orEmpty()
    }

    private fun fetchViaApi(): String? {
        val token = Utils.decode(AppConfig.COMMUNITY_TOKEN.reversed())
        if (token.isBlank()) return null
        return try {
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()
            val request = Request.Builder()
                .url(AppConfig.COMMUNITY_API_URL)
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", "CR-TUNNEL-VPN")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val content = JsonUtil.fromJsonSafe(
                    response.body?.string().orEmpty(),
                    GitHubContentResponse::class.java
                ) ?: return null
                decodeContent(content.content)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun fetchViaRaw(): String? {
        val url = AppConfig.COMMUNITY_RAW_URL + "?t=" + System.currentTimeMillis()
        return HttpUtil.getUrlContent(
            UrlContentRequest(
                url = url,
                timeout = 10000,
                userAgent = "CR-TUNNEL-VPN/${BuildConfig.VERSION_NAME}"
            )
        )
    }

    private fun parseConfigList(body: String?): List<CommunityConfigItem>? {
        if (body.isNullOrBlank()) return null
        return try {
            JsonUtil.fromJsonSafe(body, Array<CommunityConfigItem>::class.java)
                ?.toList()
                .orEmpty()
                .filter { it.link.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }

    fun addConfig(
        link: String,
        volume: String,
        duration: String,
        users: String,
        name: String,
        ownerId: String
    ): CommunityConfigItem {
        require(ownerId.isNotBlank()) { "Device id missing" }
        require(link.contains("://")) { "Invalid config link" }
        require(link.length <= MAX_LINK_LENGTH) { "Link too long" }

        val entry = CommunityConfigItem(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { "Config" },
            link = link.trim(),
            volume = volume.trim(),
            duration = duration.trim(),
            users = users.trim(),
            createdAt = System.currentTimeMillis(),
            ownerId = ownerId
        )

        updateConfigs { current ->
            (current + entry).takeLast(MAX_ENTRIES)
        }
        return entry
    }

    fun removeConfig(id: String, ownerId: String) {
        require(id.isNotBlank() && ownerId.isNotBlank()) { "Invalid delete request" }
        updateConfigs { current ->
            val target = current.firstOrNull { it.id == id }
                ?: throw RuntimeException("Entry not found")
            if (target.ownerId != ownerId) {
                throw RuntimeException("Not your config")
            }
            current.filter { it.id != id }
        }
    }

    private fun updateConfigs(
        transform: (List<CommunityConfigItem>) -> List<CommunityConfigItem>
    ) {
        val token = Utils.decode(AppConfig.COMMUNITY_TOKEN.reversed())
        require(token.isNotBlank()) { "Sharing token not configured" }

        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

        val headers = mapOf(
            "Authorization" to "Bearer $token",
            "Accept" to "application/vnd.github+json",
            "X-GitHub-Api-Version" to "2022-11-28",
            "User-Agent" to "CR-TUNNEL-VPN"
        )

        var attempt = 0
        while (true) {
            var sha: String? = null
            var existingJson = "[]"

            val builder = Request.Builder().url(AppConfig.COMMUNITY_API_URL)
            headers.forEach { (k, v) -> builder.header(k, v) }

            client.newCall(builder.get().build()).execute().use { response ->
                if (response.isSuccessful) {
                    val content = JsonUtil.fromJsonSafe(
                        response.body?.string().orEmpty(),
                        GitHubContentResponse::class.java
                    )
                    sha = content?.sha
                    existingJson = decodeContent(content?.content)
                } else if (response.code != 404) {
                    throw RuntimeException("Failed to read community configs (code ${response.code})")
                }
            }

            val current = try {
                JsonUtil.fromJsonSafe(existingJson, Array<CommunityConfigItem>::class.java)?.toList().orEmpty()
            } catch (e: Exception) {
                emptyList()
            }

            val updated = transform(current)

            val newJson = JsonUtil.toJson(updated.toTypedArray())
            val encoded = Base64.encodeToString(newJson.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

            val payload = if (sha != null) {
                """{"message":"Community config update","branch":"${AppConfig.COMMUNITY_BRANCH}","content":"$encoded","sha":"$sha"}"""
            } else {
                """{"message":"Community config update","branch":"${AppConfig.COMMUNITY_BRANCH}","content":"$encoded"}"""
            }

            val putBuilder = Request.Builder().url(AppConfig.COMMUNITY_API_URL)
            headers.forEach { (k, v) -> putBuilder.header(k, v) }
            putBuilder.put(payload.toRequestBody("application/json".toMediaType()))

            var retry = false
            client.newCall(putBuilder.build()).execute().use { response ->
                when {
                    response.isSuccessful -> return
                    (response.code == 409 || response.code == 422) && attempt == 0 -> {
                        retry = true
                        attempt++
                    }
                    else -> {
                        val err = response.body?.string().orEmpty().take(200)
                        throw RuntimeException("Upload failed (code ${response.code}) $err")
                    }
                }
            }
            if (!retry) return
        }
    }

    private fun decodeContent(content: String?): String {
        if (content.isNullOrBlank()) return "[]"
        return try {
            val cleaned = content.replace("\n", "").replace("\r", "")
            String(Base64.decode(cleaned, Base64.DEFAULT), Charsets.UTF_8)
        } catch (e: Exception) {
            "[]"
        }
    }
}
