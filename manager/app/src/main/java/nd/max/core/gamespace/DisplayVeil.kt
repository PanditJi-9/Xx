/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */
package nd.max.core.gamespace

/**
 * ستارة العرض — طبقة لون شفافة تُرسم فوق اللعبة كلها، **قواعد بلا أندرويد وبلا Compose**.
 *
 * ### وما تفعله بالضبط
 *
 * تخفّض ما يصل إلى العين: لون دافئ يقلّل الأزرق مساءً، وأخضر ليليّ أخفّ، وتعتيم محايد على مشهد
 * ساطع. كلها **لون واحد بشفافية واحدة**، ولا شيء غير ذلك.
 *
 * ### وما لا تفعله — ويُقال قبل أن يُجرَّب
 *
 * لا تزيد التباين ولا الحدّة ولا تشبّع الألوان؛ فذلك معالجة صورة لا تُنجزها طبقة لون، وادّعاؤها
 * كذبٌ يُكتشف في أول لعبة. ولذلك أيضاً لا تحمل أسماءً تُوحي بمعالجة: «دافئة» و«ليلية» و«تعتيم».
 *
 * ### والحدّ
 *
 * النافذة نفسها في طبقة الخدمة ([nd.max.service.DisplayVeilOverlay]) وتستعمل صلاحية «الرسم فوق
 * التطبيقات» القائمة، ولا تلمس عتادًا ولا تقرأ شيئًا. وهذا النوع هنا لأن **الاختيار والتدوير
 * يُقاسان بلا جهاز** — ما لا يمكن قياسه هو الشكل النهائي على شاشة حقيقية.
 *
 * @param argb اللون النهائي **بشفافيته**: القناة العليا نسبة التغطية، والباقي R·G·B.
 */
enum class DisplayVeilTone(val key: String, val argb: Int) {
    /** دافئ: يقلّل الأزرق ويرفع الأحمر قليلًا — للعب مساءً بإضاءة منخفضة. */
    WARM("WARM", 0x33FFB347),

    /** ليليّ: أخضر خفيف، أهدأ من الدافئ على شاشات OLED. */
    NIGHT("NIGHT", 0x2E66BB6A),

    /** تعتيم محايد: لا يغيّر اللون، يخفض السطوع الكلي. */
    DIM("DIM", 0x52000000);

    /** التالي في الدورة: دافئ ← ليليّ ← تعتيم ← دافئ. */
    fun next(): DisplayVeilTone = entries[(ordinal + 1) % entries.size]

    companion object {
        /** مفتاح مجهول يعود إلى الدافئ، ولا يُسقط الرسم. */
        fun fromKey(value: String?): DisplayVeilTone =
            entries.firstOrNull { it.key == value } ?: WARM
    }
}

/**
 * حالة الستارة: **مطفأة أو لون واحد**, ولا مصفوفة مقابض تُترك نصف مضبوطة.
 *
 * والدورة في [cycle] لا في الواجهة: نفس الدورة تُستعمل من بلاطة اللوحة ومن أي مدخل آخر، فتُقاس
 * باختبار واحد بدل أن تُنسخ شروطها في مكانين تختلفان بعد أول تعديل.
 */
data class DisplayVeilState(
    val enabled: Boolean = false,
    val tone: DisplayVeilTone = DisplayVeilTone.WARM
) {
    /** اللون المعروض فعلًا — `0` حين تكون مطفأة، فلا تُرسم طبقة بلا سبب. */
    val argb: Int get() = if (enabled) tone.argb else 0

    /**
     * دورة اللمسة الواحدة: مطفأة ← دافئة ← ليلية ← تعتيم ← مطفأة.
     *
     * ولماذا تُطفأ بعد التعتيم بدل أن تدور بلا نهاية: اللون الأخير هو الأثقل، ومن ضغط ثلاث
     * مرات يريد أن يعود إلى ما كان عليه لا أن يبدأ الدورة من جديد.
     */
    fun cycle(): DisplayVeilState = when {
        !enabled -> copy(enabled = true, tone = DisplayVeilTone.WARM)
        tone.next() == DisplayVeilTone.WARM -> copy(enabled = false)
        else -> copy(tone = tone.next())
    }
}
