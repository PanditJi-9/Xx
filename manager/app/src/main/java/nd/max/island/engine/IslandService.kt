/*
 * Ported from Islandify (https://github.com/bughunter11/Islandify) — MIT License,
 * Copyright (c) 2026 bughunter11. Full license text: THIRD_PARTY_NOTICES.md.
 * Adapted for MaxManager (packages, resources, integration). Modifications (C) 2026 Nader Magdy.
 */
package nd.max.island.engine

import nd.max.MainActivity
import nd.max.R
import nd.max.widget.core.WidgetIntents
import nd.max.island.core.IslandSettings
import nd.max.island.core.Perms
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Foreground service that keeps Islandify alive and shows the "running" notification.
 *
 * The island window itself is normally hosted by [IslandA11yService] (above the status bar,
 * taps work). Only when that service is not connected does this service show the older
 * "draw over other apps" window as a fallback.
 */
class IslandService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var fallback: IslandOverlay? = null

    override fun onCreate() {
        super.onCreate()
        IslandSettings.init(this)
        IslandController.attach(this)
        startAsForeground()

        val o = IslandOverlay(this, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        fallback = o
        // **ونافذة الاحتياط تتبع المفتاح لا الصلاحية وحدها:** كانت تُعرض متى وُجدت صلاحية
        // الرسم ولو كانت الجزيرة مطفأة — فخدمةٌ عادت للعمل تترك نافذةً لا يريدها المستخدم.
        // والشرط الآن ثلاثة معًا: مفتاحٌ مفتوح، وبلا استضافة وصولية، وصلاحية رسم.
        scope.launch {
            combine(IslandSettings.enabled, IslandA11yService.connected) { on, a11y -> on && !a11y }
                .collect { visible ->
                    if (visible && Perms.overlay(this@IslandService)) o.show() else o.hide()
                }
        }
        running.value = true
    }

    private fun startAsForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        // ROMs cache the notification icon per channel / notification id / icon resource.
        // So everything is new now (channel, id and icon name), and the old ones are removed.
        OLD_CHANNEL_IDS.forEach { runCatching { nm.deleteNotificationChannel(it) } }
        OLD_NOTIF_IDS.forEach { runCatching { nm.cancel(it) } }
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.ify_notif_channel), NotificationManager.IMPORTANCE_MIN).apply {
                setShowBadge(false)
            }
        )
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java)
                .putExtra(WidgetIntents.EXTRA_OPEN_ROUTE, WidgetIntents.ROUTE_ISLAND)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_islandify)
            .setColor(0xFF7B3DF5.toInt())
            .setContentTitle(getString(R.string.ify_notif_title))
            .setContentText(getString(R.string.ify_notif_text))
            .setContentIntent(open)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setShowWhen(false)
            .setSilent(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else startForeground(NOTIF_ID, n)
    }

    override fun onDestroy() {
        scope.cancel()
        fallback?.hide()
        fallback = null
        running.value = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "island_status_v3"
        private val OLD_CHANNEL_IDS = listOf("island", "island_status")
        private const val NOTIF_ID = 2
        private val OLD_NOTIF_IDS = listOf(1)

        /** The app's on/off switch reads the island's real state from here. */
        val running = MutableStateFlow(false)

        /**
         * True while the island is really on: switched on in the app AND hosted by this service
         * or by the accessibility service. After a reboot only the accessibility service is up
         * (this service starts when the app is opened), so the switch must not depend on [running] alone.
         */
        val active: Flow<Boolean> = combine(
            IslandSettings.enabled, running, IslandA11yService.connected
        ) { enabled, svc, a11y -> enabled && (svc || a11y) }

        fun isActive(): Boolean =
            IslandSettings.enabled.value && (running.value || IslandA11yService.connected.value)
    }
}
