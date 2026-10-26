/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.core.gamespace

import nd.max.core.privilege.ExecutionMode
import nd.max.core.privilege.ExecutionModeRules
import nd.max.core.privilege.PrivilegedShellRules

/**
 * من أين تعرف خدمة اللوحة اللعبة الأمامية؟
 *
 * - [SHELL]: `dumpsys` بصدفة الجذر (`FpsMonitorUtil`) — السلوك السابق حرفيًّا، وهو الأدقّ (يراجع نافذة التركيز).
 * - [USAGE_STATS]: أحداث `UsageStatsManager` — **بلا جذر ولا صدفة**؛ إذنها «وصول الاستخدام» يمنحه المستخدم أو
 *   يمنحه Shizuku بـ`appops`. هذا ما كان يغيب في وضعَي Shizuku وBasic فلا تظهر اللوحة.
 * - [NONE]: لا مصدر ⇒ الموضوع «مجهول» والواجهة تقول السبب (لا تخمين).
 */
enum class ForegroundSource { SHELL, USAGE_STATS, NONE }

/** اختيار المصدر — **نقيّ** فيُقاس على JVM. */
object ForegroundSourceRules {
    /**
     * الجذر هو المصدر متى كان **مسار الأوامر** في هذا الوضع جذرًا ([ExecutionModeRules.lane] بلا اعتبار Shizuku)،
     * فيبقى سلوك AUTO/ROOT كما كان. وغير ذلك: UsageStats إن مُنح إذنه، وإلا لا مصدر.
     * (UsageStats إذن تطبيق عاديّ لا أمر مميَّز، فلا يخالف حصر الأوضاع للأوامر المميَّزة.)
     */
    fun choose(mode: ExecutionMode, rootGranted: Boolean, usageAccess: Boolean): ForegroundSource = when {
        ExecutionModeRules.lane(mode, rootGranted, shizukuReady = false) == PrivilegedShellRules.Lane.ROOT ->
            ForegroundSource.SHELL
        usageAccess -> ForegroundSource.USAGE_STATS
        else -> ForegroundSource.NONE
    }
}

/** حدث واجهة أمامية مُجرَّد من `UsageEvents.Event`: [resumed] = `ACTIVITY_RESUMED`، وإلا `ACTIVITY_PAUSED`. */
data class ForegroundEvent(val packageName: String, val resumed: Boolean, val timeMs: Long)

/**
 * من الأحداث إلى التطبيق الأمامي — **نقيّ**. `""` = مجهول/لا شيء (نفس عقد `getForegroundPackage`).
 *
 * - استئناف حزمة ⇒ هي الأمامية.
 * - إيقاف الحزمة الأمامية الحالية ⇒ لا أمامي إلى أن يصل استئناف آخر؛ وإيقاف حزمة غيرها لا يغيّر شيئًا.
 * - **إعادة تطبيق أحداث سبق تطبيقها آمنة** (التداخل بين استعلامين متتاليين يحمي من حدث على حافة النافذة).
 */
object UsageForegroundRules {
    fun reduce(current: String, events: List<ForegroundEvent>): String {
        var now = current
        for (event in events.sortedBy { it.timeMs }) {
            if (event.resumed) {
                now = event.packageName
            } else if (event.packageName == now) {
                now = ""
            }
        }
        return now
    }
}
