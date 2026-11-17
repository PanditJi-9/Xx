/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.maxai

import nd.max.core.hardware.DeviceStateCollector.DeviceSnapshot

/**
 * الهدف لا الوضع (قرار #2).
 *
 * Performance / Balanced / Eco لم تعد ملفات ثابتة، بل ثلاثة أوزان
 * مقيسة يوازن بينها المخطِّط. هذا ما يسمح بـ«توفير خفيف» بدل قفزة
 * كاملة إلى ملف Eco، ويجعل الشاشة المطفأة هدفًا مختلفًا (طاقة+حرارة)
 * لا مجرد "إيقاف" (قرار #11).
 */
data class Objective(
    /** وزن الأداء: كم يهم رفع السقوف والاستجابة. */
    val performance: Float,
    /** وزن البطارية: كم يهم خفض الاستهلاك. */
    val battery: Float,
    /** وزن الهامش الحراري: كم يهم إبقاء الحرارة بعيدة عن السقف. */
    val thermalHeadroom: Float,
) {
    /**
     * درجة الحالة تحت هذا الهدف: أعلى = أقرب لما يريده المستخدم.
     * كل حد يقيس شيئًا فعليًا من اللقطة ويوزن بأولوية المستخدم — فلا
     * يتدخل العقل إلا حين تنخفض هذه الدرجة عن رضا مقيس (فجوة حقيقية).
     */
    fun score(s: DeviceSnapshot): Float {
        val perfTerm = (1f - s.cpuLoad) * performance
        val batteryTerm = batteryHeadroom(s.battery) * battery
        val thermalTerm = (1f - s.thermal.coerceIn(0f, 1f)) * thermalHeadroom
        val total = performance + battery + thermalHeadroom
        if (total <= 0f) return 0f
        return ((perfTerm + batteryTerm + thermalTerm) / total).coerceIn(0f, 1f)
    }

    /** الاتجاه الذي يخدم الهدف الآن: رفع الأداء أم توفير الطاقة. */
    fun preferredDirection(s: DeviceSnapshot): ControlRegistry.Direction {
        // حِمل مرتفع وأولوية أداء وشحن غير منخفض ⇒ ارفع؛ غير ذلك (حرارة/بطارية) ⇒ وفّر.
        //
        // وشرط الشحن مضاف لأنه كان ناقصًا: «أداء» عند ٨٪ شحن كان يستطيع رفع السقف، وهو
        // نقيض ما يطلبه المستخدم في تلك اللحظة — والرفع مصدر حرارة لا يبرّره وضع الأداء.
        //
        // **المتوازن لا يساوي البطارية:** قبل هذا الشرط كان `0.4 >= 0.3 + 0.3` كاذبًا دائمًا،
        // فكان «المتوازن» لا يرفع أبدًا ويهبط بالسقوف حتى تصير الواجهة ثقيلة. والآن يرفع
        // فقط حين يكون الحمل حقيقيًا والجهاز باردًا فعلًا، ويبقى خافضًا حالما يقترب الحرّ.
        val thermalFirst = thermalHeadroom >= performance
        val raiseCeiling = if (thermalFirst) THERMAL_RAISE_CEILING_THERMAL_LED else THERMAL_RAISE_CEILING
        val weightsAllowRaise = performance + battery >= thermalHeadroom
        val performanceLeadsBattery = performance >= battery
        val wantsPerformance = performanceLeadsBattery &&
            weightsAllowRaise &&
            s.cpuLoad > RAISE_LOAD_FLOOR &&
            s.thermal < raiseCeiling &&
            s.battery >= BATTERY_COMFORT_FRACTION
        return if (wantsPerformance) ControlRegistry.Direction.RAISE_PERFORMANCE
        else ControlRegistry.Direction.SAVE_ENERGY
    }

    companion object {
        /**
         * خطّ راحة البطارية: فوقه الشحن **ليس قيدًا** على الرضا، وتحته ينقص الوزن خطيًّا.
         *
         * **والعطب الذي وُلد منه (مقيس، ٢٠٢٦-١٠-٢١):** كان الوزن هو نسبة الشحن نفسها،
         * فسقف الدرجة في وضع البطارية عند شحن ١٥٪ هو `0.15 + 0.7×0.15 + 0.15 = 0.405` —
         * أي أن `SATISFIED_SCORE = 0.75` **غير قابل للبلوغ رياضيًّا**، فالمحرك يرى فجوة في
         * كل دورة ويهبط بسقف CPU درجة كل ثلاثين ثانية إلى أن يبلغ أرضية السلّم. وسجلّ
         * الجهاز يوثّق النتيجة: `cpu_limits:policy0` وصل `300000:500000` (٥٠٠ ميجاهرتز)
         * مع ٣٠١ قرار في ١٥ ساعة — لاغ مقيس سببه قرار، لا حرارة.
         *
         * فصار الشحن موردًا يُقاس بحدّ راحة معلَن: ٥٠٪ فأكثر = لا فجوة من جهة البطارية،
         * وتحته ينقص الوزن بسلاسة. وهذا لا يُلغي الترشيد عند شحن منخفض (يبقى فجوة تُبرّر
         * خطوة)، وإنما يمنع أن يُطلب من المقبض ما لا يستطيعه: رفع نسبة الشحن.
         */
        const val BATTERY_COMFORT_FRACTION = 0.5f

        /** حمل أدنى حتى يُسمح بالرفع؛ تحته يكون الرفع حرارة بلا فائدة. */
        const val RAISE_LOAD_FLOOR = 0.7f

        /** سقف الحرارة المسموح للرفع في وضع الأداء (٠..1). */
        const val THERMAL_RAISE_CEILING = 0.45f

        /**
         * سقف الرفع حين يكون الهدف حراريًا أولًا (المتوازن): أضيق من وضع الأداء.
         * الفرق مقصود: المتوازن يشتري السلاسة من دون أن يسمح للجهاز بالاقتراب من الحرّ.
         */
        const val THERMAL_RAISE_CEILING_THERMAL_LED = 0.35f

        /** جزء الشحن الذي يُعدّ رضا كاملًا من جهة البطارية (٠..1). */
        fun batteryHeadroom(fraction: Float): Float =
            (fraction / BATTERY_COMFORT_FRACTION).coerceIn(0f, 1f)

        /**
         * أوزان مسبقة تُترجم عنها أزرار الأداء/التوازن/التوفير في الواجهة.
         *
         * المتوازن **حراري أولًا** لا بطارية ثانية: الوزن الأكبر للهامش الحراري،
         * والبطارية أصغر وزن. فالمستخدم يطلب جهازًا باردًا وسلسًا، لا جهازًا يوفّر
         * بضعة ملّي واط ثم يصبح بطيئًا. والبطارية تبقى قيدًا في `preferredDirection`
         * وعتبة الرضا، فلا يُرفع السقف عند شحن منخفض.
         */
        val PERFORMANCE = Objective(performance = 0.7f, battery = 0.1f, thermalHeadroom = 0.2f)
        val BALANCED = Objective(performance = 0.42f, battery = 0.13f, thermalHeadroom = 0.45f)
        val ECO = Objective(performance = 0.15f, battery = 0.7f, thermalHeadroom = 0.15f)

        /** هدف الشاشة المطفأة: طاقة وحرارة فقط، بلا أداء (قرار #11). */
        val SCREEN_OFF = Objective(performance = 0f, battery = 0.7f, thermalHeadroom = 0.3f)

        fun fromLegacyProfile(profileId: String?): Objective = when (profileId) {
            "1" -> PERFORMANCE
            "3" -> ECO
            else -> BALANCED
        }

        /**
         * تفضيل المستخدم الصريح (قرار #10): يُسأل مرة عند التشغيل.
         * يتقدم على الملف الحالي لأن نية المستخدم أصدق من وضع ورثناه.
         */
        fun fromPreference(preference: String?): Objective = when (preference) {
            "performance" -> PERFORMANCE
            "battery" -> ECO
            "balanced" -> BALANCED
            else -> BALANCED
        }

        /** القيمة المعاكسة: تسمية Prop من كائن هدف. */
        fun labelFor(objective: Objective): String = when {
            objective.performance >= objective.battery + objective.thermalHeadroom -> "performance"
            objective.battery >= objective.performance + objective.thermalHeadroom -> "battery"
            else -> "balanced"
        }
    }
}
