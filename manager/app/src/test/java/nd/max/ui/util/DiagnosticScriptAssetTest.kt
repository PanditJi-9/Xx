/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.util

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * يحرس عقد السكربت التشخيصيّ: نصّه في أصل مختوم، لا في DEX.
 *
 * ## لماذا هذا القياس (لا الاختبار وحده)
 *
 * نقل السكربت من `LogUtil.kt` إلى `assets-internal/scripts/diagnostics.sh` يفصل شيئين
 * كانا متّصلين: **محتوى السكربت** (يبقى كما هو) و**مكانه في الحزمة** (يخرج من جدول نصوص
 * DEX إلى أصل يُختم بمفتاح البصمة). والفصل نفسه هو ما يُقاس هنا: كل قيمة يحقنها Kotlin يجب أن
 * يستهلكها الأصل بصيغة shell صحيحة، وكل مرجع صغير الحروف يجب أن يعرّفه الأصل محليًّا.
 *
 * ## وحدّه المُعلَن
 *
 * لا يُثبت أن السكربت **يعمل** على جهاز: هذا يحتاج جهازًا. الذي يُثبته: الأصل موجود،
 * والمتغيّرات التي تُمرَّر من Kotlin كلها مستهلكة فيه، ولا مرجع shell محلي بلا تعريف، وصياغة
 * الصدفة صحيحة (`sh -n`) على مُشغّل CI.
 */
class DiagnosticScriptAssetTest {

    private fun asset(): File = listOf(
        "src/main/assets-internal",
        "manager/app/src/main/assets-internal",
    ).map(::File).firstOrNull { it.isDirectory }
        ?.resolve("scripts/diagnostics.sh")
        ?: File("src/main/assets-internal/scripts/diagnostics.sh")

    /** المتغيّرات التي تُبنى في `dumpDiagnosticLogs` قبل نصّ السكربت — عقد الاثنين. */
    private val supplied = listOf(
        "BLUEPRINT_FILE", "DIAG_FILE", "OUT_NAME", "TARGET_PATH", "SAVE_TO_DOWNLOADS",
        "APP_UID", "SERVICE_BIN", "DEVICE_NAME", "CHIPSET_NAME", "APP_VERSION", "BUILD_MARK",
    )

    @Test
    fun `script lives in sealed assets`() {
        val file = asset()
        assertTrue("الأصل غير موجود: ${file.absolutePath}", file.isFile)
        assertTrue("السكربت أقصر من أن يكون التشخيص الكامل", file.length() > 4_000)
    }

    @Test
    fun `every variable supplied by kotlin is consumed by the script`() {
        val text = asset().readText()
        // يقبل الصيغتين الصحيحتين في الصدفة: `$NAME` و`${NAME:-default}`/`${NAME##*/}`.
        // البحث الحرفي القديم لم يرَ `OUT_NAME` بعد أن صارت قراءته محمية من مكوّنات المسار.
        val missing = supplied.filter { name ->
            val escaped = Regex.escape(name)
            Regex("""\$(?:$escaped\b|\{$escaped(?:\}|[^A-Za-z0-9_]))""").find(text) == null
        }
        assertEquals("متغيّرات تُمرَّر ولا يستهلكها السكربت: $missing", emptyList<String>(), missing)
    }

    @Test
    fun `lowercase shell references are declared inside the asset`() {
        val text = asset().readText()
        // متغيّرات Kotlin الممرّرة أعلاه كبيرة الحروف. وأي مرجع صغير الحروف يجب أن يكون محلّيًّا
        // عرّفه السكربت نفسه (حلقة أو إسناد)؛ وبذلك نمسك بقايا نقل Kotlin بلا تجميد قائمة أسماء
        // حلقات تتقادم كلما أضيف جمع تشخيصي جديد.
        val references = (
            Regex("""\$\{([a-z][A-Za-z0-9_]*)""").findAll(text) +
                Regex("""\$([a-z][A-Za-z0-9_]*)""").findAll(text)
            ).map { it.groupValues[1] }.toSet()
        val declared = (
            Regex("""(?m)^\s*([a-z][A-Za-z0-9_]*)=""").findAll(text) +
                Regex("""(?m)^\s*for\s+([a-z][A-Za-z0-9_]*)\s+in\b""").findAll(text)
            ).map { it.groupValues[1] }.toSet()
        assertEquals(
            "مراجع shell صغيرة الحروف بلا تعريف محلّي داخل الأصل",
            emptySet<String>(),
            references - declared,
        )
    }

    @Test
    fun `shell syntax is valid`() {
        val file = asset()
        val process = try {
            ProcessBuilder("sh", "-n", file.absolutePath).redirectErrorStream(true).start()
        } catch (missing: java.io.IOException) {
            assumeTrue("لا مُشغّل sh على هذا المضيف ⇒ القياس غير مُتحقَّق", false)
            return
        }
        val output = process.inputStream.bufferedReader().readText()
        val code = process.waitFor()
        assertEquals("sh -n رفض السكربت: $output", 0, code)
    }
}
