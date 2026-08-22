package com.cr.tunnel.ui.community

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.cr.tunnel.AppConfig
import com.cr.tunnel.R
import com.cr.tunnel.dto.CommunityConfigItem
import com.cr.tunnel.dto.TestServiceMessage
import com.cr.tunnel.extension.toastError
import com.cr.tunnel.handler.AngConfigManager
import com.cr.tunnel.handler.CommunityConfigManager
import com.cr.tunnel.handler.MmkvManager
import com.cr.tunnel.helper.MessageHelper
import com.cr.tunnel.ui.base.BaseViewModel
import com.cr.tunnel.util.JsonUtil
import com.cr.tunnel.util.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CommunityRow(
    val config: CommunityConfigItem,
    val pingText: String = "",
    val isPinging: Boolean = false,
    val isInvalid: Boolean = false
)

enum class ShareStep {
    VOLUME,
    DURATION,
    USERS,
    LINK
}

data class ShareDraft(
    val volume: String = "",
    val duration: String = "",
    val users: String = "",
    val link: String = ""
)

data class CommunityUiState(
    val loading: Boolean = false,
    val rows: List<CommunityRow> = emptyList(),
    val shareStep: ShareStep? = null,
    val draft: ShareDraft = ShareDraft(),
    val submitting: Boolean = false
)

class CommunityViewModel(application: Application) : BaseViewModel(application) {

    private val _uiState = MutableStateFlow(CommunityUiState())
    val uiState = _uiState.asStateFlow()

    private val pingJobs = mutableMapOf<String, Job>()

    init {
        load()
    }

    fun load() {
        launchLoading {
            _uiState.update { it.copy(loading = true) }
            try {
                val configs = withContext(Dispatchers.IO) {
                    CommunityConfigManager.fetchConfigs()
                }
                val previous = _uiState.value.rows.associateBy { it.config.id }
                val rows = configs.map { config ->
                    previous[config.id] ?: CommunityRow(config)
                }
                _uiState.update { it.copy(rows = rows) }
            } catch (e: Exception) {
                toastError(R.string.community_load_failed)
            } finally {
                _uiState.update { it.copy(loading = false) }
            }
        }
    }

    fun startSharing() {
        if (!CommunityConfigManager.isSharingEnabled()) {
            toastError(R.string.community_sharing_unavailable)
            return
        }
        _uiState.update { it.copy(shareStep = ShareStep.VOLUME, draft = ShareDraft()) }
    }

    fun cancelSharing() {
        if (_uiState.value.submitting) return
        _uiState.update { it.copy(shareStep = null, draft = ShareDraft()) }
    }

    fun nextShareStep(input: String) {
        val step = _uiState.value.shareStep ?: return
        when (step) {
            ShareStep.VOLUME -> _uiState.update {
                it.copy(draft = it.draft.copy(volume = input.trim()), shareStep = ShareStep.DURATION)
            }
            ShareStep.DURATION -> _uiState.update {
                it.copy(draft = it.draft.copy(duration = input.trim()), shareStep = ShareStep.USERS)
            }
            ShareStep.USERS -> _uiState.update {
                it.copy(draft = it.draft.copy(users = input.trim()), shareStep = ShareStep.LINK)
            }
            ShareStep.LINK -> submit()
        }
    }

    fun updateLinkInput(link: String) {
        _uiState.update { it.copy(draft = it.draft.copy(link = link)) }
    }

    private fun submit() {
        val draft = _uiState.value.draft
        val link = draft.link.trim()
        if (!link.contains("://") || link.length > 8000) {
            toastError(R.string.community_invalid_link)
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(submitting = true) }
            try {
                withContext(Dispatchers.IO) {
                    val remarks = AngConfigManager.parseSingleLink(link)?.description.orEmpty()
                    CommunityConfigManager.addConfig(
                        link = link,
                        volume = draft.volume,
                        duration = draft.duration,
                        users = draft.users,
                        name = remarks.ifBlank { "Config" }
                    )
                }
                toastSuccess(R.string.community_share_success)
                _uiState.update { it.copy(shareStep = null, draft = ShareDraft()) }
                load()
            } catch (e: Exception) {
                toastError(e.message ?: getString(R.string.community_share_failed))
            } finally {
                _uiState.update { it.copy(submitting = false) }
            }
        }
    }

    fun pingRow(configId: String) {
        if (pingJobs.containsKey(configId)) return
        val row = _uiState.value.rows.firstOrNull { it.config.id == configId } ?: return
        markRow(configId) { it.copy(isPinging = true, pingText = "", isInvalid = false) }

        pingJobs[configId] = viewModelScope.launch {
            var guid: String? = null
            try {
                val profile = withContext(Dispatchers.IO) {
                    AngConfigManager.parseSingleLink(row.config.link)
                }
                if (profile == null) {
                    markRow(configId) { it.copy(isPinging = false, isInvalid = true) }
                    return@launch
                }

                guid = Utils.getUuid()
                withContext(Dispatchers.IO) {
                    MmkvManager.encodeProfileDirect(guid, JsonUtil.toJson(profile))
                    MessageHelper.sendMsg2TestService(
                        app,
                        TestServiceMessage(
                            key = AppConfig.MSG_MEASURE_CONFIG_START,
                            subscriptionId = "",
                            serverGuids = listOf(guid),
                            onlyTcp = false
                        )
                    )
                }

                var delayMillis: Long? = null
                repeat(PING_POLL_ATTEMPTS) {
                    if (!isActive) return@repeat
                    delay(PING_POLL_INTERVAL_MS)
                    delayMillis = withContext(Dispatchers.IO) {
                        MmkvManager.decodeServerAffiliationInfo(guid)?.testDelayMillis?.takeIf { it != 0L }
                    }
                    if (delayMillis != null) return@repeat
                }

                val result = delayMillis
                val text = when {
                    result == null -> getString(R.string.connection_test_fail)
                    result < 0 -> getString(R.string.connection_test_fail)
                    else -> "$result ms"
                }
                markRow(configId) { it.copy(isPinging = false, pingText = text) }
            } catch (e: Exception) {
                markRow(configId) {
                    it.copy(isPinging = false, pingText = getString(R.string.connection_test_fail))
                }
            } finally {
                val g = guid
                if (g != null) {
                    withContext(Dispatchers.IO) { MmkvManager.removeServer(g) }
                }
                pingJobs.remove(configId)
            }
        }
    }

    fun cancelAllPings() {
        pingJobs.values.forEach { it.cancel() }
        pingJobs.clear()
        MessageHelper.sendMsg2TestService(
            app,
            TestServiceMessage(key = AppConfig.MSG_MEASURE_CONFIG_CANCEL)
        )
        _uiState.update { state ->
            state.copy(rows = state.rows.map {
                if (it.isPinging) it.copy(isPinging = false, pingText = "") else it
            })
        }
    }

    private fun markRow(configId: String, transform: (CommunityRow) -> CommunityRow) {
        _uiState.update { state ->
            state.copy(rows = state.rows.map {
                if (it.config.id == configId) transform(it) else it
            })
        }
    }

    companion object {
        private const val PING_POLL_ATTEMPTS = 30
        private const val PING_POLL_INTERVAL_MS = 500L
    }
}
