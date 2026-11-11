/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * **حرّاس الأبواب** — كل وجهة مرئية يجب أن يفتحها سطحٌ يذكرها، لا أن تُسجَّل وحدها.
 *
 * العطب المحروس وقع فعلًا: نُقلت `SetEdit` · `ActivityLauncher` · `FileManager` إلى
 * `DeviceLab` في محاولة M1b، والمختبر لا يعرض أبناءه من السجلّ — فصارت الثلاث وجهات
 * **مسجَّلة وبلا باب**: لا قائمة تذكرها، ولا محور يُولّدها. لم يكشفه أي اختبار قائم،
 * وكشفه المالك بقراءة الشاشة. وهذا الحارس يمنعه بنيويًّا قبل أن يُبنى.
 *
 * والقاعدة مقيسة من الشجرة نفسها لا مكتوبة مرتين:
 * - **المحاور** (`hub_*`) تُولّد صفوفها من السجلّ عبر `maxHubRows`، فأبناؤها مغطَّون بحكم البناء.
 * - **`Control`** صفحته تُذكر وجهاتها بالاسم في `ControlLayoutModel.kt` (نطاقات + أدوات + فحص).
 * - **بقية الآباء** تُذكر وجهاتها بالاسم في شاشتهم (`Settings` · `Diagnostics` · `DeviceLab` ·
 *   `Apps` · `MaxAi`).
 * - **المخفية بأمر المالك** مستثناة: وجودها في السجلّ مقصود، وغيابها عن كل قائمة هو الأمر نفسه.
 */
class DestinationDoorTest {
    private lateinit var sourceRoot: File

    @Before
    fun locateSourceRoot() {
        val found = listOf(
            File("src/main/java/nd/max"),
            File("app/src/main/java/nd/max"),
            File("../app/src/main/java/nd/max"),
            File("manager/app/src/main/java/nd/max"),
        ).firstOrNull { it.isDirectory }
        assumeTrue("Cannot locate nd.max sources; guard not evaluated", found != null)
        sourceRoot = found!!
    }

    /** الكود دون تعليقاته: تُقاس البنية، ولا تُقاس صياغة التعليق. */
    private fun read(path: String): String = File(sourceRoot, path).readText()
        .replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")
        .replace(Regex("//[^\\n]*"), "")

    private data class Dest(
        val name: String,
        val route: String,
        val parent: String?,
        val hidden: Boolean,
        /** له مقعد دائم في الشريط السفلي ⇒ الشريط نفسه بابه، فلا يُطلب له باب آخر. */
        val seated: Boolean,
    )

    /** كل تسجيل في السجلّ: الاسم والمسار والأب — بتفكيك وسائط متوازن لا بتعبير سطري. */
    private fun destinations(): List<Dest> {
        val src = read("ui/navigation/MaxDestinations.kt")
        val marker = "data object "
        val out = mutableListOf<Dest>()
        var index = 0
        while (true) {
            val start = src.indexOf(marker, index)
            if (start < 0) break
            val head = src.indexOf(" : MaxDestination(", start + marker.length)
            if (head < 0) {
                index = start + marker.length
                continue
            }
            val name = src.substring(start + marker.length, head).trim()
            val open = head + " : MaxDestination(".length
            var depth = 1
            var i = open
            while (i < src.length && depth > 0) {
                when (src[i]) {
                    '(' -> depth++
                    ')' -> depth--
                }
                i++
            }
            val body = src.substring(open, i - 1)
            val args = splitTopLevel(body)
            val route = args.firstOrNull()?.trim()?.trim('"').orEmpty()
            // `parent = null` صريح في بعض التسجيلات (جذرٌ بلا أب) — وهو ليس اسم أب،
            // وعدّه اسمًا يسقط الحارس على آبٍ غير مسجَّل اسمه «null».
            val parent = args.getOrNull(3)?.trim()
                ?.takeIf { it.matches(Regex("\\w+")) && it != "null" }
            out += Dest(
                name = name,
                route = route,
                parent = parent,
                hidden = body.contains("hidden = true"),
                seated = body.contains("isPrimary = true"),
            )
            index = i
        }
        return out
    }

    /** تفكيك الوسائط على الفواصل في المستوى الصفري — الأقواس داخل وسم لا تفصل. */
    private fun splitTopLevel(body: String): List<String> {
        val args = mutableListOf<String>()
        var depth = 0
        val current = StringBuilder()
        for (c in body) {
            when (c) {
                '(', '[', '{' -> {
                    depth++
                    current.append(c)
                }
                ')', ']', '}' -> {
                    depth--
                    current.append(c)
                }
                ',' -> if (depth == 0) {
                    args += current.toString().trim()
                    current.clear()
                } else {
                    current.append(c)
                }
                else -> current.append(c)
            }
        }
        args += current.toString().trim()
        return args
    }

    /**
     * الملف الذي يذكر أبناء الأب بالاسم. و`null` يعني «يُولّدهم من السجلّ» (محور).
     * وما ليس في الجدول ولا محورًا يفشل الاختبار بدل أن يمرّ صامتًا.
     */
    private fun doorFile(parent: Dest): String? = when {
        parent.route.startsWith("hub_") -> null
        parent.name == "Control" -> "ui/mainscreens/ControlLayoutModel.kt"
        parent.name == "Settings" -> "ui/mainscreens/SettingsScreen.kt"
        parent.name == "Diagnostics" -> "ui/mainscreens/DiagnosticsScreen.kt"
        parent.name == "DeviceLab" -> "ui/subscreens/DeviceLabScreen.kt"
        parent.name == "Apps" -> "ui/mainscreens/ApplistScreen.kt"
        parent.name == "MaxAi" -> "ui/mainscreens/MaxAiScreen.kt"
        else -> ""
    }

    @Test
    fun `every visible destination is listed by its parent`() {
        val all = destinations()
        val byName = all.associateBy { it.name }
        val orphans = mutableListOf<String>()

        all.filter { it.parent != null && !it.hidden }.forEach { child ->
            val parent = byName[child.parent]
            if (parent == null) {
                orphans += "${child.name} → ${child.parent} (أب غير مسجَّل)"
                return@forEach
            }
            val file = doorFile(parent)
            when {
                file == null -> Unit // المحور يُولّد أبناءه من السجلّ
                file.isEmpty() -> orphans += "${child.name} → ${parent.name} (لا سطح معروف يعرض أبناءه)"
                // وبابان مقبولان: الصفّ المكتوب بالاسم، أو القسم الذي يقرأ أبناء الأب من
                // السجلّ (`maxHubRows`). الثاني هو ما استُعمل في `Settings`/`Diagnostics`
                // بعد M1c، وفيه لا يظهر اسم الابن في الملف أصلًا — والمصدر واحد.
                !read(file).contains("MaxDestination.${child.name}") &&
                    !read(file).contains("maxHubRows(MaxDestination.${parent.name})") ->
                    orphans += "${child.name} → ${parent.name} ($file لا يذكره)"
            }
        }

        assertEquals(
            "وجهات مسجَّلة بلا باب — لا قائمة تذكرها ولا محور يُولّدها:\n" +
                orphans.joinToString("\n"),
            emptyList<String>(),
            orphans,
        )
    }

    @Test
    fun `no two destinations share a route`() {
        val duplicates = destinations()
            .groupBy { it.route }
            .filterValues { it.size > 1 }
            .map { (route, list) -> "$route ← ${list.joinToString { it.name }}" }

        assertEquals("مسارات مكرّرة في السجلّ:\n" + duplicates.joinToString("\n"), emptyList<String>(), duplicates)
    }

    @Test
    fun `every parent resolves to a registered destination`() {
        val names = destinations().map { it.name }.toSet()
        val missing = destinations()
            .mapNotNull { it.parent }
            .distinct()
            .filter { it !in names }

        assertTrue("آباء غير مسجَّلين: $missing", missing.isEmpty())
    }

    /**
     * **الجذور غير الأساسية — الثغرة التي كانت تُمرّر وجهةً بلا باب.**
     *
     * الحارس الأوّل يفحص `parent != null` وحده، فكل جذر مرئيّ يمرّ بلا فحص — وهذا بالضبط كيف عاشت
     * `HmaCompanion` بلا بابٍ إلا المُوجِّد. والجذر لا أبَ له في السجلّ، فلا يوجد ملفّ «أبناء أبيه»
     * يُفحص؛ فبابه يُعلَن هنا **بالاسم والملفّ**، ومن لا يجد له سطرًا في هذه الخريطة يسقط.
     *
     * والمقاعد الدائمة (`isPrimary`) مستثناة: الشريط السفلي هو بابها، ولا يُطلب لها باب ثانٍ.
     * ومدخل مُعلَن لجذرٍ لم يبقَ جذرًا (أو لم يبقَ) يسقط أيضًا — فالخريطة تُصان بالقانون نفسه.
     */
    private val rootDoors = mapOf(
        // بطاقة المحرّك في السطح الرئيسي — أعلى أولوية في التطبيق بعد السلامة.
        "MaxAi" to "ui/mainscreens/LegendaryHomeDashboard.kt",
        // بطاقة معلومات الجهاز في رفّ السطح الرئيسي.
        "DeviceInfo" to "ui/mainscreens/HomeDeckModel.kt",
        // شاشة البداية: يفتحها `MainActivity` حين لا يكون التهيئة الأولى قد تمّت.
        "GetStarted" to "MainActivity.kt",
    )

    @Test
    fun `every visible root without a bottom bar seat declares its door`() {
        val roots = destinations().filter { it.parent == null && !it.hidden && !it.seated }
        val broken = roots.mapNotNull { root ->
            val file = rootDoors[root.name]
            when {
                file == null -> "${root.name} (جذر بلا باب مُعلَن)"
                !File(sourceRoot, file).isFile -> "${root.name} → $file (الملف غير موجود)"
                !read(file).contains("MaxDestination.${root.name}") -> "${root.name} → $file (لا يذكره)"
                else -> null
            }
        }

        assertEquals(
            "جذور مرئية بلا مقعد في الشريط وبلا باب مُعلَن:\n" + broken.joinToString("\n"),
            emptyList<String>(),
            broken,
        )
    }

    @Test
    fun `the declared root doors are all still roots`() {
        val roots = destinations()
            .filter { it.parent == null && !it.hidden && !it.seated }
            .map { it.name }
            .toSet()
        val stale = rootDoors.keys - roots

        assertTrue(
            "أبواب مُعلَنة لجذور لم تعد جذورًا (أو لم تعد) — الخريطة تَكذِب: $stale",
            stale.isEmpty(),
        )
    }

    /**
     * **المُوجِّد ليس بابًا — لكنه آخر شبكة أمان، فيُحرَس.**
     *
     * الباب الحقيقي هو صفّ في شاشة؛ والمُوجِّد بحثٌ يجد الوجهة بكتابتها. ومنعُ الاعتماد عليه كبابٍ
     * هو ما فعله الحارس أعلاه. لكنه إن فقد وجهةً، صار المستخدم يكتب اسمها فلا يجدها — فتُقاس تغطيته
     * كاملةً على كل وجهة مرئية، بلا استثناء.
     */
    @Test
    fun `the finder reaches every visible destination`() {
        val visible = destinations().filter { !it.hidden }.map { it.name }.toSet()
        val finder = read("ui/mainscreens/ScreenFinderModel.kt")
        val missing = visible.filter { !finder.contains("MaxDestination.$it") }.sorted()

        assertEquals(
            "وجهات مرئية لا يجدها المُوجِّد بكتابتها:\n" + missing.joinToString("\n"),
            emptyList<String>(),
            missing,
        )
    }
}
