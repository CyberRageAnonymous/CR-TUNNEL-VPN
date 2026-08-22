package com.cr.tunnel.handler

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.cr.tunnel.AppConfig
import com.cr.tunnel.R
import com.cr.tunnel.core.CoreServiceManager
import com.cr.tunnel.dto.entities.ProfileItem
import com.cr.tunnel.extension.toSpeedString
import com.cr.tunnel.ui.main.MainActivity
import com.cr.tunnel.helper.MessageHelper
import com.cr.tunnel.util.LogUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.min

object NotificationManager {
    private const val NOTIFICATION_ID = 1
    private const val NOTIFICATION_PENDING_INTENT_CONTENT = 0
    private const val NOTIFICATION_PENDING_INTENT_STOP_V2RAY = 1
    private const val NOTIFICATION_PENDING_INTENT_RESTART_V2RAY = 2
    private const val NOTIFICATION_ICON_THRESHOLD = 3000L

    private var lastQueryTime = 0L

    /**
     * Authoritative session totals, accumulated inside the always-alive service
     * process and mirrored to the UI as absolute values. Broadcasts may be
     * dropped while the UI is backgrounded; absolute values make that lossless.
     */
    @Volatile
    var sessionUplink = 0L
        private set

    @Volatile
    var sessionDownlink = 0L
        private set

    private var mBuilder: NotificationCompat.Builder? = null
    private var mNotificationManager: NotificationManager? = null
    private var uiTrafficStatsJob: Job? = null

    /**
     * Starts the unified traffic loop: the single reader of the core counters.
     * Computes per-tag deltas (safe for both cumulative and reset-on-read
     * counter semantics), broadcasts them to the UI and updates the speed
     * notification from the very same numbers, so no traffic is ever split
     * between competing pollers.
     */
    fun startUiTrafficStatsBroadcast() {
        if (uiTrafficStatsJob != null) return
        val service = getService() ?: return
        lastQueryTime = System.currentTimeMillis()
        sessionUplink = MmkvManager.decodeSettingsLong(AppConfig.PREF_SESSION_UPLINK, 0L)
        sessionDownlink = MmkvManager.decodeSettingsLong(AppConfig.PREF_SESSION_DOWNLINK, 0L)
        uiTrafficStatsJob = CoroutineScope(Dispatchers.IO).launch {
            var lastZeroSpeed = false
            while (isActive) {
                delay(1000)
                val queryTime = System.currentTimeMillis()
                val elapsedSec = ((queryTime - lastQueryTime).coerceAtLeast(1L)) / 1000.0
                lastQueryTime = queryTime

                var upDelta = 0L
                var downDelta = 0L
                var proxyUp = 0L
                var proxyDown = 0L
                var directUp = 0L
                var directDown = 0L

                // Every reading from the core CONSUMES its counters (Go side does
                // counter.Set(0)), so each returned value is already the exact
                // amount accumulated since the previous poll. Sum it directly.
                CoreServiceManager.queryAllOutboundTrafficStats().forEach { stat ->
                    if (stat.tag == AppConfig.TAG_BLOCKED) return@forEach
                    val value = stat.value

                    when (stat.direction) {
                        AppConfig.UPLINK -> upDelta += value
                        AppConfig.DOWNLINK -> downDelta += value
                    }
                    when {
                        stat.tag == AppConfig.TAG_DIRECT -> when (stat.direction) {
                            AppConfig.UPLINK -> directUp += value
                            AppConfig.DOWNLINK -> directDown += value
                        }

                        else -> when (stat.direction) {
                            AppConfig.UPLINK -> proxyUp += value
                            AppConfig.DOWNLINK -> proxyDown += value
                        }
                    }
                }

                // Accumulate in the service process and broadcast ABSOLUTE session
                // totals, so dropped messages while the UI is backgrounded never
                // lose traffic.
                sessionUplink += upDelta
                sessionDownlink += downDelta
                MmkvManager.encodeSettings(AppConfig.PREF_SESSION_UPLINK, sessionUplink)
                MmkvManager.encodeSettings(AppConfig.PREF_SESSION_DOWNLINK, sessionDownlink)
                MessageHelper.sendMsg2UI(service, AppConfig.MSG_TRAFFIC_STATS, "$sessionUplink,$sessionDownlink")

                if (MmkvManager.decodeSettingsBool(AppConfig.PREF_SPEED_ENABLED, true)) {
                    val proxyTotal = proxyUp + proxyDown
                    val directTotal = directUp + directDown
                    val zeroSpeed = proxyTotal + directTotal == 0L
                    if (!zeroSpeed || !lastZeroSpeed) {
                        val text = StringBuilder()
                        appendSpeedString(
                            text, AppConfig.TAG_PROXY,
                            proxyUp / elapsedSec,
                            proxyDown / elapsedSec
                        )
                        appendSpeedString(
                            text, AppConfig.TAG_DIRECT,
                            directUp / elapsedSec,
                            directDown / elapsedSec
                        )
                        updateNotification(text.toString(), proxyTotal, directTotal)
                    }
                    lastZeroSpeed = zeroSpeed
                }
            }
        }
    }

    /**
     * Stops the UI traffic stats broadcast and ends the traffic session.
     */
    fun stopUiTrafficStatsBroadcast() {
        uiTrafficStatsJob?.cancel()
        uiTrafficStatsJob = null
        sessionUplink = 0L
        sessionDownlink = 0L
        MmkvManager.encodeSettings(AppConfig.PREF_SESSION_UPLINK, 0L)
        MmkvManager.encodeSettings(AppConfig.PREF_SESSION_DOWNLINK, 0L)
    }

    /**
     * Pushes the current absolute session totals to the UI. Used when a UI
     * process (re)attaches so it immediately mirrors the service numbers.
     */
    fun pushSessionTotalsToUi(service: Service) {
        MessageHelper.sendMsg2UI(
            service, AppConfig.MSG_TRAFFIC_STATS,
            "$sessionUplink,$sessionDownlink"
        )
    }

    /**
     * Shows the notification.
     * @param currentConfig The current profile configuration.
     */
    fun showNotification(currentConfig: ProfileItem?) {
        val service = getService() ?: return

        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

        val startMainIntent = Intent(service, MainActivity::class.java)
        val contentPendingIntent = PendingIntent.getActivity(service, NOTIFICATION_PENDING_INTENT_CONTENT, startMainIntent, flags)

        val stopV2RayIntent = Intent(AppConfig.BROADCAST_ACTION_SERVICE)
        stopV2RayIntent.`package` = AppConfig.ANG_PACKAGE
        stopV2RayIntent.putExtra("key", AppConfig.MSG_STATE_STOP)
        val stopV2RayPendingIntent = PendingIntent.getBroadcast(service, NOTIFICATION_PENDING_INTENT_STOP_V2RAY, stopV2RayIntent, flags)

        val restartV2RayIntent = Intent(AppConfig.BROADCAST_ACTION_SERVICE)
        restartV2RayIntent.`package` = AppConfig.ANG_PACKAGE
        restartV2RayIntent.putExtra("key", AppConfig.MSG_STATE_RESTART)
        val restartV2RayPendingIntent = PendingIntent.getBroadcast(service, NOTIFICATION_PENDING_INTENT_RESTART_V2RAY, restartV2RayIntent, flags)

        val channelId =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel()
            } else {
                // If earlier version channel ID is not used
                // https://developer.android.com/reference/android/support/v4/app/NotificationCompat.Builder.html#NotificationCompat.Builder(android.content.Context)
                ""
            }

        mBuilder = NotificationCompat.Builder(service, channelId)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle(currentConfig?.remarks ?: service.getString(R.string.app_name))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setShowWhen(false)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentPendingIntent)
            .addAction(
                R.drawable.ic_delete_24dp,
                service.getString(R.string.notification_action_stop_v2ray),
                stopV2RayPendingIntent
            )
            .addAction(
                R.drawable.ic_restore_24dp,
                service.getString(R.string.title_service_restart),
                restartV2RayPendingIntent
            )

        //mBuilder?.setDefaults(NotificationCompat.FLAG_ONLY_ALERT_ONCE)

        service.startForeground(NOTIFICATION_ID, mBuilder?.build())
    }

    /**
     * Fulfills or refreshes the foreground-service contract before a start command can
     * return early. A duplicate startForegroundService call still requires the service
     * to enter foreground state promptly, even when the core is already running.
     */
    fun ensureForeground() {
        val service = getService() ?: return
        val notification = mBuilder?.build()
        if (notification == null) showNotification(null) else service.startForeground(NOTIFICATION_ID, notification)
    }

    /**
     * Cancels the notification.
     */
    fun cancelNotification() {
        val service = getService() ?: return
        service.stopForeground(Service.STOP_FOREGROUND_REMOVE)

        mBuilder = null
        mNotificationManager = null
    }

    /**
     * Resets the speed section of the notification. The unified traffic loop in
     * startUiTrafficStatsBroadcast keeps it updated afterwards.
     */
    fun stopSpeedNotification() {
        updateNotification("", 0, 0)
    }

    /**
     * Creates a notification channel for Android O and above.
     * @return The channel ID.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(): String {
        val channelId = AppConfig.RAY_NG_CHANNEL_ID
        val channelName = AppConfig.RAY_NG_CHANNEL_NAME
        // Foreground-service notifications must remain visible; LOW is silent but valid.
        val chan = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
        chan.lightColor = Color.DKGRAY
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        getNotificationManager()?.createNotificationChannel(chan)
        return channelId
    }

    /**
     * Updates the notification with the given content text and traffic data.
     * @param contentText The content text.
     * @param proxyTraffic The proxy traffic.
     * @param directTraffic The direct traffic.
     */
    private fun updateNotification(contentText: String?, proxyTraffic: Long, directTraffic: Long) {
        if (mBuilder != null) {
            if (proxyTraffic < NOTIFICATION_ICON_THRESHOLD && directTraffic < NOTIFICATION_ICON_THRESHOLD) {
                mBuilder?.setSmallIcon(R.drawable.ic_stat_name)
            } else if (proxyTraffic > directTraffic) {
                mBuilder?.setSmallIcon(R.drawable.ic_stat_proxy)
            } else {
                mBuilder?.setSmallIcon(R.drawable.ic_stat_direct)
            }
            mBuilder?.setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            mBuilder?.setContentText(contentText)
            getNotificationManager()?.notify(NOTIFICATION_ID, mBuilder?.build())
        }
    }

    /**
     * Gets the notification manager.
     * @return The notification manager.
     */
    private fun getNotificationManager(): NotificationManager? {
        if (mNotificationManager == null) {
            val service = getService() ?: return null
            mNotificationManager = service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        }
        return mNotificationManager
    }

    /**
     * Appends the speed string to the given text.
     * @param text The text to append to.
     * @param name The name of the tag.
     * @param up The uplink speed.
     * @param down The downlink speed.
     */
    private fun appendSpeedString(text: StringBuilder, name: String?, up: Double, down: Double) {
        var n = name ?: "no tag"
        n = n.take(min(n.length, 6))
        text.append(n)
        for (i in n.length..6 step 2) {
            text.append("\t")
        }
        text.append("•  ${up.toLong().toSpeedString()}↑  ${down.toLong().toSpeedString()}↓\n")
    }

    /**
     * Gets the service instance.
     * @return The service instance.
     */
    private fun getService(): Service? {
        return CoreServiceManager.serviceControl?.get()?.getService()
    }
}
