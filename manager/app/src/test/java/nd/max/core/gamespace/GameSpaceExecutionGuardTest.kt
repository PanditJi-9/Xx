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
import org.junit.Before
import org.junit.Test

/**
 * حارس **فصل الأوضاع** في مساحة الألعاب: كل أمر يمسّ النظام يمرّ بمسار واحد يعرف الوضع
 * (`PrivilegedShell.exec` في `core/privilege`: جذر ← Shizuku ← رفض معلَن)، لا بـ`Shell.cmd/getShell/su`
 * مباشرةً — فهذه الثلاثة جذرٌ فقط وتجعل وضع Shizuku يفشل في صمت (أو تُظهر نافذة `su` من اللوحة).
 *
 * **سقف تنازليّ لا قائمة سماح:** الأعداد أدناه هي الدَّين الحالي المقيس (٢٠٢٦-١٠-٢٥) وتنقص ولا تزيد؛
 * ملفّ جديد في `core/gamespace` أو شاشات اللعب سقفه صفر. وحين يُنقل موضع إلى المسار الموحَّد يُخفَّض سقفه
 * هنا في الدفعة نفسها (كما تُخفَّض أسقف `code_health`).
 */
class GameSpaceExecutionGuardTest {
    private lateinit var sourceRoot: File

    /** الدَّين المقيس: ملفّ ← أقصى عدد استدعاءات مباشرة مسموح. */
    private val ceilings = mapOf(
        "core/gamespace/PanelControls.kt" to 1,
        "core/gamespace/PanelDevice.kt" to 1
    )

    private val directShell = Regex("""\bShell\.(cmd|getShell|su)\b""")

    @Before
    fun locate() {
        val found = listOf(
            File("src/main/java/nd/max"),
            File("app/src/main/java/nd/max"),
            File("../app/src/main/java/nd/max"),
            File("manager/app/src/main/java/nd/max")
        ).firstOrNull { it.isDirectory }
        assumeTrue("Cannot locate nd.max sources; guard not evaluated", found != null)
        sourceRoot = found!!
    }

    private fun code(file: File): String = file.readText()
        .replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")
        .replace(Regex("//[^\\n]*"), "")

    /** ملفات مساحة الألعاب: منطقها كله + شاشات اللعب والوحة العائمة والتصويب. */
    private fun gameSpaceFiles(): List<File> {
        val logic = File(sourceRoot, "core/gamespace").listFiles { f -> f.extension == "kt" }.orEmpty().toList()
        val ui = listOf(
            "ui/component" to Regex("(GamePanel|Cockpit|Lobby|Cube|Crosshair|Hud)\\w*\\.kt"),
            "ui/subscreens" to Regex("(GameLobby|Lobby|Crosshair)\\w*\\.kt"),
            "service" to Regex("(GamePanelService|CrosshairOverlay)\\.kt")
        ).flatMap { (dir, name) ->
            File(sourceRoot, dir).listFiles { f -> name.matches(f.name) }.orEmpty().toList()
        }
        return logic + ui
    }

    @Test
    fun `game space does not add direct root shell calls`() {
        val files = gameSpaceFiles()
        assertTrue("no game space sources found; the guard would pass vacuously", files.size > 20)
        val over = files.mapNotNull { file ->
            val relative = file.relativeTo(sourceRoot).invariantSeparatorsPath
            val count = directShell.findAll(code(file)).count()
            val ceiling = ceilings[relative] ?: 0
            if (count > ceiling) "$relative: $count direct Shell.cmd/getShell/su (ceiling $ceiling)" else null
        }
        assertTrue(
            "direct root-only shell calls in game space — route through PrivilegedShell.exec " +
                "(core/privilege) so Shizuku mode works and su is never requested implicitly:\n" +
                over.joinToString("\n"),
            over.isEmpty()
        )
    }

    @Test
    fun `every ceiling still describes a real file so the debt list cannot rot`() {
        for (relative in ceilings.keys) {
            assertTrue("$relative no longer exists; remove its ceiling", File(sourceRoot, relative).isFile)
        }
    }

    /** `Shell.getShell()` من اللوحة كان يُنشئ `su` ويُظهر نافذة الصلاحية: القراءة السلبية فقط. */
    @Test
    fun `the floating window availability check never creates a su shell`() {
        val floating = code(File(sourceRoot, "core/gamespace/PanelFloating.kt"))
        assertTrue("PanelFloating must not call Shell.getShell()", !floating.contains("Shell.getShell("))
        assertTrue(floating.contains("PrivilegeManager.cachedRootGranted()"))
    }

    /**
     * المواضع التي ما زالت تستدعي صدفة الجذر مباشرةً تمرّ ببوّابة الوضع: في Shizuku/Basic تُرفض بدل أن
     * تعمل لأن الجذر ممنوح بالصدفة. وحذف البوّابة من أحدها يُسقط فصل الأوضاع صامتًا.
     */
    @Test
    fun `root-direct panel paths are gated by the selected execution mode`() {
        val gated = mapOf(
            "core/gamespace/PanelControls.kt" to "fun setBypass",
            "core/gamespace/PanelDevice.kt" to "fun cleanMemory",
            "core/gamespace/PanelFloating.kt" to "private fun rootAvailable",
            "core/gamespace/PanelToggles.kt" to "fun setTouch"
        )
        for ((relative, marker) in gated) {
            val text = code(File(sourceRoot, relative))
            val body = text.substringAfter(marker, missingDelimiterValue = "")
            assertTrue("$relative: $marker not found", body.isNotEmpty())
            assertTrue(
                "$relative: $marker must consult GameRootGate before touching a root shell",
                body.take(600).contains("GameRootGate.allowed()")
            )
        }
    }
}
