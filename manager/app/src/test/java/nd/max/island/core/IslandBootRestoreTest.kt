/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.island.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * الجزيرة بعد الإقلاع — **القرار مقيس على JVM** قبل أن يُلمس جهاز.
 *
 * والعقود هنا هي التي تمنع الأعراض التي رآها المستخدم:
 *
 * 1. **لا جزيرة أطفأها المستخدم** — ولو بقيت خدمة أو استضافة وصولية من الجلسة السابقة.
 * 2. **لا خدمة أمامية ثانية** حين تستضيف الوصولية النافذة بنفسها.
 * 3. **مسار «الرسم فوق التطبيقات» وحده يعيدها** — وهو المسار الذي كان غائبًا تمامًا، فمن
 *    منحه وحده لم تعد له جزيرة بعد الإقلاع.
 * 4. **ولا ادّعاء نجاح بلا ربط**: وصولية ممنوحة لم تُربط بعد ولا صلاحية رسم ⇒ `Pending`.
 * 5. **والنيّة القديمة تُنقل** ولا تُشعل الجديد من فراغ.
 */
class IslandBootRestoreTest {

    private fun decide(
        enabled: Boolean = true,
        accessibilityConnected: Boolean = false,
        accessibilityEnabled: Boolean = false,
        overlayGranted: Boolean = false,
        serviceRunning: Boolean = false,
    ) = IslandBootRestore.decide(
        enabled = enabled,
        accessibilityConnected = accessibilityConnected,
        accessibilityEnabled = accessibilityEnabled,
        overlayGranted = overlayGranted,
        serviceRunning = serviceRunning,
    )

    @Test
    fun `a disabled island is never started even if a service or accessibility session survived`() {
        assertEquals(
            IslandBootRestore.Step.SKIP_DISABLED,
            decide(enabled = false, accessibilityConnected = true, accessibilityEnabled = true, overlayGranted = true, serviceRunning = true),
        )
    }

    @Test
    fun `an accessibility host wins over an extra foreground notification`() {
        assertEquals(
            IslandBootRestore.Step.ACCESSIBILITY_HOSTED,
            decide(accessibilityConnected = true, accessibilityEnabled = true, overlayGranted = true, serviceRunning = true),
        )
    }

    @Test
    fun `a running service is not started twice`() {
        assertEquals(
            IslandBootRestore.Step.ALREADY_RUNNING,
            decide(overlayGranted = true, serviceRunning = true),
        )
    }

    @Test
    fun `overlay alone starts the current engine`() {
        // هذا هو العطب المقيس: بلا وصولية لا يعيد النظام ربط شيء، وكانت الخدمة الحالية لا
        // تُقلع من أيّ مسار بعد الإقلاع.
        assertEquals(
            IslandBootRestore.Step.START_SERVICE,
            decide(overlayGranted = true),
        )
    }

    @Test
    fun `enabled accessibility that has not bound yet is pending, not applied`() {
        assertEquals(
            IslandBootRestore.Step.WAIT_FOR_ACCESSIBILITY,
            decide(accessibilityEnabled = true),
        )
    }

    @Test
    fun `no permission at all is skipped, and no settings screen is opened from a boot broadcast`() {
        assertEquals(IslandBootRestore.Step.SKIP_NO_PERMISSION, decide())
    }

    @Test
    fun `every step is reachable`() {
        val reached = setOf(
            decide(enabled = false),
            decide(accessibilityConnected = true),
            decide(serviceRunning = true),
            decide(overlayGranted = true),
            decide(accessibilityEnabled = true),
            decide(),
        )
        assertEquals(IslandBootRestore.Step.entries.toSet(), reached)
    }

    @Test
    fun `a legacy intent enables the current engine and is cleared once`() {
        val handover = IslandBootRestore.handover(legacyActive = true, currentEnabled = false)
        assertTrue(handover.clearLegacy)
        assertTrue(handover.enableCurrent)
    }

    @Test
    fun `no legacy intent never turns the current engine on`() {
        // من أطفأ الجديد يبقى مطفأً: غيابُ نيّة قديمة ليس نيّةً جديدة.
        val off = IslandBootRestore.handover(legacyActive = false, currentEnabled = false)
        assertFalse(off.clearLegacy)
        assertFalse(off.enableCurrent)

        val on = IslandBootRestore.handover(legacyActive = false, currentEnabled = true)
        assertFalse(on.clearLegacy)
        assertTrue(on.enableCurrent)
    }

    @Test
    fun `the stored intent never defaults to on`() {
        // حرسٌ بنيويّ كحرس الطبقات: الافتراضيّ `true` كان يجعل فتح الشاشة يُشعل الجزيرة،
        // ويجعل باب الاستعادة بعد الإقلاع يعيدها لمن لم يطلبها. الجزيرة تراكبٌ فوق كل
        // التطبيقات ⇒ لا تُشعل بلا نيّة مخزونة صريحة.
        val candidates = listOf(
            File("src/main/java/nd/max/island/core/IslandSettings.kt"),
            File("app/src/main/java/nd/max/island/core/IslandSettings.kt"),
            File("../app/src/main/java/nd/max/island/core/IslandSettings.kt"),
        )
        val source = candidates.firstOrNull { it.isFile }
        assumeTrue("IslandSettings.kt not reachable from ${File("").absolutePath}", source != null)
        val body = source!!.readText()
        assertTrue(
            "the stored island intent must default to off",
            body.contains("getBoolean(\"enabled\", false)"),
        )
        assertFalse(
            "an old default-on read is back: the island would start without the user asking",
            body.contains("getBoolean(\"enabled\", true)"),
        )
    }
}
