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

    /**
     * **أدوات التشخيص تحت التشخيص، وأدوات النسخ والصلاحيات تحت الإعدادات** — وهذا قرار
     * مالك محدَّث (M1c): كان الأصل «هذه الثلاثة تحت التحكّم لا الإعدادات»، ثم أمر المالك
     * بنقلها إلى مواضعها المنطقية بعد أن صار لكلٍّ من `Settings` و`Diagnostics` بابٌ
     * يقرأ أبنائه من السجلّ. وسجلّ البطارية بقي مع عائلته في محور الطاقة (M1a).
     */
    @Test
    fun `device and data tools sit with the screen that owns them`() {
        val registry = read("ui/navigation/MaxDestinations.kt")
        val expected = mapOf(
            "DeviceLab" to ", Diagnostics)",
            "UsageStats" to ", Diagnostics)",
            "Logs" to ", Diagnostics)",
            "ColorScheme" to ", Settings)",
            // والاثنان يحملان `MaxRisk.Advanced` بعدهما، فالسطر ينتهي بالتصنيف لا بالأب — وهو
            // سبب سقوط هذا الحارس في `f693ef18`: كان الشرط يقارن `", Settings,"` بنهاية السطر.
            "MaxBackup" to ", Settings, MaxRisk.Advanced)",
            "Permissions" to ", Settings, MaxRisk.Advanced)",
        )
        expected.forEach { (name, suffix) ->
            val line = registry.lines().first { it.contains("data object $name :") }
            assertTrue(
                "$name ليست في موضعها: ${line.trim()}",
                line.trimEnd().endsWith(suffix),
            )
        }
        // وقرار إعادة هندسة المعلومات (M1a): سجلّ البطارية يقرأ عائلة الطاقة، فصار تحت `PowerHub`
        // بدل أن يبقى شاذًّا في `Control` — والباب لا يُكسر: `maxHubRows` تُولّد أبناء المحور من السجلّ.
        run {
            val line = registry.lines().first { it.contains("data object BatteryHistory :") }
            assertTrue(
                "BatteryHistory خرجت من عائلة الطاقة — نُقلت إلى ${line.trim()}",
                line.trimEnd().endsWith(", PowerHub)"),
            )
        }
        // ولا مدخل ثانٍ: صفحة التحكّم لم تعد تذكر الستّة، والإعدادات لا تفتح أدوات التشخيص بصفٍّ خاص.
        val control = read("ui/mainscreens/ControlLayoutModel.kt")
        expected.keys.forEach { name ->
            assertFalse("ControlLayoutModel ما زال يذكر $name — مدخلان لشيء واحد", control.contains("MaxDestination.$name"))
        }
        val settings = read("ui/mainscreens/SettingsScreen.kt")
        listOf("MaxDestination.DeviceLab", "MaxDestination.UsageStats", "MaxDestination.Logs")
            .forEach { needle ->
                assertFalse("SettingsScreen يفتح $needle بصفٍّ خاص — بابها في Diagnostics", settings.contains(needle))
            }
    }
}
