package com.cr.tunnel.fmt

import com.cr.tunnel.AppConfig
import com.cr.tunnel.dto.V2rayNShareItem
import com.cr.tunnel.dto.entities.ProfileItem
import com.cr.tunnel.util.JsonUtil
import com.cr.tunnel.util.LogUtil
import com.cr.tunnel.util.Utils

object V2rayNFmt : FmtBase() {
    fun parse(str: String): ProfileItem? {
        try {
            val jsonBase64Payload = str.substringAfterLast('/')
            val jsonPayload = Utils.decode(jsonBase64Payload)
            val v2rayNShareItem = JsonUtil.fromJson(jsonPayload, V2rayNShareItem::class.java)
            return v2rayNShareItem?.toProfileItem()
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to parse V2rayN format", e)
        }
        return null
    }
}