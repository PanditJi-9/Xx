/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.hardware

import java.io.File

/**
 * نصف الكتابة/القراءة من ملفّ «تعليم الأجهزة» — والنصف الآخر (الصيغة والقواعد) في
 * [HardwareDeviceOverlay].
 *
 * لماذا ملفّ نصّيّ في مجلّد التطبيق لا قاعدة بيانات ولا SharedPreferences: هذا الملفّ
 * **يُقرأ ويُعدَّل بيد صاحبه** — نسخُه إلى جهاز آخر أو إرساله ليُدرَج في قائمة أجهزة مدعومة هو
 * نصف قيمته، وSharedPreferences أعمى لا يُنسخ سطرًا.
 *
 * والقاعدتان هنا
 * --------------
 * 1. **الكتابة تُقاس بالقراءة المرتجعة.** [save] يكتب إلى ملفّ مؤقّت ثم يُبدّله، ثم **يعيد قراءة
 *    الملفّ ويقارن** — فالقيمة الراجعة «كُتب فعلًا» لا «نفّذنا أمر كتابة». وهو نفس مبدأ الكتابة
 *    المُتحقَّقة في عتاد هذا المستودع، مطبَّقًا على قرص.
 * 2. **القراءة محدودة الحجم.** ملفّ أكبر من [MAX_BYTES] يُرفض بسبب مكتوب بدل أن يُقرأ كاملًا في
 *    كل دورة (ودورة الشاشة كل ثانيتين): الحدّ يحمي واجهةً من ملفّ ضخم، ولا يقتطع نصًّا بصمت.
 */
class HardwareOverlayStore(private val file: File) {

    /** ملفّ المستخدم كما هو على القرص: أسطرٌ قُبلت، وأسطرٌ رُفضت بأسبابها. */
    fun load(): HardwareDeviceOverlay.Parsed {
        if (!file.isFile) return HardwareDeviceOverlay.Parsed()
        if (file.length() > MAX_BYTES) {
            return HardwareDeviceOverlay.Parsed(
                rejections = listOf(
                    HardwareDeviceOverlay.Rejection(0, file.name, HardwareDeviceOverlay.REASON_TOO_LARGE),
                ),
            )
        }
        val text = runCatching { file.readText() }.getOrNull()
            ?: return HardwareDeviceOverlay.Parsed(
                rejections = listOf(
                    HardwareDeviceOverlay.Rejection(0, file.name, HardwareDeviceOverlay.REASON_UNREADABLE),
                ),
            )
        return HardwareDeviceOverlay.parse(text)
    }

    /**
     * يكتب القائمة كاملة. `true` فقط إذا **قُرئ بعد الكتابة ما كُتب**.
     *
     * والفشل هنا لا يُرمى: ملفّ لا يُكتب (مجلّد بلا صلاحية، قرص ممتلئ) حالةٌ تُعرَض للمستخدم
     * بجملة، ولا تُسقط الشاشة. وما لم يُكتب لا يُشغَّل في الذاكرة أيضًا — يقرّره المستدعي.
     */
    fun save(entries: List<HardwareDeviceOverlay.Entry>): Boolean {
        val rendered = HardwareDeviceOverlay.render(entries)
        val target = file.absoluteFile
        val temp = File(target.parentFile, target.name + TEMP_SUFFIX)
        val written = runCatching {
            target.parentFile?.mkdirs()
            temp.writeText(rendered)
            // بعض الأنظمة لا تُبدّل عبر renameTo إن كان الهدف قائمًا؛ والحذف هنا مقصود
            // ومحدود: ملفّنا نفسه في مجلّد التطبيق، لا ملفّ نظام. **ونتيجة المحاولة الثانية
            // تُقرأ أيضًا** — إغفالها يجعل دالة تُعلن نجاحًا لم يقع، والقراءة المرتجعة بعدها
            // تكشفه، لكن الكود يجب أن يقول ما يفعله لا أن يُنقذه فحصٌ لاحق.
            temp.renameTo(target) || (target.delete() && temp.renameTo(target))
        }.getOrDefault(false)
        if (!written) {
            runCatching { temp.delete() }
            return false
        }
        // القراءة المرتجعة: «كُتب» تعني «يمكن أن يُقرأ»، وإلا فالشاشة ستُعلّم جهازًا لا ملفّ له.
        val readBack = runCatching { target.readText() }.getOrNull()
        return readBack == rendered
    }

    /** يضيف سطرًا (أو يُحدّث صنف الجهاز نفسه) — قراءة ثم كتابة، والملفّ هو المصدر. */
    fun teach(entry: HardwareDeviceOverlay.Entry): Boolean =
        save(HardwareDeviceOverlay.with(load().entries, entry))

    /** ينسى تعليم جهاز. جهازٌ غير مُعلَّم ليس خطأً. */
    fun forget(device: String): Boolean =
        save(HardwareDeviceOverlay.without(load().entries, device))

    companion object {

        const val FILE_NAME = "hardware-overlay.txt"

        /** سقف حجم الملفّ — عشرات الأسطر تكفي، وما فوقه سلوك غير متوقّع لا ملفّ تعليم. */
        const val MAX_BYTES = 32 * 1024L

        private const val TEMP_SUFFIX = ".tmp"

        /** الملفّ في مجلّد التطبيق الخاصّ: لا صلاحية إضافية ولا مسار نظام. */
        fun inDirectory(directory: File): HardwareOverlayStore =
            HardwareOverlayStore(File(directory, FILE_NAME))
    }
}
