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
 * (صلاحيات + سياق SELinux)، لكنها **لا تُثبِت موديولًا عبر مدير الجذر**. فالنتيجة بعد الإقلاع
 * تُقرأ من `ModuleHealthUtil` — وإن لم تظهر الوحدة فالحالة «فشلت» بسببه، لا نجاح مُفترض.
 *
 * **والكتابة staging ثم `mv` لا `rm -rf`** (عطب مقيس في المراجعة ٢٠٢٦-١١-٢٢): كان الأمر يمحو
 * وحدةً قائمة ثم ينسخ فوقها، فيبقى نصف وحدة إذا فشل النسخ. والآن تُبنى نسخة كاملة في
 * `/data/adb/modules/.MaxManager.bootstrap`، وتُتحقَّق، ثم تُنقل **فقط** إذا لم يكن الهدف موجودًا؛
 * فوحدة صالحة (لها `module.prop`) لا تُمحى أبدًا. ومجلّد بلا `module.prop` بقايا محاولة قديمة
 * تُنظَّف قبل البناء، لأنها ليست وحدة يعرفها مدير الجذر.
 *
 * **وحِمل CI لا يحمل APK** (لأنه هو التطبيق نفسه)؛ فتُنسخ نسخة التطبيق المشغَّل
 * (`applicationInfo.sourceDir`) إلى `app/MaxManager.apk`. بلا هذا النسخ لا يستطيع `service.sh`
 * تشغيل الرفيق بـ`app_process`، ولا يرى الخادم قائمة الألعاب. وهذا هو الفرق بين «ملفات مكتوبة»
 * و«وحدة تعمل».
 */
object ModuleBootstrap {

    /** اسم الأصل داخل الـAPK — يكتبه CI في خطوة التصليب قبل إعادة التغليف. */
    const val ASSET = "module_payload.zip"

    /** مجلّد البناء المؤقّت **على نفس القسم**، فيصير النقل إلى الهدف ذرّيًّا. */
    private const val STAGING_DIR = "/data/adb/modules/.MaxManager.bootstrap"

    /** نتيجة محاولة التفعيل: نجحت، أو تعذّرت بسبب مسمّى، أو لم تُنفَّذ لغياب الجذر. */
    sealed interface Report {
        /** كُتبت الملفّات على القرص؛ **ولا تُدَّعى الفعّالية قبل إعادة التشغيل**. */
        data object Written : Report

        /** لا حِمل في هذه الحزمة (بناء لم يمرّ بخطوة CI، أو قناة أخرى). */
        data class MissingPayload(val reason: String) : Report

        /** الجذر لم يُمنح — لا نكتب شيئًا ولا نزعج المستخدم برسالة فشل تقنية. */
        data object NoRoot : Report

        /** سبق أن كتبت وحدةٌ أخرى نفس الهدف — لا يُستبدل تثبيت قائم صامتًا. */
        data object AlreadyInstalled : Report

        /** فشل مقيس: المخرج والخطأ كما قالهما الصدفة، لا «حدث خطأ» مبهم. */
        data class Failed(val detail: String) : Report
    }

    /** هل تحمل هذه الحزمة حِملًا؟ فحص الأصل وحده — بلا جذر وبلا كتابة. */
    fun hasPayload(context: Context): Boolean = runCatching {
        context.assets.open(ASSET).use { it.read() >= 0 }
    }.getOrDefault(false)

    /**
     * اقتباس POSIX آمن: `'` تصبح `'\''`. وكل مسار يُمرَّر إلى الصدفة يُقتبس بها —
     * باسم مجلّد فيه مسافة أو `;` يصبح الأمر أمرًا آخر يُنفَّذ بصلاحية جذر.
     */
    private fun quote(value: String): String = "'" + value.replace("'", "'\\''") + "'"

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
        val config = MaxManagerPaths.MODULE_CONFIG
        // **APK التطبيق المشغَّل:** حِمل CI لا يحمله (كي لا يحمل التطبيق نفسه داخل نفسه)،
        // و`service.sh` يحتاجه لتشغيل الرفيق بـ`app_process`. ونسخه هنا يجعل الوحدة المكتوبة
        // تعمل بعد الإقلاع بلا إنترنت ولا خطوة ثانية.
        val sourceApk = context.applicationInfo?.sourceDir
        if (sourceApk.isNullOrBlank()) return Report.Failed("app-apk-unknown")

        // **لا استبدال لتثبيت قائم:** وجود `module.prop` غير الفارغ يعني وحدة صالحة، فتُرفض
        // المحاولة قبل أن يُمحى أي ملف. أما مجلّد بلا `module.prop` فبقايا محاولة، ويُنظَّف
        // في أوّل أمر بعد هذا الفحص (وبفحصه مرة ثانية هناك، فلا يُمحى ما ظهر بينهما).
        val present = runCatching {
            RootCommands.cmd("test -s ${quote("$target/module.prop")}").exec()
        }.getOrElse { return Report.NoRoot }
        if (present.code == -1) return Report.NoRoot
        if (present.isSuccess) return Report.AlreadyInstalled

        val configDirs = listOf(
            config,
            "$config/debug",
            "$config/API",
            "$config/preload",
            "$config/bypasschgconfig",
            "$config/gamelist",
            "$config/runtime",
        )
        val applist = "$config/gamelist/maxmanagerApplist.json"
        // الخطوة الأخيرة **سلسلة `&&` واحدة**: مكتبة الصدفة تُنفّذ العناصر السابقة كلها وإن
        // فشل أحدها، فرمزان فقط يُقاسان — الأوّل للرفض، والأخير للحقيقة. ولو بقي `touch`
        // الأخير وحده لكان فشل النسخ يُقرأ «نجح» (العطب الذي جاء هذا الترتيب لإزالته).
        // **والعلامة تُكتب في مجلّد البناء قبل النقل:** كل ما بعد `mv` فحصٌ لا تعديل، فإذا فشل
        // فحصٌ لا تبقى وحدة نصف مكتوبة في الهدف. ولو كُتبت العلامة بعده لكان فشل `touch` يترك
        // وحدةً كاملة بلا علامة إقلاع — أي «مفعّلة» وهي لا تعمل.
        val commit = listOf(
            "test -s ${quote("$STAGING_DIR/module.prop")}",
            "test -s ${quote("$STAGING_DIR/app/MaxManager.apk")}",
            "test -x ${quote("$STAGING_DIR/system/bin/sys.maxmanager-service")}",
            "test -f ${quote("$STAGING_DIR/update")}",
            "[ ! -e ${quote(target)} ]",
            "mv ${quote(STAGING_DIR)} ${quote(target)}",
            "test -s ${quote("$target/module.prop")}",
            "test -s ${quote("$target/app/MaxManager.apk")}",
            "test -x ${quote("$target/system/bin/sys.maxmanager-service")}",
            "test -f ${quote("$target/update")}",
        ).joinToString(" && ")

        val commands = arrayOf(
            // ٠) بقايا مجلّد بلا `module.prop` فقط — والفحص المكرَّر يمنع محو وحدة ظهرت للتوّ.
            "[ -s ${quote("$target/module.prop")} ] || rm -rf ${quote(target)}",
            // ١) بقايا محاولة سابقة تُزال — مجلّد البناء يملكه هذا الكائن وحده.
            "rm -rf ${quote(STAGING_DIR)}",
            "mkdir -p ${quote("$STAGING_DIR/system/bin")}",
            "mkdir -p ${quote("$STAGING_DIR/app")}",
            // ٢) النسخ من مجلّد التطبيق (قرأه التطبيق بنفسه) إلى مجلّد البناء.
            "cp -rf ${quote("$staged/.")} ${quote("$STAGING_DIR/")}",
            "cp -f ${quote(sourceApk)} ${quote("$STAGING_DIR/app/MaxManager.apk")}",
            // ٣) الملكية والصلاحيات كما يفرضها مثبّت الوحدة: جذر/جذر، سكربتات 0755، والبقية 0644.
            "chown -R 0:0 ${quote(STAGING_DIR)}",
            "find ${quote(STAGING_DIR)} -type d -exec chmod 0755 {} +",
            "find ${quote(STAGING_DIR)} -type f -name '*.sh' -exec chmod 0755 {} +",
            "find ${quote("$STAGING_DIR/system/bin")} -type f -exec chmod 0755 {} +",
            // **وبقية الملفّات 0644:** ما يستخرجه التطبيق يحمل صلاحية مجلّده المؤقّت (0600 غالبًا)،
            // و`cp -rf` يحفظها. فيقرأها الجذر وحده، ومدير الجذر (تطبيق بلا جذر دائم) يرى وحدةً
            // بلا `module.prop`. وهذا ما يفعله `set_perm_recursive` في المثبّت.
            "find ${quote(STAGING_DIR)} -type f ! -name '*.sh' ! -path ${quote("$STAGING_DIR/system/bin/*")} -exec chmod 0644 {} +",
            // ٤) سياق SELinux للملفّات التنفيذية — بلا هذا يرفض `init` تشغيل الخدمة.
            "chcon -R u:object_r:system_file:s0 ${quote("$STAGING_DIR/system/bin")}",
            // ٥) الإعداد المشترك الذي ينشئه `customize.sh` عادةً — بلا إنشائه يكتب الرفيق في
            //    مسارات غير موجودة. `mkdir -p` و`[ -f ]` لا يمسّان ملفًّا للمستخدم.
            "mkdir -p ${configDirs.joinToString(" ") { quote(it) }}",
            "chmod 0755 ${configDirs.joinToString(" ") { quote(it) }}",
            // ٥.١) **بذرة قائمة التطبيقات من الحِمل نفسه لا `{}`:** `customize.sh` ينسخ
            //      `maxmanagerApplist.json` المشحون إلى الإعداد، و`{}` كان يمحو كل بروفايل
            //      افتراضي في أول تفعيل بلا إنترنت. والنسخة الاحتياطية `{}` تبقى فقط إن غاب
            //      الملفّ من الحِمل، حتى لا يبقى مسار غير موجود.
            "[ -f ${quote("$STAGING_DIR/maxmanagerApplist.json")} ] && cp -f ${quote("$STAGING_DIR/maxmanagerApplist.json")} ${quote(applist)}",
            "[ -f ${quote(applist)} ] || printf '{}\\n' > ${quote(applist)}",
            // ٥.٢) `customize.sh` يكتب `current_modes=0` (MAX AI مطفأ افتراضيًّا). غيابه ليس
            //      «مفعّلًا» بل قيمة غير مقروءة؛ فيُكتب الافتراضي نفسه.
            "[ -f ${quote("$config/API/current_modes")} ] || printf '0\\n' > ${quote("$config/API/current_modes")}",
            // ٥.٣) علامة Mountify التي يضعها المثبّت: الوحدة تحتاج تركيب `system/bin` بنفسها،
            //      فلا يتولّاها Mountify بمسار آخر. وجودها لا يضرّ على Magisk/KSU العاديين.
            "touch ${quote("$STAGING_DIR/skip_mountify")}",
            // ٥.٤) علامة «كُتبت ولم يُقلع بعد» في مجلّد البناء — تُنقل مع الوحدة، فالحالة لا
            //      تُقرأ «مفعّلة» قبل الإقلاع، و`service.sh` يمسحها بعد أن تعمل السكربتات.
            "touch ${quote("$STAGING_DIR/update")}",
            // ٦) البناء والتحقّق والنقل: لا شيء يصل الهدف حتى تنجح السلسلة كلها.
            commit,
        )

        val result = runCatching { RootCommands.cmd(*commands).exec() }
            .getOrElse { return Report.NoRoot }
        // `-1` هي علامة `RootCommands` نفسها على «لم تُنفَّذ» (لا صدفة، أو الوضع تبدّل)،
        // وهي مختلفة عن خروج أمر فشل: الأول لا نكتب معه شيئًا، والثاني نُبلغ بسببه.
        if (result.code == -1) return Report.NoRoot
        if (!result.isSuccess) {
            // مجلّد البناء لا يُترك خلف محاولة فاشلة: المحاولة التالية تبدأ من جديد، ولا يظهر
            // مجلّد نصف وحدة بجوار وحدات مدير الجذر.
            runCatching { RootCommands.cmd("rm -rf ${quote(STAGING_DIR)}").exec() }
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
                    // حماية المسار: مدخل مطلق أو فيه `..` أو `\` يكتب خارج مجلّد الاستخراج
                    // (Zip Slip). والفحص الثاني يقارن المسار القانوني لا النصّ وحده.
                    val name = entry.name
                    val unsafe = name.isBlank() || name.startsWith("/") ||
                        name.contains('\\') || name.split('/').any { it == ".." }
                    if (unsafe) return null
                    val out = File(dir, name)
                    val rootPath = dir.canonicalFile
                    val outPath = out.canonicalFile
                    if (outPath != rootPath && !outPath.path.startsWith(rootPath.path + File.separator)) {
                        return null
                    }
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
