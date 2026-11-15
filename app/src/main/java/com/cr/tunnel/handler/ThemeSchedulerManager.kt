package com.cr.tunnel.handler

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.ExistingPeriodicWorkPolicy
import java.util.concurrent.TimeUnit
import com.cr.tunnel.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ThemeSchedulerManager {

    private const val DEFAULT_START_HOUR = 20 // 8 PM
    private const val DEFAULT_END_HOUR = 7 // 7 AM

    fun isSchedulerEnabled(context: Context): Boolean {
        return MmkvManager.decodeSettingsBool(AppConfig.PREF_THEME_SCHEDULER_ENABLED, false)
    }

    fun setSchedulerEnabled(context: Context, enabled: Boolean) {
        MmkvManager.encodeSettings(AppConfig.PREF_THEME_SCHEDULER_ENABLED, enabled)
        if (enabled) {
            scheduleThemeCheck(context)
        } else {
            cancelThemeCheck(context)
        }
    }

    fun getStartHour(context: Context): Int {
        return MmkvManager.decodeSettingsString(AppConfig.PREF_THEME_SCHEDULER_START, DEFAULT_START_HOUR.toString())?.toIntOrNull() ?: DEFAULT_START_HOUR
    }

    fun setStartHour(context: Context, hour: Int) {
        MmkvManager.encodeSettings(AppConfig.PREF_THEME_SCHEDULER_START, hour.toString())
        if (isSchedulerEnabled(context)) {
            scheduleThemeCheck(context)
        }
    }

    fun getEndHour(context: Context): Int {
        return MmkvManager.decodeSettingsString(AppConfig.PREF_THEME_SCHEDULER_END, DEFAULT_END_HOUR.toString())?.toIntOrNull() ?: DEFAULT_END_HOUR
    }

    fun setEndHour(context: Context, hour: Int) {
        MmkvManager.encodeSettings(AppConfig.PREF_THEME_SCHEDULER_END, hour.toString())
        if (isSchedulerEnabled(context)) {
            scheduleThemeCheck(context)
        }
    }

    fun shouldUseDarkTheme(context: Context): Boolean {
        if (!isSchedulerEnabled(context)) {
            return false // Let system/user preference decide
        }

        val startHour = getStartHour(context)
        val endHour = getEndHour(context)
        val calendar = java.util.Calendar.getInstance()
        val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)

        return if (startHour > endHour) {
            // Overnight schedule (e.g., 20:00 - 07:00)
            currentHour >= startHour || currentHour < endHour
        } else {
            // Same day schedule (e.g., 07:00 - 20:00)
            currentHour >= startHour && currentHour < endHour
        }
    }

    private fun scheduleThemeCheck(context: Context) {
        // Use WorkManager to periodically check and apply theme
        val workRequest = PeriodicWorkRequestBuilder<ThemeCheckWorker>(1, TimeUnit.HOURS)
            .setInitialDelay(0, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "ThemeSchedulerCheck",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun cancelThemeCheck(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork("ThemeSchedulerCheck")
    }

    fun applyThemeIfNeeded(context: Context) {
        if (isSchedulerEnabled(context)) {
            val shouldBeDark = shouldUseDarkTheme(context)
            val currentMode = MmkvManager.decodeSettingsString(AppConfig.PREF_UI_MODE_NIGHT, "0") ?: "0"
            val targetMode = if (shouldBeDark) "2" else "1"

            if (currentMode != targetMode) {
                MmkvManager.encodeSettings(AppConfig.PREF_UI_MODE_NIGHT, targetMode)
            }
        }
    }
}

class ThemeCheckWorker(
    context: Context,
    params: androidx.work.WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): androidx.work.Result {
        withContext(Dispatchers.IO) {
            ThemeSchedulerManager.applyThemeIfNeeded(applicationContext)
        }
        return androidx.work.Result.success()
    }
}
