/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.contract

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * حرس **مستودع التحكّم المشترك** — أقفال العطب الذي أسقط الوحدة كلها على جهاز حقيقيّ.
 *
 * **والعطب مقيس من سجلّ المستخدم، لا مُتخيَّل (تكملة ٢٢٧):**
 *
 * ```
 * 17:36:31.885 LockSettingsService: Not unlocking CE storage for user 0 yet because user is secured
 * 17:36:33.535 ContextImpl: Failed to ensure /data/user/0/nd.max/files: mkdir failed: ENOENT
 * 17:36:33.595 AndroidRuntime: java.lang.IllegalStateException: cannot-create-shared-control-directory
 * 17:36:48.555 LockSettingsService: Unlocked CE storage for secured user 0
 * 17:38:35.291 MaxManager: EVENT=JAVA_COMPANION_TIMEOUT checks=120 action=exit
 * ```
 *
 * الرفيق الجذريّ كان يبني المستودع في **التخزين المحميّ باعتماد المستخدم** (`/data/user/0/...`)، وهو
 * لا يُفتح قبل أوّل فتحٍ للشاشة؛ فمات في ثوانٍ من الإقلاع، وانتظر الخادم ١٢٠ ثانية ثم أغلق الوحدة
 * برسالة «Java companion daemon crashed or failed to start» — وكل ما بعدها فشل. **وهذا هو تفسير
 * «تعمل مرّة ولا تعمل أخرى»**: العطب في توقيت الإقلاع مقابل قفل الشاشة، لا في العتاد ولا في الإعداد.
 *
 * **ولماذا حرسٌ على النصّ لا اختبارٌ سلوكيّ:** القرار نفسه (`createDeviceProtectedStorageContext`)
 * يحتاج `android.jar` وسياقًا لا يوجدان في هذه البيئة، والسلوك يحتاج **إقلاع جهاز** — والمقيس هنا
 * هو **البنية التي تُنتج العطب**: أيّ مجلد يُسلَّم للمستودع، ومن يستطيع أن يرمي من `main`، وهل
 * يبقى الخادم ميتًا إذا تأخّر الرفيق أو تنفلت محاولة استعادته. وهذا يُقاس في ثوانٍ بلا جهاز.
 *
 * **والحدّ المُعلن:** هذا الحرس يقيس **الشكل** لا الزمن: أن المستودع يُبنى في تخزين المحميّ بالجهاز
 * ومن الموضع الواحد، وأن الرفيق لا يموت من استثناء غير مُلتقَط، وأن السكربت يبدأ الخادم عند جاهزية
 * الحزمة ويقصر استعادة الرفيق المتأخّر على عدد معلوم من المحاولات.
 * وما يحتاج جهازًا يبقى يحتاج جهازًا: أن `mkdirs` تنجح فعلًا قبل الفتح، وأن الإقلاع يعمل على ROM بعينه.
 */
class SharedControlPlaneContractTest {

    private val mainSources: List<File> by lazy { ContractFixtures.mainSourceFiles() }

    /** الكود دون تعليقاته: تُقاس البنية ولا تُقاس صياغة الشرح. */
    private fun codeOf(file: File): String = file.readText()
        .replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")
        .replace(Regex("//[^\\n]*"), "")

    private fun source(relative: String): String {
        val file = mainSources.firstOrNull { it.path.replace('\\', '/').endsWith(relative) }
        assumeTrue("$relative not reachable; guard not evaluated", file != null)
        return codeOf(file!!)
    }

    /**
     * السكربت **المقيس**، ويُسمّى مساره في كل رسالة فشل: حكمٌ لا يقول أيّ ملف قرأه لا يُتّهم به أحد.
     *
     * ويُوجد بالصعود من مجلد التنفيذ مثل بقية الحرس، لا من جذر المستودع — فحين يُقاس الاصطناع
     * (شجرة منسوخة في `/tmp`) يُقاس **الملف المنسوخ** لا ملفّ المستودع.
     */
    private val scriptPath: File by lazy {
        generateSequence(File("").absoluteFile) { it.parentFile }
            .map { File(it, "mainfiles/service.sh") }
            .firstOrNull { it.isFile }
            ?: File("mainfiles/service.sh")
    }

    private fun script(): String {
        assumeTrue("mainfiles/service.sh not reachable; guard not evaluated", scriptPath.isFile)
        return scriptPath.readText()
    }

    /** أيّ ملف قُرئ فعلًا — يُذكر مرّةً في الرسائل لا يُخفي. */
    private val scriptLabel: String get() = scriptPath.absolutePath

    // ── ١) القرار محصورٌ في موضعٍ واحد، وهو المحميّ بالجهاز ───────────────────

    @Test
    fun `the shared store lives in device-protected storage, decided in one place`() {
        val decision = source("core/hardware/SharedControlStorage.kt")
        assertTrue(
            "قرار التخزين يجب أن يكون محميًّا بالجهاز: بدونه لا يُبنى المستودع قبل فتح الشاشة",
            decision.contains("createDeviceProtectedStorageContext()"),
        )
        assertTrue(
            "ويُسلَّم من دالّة واحدة تُستدعى من العمليّتين (لا نسختين تتفرّقان)",
            Regex("fun filesDir\\(context: Context\\): File").containsMatchIn(decision),
        )
    }

    @Test
    fun `both processes take the directory from that one place`() {
        listOf("MaxManagerApplication.kt", "AppMonitor.kt").forEach { name ->
            val code = source(name)
            assertTrue(
                "$name يبني المستودع من تخزينٍ آخر غير تخزين الرفيق — فينقسم السجلّ إلى مستودعين",
                code.contains("SharedControlStorage.filesDir("),
            )
        }
    }

    /**
     * **وعطب التخزين الخاطئ يُقاس بشكله:** `configure(...)` تُسلَّم لها خاصيّة `filesDir` العائدة
     * إلى سياق (`ctx.filesDir` · `controlContext.filesDir`) — وهو التخزين المحميّ بالاعتماد.
     * والمُسلَّم الصحيح متغيّرٌ قادم من `SharedControlStorage`، فلا نقطة فيه.
     */
    @Test
    fun `no call site hands a plain context filesDir to the shared store`() {
        val offenders = mainSources.filter { file ->
            val code = codeOf(file)
            Regex("(SharedHardwareOwnershipStore|ManualControlLocks)\\.configure\\(\\s*[A-Za-z_][A-Za-z0-9_]*\\.filesDir")
                .containsMatchIn(code)
        }.map { it.name }

        assertTrue(
            "مستودع التحكّم يُبنى في تخزين سياقٍ عاديّ (المحميّ بالاعتماد، لا يُفتح قبل فتح الشاشة): $offenders",
            offenders.isEmpty(),
        )
    }

    // ── ٢) والرفيق لا يموت من استثناء غير مُلتقَط ─────────────────────────────

    @Test
    fun `the companion cannot die from an uncaught store-configure failure`() {
        val companion = source("AppMonitor.kt")

        assertTrue(
            "`main` يجب أن يحرس التهيئة ويخرج منها قبل حمل القفل",
            companion.contains("if (!configureSharedControlPlane(controlContext)) return"),
        )

        val body = Regex("private fun configureSharedControlPlane\\(.*?\n    }", RegexOption.DOT_MATCHES_ALL)
            .find(companion)?.value
        assumeTrue("configureSharedControlPlane not reachable; guard not evaluated", body != null)

        assertTrue(
            "التهيئة داخل `runCatching` — ورميُها من `main` كان يقتل الرفيق بلا سطر سبب",
            body!!.contains("runCatching") &&
                body.contains("SharedHardwareOwnershipStore.configure("),
        )
        assertTrue(
            "وبحدّ محاولات لا حلقة لا نهائية (الخادم ينتظر ١٢٠ ثانية فقط)",
            body.contains("SHARED_CONTROL_CONFIGURE_ATTEMPTS"),
        )
        assertTrue(
            "وعند الفشل يُعلن السبب بالاسم في سجلّ الرفيق بدل أن يسقط صامتًا",
            companion.contains("EVENT=SHARED_CONTROL_PLANE_FAILED"),
        )
    }

    @Test
    fun `the app announces an unavailable store instead of crashing at launch`() {
        val application = source("MaxManagerApplication.kt")
        assertTrue(
            "فشل تهيئة المستودع في `onCreate` كان يُسقط التطبيق عند الإقلاع بلا سبب",
            application.contains("runCatching {") && application.contains("SharedHardwareOwnershipStore.configure("),
        )
        assertTrue(
            "ويُعلن في تشخيص التطبيق ليراه المستخدم في شاشة التشخيص",
            application.contains("DiagnosticCenter.record("),
        )
    }

    // ── ٣) الخادم لا يبقى ميتًا إذا تأخّر الرفيق، والاستعادة المتأخرة محدودة ──

    @Test
    fun `the module script starts the daemon and bounds late companion recovery`() {
        val script = script()
        val where = scriptLabel

        assertTrue(
            "اسم الرفيق يُعلن مرّة، والقياس عليه بالاسم لا بالتخمين [$where]",
            script.contains("readonly COMPANION_NAME=\"sys.maxmanager-appmonitoring\""),
        )
        assertTrue(
            "وحضوره يُقاس بأداة المنصّة",
            script.contains("toybox pidof \"\$COMPANION_NAME\""),
        )
        assertTrue(
            "وغياب أداة القياس لا يُسقط الوحدة: يُقال إنه لم يُقس ويُعاد السلوك السابق",
            script.contains("pidof unavailable; companion liveness not measured"),
        )
        assertTrue(
            "وفشل المحاولتين لا يقتل الخادم: يُعلن الانتقال إلى الاستعادة المتأخرة",
            script.contains("java companion not ready after retry; starting daemon anyway"),
        )
        assertTrue(
            "والاستعادة المتأخرة محدودة بعشرين محاولة، لا حلقة خفيّة دائمة",
            script.contains("while [ \"\$n\" -lt 20 ]; do"),
        )
        assertTrue(
            "وتتوقف الاستعادة فور تعطيل الوحدة أو طلب إزالتها",
            script.contains("[ -f \"\$MODDIR/disable\" ] && exit 0") &&
                script.contains("[ -f \"\$MODDIR/remove\" ] && exit 0"),
        )
        assertTrue(
            "الخادم يبدأ متى صارت الحزمة جاهزة، ولو كان الرفيق ما زال في الاستعادة المتأخرة [$where]",
            script.contains("if [ \"\$PACKAGE_READY\" -eq 1 ]; then\n    sleep 1 && exec \"\$BIN_SVC\" --run"),
        )
        // **وسطر تنفيذ الخادم واحد لا اثنان:** الاستعادة المتأخرة تخصّ الرفيق فقط؛ فلا يجوز أن
        // تُنشئ خادمًا ثانيًا بالتوازي. والعدّ على سطور التنفيذ لا التعليقات.
        val execLines = script.lines()
            .filterNot { it.trimStart().startsWith("#") }
            .filter { it.contains("--run") }
        assertEquals(
            "سطور تنفيذ الخادم يجب أن تكون واحدة لا اثنتين [$where]",
            1,
            execLines.size,
        )
    }
}
