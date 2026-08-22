package com.cr.tunnel.dto

import com.google.gson.annotations.SerializedName

data class CommunityConfigItem(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String = "",
    @SerializedName("link") val link: String = "",
    @SerializedName("volume") val volume: String = "",
    @SerializedName("duration") val duration: String = "",
    @SerializedName("users") val users: String = "",
    @SerializedName("createdAt") val createdAt: Long = 0L
)

data class GitHubContentResponse(
    @SerializedName("sha") val sha: String? = null,
    @SerializedName("content") val content: String? = null
)
