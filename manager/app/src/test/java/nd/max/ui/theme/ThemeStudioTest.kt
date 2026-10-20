/*
 * Copyright (C) 2026-2027 Zexshia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nd.max.ui.theme

import com.materialkolor.Contrast
import nd.max.ui.design.MaxCardStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * عقد «استوديو السمة» — يقيس هنا ما يستحيل قياسه على جهاز في كل مراجعة:
 * القراءة المتسامحة (قيمة غائبة أو مجهولة لا تُسقط شيئًا)، وأن الافتراضيّات **هي سلوك
 * اليوم** (من لم يفتح الخيار لا يتغيّر عنده شيء)، وأن الأنماط الستّة تغطّي التوزيع
 * الموثّق، وأن بذورها من شرائح الشاشة نفسها.
 *
 * **والحدّ المُعلَن:** هذا قياس قيم لا قياس مشهد — هل تبدو «الاستدارة الحادّة» جيدة على
 * الزجاج، وهل «الملتصق» يريح الإبهام؟ هذا يبقى يحتاج جهازًا (UAT)، ولا يُدَّعى هنا.
 */
class ThemeStudioTest {

    // ── القراءة المتسامحة: الافتراضيّ عند الغياب أو الجهالة ─────────────────

    @Test
    fun `every studio enum falls back to its default for missing and unknown keys`() {
        assertEquals(ThemeContrast.DEFAULT, ThemeContrast.fromKey(null))
        assertEquals(ThemeContrast.DEFAULT, ThemeContrast.fromKey("no_such_key"))
        assertEquals(ThemeRoundness.STANDARD, ThemeRoundness.fromKey(null))
        assertEquals(ThemeRoundness.STANDARD, ThemeRoundness.fromKey("no_such_key"))
        assertEquals(ThemeFontScale.STANDARD, ThemeFontScale.fromKey(null))
        assertEquals(ThemeFontScale.STANDARD, ThemeFontScale.fromKey("no_such_key"))
        assertEquals(NavBarStyle.FLOATING, NavBarStyle.fromKey(null))
        assertEquals(NavBarStyle.FLOATING, NavBarStyle.fromKey("no_such_key"))
        assertEquals(ThemeMotion.FULL, ThemeMotion.fromKey(null))
        assertEquals(ThemeMotion.FULL, ThemeMotion.fromKey("no_such_key"))
        assertEquals(ThemeDensity.STANDARD, ThemeDensity.fromKey(null))
        assertEquals(ThemeDensity.STANDARD, ThemeDensity.fromKey("no_such_key"))
    }

    @Test
    fun `every entry round-trips through its own stored key`() {
        ThemeContrast.entries.forEach { assertEquals(it, ThemeContrast.fromKey(it.key)) }
        ThemeRoundness.entries.forEach { assertEquals(it, ThemeRoundness.fromKey(it.key)) }
        ThemeFontScale.entries.forEach { assertEquals(it, ThemeFontScale.fromKey(it.key)) }
        NavBarStyle.entries.forEach { assertEquals(it, NavBarStyle.fromKey(it.key)) }
        ThemeMotion.entries.forEach { assertEquals(it, ThemeMotion.fromKey(it.key)) }
        ThemeDensity.entries.forEach { assertEquals(it, ThemeDensity.fromKey(it.key)) }
    }

    @Test
    fun `every card style key is stable and unique`() {
        val keys = MaxCardStyle.entries.map { it.key }
        assertTrue("مفتاح فارغ في $keys", keys.none { it.isBlank() })
        assertEquals("مفتاح مكرّر في $keys", keys.size, keys.toSet().size)
    }

    @Test
    fun `stored keys are non-blank and unique inside every enum`() {
        val keySets = listOf(
            ThemeContrast.entries.map { it.key },
            ThemeRoundness.entries.map { it.key },
            ThemeFontScale.entries.map { it.key },
            NavBarStyle.entries.map { it.key },
            ThemeMotion.entries.map { it.key },
            ThemeDensity.entries.map { it.key },
            MaxCardStyle.entries.map { it.key },
        )
        keySets.forEach { keys ->
            assertTrue("مفتاح فارغ في ${keys}", keys.none { it.isBlank() })
            assertEquals("مفتاح مكرّر في $keys", keys.size, keys.toSet().size)
        }
    }

    // ── الافتراضيّات = سلوك اليوم حرفيًّا ────────────────────────────────────

    @Test
    fun `the defaults reproduce today's behaviour`() {
        // من لم يفتح الخيارات الجديدة: تباين المولّد الافتراضيّ، استدارة 1.0، حجم نص 1.0،
        // الشريط العائم القائم، الحركة الكاملة القائمة — أي صفر تغيير بصريّ عند الترقية.
        assertEquals(Contrast.Default.value, ThemeContrast.DEFAULT.level, 0.0)
        assertEquals(1.0f, ThemeRoundness.STANDARD.scale, 0f)
        assertEquals(1.0f, ThemeFontScale.STANDARD.scale, 0f)
        assertEquals(NavBarStyle.FLOATING, NavBarStyle.fromKey(null))
        assertEquals(ThemeMotion.FULL, ThemeMotion.fromKey(null))
        // كثافة 1.0 (حشو اليوم) و`OUTLINED` (قشرة اليوم) — أي صفر تغيير بصريّ عند الترقية.
        assertEquals(1.0f, ThemeDensity.STANDARD.scale, 0f)
        assertEquals(MaxCardStyle.OUTLINED, MaxCardStyle.entries.first())
    }

    @Test
    fun `scales are strictly ordered and stay inside the declared bounds`() {
        assertTrue(ThemeRoundness.SHARP.scale < ThemeRoundness.STANDARD.scale)
        assertTrue(ThemeRoundness.STANDARD.scale < ThemeRoundness.ROUND.scale)
        ThemeRoundness.entries.forEach { assertTrue(it.scale > 0f && it.scale < 2f) }

        assertTrue(ThemeFontScale.SMALL.scale < ThemeFontScale.STANDARD.scale)
        assertTrue(ThemeFontScale.STANDARD.scale < ThemeFontScale.LARGE.scale)
        // المجال أضيق عمدًا (0.90–1.12): نسبة أوسع تكسر شبكات الشاشات الضيّقة.
        ThemeFontScale.entries.forEach { assertTrue(it.scale in 0.85f..1.15f) }

        assertTrue(ThemeContrast.DEFAULT.level < ThemeContrast.MEDIUM.level)
        assertTrue(ThemeContrast.MEDIUM.level < ThemeContrast.HIGH.level)

        assertTrue(ThemeDensity.COMPACT.scale < ThemeDensity.STANDARD.scale)
        assertTrue(ThemeDensity.STANDARD.scale < ThemeDensity.SPACIOUS.scale)
        // المجال محسوب: الأصغر 0.78 (حشو 16dp ← 12.5dp) والأكبر 1.28 (← 20.5dp) — لا يكسران صفًّا.
        ThemeDensity.entries.forEach { assertTrue(it.scale in 0.75f..1.30f) }
    }

    // ── مفاتيح التخزين ───────────────────────────────────────────────────────

    @Test
    fun `storage keys are distinct and never collide with the legacy theme keys`() {
        val keys = listOf(
            ThemeKeys.CONTRAST,
            ThemeKeys.ROUNDNESS,
            ThemeKeys.FONT_SCALE,
            ThemeKeys.NAV_BAR_STYLE,
            ThemeKeys.MOTION,
            ThemeKeys.DENSITY,
            ThemeKeys.CARD_STYLE,
        )
        assertTrue(keys.none { it.isBlank() })
        assertEquals(keys.size, keys.toSet().size)

        // المفاتيح القديمة تُقرأ كما هي بلا هجرة — فمفتاح جديد بنفس اسمها يمحو اختيارًا قائمًا.
        val legacy = listOf("color_mode", "key_color", "color_spec", "expressive_blur_ui")
        assertTrue("تصادم مع مفتاح قديم: ${keys.filter { it in legacy }}", keys.none { it in legacy })
    }

    // ── الأنماط الستّة ───────────────────────────────────────────────────────

    @Test
    fun `presets cover the documented spread with distinct opaque seeds`() {
        assertEquals(6, ThemeStudioPresets.size)
        assertEquals(6, ThemeStudioPresets.map { it.id }.toSet().size)

        ThemeStudioPresets.forEach { preset ->
            assertTrue("معرّف فارغ", preset.id.isNotBlank())
            assertTrue("لا مورد اسم للنمط ${preset.id}", preset.nameRes != 0)
            assertNotEquals("بذرة النمط ${preset.id} صفرية (تعني «تلقائي» لا لونًا)", 0, preset.seed)
            assertEquals(
                "بذرة النمط ${preset.id} ليست معتمة",
                0xFF,
                (preset.seed shr 24) and 0xFF,
            )
        }

        // التوزيع الموثّق في `ThemeStudioPresets` (٢ افتراضيّ · ٣ متوسط · ١ عالٍ):
        assertEquals(2, ThemeStudioPresets.count { it.contrast == ThemeContrast.DEFAULT })
        assertEquals(3, ThemeStudioPresets.count { it.contrast == ThemeContrast.MEDIUM })
        assertEquals(1, ThemeStudioPresets.count { it.contrast == ThemeContrast.HIGH })
        // (٣ دائريّة · ١ حادّة · ٢ قياسيّة):
        assertEquals(3, ThemeStudioPresets.count { it.roundness == ThemeRoundness.ROUND })
        assertEquals(1, ThemeStudioPresets.count { it.roundness == ThemeRoundness.SHARP })
        assertEquals(2, ThemeStudioPresets.count { it.roundness == ThemeRoundness.STANDARD })
        // والمظاهر: نظام · فاتحان · داكنان · أموليد:
        assertEquals(1, ThemeStudioPresets.count { it.mode == ColorMode.SYSTEM })
        assertEquals(2, ThemeStudioPresets.count { it.mode == ColorMode.LIGHT })
        assertEquals(2, ThemeStudioPresets.count { it.mode == ColorMode.DARK })
        assertEquals(1, ThemeStudioPresets.count { it.mode == ColorMode.DARKAMOLED })
    }

    @Test
    fun `matches is true only for the exact four-value combination`() {
        val preset = ThemeStudioPresets.first { it.id == "midnight" }
        assertTrue(preset.matches(preset.seed, preset.mode, preset.contrast, preset.roundness))
        assertFalse(preset.matches(preset.seed, ColorMode.LIGHT, preset.contrast, preset.roundness))
        assertFalse(preset.matches(preset.seed, preset.mode, ThemeContrast.HIGH, preset.roundness))
        assertFalse(preset.matches(preset.seed, preset.mode, preset.contrast, ThemeRoundness.SHARP))
        assertFalse(preset.matches(0xFFFF0000.toInt(), preset.mode, preset.contrast, preset.roundness))
    }

    @Test
    fun `every preset seed is a swatch the accent row still offers`() {
        // القاعدة الموثّقة في ThemeSettings.kt: «البذرة من شرائح الشاشة نفسها … فلا يدخل
        // التطبيق لونٌ لا يستطيع المستخدم اختياره بيده». تُقاس من المصدر لأن اللوحة خاصّة
        // بالشاشة (`private val keyColorOptions`)، فلو شُذبت شريحة من الصفّ يوماً ونُسي
        // نمطٌ يعتمدها، كشف هذا الاختبار التناقض قبل أن يراه المستخدم.
        val source = locateSource("ui/subscreens/CustomThemeScreen.kt")
        assumeTrue("CustomThemeScreen.kt not found; guard not evaluated", source != null)

        val text = source!!.readText()
        val start = text.indexOf("private val keyColorOptions")
        assertTrue("keyColorOptions not found in the source", start >= 0)
        val end = text.indexOf("\n)", start)
        assertTrue("keyColorOptions block does not close as expected", end > start)

        // القطع بـ`0x` كاملةً (مع قناة الألفا `FF`) لأن بذور الأنماط هي `0xFF……` كأعداد ARGB —
        // واقتطاع `FF` كان يعطي ٢٤ بتًا فتُقارَن بـ`seed` بتمثيل مختلف (قِيس: فشل الاختبار أول مرة).
        val palette = Regex("0x(FF[0-9A-Fa-f]{6})")
            .findAll(text.substring(start, end))
            .map { it.groupValues[1].toLong(16).toInt() }
            .toSet()
        assertTrue("palette parse is empty — the regex and the source drifted", palette.isNotEmpty())

        ThemeStudioPresets.forEach { preset ->
            assertTrue(
                "بذرة النمط ${preset.id} (${preset.seed.toUInt().toString(16)}) ليست في keyColorOptions",
                preset.seed in palette,
            )
        }
    }

    private fun locateSource(relative: String): File? =
        listOf(
            File("src/main/java/nd/max"),
            File("app/src/main/java/nd/max"),
            File("../app/src/main/java/nd/max"),
            File("manager/app/src/main/java/nd/max"),
        ).firstOrNull { it.isDirectory }?.resolve(relative)
}
