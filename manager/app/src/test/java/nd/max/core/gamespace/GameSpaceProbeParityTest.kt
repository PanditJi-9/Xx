/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.core.gamespace

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * `tools/gamespace_shell_probe.sh` يجرّب على الجهاز **الأوامر نفسها** التي يرسلها التطبيق تحت Shizuku. فإن تغيّر
 * مفتاح أو أمر في الكود دون المسبار صار المسبار يطمئن على شيء لم يعد يُنفَّذ؛ وهذا الحارس يجمعهما.
 */
class GameSpaceProbeParityTest {

    private fun script(): String {
        val candidates = listOf(
            File("../../tools/gamespace_shell_probe.sh"), File("../tools/gamespace_shell_probe.sh"),
            File("tools/gamespace_shell_probe.sh"), File("manager/../tools/gamespace_shell_probe.sh")
        )
        val file = candidates.firstOrNull { it.isFile }
        assumeTrue("Cannot locate tools/gamespace_shell_probe.sh", file != null)
        return file!!.readText()
    }

    @Test
    fun `the probe writes back every settings key the in-app profile writes`() {
        val text = script()
        val keys = InAppProfileRules.REFRESH_REFS + InAppProfileRules.HAPTIC
        for (ref in keys) {
            assertTrue(
                "the probe must write back ${ref.namespace} ${ref.key}",
                text.contains("writeback ${ref.namespace} ${ref.key} ")
            )
        }
        assertTrue(text.contains("settings get global zen_mode") && text.contains("settings put global zen_mode"))
        for (ns in listOf("global", "secure", "system")) assertTrue("namespace $ns not probed", text.contains("for ns in global secure system"))
    }

    @Test
    fun `the probe exercises the same commands as the lane-routed features`() {
        val text = script()
        for (needle in listOf(
            "screencap -p",
            "screenrecord --time-limit",
            "wm size",
            "wm density",
            "appops set \$PKG GET_USAGE_STATS",
            "svc wifi",
            "svc data",
            "pkill -2 screenrecord",
        )) assertTrue("probe is missing: $needle", text.contains(needle))
    }

    @Test
    fun `the probe never leaves its test keys or files behind`() {
        val text = script()
        assertTrue(text.contains("settings delete \$ns \$key"))
        assertTrue(text.contains("rm -rf \$PIC"))
        assertTrue(text.contains("rm -f \$MOV"))
    }
}
