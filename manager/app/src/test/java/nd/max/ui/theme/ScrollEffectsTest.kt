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
 * عقد **تأثيرات التمرير** (تُعرض باسم «الظهور التدريجي») — معيار القبول ٨: «مفعّلة افتراضيًّا · قابلة للتعطيل · تحترم إعدادات
 * إمكانية الوصول».
 *
 * والقياس هنا على ثلاثة مستويات، وكلٌّ منها يقيس ما يستطيعه:
 *
 * 1. **سلوك القراءة** (JVM حقيقي): `ThemeStudio.scrollEffects` بمخزن تفضيلات مصنوع بـ`Proxy`
 *    — الغياب يعود «مفعّلة»، والمكتوب يُقرأ كما هو. وهذا ما لا يُقاس على جهاز في كل مراجعة.
 * 2. **البوابة الواحدة**: المواضع الثلاثة المربوطة في `MaxMotion` (`MaxReveal` · `maxEnter` ·
 *    `maxItemMotion`) تمرّ كلها بـ`rememberScrollEffectsEnabled()`، ولا موضع يتجاوزها إلى
 *    تفضيل النظام وحده.
 * 3. **وجود المفتاح في الشاشة وفي اللغتين**: لا مفتاحَ يُكتب ولا يُقرأ، ولا نصَّ بالإنجليزية
 *    وحدها (ADR-14).
 *
 * **والحدّ المُعلَن:** «هل يبدو الكشف هادئًا على الزجاج؟» و«هل يتوقف فعلًا في كل مكوّن عند
 * التعطيل؟» — يبقى الأوّل UAT، ويبقى الثاني مرئيًّا لا مُدَّعى، حتى يُشغَّل على جهاز.
 */
class ScrollEffectsTest {

    // ── ① سلوك القراءة ────────────────────────────────────────────────────────

    @Test
    fun `absent key means the effect is on, and a stored value is followed as-is`() {
        assertTrue(
            "لا مفتاح ⇒ ما كان عليه التطبيق قبل المفتاح: مفعّلة",
            ThemeStudio.scrollEffects(prefsOf(emptyMap())),
        )
        assertFalse(
            "والمكتوب false يُعطّلها فعلًا",
            ThemeStudio.scrollEffects(prefsOf(mapOf(ThemeKeys.SCROLL_EFFECTS to false))),
        )
        assertTrue(
            "والمكتوب true يُبقيها",
            ThemeStudio.scrollEffects(prefsOf(mapOf(ThemeKeys.SCROLL_EFFECTS to true))),
        )
        assertEquals(
            "والمفتاح نصّه واحد لا نسختان",
            "theme_scroll_effects",
            ThemeKeys.SCROLL_EFFECTS,
        )
    }

    @Test
    fun `a value of another type never crashes the read`() {
        // مخزن أقدم أو ملفّ عُدّل يدويًّا: المفتاح موجود بقيمة نصّية. القراءة لا تُسقط شيئًا،
        // والقيمة غير المنطقية تُقرأ افتراضيًّا («لا نعرف» ⇒ السلوك المعتاد لا إسقاط حركة).
        assertTrue(
            ThemeStudio.scrollEffects(prefsOf(mapOf(ThemeKeys.SCROLL_EFFECTS to "nonsense"))),
        )
    }

    // ── ② البوابة الواحدة في MaxMotion ────────────────────────────────────────

    @Test
    fun `all three scroll-driven APIs pass through the single gate`() {
        val motion = sourceFile("ui/component/MaxMotion.kt").readText()
        assertTrue(
            "البوابة معرَّفة",
            motion.contains("fun rememberScrollEffectsEnabled()"),
        )
        assertTrue(
            "وتقرأ تفضيل المستخدم من مخزن الإعدادات",
            motion.contains("ThemeStudio.scrollEffects("),
        )
        assertTrue(
            "وتحترم تقليل الحركة في النظام",
            motion.contains("val systemAllows = rememberAnimationsEnabled()"),
        )
        val declared = if (motion.contains("fun rememberScrollEffectsEnabled()")) 1 else 0
        val uses = Regex("rememberScrollEffectsEnabled\\(\\)").findAll(motion).count()
        assertEquals(
            "ثلاثة مواضع فقط تمرّ بالبوابة: كشف كتلة · دخول كتلة · حركة صفّ",
            3,
            uses - declared,
        )
        assertFalse(
            "ولا موضع في الحركة يتجاوز البوابة إلى تفضيل النظام وحده",
            motion.contains("if (!rememberAnimationsEnabled())"),
        )
        assertFalse(
            "ولا موضع يُبقي الصفّ يتحرّك بها دون المفتاح",
            motion.contains("if (rememberAnimationsEnabled())"),
        )
    }

    // ── ③ المفتاح في الشاشة وفي اللغتين ──────────────────────────────────────

    @Test
    fun `the customization screen owns the switch and writes the key`() {
        val sections = sourceFile("ui/subscreens/ThemeStudioSections.kt").readText()
        assertTrue(
            "الصفّ يعرض عنوانه من الموارد لا نصًّا محفورًا",
            sections.contains("stringResource(R.string.str_scroll_effects)"),
        )
        assertTrue(
            "والكتابة تمرّ بنقطة الكتابة الواحدة",
            sections.contains("persistFlag(prefs, ThemeKeys.SCROLL_EFFECTS, next)"),
        )
        assertFalse(
            "والقيمة لا تُكتب حين لا تتغيّر",
            sections.contains("if (old == new) return").not(),
        )
        assertFalse(
            "الصفّ بلا وصف طويل: لا يُقرأ مفتاح الوصف في الاستوديو",
            sections.contains("str_scroll_effects_summary"),
        )
    }

    @Test
    fun `the title exists in English and Arabic, and the title does not say scroll`() {
        val en = resFile("values/strings.xml").readText()
        val ar = resFile("values-ar/strings.xml").readText()
        assertTrue("ناقص في الإنجليزية: str_scroll_effects", en.contains("name=\"str_scroll_effects\""))
        assertTrue("ناقص في العربية: str_scroll_effects", ar.contains("name=\"str_scroll_effects\""))
        // الوصف الطويل أُزيل من الصفّ: لا يبقى مفتاحه يتيمًا في اللغتين
        assertFalse("الوصف أُزيل فلا مفتاح له", en.contains("str_scroll_effects_summary"))
        assertFalse("الوصف أُزيل فلا مفتاح له", ar.contains("str_scroll_effects_summary"))
        // الاسم لا يحمل «التمرير»: الكلمة صارت لإيماءة التنقّل الأفقي، فكان الاسم يُقرأ أنه أفقي
        val arTitle = Regex("name=\"str_scroll_effects\">([^<]*)<").find(ar)?.groupValues?.get(1).orEmpty()
        assertFalse("الاسم لا يحمل «التمرير»: $arTitle", arTitle.contains("التمرير"))
    }

    // ── أدوات القياس ─────────────────────────────────────────────────────────

    /** مخزن تفضيلات في الذاكرة — بلا أندرويد، وبلا ملفّ؛ يكفي لقراءة واحدة. */
    private fun prefsOf(values: Map<String, Any?>): SharedPreferences =
        Proxy.newProxyInstance(
            SharedPreferences::class.java.classLoader,
            arrayOf(SharedPreferences::class.java),
        ) { _, method, args ->
            val key = (args?.getOrNull(0) as? String) ?: ""
            when (method.name) {
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
