/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.maxai

/**
 * حكم نقيّ على فرق «مطلوب/حيّ» لمقابض المدى (`min:max`): هل هو قمع منصّة أم انحدار حقيقي؟
 *
 * والدافع مقيس في حزم المالك (rodin/MT6899): كل استرجاع خط أساس على `cpu_limits:policyN`
 * كان يُسجَّل `regression rollback … :: FAILED` بمستوى ERROR، وفي ٦ من ٧ كان المقروء
 * **خطوةً واحدة أدنى على السلّم المُعلن** من المكتوب — أي أن سلطة المنصّة (حرارة MI/MTK)
 * تمسك سقفًا أدنى، لا أن كتابتنا فشلت. والحالتان كانتا تُطبعان بالكلمة نفسها، فيزداد عدّاد
 * مشاكل الواجهة بعطب ليس عطبنا، وتُعاد الكتابة كل دورة فتُشغَّل سياسة التردد في النواة
 * (كلفة حرارة ولاغ) بلا فائدة ممكنة.
 *
 * **ما تفعله هذه الدوال — ولا شيء غيره:** تصنيف نصّين. **لا** تقرأ عتادًا، **ولا** تعرف
 * أي سلطة قائمة — فـ[CLAMPED_BELOW] «قمع محتمل» لا حكم باتّ: البتّ للمتصل الذي يملك
 * قراءة السلطة. وما ليس مدى (`governor` …) يُطابق نصًّا أو يُعَدّ [DIVERGED] — أي أن
 * المصنّف لا يُليّن إلا حالة المدى المقموع وحده، وسلوك البقية كما كان.
 */
object ClampVerdict {

    /** تطابق · قمع محتمل من المنصّة (سقف حيّ أدنى والأرضية نفسها) · أي فرق آخر. */
    enum class Verdict {
        MATCHED,
        CLAMPED_BELOW,
        DIVERGED,
    }

    /**
     * تصنيف خالص، مُختبر بلا جهاز.
     *
     * @param wanted ما طُلب (`min:max` لمقابض المدى، أو أي نصّ لغيرها).
     * @param live ما قُرئ حيًّا (`null` = لا قراءة).
     */
    fun classify(wanted: String?, live: String?): Verdict {
        if (wanted == null || live == null) return Verdict.DIVERGED
        if (wanted == live) return Verdict.MATCHED
        val w = parseRange(wanted) ?: return Verdict.DIVERGED
        val l = parseRange(live) ?: return Verdict.DIVERGED
        return if (l.first == w.first && l.second < w.second) Verdict.CLAMPED_BELOW
        else Verdict.DIVERGED
    }

    /** `min:max` عدديّان، أو `null` حين ليس مدى — فلا يُصنَّف غيرُ المدى قمعًا أبدًا. */
    private fun parseRange(value: String): Pair<Long, Long>? {
        val parts = value.trim().split(":")
        if (parts.size != 2) return null
        val min = parts[0].toLongOrNull() ?: return null
        val max = parts[1].toLongOrNull() ?: return null
        return min to max
    }
}
