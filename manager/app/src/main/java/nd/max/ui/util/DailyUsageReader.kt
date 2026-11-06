/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.util

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.app.AppOpsManager
import android.content.Context
import android.os.Process
import nd.max.core.stats.DailyUsage
import java.time.ZoneId

/**
 * يقرأ أحداث المقدّمة الخام ويعيد مجموع كل يوم منها.
 *
 * **ولماذا الأحداث لا `queryUsageStats`:** الاستعلام المجمَّع يعيد ما قرّره النظام عن
 * حدود اليوم، ولا يعيد تفصيلًا يوميًّا لنافذة أسبوع. الأحداث تعطي لحظة البداية والنهاية،
 * فالتقطيع يصير عندنا (`DailyUsage`) وقابلًا للاختبار.
 *
 * يومُ تمهيد قبل النافذة يلتقط نشاطًا بدأ قبل منتصف الليل؛ ثم يُقصّ زمنه عند حدود الرسم.
 * الأحداث القديمة المحذوفة تبقى مجهولة، والإيقاف بلا بداية لا يخلق زمنًا. لهذا لا يلزم
 * أن يساوي مجموع الرسم أرقام قاعدة الاستخدام المجمَّعة في قائمة التطبيقات.
 */
object DailyUsageReader {

    fun read(
        context: Context,
        days: Int,
        nowMs: Long = System.currentTimeMillis()
    ): Result<List<DailyUsage.Day>> = runCatching {
        require(days in 1..31)
        requireUsageAccess(context)
        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: error("UsageStatsManager unavailable")
        val zone = ZoneId.systemDefault()
        val today = DailyUsage.dayOf(nowMs, zone)
        val from = today - (days - 1)
        val queryStart = DailyUsage.startOfDay(from - 1, zone)
        val stream = manager.queryEvents(queryStart, nowMs)
            ?: error("Usage events unavailable")

        val events = ArrayList<DailyUsage.Event>()
        val event = UsageEvents.Event()
        while (stream.hasNextEvent()) {
            stream.getNextEvent(event)
            val kind = when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> DailyUsage.Kind.RESUME
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED -> DailyUsage.Kind.PAUSE
                UsageEvents.Event.SCREEN_NON_INTERACTIVE,
                UsageEvents.Event.KEYGUARD_SHOWN,
                UsageEvents.Event.DEVICE_SHUTDOWN,
                UsageEvents.Event.DEVICE_STARTUP -> DailyUsage.Kind.CLOSE_ALL
                else -> null
            } ?: continue
            events += DailyUsage.Event(
                event.timeStamp, kind, event.packageName.orEmpty(), event.className.orEmpty()
            )
        }
        // سحب الإذن أثناء القراءة قد يعيد تدفّقًا فارغًا: لا نسمّيه يومًا صفريًّا.
        requireUsageAccess(context)
        DailyUsage.bucket(DailyUsage.foregroundSpans(events, nowMs), from, today, zone)
    }

    private fun requireUsageAccess(context: Context) {
        val appOps = context.getSystemService(AppOpsManager::class.java)
            ?: error("Usage access unavailable")
        check(appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName
        ) == AppOpsManager.MODE_ALLOWED) { "Usage access not granted" }
    }
}
