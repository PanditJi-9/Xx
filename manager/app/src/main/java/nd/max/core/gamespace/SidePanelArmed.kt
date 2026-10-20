/* Copyright (C) 2026 Nader Magdy. All rights reserved. */
package nd.max.core.gamespace

import nd.max.core.platform.ForegroundAppResolver

/**
 * قائمة الألعاب التي تُفعَّل لها اللوحة الجانبية، **كما يقرؤها الرفيق الجذري**.
 *
 * ### لماذا ملفّ بجانب التفضيلات
 *
 * التفضيلات تُحفَظ داخل التطبيق، والتطبيق يُقتل بعد إغلاقه من الحديثة على HyperOS، ولا شيء يُعيد تشغيله حين
 * تُفتح اللعبة من المشغّل. الرفيق الجذري (`AppMonitor`) يعيش مستقلًّا ويرى كل تبديل للمقدّمة، فيكفيه ملف نصّي
 * فيه حزمة في كل سطر، ليطلب من النظام تشغيل خدمة اللوحة حين تصبح إحدى هذه الحزم أمامية.
 *
 * والملف نسخة من التفضيلات لا مصدر ثانٍ لها: تُكتب من الردهة عند كل تغيير، ومن بدء التطبيق مزامنةً
 * (`SidePanelArmedStore`)، فإن اختلفا غلبت التفضيلات.
 */
object SidePanelArmed {

    /** المكوّن الذي يشغّله الرفيق من الجذر. اسم الحزمة ثابت لأن الوحدة تُركّب التطبيق بهذا الاسم. */
    const val SERVICE_COMPONENT = "nd.max/nd.max.service.GamePanelService"

    /** نصّ الملف: حزمة صالحة في كل سطر، مرتبة، بلا مكرّر ولا أسماء غير صالحة. */
    fun encode(packages: Set<String>): String =
        packages.map { it.trim() }
            .filter(ForegroundAppResolver::isPackageName)
            .distinct()
            .sorted()
            .joinToString(separator = "") { "$it\n" }

    /** يقرأ الملف، ويتجاهل السطور الفارغة وكل ما ليس اسم حزمة صالحًا. */
    fun decode(text: String?): Set<String> =
        text.orEmpty().lineSequence()
            .map { it.trim() }
            .filter(ForegroundAppResolver::isPackageName)
            .toSet()

    /** هل هذه الحزمة مكتوبة في القائمة؟ تطابق سطر كامل لا بادئة. */
    fun contains(text: String?, packageName: String): Boolean =
        ForegroundAppResolver.isPackageName(packageName) && packageName in decode(text)
}
