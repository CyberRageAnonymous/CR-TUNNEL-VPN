package com.cr.tunnel.handler

import com.cr.tunnel.AppConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Per-day and per-month traffic totals, accumulated in the service process
 * and read by the statistics screen.
 */
object StatsManager {

    private val dayFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val monthFormatter = SimpleDateFormat("yyyy-MM", Locale.US)

    data class TrafficSnapshot(
        val todayUp: Long,
        val todayDown: Long,
        val monthUp: Long,
        val monthDown: Long
    )

    fun accumulate(upDelta: Long, downDelta: Long) {
        if (upDelta <= 0L && downDelta <= 0L) return

        val today = dayFormatter.format(Date())
        if (MmkvManager.decodeSettingsString(AppConfig.PREF_STATS_TODAY_DATE) != today) {
            MmkvManager.encodeSettings(AppConfig.PREF_STATS_TODAY_UP, 0L)
            MmkvManager.encodeSettings(AppConfig.PREF_STATS_TODAY_DOWN, 0L)
            MmkvManager.encodeSettings(AppConfig.PREF_STATS_TODAY_DATE, today)
        }
        MmkvManager.encodeSettings(
            AppConfig.PREF_STATS_TODAY_UP,
            MmkvManager.decodeSettingsLong(AppConfig.PREF_STATS_TODAY_UP, 0L) + upDelta
        )
        MmkvManager.encodeSettings(
            AppConfig.PREF_STATS_TODAY_DOWN,
            MmkvManager.decodeSettingsLong(AppConfig.PREF_STATS_TODAY_DOWN, 0L) + downDelta
        )

        val month = monthFormatter.format(Date())
        if (MmkvManager.decodeSettingsString(AppConfig.PREF_STATS_MONTH_DATE) != month) {
            MmkvManager.encodeSettings(AppConfig.PREF_STATS_MONTH_UP, 0L)
            MmkvManager.encodeSettings(AppConfig.PREF_STATS_MONTH_DOWN, 0L)
            MmkvManager.encodeSettings(AppConfig.PREF_STATS_MONTH_DATE, month)
        }
        MmkvManager.encodeSettings(
            AppConfig.PREF_STATS_MONTH_UP,
            MmkvManager.decodeSettingsLong(AppConfig.PREF_STATS_MONTH_UP, 0L) + upDelta
        )
        MmkvManager.encodeSettings(
            AppConfig.PREF_STATS_MONTH_DOWN,
            MmkvManager.decodeSettingsLong(AppConfig.PREF_STATS_MONTH_DOWN, 0L) + downDelta
        )
    }

    fun snapshot(): TrafficSnapshot {
        val todayUp = MmkvManager.decodeSettingsLong(AppConfig.PREF_STATS_TODAY_UP, 0L)
        val todayDown = MmkvManager.decodeSettingsLong(AppConfig.PREF_STATS_TODAY_DOWN, 0L)
        val monthUp = MmkvManager.decodeSettingsLong(AppConfig.PREF_STATS_MONTH_UP, 0L)
        val monthDown = MmkvManager.decodeSettingsLong(AppConfig.PREF_STATS_MONTH_DOWN, 0L)
        return TrafficSnapshot(todayUp, todayDown, monthUp, monthDown)
    }

    fun reset() {
        MmkvManager.encodeSettings(AppConfig.PREF_STATS_TODAY_UP, 0L)
        MmkvManager.encodeSettings(AppConfig.PREF_STATS_TODAY_DOWN, 0L)
        MmkvManager.encodeSettings(AppConfig.PREF_STATS_MONTH_UP, 0L)
        MmkvManager.encodeSettings(AppConfig.PREF_STATS_MONTH_DOWN, 0L)
    }

    fun formatBytes(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = bytes / (1024.0 * 1024.0)
        val gb = bytes / (1024.0 * 1024.0 * 1024.0)
        return when {
            gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
            mb >= 1.0 -> String.format(Locale.US, "%.2f MB", mb)
            kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
            else -> "$bytes B"
        }
    }
}