/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.gamespace

import java.util.Locale

/**
 * حساب شريط التردد وأنصّته — **نقيّ** (لا أندرويد ولا عتاد) فيُقاس على JVM.
 *
 * القاعدة التي يحملها: ما لا يُقرأ يبقى `null` ولا يُصبح صفرًا. شريط فارغ مع «—» يقول «لم أقرأ»،
 * وشريط عند الصفر يقول «التردد صفر» وهذا كذب.
 */
object PanelClockMath {

    /** عدد مقاطع الشريط (عموديًّا في العرضي، أفقيًّا في الطولي). */
    const val SEGMENTS = 20

    /**
     * مستوى التردد الحيّ بين أرضيّته وسقفه، 0..1.
     *
     * الأرضية تُستعمل حين تُعرف وتكون أدنى من السقف، فيتحرّك الشريط بين أدنى تردد يعلنه العتاد
     * وأعلاه؛ وبلا أرضية يُقاس من الصفر. أيّ مُدخل ناقص أو غير موجب ⇒ `null`.
     */
    fun level(currentMhz: Int?, floorMhz: Int?, ceilingMhz: Int?): Float? {
        if (currentMhz == null || ceilingMhz == null || currentMhz <= 0 || ceilingMhz <= 0) return null
        val floor = floorMhz?.takeIf { it in 1 until ceilingMhz } ?: 0
        return ((currentMhz - floor).toFloat() / (ceilingMhz - floor)).coerceIn(0f, 1f)
    }

    /** عدد المقاطع المضاءة (0..[SEGMENTS]) لمستوى 0..1؛ `null` ⇒ صفر مقاطع. */
    fun litSegments(level: Float?): Int =
        ((level ?: 0f).coerceIn(0f, 1f) * SEGMENTS).toInt().coerceIn(0, SEGMENTS)

    /** «3.51» من 3510 ميغاهرتز؛ `null` حين لا قراءة موجبة. */
    fun ghz(mhz: Int?): String? =
        mhz?.takeIf { it > 0 }?.let { String.format(Locale.US, "%.2f", it / 1000f) }

    /** «342» من 342 ميغاهرتز؛ `null` حين لا قراءة موجبة. */
    fun mhz(mhz: Int?): String? = mhz?.takeIf { it > 0 }?.toString()

    /** نص الشارة العائمة على شريط المعالج: «3.51 GHz». */
    fun cpuTag(mhz: Int?): String? = ghz(mhz)?.let { "$it GHz" }

    /** نص الشارة العائمة على شريط الرسّام: «342 MHz». */
    fun gpuTag(mhz: Int?): String? = mhz(mhz)?.let { "$it MHz" }

    /** «MAX 3.25» للمعالج؛ `null` حين لا سقف مقروء. سقف الرسّام الديناميكي يُعرض بنصّ مترجم في اللوحة. */
    fun cpuMax(ceilingMhz: Int?): String? = ghz(ceilingMhz)?.let { "MAX $it" }

}
