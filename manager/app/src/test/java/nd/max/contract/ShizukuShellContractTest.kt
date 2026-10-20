/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.contract

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * حرس عقد تنفيذ Shizuku (`AR-20`) — **السلك** الذي لا تكشفه ترجمة ولا اختبار سلوك بلا جهاز.
 *
 * لماذا حرس بنيويّ لا اختبار سلوك: خدمة المستخدم تُنشأ في عملية يديرها خادم Shizuku، وربطها
 * لا يعمل على JVM. فالمقيس هنا ما يمكن قياسه بصدق: أن رقم معاملة `destroy` الذي يحفظه الخادم
 * لم يتغيّر، وأن الخدمة ما زالت `IShizukuShell.Stub` ومنفّذةً للعملية، وأن الجسر يربط عبر
 * الواجهة **العامة** ويفكّ الربط، وأن R8 مُخبَر بالإبقاء، وأن مسار اللوحة لم يعد يتجاوز
 * الطبقات. وأي انحراف في واحد منها يكسر التنفيذ على الجهاز — فيسقط هنا في ثانية بدلًا من ذلك.
 *
 * وحدّه المعلن: هذا لا يُثبت أن الربط **ينجح** على هاتف؛ ذلك يحتاج جهازًا، ولم يُدَّع.
 */
class ShizukuShellContractTest {

    private val repoRoot: File get() = ContractFixtures.repoRoot

    private fun source(relative: String): String {
        val file = File(repoRoot, relative)
        assumeTrue("$relative not reachable; guard not evaluated", file.isFile)
        return file.readText()
    }

    /** نصّ بلا تعليقات: السطر الذي يشرح عملية ليس عملية، وقراءته دعوى كاذبة. */
    private fun stripComments(text: String): String = text
        .replace(Regex("(?s)/\\*.*?\\*/"), " ")
        .lines()
        .filterNot { it.trimStart().startsWith("//") }
        .joinToString("\n")

    private fun codeOf(relative: String): String = stripComments(source(relative))

    private fun aidlDeclarations(): List<String> =
        stripComments(source("manager/app/src/main/aidl/nd/max/core/privilege/IShizukuShell.aidl"))
            .lineSequence()
            .map { it.trim() }
            .filter { it.endsWith(";") && it.contains("(") && it.contains(")") }
            .toList()

    @Test
    fun `the interface declares exactly destroy and exec with their frozen transaction codes`() {
        assertEquals(
            "the Shizuku user-service wire changed; the server knows destroy at a fixed code",
            listOf(
                "void destroy() = 16777114;",
                "String[] exec(String command) = 1;",
            ),
            aidlDeclarations(),
        )
    }

    @Test
    fun `the service is the aidl stub with both operations and bounded execution`() {
        val body = codeOf("manager/app/src/main/java/nd/max/core/privilege/ShizukuShellService.kt")
        assertTrue("the service must extend the generated stub", body.contains(": IShizukuShell.Stub()"))
        assertTrue("exec is the whole point of the service", body.contains("override fun exec("))
        assertTrue("Shizuku calls destroy by its reserved code", body.contains("override fun destroy()"))
        assertTrue("a shell must be launched explicitly", body.contains("ProcessBuilder("))
        assertTrue(
            "stderr must be merged so one reader cannot deadlock on a full pipe",
            body.contains("redirectErrorStream(true)"),
        )
        assertTrue(
            "a hanging command must be killed, not waited on forever",
            body.contains("destroyForcibly"),
        )
        assertTrue(
            "output must be declared truncated, not silently cut",
            body.contains("ShizukuExecRules.TRUNCATION_MARK"),
        )
        assertFalse(
            "the service must never escalate to root on its own",
            body.contains("\"su\""),
        )
    }

    @Test
    fun `the gateway binds and unbinds the public user service`() {
        val body = codeOf("manager/app/src/main/java/nd/max/core/privilege/ShizukuGateway.kt")
        assertTrue("binding must use the public API, not a hidden process", body.contains("Shizuku.bindUserService("))
        assertTrue("the service must be removable again", body.contains("Shizuku.unbindUserService("))
        assertTrue("the binder must be unwrapped through the stub", body.contains("IShizukuShell.Stub.asInterface("))
        assertTrue(
            "the service must not outlive the app (daemon = false)",
            body.contains(".daemon(false)"),
        )
        assertTrue("the service process needs a declared suffix", body.contains(".processNameSuffix("))
    }

    @Test
    fun `r8 is told to keep the reflectively created service class`() {
        val rules = source("manager/app/proguard-rules.pro")
        assertTrue(
            "without a keep rule R8 may remove the no-arg constructor the server needs",
            rules.contains("-keep class nd.max.core.privilege.ShizukuShellService"),
        )
    }

    @Test
    fun `the application installs the context the binding needs`() {
        val app = codeOf("manager/app/src/main/java/nd/max/MaxManagerApplication.kt")
        assertTrue(
            "the gateway cannot build ComponentName without an installed application context",
            app.contains("ShizukuGateway.install(this)"),
        )
    }

    @Test
    fun `the shizuku provider stays declared for the bridge`() {
        val manifest = source("manager/app/src/main/AndroidManifest.xml")
        assertTrue(
            "Shizuku requires its provider; removing it silently disables the bridge",
            manifest.contains("rikka.shizuku.ShizukuProvider"),
        )
    }

    @Test
    fun `panel writes go through the one privileged lane, not a bare shell`() {
        val body = codeOf("manager/app/src/main/java/nd/max/core/gamespace/PanelToggles.kt")
        assertTrue(
            "the panel must choose its lane explicitly",
            body.contains("PrivilegedShell.exec("),
        )
        assertFalse(
            "a bare Shell.cmd on these toggles silently runs as the app uid when root is absent",
            body.contains("Shell.cmd(\"svc wifi") || body.contains("Shell.cmd(\"svc data"),
        )
        assertFalse(
            "the DND write must also go through the declared lane",
            body.contains("Shell.cmd(\"settings put global zen_mode"),
        )
    }
}
