/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.core.gamespace

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Process
import android.provider.Settings
import nd.max.core.platform.FpsMonitorUtil
import nd.max.core.privilege.ExecutionModeStore
import nd.max.core.privilege.PrivilegeManager
import nd.max.core.privilege.PrivilegedShell

/**
 * إذن «وصول الاستخدام» (`GET_USAGE_STATS`): قراءته، ومنحه بالمسار الموحَّد، وبديله اليدويّ.
 *
 * المنح عبر `appops set` أمر مميَّز فيمرّ بـ[PrivilegedShell.exec] (جذر أو Shizuku حسب الوضع، ويُرفض في Basic).
 * **غير مقيس على جهاز:** قبول uid الـshell لهذا الأمر؛ ولذلك لا يُعلَن النجاح قبل أن تُعاد قراءة الإذن فعلًا.
 */
object UsageAccess {
    fun isGranted(context: Context): Boolean = runCatching {
        val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        ops.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        ) == AppOpsManager.MODE_ALLOWED
    }.getOrDefault(false)

    /** يحاول المنح بالمسار الموحَّد ويُعيد الحكم **المقروء بعده** لا ما أعلنه الأمر. */
    suspend fun grantViaPrivilege(context: Context): Boolean {
        PrivilegedShell.exec("appops set ${context.packageName} GET_USAGE_STATS allow")
        return isGranted(context)
    }

    /** صفحة «وصول الاستخدام» في الإعدادات — البديل حين يتعذّر المنح الآلي (أو وضع Basic). */
    fun settingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

/**
 * قارئ اللعبة الأمامية من أحداث الاستخدام — يحتفظ بالحالة بين الاستطلاعات فلا يعيد قراءة الساعات الماضية.
 * أول استعلام يغطّي [INITIAL_WINDOW_MS] ليلتقط لعبة بدأت قبل تشغيل الخدمة؛ وما بعده من آخر استعلام مع
 * تداخل [OVERLAP_MS] (إعادة التطبيق آمنة — انظر [UsageForegroundRules]).
 */
object UsageStatsForeground {
    private const val INITIAL_WINDOW_MS = 2 * 60 * 60 * 1000L
    private const val OVERLAP_MS = 1_500L

    private var lastQueryMs = 0L
    private var current = ""

    @Synchronized
    fun current(context: Context): String {
        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return current
        val now = System.currentTimeMillis()
        val from = if (lastQueryMs == 0L) now - INITIAL_WINDOW_MS else lastQueryMs - OVERLAP_MS
        val stream = runCatching { manager.queryEvents(from, now) }.getOrNull() ?: return current
        val events = ArrayList<ForegroundEvent>()
        val event = UsageEvents.Event()
        while (stream.hasNextEvent()) {
            stream.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED ->
                    events += ForegroundEvent(event.packageName, resumed = true, timeMs = event.timeStamp)
                UsageEvents.Event.ACTIVITY_PAUSED ->
                    events += ForegroundEvent(event.packageName, resumed = false, timeMs = event.timeStamp)
            }
        }
        lastQueryMs = now
        current = UsageForegroundRules.reduce(current, events)
        return current
    }

    /** يُصفَّر عند تبديل المصدر أو إعادة تشغيل الخدمة: لا حالة قديمة تُنسب إلى مصدر جديد. */
    @Synchronized
    fun reset() {
        lastQueryMs = 0L
        current = ""
    }
}

/** المدخل الواحد للخدمة: يختار المصدر بحسب الوضع والإذن ثم يقرأ منه. `""` = مجهول. */
object GameForeground {
    fun source(context: Context): ForegroundSource = ForegroundSourceRules.choose(
        mode = ExecutionModeStore.current(),
        rootGranted = PrivilegeManager.cachedRootGranted(),
        usageAccess = UsageAccess.isGranted(context)
    )

    fun current(context: Context, source: ForegroundSource): String = when (source) {
        ForegroundSource.SHELL -> FpsMonitorUtil.getForegroundPackage()
        ForegroundSource.USAGE_STATS -> UsageStatsForeground.current(context)
        ForegroundSource.NONE -> ""
    }
}
