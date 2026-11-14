package com.cr.tunnel.ui.main

import android.os.Bundle
import androidx.compose.runtime.Composable
import com.cr.tunnel.ui.base.BaseComponentActivity

class LatencyGraphActivity : BaseComponentActivity() {

    private var guid: String = ""
    private var serverName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        guid = intent.getStringExtra("guid") ?: ""
        serverName = intent.getStringExtra("server_name") ?: "Server"
    }

    @Composable
    override fun ScreenContent() {
        LatencyGraphScreen(
            serverGuid = guid,
            serverName = serverName,
            onBackClick = { finish() }
        )
    }
}