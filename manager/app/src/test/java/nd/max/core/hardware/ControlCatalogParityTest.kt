/* Copyright (C) 2026 Nader Magdy. All rights reserved. */
package nd.max.core.hardware

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/** مدخل كتالوج واحد كما يُقرأ من البيانات — بلا سلوك. */
internal data class CatalogEntry(
    val key: String,
    val transport: String,
    val candidates: List<String>,
    val write: List<String>,
    val danger: String,
    val readOnly: Boolean = false,
    val readOnlyReason: String? = null,
)

/**
 * مفاتيح بقيت **قراءةً فقط** بقرار مكتوب (ADR-89).
 *
 * **ولماذا قائمة في الاختبار:** القرار نفسه يجب أن يُقاس. فلو حُوّل أحدها إلى قابل للكتابة بلا
 * جولة قرار جديدة، سقط الاختبار بالاسم — بدل أن يمرّ التغيير صامتًا لأن «الكتالوج ما زال سليمًا».
 */
internal val IMPOUNDED_READ_ONLY_KEYS = listOf(
    "cpu.perfserv_freq",
    "cpu_core_online:{n}",
    "gpu_governor_policy:{device}",
    "touch_gesture:{path}",
    "zram.{n}.size",
)

/** مفردات الأسباب المعلنة — مطابقة لِـ`READ_ONLY_REASONS` في `tools/gen_control_catalog.py`. */
internal val READ_ONLY_REASONS = setOf(
    "EXISTING_WRITER_NOT_ROUTED",
    "AWAITING_CANONICAL_KEY",
    "NO_WRITE_PATH",
    "PROTECTED_BY_DESIGN",
)

/**
 * تطابق الكتالوج المعلن مع مفاتيح `HardwareControlKey` والمسارات المقيسة (M1).
 *
 * **ولماذا اختبار لا أداة**: الأرقام التي يقيسها (١٤ بادئة · مرشّحات · حراسة `PROTECTED`) تُقاس في
 * نفس لحظة بناء الحزمة، فمفتاحٌ مخترع في `core/hardware` يُسقط البناء بدل أن يمرّ صامتًا.
 *
 * **وثلاث حالات سلبية شرط قبول لا تحسين**: دالّة لا تُكسر لا تقيس شيئًا (§7.3 من الخطة).
 */
class ControlCatalogParityTest {

    private lateinit var entries: List<CatalogEntry>
    private lateinit var unmapped: List<String>
    private lateinit var prefixes: List<String>
    private lateinit var measured: List<String>

    private fun firstDirectory(vararg relative: String): File =
        relative.map(::File).firstOrNull { it.isDirectory }
            ?: error("لم يُوجد أيّ من المسارات: ${relative.toList()}")

    @Before
    fun load() {
        val assets = firstDirectory("src/main/assets", "app/src/main/assets", "../app/src/main/assets")
        val catalogFile = File(assets, "control_catalog.json")
        assumeTrue("الكتالوج غير موجود؛ لم يُقَس", catalogFile.isFile)

        val root = Json.parseToJsonElement(catalogFile.readText()).jsonObject
        entries = root.getValue("entries").jsonArray.map { element ->
            val entry = element.jsonObject
            CatalogEntry(
                key = entry.str("key"),
                transport = entry.str("transport"),
                candidates = entry.getValue("candidates").jsonArray.map { it.jsonObject.str("path") },
                write = entry.getValue("privilege").jsonObject.getValue("write").jsonArray
                    .map { it.jsonPrimitive.content },
                danger = entry.str("danger"),
                readOnly = entry["read_only_until_canonical_key"]?.jsonPrimitive?.content?.toBoolean() == true,
                readOnlyReason = entry["read_only_reason"]?.jsonPrimitive?.content,
            )
        }
        unmapped = root.getValue("unmapped").jsonArray.map { it.jsonObject.str("path") }

        val sources = firstDirectory("src/main/java/nd/max", "app/src/main/java/nd/max", "../app/src/main/java/nd/max")
        val controlKey = File(sources, "core/hardware/HardwareControlKey.kt").readText()
        prefixes = Regex("""const val [A-Z0-9_]+ = "([^"]*)"""")
            .findAll(controlKey).map { it.groupValues[1] }.toList()

        val hardware = File(sources, "core/hardware")
        measured = hardware.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { file ->
                val text = file.readText()
                val system = Regex(""""(/sys/[^"]+)"""").findAll(text).map { it.groupValues[1] }
                val proc = Regex(""""(/proc/[^"]+)"""").findAll(text).map { it.groupValues[1] }
                (system + proc).toList()
            }
            .distinct()
            .sorted()
            .toList()
    }

    private fun JsonObject.str(key: String): String = getValue(key).jsonPrimitive.content

    // ───────────────────────────── الحالات الموجبة ─────────────────────────────

    @Test
    fun everyHardwareControlKeyPrefixHasAnEntry() {
        assertEquals("البوادئ المقيسة تغيّرت — حدّث المصدر لا الاختبار", 15, prefixes.size)
        assertEquals(emptyList<String>(), CatalogGaps.missingPrefixes(entries, prefixes))
    }

    @Test
    fun noEntryInventsAKeyOutsideHardwareControlKey() {
        assertEquals(emptyList<String>(), CatalogGaps.keysOutsideControlKey(entries, prefixes))
    }

    @Test
    fun aProtectedEntryNeverCarriesAWriteList() {
        assertEquals(emptyList<String>(), CatalogGaps.protectedWithWrite(entries))
    }

    @Test
    fun aNodeEntryNeverShipsWithoutCandidates() {
        assertEquals(emptyList<String>(), CatalogGaps.emptyNodeCandidates(entries))
    }

    @Test
    fun everyMeasuredPathIsInTheCatalogOrDeclaredUnmapped() {
        assertTrue("لم تُقرأ مسارات من core/hardware — الفحص أعمى", measured.isNotEmpty())
        assertEquals(
            "مسار مقيس خارج الكتالوج وخارج unmapped — أضفه بقرار لا بصمت",
            emptyList<String>(),
            CatalogGaps.uncoveredMeasured(measured, entries, unmapped),
        )
    }

    @Test
    fun theReadOnlyLedgerIsExactlyTheDecidedFive() {
        assertEquals(IMPOUNDED_READ_ONLY_KEYS.sorted(), entries.filter { it.readOnly }.map { it.key }.sorted())
    }

    @Test
    fun aReadOnlyEntryCarriesADeclaredReason() {
        assertEquals(emptyList<String>(), CatalogGaps.readOnlyWithoutReason(entries, READ_ONLY_REASONS))
        assertEquals(
            "سبب بلا عَلَم — سببٌ يتيم",
            emptyList<String>(),
            CatalogGaps.reasonWithoutReadOnly(entries),
        )
    }

    // ───────────────────────────── الحالات السلبية ─────────────────────────────

    @Test
    fun removingTheGpuEntryIsCaughtByPrefix() {
        val trimmed = entries.filterNot { it.key.startsWith(HardwareControlKey.GPU_FREQUENCY_PREFIX) }
        assertEquals(
            listOf(HardwareControlKey.GPU_FREQUENCY_PREFIX),
            CatalogGaps.missingPrefixes(trimmed, prefixes),
        )
    }

    @Test
    fun anInventedKeyIsCaughtByName() {
        val mutated = entries + CatalogEntry("my.custom.knob", "PROP", emptyList(), listOf("MAGISK"), "LOW")
        assertEquals(listOf("my.custom.knob"), CatalogGaps.keysOutsideControlKey(mutated, prefixes))
    }

    @Test
    fun aProtectedEntryWithAWriteListIsCaught() {
        val key = HardwareControlKey.TOUCH_BOOST_PREFIX + "x"
        val mutated = entries + CatalogEntry(
            key = key,
            transport = "NODE",
            candidates = listOf("/sys/android_touch/game_switch_enable"),
            write = listOf("MAGISK"),
            danger = "PROTECTED",
        )
        assertEquals(listOf(key), CatalogGaps.protectedWithWrite(mutated))
    }

    @Test
    fun aNodeEntryWithoutCandidatesIsCaught() {
        val mutated = entries + CatalogEntry(
            key = HardwareControlKey.CPU_BOOST,
            transport = "NODE",
            candidates = emptyList(),
            write = emptyList(),
            danger = "LOW",
        )
        assertTrue(CatalogGaps.emptyNodeCandidates(mutated).contains(HardwareControlKey.CPU_BOOST))
    }

    @Test
    fun aReadOnlyEntryWithoutAReasonIsCaughtByName() {
        val key = "cpu.perfserv_freq"
        val mutated = listOf(CatalogEntry(key, "NODE", listOf("/proc/x"), listOf("MAGISK"), "HIGH", readOnly = true))
        assertEquals(listOf(key), CatalogGaps.readOnlyWithoutReason(mutated, READ_ONLY_REASONS))
    }

    @Test
    fun anInventedReadOnlyReasonIsCaught() {
        val key = "zram.{n}.size"
        val mutated = listOf(
            CatalogEntry(key, "NODE", listOf("/sys/block/zram0/disksize"), emptyList(), "HIGH",
                readOnly = true, readOnlyReason = "BECAUSE"),
        )
        assertEquals(listOf(key), CatalogGaps.readOnlyWithoutReason(mutated, READ_ONLY_REASONS))
    }
}

/**
 * الدوالّ الصافية التي يقيسها الاختبار — بلا قراءة ملفّات.
 *
 * و`internal` لأنها ليست عقد إنتاج: تُقاس هنا ولا تُستدعى من الشاشة.
 */
internal object CatalogGaps {

    fun missingPrefixes(entries: List<CatalogEntry>, prefixes: List<String>): List<String> =
        prefixes.filter { prefix -> prefix.isNotEmpty() && entries.none { it.key.startsWith(prefix) } }

    /**
     * **الكتابة فقط** هي التي يُحرّم عليها مفتاحٌ غير مُشتقّ من `HardwareControlKey`.
     *
     * ومسارات القراءة (`/proc/loadavg` وأمثالها) لا مقبض تحكّم لها؛ فإدخالها بالكتابة كان سيعني
     * مقبضًا ثابتًا مخترعًا في الكتالوج — وهو أخطر من غيابه. فالقيد على `write.isNotEmpty()`.
     */
    fun keysOutsideControlKey(entries: List<CatalogEntry>, prefixes: List<String>): List<String> =
        entries.filter { it.write.isNotEmpty() }
            .map { it.key }
            .filter { key -> prefixes.none { it.isNotEmpty() && key.startsWith(it) } }

    fun protectedWithWrite(entries: List<CatalogEntry>): List<String> =
        entries.filter { it.danger == "PROTECTED" && it.write.isNotEmpty() }.map { it.key }

    fun emptyNodeCandidates(entries: List<CatalogEntry>): List<String> =
        entries.filter { it.transport == "NODE" && it.candidates.isEmpty() }.map { it.key }

    /** قراءة-فقط بلا سبب معلن — أو بسبب خارج المفردات. */
    fun readOnlyWithoutReason(entries: List<CatalogEntry>, reasons: Set<String>): List<String> =
        entries.filter { entry ->
            entry.readOnly && entry.readOnlyReason?.let { it in reasons } != true
        }.map { it.key }

    /** سببٌ كُتب بلا عَلَم — سببٌ يتيم يشير إلى قرار غير قائم. */
    fun reasonWithoutReadOnly(entries: List<CatalogEntry>): List<String> =
        entries.filter { !it.readOnly && it.readOnlyReason != null }.map { it.key }

    fun uncoveredMeasured(
        measured: List<String>,
        entries: List<CatalogEntry>,
        unmapped: List<String>,
    ): List<String> {
        val covered = entries.flatMap { it.candidates }.toMutableSet()
        covered += unmapped
        return measured.filterNot { covered.contains(it) }
    }
}
