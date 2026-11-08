/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.mainscreens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * **قرارات المالك على السطح — محروسة لا محفوظة في الذاكرة.**
 *
 * الموجة الثالثة أزالت شريط «من يحكم» من «الآن» وسلّم الحكّام من «التحكّم»، ونقلت مفتاح
 * مكتبة الألعاب من بطاقة داخل الصفحة إلى شريط العنوان، ونقلت ثلاث أدوات جهاز من الإعدادات
 * إلى التحكّم. وكلّها قرارات **تُعاد بضغطة واحدة** لو لم يقم لها حارس: كافٍ أن تُضاف البطاقة
 * من جديد «لأنها مفيدة» ليعود ما أمر المالك بإزالته.
 *
 * وحدّه المُعلَن: فحص نصّيّ على المصدر. الشكل على الجهاز (محاذاة، لمس، RTL) يبقى غير مختبر هنا.
 */
class OwnerDecisionsGuardTest {

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

    private fun read(path: String): String = File(sourceRoot, path).readText()

    /** ما أدخله المالك مرّة كزينة لا يعود إلى الشاشتين بلا قرارٍ جديد مكتوب. */
    @Test
    fun `governance stays out of Now and Control`() {
        listOf(
            "ui/mainscreens/LegendaryHomeDashboard.kt",
            "ui/mainscreens/HomeScreen.kt",
            "ui/mainscreens/ControlScreen.kt",
        ).forEach { file ->
            val body = read(file)
            assertFalse(
                "$file يعرض «من يحكم» من جديد — أزاله المالك من هاتين الشاشتين (الموجة الثالثة)",
                body.contains("MaxGovernorStrip") || body.contains("MaxGovernanceLadder"),
            )
        }
        assertTrue(
            "وبطاقة الحكّام تبقى حيث تُقرأ بالضغط: التشخيص",
            read("ui/mainscreens/DiagnosticsScreen.kt").contains("MaxGovernanceCard"),
        )
    }

    /** مفتاح المكتبة في شريط العنوان لا بطاقةً ضخمة وسط الصفحة. */
    @Test
    fun `library membership lives in the top bar`() {
        val body = read("ui/subscreens/AppSettingsScreen.kt")
        assertFalse(
            "عادت بطاقة «إدراج في مكتبة الألعاب» إلى المحتوى — المالك أمر بإزالتها",
            body.contains("app_game_library_title"),
        )
        assertTrue(
            "ولا بدّ أن يكون المفتاح في الشريط فعلًا: كتابةُ العضوية من الشاشة",
            body.contains("gameSpace.membership"),
        )
    }

    /** أدوات الجهاز تحت التحكّم، ولا مدخل لها في الإعدادات. */
    @Test
    fun `device tools are parented by Control and absent from Settings`() {
        val registry = read("ui/navigation/MaxDestinations.kt")
        listOf("DeviceLab", "UsageStats", "BatteryHistory").forEach { name ->
            val line = registry.lines().first { it.contains("data object $name :") }
            assertTrue(
                "$name عادت تحت Settings — نقلها المالك إلى التحكّم",
                line.trimEnd().endsWith(", Control)"),
            )
        }
        val settings = read("ui/mainscreens/SettingsScreen.kt")
        listOf("MaxDestination.DeviceLab", "MaxDestination.UsageStats", "MaxDestination.BatteryHistory")
            .forEach { needle ->
                assertFalse("SettingsScreen ما زال يفتح $needle بصفٍّ خاص", settings.contains(needle))
            }
    }
}
