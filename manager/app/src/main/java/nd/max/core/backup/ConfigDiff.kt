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
}
