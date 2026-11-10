/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.subscreens

import nd.max.ui.navigation.MaxDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * عقد **بيت أدوات المختبر**: الأدوات المتقدّمة تُسكن في سجلّ الوجهات تحت
 * [MaxDestination.DeviceLab]، وتعرضها شاشة المختبر من السجلّ نفسه ([maxHubRows])
 * لا من قائمة مكتوبة بجانبها.
 *
 * والعطب المحروس عيبٌ بنيوي مقيس في `docs/ai/PRODUCT-MAP.md` (ع١): كانت هذه الثلاثة
 * صفوفًا سائبة في صندوق أدوات التحكّم — اثنا عشر صفًّا بلا عنوان جامع — فتُقرأ الصفحة
 * مخزنًا لا محورًا. ونقلها إلى المختبر بلا قائمة ثانية هو ما يجعل الإضافة اللاحقة
 * تحدث في السجلّ وحده.
 */
class DeviceLabToolsTest {

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

    /** الاسم كما يُكتب في السجلّ، والوجهة نفسها — الاثنان معًا حتى لا يمرّ حارسٌ على اسمٍ قديم. */
    private val labTools = listOf(
        "SetEdit" to MaxDestination.SetEdit,
        "ActivityLauncher" to MaxDestination.ActivityLauncher,
        "FileManager" to MaxDestination.FileManager,
    )

    @Test
    fun `the lab tools are parented by the device lab`() {
        labTools.forEach { (name, tool) ->
            assertEquals(
                "$name is not owned by the device lab",
                MaxDestination.DeviceLab,
                tool.parent,
            )
        }
    }

    @Test
    fun `the lab renders its tools from the registry, not from a second list`() {
        val screen = read("ui/subscreens/DeviceLabScreen.kt")
        assertTrue(
            "DeviceLabScreen stopped reading its children from the registry",
            screen.contains("maxHubRows(MaxDestination.DeviceLab)"),
        )
    }

    @Test
    fun `no lab tool keeps a second door in the control toolbox`() {
        val model = read("ui/mainscreens/ControlLayoutModel.kt")
        labTools.forEach { (name, _) ->
            assertFalse(
                "$name is still listed on the control page — two doors for one screen",
                model.contains("MaxDestination.$name"),
            )
        }
    }
}
