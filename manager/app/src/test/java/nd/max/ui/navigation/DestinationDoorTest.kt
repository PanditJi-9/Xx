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
                !read(file).contains("MaxDestination.${child.name}") ->
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
}
