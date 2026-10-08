package com.cr.tunnel.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.lifecycle.lifecycleScope
import com.cr.tunnel.AppConfig
import com.cr.tunnel.R
import com.cr.tunnel.extension.toast
import com.cr.tunnel.extension.toastError
import com.cr.tunnel.handler.AngConfigManager
import com.cr.tunnel.handler.MmkvManager
import com.cr.tunnel.handler.SettingsChangeManager
import com.cr.tunnel.ui.base.BaseComponentActivity
import com.cr.tunnel.ui.main.MainActivity
import com.cr.tunnel.util.LogUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLDecoder

class UrlSchemeActivity : BaseComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val uri = intent.data
            val action = intent.action
            val host = uri?.host
            when {
                action == Intent.ACTION_SEND -> {
                    if ("text/plain" == intent.type) {
                        intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
                            parseUri(it, null)
                        }
                    }
                    openMain()
                }

                action == Intent.ACTION_VIEW &&
                    (host == "install-config" || host == "install-sub") -> {
                    parseUri(uri?.getQueryParameter("url").orEmpty(), uri?.fragment)
                    openMain()
                }

                action == Intent.ACTION_VIEW &&
                    uri != null &&
                    (uri.scheme == "content" || uri.scheme == "file") -> {
                    importFile(uri)
                }

                else -> {
                    toastError(R.string.toast_failure)
                    openMain()
                }
            }
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Error processing URL scheme", e)
            openMain()
        }
    }

    @Composable
    override fun ScreenContent() {
    }

    private fun openMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun importFile(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            val text = runCatching {
                contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull().orEmpty()
            val groupId = MmkvManager
                .decodeSettingsString(AppConfig.CACHE_SUBSCRIPTION_ID, "")
                .orEmpty()
            val count = if (text.isBlank()) {
                0
            } else {
                AngConfigManager.importBatchConfig(text, groupId, true).first
            }
            withContext(Dispatchers.Main) {
                when {
                    count > 0 -> {
                        SettingsChangeManager.makeSetupGroupTab()
                        toast(getString(R.string.title_import_config_count, count))
                    }

                    AngConfigManager.isCrtContent(text) ->
                        toastError(R.string.toast_crt_invalid)

                    AngConfigManager.isEncryptedNpvConfig(text) ->
                        toastError(R.string.toast_npv_encrypted)

                    else -> toastError(R.string.toast_failure)
                }
                openMain()
            }
        }
    }

    private fun parseUri(uriString: String?, fragment: String?) {
        if (uriString.isNullOrEmpty()) {
            return
        }
        LogUtil.i(AppConfig.TAG, uriString)

        var decodedUrl = URLDecoder.decode(uriString, "UTF-8")
        val uri = Uri.parse(decodedUrl)
        if (uri != null) {
            if (uri.fragment.isNullOrEmpty() && !fragment.isNullOrEmpty()) {
                decodedUrl += "#${fragment}"
            }
            LogUtil.i(AppConfig.TAG, decodedUrl)
            lifecycleScope.launch(Dispatchers.IO) {
                val (count, countSub) = AngConfigManager.importBatchConfig(decodedUrl, "", false)
                withContext(Dispatchers.Main) {
                    if (count + countSub > 0) {
                        toast(R.string.import_subscription_success)
                    } else {
                        toast(R.string.import_subscription_failure)
                    }
                }
            }
        }
    }
}
