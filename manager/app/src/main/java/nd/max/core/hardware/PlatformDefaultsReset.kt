/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.core.hardware

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nd.max.MaxManagerPaths
import nd.max.MaxManagerProps
import nd.max.core.platform.PropertyUtils

/**
 * «استعادة افتراضي المنصّة» — الفعل الواحد الذي يمحو **تعديلاتنا** عن العتاد والملفّات المحفوظة.
 *
 * ### العطب الذي يُصلحه (تقرير SHARK)
 *
 * إزالة الوحدة ليست استعادة: مسار إزالة KernelSU لم يكن يمحو `persist.sys.maxmanager*` ولا
 * `/data/adb/.config/MaxManager`، وفي الإقلاع التالي يقرأ الخادم `API/max_ai_mode` و
 * `API/current_profile` ويعيد تطبيق ملفّ النسخة السابقة. ومعها `binprofiles` يعيد كتابة حدود
 * CPU كلّما تغيّر التطبيق الأماميّ، لأن الحارس الوحيد `sys.maxmanager.manual_freq_session` غير
 * دائم ويُصفَّر عند الإقلاع. فيبقى المستخدم «عالقًا على ملفّ النسخة القديمة» بلا باب يخرج منه
 * غير إعادة التثبيت — وهذا الباب.
 *
 * ### ما يفعله بالترتيب، ولماذا بهذا الترتيب
 *
 * 1. **الأقفال اليدوية** تُرفع أولًا: قفلٌ باقٍ يمنع المالكين الآليّين من الكتابة، والاستعادة
 *    هي عكس القفل لا طلبٌ يمرّ بجانبه.
 * 2. **حدود CPU** تُعاد إلى `provenMinKHz`/`provenMaxKHz` لكل سياسة — الحدّ الذي يعلنه العتاد
 *    نفسه، لا رقمًا نخترعه. وهو الفرق بين «استعادة» و«قيمة أخرى نختارها».
 * 3. **راية الجلسة اليدوية** تُصفَّر قبل أي شيء يعيد الملفّ: `binprofiles` يتخطّى كتابة CPU
 *    متى كانت `1`، فبقاؤها يجعل «الاستعادة» لا تصل إلى العتاد أصلًا.
 * 4. **التفضيلات المحفوظة** (`CUSTOM_*` و`freqoffset`) تُمحى: هذه هي التي يقرؤها ملفّ
 *    `balanced` فيعيد حاكم النسخة السابقة عند كل إقلاع.
 * 5. **GPU** يُحرَّر من قفل OPP الثابت ويُعاد إلى DVFS ثم تُمحى مفاتيحه المحفوظة. وإن فشل
 *    التحرير تبقى المفاتيح مُقاسة الخطأ في التقرير ولا يُقال «تمّ».
 * 6. **الملفّ العام** يعود `balanced` (٢) في **النيّة** (`API/max_ai_mode`) و**الفاعل**
 *    (`API/current_profile`) معًا. أحدهما وحده لا يكفي: قراءة `current_profile` على أنها نيّة
 *    المستخدم هي بالضبط ما جعل وضع اللعب يصير «الاختيار» في الإقلاع التالي.
 *
 * ### حدوده المعلنة
 *
 * - لا يمسّ اختيارات التطبيق ولا ملفّات المستخدم: ذلك في `resetAppToDefaults` وحده.
 * - ولا يمسّ حالة Max AI (مفعّل/مطفأ) ولا حرارة/شبكة/لمس: قدرة أخرى، لا «استعادة عتاد».
 * - **وفشل خطوة لا يُسقط البقية**: كل خطوة تُحاوَل ثم يُقال ما نجح وما فشل. من يعيد النجاح
 *   الكامل بلا قياس يكذب على المستخدم في أخطر موضع — عتاد قد يبقى مجمّدًا.
 */
object PlatformDefaultsReset {

    /** مفتاح `freqoffset` كما يقرؤه `binprofiles.get_limiter()`: `Disabled` تعني ١٠٠٪. */
    private const val LIMITER_DISABLED = "Disabled"

    private val CUSTOM_GOVERNOR_KEYS = listOf(
        MaxManagerProps.Governor.CPU_CUSTOM_DEFAULT,
        MaxManagerProps.Governor.CPU_CUSTOM_PERFORMANCE,
        MaxManagerProps.Governor.CPU_CUSTOM_POWERSAVE,
        MaxManagerProps.Governor.IO_CUSTOM_DEFAULT,
        MaxManagerProps.Governor.IO_CUSTOM_PERFORMANCE,
        MaxManagerProps.Governor.IO_CUSTOM_POWERSAVE,
        MaxManagerProps.Governor.MALIGPU_CUSTOM_DEFAULT,
        MaxManagerProps.Governor.MALIGPU_CUSTOM_PERFORMANCE,
        MaxManagerProps.Governor.MALIGPU_CUSTOM_POWERSAVE,
    )

    /** خطوة واحدة: اسمها كما يُعرَض، ونجاحها المقيس، وسبب فشلها حين تفشل. */
    data class Step(val name: String, val ok: Boolean, val reason: String? = null)

    data class Outcome(val steps: List<Step>) {
        /** الخطوات الفاشلة وحدها — هي ما يُقال للمستخدم؛ النجاح لا يُسرد في تحذير. */
        val failed: List<Step> get() = steps.filter { !it.ok }
        val allOk: Boolean get() = failed.isEmpty()
    }

    suspend fun reset(): Outcome = withContext(Dispatchers.IO) {
        val steps = mutableListOf<Step>()
        steps += Step("locks", liftManualLocks())
        steps += Step("cpu-ranges", resetCpuRanges())
        steps += Step("manual-session", clearManualSessionFlag())
        steps += Step("saved-overrides", clearSavedOverrides())
        steps += Step("gpu", resetGpuHardware())
        val gpuKeysCleared = runCatching { GpuTweakPersistence.clearSaved() }.getOrDefault(false)
        steps += Step(
            "gpu-saved",
            gpuKeysCleared,
            "gpu-keys-not-cleared".takeIf { !gpuKeysCleared },
        )
        steps += Step("profile", resetProfile())
        Outcome(steps)
    }

    private fun liftManualLocks(): Boolean = runCatching {
        ManualControlLocks.clearAll()
        ManualControlLocks.lockedKeys().isEmpty()
    }.getOrDefault(false)

    /**
     * حدود كل سياسة إلى ما يعلنه العتاد. و`proven*` قد تكون `null` على سياسة لم تُقرأ، فتُعدّ
     * فاشلةً بصراحة: الصمت عنها يجعل «الاستعادة» تُنجح جهازًا لم تُقرأ منه سياسة واحدة.
     */
    private fun resetCpuRanges(): Boolean {
        val policies = CpuHardwareBackend.policies()
        if (policies.isEmpty()) return false
        return policies.all { policy ->
            val min = policy.provenMinKHz ?: return@all false
            val max = policy.provenMaxKHz ?: return@all false
            CpuHardwareBackend.setPolicyLimits(policy.path, min, max).verified
        }
    }

    /** الراية غير دائمة، لكنها قد تكون `1` الآن — و`binprofiles` يتخطّى الكتابة متى كانت كذلك. */
    private fun clearManualSessionFlag(): Boolean = runCatching {
        val key = MaxManagerProps.CoreControl.MANUAL_FREQ_SESSION
        PropertyUtils.get(key) != "1" || PropertyUtils.setAndConfirm(key, "0")
    }.getOrDefault(false)

    /**
     * مفاتيح التفضيل المحفوظة: تُكتب `\"\"` فقط إن كانت تحمل قيمة. كتابة الفراغ على مفتاح
     * غير موجود تفشل على نظام يرفض المفتاح الجديد، فتُقرأ «استعادة فاشلة» بلا عطب حقيقي.
     */
    private fun clearSavedOverrides(): Boolean {
        val governorsCleared = CUSTOM_GOVERNOR_KEYS.all { key ->
            val current = PropertyUtils.get(key)
            current.isEmpty() || PropertyUtils.setAndConfirm(key, "")
        }
        val limiter = PropertyUtils.get(MaxManagerProps.Conf.FREQ_OFFSET)
        val limiterCleared = limiter.isEmpty() || limiter == LIMITER_DISABLED ||
            (
                PropertyUtils.setAndConfirm(MaxManagerProps.Conf.FREQ_OFFSET, LIMITER_DISABLED) &&
                    // ملفّ الحدّ يقرؤه من لا يقرأ الخصائص؛ إبقاؤه على نسبة قديمة يناقض الخصيصة.
                    RootFileAccess.write("${MaxManagerPaths.MODULE_CONFIG}/freqoffset", LIMITER_DISABLED)
                )
        return governorsCleared && limiterCleared
    }

    /**
     * مسار GPU كما هو في `GpuHardwareBackend`: تحرير القفل الثابت، وإرجاع DVFS، ثم كتابة المدى.
     * ثم تُقرأ النتيجة لا يُفترض نجاحها.
     */
    private fun resetGpuHardware(): Boolean = runCatching {
        GpuHardwareBackend.resetToPlatformDefaults()
    }.getOrDefault(false)

    /**
     * الملفّ العام: النيّة ثم الفاعل.
     *
     * والنيّة تُكتب عبر `ProfileApplier` لا بكتابة ملفّ مباشرة — هي الطبقة الوحيدة المسموح لها
     * بذلك (حرس `ControlPlaneArchitectureTest`)، والكتابة بيد ثانية هنا تُنشئ مالكًا ثانيًا للحالة.
     */
    private fun resetProfile(): Boolean {
        val intentWritten = ProfileApplier.persistMaxAiMode(ProfileApplier.PROFILE_BALANCED) &&
            RootFileAccess.read(MaxManagerPaths.MAX_AI_MODE)?.trim() == ProfileApplier.PROFILE_BALANCED
        // المرآة القديمة (`AIobjective`) يُعاد ضبطها مع النيّة، وإلا قرأها مسار الاحتياط عند
        // غياب `max_ai_mode` فأعاد هدف النسخة السابقة.
        val mirror = PropertyUtils.get(MaxManagerProps.Conf.AI_OBJECTIVE)
        val mirrorOk = mirror.isEmpty() || mirror == "balanced" ||
            PropertyUtils.setAndConfirm(MaxManagerProps.Conf.AI_OBJECTIVE, "balanced")
        if (!intentWritten || !mirrorOk) return false
        // الفاعل: إن كان `2` أصلًا فلا حاجة لنداء الخادم؛ وإلا يُطبَّق ويُقرأ.
        val active = ProfileApplier.currentProfile()?.trim()
        if (active == ProfileApplier.PROFILE_BALANCED) return true
        if (!ProfileApplier.apply(ProfileApplier.PROFILE_BALANCED)) return false
        return ProfileApplier.currentProfile()?.trim() == ProfileApplier.PROFILE_BALANCED
    }
}
