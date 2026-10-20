/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.design

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * عقد **الهوية البصرية** (§٩ من خطة المستوى: «تطوير التصميم العام وإزالة الطابع الاصطناعي»)،
 * ومعيارُ قبوله: «تُعالج ملاحظات الهوية البصرية والطابع الاصطناعي في مختلف أجزاء التطبيق».
 *
 * وملاحظة التقييم الأصلية ثلاثة أسطر: «AI seems to be overused · unique, personal touch for the
 * UI/UX · your own personal refinements». والمقيس هنا هو ما يمكن قياسه في الكود — **الرموز
 * والعلامات** — لا الذوق:
 *
 * 1. **رموزُ «الذكاء» لا تخرج عن موضعها:** `AutoAwesome` (النجمة) **علامة أسطح الدخول**
 *    (زرّ الدخول · الاختصار · وجهة الملاحة)، و`Psychology` (العقل) **علامة شاشتَي الذكاء
 *    والحيّ** — رُوجع الاثنان فلم يجدا في سطحٍ لا يخصّ Max AI. وكانت النجمة قبل §٩ تُرسم أمام
 *    وصف **كل** شاشة تقريبًا، فتبدو الواجهة موسومة بالذكاء حيث لا ذكاء — وهو نصّ الملاحظة.
 * 2. **العلامة موتيف المشروع لا أيقونة:** وصف الشاشة (`MaxScreenDescription`) وملاحظتها
 *    (`MaxManagerInsight`) ورأس القسم (`StudioSectionHeader`) صارت تُعلَّم بـ**الشرطة الرأسية**
 *    بأرقام `MaxSectionSpec.accent*` — هي نفسها في `NeuralSectionHeader` — **وهي مربوطة بعدم
 *    وجود أي `Icons.` في أجسادها**: العلامة معلومة (هذا قسم، وهذا لونه) لا زخرفة.
 * 3. **الرمز يدلّ على الوظيفة:** كبسولة الجزيرة الحيّة `PictureInPictureAlt` (نافذة عائمة فوق
 *    المحتوى — هي ما تفعله)، و«تلقائي» في منتقي التطبيقات `Autorenew` (دوران التجديد) لا نجمة.
 * 4. **ولا رموز ذكاء عامة أخرى:** `SmartToy` · `Psychology` · `AutoFix*` · `AutoMode` ممنوعة
 *    في `ui/` كلها — الرمز في هذا التطبيق يقول **ماذا يفعل الزرّ**، لا «كم هو ذكي».
 * 5. **الصاروخ ليس فعلًا:** `RocketLaunch` ممنوع في `ui/` كلها — كان يقف فوق «تحرير الذاكرة»
 *    وفوق «التحميل المسبق»، وكلاهما لا يُطلق شيئًا.
 * 6. **الاسم يُكتب ولا يُصرَخ:** لا `MAX AI` ولا `ماكس AI` في `values/` و`values-ar/` — اسم
 *    واحد `Max AI` في اللغتين (وكانت العربية تُبدّل بين الشكلين).
 * 7. **الشاشات الأساسية لا تُكرّر حركة بلا نهاية، وكل انتقال فيها ≤ 360ms** (سقف `DESIGN.md`).
 *    وكان أول انتقال بعد الإعداد 700ms، ولمعان معاينة السمة يدور كل 3.2 ثانية.
 * 8. **سطح الدخول مسطّح:** كبسولة `MaxAiEntryButton` لا تُرسم بتدرّج — التدرّج المتكرّر هو
 *    «البطاقة اللامعة» التي رفضها §٩.
 * 9. **ونصوص الذكاء كلها حيّة:** كل مفتاح نصّ في `values/` يخصّ Max AI له نداء في المصدر —
 *    كان في الشجرة **٥٧ مفتاحًا ميتًا** (نسخة شاشة قديمة) تتكرّر في ٨٤ لغة.
 *
 * **والحدّ المُعلَن:** هذا قياسٌ على المصدر (وجود الرمز وموضعه)، ولا يقيس **كيف يبدو** شيء على
 * الشاشة: تناسق العلامة الجديدة، ووزن الشرطة أمام النصّ في RTL، وتباينها في الوضع الفاتح —
 * UAT على جهاز. ولا يُدَّعى أنه قيس هنا.
 */
class VisualIdentityTest {

    // ── ① رموز الذكاء = أسطح Max AI وحدها ───────────────────────────────────

    @Test
    fun `the AI glyphs mark Max AI surfaces only`() {
        val glyphs = listOf("AutoAwesome", "Psychology")
        val filesWithGlyphs = uiFiles()
            .filter { file -> codeLines(file).any { line -> glyphs.any { line.contains(it) } } }
            .map { it.name }
            .toSortedSet()

        assertEquals(
            "«النجمة/العقل» في سطحٍ غير Max AI = «ذكاء مفرط» في كل شاشة (ملاحظة §٩ الأصلية)",
            emptyList<String>(),
            filesWithGlyphs.filterNot(::isMaxAiIdentitySurface),
        )
        assertTrue(
            "وتبقى على أسطح الدخول الثلاثة — لا يُفرَغ الرمز من معناه بإزالته من كل موضع",
            filesWithGlyphs.containsAll(
                setOf("MaxAiEntryButton.kt", "MaxAiShortcut.kt", "MaxDestinations.kt"),
            ),
        )
    }

    // ── ② العلامة شرطةٌ من الموتيف، لا أيقونة ────────────────────────────────

    @Test
    fun `descriptions, notes and section headers draw the app rule, not a glyph`() {
        val chrome = sourceFile("component/ScreenChrome.kt").readText()
        val description = chrome.substringAfter("fun MaxScreenDescription(").substringBefore("fun MaxManagerInsight(")
        val insight = chrome.substringAfter("fun MaxManagerInsight(").substringBefore("fun MaxManagerSectionTitle(")

        listOf("وصف الشاشة" to description, "ملاحظة الشاشة" to insight).forEach { (label, body) ->
            assertTrue(
                "$label: العلامة من موتيف المشروع (MaxSectionSpec.accentWidth)",
                body.contains("MaxSectionSpec.accentWidth"),
            )
            assertFalse(
                "$label: ولا أيقونة في العلامة — الشرطة معلومة، والرمز المكرَّر زخرفة",
                body.contains("Icons."),
            )
        }

        val studio = sourceFile("component/StudioSectionHeader.kt").readText()
        assertTrue(
            "رأس الأقسام في شاشات الاستوديو: شرطة الموتيف نفسها",
            studio.contains("MaxSectionSpec.accentWidth"),
        )
        assertFalse(
            "رأس الأقسام: بلا أيقونة — كانت كلها نجمةً واحدة قبل §٩",
            studio.contains("Icons."),
        )
    }

    // ── ③ والرمز الوظيفي يدلّ على وظيفته ────────────────────────────────────

    @Test
    fun `functional icons stand for what the surface does`() {
        val tabs = sourceFile("subscreens/IslandTabBar.kt").readText()
        val island = sourceFile("subscreens/DynamicIslandScreen.kt").readText()
        assertTrue(
            "تبويب «Core» في الجزيرة: كبسولة عائمة (PictureInPictureAlt) لا نجمة",
            tabs.contains("Icons.Rounded.PictureInPictureAlt"),
        )
        assertTrue(
            "وزاوية الشاشة بالرمز نفسه — لا رمزان لميزة واحدة",
            island.contains("Icons.Rounded.PictureInPictureAlt"),
        )

        val shelf = sourceFile("component/AppShelfPicker.kt").readText()
        assertTrue(
            "«تلقائي» في منتقي التطبيقات: حلقة التجديد (Autorenew)، والمعنى يحمله نصّه أيضًا",
            shelf.contains("Icons.Rounded.Autorenew"),
        )
    }

    // ── ④ ولا رموز «ذكاء» عامة أخرى تدخل ───────────────────────────────────

    @Test
    fun `no generic AI glyphs beyond the identity`() {
        val banned = listOf("SmartToy", "AutoFixHigh", "AutoFixNormal", "AutoMode", "EmojiObjects")
        val offenders = uiFiles().flatMap { file ->
            codeLines(file)
                .filter { line -> banned.any { line.contains(it) } }
                .map { "${file.name}: ${it.trim()}" }
        }
        assertEquals(
            "رموز «الروبوت · العقل · العصا السحرية» ممنوعة: الرمز يقول ماذا يفعل الزرّ، لا «كم هو ذكي»",
            emptyList<String>(),
            offenders,
        )
    }

    // ── ⑤ الصاروخ ليس فعلًا ──

    @Test
    fun `no rocket stands in for an operation`() {
        val offenders = uiFiles().flatMap { file ->
            codeLines(file)
                .filter { it.contains("RocketLaunch") }
                .map { "${file.name}: ${it.trim()}" }
        }
        assertEquals(
            "الصاروخ يرسم «إطلاقًا»؛ وما في هذه الشاشات تحريرُ ذاكرةٍ وتحميلٌ مسبق",
            emptyList<String>(),
            offenders,
        )
    }

    // ── ⑥ الاسم يُكتب ولا يُصرَخ ──

    @Test
    fun `the product name is never shouted or transliterated`() {
        val offenders = resourceDirs().flatMap { dir ->
            dir.walkTopDown()
                .filter { it.isFile && it.extension == "xml" }
                .flatMap { file ->
                    file.readLines()
                        .filter { it.contains("<string") && (it.contains("MAX AI") || it.contains("ماكس AI")) }
                        .map { "${file.name}: ${it.trim()}" }
                }
                .toList()
        }
        assertEquals(
            "اسم واحد `Max AI` في الإنجليزية والعربية — لا صياح ولا نقل حرفي",
            emptyList<String>(),
            offenders,
        )
    }

    // ── ⑦ الحركة: لا حلقة في الشاشات الأساسية، ولا انتقال فوق السقف ──

    @Test
    fun `primary screens never loop an animation`() {
        val offenders = (mainScreenFiles() + listOf(rootSource("MainActivity.kt"))).flatMap { file ->
            codeLines(file)
                .filter { it.contains("rememberInfiniteTransition") || it.contains("infiniteRepeatable") }
                .map { "${file.name}: ${it.trim()}" }
        }
        assertEquals(
            "لا حركة دائمة في مسارات التنقّل الأساسية (DESIGN.md: الحركة تفسّر تغيّرًا ولا تسكن)",
            emptyList<String>(),
            offenders,
        )
    }

    @Test
    fun `primary screens keep every tween inside the motion ceiling`() {
        val tween = Regex("""tween\(\s*(\d{3,})""")
        val offenders = (mainScreenFiles() + listOf(rootSource("MainActivity.kt"))).flatMap { file ->
            codeLines(file).mapNotNull { line ->
                val millis = tween.find(line)?.groupValues?.get(1)?.toIntOrNull() ?: return@mapNotNull null
                if (millis <= MaxDuration.deliberate) null else "${file.name}: $millis ms — ${line.trim()}"
            }
        }
        assertEquals(
            "سقف الحركة ${MaxDuration.deliberate}ms؛ وما فوقه زينة (وكان الانتقال الأول 700ms)",
            emptyList<String>(),
            offenders,
        )
    }

    // ── ⑧ سطح الدخول مسطّح لا لامع ──

    @Test
    fun `the AI entry is a surface not a glossy capsule`() {
        val body = codeLines(sourceFile("component/MaxAiEntryButton.kt")).joinToString("\n")
        assertFalse(
            "لا تدرّج في كبسولة الدخول — التدرّج المتكرّر هو «البطاقة اللامعة»",
            body.contains("Brush."),
        )
        assertTrue(
            "والحالة تُحمل بطبقة اللون من الرموز (`MaxAlpha.toneContainer`) لا بوهج",
            body.contains("MaxAlpha.toneContainer"),
        )
    }

    // ── ⑨ كل نصّ يخصّ Max AI له نداء ──

    @Test
    fun `every AI-labelled string is called by the UI`() {
        val aiKey = Regex("""(^|_)ai(_|$)|maxai|max_ai""", RegexOption.IGNORE_CASE)
        val aiValue = Regex("""\bAI\b|Max AI|ذكاء""")
        val declared = resourceDirs().first().walkTopDown()
            .filter { it.isFile && it.extension == "xml" }
            .flatMap { file ->
                Regex(
                    """<string name="([^"]+)"[^>]*>(.*?)</string>""",
                    setOf(RegexOption.DOT_MATCHES_ALL),
                )
                    .findAll(file.readText())
                    .mapNotNull { match ->
                        val key = match.groupValues.getOrNull(1) ?: return@mapNotNull null
                        val value = match.groupValues.getOrNull(2).orEmpty()
                        key.takeIf { aiKey.containsMatchIn(it) || aiValue.containsMatchIn(value) }
                    }
                    .toList()
            }
            .toList()

        val unused = declared.filter { key ->
            !mainSourceBlob.contains("R.string.$key") && !mainSourceBlob.contains("@string/$key")
        }
        assertEquals(
            "مفتاح ذكاء بلا نداء = نسخة شاشة قديمة تُترجم في ٨٤ لغة ولا تُقرأ",
            emptyList<String>(),
            unused,
        )
    }

    // ── ⑩ ولا مسار نواة في وجه المستخدم ─────────────────────────────────

    @Test
    fun `user facing CPU copy names no kernel internals`() {
        val visibleKeys = listOf(
            "cpu_core_probe_detail",
            "net_sched_probe_detail",
            "home_waiting_core_data",
        )
        val banned = listOf("/sys/", "/proc/", "cpufreq", "topology", "this kernel", "kernel exposes")
        resourceDirs().forEach { dir ->
            val strings = File(dir, "strings.xml").readText()
            visibleKeys.forEach { key ->
                val value = Regex(
                    """<string name="$key"[^>]*>(.*?)</string>""",
                    setOf(RegexOption.DOT_MATCHES_ALL),
                ).find(strings)?.groupValues?.get(1)
                    ?: error("المفتاح غائب: ${dir.name}/$key")
                banned.forEach { term ->
                    assertFalse(
                        "${dir.name}/$key يعرض تفصيل تنفيذ ('$term') — المالك: «فقط AI يعرض sys/devices»",
                        value.contains(term, ignoreCase = true),
                    )
                }
            }
        }

        val screen = sourceFile("subscreens/CpuCoreControlScreen.kt").readText()
        val condition = screen.substringAfter("val condition = when").substringBefore("// **نتيجة الإجراء")
        assertFalse(
            "بطاقة حالة CPU لا تعرض مسار sysfs — بقي في المساعدة وحدها",
            condition.contains("technicalDetail"),
        )
        assertTrue(
            "المسار لم يُفقد: متاح في المساعدة لمن يريده",
            File(resRoot(), "values/strings.xml").readText()
                .contains("name=\"cpu_core_technical_source\">Technical detail: /sys/devices/system/cpu<"),
        )
    }

    // ── أدوات القياس ───────────────────────────────────────────────────────

    /** سطحُ Max AI المشروع: ملفّه باسم `MaxAi…` أو `MaxLive…` (شاشة الحيّ)، أو وجهة الملاحة. */
    private fun isMaxAiIdentitySurface(fileName: String): Boolean =
        fileName.startsWith("MaxAi") || fileName.startsWith("MaxLive") || fileName == "MaxDestinations.kt"

    /** أسطر الكود وحدها — وذكر الرمز في تعليق (وهو كثير في شرح هذا التغيير) ليس استعمالًا. */
    private fun codeLines(file: File): List<String> = file.readLines().filterNot { line ->
        val t = line.trim()
        t.startsWith("//") || t.startsWith("*") || t.startsWith("/*")
    }

    /** كل ملفات واجهة المستخدم — المسح على الشجرة لا على قائمةٍ مكتوبة بيد. */
    private fun uiFiles(): List<File> =
        uiRoot().walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    /** شاشات المسارات الأساسية وحدها (الرئيسية · التطبيقات · التحكّم · الإعدادات وشِقّاتها). */
    private fun mainScreenFiles(): List<File> =
        File(uiRoot(), "mainscreens").walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .toList()

    /** ملفّ مصدر بجوار `ui/` مباشرةً (`MainActivity.kt`)، لا داخل حزمة `ui`. */
    private fun rootSource(relative: String): File {
        val file = File(uiRoot().parentFile, relative)
        check(file.isFile) { "لم يُوجد المصدر: $relative" }
        return file
    }

    /** مجلّدا اللغتين المالكين (`values/` و`values-ar/`) — لا تُمسّ بقية اللغات (§0.2). */
    private fun resourceDirs(): List<File> {
        val res = resRoot()
        return listOf(File(res, "values"), File(res, "values-ar")).filter { it.isDirectory }
    }

    private fun resRoot(): File {
        val candidates = listOf(
            File("src/main/res"),
            File("app/src/main/res"),
            File("manager/app/src/main/res"),
            File("../app/src/main/res"),
        )
        return candidates.firstOrNull { it.isDirectory }
            ?: error("لم يُوجد جذر res/ — جُرِّبت ${candidates.size} مسارات")
    }

    /**
     * نصّ كل مصادر التطبيق مرّة واحدة — للمقارنة مع مفاتيح الموارد.
     *
     * ومجلّدات `values*` مستثناة: المفتاح يُعدّ حيًّا حين **يناديه** المصدر، لا حين يُعرَّف.
     */
    private val mainSourceBlob: String by lazy {
        val roots = listOf(
            File("src/main"),
            File("app/src/main"),
            File("manager/app/src/main"),
            File("../app/src/main"),
        )
        val root = roots.firstOrNull { it.isDirectory }
            ?: error("لم يُوجد src/main — جُرِّبت ${roots.size} مسارات")
        val builder = StringBuilder()
        root.walkTopDown()
            .filter { it.isFile && it.extension in setOf("kt", "java", "xml") }
            .filterNot { it.invariantSeparatorsPath.contains("/values") }
            .forEach { builder.append(it.readText()).append('\n') }
        builder.toString()
    }

    /** جذر `ui/` — يعمل من `manager/` في CI ومن جذر المستودع محلّيًّا. */
    private fun uiRoot(): File {
        val candidates = listOf(
            File("src/main/java/nd/max/ui"),
            File("app/src/main/java/nd/max/ui"),
            File("manager/app/src/main/java/nd/max/ui"),
            File("../app/src/main/java/nd/max/ui"),
        )
        return candidates.firstOrNull { it.isDirectory }
            ?: error("لم يُوجد جذر ui/ — جُرِّبت ${candidates.size} مسارات")
    }

    /** ملفّ مصدر واحد بالمسار النسبيّ داخل `nd/max/ui`. */
    private fun sourceFile(relative: String): File {
        val file = File(uiRoot(), relative)
        check(file.isFile) { "لم يُوجد المصدر: $relative" }
        return file
    }
}
