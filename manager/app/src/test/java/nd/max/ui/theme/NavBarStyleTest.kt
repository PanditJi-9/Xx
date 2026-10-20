/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.theme

import androidx.compose.ui.unit.dp
import java.io.File
import nd.max.ui.design.MaxSize
import nd.max.ui.subscreens.descriptionRes
import nd.max.ui.subscreens.labelRes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * عقد **أنماط الشريط السفلي** (§٦-ب من خطة المستوى): «أضف خيارات متعددة بتصاميم مستقلة… تأكد من
 * بقاء التنقل ظاهرًا وقابلًا للاستخدام في كل حالات الصلاحيات، ومن عدم تداخل العناصر مع إيماءات
 * النظام أو لوحة المفاتيح أو مساحة العرض الآمنة».
 *
 * والقياس على أربعة مستويات:
 *
 * 1. **الاستقلال**: ستّة أنماط، ولا نمط بقيوده نسخةٌ من آخر — والافتراضيّ [NavBarStyle.FLOATING]
 *    لم تتغيّر قيوده (٢٣dp أيقونة · ٦٤dp أرض · نصّ دائمًا)، فلا يتغيّر شكل شاشةٍ لمن لم يطلب.
 * 2. **الأرض اللمسية**: أقلّ ارتفاع عنصر في كل نمط ≥ حدّ `MaxSize.minTouchTarget` — ومن أسقط صفّ
 *    النصّ ([NavLabelMode.NONE]) لا يصير زرًّا صامتًا: الأيقونة تحمل `contentDescription` من نصّ
 *    الوجهة نفسه (مقيسٌ بـ`grep` على موضعه في `MaxNavBar`).
 * 3. **المصدر الواحد**: الرسم والمعاينة في الاستوديو يقرآن القواعد من النمط (`style.labels` ·
 *    `navStyle.iconSize`…) لا من جدولٍ ثانٍ — والاستوديو يبني الستّة من `NavBarStyle.entries`.
 * 4. **النصوص**: كل نمط له اسمٌ ووصف — والستّة معنونة في اللغتين، ومجموعة مفاتيح `theme_nav_*`
 *    **متطابقة** بين الإنجليزية والعربية (لا نصّ في لغة دون أخرى).
 *
 * **والحدّ المُعلَن:** «هل يبدو كل نمط فعلًا مختلفًا وجميلًا؟» و«هل يلمس المستخدم الزرّ الصحيح على
 * شاشةٍ صغيرة؟» — UAT على جهاز. والمقيس هنا: البنية والتغطية والربط، لا الذوق.
 */
class NavBarStyleTest {

    // ── ① الاستقلال: لا نمط نسخةٌ من آخر ─────────────────────────────────────

    @Test
    fun `six styles, and no two share the same structural rules`() {
        assertEquals("الأنماط ستّة: خمسة في الخطة + «المتكيّف مع المحتوى»", 6, NavBarStyle.entries.size)
        val rules = NavBarStyle.entries.map { style ->
            listOf(
                style.floating,
                style.labels,
                style.expandsActive,
                style.iconSize,
                style.itemMinHeight,
                style.iconTextGap,
                style.itemPadV,
                style.largeLabels,
                style.pagePlane,
            )
        }
        assertEquals(
            "نمطٌ بقيود نمطٍ آخر = نسخة أخرى من النمط نفسه، وهو ما نهت عنه الخطة",
            rules.size,
            rules.toSet().size,
        )
    }

    @Test
    fun `the default style keeps the shipped look`() {
        assertEquals(NavBarStyle.FLOATING, NavBarStyle.fromKey(null))
        assertEquals(NavBarStyle.FLOATING, NavBarStyle.fromKey("no_such_key"))
        val floating = NavBarStyle.FLOATING
        assertTrue("الافتراضيّ عائم", floating.floating)
        assertEquals("نصّ دائمًا", NavLabelMode.ALWAYS, floating.labels)
        assertEquals("أيقونة ٢٣dp كما كانت", 23.dp, floating.iconSize)
        assertEquals("أرض ٦٤dp كما كانت", 64.dp, floating.itemMinHeight)
        assertTrue("ولا تمدّد في الافتراضيّ", !floating.expandsActive)
        assertTrue("ولا طباعة أكبر فيه", !floating.largeLabels)
    }

    // ── ② الأرض اللمسية وإمكانية الوصول ─────────────────────────────────────

    @Test
    fun `every style stays at or above the touch-target floor`() {
        NavBarStyle.entries.forEach { style ->
            assertTrue(
                "«${style.key}» ينزل تحت حدّ اللمس (${style.itemMinHeight} < ${MaxSize.minTouchTarget})",
                style.itemMinHeight >= MaxSize.minTouchTarget,
            )
        }
        assertEquals(
            "ولا فراغ بين أيقونة ونصٍّ غير مرغوب: الغياب صفر",
            0.dp,
            NavBarStyle.COMPACT.iconTextGap,
        )
    }

    @Test
    fun `a hidden label moves the description to the icon`() {
        assertEquals(NavLabelMode.NONE, NavBarStyle.COMPACT.labels)
        assertEquals(NavLabelMode.ACTIVE_ONLY, NavBarStyle.LABELED.labels)
        assertTrue(NavBarStyle.LABELED.expandsActive)

        val bar = sourceFile("ui/navigation/MaxNavBar.kt").readText()
        assertTrue(
            "قاعدة النصّ تُقرأ من النمط لا من فرع مكتوب في الرسم",
            bar.contains("when (style.labels)"),
        )
        assertTrue(
            "وبلا نصٍّ ظاهر ينتقل الوصف إلى الأيقونة — وإلا صار العنصر صامتًا لقارئ الشاشة",
            bar.contains("contentDescription = if (showLabel) null else stringResource(item.labelRes)"),
        )
        assertTrue(
            "وتمدّد العنصر النشط يُقرأ من النمط أيضًا",
            bar.contains("if (style.expandsActive && isSelected) 1.9f else 1f"),
        )
    }

    // ── ③ المصدر الواحد: رسمٌ ومعاينةٌ واستوديو ──────────────────────────────

    @Test
    fun `the bar, the preview and the studio all read the same rules`() {
        val preview = sourceFile("ui/subscreens/CustomThemeScreen.kt").readText()
        assertTrue(
            "المعاينة ترسم بالقيود نفسها لا بنسخةٍ منها",
            preview.contains("navStyle.labels") &&
                preview.contains("navStyle.iconSize") &&
                preview.contains("navStyle.itemMinHeight") &&
                preview.contains("navStyle.expandsActive"),
        )

        val studio = sourceFile("ui/subscreens/ThemeStudioSections.kt").readText()
        assertTrue(
            "والاستوديو يبني الستّة من السجلّ نفسه — لا قائمة موازية تُنسى إن زاد نمط",
            studio.contains("NavBarStyle.entries.forEach") && studio.contains("MaxChoiceRow("),
        )

        val activity = sourceFile("MainActivity.kt").readText()
        assertTrue(
            "والنمط يصل إلى الشريطين معًا: السفليّ والرأس (والاثنان «يجب أن يبقيا متطابقين»)",
            activity.contains("style = navBarStyle"),
        )
    }

    // ── ④ النصوص: ستّة × لغتان ──────────────────────────────────────────────

    @Test
    fun `every style has a name and a description, and both languages agree`() {
        assertEquals(
            "اسمٌ لكل نمط، ولا نمطان بنصّ واحد",
            NavBarStyle.entries.size,
            NavBarStyle.entries.map { it.labelRes() }.toSet().size,
        )
        assertEquals(
            "ووصفٌ لكل نمط، ولا نمطان بوصف واحد",
            NavBarStyle.entries.size,
            NavBarStyle.entries.map { it.descriptionRes() }.toSet().size,
        )

        val en = themeNavKeys(resFile("values/strings.xml").readText())
        val ar = themeNavKeys(resFile("values-ar/strings.xml").readText())
        assertEquals(
            "مجموعة مفاتيح شريط التنقّل متطابقة بين اللغتين — لا نصّ في لغة دون أخرى",
            en,
            ar,
        )
        assertEquals(
            "ثلاثة عشر مفتاحًا: ٦ أسماء + ٦ أوصاف + عنوان القسم — ولا مفتاح زائد",
            13,
            en.size,
        )
    }

    /** مفاتيح `theme_nav_*` في ملف موارد — تُقرأ بالنصّ فلا تحتاج مترجمًا ولا جهازًا. */
    private fun themeNavKeys(xml: String): Set<String> =
        Regex("name=\"(theme_nav_[a-z_]+)\"").findAll(xml).map { it.groupValues[1] }.toSet()

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
