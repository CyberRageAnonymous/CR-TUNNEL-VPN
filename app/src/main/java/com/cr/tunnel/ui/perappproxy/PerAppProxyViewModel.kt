package com.cr.tunnel.ui.perappproxy

import android.app.Application
import android.content.Context
import com.cr.tunnel.AppConfig
import com.cr.tunnel.dto.AppInfo
import com.cr.tunnel.dto.UrlContentRequest
import com.cr.tunnel.handler.MmkvManager
import com.cr.tunnel.handler.SettingsChangeManager
import com.cr.tunnel.handler.SettingsManager
import com.cr.tunnel.ui.AppSelection
import com.cr.tunnel.ui.base.BaseViewModel
import com.cr.tunnel.util.AppManagerUtil
import com.cr.tunnel.util.HttpUtil
import com.cr.tunnel.util.LogUtil
import com.cr.tunnel.util.Utils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.text.Collator

enum class PerAppMode { DEFAULT, PROXY, DIRECT }

class PerAppProxyViewModel(application: Application) : BaseViewModel(application) {

    private val _proxySet = MutableStateFlow(loadProxySet())
    val proxySet: StateFlow<Set<String>> = _proxySet.asStateFlow()

    private val _directSet = MutableStateFlow(loadDirectSet())
    val directSet: StateFlow<Set<String>> = _directSet.asStateFlow()

    private val _favorites = MutableStateFlow(loadFavorites())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    private val _displayedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val displayedApps: StateFlow<List<AppInfo>> = _displayedApps.asStateFlow()

    private val _perAppProxyEnabled = MutableStateFlow(
        MmkvManager.decodeSettingsBool(AppConfig.PREF_PER_APP_PROXY, false)
    )
    val perAppProxyEnabled: StateFlow<Boolean> = _perAppProxyEnabled.asStateFlow()

    private val _bypassApps = MutableStateFlow(
        MmkvManager.decodeSettingsBool(AppConfig.PREF_BYPASS_APPS, false)
    )
    val bypassApps: StateFlow<Boolean> = _bypassApps.asStateFlow()

    private val _favoritesOnly = MutableStateFlow(false)
    val favoritesOnly: StateFlow<Boolean> = _favoritesOnly.asStateFlow()

    private var appsAll: List<AppInfo>? = null
    private var currentQuery = ""
    private var isAppListLoading = false

    init {
        ensureModesMigrated()
    }

    fun modeOf(packageName: String): PerAppMode {
        return when {
            packageName in _directSet.value -> PerAppMode.DIRECT
            packageName in _proxySet.value -> PerAppMode.PROXY
            else -> PerAppMode.DEFAULT
        }
    }

    fun setAppMode(packageName: String, mode: PerAppMode) {
        val newProxy = _proxySet.value.toMutableSet()
        val newDirect = _directSet.value.toMutableSet()
        when (mode) {
            PerAppMode.PROXY -> {
                newProxy.add(packageName)
                newDirect.remove(packageName)
            }

            PerAppMode.DIRECT -> {
                newDirect.add(packageName)
                newProxy.remove(packageName)
            }

            PerAppMode.DEFAULT -> {
                newProxy.remove(packageName)
                newDirect.remove(packageName)
            }
        }
        replaceSelection(newProxy, newDirect)
        SettingsChangeManager.makeRestartService()
    }

    fun setFavorite(packageName: String, favorite: Boolean) {
        val newFavorites = _favorites.value.toMutableSet()
        if (favorite) {
            newFavorites.add(packageName)
        } else {
            newFavorites.remove(packageName)
        }
        _favorites.value = newFavorites
        MmkvManager.encodeSettings(AppConfig.PREF_PER_APP_PROXY_FAVORITE, newFavorites)
        applyCurrentFilter()
    }

    fun setFavoritesOnly(favoritesOnly: Boolean) {
        if (_favoritesOnly.value != favoritesOnly) {
            _favoritesOnly.value = favoritesOnly
            applyCurrentFilter()
        }
    }

    private fun loadProxySet(): Set<String> {
        return MmkvManager.decodeSettingsStringSet(AppConfig.PREF_PER_APP_PROXY_SET)?.toSet() ?: emptySet()
    }

    private fun loadDirectSet(): Set<String> {
        return MmkvManager.decodeSettingsStringSet(AppConfig.PREF_PER_APP_PROXY_DIRECT)?.toSet() ?: emptySet()
    }

    private fun loadFavorites(): Set<String> {
        return MmkvManager.decodeSettingsStringSet(AppConfig.PREF_PER_APP_PROXY_FAVORITE)?.toSet() ?: emptySet()
    }

    private fun replaceSelection(newProxy: Set<String>, newDirect: Set<String>) {
        if (newProxy != _proxySet.value || newDirect != _directSet.value) {
            _proxySet.value = newProxy
            _directSet.value = newDirect
            MmkvManager.encodeSettings(AppConfig.PREF_PER_APP_PROXY_SET, newProxy.toMutableSet())
            MmkvManager.encodeSettings(AppConfig.PREF_PER_APP_PROXY_DIRECT, newDirect.toMutableSet())
            applyCurrentFilter()
        }
    }

    private fun ensureModesMigrated() {
        if (MmkvManager.decodeSettingsBool(AppConfig.PREF_PER_APP_PROXY_MODES_MIGRATED, false)) return

        val legacy = loadProxySet()
        val bypassApps = MmkvManager.decodeSettingsBool(AppConfig.PREF_BYPASS_APPS, false)
        if (bypassApps && legacy.isNotEmpty()) {
            val mergedDirect = (loadDirectSet() + legacy).toMutableSet()
            MmkvManager.encodeSettings(AppConfig.PREF_PER_APP_PROXY_DIRECT, mergedDirect)
            MmkvManager.encodeSettings(AppConfig.PREF_PER_APP_PROXY_SET, mutableSetOf())
            _proxySet.value = emptySet()
            _directSet.value = mergedDirect
        }
        MmkvManager.encodeSettings(AppConfig.PREF_PER_APP_PROXY_MODES_MIGRATED, true)
    }

    fun setPerAppProxyEnabled(enabled: Boolean) {
        if (_perAppProxyEnabled.value != enabled) {
            _perAppProxyEnabled.value = enabled
            MmkvManager.encodeSettings(AppConfig.PREF_PER_APP_PROXY, enabled)
        }
    }

    fun setBypassAppsEnabled(enabled: Boolean) {
        if (_bypassApps.value != enabled) {
            _bypassApps.value = enabled
            MmkvManager.encodeSettings(AppConfig.PREF_BYPASS_APPS, enabled)
            SettingsChangeManager.makeRestartService()
        }
    }

    fun loadApps(context: Context) {
        if (appsAll != null || isAppListLoading) return

        val applicationContext = context.applicationContext
        isAppListLoading = true
        launchLoading {
            try {
                val apps = withContext(Dispatchers.IO) {
                    val list = AppManagerUtil.loadNetworkAppList(applicationContext)
                    sortApps(list)
                }
                appsAll = apps
                applyCurrentFilter()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                LogUtil.e(AppConfig.ANG_PACKAGE, "Error loading apps", e)
            } finally {
                isAppListLoading = false
            }
        }
    }

    fun filterApps(query: String) {
        currentQuery = query
        applyCurrentFilter()
    }

    private fun applyCurrentFilter() {
        val apps = appsAll ?: return
        _displayedApps.value = filterList(apps)
    }

    private fun filterList(apps: List<AppInfo>): List<AppInfo> {
        val filterByQuery = currentQuery.isNotEmpty()
        val favorites = _favorites.value
        return apps.filter { app ->
            val matchesQuery = !filterByQuery ||
                app.appName.contains(currentQuery, ignoreCase = true) ||
                app.packageName.contains(currentQuery, ignoreCase = true)
            val matchesFavorites = !_favoritesOnly.value || app.packageName in favorites
            matchesQuery && matchesFavorites
        }
    }

    private fun sortApps(apps: List<AppInfo>): List<AppInfo> {
        val collator = Collator.getInstance()
        val favorites = _favorites.value
        val proxySet = _proxySet.value
        val directSet = _directSet.value
        return apps.sortedWith { p1, p2 ->
            val favorite1 = p1.packageName in favorites
            val favorite2 = p2.packageName in favorites
            val marked1 = p1.packageName in proxySet || p1.packageName in directSet
            val marked2 = p2.packageName in proxySet || p2.packageName in directSet
            when {
                favorite1 && !favorite2 -> -1
                !favorite1 && favorite2 -> 1
                marked1 && !marked2 -> -1
                !marked1 && marked2 -> 1
                p1.isSystemApp && !p2.isSystemApp -> 1
                !p1.isSystemApp && p2.isSystemApp -> -1
                else -> collator.compare(p1.appName, p2.appName)
            }
        }
    }

    fun selectAll() {
        val displayedApps = _displayedApps.value
        val currentSelection = _proxySet.value
        val allSelected = displayedApps.all { it.packageName in currentSelection }
        val newProxy = _proxySet.value.toMutableSet()
        displayedApps.forEach { app ->
            if (allSelected) {
                newProxy.remove(app.packageName)
            } else {
                newProxy.add(app.packageName)
            }
        }
        replaceSelection(newProxy, _directSet.value)
        enablePerAppProxyAndRestart()
    }

    fun invertSelection() {
        val packageNames = _displayedApps.value.map { it.packageName }
        val newProxy = _proxySet.value.toMutableSet()
        packageNames.forEach { packageName ->
            if (!newProxy.remove(packageName)) {
                newProxy.add(packageName)
            }
        }
        replaceSelection(newProxy, _directSet.value)
        enablePerAppProxyAndRestart()
    }

    fun selectProxyAppAuto(context: Context) {
        val applicationContext = context.applicationContext
        launchLoading {
            val url = AppConfig.ANDROID_PACKAGE_NAME_LIST_URL
            var content = withContext(Dispatchers.IO) {
                HttpUtil.getUrlContent(
                    UrlContentRequest(
                        url = url,
                        timeout = 5000
                    )
                )
            }
            if (content.isNullOrEmpty()) {
                val proxyUsername = SettingsManager.getSocksUsername()
                val proxyPassword = SettingsManager.getSocksPassword()
                val httpPort = SettingsManager.getHttpPort()
                content = withContext(Dispatchers.IO) {
                    HttpUtil.getUrlContent(
                        UrlContentRequest(
                            url = url,
                            timeout = 5000,
                            httpPort = httpPort,
                            proxyUsername = proxyUsername,
                            proxyPassword = proxyPassword
                        )
                    )
                } ?: ""
            }
            val success = applyProxyAppList(
                content = content,
                context = applicationContext,
                forceGoogleApps = true
            )
            if (success) {
                enablePerAppProxyAndRestart()
            }
        }
    }

    fun importProxyApp(content: String?, context: Context) {
        if (content.isNullOrEmpty()) return

        val applicationContext = context.applicationContext
        launchLoading {
            val success = applyProxyAppList(
                content = content,
                context = applicationContext,
                forceGoogleApps = false
            )
            if (success) {
                enablePerAppProxyAndRestart()
            }
        }
    }

    fun exportProxyApp(): String {
        return buildString {
            append(_bypassApps.value)
            _proxySet.value.forEach { packageName ->
                append(System.lineSeparator())
                append(packageName)
            }
        }
    }

    private suspend fun applyProxyAppList(content: String, context: Context, forceGoogleApps: Boolean): Boolean {
        val installedApps = appsAll ?: return false

        try {
            val proxyAppList = if (content.isEmpty()) {
                withContext(Dispatchers.IO) {
                    Utils.readTextFromAssets(context, "proxy_package_name")
                }
            } else content
            if (proxyAppList.isNullOrEmpty()) return false

            val bypassApps = _bypassApps.value
            val newProxy = withContext(Dispatchers.Default) {
                AppSelection.fromProxyList(
                    packageNames = installedApps.map { it.packageName },
                    proxyAppList = proxyAppList,
                    bypassApps = bypassApps,
                    forceGoogleApps = forceGoogleApps
                )
            }
            replaceSelection(newProxy, _directSet.value)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Error selecting proxy app", e)
            return false
        }
        return true
    }

    private fun enablePerAppProxyAndRestart() {
        setPerAppProxyEnabled(true)
        SettingsChangeManager.makeRestartService()
    }
}