/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.backup

/**
 * فرق الإعدادات بين ملف نسخة احتياطية والجهاز الحالي.
 *
 * **ولماذا دالّة نقية بلا أندرويد:** الحكم «هل ستتغيّر هذه القيمة؟» هو ما يقرّر
 * المستخدم على أساسه هل يستعيد أو يتراجع؛ وهو مقارنة نصوص لا قراءة نظام. فصله
 * يعني أن حالات الحدّ — قيمة غائبة، قيمة فارغة، مفتاح جديد — تُختبر بلا جهاز.
 *
 * **وحدّه المعلن:** [diff] تقارن المفاتيح التي **يحملها** الملف فقط. ما على الجهاز
 * ولا في الملف لا يظهر هنا، لأنه لا يُحذف بالاستعادة (الاستعادة تكتب ولا تمحو)،
 * فعدّه «فرقًا» يُخيف بلا سبب.
 */
object ConfigDiff {

    /** تغيّر واحد: القيمة السابقة (أو `null` إن لم تكن مضبوطة) والقادمة. */
    data class Change(val key: String, val from: String?, val to: String)

    /**
     * المفاتيح التي ستتغيّر فعلًا، مرتّبة بالمفتاح ليكون العرض ثابتًا بين جولتين.
     *
     * `from` فارغة تُطبَّع إلى `null`: خصيصة غير مضبوطة تُقرأ `""` من المنصّة،
     * وعرضها فراغًا في الواجهة يُوهم بأن هناك قيمة سابقة. والقيمة القادمة يجب
     * أن تكون غير فارغة كما في الاستعادة نفسها — وإلا لا يُكتب شيء فلا يُعلن فرق.
     */
    fun diff(current: Map<String, String?>, incoming: Map<String, String>): List<Change> =
        incoming.entries
            .filter { it.value.isNotEmpty() }
            .mapNotNull { (key, to) ->
                val from = current[key]?.takeIf { it.isNotBlank() }
                if (from == to) null else Change(key, from, to)
            }
            .sortedBy { it.key }

    /** كم سطرًا يُضاف وكم يُحذف في ملف نصّي (قائمة التطبيقات) — بالتكرار لا بالمجموعة. */
    data class LineDelta(val added: Int, val removed: Int) {
        val total: Int get() = added + removed
    }

    /**
     * فرق ملف أسطر (قائمة التطبيقات) بين الحالي والقادم.
     *
     * **ولماذا بالتكرار لا بالمجموعة:** القائمة قد تحمل السطر نفسه مرّتين لعنصرين
     * مختلفين؛ عدّه مرة واحدة يُخفي حذفًا وقع. والفراغات وأسطر التعليق الفارغة
     * تُسقط من الطرفين، لأنها ليست عناصر.
     *
     * والعدّ ليس تعديلًا: الاستعادة تكتب الملف كاملًا، والفرق يُعلن حجم ما سيُستبدل
     * قبل أن يُستبدل — لا أكثر.
     */
    fun lineDelta(current: String, incoming: String): LineDelta {
        val before = counts(current)
        val after = counts(incoming)
        var added = 0
        var removed = 0
        (before.keys + after.keys).forEach { line ->
            val was = before[line] ?: 0
            val now = after[line] ?: 0
            if (now > was) added += now - was
            if (was > now) removed += was - now
        }
        return LineDelta(added = added, removed = removed)
    }

    private fun counts(text: String): Map<String, Int> =
        text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .groupingBy { it }
            .eachCount()
}
