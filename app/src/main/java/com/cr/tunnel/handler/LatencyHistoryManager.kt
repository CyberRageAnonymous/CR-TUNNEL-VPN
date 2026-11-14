package com.cr.tunnel.handler

import com.cr.tunnel.AppConfig
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

object LatencyHistoryManager {

    private const val MAX_HISTORY_POINTS = 168 // 7 days * 24 hours
    private const val MAX_HISTORY_DURATION_MS = 7 * 24 * 60 * 60 * 1000L

    private val gson = Gson()
    private val typeToken: Type = object : TypeToken<List<LatencyPoint>>() {}.type

    data class LatencyPoint(
        val timestamp: Long,
        val latencyMs: Int,
        val serverGuid: String
    )

    fun addLatencyPoint(serverGuid: String, latencyMs: Int) {
        if (latencyMs <= 0) return
        
        val key = "${AppConfig.PREF_LATENCY_HISTORY}_$serverGuid"
        val history = getHistoryInternal(key)
        
        val now = System.currentTimeMillis()
        val newPoint = LatencyPoint(now, latencyMs, serverGuid)
        
        // Add new point and clean old ones
        val updated = (history + newPoint)
            .filter { now - it.timestamp < MAX_HISTORY_DURATION_MS }
            .takeLast(MAX_HISTORY_POINTS)
        
        MmkvManager.encodeSettings(key, gson.toJson(updated))
    }

    fun getHistory(serverGuid: String): List<LatencyPoint> {
        val key = "${AppConfig.PREF_LATENCY_HISTORY}_$serverGuid"
        return getHistoryInternal(key)
    }

    private fun getHistoryInternal(key: String): List<LatencyPoint> {
        val json = MmkvManager.decodeSettingsString(key, "") ?: return emptyList()
        return try {
            gson.fromJson(json, typeToken) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clearHistory(serverGuid: String) {
        val key = "${AppConfig.PREF_LATENCY_HISTORY}_$serverGuid"
        MmkvManager.encodeSettings(key, "")
    }

    fun clearAllHistory() {
        // This would need to iterate all keys, for now just clear via prefix if needed
    }

    fun getHistoryForPeriod(serverGuid: String, periodHours: Int): List<LatencyPoint> {
        val history = getHistory(serverGuid)
        val cutoff = System.currentTimeMillis() - periodHours * 60 * 60 * 1000L
        return history.filter { it.timestamp >= cutoff }
    }
}
