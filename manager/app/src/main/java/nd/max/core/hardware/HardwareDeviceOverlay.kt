/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.hardware

/**
 * **تصنيف المستخدم** لعُقَد `devfreq` التي لا تعرفها قواعدُنا — «علّم أطلس جهازك».
 *
 * لماذا وُجد
 * ----------
 * قواعد التصنيف في [MemoryUfsHardwareBackend] مفرداتٌ مرصودة من سائقين معلومين
 * (`dvfsrc` · `cpubw` · `mif` · `ufshci` …). وجهازٌ باسم لا نعرفه يعطي اليوم نتيجةً واحدة:
 * **يُسقَط بصمت** — فيقرأ المستخدم «لا ميزة على جهازك» وهو لا يدري أنّ العقدة موجودة وأن اسمها
 * فقط هو ما لم يُقرأ. والقاعدة في هذا المستودع أن المجهول **يُعلَن** ولا يُخمَّن، وأن يُسمّى ما
 * ينقص بالضبط.
 *
 * وهنا يأتي الفرق عن «جدول أجهزة» ثانٍ: نحن **لا نضيف أسماءً** إلى الشفرة (وهو ما يجعل الطريقة
 * تعمل على الجهاز الذي كُتبت عليه وحدها)، بل نجعل **الجهاز نفسه هو المصدر**: أطلس يسرد ما رآه
 * ولم يعرفه، والمستخدم يعلّم الاسم صنفَه مرّة واحدة، فيعمل التصنيف على نواة لم نرها بعد.
 *
 * والقواعد التي تجعل التعليم آمنًا
 * -------------------------------
 * 1. **القاعدة المبنية تفوز دائمًا.** تعليمٌ لجهاز تعرفه قواعدنا لا يُغيّر صنفه — وإلا صار
 *    للمقبض تعريفان بحسب ملفٍّ في مجلد التطبيق. (يُقبل السطر ويُهمَل أثره، ويُعلَّم ذلك.)
 * 2. **لا تعليم لرسوميات.** اسمٌ يحمل رمزًا رسوميًّا يُرفض بسببه: `gpu_frequency` له مالكه
 *    القائم، وتعليمُ عنقود `mali` ذاكرةً يعني **مقبضًا له كاتبان** — وهو ما يمنعه المستودع كلّه.
 * 3. **صنفان فقط.** `MEMORY` و`STORAGE`. أي صنف آخر (معالج، رسوميات، حرارة) يُرفض: هذه القدرة
 *    وُجدت لمقبضَي تردّد الذاكرة والتخزين، ولا تُبنى بها ملكية ثالثة.
 * 4. **الاسم مُقنَّن.** مقطع واحد (`[A-Za-z0-9][A-Za-z0-9._-]{0,63}`) — لا مسار ولا `*` ولا
 *    مسافة: التعليم على مسار متغيّر يجعل المفتاح يتغيّر بنفسه.
 * 5. **التعليم يُقاس قبل أن يُخزَّن** ([MemoryUfsHardwareBackend.probe]): لا يُقبل اسمٌ لعقدة
 *    لم تُقرأ، ولا عقدة ليست واجهة تردّد. وما لم يُقَس لا يُكتب في الملفّ.
 *
 * والصيغة (نصّ بسيط يقبل التعديل بيد المستخدم):

 * ```text
 * # Max Atlas — device classification overlay
 * dvfsrc        memory
 * 112b0000.ufshci  storage
 * ```
 *
 * وكل سطر يُقبل أو يُرفض **بسبب مكتوب** ([Rejection]); وما يُرفض لا يُسقط الملفّ.
 *
 * ومتى يُقرأ الملفّ — ثلاث نقاط معلنة، ولا رابعة
 * --------------------------------------------
 * 1. **إقلاع العملية** (`MaxManagerApplication`): كي يرى كل من يقرأ قدرةً، من أوّل نداء، التصنيف
 *    نفسه (خريطة أطلس والقدرة والتشخيص وMAX AI).
 * 2. **فتح شاشة الذاكرة وUFS** (وفي كل تعديل منها: تعليم أو نسيان).
 * 3. **بعد كل تعليم/نسيان** — بإعادة قراءة فوريّة، لا بعد دورة الشاشة.
 *
 * **وحدّ مُعلن:** ملفّ يُعدَّل بيد خارجيًّا بينما التطبيق يعمل لا يُقرأ حتى يُعاد فتح الشاشة أو
 * يُعاد تشغيل التطبيق — ولو قُرئ في كل دورة (كل ثانيتين) لصار الملفّ مصدرًا يُقاس مرّتين في
 * الثانية بلا حاجة. فالتعديل الخارجيّ يعمل، لكن وقتَه معلوم: عند أوّل قراءة تالية.
 */
object HardwareDeviceOverlay {

    /**
     * أقصى عدد أسطر مُثمرة.
     *
     * والحدّ ليس ترفًا: الملفّ يُقرأ في كل اكتشاف (ودورة الشاشة كل ثانيتين)، وملفّ بآلاف الأسطر
     * يجعل كل نداء يمسح قائمة لا تنتهي بلا معلومة جديدة. وما فوقه يُرفض برمزه لا يُقتطع بصمت.
     */
    const val MAX_ENTRIES = 64

    /** أقصى طول لاسم عقدة. رموز sysfs أقصر من هذا بكثير؛ والحدّ يمنع رمي نصّ في اسم. */
    const val MAX_NAME_LENGTH = 64

    /** الأصناف التي يقبلها هذا الملفّ — وليس كل [MemoryUfsHardwareBackend.Kind] بالضرورة مستقبلًا. */
    val SUPPORTED_KINDS: List<MemoryUfsHardwareBackend.Kind> = listOf(
        MemoryUfsHardwareBackend.Kind.MEMORY,
        MemoryUfsHardwareBackend.Kind.STORAGE,
    )

    /** مقطعُ مسارٍ واحد: يبدأ بحرف أو رقم، وبقية الحروف/الأرقام و`.` و`_` و`-` فقط. */
    private val DEVICE_NAME = Regex("^[A-Za-z0-9][A-Za-z0-9._-]{0," + (MAX_NAME_LENGTH - 1) + "}$")

    /** فواصل الحقول المقبولة: مسافة أو `=` أو `,` أو `;` — والتبويب مسافة. */
    private val FIELD_SEPARATORS = Regex("[\\s=,;]+")

    private const val COMMENT = '#'

    private const val MEMORY_TOKEN = "memory"
    private const val STORAGE_TOKEN = "storage"

    /** سطر تعليم واحد: اسم عقدة `devfreq` وصنفه. */
    data class Entry(val device: String, val kind: MemoryUfsHardwareBackend.Kind) {
        init {
            require(isCanonicalName(device)) { "device name is not canonical: $device" }
            require(kind in SUPPORTED_KINDS) { "kind is outside this overlay's scope: $kind" }
        }

        /** السطر كما يُكتب في الملفّ — `/` صيغةٌ واحدة للقراءة والكتابة. */
        val line: String get() = "$device\t${token(kind)}"
    }

    /** سطرٌ لم يُقبل، وسببه كرمز ثابت (والجملة في الواجهة). */
    data class Rejection(val line: Int, val text: String, val reason: String)

    /** نتيجة قراءة ملفّ: ما قُبل، وما رُفض باسم سببه، وما أُهمل أثره بقاعدة (لا يُسقط الملفّ). */
    data class Parsed(
        val entries: List<Entry> = emptyList(),
        val rejections: List<Rejection> = emptyList(),
    ) {
        val usable: Boolean get() = entries.isNotEmpty()

        fun kindOf(device: String): MemoryUfsHardwareBackend.Kind? =
            entries.firstOrNull { it.device == device }?.kind

        val deviceNames: Set<String> get() = entries.mapTo(linkedSetOf()) { it.device }
    }

    const val REASON_SHAPE = "overlay-line-shape"
    const val REASON_NAME_INVALID = "overlay-name-invalid"
    const val REASON_UNKNOWN_KIND = "overlay-kind-unknown"
    const val REASON_GRAPHICS_OWNED = "overlay-graphics-owned"
    const val REASON_CONFLICT = "overlay-device-conflict"
    const val REASON_LIMIT = "overlay-entry-limit"

    /** الملفّ نفسه لم يُقرأ (صلاحية) — والسطر `0` يعني «الملفّ» لا سطرًا فيه. */
    const val REASON_UNREADABLE = "overlay-file-unreadable"

    /** الملفّ أكبر من السقف المعلن في [HardwareOverlayStore.MAX_BYTES]. */
    const val REASON_TOO_LARGE = "overlay-file-too-large"

    /**
     * يقرأ ملفّ المستخدم سطرًا سطرًا.
     *
     * والمحلّل **لا يرمي ولا يُصلح**: سطر مشوّه يُرفض بسببٍ مكتوب، وبقية الملفّ تبقى صالحة —
     * فإعادة كتابة ملفّ كامل على خطأ سطر واحد تجعل التعليم تجربةً تُعاقب على الخطأ المطبعيّ.
     */
    fun parse(text: String?): Parsed {
        if (text.isNullOrBlank()) return Parsed()
        val entries = linkedMapOf<String, Entry>()
        val rejections = mutableListOf<Rejection>()

        text.lineSequence().forEachIndexed { index, raw ->
            val lineNumber = index + 1
            val body = raw.substringBefore(COMMENT).trim()
            if (body.isEmpty()) return@forEachIndexed

            val tokens = body.split(FIELD_SEPARATORS).filter(String::isNotBlank)
            if (tokens.size != 2) {
                rejections += Rejection(lineNumber, raw.trim(), REASON_SHAPE)
                return@forEachIndexed
            }
            val (device, kindToken) = tokens
            if (!isCanonicalName(device)) {
                rejections += Rejection(lineNumber, raw.trim(), REASON_NAME_INVALID)
                return@forEachIndexed
            }
            val kind = kindOfToken(kindToken)
            if (kind == null) {
                rejections += Rejection(lineNumber, raw.trim(), REASON_UNKNOWN_KIND)
                return@forEachIndexed
            }
            if (MemoryUfsHardwareBackend.graphicsOwned(device)) {
                // الرمز الرسوميّ مانع هنا كما هو مانع في التصنيف: مالك GPU قائم، ولا ثاني له.
                rejections += Rejection(lineNumber, raw.trim(), REASON_GRAPHICS_OWNED)
                return@forEachIndexed
            }
            val known = entries[device]
            if (known != null) {
                // تكرارٌ بنفس الصنف يُقبل مرّة (ملفّ يُنسخ يدويًّا يتكرّر)، وبصنف آخر يُرفض:
                // «آخر سطر يفوز» تجعل معنى الملفّ يعتمد على ترتيبه لا على ما فيه.
                if (known.kind != kind) rejections += Rejection(lineNumber, raw.trim(), REASON_CONFLICT)
                return@forEachIndexed
            }
            if (entries.size >= MAX_ENTRIES) {
                rejections += Rejection(lineNumber, raw.trim(), REASON_LIMIT)
                return@forEachIndexed
            }
            entries[device] = Entry(device, kind)
        }
        return Parsed(entries = entries.values.toList(), rejections = rejections)
    }

    /** يكتب الملفّ: رأسٌ يشرح الصيغة، ثم الأسطر مرتَّبةً بالاسم — فترتيب الكتابة ليس عشوائيًّا. */
    fun render(entries: List<Entry>): String = buildString {
        appendLine("# Max Atlas — device classification overlay")
        appendLine("# <device>  <memory|storage>   (one device per line; '#' starts a comment)")
        entries.distinctBy { it.device }.sortedBy { it.device }.forEach { appendLine(it.line) }
    }

    /** إضافة سطر إلى مجموعة قائمة: يُستبدل صنف الجهاز نفسه إن اختلف، ولا يتكرّر. */
    fun with(entries: List<Entry>, entry: Entry): List<Entry> =
        (entries.filterNot { it.device == entry.device } + entry).sortedBy { it.device }

    /** إزالة تعليم جهاز — بلا خطأ إن لم يكن مُعلَّمًا (نسيانٌ لا فشل). */
    fun without(entries: List<Entry>, device: String): List<Entry> =
        entries.filterNot { it.device == device }

    /**
     * هل الاسم مقطعٌ مقنَّن؟ — موضعٌ واحد للحكم، يتّفق فيه المحلّل ([parse]) والفحص
     * ([MemoryUfsHardwareBackend.probe]) وبناء السطر ([Entry]).
     */
    fun isCanonicalName(name: String): Boolean = DEVICE_NAME.matches(name)

    /** رمز الصنف في الملفّ. */
    fun token(kind: MemoryUfsHardwareBackend.Kind): String = when (kind) {
        MemoryUfsHardwareBackend.Kind.MEMORY -> MEMORY_TOKEN
        MemoryUfsHardwareBackend.Kind.STORAGE -> STORAGE_TOKEN
    }

    /** رمز الصنف ← صنف؛ `null` لأي رمز آخر (ومنه أسماء الأصناف نفسها بلا معنى هنا). */
    fun kindOfToken(token: String): MemoryUfsHardwareBackend.Kind? = when (token.lowercase()) {
        MEMORY_TOKEN, "mem", "ram", "ddr" -> MemoryUfsHardwareBackend.Kind.MEMORY
        STORAGE_TOKEN, "ufs", "emmc", "nvme", "disk" -> MemoryUfsHardwareBackend.Kind.STORAGE
        else -> null
    }
}
