/*
 * Copyright (C) 2026-2027 Zexshia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nd.max.receiver

import nd.max.core.security.SecurityEnforcement
import nd.max.core.security.IntegrityCheckpoint
import nd.max.MaxManagerProps


import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nd.max.MainActivity
import nd.max.R
import nd.max.core.maxai.ActiveObjectiveNotice
import nd.max.core.platform.PropertyUtils


class MaxManagerReceiver : BroadcastReceiver() {

    companion object {
        private const val CH_PROFILE = "max_profile"
        private const val CH_SYSTEM = "max_system"
        private const val PROFILE_ID = 1001
        private const val GROUP_SYSTEM_LOGS = "nd.max.GROUP_SYSTEM"
        
        const val ACTION_MANAGE = "nd.max.ACTION_MANAGE"
        private const val ACTION_RESHOW = "nd.max.ACTION_RESHOW"

        /** النص الأصلي القادم من الخادم (بلا سطر Max AI) — يُحفظ في الإشعار نفسه ليُعاد بناؤه منه. */
        private const val EXTRA_BASE_TEXT = "nd.max.extra.BASE_TEXT"

        /**
         * عنوان الخادم الأصلي («Balanced Profile» / «ECO Mode» …). العنوان الظاهر صار اسم التطبيق،
         * فيُحفظ الأصلي هنا: منه يُعرف أن الإشعار بطاقة ملف، ومنه يُعاد البناء، ويُدرج في نص الإشعار.
         */
        private const val EXTRA_BASE_TITLE = "nd.max.extra.BASE_TITLE"

        /**
         * يُعيد بناء بطاقة الملف **القائمة** بهدف Max AI الفاعل الحالي.
         *
         * ولماذا: البطاقة يرسلها الخادم عند تطبيق ملف عام فقط، وMax AI لا يطبّق ملفات (يحرّك مقابض
         * بأوزان هدف) — فكانت تبقى «Balanced Profile» أبدًا والمحرك يعمل، ويظنّ المستخدم أنه معطّل.
         * تُستدعى من المحرك كلما تغيّر الهدف الفاعل.
         *
         * **لا تُحيي بطاقة غائبة**: إن أزالها الخادم (Max AI مطفأ / أساس متوازن تلقائي) أو أزاحها
         * المستخدم فلا شيء يُنشر هنا — الإحياء كان سيعيد إشعارًا قرّر غيرنا إسقاطه.
         * والنص الأصلي يُقرأ من إشعارنا النشط نفسه (لا متغيّر ساكن) فيصمد أمام إعادة تشغيل العملية.
         */
        fun refreshProfileCard(context: Context, objectiveOverride: String? = null) {
            runCatching {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val active = manager.activeNotifications.firstOrNull { it.id == PROFILE_ID } ?: return
                if (PropertyUtils.get(MaxManagerProps.General.PROFILE_NOTIFICATIONS) == "0") return
                val extras = active.notification.extras
                // الإشعارات القديمة (قبل هذا التغيير) عنوانها الظاهر هو عنوان الخادم نفسه.
                val title = extras.getString(EXTRA_BASE_TITLE)
                    ?: extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
                    ?: return
                val base = extras.getString(EXTRA_BASE_TEXT)
                    ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
                    ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
                val chrono = extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER, false)
                postNotification(context, manager, title, base, chrono, 0L, objectiveOverride)
            }
        }

        /**
         * [objectiveOverride]: هدف اختاره المستخدم للتوّ. يُقدَّم على الخصيصة المنشورة لأن المحرك
         * ينشرها بعد دورة كاملة — وهذا بالضبط سبب تأخّر الإشعار بعد الضغط.
         */
        private fun maxAiLine(context: Context, objectiveOverride: String? = null): String? =
            ActiveObjectiveNotice.line(
                context,
                ActiveObjectiveNotice.resolve(
                    PropertyUtils.get(MaxManagerProps.Conf.AI_ENABLED),
                    objectiveOverride?.let { ActiveObjectiveNotice.encode(it, "user") }
                        ?: PropertyUtils.get(MaxManagerProps.AiRuntime.ACTIVE_OBJECTIVE),
                    objectiveOverride ?: PropertyUtils.get(MaxManagerProps.Conf.AI_OBJECTIVE),
                ),
            )

        private fun postNotification(
            context: Context,
            manager: NotificationManager,
            title: String,
            message: String,
            chrono: Boolean,
            timeout: Long,
            objectiveOverride: String? = null,
        ) {
            val isProfile = title.lowercase(Locale.ROOT).let {
                it.contains("profile") || it.contains("mode") || it.contains("initializing...")
            }

            if (isProfile && PropertyUtils.get(MaxManagerProps.General.PROFILE_NOTIFICATIONS) == "0") {
                return
            }

            val channelId = if (isProfile) CH_PROFILE else CH_SYSTEM

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name = if (isProfile) context.getString(R.string.notif_ch_profiles) else context.getString(R.string.notif_ch_system)

                val channel = NotificationChannel(channelId, name, NotificationManager.IMPORTANCE_LOW)
                manager.createNotificationChannel(channel)
            }

            val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            val clickIntent = Intent(context, MainActivity::class.java)
            val clickPI = PendingIntent.getActivity(context, 0, clickIntent, pendingIntentFlags)

            // بطاقة الملف وحدها تحمل سطر Max AI: ما عداها إشعارات نظام لا علاقة لها بالهدف.
            val aiLine = if (isProfile) maxAiLine(context, objectiveOverride) else null
            // عنوان بطاقة الملف هو اسم التطبيق دائمًا؛ واسم الملف («Balanced Profile» …) ينتقل إلى النص.
            val displayTitle = if (isProfile) context.getString(R.string.app_name) else title
            val profileLine = "$title — $message"
            val shownText = if (aiLine != null) "$profileLine\n$aiLine" else profileLine

            val builder = NotificationCompat.Builder(context, channelId).apply {

                setSmallIcon(R.drawable.avatar_transparent)
                setContentTitle(displayTitle)
                setUsesChronometer(chrono)
                setShowWhen(false)
                setOngoing(isProfile)
                setAutoCancel(!isProfile)
                setContentIntent(clickPI)

                if (isProfile) {
                    // السطر المطويّ كان فارغًا (BigTextStyle بلا ContentText) فلا يظهر Max AI إلا
                    // بالتوسيع. الآن المطويّ يحمل ما يعمل، والموسَّع يحمل الاثنين.
                    setContentText(aiLine ?: profileLine)
                    setStyle(NotificationCompat.BigTextStyle().bigText(shownText))
                    setOnlyAlertOnce(true)
                    addExtras(Bundle().apply {
                        putString(EXTRA_BASE_TEXT, message)
                        putString(EXTRA_BASE_TITLE, title)
                    })
                } else {
                    setContentText(message)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    setColor(ContextCompat.getColor(context, android.R.color.holo_blue_dark))
                }

                if (isProfile) {
                    val reshowIntent = Intent(context, MaxManagerReceiver::class.java).apply {
                        this.action = ACTION_RESHOW
                        putExtra("notifytitle", title)
                        putExtra("notifytext", message)
                        putExtra("chrono_bool", chrono)
                    }
                    val deletePI = PendingIntent.getBroadcast(context, title.hashCode(), reshowIntent, pendingIntentFlags)
                    setDeleteIntent(deletePI)
                } else {
                    setGroup(GROUP_SYSTEM_LOGS)
                }

                if (timeout > 0L) {
                    setTimeoutAfter(timeout)
                }
            }

            manager.notify(if (isProfile) PROFILE_ID else title.hashCode(), builder.build())
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        // بوّابة المستقبِل: أشهر مسار لتشغيل عمل بلا واجهة على نسخة معدَّلة.
        if (SecurityEnforcement.refuseReceiver(context, IntegrityCheckpoint.RECEIVER)) return
        val action = intent.action ?: return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        when (action) {
            ACTION_MANAGE -> {
                val clearAll = intent.getBooleanExtra("clearall", false) || 
                              intent.getStringExtra("clearall") == "true"
                if (clearAll) {
                    manager.cancelAll()
                }
                // The daemon asks for the profile card to be dropped (Max AI off, automatic balanced baseline).
                // Nothing is announced; only the stale profile notification is removed.
                val clearProfile = intent.getBooleanExtra("clearprofile", false) ||
                    intent.getStringExtra("clearprofile") == "true"
                if (clearProfile) {
                    manager.cancel(PROFILE_ID)
                    return
                }
                intent.getStringExtra("toasttext")?.let {
                    Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                }
                intent.getStringExtra("notifytext")?.let {
                    handleNotification(context, intent, manager)
                }
            }

            ACTION_RESHOW -> {


                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.Default).launch {
                    delay(3000)
                    val reshow = Intent(context, MaxManagerReceiver::class.java).apply {
                        this.action = ACTION_MANAGE
                        putExtras(intent.extras ?: return@launch)
                    }
                    context.sendBroadcast(reshow)
                    pendingResult.finish()
                }
            }
        }
    }

    private fun handleNotification(context: Context, intent: Intent, manager: NotificationManager) {
        val title = intent.getStringExtra("notifytitle") ?: "MaxManager"
        val message = intent.getStringExtra("notifytext") ?: ""

        val chrono = intent.getBooleanExtra("chrono_bool",
            intent.getStringExtra("chrono") == "true")

        val timeout = intent.getStringExtra("timeout")?.toLongOrNull() ?: 0L

        postNotification(context, manager, title, message, chrono, timeout)
    }
}
