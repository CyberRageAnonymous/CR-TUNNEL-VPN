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
import com.cr.tunnel.util.LogUtil
import java.lang.ref.SoftReference

class CoreProxyOnlyService : Service(), ServiceControl {
    
    override fun onCreate() {
        super.onCreate()
        LogUtil.i(AppConfig.TAG, "StartCore-Proxy: Service created")
        CoreServiceManager.serviceControl = SoftReference(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        NotificationManager.ensureForeground()
        LogUtil.i(AppConfig.TAG, "StartCore-Proxy: Service command received")

        if (!CoreServiceManager.startCoreLoop(null)) {
            LogUtil.e(AppConfig.TAG, "StartCore-Proxy: Failed to start core loop")
            MessageHelper.sendMsg2UI(this, AppConfig.MSG_STATE_START_FAILURE, getString(R.string.toast_services_failure))
            stopSelf()
            return START_NOT_STICKY
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        CoreServiceManager.stopCoreLoop()
    }

    override fun getService(): Service {
        return this
    }

    override fun startService() {
        // do nothing
    }

    override fun stopService() {
        stopSelf()
    }

    override fun vpnProtect(socket: Int): Boolean {
        return true
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun attachBaseContext(newBase: Context?) {
        val context = newBase?.let(AppLocaleManager::localizedContext)
        super.attachBaseContext(context)
    }
}
