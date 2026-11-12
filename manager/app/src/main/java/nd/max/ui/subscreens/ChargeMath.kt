/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.subscreens

/**
 * فرق عدّاد الشحنة ← تيار متوسط بالملّيأمبير.
 *
 * العدّاد التراكمي يقيس الشحنة بالـµAh، والزمن بالثانية. فمعدّل التغيّر
 * `µAh/s` يساوي `3.6 mA` بالضبط: واحد ميكروأمبير-ساعة في الثانية هو
 * ١e-6 Ah ÷ (١/٣٦٠٠) h = ٣.٦e-3 A.
 *
 * **والاتجاه من إشارة الفرق لا من إشارة `CURRENT_NOW`:** عدّادٌ يزيد يعني
 * شحنة تدخل، وعدّادٌ ينقص يعني شحنة تخرج — وهذا لا يختلف بين الأجهزة، بينما
 * اصطلاح إشارة التيار اللحظي يختلف. ولذلك تُستعمل هذه الدالة أولًا، والتيار
 * المُعلَن بديلٌ عند غياب العدّاد.
 *
 * دالة نقيّة بلا Android، فتُقاس في اختبار JVM عادي.
 */
internal fun chargeMilliamps(deltaUah: Int, seconds: Int): Double? {
    if (seconds <= 0) return null
    return deltaUah / seconds.toDouble() * 3.6
}
