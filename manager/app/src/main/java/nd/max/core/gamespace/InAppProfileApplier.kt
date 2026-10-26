/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.core.gamespace

import android.content.Context
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import nd.max.AppMonitorLogger
import nd.max.PerAppRefreshRateController
import nd.max.core.privilege.ExecutionModeStore
import nd.max.core.privilege.PrivilegeManager
import nd.max.core.privilege.PrivilegedShell

/**
 * مطبِّق بروفايل اللعبة **داخل خدمة اللوحة** حين لا يكون رفيق الجذر هو المسؤول ([InAppProfileRules.isActive]):
 * عند دخول لعبة تُؤخذ لقطة بالقيم الحالية، ثم تُكتب قيم البروفايل بالمسار الموحَّد، وعند الخروج تُعاد اللقطة.
 *
 * ثلاث ضمانات:
 * 1. **اللقطة تُحفظ قبل أول كتابة** (`commit`) فتنجو من موت العملية؛ وتُسترجع عند الإقلاع التالي ([recover]).
 * 2. **فشل الاسترجاع لا يُتبَع بتطبيق جديد**: وإلا لُقطت القيم المفروضة على أنها «الأصل» وصارت دائمة.
 * 3. **كل كتابة تُقرأ بعدها** ويُسجَّل «طُبِّق» أو «لم يُحقَّق» (نفس انضباط `AppMonitor`: لا ادّعاء بلا قراءة).
 *
 * **غير مقيس على جهاز:** قبول uid الـshell تحت Shizuku لـ`settings put` على `secure`/`system`/`global`.
 */
object InAppProfileApplier {
    private const val PREFS_NAME = "inapp_profile"
    private const val KEY_SAVED = "saved"
    private const val RETRY_MS = 10_000L

    private val mutex = Mutex()
    private var activePkg: String? = null
    private var retryNotBefore = 0L

    /** تُنادى من حلقة الاستطلاع بالحزمة الأمامية المتتبَّعة (أو `null`): ترخيص الدخول والخروج في مكان واحد. */
    suspend fun sync(context: Context, tracked: String?) {
        val app = context.applicationContext
        mutex.withLock {
            if (!InAppProfileRules.isActive(ExecutionModeStore.current(), PrivilegeManager.cachedRootGranted())) {
                if (activePkg != null) leave(app)
                return
            }
            if (tracked == activePkg) return
            if (activePkg != null) leave(app)
            if (tracked == null) return
            // مسار غير جاهز (Shizuku لم يتّصل بعد، أو لقطة قديمة تعذّر إعادتها): نعيد المحاولة بعد مهلة لا كل ثانية.
            if (android.os.SystemClock.uptimeMillis() < retryNotBefore) return
            if (enter(app, tracked)) {
                activePkg = tracked
            } else {
                retryNotBefore = android.os.SystemClock.uptimeMillis() + RETRY_MS
            }
        }
    }

    /** يسترجع ما بقي من جلسة سابقة ماتت قبل أن تُعيد قيمها. آمن للنداء في كل إقلاع للخدمة. */
    suspend fun recover(context: Context) {
        mutex.withLock { restoreLeftovers(context.applicationContext) }
    }

    /** عند إيقاف الخدمة: تُعاد القيم فورًا (وإن فشل بقيت اللقطة لـ[recover]). */
    suspend fun release(context: Context) {
        val app = context.applicationContext
        mutex.withLock { if (activePkg != null) leave(app) }
    }

    /** `true` = عولجت اللعبة (طُبِّق أو لا بروفايل أو تُخطّي بسبب)، و`false` = المسار غير جاهز فيُعاد لاحقًا. */
    private suspend fun enter(context: Context, pkg: String): Boolean {
        if (!restoreLeftovers(context)) return false
        if (GameProfileRepository.state.value.loading) GameProfileRepository.load()
        val config = GameProfileRepository.state.value.profiles[pkg] ?: return true
        val wants = InAppProfileRules.wants(config, PerAppRefreshRateController.supportedRates(context))
        if (!wants.any) return true

        val before = LinkedHashMap<SettingRef, String?>()
        for (ref in InAppProfileRules.refsFor(wants)) {
            val read = readSetting(ref)
            if (!read.ok) {
                AppMonitorLogger.w("EVENT=INAPP_PROFILE_SKIPPED pkg=$pkg reason=lane-unavailable")
                return false
            }
            before[ref] = read.value
        }
        val plan = InAppProfileRules.plan(wants, before)
        plan.skipped.forEach { (knob, reason) ->
            AppMonitorLogger.w("EVENT=INAPP_PROFILE_SKIPPED knob=$knob pkg=$pkg reason=$reason")
        }
        if (plan.writes.isEmpty()) return true

        // اللقطة قبل أول كتابة: لو ماتت العملية بعدها وجد الإقلاع التالي ما يُعيده.
        prefs(context).edit().putString(KEY_SAVED, InAppProfileRules.encodeSaved(pkg, plan.saved)).commit()
        var written = 0
        for (write in plan.writes) {
            val outcome = PrivilegedShell.exec(InAppProfileRules.putCommand(write.ref, write.value))
            if ((outcome as? PrivilegedShell.Outcome.Ran)?.ok != true) break
            written++
        }
        val verified = plan.writes.count { readSetting(it.ref).let { r -> r.ok && r.value == it.value } }
        AppMonitorLogger.w(
            "EVENT=INAPP_PROFILE_APPLIED pkg=$pkg writes=${plan.writes.size} written=$written verified=$verified"
        )
        return true
    }

    private suspend fun leave(context: Context) {
        restoreLeftovers(context)
        activePkg = null
    }

    /** `true` إن لم يبقَ شيء أو أُعيد كله؛ و`false` إن تعذّر شيء (تبقى اللقطة). */
    private suspend fun restoreLeftovers(context: Context): Boolean {
        val raw = prefs(context).getString(KEY_SAVED, null) ?: return true
        val decoded = InAppProfileRules.decodeSaved(raw)
        if (decoded == null) {
            // لقطة فاسدة لا تُنفَّذ أوامرها: تُسقَط ويُسجَّل.
            AppMonitorLogger.w("EVENT=INAPP_PROFILE_SNAPSHOT_CORRUPT")
            prefs(context).edit().remove(KEY_SAVED).commit()
            return true
        }
        var restoredAll = true
        for (command in InAppProfileRules.restoreCommands(decoded.second)) {
            val outcome = PrivilegedShell.exec(command)
            if ((outcome as? PrivilegedShell.Outcome.Ran)?.ok != true) restoredAll = false
        }
        if (restoredAll) prefs(context).edit().remove(KEY_SAVED).commit()
        AppMonitorLogger.w("EVENT=INAPP_PROFILE_RESTORED pkg=${decoded.first} complete=$restoredAll")
        return restoredAll
    }

    private class Read(val ok: Boolean, val value: String?)

    private suspend fun readSetting(ref: SettingRef): Read {
        val outcome = PrivilegedShell.exec(InAppProfileRules.getCommand(ref))
        val ran = outcome as? PrivilegedShell.Outcome.Ran
        if (ran == null || !ran.ok) return Read(false, null)
        return Read(true, InAppProfileRules.readValue(PanelShellOutput.lastLine(outcome)))
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
