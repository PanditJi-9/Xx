/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.hardware

/**
 * نواة محرّك الوصفات: تحوّل وصفة نصّية إلى **خطوات قابلة للتطبيق أو مرفوضة بسببها**، قبل أي كتابة.
 *
 * **ولماذا التخطيط فصلًا عن الكتابة:** الكتابة إلى عقدة نواة بلا قرار مسبق هي الطريقة الأولى لإدخال
 * الجهاز في حلقة إقلاع. فالمحرّك هنا لا يفتح ملفًا ولا يكتب بايتًا؛ يقرأ كتالوج التحكّم (المصدر
 * الوحيد للمفاتيح والمسارات)، ويحسم لكل سطر: هل المفتاح معروف؟ هل له مسار؟ هل الكتابة مسموحة أصلًا
 * بحكم التصنيف؟ هل القيمة من نوعه؟ والمخرج `Plan` يُعرض للمستخدم ثم يمرّ بالـarbiter (ADR-11) عند
 * التطبيق — وهذه النواة لا تتجاوزه ولا تُغني عنه.
 *
 * **وحدّها:** لا تتحقّق من وجود العقدة على الجهاز (ذلك `NodeCatalogReader`)، ولا من الصلاحية الفعلية
 * (ذلك `PrivilegeManager`). وحكمها **بنيويّ** لا سلوكيّ: «هذا مدخل صالح للكتابة» لا «هذه الكتابة ستنجح».
 */
object RecipePlan {

    /** حكم كل سطر. الأسباب مختلفة فيُعرض السبب، ولا يُخفى السطر المرفوض. */
    enum class Verdict { READY, UNKNOWN_KEY, NO_NODE, NOT_WRITABLE, PROTECTED, BAD_VALUE }

    data class Entry(
        val key: String,
        val transport: String,
        val candidates: List<String>,
        val writePrivilege: List<String>,
        val danger: String,
        val kind: String,
    )

    data class Step(
        val key: String,
        val value: String,
        val path: String?,
        val verdict: Verdict,
    )

    /**
     * خطة كاملة. `applicable` تعني: كل سطر مقبول، وسطر واحد على الأقل. أي رفض ⇒ لا تطبيق جزئيّ،
     * لأن نصف وصفة قد يترك الجهاز في حالة لم تُقصَد.
     */
    data class Plan(val steps: List<Step>) {
        val ready: List<Step> get() = steps.filter { it.verdict == Verdict.READY }
        val blocked: List<Step> get() = steps.filter { it.verdict != Verdict.READY }
        val applicable: Boolean get() = steps.isNotEmpty() && blocked.isEmpty()
    }

    /** السطر: `key=value`، ويُهمَل الفراغ والسطر المعلَّق بـ`#`. */
    fun parse(recipe: String): List<Pair<String, String>> = recipe.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") }
        .mapNotNull { line ->
            val at = line.indexOf('=')
            if (at <= 0 || at == line.lastIndex) null
            else line.substring(0, at).trim() to line.substring(at + 1).trim()
        }
        .filter { (key, value) -> key.isNotEmpty() && value.isNotEmpty() }
        .toList()

    fun plan(recipe: String, catalog: List<Entry>): Plan {
        val byKey = catalog.associateBy { it.key }
        return Plan(
            parse(recipe).map { (key, value) ->
                val entry = byKey[key]
                when {
                    entry == null -> Step(key, value, null, Verdict.UNKNOWN_KEY)
                    entry.danger == "PROTECTED" -> Step(key, value, null, Verdict.PROTECTED)
                    entry.writePrivilege.isEmpty() -> Step(key, value, null, Verdict.NOT_WRITABLE)
                    entry.candidates.isEmpty() -> Step(key, value, null, Verdict.NO_NODE)
                    !valueFits(entry.kind, value) -> Step(key, value, null, Verdict.BAD_VALUE)
                    else -> Step(key, value, entry.candidates.first(), Verdict.READY)
                }
            }
        )
    }

    /**
     * فحص القيمة بنوع المدخل. القبول واسع عن قصد للأنواع النصّية، وضيّق حيث الخطأ يعني كتابة رقمية
     * خارج المدى — لأن العقدة قد تقبلها وتُفسد السلوك بدل أن ترفضها.
     */
    internal fun valueFits(kind: String, value: String): Boolean = when (kind) {
        "BOOL" -> value in setOf("0", "1", "true", "false")
        "INT" -> value.toLongOrNull() != null
        "PERCENT" -> value.toIntOrNull()?.let { it in 0..100 } == true
        "FREQUENCY_RANGE" -> value.toLongOrNull()?.let { it > 0L } == true
        else -> value.isNotBlank()
    }
}
