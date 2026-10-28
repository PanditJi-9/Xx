/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.spoof

import kotlin.random.Random

/**
 * قيمة IMEI المزيّفة لكل تطبيق — **نقيّة**: لا قراءة ولا كتابة ولا أيّ API من أندرويد، فتُختبر على JVM.
 *
 * IMEI هنا 15 رقمًا: ثمانية للـTAC، وستة للرقم التسلسلي، ورقم تحقّق Luhn. القيمة المولَّدة تجتاز تحقّق
 * Luhn كما يجتازه IMEI حقيقي، فلا يرفضها تطبيق يتحقّق من الصيغة. وهي **عشوائية لا تعود لجهاز حقيقي**.
 * الشريحة الثانية تُشتقّ من الأولى بزيادة الرقم التسلسلي، كما تختلف شريحتا الجهاز الحقيقي.
 */
object ImeiSpoof {
    /** ملفّ التفضيلات الذي يقرؤه الخطّاف داخل عملية التطبيق الهدف (انظر nd.max.xposed.ImeiHook). */
    const val PREFS_NAME = "imei_spoof"
    private const val KEY_PREFIX = "imei."
    const val LENGTH = 15

    /** مفتاح تطبيق واحد في التفضيلات. اسم الحزمة وحده، فلا يتصادم تطبيقان. */
    fun key(packageName: String): String = KEY_PREFIX + packageName

    /** رقم Luhn لجسم من 14 رقمًا (الرقم الأيمن في الجسم يُضاعَف لأن رقم التحقّق سيأخذ موضع الآحاد). */
    fun checkDigit(body: String): Int {
        require(body.length == LENGTH - 1 && body.all { it in '0'..'9' }) { "IMEI body must be 14 digits" }
        var sum = 0
        body.reversed().forEachIndexed { index, char ->
            var digit = char - '0'
            if (index % 2 == 0) {
                digit *= 2
                if (digit > 9) digit -= 9
            }
            sum += digit
        }
        return (10 - sum % 10) % 10
    }

    /** هل هو 15 رقمًا يجتاز Luhn؟ */
    fun isValid(imei: String): Boolean =
        imei.length == LENGTH && imei.all { it in '0'..'9' } && checkDigit(imei.take(LENGTH - 1)) == imei.last() - '0'

    /** IMEI عشوائي صالح الصيغة. المولّد `Random` يُمرَّر ليكون الاختبار حتميًّا. */
    fun generate(random: Random = Random.Default): String {
        val body = buildString {
            append("35")
            repeat(12) { append(random.nextInt(10)) }
        }
        return body + checkDigit(body)
    }

    /**
     * IMEI الشريحة `slot`: الأولى (0 أو أقلّ) هي القيمة نفسها، وغيرها يزيد الرقم التسلسلي (آخر ستة قبل رقم
     * التحقّق) بمقدار الرقم، ويُعاد حساب رقم التحقّق. قيمة غير صالحة تُعاد كما هي ولا تُحوَّل.
     */
    fun forSlot(imei: String, slot: Int): String {
        if (slot <= 0 || !isValid(imei)) return imei
        val tac = imei.substring(0, 8)
        val serial = (imei.substring(8, 14).toInt() + slot) % 1_000_000
        val body = tac + serial.toString().padStart(6, '0')
        return body + checkDigit(body)
    }
}
