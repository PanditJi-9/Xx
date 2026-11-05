/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.battery

/**
 * معدّل الاستنزاف مقسومًا بحسب حالة الشاشة.
 *
 * **ولماذا القسمة أصلًا:** ‏«٪/ساعة» واحدة على جهاز يستخدمه صاحبه ساعات وساعات
 * لا يقول شيئًا: الرقم الناتج خليط بين ساعات إضاءة وساعات نوم، ولا ينطبق على أي
 * منهما. الفصل يجعل كل رقم يصف حالة واحدة يمكن مقارنتها بيوم آخر.
 *
 * **[overallPerHour]** يبقى معروضًا لأنه ما يُقارن بتقدير النظام، لكنه ليس بديلًا
 * عن القسمة — بل مجموعها الموزون.
 */
data class DrainSummary(
    val overallPerHour: Float?,
    val activePerHour: Float?,
    val idlePerHour: Float?,
    val activeSpanHours: Float,
    val idleSpanHours: Float
) {
    /** هل قِيس أي استنزاف على الإطلاق؟ */
    val hasReading: Boolean get() = overallPerHour != null
}

object BatteryDrain {

    /**
     * أدنى مدّة تُقبل لحساب معدّل.
     *
     * **ولماذا ربع ساعة:** عيّنة كل ١٥ دقيقة؛ وزوج واحد يعطي فرقًا بين قراءتين
     * قد تكون كلٌّ منهما قرّبت إلى نسبة صحيحة، فيخرج الرقم مضاعفًا أو صفرًا.
     * ربع ساعة هو أقصر مدّة تمثّل الفاصل نفسه بدل ضجيج التقريب.
     */
    private const val MIN_SPAN_HOURS = 0.25f

    /**
     * يلخّص الاستنزاف من العيّنات المحفوظة، بلا قراءة جديدة من العتاد.
     *
     * الشروط على كل زوج متتالٍ: كلاهما يُفرّغ، والفاصل موجب. ولا يُجمع إلا
     * **الهبوط** (فرق سالب في النسبة)، لأن ارتفاع النسبة داخل فترة التفريغ
     * يعني قراءة شاذّة أو إعادة معايرة، لا شحنًا.
     */
    fun summarise(samples: List<BatterySample>): DrainSummary {
        var overallDrop = 0f
        var overallHours = 0f
        var activeDrop = 0f
        var activeHours = 0f
        var idleDrop = 0f
        var idleHours = 0f

        var previous: BatterySample? = null
        for (sample in samples) {
            val from = previous
            previous = sample
            if (from == null) continue
            if (!from.discharging || !sample.discharging) continue
            val hours = (sample.atMillis - from.atMillis) / 3_600_000f
            if (hours <= 0f) continue
            val drop = (from.levelPercent - sample.levelPercent).toFloat()
            if (drop <= 0f) continue

            overallDrop += drop
            overallHours += hours
            when {
                from.screenOn == true && sample.screenOn == true -> {
                    activeDrop += drop
                    activeHours += hours
                }

                from.screenOn == false && sample.screenOn == false -> {
                    idleDrop += drop
                    idleHours += hours
                }
                // Mixed or unknown: counted overall, in neither bucket.
            }
        }

        return DrainSummary(
            overallPerHour = rate(overallDrop, overallHours),
            activePerHour = rate(activeDrop, activeHours),
            idlePerHour = rate(idleDrop, idleHours),
            activeSpanHours = activeHours,
            idleSpanHours = idleHours
        )
    }

    /** معدّل صالح، أو `null` حين لا تكفي المدّة — لا صفر يوهم بقياس. */
    private fun rate(drop: Float, hours: Float): Float? =
        if (hours < MIN_SPAN_HOURS) null else drop / hours
}
