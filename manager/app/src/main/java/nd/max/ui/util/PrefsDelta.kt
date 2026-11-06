/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.util

/**
 * عدد مفاتيح التفضيلات التي تختلف فعلًا بين حِزمة محفوظة وما على الجهاز.
 *
 * **ولماذا هذا القياس قبل الاستعادة:** التفضيلات تُدمج ولا تُمحى، فالخطر فيها ليس
 * فقدان ملفّ بل قيمة تُستبدل بلا أن يراها المستخدم. العدّ يجعل الحوار يقول «كم
 * مفتاحًا سيتغيّر» بدل كلمة «تفضيلات» التي لا تُخبر بشيء.
 *
 * **ولا يُعدّ الملفّ كاملًا:** ملفّ جديد على الجهاز ليس تغييرًا لملفّ قديم، بل إضافة
 * ملفّ — وهو ما يقوله القسم نفسه؛ العدّ هنا للمفاتيح داخل الملفّات المشتركة.
 */
object PrefsDelta {

    /**
     * @param incoming حِزمة الملفّ ← مفاتيح ← قيم (من ملفّ النسخة).
     * @param current ما قرأه الجهاز الآن؛ `null` يعني تعذّرت القراءة.
     * @return عدد المفاتيح المختلفة (أو الغائبة عن الجهاز).
     */
    fun count(
        incoming: Map<String, Map<String, ConfigBackupInventory.PrefValue>>,
        current: Map<String, Map<String, ConfigBackupInventory.PrefValue>>?,
    ): Int = incoming.entries.sumOf { (file, values) ->
        val onDevice = current?.get(file)
        values.count { (key, value) -> onDevice?.get(key) != value }
    }
}
