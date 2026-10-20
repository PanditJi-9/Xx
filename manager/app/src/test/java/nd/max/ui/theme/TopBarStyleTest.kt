/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.theme

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.lang.reflect.Proxy

/**
 * عقد **أنماط الشريط العلوي** (§٦-أ): «أنشئ مجموعة من أنماط الشريط العلوي، بحيث يختلف كل نمط في
 * بنية التصميم نفسها، وليس اللون فقط… كل نمط يجب أن يمتلك قواعد واضحة للمسافات والطباعة
 * والأيقونات والعناوين والإجراءات والحركة، وألّا يكون مجرّد نسخة أخرى من النمط نفسه».
 *
 * والقياس هنا على ثلاثة مستويات، وكلٌّ يقيس ما يستطيعه:
 *
 * 1. **القراءة والافتراضيّ** (JVM حقيقي): [ThemeStudio.topBarStyle] بمخزن تفضيلات مصنوع
 *    بـ`Proxy` — الغياب والمفتاح المجهول والقيمة غير النصّيّة تعود كلها [TopBarStyle.CLASSIC]،
 *    أي شريط التطبيق القائم؛ فلا يتغيّر شكل شاشةٍ على صاحبها.
 * 2. **الفروق البنيوية**: قواعد النمط بياناتٌ على التعداد ([TopBarStyle.heroTitle] ·
 *    `compactBar` · `ownsDescription` · `showsGlyph`)، ويُقاس أن لا نمطين يتقاسمان القواعد
 *    الأربع نفسها — وهو نصّ البند («ألّا يكون مجرّد نسخة أخرى»).
 * 3. **الحياكة**: نقطة رسم واحدة ([MaxStyledTopBar]) بأربعة فروع، وبابٌ عامّ يقرأ النمط
 *    ويفوّض ([MaxManagerSubScreenTopBar]) فتكتسبه الشاشات ذات الهياكل الخاصّة، والهياكل الثلاثة
 *    تمرّ بالنقطة نفسها، والاستوديو يكتب المفتاح، والمعاينة ترسم النمط.
 *
 * **والحدّ المُعلَن:** «هل يبدو النمط جيّدًا فعلًا؟ وهل يصمد مع RTL والشاشات الصغيرة والفتحة
 * العلويّة والقارئ الشاشيّ؟» — UAT على جهاز، لا يُدّعى هنا. والمقيس في هذا الملف: القواعد
 * والربط؛ لا الذوق ولا البكسل ولا سلوك اللمس.
 */
class TopBarStyleTest {

    // ── ① القراءة والافتراضيّ ────────────────────────────────────────────────

    @Test
    fun `absent or unknown key reads classic, and every style key round-trips`() {
        assertEquals(
            "لا مفتاح ⇒ الشريط القائم (لا يتغيّر شكلُ من لم يطلب)",
            TopBarStyle.CLASSIC,
            ThemeStudio.topBarStyle(prefsOf(emptyMap())),
        )
        assertEquals(
            "ومفتاحٌ لا يُعرف (نسخة أقدم أو ملفّ عُدّل يدويًّا) يعود بالقائم لا يُسقط الشاشة",
            TopBarStyle.CLASSIC,
            ThemeStudio.topBarStyle(prefsOf(mapOf(ThemeKeys.TOP_BAR to "NONSENSE"))),
        )
        for (style in TopBarStyle.entries) {
            assertEquals(
                "النمط يُخزَّن بمفتاحه ويُقرأ به: ${style.key}",
                style,
                ThemeStudio.topBarStyle(prefsOf(mapOf(ThemeKeys.TOP_BAR to style.key))),
            )
        }
        assertEquals(
            "ونصّ المفتاح واحد لا نسختان",
            "theme_top_bar",
            ThemeKeys.TOP_BAR,
        )
    }

    @Test
    fun `a value of another type never crashes the read`() {
        // مخزن أقدم أو ملفّ عُدّل يدويًّا: المفتاح موجود بقيمة غير نصّيّة. القراءة لا تُسقط
        // شيئًا، وتعود بالشريط القائم.
        assertEquals(
            TopBarStyle.CLASSIC,
            ThemeStudio.topBarStyle(prefsOf(mapOf(ThemeKeys.TOP_BAR to 42))),
        )
    }

    // ── ② الفروق البنيوية: لا نمط نسخةٌ من آخر ──────────────────────────────

    @Test
    fun `no two styles share the same structural rules`() {
        val shapes = TopBarStyle.entries.map {
            listOf(it.heroTitle, it.compactBar, it.ownsDescription, it.showsGlyph)
        }
        assertEquals(
            "أربعة أنماط بقواعدَ مختلفة — لا نمط نسخةٌ من آخر",
            TopBarStyle.entries.size,
            shapes.toSet().size,
        )
        assertEquals(
            "عنوان كبير واحد فقط (سينمائيّ)",
            setOf(TopBarStyle.CINEMATIC),
            TopBarStyle.entries.filter { it.heroTitle }.toSet(),
        )
        assertEquals(
            "الوصف داخل الشريط في النمطين اللذين يملكانه فقط",
            setOf(TopBarStyle.CINEMATIC, TopBarStyle.TECHNICAL),
            TopBarStyle.entries.filter { it.ownsDescription }.toSet(),
        )
        assertEquals(
            "رمز المجال يُرسم في الكلاسيكيّ والسينمائيّ وحدهما",
            setOf(TopBarStyle.CLASSIC, TopBarStyle.CINEMATIC),
            TopBarStyle.entries.filter { it.showsGlyph }.toSet(),
        )
        assertEquals(
            "المصغّر هو الوحيد بشريط الصفّ الواحد",
            setOf(TopBarStyle.MINIMAL),
            TopBarStyle.entries.filter { it.compactBar }.toSet(),
        )
        assertFalse(
            "والمصغّر لا يخفي معلومة ثمّ يُعلن أنه لا يخفيها: لا رمز مجال ولا وصف في شريطه",
            TopBarStyle.MINIMAL.showsGlyph || TopBarStyle.MINIMAL.ownsDescription,
        )
    }

    // ── ③ الحياكة: نقطة رسم واحدة، وبابٌ عامّ، وثلاثة هياكل ──────────────────

    @Test
    fun `every style has its own branch behind one bar entry point`() {
        val chrome = sourceFile("ui/component/ScreenChrome.kt").readText()
        assertTrue(
            "نقطة الرسم الواحدة موجودة",
            chrome.contains("fun MaxStyledTopBar("),
        )
        assertTrue(
            "والباب العامّ يقرأ النمط من مخزن الإعدادات نفسه ويفوّض — فلا شاشة تنفرد بشريطها",
            chrome.contains("fun MaxManagerSubScreenTopBar(") &&
                chrome.contains("style = rememberTopBarStyle(),") &&
                chrome.contains("getSharedPreferences(\"settings\""),
        )
        assertTrue(
            "والتنفيذ الكلاسيكيّ صار خاصًّا خلف الباب",
            chrome.contains("private fun ClassicMediumTopBar("),
        )
        for (style in TopBarStyle.entries) {
            assertTrue(
                "ولكل نمط فرعه المرسوم لا فرعٌ ساقط: ${style.name}",
                chrome.contains("TopBarStyle.${style.name} ->"),
            )
        }
        assertFalse(
            "ولا فرع كلاسيكيّ يعود إلى الباب العامّ فيدور نداءٌ لا ينتهي",
            chrome.contains("TopBarStyle.CLASSIC -> MaxManagerSubScreenTopBar("),
        )
        assertFalse(
            "ولا فرع 'else' يبتلع نمطًا لم يُرسم (التعداد مُستنفَد)",
            chrome.contains("else ->"),
        )
    }

    @Test
    fun `the three shells pass through the styled bar and never duplicate the description`() {
        val shell = sourceFile("ui/design/MaxScreenScaffold.kt").readText()
        assertEquals(
            "ثلاثة هياكل (صفحة · مفصّلة · قائمة) تمرّ بنقطة الرسم الواحدة",
            3,
            Regex("MaxStyledTopBar\\(").findAll(shell).count(),
        )
        assertEquals(
            "وكلّها تقرأ النمط مرّة واحدة",
            3,
            Regex("rememberTopBarStyle\\(\\)").findAll(shell).count(),
        )
        assertEquals(
            "وقرار «الشريط يملك الوصف»: إعلانٌ وشرطٌ في كل هيكل (٣ + ٣)",
            6,
            Regex("barOwnsDescription").findAll(shell).count(),
        )
        assertTrue(
            "والشرط لا يُخفي وصفًا حين لا وصف: النمط يملكه والوصف موجود",
            shell.contains("topBarStyle.ownsDescription && !subtitle.isNullOrBlank()"),
        )
    }

    @Test
    fun `the customization screen writes the key and the preview draws the style`() {
        val sections = sourceFile("ui/subscreens/ThemeStudioSections.kt").readText()
        assertTrue(
            "الصفّ يعرض عنوانه من الموارد لا نصًّا محفورًا",
            sections.contains("stringResource(R.string.theme_topbar_title)"),
        )
        assertTrue(
            "والكتابة تمرّ بنقطة الكتابة الواحدة وبالنمط المختار",
            sections.contains("persistChoice(prefs, ThemeKeys.TOP_BAR, currentTopBar.key, next.key)"),
        )
        val screen = sourceFile("ui/subscreens/CustomThemeScreen.kt").readText()
        assertTrue(
            "والشاشة تقرأ النمط المحفوظ عند فتحها",
            screen.contains("ThemeStudio.topBarStyle(prefs)"),
        )
        assertTrue(
            "وإعادة الضبط تعيد الشريط القائم صراحةً لا بالحذف وحده",
            screen.contains("putString(ThemeKeys.TOP_BAR, TopBarStyle.CLASSIC.key)"),
        )
        assertTrue(
            "والمعاينة ترسم النمط بالدالّة النقيّة",
            screen.contains("previewHeroHeight(topBarStyle)"),
        )
        assertEquals(
            "ولكل نمط ارتفاعه في المعاينة — فلا نمط يُعرض بارتفاع غيره",
            TopBarStyle.entries.size,
            Regex("TopBarStyle\\.[A-Z]+ ->").findAll(screen).count(),
        )
    }

    @Test
    fun `the five strings exist in English and Arabic`() {
        val en = resFile("values/strings.xml").readText()
        val ar = resFile("values-ar/strings.xml").readText()
        for (key in listOf(
            "theme_topbar_title",
            "theme_topbar_classic",
            "theme_topbar_minimal",
            "theme_topbar_cinematic",
            "theme_topbar_technical",
        )) {
            assertTrue("ناقص في الإنجليزية: $key", en.contains("name=\"$key\""))
            assertTrue("ناقص في العربية: $key", ar.contains("name=\"$key\""))
        }
    }

    // ── أدوات القياس ─────────────────────────────────────────────────────────

    /** مخزن تفضيلات في الذاكرة — بلا أندرويد وبلا ملفّ؛ يكفي لقراءة واحدة. */
    private fun prefsOf(values: Map<String, Any?>): SharedPreferences =
        Proxy.newProxyInstance(
            SharedPreferences::class.java.classLoader,
            arrayOf(SharedPreferences::class.java),
        ) { _, method, args ->
            val key = (args?.getOrNull(0) as? String) ?: ""
            when (method.name) {
                "getString" -> {
                    val stored = values[key]
                    if (stored is String) stored else args?.getOrNull(1)
                }
                "getBoolean" -> {
                    val stored = values[key]
                    if (stored is Boolean) stored else args?.getOrNull(1)
                }
                "contains" -> values.containsKey(key)
                "toString" -> "InMemorySharedPreferences($values)"
                "hashCode" -> values.hashCode()
                "equals" -> false
                else -> throw UnsupportedOperationException("نداء غير متوقَّع: ${method.name}")
            }
        } as SharedPreferences

    /** مسار ملفّ مصدر من جذر يعمل في الموضعين: CI من `manager/`، ومحلّيًّا من الجذر. */
    private fun sourceFile(relative: String): File {
        val candidates = listOf(
            File("src/main/java/nd/max/$relative"),
            File("app/src/main/java/nd/max/$relative"),
            File("../app/src/main/java/nd/max/$relative"),
            File("manager/app/src/main/java/nd/max/$relative"),
        )
        return candidates.firstOrNull { it.isFile } ?: error("لم يُوجد المصدر: $relative")
    }

    /** مسار ملفّ موارد — نفس المنطق على مجلّد `res`. */
    private fun resFile(relative: String): File {
        val candidates = listOf(
            File("src/main/res/$relative"),
            File("app/src/main/res/$relative"),
            File("../app/src/main/res/$relative"),
            File("manager/app/src/main/res/$relative"),
        )
        return candidates.firstOrNull { it.isFile } ?: error("لم يُوجد المورد: $relative")
    }
}
