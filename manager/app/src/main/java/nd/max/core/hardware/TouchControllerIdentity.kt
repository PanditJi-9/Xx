/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.hardware

/**
 * **هوية محكّم اللمس كما يعلنها النظام — تُقرأ ولا تُستنتج.**
 *
 * ولماذا هذا الملف مستقلّ عن [TouchControlBackend]
 * -------------------------------------------------
 * `TouchControlBackend` يجيب سؤالًا واحدًا: «هل توجد عقدة تحكّم تُقرأ/تُكتب؟». أمّا هنا فالسؤال
 * مختلف: «ما وحدة الإدخال التي يعلنها Android، وهل اسمها يدلّ على عائلة محكّم معروفة؟». وخلط
 * السؤالين يُنتج ما تمنعه خطة §٧ نصًّا: أن يُستنتج معدّل اللمس من اسم الهاتف أو من شريحةٍ مشتركة.
 * فمدخل هذا الملفّ **اسم جهاز الإدخال وحده** (كما يعيده `InputManager`)، ولا يدخل فيه طراز الهاتف
 * ولا اسم الشركة المصنّعة للهاتف.
 *
 * **وحدّ التصنيف المُعلن:** المطابقة على أنماط أسماء الوحدات المعروفة (`focaltech` · `novatek` ·
 * `goodix` …). وهي **قرينة تعريف** لا إثبات قدرة: العائلة لا تحمل معدّلًا ولا حدًّا موثّقًا — تلك
 * تُقرأ من سجلّ منفصل (انظر `TouchRateChannels`)، ويبقى المجهول مجهولًا.
 */
enum class TouchControllerFamily(val brand: String?) {
    FOCALTECH("FocalTech"),
    NOVATEK("Novatek"),
    GOODIX("Goodix"),
    SYNAPTICS("Synaptics"),
    ATMEL("Atmel"),
    ELAN("Elan"),
    HIMAX("Himax"),

    /**
     * اسم لم يطابق نمطًا معروفًا. **لا يُخمَّن بديل**: العائلة المجهولة تُعرض مجهولة، ويُقال
     * سببها، بدل أن تُنسب إلى «أشهر محكّم في نفس الشريحة» — وهو الخطأ الذي حذّرت منه §٧.
     */
    UNKNOWN(null),
}

/**
 * جهاز إدخال واحد كما يعيده `InputManager` — بيانات خامّة بلا تفسير.
 *
 * @param vendorId معرّف البائع كما يعلنه الناقل (قد يكون صفرًا على نواقل غير USB).
 * @param productId معرّف المنتج كما يعلنه الناقل (قد يكون صفرًا).
 * @param descriptor بصمة الإدخال التي يعطيها Android للجهاز (تصلح للتمييز لا للاستدلال).
 */
data class TouchInputDevice(
    val name: String,
    val vendorId: Int,
    val productId: Int,
    val descriptor: String,
)

/** الهوية الناتجة: الجهاز المقروء (أو غيابه) والعائلة المصنَّفة من اسمه وحده. */
data class TouchControllerIdentity(
    val device: TouchInputDevice?,
    val family: TouchControllerFamily,
) {
    /** هل طابق الاسم عائلة معروفة؟ `false` تعني «مجهول مُعلَن» لا «فشل قراءة». */
    val identified: Boolean get() = family != TouchControllerFamily.UNKNOWN

    /** معرّفا الناقل كما هما — سدّاسيّان للقراءة، بلا أي استنتاج منهما. */
    val idsLabel: String? get() = device?.let { "0x%04X / 0x%04X".format(it.vendorId, it.productId) }
}

/**
 * المصنِّف: **دالّة على الاسم وحده** — والتوقيع نفسه هو الحارس.
 *
 * لا يوجد مدخل لطراز الهاتف ولا للشريحة، فلا يمكن لنداء أن «يعرف» الجهاز من اسمه التجاري.
 * وهذا مقصود ومُختبَر: `classify("POCO X7 Pro")` و`classify("rodin")` تُعيدان [TouchControllerFamily.UNKNOWN].
 */
object TouchControllerClassifier {

    /**
     * الأنماط مرتّبة: الأخصّ أولًا. والاسم يُوحَّد إلى أحرف صغيرة قبل المطابقة.
     *
     * ولا تُطابَق الأسماء القصيرة جدًّا (`ft` وحدها مثلًا): «ft» قد ترد في اسم أي شيء. المطابقة
     * على جذرٍ يدلّ على الوحدة (`fts` · `ft5` · `nt36` · `gt9` …) لا على حرفين.
     */
    private val patterns: List<Pair<TouchControllerFamily, List<String>>> = listOf(
        TouchControllerFamily.FOCALTECH to listOf("focaltech", "fts", "ft5", "ft6", "ft3"),
        TouchControllerFamily.NOVATEK to listOf("novatek", "nvt", "nt36", "nt110", "nt112"),
        TouchControllerFamily.GOODIX to listOf("goodix", "gt9", "gt1", "gx_"),
        TouchControllerFamily.SYNAPTICS to listOf("synaptics", "syna", "dsx", "td43"),
        TouchControllerFamily.ATMEL to listOf("atmel", "maxtouch", "mxt"),
        TouchControllerFamily.ELAN to listOf("elan"),
        TouchControllerFamily.HIMAX to listOf("himax", "hx8"),
    )

    /**
     * يصنّف **اسم جهاز الإدخال** وحده. المجهول يعود [TouchControllerFamily.UNKNOWN] ولا يُقارَب.
     */
    fun classify(name: String): TouchControllerFamily {
        val normalized = name.lowercase()
        return patterns.firstOrNull { (_, needles) ->
            needles.any(normalized::contains)
        }?.first ?: TouchControllerFamily.UNKNOWN
    }

    /**
     * يختار جهاز اللمس من قائمة أجهزة الإدخال.
     *
     * والاختيار **أول جهاز** بتقارير النظام؛ والعدّاد يُترك لمن يعرض القائمة كاملة. ولا يُدمج
     * جهازان في هوية واحدة: دمجُ اسمين يعني اختراع عائلة ثالثة لا وجود لها.
     */
    fun identify(devices: List<TouchInputDevice>): TouchControllerIdentity {
        val device = devices.firstOrNull() ?: return TouchControllerIdentity(null, TouchControllerFamily.UNKNOWN)
        return TouchControllerIdentity(device, classify(device.name))
    }
}
