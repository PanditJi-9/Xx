/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.core.gamespace

import nd.max.core.privilege.ExecutionMode
import nd.max.core.privilege.ExecutionModeRules
import nd.max.core.privilege.ExecutionModeStore
import nd.max.core.privilege.PrivilegeLevel
import nd.max.core.privilege.PrivilegedShellRules

/**
 * ميزات مساحة الألعاب التي تتغيّر إتاحتها بحسب الامتياز، وأدنى امتياز **تعمل به فعلًا اليوم** كل ميزة.
 *
 * القيم مأخوذة من الكود الحالي لا من الأمنيات (ADR-07): ما ينفَّذ عبر `PrivilegedShell.exec` يحتاج
 * [PrivilegeLevel.SHIZUKU] فما فوق، وما يزال يستدعي `Shell.cmd`/`RootFileAccess` مباشرةً أو يعتمد على
 * رفيق الجذر يحتاج [PrivilegeLevel.ROOT]. وحين يُنقل موضع إلى المسار الموحَّد يُخفَّض مطلوبه هنا في
 * الدفعة نفسها — فالجدول يعكس الواقع لا الخطة.
 *
 * ما لا يحتاج امتيازًا ([PrivilegeLevel.NONE]) يعتمد على أذونات خاصة يمنحها المستخدم نفسه (تراكب).
 *
 * **غير مقيس على جهاز:** أيّ الأوامر يقبله uid الـshell فعلًا تحت Shizuku (`svc` · `screencap` · `screenrecord` ·
 * `wm` · `settings put global`)؛ والجدول مبنيّ على أن الأمر يمرّ بالمسار الموحَّد لا على أن Shizuku يقبله.
 */
enum class GameFeature(val requires: PrivilegeLevel) {
    /** التصويب العائم: نافذة تراكب فقط. */
    CROSSHAIR_OVERLAY(PrivilegeLevel.NONE),

    /** `svc wifi` عبر المسار الموحَّد. */
    WIFI_TOGGLE(PrivilegeLevel.SHIZUKU),

    /** `svc data` عبر المسار الموحَّد. */
    MOBILE_DATA_TOGGLE(PrivilegeLevel.SHIZUKU),

    /**
     * «عدم الإزعاج» عبر `cmd notification set_dnd` (‏Android 11+) على المسار الموحَّد، أو
     * `NotificationManager.setInterruptionFilter` إن مُنحت صلاحية سياسة الإشعارات.
     * و`settings put global zen_mode` **ليس** مسارًا: `ZenModeHelper` يردّه (انظر `DndControl`).
     */
    DND_TOGGLE(PrivilegeLevel.SHIZUKU),

    /** `screencap` عبر المسار الموحَّد. */
    SCREENSHOT(PrivilegeLevel.SHIZUKU),

    /** `screenrecord` عبر المسار الموحَّد (استمرار العملية الخلفية تحت Shizuku غير مقيس على جهاز). */
    SCREEN_RECORD(PrivilegeLevel.SHIZUKU),

    /** `wm size/density` عبر المسار الموحَّد. */
    DISPLAY_SCALE(PrivilegeLevel.SHIZUKU),

    /** فتح تطبيق كنافذة عائمة: ما زال بصدفة جذر (`RootShell`). */
    FLOATING_WINDOW(PrivilegeLevel.ROOT),

    /** تحرير الذاكرة (`drop_caches`/`compact`): جذر. */
    MEMORY_FREE(PrivilegeLevel.ROOT),

    /** دفعة اللمس: عقد مصنّع/خصائص — جذر. */
    TOUCH_BOOST(PrivilegeLevel.ROOT),

    /** منع الشحن (bypass): sysfs — جذر. */
    BYPASS_CHARGING(PrivilegeLevel.ROOT),

    /** سقوف CPU/GPU وقراءة ترددات اللوحة: sysfs — جذر. */
    CLOCK_CONTROL(PrivilegeLevel.ROOT),

    /** بروفايل كل لعبة التلقائي: يطبّقه `AppMonitor` (رفيق الجذر). */
    PROFILE_AUTOMATION(PrivilegeLevel.ROOT),
}

/** إتاحة الميزات بحسب الوضع المختار والامتياز الممنوح — **نقيّة** فتُقاس على JVM. */
object GameCapabilities {

    /** لماذا لا تعمل الميزة: [MODE_EXCLUDES] = الجهاز يملك الامتياز لكن الوضع المختار يستبعده. */
    enum class Cause { MODE_EXCLUDES, NOT_GRANTED }

    sealed interface Availability {
        data object Available : Availability

        /** [requires] أدنى امتياز تحتاجه، و[cause] يقول هل الحل «بدّل الوضع» أم «امنح الامتياز». */
        data class Unavailable(val requires: PrivilegeLevel, val cause: Cause) : Availability
    }

    fun availability(
        feature: GameFeature,
        mode: ExecutionMode,
        rootGranted: Boolean,
        shizukuReady: Boolean,
    ): Availability {
        val effective = ExecutionModeRules.level(mode, rootGranted, shizukuReady)
        if (effective.atLeast(feature.requires)) return Availability.Available
        // أعلى ما يوفّره الجهاز بصرف النظر عن الوضع المختار.
        val offered = when {
            rootGranted -> PrivilegeLevel.ROOT
            shizukuReady -> PrivilegeLevel.SHIZUKU
            else -> PrivilegeLevel.NONE
        }
        val cause = if (offered.atLeast(feature.requires)) Cause.MODE_EXCLUDES else Cause.NOT_GRANTED
        return Availability.Unavailable(feature.requires, cause)
    }

    fun all(mode: ExecutionMode, rootGranted: Boolean, shizukuReady: Boolean): Map<GameFeature, Availability> =
        GameFeature.entries.associateWith { availability(it, mode, rootGranted, shizukuReady) }
}

/**
 * حال الوضع المختار الآن — هل يعمل، أم ينتظر امتيازًا لم يُمنح؟ **نقيّة**، والواجهة تترجمها إلى جملة.
 * ويفرّق «اخترتَ Root ولم يُمنح» من «لا امتياز أصلًا في التلقائي» لأن الحلّ مختلف.
 */
enum class GameModeStatus {
    ACTIVE_ROOT,
    ACTIVE_SHIZUKU,
    ACTIVE_BASIC,
    NEEDS_ROOT,
    NEEDS_SHIZUKU,
    NO_PRIVILEGE,
    ;

    companion object {
        fun of(mode: ExecutionMode, rootGranted: Boolean, shizukuReady: Boolean): GameModeStatus =
            when (ExecutionModeRules.lane(mode, rootGranted, shizukuReady)) {
                PrivilegedShellRules.Lane.ROOT -> ACTIVE_ROOT
                PrivilegedShellRules.Lane.SHIZUKU -> ACTIVE_SHIZUKU
                null -> when (mode) {
                    ExecutionMode.ROOT -> NEEDS_ROOT
                    ExecutionMode.SHIZUKU -> NEEDS_SHIZUKU
                    ExecutionMode.BASIC -> ACTIVE_BASIC
                    ExecutionMode.AUTO -> NO_PRIVILEGE
                }
            }
    }
}

/**
 * بوّابة الجذر لمواضع مساحة الألعاب التي ما زالت تستدعي صدفة الجذر مباشرةً (`PanelControls` · `PanelDevice` ·
 * `PanelFloating` · دفعة اللمس): في وضع Shizuku وBasic تُرفض بدل أن «تنجح» لأن الجذر ممنوح بالصدفة.
 * وحين يُنقل موضع إلى `PrivilegedShell.exec` تسقط حاجته إلى هذه البوّابة (الوضع يحكمه المسار نفسه).
 */
internal object GameRootGate {
    fun allowed(): Boolean = ExecutionModeRules.mayUseRoot(ExecutionModeStore.current())
}
