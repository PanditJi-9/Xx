/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential. See LICENSE at the repository root.
 */
package nd.max.core.module

import android.content.Context
import nd.max.MaxManagerPaths
import nd.max.core.privilege.RootCommands
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

/**
 * تفعيل الوحدة **بلا إنترنت** من حِمل مضمَّن في التطبيق نفسه.
 *
 * **لماذا هو في `core/` لا في `ui/`:** الخطّ الأحمر في هذا المستودع يمنع كتابة الجذر من
 * مجلّد الواجهة (ADR-11). فالشاشة تطلب، وهذا الكائن يكتب — والكتابة كلها عبر [RootCommands]
 * وحده، لا صدفةً ثانية.
 *
 * **والحِمل ليس نسخةً ثانية من الوحدة:** هو `.github/scripts/module_payload.sh` نفسه الذي
 * يبنيه CI من `mainfiles/` ومخارج البناء الأصلية (£٢٫٩ م.ب: السكربتات + `libs/` +
 * `system/bin`)، بلا `app/MaxManager.apk` — لأن التطبيق نفسه هو الذي يشتغل الآن.
 *
 * **وحدوده تُقال لا تُخفى:** الكتابة إلى `/data/adb/modules` تحاكي ما يفعله مثبّت Magisk
 * (صلاحيات + سياق SELinux)، لكنها **لا تُثبِت موديولًا عبر مدير الجذر** ولا تتحقّق من
 * `customize.sh`. فالنتيجة بعد الإقلاع تُقرأ من `ModuleHealthUtil` — وإن لم تظهر الوحدة
 * فالحالة «فشلت» بسببه، لا نجاح مُفترض.
 */
object ModuleBootstrap {

    /** اسم الأصل داخل الـAPK — يكتبه CI في خطوة التصليب قبل إعادة التغليف. */
    const val ASSET = "module_payload.zip"

    /** نتيجة محاولة التفعيل — ثلاث حالات لا رابعة: نجح، أو تعذّر بسببه، أو لم يُنفَّذ. */
    sealed interface Report {
        /** كُتبت الملفّات على القرص؛ **ولا تُدَّعى الفعّالية قبل إعادة التشغيل**. */
        data object Written : Report

        /** لا حِمل في هذه الحزمة (بناء لم يمرّ بخطوة CI، أو قناة أخرى). */
        data class MissingPayload(val reason: String) : Report

        /** الجذر لم يُمنح — لا نكتب شيئًا ولا نزعج المستخدم برسالة فشل تقنية. */
        data object NoRoot : Report

        /** فشل مقيس: المخرج والخطأ كما قالهما الصدفة، لا «حدث خطأ» مبهم. */
        data class Failed(val detail: String) : Report
    }

    /** هل تحمل هذه الحزمة حِملًا؟ فحص الأصل وحده — بلا جذر وبلا كتابة. */
    fun hasPayload(context: Context): Boolean = runCatching {
        context.assets.open(ASSET).use { it.read() >= 0 }
    }.getOrDefault(false)

    /**
     * يستخرج الحِمل ثم يكتبه بصلاحية جذر إلى [MaxManagerPaths.MODULE_DIR].
     *
     * **يجب أن يُنادى خارج الخيط الرئيسي** (`RootCommands.exec()` تنتظر صفّ `su`).
     */
    fun install(context: Context): Report {
        val extracted = runCatching { extract(context) }.getOrElse { error ->
            return Report.MissingPayload(error.message ?: "read-failed")
        } ?: return Report.MissingPayload("asset-${ASSET}-missing")

        val target = MaxManagerPaths.MODULE_DIR
        val staged = extracted.absolutePath
        val commands = arrayOf(
            // ١) الوحدة القديمة تُزال كاملة: بقايا `system/bin` من نسخة أقدم تُشغَّل قبل الجديدة.
            "rm -rf ${quote(target)}",
            "mkdir -p ${quote("$target/system/bin")}",
            // ٢) النسخ من مجلّد التطبيق (قرأه التطبيق بنفسه) إلى مسار الوحدة.
            "cp -rf ${quote("$staged/.")} ${quote("$target/")}",
            // ٣) الملكية والصلاحيات كما يفرضها مثبّت الوحدة: جذر/جذر، سكربتات 0755، والبقية 0644.
            "chown -R 0:0 ${quote(target)}",
            "find ${quote(target)} -type f -name '*.sh' -exec chmod 0755 {} +",
            "find ${quote("$target/system/bin")} -type f -exec chmod 0755 {} +",
            // ٤) سياق SELinux للملفّات التنفيذية — بلا هذا يرفض `init` تشغيل الخدمة.
            "chcon -R u:object_r:system_file:s0 ${quote("$target/system/bin")}",
            // ٥) قراءة مرتجعة: لا «نجح» بلا دليل من الملفّ نفسه.
            "test -s ${quote("$target/module.prop")}",
        )

        val result = runCatching { RootCommands.cmd(*commands).exec() }
            .getOrElse { return Report.NoRoot }
        // `-1` هي علامة `RootCommands` نفسها على «لم تُنفَّذ» (لا صدفة، أو الوضع تبدّل)،
        // وهي مختلفة عن خروج أمر فشل: الأول لا نكتب معه شيئًا، والثاني نُبلغ بسببه.
        if (result.code == -1) return Report.NoRoot
        if (!result.isSuccess) {
            val detail = (result.err + result.out).firstOrNull { it.isNotBlank() } ?: "exit-${result.code}"
            return Report.Failed(detail)
        }
        return Report.Written
    }

    /** يستخرج الحِمل إلى `cacheDir` (بلا جذر) ويعيد مجلّد الاستخراج. */
    private fun extract(context: Context): File? {
        val dir = File(context.cacheDir, "module_payload").apply {
            deleteRecursively()
            mkdirs()
        }
        context.assets.open(ASSET).use { raw ->
            ZipInputStream(raw.buffered()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    // حماية المسار: مدخل فيه `..` يكتب خارج مجلّد الاستخراج (Zip Slip).
                    val name = entry.name
                    if (name.contains("..")) return null
                    val out = File(dir, name)
                    if (entry.isDirectory) {
                        out.mkdirs()
                    } else {
                        out.parentFile?.mkdirs()
                        FileOutputStream(out).use { zip.copyTo(it) }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }
        return dir.takeIf { File(it, "module.prop").isFile }
    }
}
