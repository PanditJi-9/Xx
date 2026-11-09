/*
 * Copyright (C) 2026-2027 MaxManager contributors
 *
 * Licensed under the Apache License, Version 2.0.
 */
package nd.max.core.platform

import nd.max.core.hardware.RootFileAccess

/**
 * عتبات قاتل الذاكرة في النواة (`lowmemorykiller/minfree`) — **كتابة حقيقية بتحقّق**.
 *
 * **ولماذا ملفٌّ منفصل عن [VmTuning]:** ذاك ليس عقدة عدد صحيح واحدًا بل **متجه ستّ عتبات**
 * بالصفحات، يُكتب سطرًا مفصولًا بفواصل. وقصّه إلى رقم واحد كان سيُنتج واجهة تكتب قيمةً لا
 * معنى لها.
 *
 * **وصدق الحدّ:** العتبات لا تُكتب إن لم تنشرها النواة (`/sys/module/lowmemorykiller` غير موجود
 * على نوى حديثة كثيرة)، ولا يُدَّعى نجاحٌ بلا قراءة مرتجعة. والخطورة ليست في الكتابة نفسها بل في
 * الرقم: عتبة عالية تقتل تطبيقات في الخلفية، ومنخفضة تترك الذاكرة تمتلئ حتى يتباطأ الجهاز.
 *
 * والحساب كلّه ([parse] · [scale] · [pageToMegabytes]) دالّات نقيّة تُقاس على JVM، فالقراءة
 * والكتابة في [Io] وحده.
 */
object LmkTuning {

    const val MINFREE = "/sys/module/lowmemorykiller/parameters/minfree"

    /** أصغر عتبة مقبولة — صفر تعطّل العتبة، فلا يُكتب. */
    const val MIN_PAGE = 1

    /** سقف عمليّ: نصف مليون صفحة (٢ جيجابايت) لكل عتبة — أعلى من ذلك يعني قتل كل شيء. */
    const val MAX_PAGE = 500_000

    /** حدود المعامل الذي تسمح به الواجهة: لا «إضعاف» بنسبة تُفرغ الجدول ولا مضاعفة تُخنقه. */
    const val MIN_PERCENT = 50
    const val MAX_PERCENT = 200

    /**
     * يفكّ نصّ العقدة إلى قائمة صفحات. `null` لأي شكل لا يُفهم بالكامل.
     *
     * ولا يُقبل جزءٌ صالح: عقدة تحمل `10240,20480,abc` ليست جدولًا — وقبول الأجزاء كان سيكتب
     * متجهًا أقصر من متجه النواة.
     */
    fun parse(raw: String?): List<Int>? {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) return null
        val parts = text.split(',', ' ', '\n', '\t').filter { it.isNotBlank() }
        if (parts.size < 2) return null
        val values = parts.map { part ->
            if (part.any { it !in '0'..'9' }) return null
            val value = part.toIntOrNull() ?: return null
            if (value < MIN_PAGE || value > MAX_PAGE) return null
            value
        }
        return values
    }

    /** سطر الكتابة كما تنتظره النواة: فواصل بلا مسافات. */
    fun format(values: List<Int>): String = values.joinToString(",")

    /**
     * يضرب كل عتبة في [percent]٪ ويُعيد متجهًا **متزايدًا** يبقى عددُه عددَ الأصل.
     *
     * **ولماذا يُصلَح الترتيب:** النواة تقرأ العتبات كسلّم (كل عتبة أعلى من سابقتها)؛ وضرب
     * قيمٍ صغيرة قد يُسوّي بينها بعد التقريب، فيُرفض السطر أو يُقرأ سلّمًا مكسورًا. فالتساوي
     * يُرفع بصفحة واحدة، ويُقال ذلك في [OrderAdjusted] ليُعرض للمستخدم لا ليُخفى.
     */
    fun scale(values: List<Int>, percent: Int): List<Int>? {
        if (percent < MIN_PERCENT || percent > MAX_PERCENT) return null
        if (values.isEmpty()) return null
        val scaled = values.map { value ->
            ((value.toLong() * percent) / 100L).coerceIn(MIN_PAGE.toLong(), MAX_PAGE.toLong()).toInt()
        }.toMutableList()
        for (index in 1 until scaled.size) {
            if (scaled[index] <= scaled[index - 1]) {
                scaled[index] = (scaled[index - 1] + 1).coerceAtMost(MAX_PAGE)
            }
        }
        return scaled
    }

    /** هل عدّل [scale] الترتيب بعد التقريب؟ يُعرض سطرًا واحدًا، فلا يُدَّعى أنها نسبة دقيقة. */
    fun orderAdjusted(original: List<Int>, scaled: List<Int>, percent: Int): Boolean =
        scaled != original.map { value ->
            ((value * percent) / 100).coerceIn(MIN_PAGE, MAX_PAGE)
        }

    /** الصفحات → ميجابايت بصفحة [pageKb] (٤ كيلوبايت هي الغالبة). */
    fun pageToMegabytes(page: Int, pageKb: Int = 4): Int = (page.toLong() * pageKb / 1024L).toInt()

    /** نتيجة محاولة كتابة — كـ[VmWriteOutcome] لا `Boolean` يخلط الرفض بالفشل. */
    enum class Outcome { APPLIED, NOT_PRESENT, NOT_WRITABLE, UNVERIFIED }

    /**
     * المِنفذ الحقيقيّ: قراءة وكتابة عبر [RootFileAccess] وحده (نفس مسار [VmTuning])، والحكم
     * على **القيمة المقروءة** بعد الكتابة لا على رمز الأمر.
     */
    class Io(
        private val read: (String) -> String? = { RootFileAccess.read(it) },
        private val write: (String, String) -> Boolean = { path, value -> RootFileAccess.write(path, value) },
    ) {

        fun current(): List<Int>? = parse(read(MINFREE))

        fun apply(values: List<Int>): Outcome {
            val before = current() ?: return Outcome.NOT_PRESENT
            if (values.size != before.size) return Outcome.UNVERIFIED
            if (values.any { it < MIN_PAGE || it > MAX_PAGE }) return Outcome.NOT_WRITABLE
            val accepted = runCatching { write(MINFREE, format(values)) }.getOrDefault(false)
            if (!accepted) return Outcome.NOT_WRITABLE
            return if (current() == values) Outcome.APPLIED else Outcome.UNVERIFIED
        }

        companion object {
            fun system(): Io = Io()
        }
    }
}
