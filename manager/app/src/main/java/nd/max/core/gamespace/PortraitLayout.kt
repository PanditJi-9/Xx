/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.gamespace

import kotlin.math.max
import kotlin.math.min

/**
 * هندسة اللوحة في وضع **الطولي** (كل القيم dp) — دالة في (عرض، ارتفاع) وحدهما، كما [CubeLayout].
 *
 * اللوحة بطاقة **عائمة** مستديرة الزوايا الأربع فوق لعبة طولية: عرضها [PortraitLayoutMath.WIDTH_FRACTION]
 * من الشاشة فتبقى أطراف اللعبة ظاهرة على جانبيها، وارتفاعها حدّه [PortraitLayoutMath.HEIGHT_FRACTION]
 * فيبقى أعلاها ظاهرًا (ولمسته تطوي اللوحة). ولها عمودان للمعالج والرسّام، ثم منزلقان أفقيّان للسطوع
 * والصوت، ثم بروفايلات الحرارة، ثم شبكة البلاطات بأربعة أعمدة. وما زاد عن ارتفاعها يُمرَّر.
 */
data class PortraitLayout(
    /** عرض البطاقة: نسبة من عرض الشاشة ([PortraitLayoutMath.WIDTH_FRACTION]) حتى [MAX_CARD_W]، فلا تتمطّط على لوح عريض. */
    val cardW: Float,
    /** أقصى ارتفاع للبطاقة؛ ما فوقه يبقى من اللعبة ظاهرًا. */
    val cardMaxH: Float,
    val pad: Float,
    val gap: Float,
    /** عرض بطاقة قياس واحدة (CPU أو GPU). */
    val gaugeW: Float,
    val numberSp: Float,
    /** قطر الأزرار الدائرية (≥ ٤٨ لمسًا). */
    val btn: Float,
    /** مقاس البلاطة ومقاس عمودها في الشبكة، وعدد الأعمدة. */
    val tile: Float,
    val tileColW: Float,
    val tileCols: Int,
    /** ارتفاع صفّ البروفايل وارتفاع المنزلق. */
    val optionH: Float,
    val sliderH: Float,
) {
    /** عرض المحتوى داخل حشو البطاقة. */
    val inner: Float get() = cardW - 2f * pad
}

object PortraitLayoutMath {
    const val MAX_CARD_W = 560f
    const val COLUMNS = 4

    /**
     * نسبة عرض البطاقة من عرض الشاشة. أقلّ من الكامل بقصد: الحافّتان تبقيان للعبة فتبدو البطاقة
     * عائمة لا شريطًا ملتصقًا بالحافّة.
     */
    const val WIDTH_FRACTION = 0.9f

    /**
     * أقصى ارتفاع للبطاقة من ارتفاع الشاشة. كان ٨٨٪ فكانت تحجب اللعبة تقريبًا كلّها؛ الآن يبقى
     * ما فوقها ظاهرًا، وما زاد من محتوى يُمرَّر داخلها.
     */
    const val HEIGHT_FRACTION = 0.6f

    fun of(widthDp: Float, heightDp: Float): PortraitLayout {
        val w = max(widthDp, 280f)
        val h = max(heightDp, 360f)
        val cardW = min(w * WIDTH_FRACTION, MAX_CARD_W)
        val pad = (cardW * 0.04f).coerceIn(12f, 20f)
        val gap = 8f
        val inner = cardW - 2f * pad
        val gaugeW = (inner - gap) / 2f
        val colW = (inner - gap * (COLUMNS - 1)) / COLUMNS
        val tile = (colW - 10f).coerceIn(40f, 64f).coerceAtMost(colW)
        return PortraitLayout(
            cardW = cardW,
            cardMaxH = h * HEIGHT_FRACTION,
            pad = pad,
            gap = gap,
            gaugeW = gaugeW,
            numberSp = (gaugeW * 0.2f).coerceIn(20f, 32f),
            btn = 48f,
            tile = tile,
            tileColW = colW,
            tileCols = COLUMNS,
            optionH = 44f,
            sliderH = 40f,
        )
    }
}
