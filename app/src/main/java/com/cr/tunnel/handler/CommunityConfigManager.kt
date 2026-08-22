package com.cr.tunnel.handler

import android.util.Base64
import com.cr.tunnel.AppConfig
import com.cr.tunnel.BuildConfig
import com.cr.tunnel.dto.CommunityConfigItem
import com.cr.tunnel.dto.GitHubContentResponse
import com.cr.tunnel.dto.UrlContentRequest
import com.cr.tunnel.util.HttpUtil
import com.cr.tunnel.util.JsonUtil
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID
import java.util.concurrent.TimeUnit

object CommunityConfigManager {

    private const val MAX_ENTRIES = 200
    private const val MAX_LINK_LENGTH = 8000

    fun isSharingEnabled(): Boolean = AppConfig.COMMUNITY_TOKEN.isNotBlank()

    fun fetchConfigs(): List<CommunityConfigItem> {
        val body = HttpUtil.getUrlContent(
            UrlContentRequest(
                url = AppConfig.COMMUNITY_RAW_URL,
                timeout = 10000,
                userAgent = "CR-TUNNEL-VPN/${BuildConfig.VERSION_NAME}"
            )
        ) ?: return emptyList()
        return try {
            JsonUtil.fromJsonSafe(body, Array<CommunityConfigItem>::class.java)
                ?.toList()
                .orEmpty()
                .filter { it.link.isNotBlank() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addConfig(
        link: String,
        volume: String,
        duration: String,
        users: String,
        name: String
    ) {
        val token = AppConfig.COMMUNITY_TOKEN
        require(token.isNotBlank()) { "Sharing token not configured" }
        require(link.contains("://")) { "Invalid config link" }
        require(link.length <= MAX_LINK_LENGTH) { "Link too long" }

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

        val builder = Request.Builder().url(AppConfig.COMMUNITY_API_URL)
        headers.forEach { (k, v) -> builder.header(k, v) }

        var sha: String? = null
        var existingJson = "[]"

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

        val entry = CommunityConfigItem(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { "Config" },
            link = link.trim(),
            volume = volume.trim(),
            duration = duration.trim(),
            users = users.trim(),
            createdAt = System.currentTimeMillis()
        )

        val updated = (current + entry).takeLast(MAX_ENTRIES)
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

        client.newCall(putBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) {
                val err = response.body?.string().orEmpty().take(200)
                throw RuntimeException("Upload failed (code ${response.code}) $err")
            }
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
