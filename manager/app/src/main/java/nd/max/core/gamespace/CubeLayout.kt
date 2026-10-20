/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.gamespace

import kotlin.math.max
import kotlin.math.min

/**
 * هندسة لوحة «المكعّب» كلها في **مكان واحد** (كل القيم dp).
 *
 * لماذا وُجدت
 * -----------
 * كانت المقاسات تُحسب متفرّقة داخل الدالة المرسومة بـ`maxOf(40f, u * 7.6f)` وأشباهها، فحدٌّ أدنى
 * ثابت لعنصر يغلب مقياس الشاشة في ارتفاع ٤٠٠dp، وتتراكب الأعمدة (زرّ العرض فوق دوائر الأعلى،
 * وشبكة البلاطات تقصّها حافة الشاشة، والحبوب تقطع نصوصها). وحين تتفرّق القياسات لا يستطيع أحد أن
 * يقول «هل يتراكب عنصران؟» إلا بتشغيل الجهاز.
 *
 * فالقاعدة هنا: كل بُعد دالة في (عرض، ارتفاع) وحدهما، والأعمدة تُوزَّع بعرض مضمون لا يتقاطع
 * (يُختبر في `CubeLayoutTest`)، والرأسي يتقاسم الارتفاع المتاح فيُحسب طول المقياس مما يتبقّى
 * بدل أن يُفترض فيتجاوز الحافة السفلى.
 */
data class CubeLayout(
    /** وحدة القياس الأساس: أصغر نسبتي الارتفاع والعرض، فتتّسع الواجهة بتناسب لا بقفزات. */
    val u: Float,
    val compact: Boolean,
    val edge: Float,
    val topPad: Float,
    val btn: Float,
    /** شريط الإحصاءات: صفّ مستقلّ تحت الأزرار، يبدأ عند [statsTop] بارتفاع [statsH]. */
    val statsTop: Float,
    val statsH: Float,
    /** خط الفصل تحت صفّ الدوائر (كما في المرجع)، وبعده يبدأ الجسم. */
    val dividerY: Float,
    val bodyTop: Float,
    val bodyBottom: Float,
    // ── العمود الأيسر (بروفايلات الحرارة)
    val leftW: Float,
    val toggleH: Float,
    val cardH: Float,
    val gridCardH: Float,
    // ── الشبكة اليمنى
    val tile: Float,
    val colW: Float,
    val columns: Int,
    val colGap: Float,
    val rowGap: Float,
    val gridW: Float,
    val gridX: Float,
    // ── عمودا CPU / GPU
    val gaugeW: Float,
    val cpuX: Float,
    val gpuX: Float,
    val gaugeTop: Float,
    val numberSp: Float,
    val unitSp: Float,
    val labelSp: Float,
    val orbH: Float,
    val pillW: Float,
    val pillH: Float,
    val meterW: Float,
    val meterH: Float,
    // ── الأسفل
    val statusW: Float,
    // ── شريطا التردد (طويلان، عموديّان، يلاصقان الحافة الداخلية لكل عمود)
    /** هل يتّسع الوسط لشريطين عموديّين؟ وإلا يُرسم شريط أفقيّ داخل عمود القياس نفسه. */
    val railOn: Boolean,
    val railW: Float,
    val railTagW: Float,
    val railTop: Float,
    val railH: Float,
    /** يسار حاوية شريط CPU (الشريط ثم شارته نحو الوسط) ويسار حاوية شريط GPU (شارته ثم الشريط). */
    val cpuRailX: Float,
    val gpuRailX: Float,
    /**
     * إزاحة قمّة الشريط نحو اللعبة بالـdp عن طرفه السفليّ (صفر ⇒ مستقيم)؛ تتوسّع به حاوية كل شريط بقدر
     * قوسه. الشريط حزام بشكل «>» (CPU) / «<» (GPU) له ساقان بميل واحد، وقمّته قرب ثلثيه العلويّ.
     */
    val railBow: Float = 0f,
) {
    /** حافة العمود الأيسر اليمنى (0 إن كان مغلقًا). */
    val leftRight: Float get() = if (leftW > 0f) edge + leftW else 0f

    /** مدى عمود CPU أفقيًّا. */
    val cpuRange: ClosedFloatingPointRange<Float> get() = (cpuX - gaugeW / 2f)..(cpuX + gaugeW / 2f)

    /** مدى عمود GPU أفقيًّا. */
    val gpuRange: ClosedFloatingPointRange<Float> get() = (gpuX - gaugeW / 2f)..(gpuX + gaugeW / 2f)

    /** الارتفاع المتاح لشبكة اليمين ولعمود اليسار. */
    val bodyHeight: Float get() = (bodyBottom - bodyTop).coerceAtLeast(0f)

    /** عرض حاوية الشريط كاملة: الشريط + قوسه + فجوة + شارة التردد. */
    val railBoxW: Float get() = railW + railBow + RAIL_TAG_GAP + railTagW

    /** الحافة الداخلية للوح الأيسر: يمين شريط CPU (أو يمين عموده حين لا شريط). */
    val leftPanelEdge: Float get() = if (railOn) cpuRailX + railW + railBow + 6f else cpuRange.endInclusive + 6f

    /** الحافة الداخلية للوح الأيمن: يسار شريط GPU (أو يسار عموده حين لا شريط). */
    val rightPanelEdge: Float get() = if (railOn) gpuRailX + railBoxW - railW - railBow - 6f else gpuRange.start - 6f

    /**
     * عرض الفتحة الحرّة عند قاع اللوحة بين الحافتين الداخليتين لقدمَي الشريطين: أقصى ما يتّسع له شريط الحالة
     * السفلي دون أن يتراكب مع الشريطين (القدم أعرض من القمّة بقدر قوسَي الشريطين).
     */
    val bottomFreeW: Float
        get() = if (railOn) (gpuRailX + railBoxW - railW) - (cpuRailX + railW)
        else gpuRange.start - cpuRange.endInclusive

    companion object {
        /** لا وسم عائم بجوار الشريط بعد الآن، فلا فجوة محجوزة له. */
        const val RAIL_TAG_GAP = 0f

        /** أقلّ عرض للفتحة عند قمّتَي الشريطين نسبةً إلى عرض الشاشة، فتبقى اللعبة ظاهرة بين الحزامين. */
        const val APEX_GAP_RATIO = 0.17f

        /** أقلّ وسط حرّ يبقى بين حاويتي الشريطين بعد انحنائهما (الاختبار يشترط ٤٠dp، وهذا أوسع منه قليلًا). */
        const val RAIL_MIN_FREE = 48f

        /** أصغر انحناء يستحقّ الرسم؛ أدنى منه يبقى الشريط مستقيمًا بدل قوس هزيل. */
        const val RAIL_MIN_BOW = 14f
    }
}

object CubeLayoutMath {

    /** ثوابت رأسية لعمود المقياس: مجموع ما فوقه من عناصر ثابتة الطول (تقدير مقصود، محافِظ). */
    private const val ICON_BELOW_METER = 22f

    /** موضع قمّة حزام التردد من قاعه (0..1). الساق السفلى أطول من العليا كما في المرجع. */
    const val RAIL_APEX = 0.68f

    /** انحناء الطرف العلوي للحزام بنسبة انحناء القمّة (1 عند القمّة ⇒ هذا عند الطرف العلوي). */
    private const val RAIL_TOP_BULGE = 0.5f

    /**
     * انحناء الحزام عند نسبة الارتفاع [t] من القاع: صفر عند القاع، و1 عند القمّة، و[RAIL_TOP_BULGE] عند
     * الطرف العلوي. خطّيّ على كل ساق، فيُرسم الحزام زاويةً حادّة لا منحنى.
     */
    fun railBulge(t: Float): Float {
        val x = t.coerceIn(0f, 1f)
        return if (x <= RAIL_APEX) x / RAIL_APEX
        else 1f - (1f - RAIL_TOP_BULGE) * (x - RAIL_APEX) / (1f - RAIL_APEX)
    }

    /** ميل [railBulge] عند [t] (مشتقّته)، ليُدار كل مقطع بزاوية ساقه. */
    fun railBulgeSlope(t: Float): Float =
        if (t < RAIL_APEX) 1f / RAIL_APEX else -(1f - RAIL_TOP_BULGE) / (1f - RAIL_APEX)

    /** ارتفاع الشريط الأفقيّ داخل عمود القياس حين لا يتّسع الوسط للشريطين العموديّين. */
    const val FREQ_BAR_H = 8f

    /**
     * @param leftOpen هل العمود الأيسر مفتوح؟ (يؤثّر في موضع عمود CPU كي لا يتقاطعا)
     * @param wideLeft هل العمود الأيسر في وضع التفاصيل (أعرض)؟
     */
    fun of(widthDp: Float, heightDp: Float, leftOpen: Boolean = true, wideLeft: Boolean = false): CubeLayout {
        val w = max(widthDp, 320f)
        val h = max(heightDp, 240f)
        val u = min(h / 100f, w / 190f).coerceIn(2.4f, 8f)
        val compact = CockpitModel.isCompact(w)

        val edge = (u * 4.6f).coerceIn(12f, 28f)
        val btn = (u * 10f).coerceIn(38f, 56f)
        val topPad = (u * 2.6f).coerceIn(6f, 16f)
        // خطّ الفاصل مباشرة تحت صفّ الأزرار (بلا شريط إحصاءات مكرّر — المعلومات في الأسفل فقط).
        val statsTop = topPad + btn + (u * 0.8f).coerceIn(3f, 6f)
        val statsH = 0f
        val dividerY = statsTop + (u * 0.6f).coerceIn(2f, 6f)
        val bodyTop = dividerY + (u * 1.2f).coerceIn(4f, 10f)
        val bodyBottom = h - (u * 2.6f).coerceIn(8f, 14f)

        // العمود الأيسر يحمل اسم بروفايل وسطرًا فرعيًّا بالعربية أيضًا: تحت ١١٦dp كانت النصوص تُبتر
        // («افتر…»، «وضع متوا…» في اللقطة) لأن الأيقونة وعلامة الاختيار تأكلان ما يبقى لها.
        val baseLeft = (w * 0.15f).coerceIn(116f, 172f)
        val leftW = when {
            !leftOpen -> 0f
            wideLeft -> max(baseLeft, 176f)
            else -> baseLeft
        }
        val toggleH = (u * 8f).coerceIn(30f, 40f)
        val cardH = (u * 9f).coerceIn(40f, 48f)
        val gridCardH = (u * 11.5f).coerceIn(48f, 62f)

        val tile = (u * 10.4f).coerceIn(40f, 60f)
        val colW = tile + 18f
        val columns = if (compact) 1 else 2
        val colGap = 6f
        val rowGap = (u * 1.2f).coerceIn(4f, 10f)
        val gridW = colW * columns + colGap * (columns - 1)
        val gridX = w - edge - gridW

        val gaugeW = if (compact) w * 0.28f else (w * 0.13f).coerceIn(90f, 156f)
        val leftRight = if (leftW > 0f) edge + leftW else 0f
        val cpuX = if (compact) {
            w * 0.30f
        } else {
            max(w * 0.22f, if (leftRight > 0f) leftRight + 6f + gaugeW / 2f else 0f)
        }
        // عمود GPU مرآة CPU، لكنه لا يدخل شبكة البلاطات أبدًا (كان يتراكب معها بحتى ٢٨dp تحت ٨٠٠dp).
        val gpuX = if (compact) w * 0.62f else min(w - cpuX, gridX - 6f - gaugeW / 2f)
        val gaugeTop = bodyTop

        val numberSp = (u * 5f).coerceIn(20f, 34f)
        val unitSp = (u * 2.2f).coerceIn(9f, 13f)
        val labelSp = (u * 2.5f).coerceIn(10f, 14f)
        val orbH = (u * 6.5f).coerceIn(26f, 48f)
        val pillW = if (compact) gaugeW * 0.92f else (w * 0.095f).coerceIn(78f, 120f)
        val pillH = (u * 5.5f).coerceIn(24f, 34f)
        val meterW = (u * 9f).coerceIn(36f, 52f)

        // شريطا التردد الطويلان: عموديّان يلاصقان الحافة الداخلية لكل عمود حين يتّسع الوسط،
        // وإلا يُرسم شريط أفقيّ قصير داخل العمود نفسه (فلا يغيب التردد على الشاشات الضيّقة).
        val railTagW = 0f // بلا وسم عائم: الحاوية بقدر الشريط وقوسه فقط، فيتّسع القوس ويكبر الحزام
        val railTop = bodyTop
        val railH = (bodyBottom - bodyTop).coerceAtLeast(0f)
        val cpuRailX = cpuX + gaugeW / 2f + 4f
        // الوسط الحرّ بين حاويتي الشريطين (قبل أي انحناء) لو كان عرض الشريط [w].
        fun freeMiddle(w: Float): Float {
            val box = w + CubeLayout.RAIL_TAG_GAP + railTagW
            return (gpuX - gaugeW / 2f - 4f - box) - (cpuRailX + box)
        }
        // الشريط عريض كما في الأصل (حزام مقاطعه كبيرة لا نقاط صغيرة)؛ فإن ضاق الوسط عاد إلى العرض القديم
        // فلا تفقد الشاشات المتوسّطة شريطها العموديّ بسبب العرض وحده.
        val railWide = (railH * 0.07f).coerceIn(14f, 24f)
        val railNarrow = (u * 2.8f).coerceIn(10f, 14f)
        val railW = if (freeMiddle(railWide) >= 40f) railWide else railNarrow
        val railBaseBox = railW + CubeLayout.RAIL_TAG_GAP + railTagW
        val gpuRailBase = gpuX - gaugeW / 2f - 4f - railBaseBox
        val freeBase = gpuRailBase - (cpuRailX + railBaseBox)
        val railOn = !compact && freeBase >= 40f
        // انحناء الشريط نحو اللعبة (قمّة الزاوية) يوسّع كل حاوية بقدر قوسه. يأخذ ما يتّسع له الوسط ويُبقي
        // [RAIL_MIN_FREE] حرّة بعد التوسعتين معًا؛ وإن لم يتّسع [RAIL_MIN_BOW] بقي الشريط مستقيمًا.
        val bowWanted = (railH * 0.32f).coerceIn(24f, 110f)
        val bowRoom = (freeBase - max(CubeLayout.RAIL_MIN_FREE, w * CubeLayout.APEX_GAP_RATIO)) / 2f
        val railBow = if (railOn && bowRoom >= CubeLayout.RAIL_MIN_BOW) min(bowWanted, bowRoom) else 0f
        val railBox = railBaseBox + railBow
        val gpuRailX = gpuX - gaugeW / 2f - 4f - railBox

        // ما فوق المقياس: رقم + قرص + وسم + فجوة + حبّة + فجوة (+ شريط أفقيّ إن غاب العمودي)، وتحته أيقونته.
        val freqBarBlock = if (railOn) 0f else FREQ_BAR_H + u * 1f
        val above = numberSp * 1.15f + orbH + labelSp * 1.3f + u * 1.6f + pillH + u * 1.4f + freqBarBlock
        val room = bodyBottom - gaugeTop - above - ICON_BELOW_METER
        val meterH = room.coerceIn(64f, (u * 24f).coerceAtLeast(64f))

        val statusW = (w * 0.34f).coerceIn(180f, 360f)

        return CubeLayout(
            u = u, compact = compact, edge = edge, topPad = topPad, btn = btn,
            statsTop = statsTop, statsH = statsH,
            dividerY = dividerY, bodyTop = bodyTop, bodyBottom = bodyBottom,
            leftW = leftW, toggleH = toggleH, cardH = cardH, gridCardH = gridCardH,
            tile = tile, colW = colW, columns = columns, colGap = colGap, rowGap = rowGap,
            gridW = gridW, gridX = gridX,
            gaugeW = gaugeW, cpuX = cpuX, gpuX = gpuX, gaugeTop = gaugeTop,
            numberSp = numberSp, unitSp = unitSp, labelSp = labelSp, orbH = orbH,
            pillW = pillW, pillH = pillH, meterW = meterW, meterH = meterH,
            statusW = statusW,
            railOn = railOn, railW = railW, railTagW = railTagW, railTop = railTop, railH = railH,
            cpuRailX = cpuRailX, gpuRailX = gpuRailX, railBow = railBow,
        )
    }
}
