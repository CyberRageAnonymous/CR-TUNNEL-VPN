package com.cr.tunnel.handler

import com.cr.tunnel.AppConfig
import java.util.concurrent.atomic.AtomicBoolean

object SettingsChangeManager {

    private val restartService = AtomicBoolean(false)
    private val setupGroupTab = AtomicBoolean(false)

    // Keys that affect only UI behavior and do not require core service restart.
    private val uiOnlyKeys = setOf(
        AppConfig.PREF_CONFIRM_REMOVE,
        AppConfig.PREF_DOUBLE_COLUMN_DISPLAY,
        AppConfig.PREF_GROUP_ALL_DISPLAY,
        AppConfig.PREF_LANGUAGE,
        AppConfig.PREF_UI_MODE_NIGHT,
        AppConfig.PREF_IS_BOOTED,
    )

    fun notifySettingChanged(key: String) {
        if (key !in uiOnlyKeys) {
            makeRestartService()
        }
        makeSetupGroupTab()
    }

    fun makeRestartService() {
        restartService.set(true)
    }

    fun consumeRestartService(): Boolean =
        restartService.compareAndSet(true, false)

    fun makeSetupGroupTab() {
        setupGroupTab.set(true)
    }

    fun consumeSetupGroupTab(): Boolean =
        setupGroupTab.compareAndSet(true, false)
}
