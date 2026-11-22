package com.cr.tunnel.handler

import android.content.Context
import com.cr.tunnel.handler.MmkvManager

object AdBlockManager {

    private val DEFAULT_AD_DOMAINS = setOf(
        "doubleclick.net",
        "adservice.google.com",
        "googleadservices.com",
        "googlesyndication.com",
        "google-analytics.com",
        "analytics.google.com",
        "adsrvr.org",
        "adnxs.com",
        "rubiconproject.com",
        "criteo.com",
        "creativecdn.com",
        "adcolony.com",
        "adjust.com",
        "appsflyer.com",
        "ironpla.net",
        "admixer.net",
        "adtrue.com",
        "adkmob.com",
        "mediaplex.com",
        "advertising.com",
        "revenuehits.com",
        "popads.net",
        "popcash.net"
    )

    private val blockedHosts = mutableSetOf<String>()

    fun isEnabled(context: Context): Boolean {
        return MmkvManager.decodeSettingsBool("adblock_enabled", false)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        MmkvManager.encodeSettings("adblock_enabled", enabled)
        rebuildHosts(context)
    }

    fun getCustomList(context: Context): List<String> {
        val raw = MmkvManager.decodeSettingsString("adblock_custom_list", "") ?: ""
        return raw.lines().map { it.trim().lowercase() }.filter { it.isNotEmpty() }
    }

    fun setCustomList(context: Context, list: List<String>) {
        MmkvManager.encodeSettings("adblock_custom_list", list.joinToString("\n"))
        rebuildHosts(context)
    }

    fun isAdBlocked(host: String): Boolean {
        val h = host.trim().lowercase()
        if (h.isEmpty()) return false
        return blockedHosts.any { b -> h == b || h.endsWith(".$b") }
    }

    private fun rebuildHosts(context: Context) {
        blockedHosts.clear()
        if (!isEnabled(context)) return
        blockedHosts.addAll(DEFAULT_AD_DOMAINS)
        blockedHosts.addAll(getCustomList(context))
    }
}
