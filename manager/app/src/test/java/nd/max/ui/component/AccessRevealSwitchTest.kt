/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.component

import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * **حارس مفتاح الكشف — الباب الظاهر لمن يظنّ الميزة غير موجودة.**
 *
 * أمر المالك (تكملة ٨٧): «اذا اختار shuzki … تختفي بعض الميزات والبطاقات والشاشات واذا اراد
 * معرفة وجودها يفعل المفتاح». فالمفتاح يجب أن يكون في الموضعين اللذين سمّاهما — شاشة البداية
 * والإعدادات — وأن يقرأ ويكتب **المخزن نفسه** الذي يقرؤه الفلتر (`AccessFilterStore`)، لا
 * تفضيلًا ثانيًا يوهم بأن للعرض مصدرَي حقيقة.
 *
 * والحدّ الذي يحرسه الاختبار: `hide_unavailable_features` مفتاح آخر (قدرات الجهاز، لا مستوى
 * الوصول) — فلا يُخلط بينهما، ولا يُستبدل أحدهما بالآخر.
 */
class AccessRevealSwitchTest {

    private lateinit var sourceRoot: File

    @Before
    fun locateSourceRoot() {
        val root = listOf(
            "src/main/java/nd/max",
            "app/src/main/java/nd/max",
            "../app/src/main/java/nd/max",
            "manager/app/src/main/java/nd/max",
        ).map(::File).firstOrNull { it.isDirectory }
        assumeTrue("source tree not available in this runtime", root != null)
        sourceRoot = requireNotNull(root)
    }

    private fun read(path: String): String = File(sourceRoot, path).readText()

    @Test
    fun `the reveal switch is offered where the owner asked for it`() {
        val begin = read("ui/mainscreens/GetStartedScreen.kt")
        val settings = read("ui/mainscreens/SettingsScreen.kt")
        assertTrue(
            "شاشة البداية لا تعرض AccessRevealSwitch — من اختار Shizuku فيها يظنّ الأدوات غير موجودة",
            begin.contains("AccessRevealSwitch()"),
        )
        assertTrue(
            "الإعدادات لا تعرض AccessRevealSwitch — لا باب ثانٍ لمن فاته الإعداد الأول",
            settings.contains("AccessRevealSwitch()"),
        )
    }

    @Test
    fun `the switch drives the one filter store, not a second preference`() {
        val source = read("ui/component/AccessRevealSwitch.kt")
        listOf(
            "AccessFilterStore.state",
            "AccessFilterStore.save",
            "AccessFilter.SUPPORTED_ONLY",
            "AccessFilter.EVERYTHING",
        ).forEach { needle ->
            assertTrue("AccessRevealSwitch لا يذكر $needle", source.contains(needle))
        }
        // ولا يكتب في تفضيل «إخفاء الميزات غير المتاحة»: ذاك حكم قدرات الجهاز، وهذا حكم
        // مستوى الوصول — وخلطهما يجعل مفتاحًا واحدًا يغيّر معنيين.
        assertTrue(
            "AccessRevealSwitch يخلط حكم الوصول بحكم قدرات الجهاز",
            !source.contains("HIDE_UNAVAILABLE"),
        )
    }
}
