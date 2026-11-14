/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import nd.max.MainActivity
import nd.max.R
import nd.max.core.platform.SessionCause
import nd.max.core.platform.SessionInsight
import nd.max.core.platform.SessionVerdict
import nd.max.core.platform.StoredGameSession
import nd.max.ui.settings.AppLanguage
import nd.max.widget.core.WidgetIntents

/**
 * إشعار نتيجة الجلسة: الجملة التي تُغلق الحلقة.
 *
 * **المشكلة التي يحلّها:** الجلسة تُحفظ في ملفّ لا يراه أحد. من لعب نصف ساعة ثم أغلق اللعبة
 * لا يعرف أنه سُجّل، ولا أن أداءه تراجع — حتى يتذكّر أن يفتح الشاشة. فالإشعار هو التسليم:
 * الحكم في سطر، والضغط يفتح السجلّ على الجلسة نفسها.
 *
 * **والحدّ المعلن:** بلا `POST_NOTIFICATIONS` (أندرويد ١٣+) لا يظهر شيء، ولا يُطلب الإذن من
 * هنا — الشاشة هي موضع الطلب. وكل شيء داخل `runCatching`: فشل الإشعار لا يُسقط الحفظ ولا
 * يُسقط الجلسة، لأن الإشعار زينة والحفظ أصل.
 */
object SessionSummaryNotifier {

    private const val CHANNEL_ID = "max_session_summary"
    private const val NOTIFICATION_ID = 0x5E55

    fun notify(context: Context, session: StoredGameSession, insight: SessionInsight) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            val text = AppLanguage.wrap(context)
            val manager = text.getSystemService(NotificationManager::class.java) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                manager.getNotificationChannel(CHANNEL_ID) == null
            ) {
                manager.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        text.getString(R.string.session_notify_channel),
                        NotificationManager.IMPORTANCE_DEFAULT
                    )
                )
            }
            val open = PendingIntent.getActivity(
                text,
                NOTIFICATION_ID,
                Intent(text, MainActivity::class.java).apply {
                    putExtra(WidgetIntents.EXTRA_OPEN_ROUTE, WidgetIntents.ROUTE_GAME_SESSIONS)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val body = body(text, session, insight)
            val notification = NotificationCompat.Builder(text, CHANNEL_ID)
                .setContentTitle(text.getString(R.string.session_notify_title, session.displayName))
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(open)
                .build()
            manager.notify(NOTIFICATION_ID, notification)
        }
    }

    /**
     * السطر: «٥٨ FPS · ١٢٪ jank · أسوأ من المعتاد».
     * والأجزاء تُبنى بالترتيب، وما لا يُقاس يُحذف بدل أن يُكتب صفرًا (ADR-07).
     */
    private fun body(context: Context, session: StoredGameSession, insight: SessionInsight): String {
        val parts = ArrayList<String>(5)
        // المدة أولًا لأنها السؤال الأول بعد الجلسة، وتُذكر فقط حين تكون مقيسة (نهاية بعد بداية).
        val durationMs = (session.endedAtMs - session.startedAtMs).takeIf { it > 0L }
        if (durationMs != null) {
            val minutes = durationMs / 60_000L
            parts += if (minutes >= 60L) {
                context.getString(R.string.session_notify_duration_hm, minutes / 60L, minutes % 60L)
            } else {
                context.getString(R.string.session_notify_duration_min, minutes)
            }
        }
        session.framesAverage?.let { parts += context.getString(R.string.session_notify_fps, it.toInt()) }
        session.jankAverage?.let { parts += context.getString(R.string.session_notify_jank, it) }
        val verdict = when (insight.verdict) {
            SessionVerdict.Worse -> R.string.session_notify_worse
            SessionVerdict.Better -> R.string.session_notify_better
            SessionVerdict.Similar -> R.string.session_notify_similar
            SessionVerdict.FirstRun -> R.string.session_notify_first
            SessionVerdict.Unusable -> R.string.session_notify_unusable
        }
        parts += context.getString(verdict)
        if (insight.verdict == SessionVerdict.Worse) {
            val cause = when (insight.cause) {
                SessionCause.Thermal -> R.string.session_cause_thermal
                SessionCause.Jank -> R.string.session_cause_jank
                SessionCause.Energy -> R.string.session_cause_energy
                SessionCause.PowerSave -> R.string.session_cause_power_save
                SessionCause.Refresh -> R.string.session_cause_refresh
                SessionCause.Renderer -> R.string.session_cause_renderer
                SessionCause.None -> 0
            }
            if (cause != 0) parts += context.getString(cause)
        }
        return parts.joinToString(" · ")
    }
}
