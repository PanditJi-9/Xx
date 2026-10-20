/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.hardware

import nd.max.MaxManagerPaths

/**
 * لقطة اكتشاف GPU منشورة من الرفيق الجذري — الرابط بين الاكتشاف المميّز وأطلس.
 *
 * والمشكلة المقيسة (سنابدراجون SM8650 في حزمة ٢٠٢٦-١٠-١٤): عملية التطبيق (`untrusted_app`)
 * ممنوعة SELinux من عبور `/sys/class/devfreq`، فاكتشافها المحلي يعود فارغًا وتُسجَّل القدرة
 * `not-detected` — بينما الرفيق (جذر) يرى العقد نفسها. فالرفيق ينشر ما رآه كل ٥ دقائق،
 * وعملية التطبيق **تتبنّاه بوسمه** حين لا يجد عدّها شيئًا، فتدخل العقد في الاختيار
 * والملاءمات والبصمة بدل الغياب.
 *
 * **وضمانات الصدق الأربع — لا ذكاء خارق ولا تخمين:**
 * 1. لا تُنشَر الفراغ: لقطة بلا جهاز واحد لا تُكتب، فلا تُخفي «لم يُقَس بعد» عن «لا عتاد».
 * 2. الوسم يُرافق: كل جهاز متبنّى يحمل `companion-published` في دليله فيُقرأ مصدره.
 * 3. القيود نفسها: الجهاز المتبنّى يمرّ بالاختيار والملاءمات والمحكّم ذاتها — بلا سلّم
 *    يُصنَّف للقياس لا للكتابة، فلا سلوك جديد بلا دليل جديد.
 * 4. الطزاجة تُحكَم: إصدار غير `1` أو لقطة أقدم من المهلة أو سطر معطوب ⇒ تُهمَل كلها
 *    ويبقى سلوك اليوم (فراغ محلي = غياب) — فالقناة مضافة لا بديلة.
 *
 * والصيغة سطورًا (`v=` أولًا) كقرينها [`PerAppHardwareStatus`]: الترميز والفكّ خالصان
 * يُقاسان بلا جذر وبلا قرص، والقرص يُلمَس في `read`/`write` وحدهما.
 */
object GpuDiscoverySnapshot {

    const val VERSION = 1

    /** بين نشر وآخر من الرفيق — التعداد الكامل كل ٥ دقائق لا كل دورة (البطارية). */
    const val PUBLISH_INTERVAL_MS = 300_000L

    /**
     * عمر اللقطة المقبول: ١٥ دقيقة (ثلاثة أضعاف النشر) — تتسامح مع موت الرفيق لحظات
     * دون أن تُبقي قرارات على قياس ميت. والطابع الزمني المستقبلي (ساعة راجعة) يُهمَل
     * لا يُقبَل: الاتجاه الآمن عند الشك هو التجاهل لا التبني.
     */
    const val SNAPSHOT_TTL_MS = 900_000L

    /** سطر واحد لكل جهاز: ١٥ حقلًا بعد الكلمة، و`none` للغائب — لا فراغات أبدًا. */
    private const val EXPECTED_FIELDS = 15

    /** طازجة؟ — خالصة تُقاس وحدها. */
    fun isFresh(atMs: Long, nowMs: Long): Boolean =
        nowMs >= atMs && nowMs - atMs < SNAPSHOT_TTL_MS

    /** ترميز خالص: أجهزة الاختيار كما رآها الرفيق، والنتيجة تُحفظ معها لئلا تُعاد حسابًا. */
    fun encode(atMs: Long, entries: List<Entry>): String = buildString {
        appendLine("v=$VERSION")
        appendLine("at=$atMs")
        entries.forEach { (device, score) -> appendLine(encodeDevice(device, score)) }
    }

    /** سطر جهاز واحد — مفصول ليُختبر مستقلًا عن الملفّ كاملًا. */
    internal fun encodeDevice(device: GpuHardwareBackend.Device, score: Int): String =
        listOf(
            "device",
            singleLine(device.path),
            singleLine(device.name),
            device.family.name,
            score.toString(),
            csvOrNone(device.frequencies.map(Long::toString)),
            device.minFreq?.toString() ?: "none",
            device.maxFreq?.toString() ?: "none",
            device.currentFreq?.toString() ?: "none",
            singleLine(device.governor ?: "none"),
            csvOrNone(device.governors),
            device.frequencyUnit.name,
            csvOrNone(device.evidence),
            singleLine(device.mtkFixedIndexPath ?: "none"),
            mtkTableOrNone(device.mtkOppIndexByFrequency),
        ).joinToString(" ")

    /** اللقطة أو `null`: إصدار مجهول أو زمن غائب أو بلا جهاز صالح واحد — لا تخمين. */
    fun parse(text: String): Snapshot? {
        var atMs: Long? = null
        val entries = mutableListOf<Entry>()
        text.lineSequence().forEach { raw ->
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEach
            val head = line.substringBefore(' ')
            when {
                head.startsWith("v=") -> if (head != "v=$VERSION") return null
                head.startsWith("at=") -> atMs = head.substringAfter("at=").toLongOrNull()
                head == "device" -> parseDevice(line)?.let(entries::add)
            }
        }
        val at = atMs ?: return null
        if (entries.isEmpty()) return null
        return Snapshot(atMs = at, entries = entries)
    }

    /** جهاز واحد ونتيجته كما رآها الرفيق — تُبنى منهما المرشّحة نفسها بلا إعادة حساب. */
    data class Entry(val device: GpuHardwareBackend.Device, val score: Int)

    data class Snapshot(val atMs: Long, val entries: List<Entry>) {
        /** الأجهزة وحدها — لمن يعرض ولا يرشّح (البصمة والتشخيص). */
        val devices: List<GpuHardwareBackend.Device> get() = entries.map(Entry::device)
    }

    /**
     * قراءة اللقطة — للعملية العمياء. قديمة أو معطوبة ⇒ `null` ويبقى سلوك اليوم.
     * والقراءة نفسها عبر الطبقات ([RootFileAccess.read]) لا قراءة مباشرة.
     */
    fun read(nowMs: Long = System.currentTimeMillis()): Snapshot? {
        val text = runCatching { RootFileAccess.read(MaxManagerPaths.GPU_DISCOVERY_SNAPSHOT) }
            .getOrNull()?.takeIf { it.isNotBlank() } ?: return null
        val snapshot = parse(text) ?: return null
        return snapshot.takeIf { isFresh(snapshot.atMs, nowMs) }
    }

    /** كتابة اللقطة — من الرفيق وحده. العائد: نجاح الأمر (لا حكم على القيمة). */
    fun write(text: String): Boolean =
        runCatching { RootFileAccess.atomicWriteText(MaxManagerPaths.GPU_DISCOVERY_SNAPSHOT, text) }
            .getOrDefault(false)

    private fun parseDevice(line: String): Entry? {
        val fields = line.split(' ')
        if (fields.size != EXPECTED_FIELDS || fields[0] != "device") return null
        val family = runCatching { GpuHardwareBackend.Family.valueOf(fields[3]) }
            .getOrDefault(GpuHardwareBackend.Family.UNKNOWN)
        val unit = runCatching { GpuHardwareBackend.FrequencyUnit.valueOf(fields[11]) }
            .getOrDefault(GpuHardwareBackend.FrequencyUnit.AMBIGUOUS)
        val score = fields[4].toIntOrNull() ?: return null
        if (score < 0) return null
        val device = GpuHardwareBackend.Device(
            path = fields[1],
            name = fields[2].ifBlank { fields[1].substringAfterLast('/') },
            governor = fields[9].takeUnless { it == "none" },
            governors = parseCsv(fields[10]),
            minFreq = fields[6].toLongOrNull(),
            maxFreq = fields[7].toLongOrNull(),
            currentFreq = fields[8].toLongOrNull(),
            frequencies = parseCsv(fields[5]).mapNotNull(String::toLongOrNull)
                .filter { it > 0L }.distinct().sorted(),
            family = family,
            frequencyUnit = unit,
            // الصلاحية تُعاد قراءتها محليًا عند الاستعمال: اللقطة تنقل **الملاحظة**
            // لا الصلاحية، فالكتابة تُثبت في عمليتها دائمًا — وهذا ما يجعل التبني آمنًا.
            minWritable = false,
            maxWritable = false,
            governorWritable = false,
            mtkFixedIndexPath = fields[13].takeUnless { it == "none" },
            mtkLockWritable = false,
            mtkOppIndexByFrequency = parseMtkTable(fields[14]),
            // والوسم يُرافق: متبنّى من الرفيق لا مقيس هنا — فيُقرأ مصدره في البصمة والحزمة.
            evidence = parseCsv(fields[12]) + "companion-published",
        )
        return Entry(device, score)
    }

    private fun parseCsv(raw: String): List<String> =
        if (raw == "none") emptyList() else raw.split(',').filter(String::isNotBlank)

    private fun csvOrNone(values: List<String>): String =
        if (values.isEmpty()) "none" else values.joinToString(",")

    private fun mtkTableOrNone(table: Map<Long, String>): String =
        if (table.isEmpty()) "none"
        else table.entries.sortedBy { it.key }.joinToString(",") { "${it.key}=${singleLine(it.value)}" }

    private fun parseMtkTable(raw: String): Map<Long, String> {
        if (raw == "none") return emptyMap()
        return raw.split(',').mapNotNull { entry ->
            val key = entry.substringBefore('=').toLongOrNull() ?: return@mapNotNull null
            val value = entry.substringAfter('=', "").takeIf(String::isNotBlank) ?: return@mapNotNull null
            key to value
        }.toMap()
    }

    /** قيمة حقل واحد: بلا أي فراغ، فيبقى السطر حقولًا ثابتة العدد قابلة للبحث. */
    private fun singleLine(value: String): String =
        value.replace(Regex("[\\s]+"), "_").take(160)
}
