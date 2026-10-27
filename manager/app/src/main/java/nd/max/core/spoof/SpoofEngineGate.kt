/* Copyright (C) 2026 Nader Magdy. All rights reserved. */
package nd.max.core.spoof

/**
 * بوّابة المحرّك: **قرارٌ نقيّ من قياسين**، لا قراءة عقدة ولا كتابة.
 *
 * لماذا وُجدت
 * -----------
 * تصدير المالك (٢٠٢٦-١٠-٠٦ ١٩:٢٠) حمل سطرين فقط عن كلّ محاولة تجهيز:
 *
 * ```
 * EVENT=OP_RESULT screen=SpoofPerApp action=apply ok=false duration_ms=427
 * W diag: apply refused: ENGINE_UNAVAILABLE
 * ```
 *
 * و`ENGINE_UNAVAILABLE` رمزٌ واحد كان يخلط **أربع حالات علاجها مختلف تمامًا**: مجلّد وحدةٍ غائب،
 * أو وحدةٌ معطَّلة في مدير الجذر، أو محذوفة تنتظر إقلاعًا، أو محدَّثة تنتظر إقلاعًا. و`COPG`
 * **مثبَّتة فعلًا** في ذلك التصدير (`[other installed modules]` تسرد `COPG`) — فالرمز لم يكن يقول
 * حتى «الوحدة غائبة» بل «شيءٌ ما في الوحدة». وهذا عطب تشخيص، لا عطب قدرة: من يقرأ الملفّ وحده
 * لا يعرف أيّ إجراء يلزمه.
 *
 * وقاعدتها
 * --------
 * * **الاسم هو الدليل.** كلُّ سبب يخرج من هنا يُسمّى بحالته المقيسة، فيُقرأ في الشاشة وفي التصدير
 *   بلا ترجمة ولا تخمين.
 * * **فشلٌ مغلق.** القياس المجهول (ملفّ لا يُقرأ، سطر `id=` غائب) لا يُعامل «متاح» أبدًا.
 * * **والترتيب مقصود:** العلامة `disable` تتقدّم على `remove` وعلى `update`، لأن وحدةً معطَّلة لا
 *   يُصلحها انتظارُ إقلاع: أوّل ما يُفعل تفعيلها.
 *
 * وما لا يفعله هذا الملفّ: لا مسار، ولا أمر صدفة، ولا كتابة. المسارات تُقاس في
 * [SpoofConfigTransaction]، وهذه الدوالّ تحكم على القياس وحده — فتُقاس بذاتها في اختبار نقيّ.
 */
internal object SpoofEngineGate {

    /** علامة يكتبها مدير الجذر بجانب الوحدة: الوحدة معطَّلة. */
    const val DISABLE: String = "disable"

    /** علامة: الوحدة في طابور الحذف، ويُنفَّذ عند الإقلاع. */
    const val REMOVE: String = "remove"

    /** علامة: تحديثٌ للوحدة لم يُطبَّق بعد، ويُنفَّذ عند الإقلاع. */
    const val UPDATE: String = "update"

    /**
     * العلامات الثلاث بأولوية القراءة لا بترتيب أبجديّ.
     *
     * وهي **مصدر واحد** لاسم الملفّ: كان الاسم مكتوبًا داخل شرط `eligibility` نصًّا، فكلّ موضع
     * يسأل عن علامةٍ كان ينسخ اسمها. ومن هنا يقرؤها القياس والاختبار معًا.
     */
    val markers: List<String> = listOf(DISABLE, REMOVE, UPDATE)

    /**
     * هل الوحدة تُعرّف عن نفسها بأنها [engineId]؟ `null` يعني نعم (لا مانع من هذه الجهة).
     *
     * و`module.prop` هو تعريف الوحدة بنفسها كما يقرؤه مدير الجذر، وسطر `id=` فيه هو المعرّف لا
     * الاسم المعروض. **والغائب ليس نفيًا ولا إثباتًا** بل «لا تُعرّف نفسها»: فملفٌّ غير موجود أو
     * غير مقروء أو بلا سطر `id=` يخرج بـ[`ENGINE_MODULE_ABSENT`][SpoofEngineReason.ENGINE_MODULE_ABSENT]
     * — لا يُفترض أن الوحدة هي المطلوبة لأن مجلّدًا باسمها موجود.
     */
    fun identityReason(moduleProp: String?, engineId: String): SpoofEngineReason? =
        if (moduleProp?.lineSequence()?.any { it.trim() == "id=$engineId" } == true) null
        else SpoofEngineReason.ENGINE_MODULE_ABSENT

    /**
     * العلامات **المقيسة** ⇒ سببها، أو `null` حين لا علامة (الوحدة سليمة من هذه الجهة).
     *
     * والمقيس هو ما وُجد فعلًا لا ما سُئل عنه: القياس يمرّر مجموعةً فارغة حين لا علامة، وحين
     * يعجز عن السؤال **لا** يمرّر «لا علامة» — ذاك فرقٌ بين «قِيس فلم يُوجد» و«لم يُقَس».
     */
    fun markerReason(present: Set<String>): SpoofEngineReason? = when {
        DISABLE in present -> SpoofEngineReason.ENGINE_DISABLED
        REMOVE in present -> SpoofEngineReason.ENGINE_REMOVAL_PENDING
        UPDATE in present -> SpoofEngineReason.ENGINE_UPDATE_PENDING
        else -> null
    }

    /**
     * تثبيتٌ أو تحديثٌ جديد يعيش في `modules_update` حتى الإقلاع — **فلا يُحكم بالغياب**.
     *
     * كان `eligibility` يقرأ `/data/adb/modules/<id>/module.prop` وحده، فوحدةٌ ثُبِّتت للتوّ
     * (وهي في `modules_update` بالتصميم) تُقرأ «غائبة» وتُطلب إعادة تثبيتٍ لا معنى لها. وهذا
     * مقيس من تصميم مديري الجذر: المجلّد المرحليّ يُدمج عند الإقلاع، والملفّ يُقرأ الآن.
     *
     * وحدّه: [stagedModuleProp] بلا سطر `id=` مطابق **ليس** دليلًا على أن محرّكنا هو المرحّل
     * (قد تكون وحدة أخرى في الطابور)، فيُترك الحكم للسبب الأصليّ. والوسيط `null` يعني
     * «لم يُقرأ» لا «غير موجود»؛ وكلاهما لا ينتج سببًا هنا.
     */
    fun stagedReason(stagedModuleProp: String?, engineId: String): SpoofEngineReason? =
        if (stagedModuleProp != null && identityReason(stagedModuleProp, engineId) == null) {
            SpoofEngineReason.ENGINE_UPDATE_PENDING
        } else {
            null
        }

    /** قيمة سطر `id=` الأول (بعد `=`)، أو `null` إن لم يُقرأ السطر. */
    fun idValue(moduleProp: String?): String? = moduleProp?.lineSequence()
        ?.map { it.trim() }
        ?.firstOrNull { it.startsWith("id=") }
        ?.substringAfter('=')
        ?.takeIf { it.isNotEmpty() }

    /**
     * تشخيص المحرّك: **ما قيس فعلًا**، لا استنتاجًا من رمز الرفض.
     *
     * سؤال المالك المتكرّر «لماذا ظهرت الرسالة؟» كان بلا جواب في التصدير: الرمز يقول
     * `ENGINE_MODULE_ABSENT` ولا يقول أيّ شرطٍ من شروطه الأربعة سقط. والسطر هنا يُجيب بالحقول
     * نفسها التي حكمت: الجذر، وجود المجلّد، قراءة `module.prop`، سطر `id=`, وحالة التثبيت
     * المرحليّ. وهو **للقراءة فقط** ولا يغيّر أي حكم.
     */
    fun probeLine(probe: EngineProbe): String = buildString {
        append("EVENT=SPOOF_ENGINE_PROBE engine=").append(safe(probe.engine))
        append(" reason=").append(probe.reason?.name ?: "OK")
        append(" root=").append(if (probe.rootGranted) 1 else 0)
        append(" dir=").append(if (probe.moduleDirPresent) 1 else 0)
        append(" prop=").append(if (probe.modulePropReadable) 1 else 0)
        append(" id=").append(probe.moduleIdValue?.let(::safe) ?: "none")
        append(" staged_dir=").append(if (probe.stagedDirPresent) 1 else 0)
        append(" staged_prop=").append(if (probe.stagedPropReadable) 1 else 0)
        append(" staged_id=").append(if (probe.stagedIdMatches) 1 else 0)
    }

    /** قيمة دخيلة (سطر `module.prop`) لا تُكسر سطر السجل ولا تحمل فراغًا أو اقتباسًا. */
    private fun safe(value: String): String =
        value.replace(Regex("[^A-Za-z0-9_.:-]"), "_").take(64)
}

/** قياسات المحرّك كما قرأها [SpoofConfigTransaction] — لا حكم فيها بذاتها. */
internal data class EngineProbe(
    val engine: String,
    val reason: SpoofEngineReason?,
    val rootGranted: Boolean,
    val moduleDirPresent: Boolean = false,
    val modulePropReadable: Boolean = false,
    val moduleIdValue: String? = null,
    val stagedDirPresent: Boolean = false,
    val stagedPropReadable: Boolean = false,
    val stagedIdMatches: Boolean = false,
)
