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
    enum class Verdict { READY, UNKNOWN_KEY, NO_NODE, NOT_WRITABLE, PROTECTED, BAD_VALUE, TEMPLATED }

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
        .mapNotNull(::resolveLine)
        .toList()

    /**
     * عدد الأسطر التي **لم تُفهم**: غير فارغة، غير معلّقة، ولم تُحلّ إلى `key=value`.
     *
     * **ولماذا يُعدّ بدل أن يُسكَت عنه:** [parse] تُسقط ما لا تفهمه، وهو الصواب للمحرّك (سطر
     * معطوب لا يمنع سطرًا صحيحًا). لكن سطحًا يقول «٣ أسطر جاهزة» عن نصّ فيه خمسة أسطر يوهم أن
     * السطرين الآخرين طُبِّقا. فالعدد **للعرض وحده**، ولا يدخل حكم [plan] ولا يمنع تطبيقًا.
     */
    fun ignored(recipe: String): Int = recipe.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") }
        .count { resolveLine(it) == null }

    /** حسم سطر واحد إلى زوج، أو `null` إن لم يكن `key=value` مكتملًا. */
    private fun resolveLine(line: String): Pair<String, String>? {
        val at = line.indexOf('=')
        if (at <= 0 || at == line.lastIndex) return null
        val key = line.substring(0, at).trim()
        val value = line.substring(at + 1).trim()
        return if (key.isEmpty() || value.isEmpty()) null else key to value
    }

    fun plan(recipe: String, catalog: List<Entry>): Plan {
        val byKey = catalog.associateBy { it.key }
        return Plan(
            parse(recipe).map { (key, value) ->
                val entry = byKey[key]
                when {
                    entry == null -> Step(key, value, null, Verdict.UNKNOWN_KEY)
                    entry.danger == "PROTECTED" -> Step(key, value, null, Verdict.PROTECTED)
                    entry.writePrivilege.isEmpty() -> Step(key, value, null, Verdict.NOT_WRITABLE)
                    // مفتاح بقالب (`cpu_limits:{policy}`) يصف **عائلة** عقد لا عقدة؛ واختيار أول
                    // مرشّح له حكمٌ بلا أساس: قد يكون مجلّدًا، أو عقدة قراءة، أو مقبض تعزيز لا حدًّا.
                    // فيُرفض بسببٍ مسمّى بدل أن تُكتب قيمة إلى عقدة لم يخترها أحد.
                    isTemplated(entry.key) -> Step(key, value, null, Verdict.TEMPLATED)
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
    /**
     * مفتاح قالبي؟ القالب يعني «اختر جهازًا/سياسة» وهذا الاختيار ليس من عمل المحرّك.
     *
     * والمرشّح نفسه يُشترط أن يكون مسارًا حرفيًّا مطلقًا بلا `{}`/`$`/`*` — نفس شرط
     * `CatalogControlIndex.isLiteralNode` — لأن القالب والمجلّد ليسا عقدة.
     */
    internal fun isTemplated(key: String): Boolean =
        key.contains('{') || key.contains('}') || key.contains('$') || key.contains('*')

    internal fun valueFits(kind: String, value: String): Boolean = when (kind) {
        "BOOL" -> value in setOf("0", "1", "true", "false")
        "INT" -> value.toLongOrNull() != null
        "PERCENT" -> value.toIntOrNull()?.let { it in 0..100 } == true
        "FREQUENCY_RANGE" -> value.toLongOrNull()?.let { it > 0L } == true
        else -> value.isNotBlank()
    }
}
