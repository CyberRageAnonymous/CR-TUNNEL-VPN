package com.cr.tunnel.ui.dnssettings

import android.app.Application
import com.cr.tunnel.AppConfig
import com.cr.tunnel.handler.MmkvManager
import com.cr.tunnel.ui.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DnsHostEntry(
    val domain: String,
    val address: String
)

class DnsCustomViewModel(application: Application) : BaseViewModel(application) {

    private val _remoteServers = MutableStateFlow(loadServerPrefs(AppConfig.PREF_REMOTE_DNS, AppConfig.DNS_PROXY))
    val remoteServers: StateFlow<List<String>> = _remoteServers.asStateFlow()

    private val _domesticServers = MutableStateFlow(loadServerPrefs(AppConfig.PREF_DOMESTIC_DNS, AppConfig.DNS_DIRECT))
    val domesticServers: StateFlow<List<String>> = _domesticServers.asStateFlow()

    private val _dnsHosts = MutableStateFlow(loadHosts())
    val dnsHosts: StateFlow<List<DnsHostEntry>> = _dnsHosts.asStateFlow()

    private fun loadServerPrefs(key: String, default: String): List<String> {
        val raw = MmkvManager.decodeSettingsString(key) ?: return listOf(default)
        val values = raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        return values.ifEmpty { listOf(default) }
    }

    private fun loadHosts(): List<DnsHostEntry> {
        return MmkvManager.decodeSettingsString(AppConfig.PREF_DNS_HOSTS)
            ?.split(",").orEmpty()
            .map { it.trim() }
            .filter { it.isNotEmpty() && it.contains(":") }
            .map {
                val parts = it.split(":", limit = 2)
                DnsHostEntry(parts[0].trim(), parts[1].trim())
            }
    }

    fun addRemoteServer(address: String) {
        if (address in _remoteServers.value) return
        val updated = _remoteServers.value + address
        _remoteServers.value = updated
        MmkvManager.encodeSettings(AppConfig.PREF_REMOTE_DNS, updated.joinToString(","))
    }

    fun removeRemoteServer(index: Int) {
        if (index !in _remoteServers.value.indices) return
        val updated = _remoteServers.value.toMutableList().apply { removeAt(index) }
        _remoteServers.value = updated
        MmkvManager.encodeSettings(AppConfig.PREF_REMOTE_DNS, if (updated.isEmpty()) "" else updated.joinToString(","))
    }

    fun addDomesticServer(address: String) {
        if (address in _domesticServers.value) return
        val updated = _domesticServers.value + address
        _domesticServers.value = updated
        MmkvManager.encodeSettings(AppConfig.PREF_DOMESTIC_DNS, updated.joinToString(","))
    }

    fun removeDomesticServer(index: Int) {
        if (index !in _domesticServers.value.indices) return
        val updated = _domesticServers.value.toMutableList().apply { removeAt(index) }
        _domesticServers.value = updated
        MmkvManager.encodeSettings(AppConfig.PREF_DOMESTIC_DNS, if (updated.isEmpty()) "" else updated.joinToString(","))
    }

    fun addHost(domain: String, address: String) {
        if (_dnsHosts.value.any { it.domain == domain && it.address == address }) return
        val updated = _dnsHosts.value + DnsHostEntry(domain, address)
        _dnsHosts.value = updated
        MmkvManager.encodeSettings(AppConfig.PREF_DNS_HOSTS, updated.joinToString(",") { "${it.domain}:${it.address}" })
    }

    fun removeHost(index: Int) {
        if (index !in _dnsHosts.value.indices) return
        val updated = _dnsHosts.value.toMutableList().apply { removeAt(index) }
        _dnsHosts.value = updated
        MmkvManager.encodeSettings(AppConfig.PREF_DNS_HOSTS, if (updated.isEmpty()) "" else updated.joinToString(",") { "${it.domain}:${it.address}" })
    }
}