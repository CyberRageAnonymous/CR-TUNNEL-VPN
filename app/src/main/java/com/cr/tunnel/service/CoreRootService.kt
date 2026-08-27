package com.cr.tunnel.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.cr.tunnel.AppConfig
import com.cr.tunnel.R
import com.cr.tunnel.contracts.ServiceControl
import com.cr.tunnel.core.CoreServiceManager
import com.cr.tunnel.handler.AppLocaleManager
import com.cr.tunnel.handler.NotificationManager
import com.cr.tunnel.handler.SettingsManager
import com.cr.tunnel.helper.MessageHelper
import com.cr.tunnel.root.RootProxyManager
import com.cr.tunnel.util.LogUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.lang.ref.SoftReference

/**
 * Foreground service for the root (system-wide) run modes. Unlike [CoreVpnService] it
 * does not use Android VpnService — traffic is routed by iptables instead
 * (see [RootProxyManager]).
 *
 * The in-process core is started first (so its listener is up and the foreground
 * notification is posted promptly), then the root routing rules are installed off the
 * main thread. On teardown the rules are removed before the core stops.
 */
class CoreRootService : Service(), ServiceControl {

    private var setupJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        LogUtil.i(AppConfig.TAG, "StartCore-Root: Service created")
        CoreServiceManager.serviceControl = SoftReference(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        NotificationManager.ensureForeground()
        LogUtil.i(AppConfig.TAG, "StartCore-Root: command received")

        // Start the core first, then install the root routing off the main thread.
        if (!CoreServiceManager.startCoreLoop(null)) {
            LogUtil.e(AppConfig.TAG, "StartCore-Root: core failed to start")
            MessageHelper.sendMsg2UI(this, AppConfig.MSG_STATE_START_FAILURE, getString(R.string.toast_services_failure))
            stopService()
            return START_NOT_STICKY
        }

        setupJob = CoroutineScope(Dispatchers.IO).launch {
            if (!RootProxyManager.start(this@CoreRootService)) {
                LogUtil.e(AppConfig.TAG, "StartCore-Root: failed to start root mode, stopping")
                MessageHelper.sendMsg2UI(this@CoreRootService, AppConfig.MSG_STATE_START_FAILURE, getString(R.string.toast_services_failure))
                stopService()
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        // Let an in-flight async setup finish so it cannot re-install rules
        // pointing at a core that is already dead.
        runBlocking { setupJob?.cancelAndJoin() }
        // Remove routing rules before stopping the core.
        RootProxyManager.stop(this)
        CoreServiceManager.stopCoreLoop()
    }

    override fun getService(): Service = this

    override fun startService() {
        // do nothing
    }

    override fun stopService() {
        stopSelf()
    }

    override fun vpnProtect(socket: Int): Boolean = true

    override fun onBind(intent: Intent?): IBinder? = null

    override fun attachBaseContext(newBase: Context?) {
        val context = newBase?.let(AppLocaleManager::localizedContext)
        super.attachBaseContext(context)
    }
}
