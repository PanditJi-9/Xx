/*
 * Copyright (C) 2026-2027 Zexshia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * تنفيذ مدير الملفات — **لا يقرّر شيئًا**، يُنفّذ ما قرّره [FileSystemModel].
 *
 * القواعد الثابتة هنا:
 *
 * 1. **لا تحليل لمخرج `ls` للحصول على البيانات.** `ls -l` نصّ بشري يتغيّر شكله بين
 *    toybox وcoreutils، وتحليله بالمسافات هو أصل أكثر مدراء الملفات هشاشةً. فنقرأ
 *    `stat -c` بحقول مفصولة بـ`tab` ومع الاسم **أخيرًا**.
 * 2. **لكل مسار معنى صريح**: كل قيمة تُمرَّر إلى shell تُقتبس بـ[RootShell.quote]،
 *    وكل المسارات مُطبَّعة من الجذر — فلا يبدأ أي منها بـ`-` فيُقرأ كخيار.
 * 3. **لا «نجاح» بلا إثبات** أين يمكن الإثبات: بعد النسخ/النقل/الإنشاء/إعادة التسمية
 *    نتحقّق من وجود الهدف، و`verified` تُعلن النتيجة لا تُفترض.
 * 4. **لا كتابة عتاد من هنا.** هذا مدير ملفات لا محرّك أداء: لا يكتب في `sysfs`، ولا
 *    يمرّ بالـarbiter، ولا يلمس مقابض التحكّم (ADR-11).
 */
package nd.max.ui.util

import java.io.File

/** نتيجة عملية: هل نُفِّذت، وهل **أثبتنا** أثرها. */
data class FileOpOutcome(val executed: Boolean, val verified: Boolean) {
    val ok: Boolean get() = executed && verified
}

/** نتيجة معاينة نصّية. الأربع حالات مختلفة، فلا تُدمج في «فشل». */
sealed interface TextPreview {
    data class Ready(val content: String, val truncated: Boolean) : TextPreview
    data object Binary : TextPreview
    data object TooLarge : TextPreview
    data object Unreadable : TextPreview
}

object FileSystemEngine {

    /**
     * أكبر ملف نعرضه كنصّ. الحدّ **معلن للمستخدم** لا سقف خفيّ: ملف أكبر منه لا
     * يُفتح نصًّا في هذه الشاشة، ويُقال ذلك بدل أن تُعرض نصفه ويظنّ المستخدم أنه كامل.
     */
    const val MAX_PREVIEW_BYTES: Long = 512 * 1024

    private enum class Probe { Ok, Missing, NotDirectory, NotAccessible, ShellDown }

    fun list(rawPath: String): DirectoryListing {
        val path = FileBrowser.normalize(rawPath)
        when (probe(path)) {
            Probe.Missing -> return DirectoryListing.Unreadable(path, ListingFailure.NotFound)
            Probe.NotDirectory -> return DirectoryListing.Unreadable(path, ListingFailure.NotADirectory)
            Probe.NotAccessible -> return DirectoryListing.Unreadable(path, ListingFailure.PermissionDenied)
            Probe.ShellDown -> return DirectoryListing.Unreadable(path, ListingFailure.ShellUnavailable)
            Probe.Ok -> Unit
        }

        val statLines = RootShell.run(statLoop(path))
        if (statLines != null) {
            val parsed = FileStatParser.parse(statLines, path)
            return DirectoryListing.Entries(path, parsed.entries, parsed.skipped, attributesAvailable = true)
        }

        // `stat` غير متاح أو مرفوض: ننزل إلى **الأسماء وحدها** ونُعلن أن الصفات غير مقروءة،
        // بدل أن نُعلن مجلدًا فارغًا — وهو كذب يُفقد المستخدم ثقته فورًا.
        val names = RootShell.run("ls -1A ${RootShell.quote(path)}")
            ?: return DirectoryListing.Unreadable(path, ListingFailure.ShellUnavailable)
        return DirectoryListing.Entries(
            path = path,
            entries = FileStatParser.degraded(names, path),
            skippedLines = 0,
            attributesAvailable = false,
        )
    }

    /**
     * حلقة `stat` واحدة لكل مدخلات المجلد.
     *
     * الأنماط الثلاثة تغطّي: العاديّ · المخفيّ (`.[!.]*` يستثني `.` و`..`) · والأسماء
     * الشاذّة التي تبدأ بنقطتين. وعند عدم المطابقة يبقى النمط حرفيًّا فيُسقطه شرط الوجود،
     * بدل أن يُنتج صفًّا مزيّفًا باسم `*`.
     */
    private fun statLoop(path: String): String {
        val dir = RootShell.quote(path)
        val format = RootShell.quote(FileStatParser.FORMAT)
        return "for f in $dir/* $dir/.[!.]* $dir/..?*; " +
            "do if [ -e \"\$f\" ] || [ -L \"\$f\" ]; then stat -c $format \"\$f\"; fi; done"
    }

    private fun probe(path: String): Probe {
        val quoted = RootShell.quote(path)
        val exists = RootShell.run("test -e $quoted")
        if (exists == null) {
            // الفشل قد يكون «المسار غير موجود» أو «لا shell جذر». نفرّق بـ`File`:
            // ما نراه بأنفسنا موجود، فالمشكلة في الجذر لا في المسار.
            return if (File(path).exists()) Probe.ShellDown else Probe.Missing
        }
        val isDirectory = RootShell.run("test -d $quoted")
            ?: return Probe.ShellDown
        val accessible = RootShell.run("test -r $quoted && test -x $quoted")
        if (accessible == null) return Probe.NotAccessible
        return Probe.Ok
    }

    /** معاينة نصّية محدودة. الثنائي يُكتشف من محتواه لا من امتداده. */
    fun preview(rawPath: String, maxBytes: Long = MAX_PREVIEW_BYTES): TextPreview {
        val path = FileBrowser.normalize(rawPath)
        val sizeLine = RootShell.run("stat -c %s ${RootShell.quote(path)}")
            ?.firstOrNull()?.trim()?.toLongOrNull()
        if (sizeLine != null && sizeLine > maxBytes) return TextPreview.TooLarge

        val lines = RootShell.run("head -c $maxBytes ${RootShell.quote(path)}")
            ?: return TextPreview.Unreadable
        val text = lines.joinToString("\n")
        if (text.isBlank()) return TextPreview.Ready("", truncated = false)
        if (isBinary(text)) return TextPreview.Binary
        val truncated = sizeLine != null && sizeLine > maxBytes
        return TextPreview.Ready(text, truncated)
    }

    /**
     * كشف الثنائي: وجود محرف تحكّم لا يظهر في نصّ (ما عدا `\t` و`\n` و`\r`).
     * وهي إشارة محافظة: تُنتج «ثنائي» ولا تُنتج «نصّ» لملف ثنائي.
     */
    private fun isBinary(text: String): Boolean = text.any { it.code < 0x09 || (it.code in 0x0E..0x1F) }

    fun copy(
        sources: List<String>,
        destination: String,
        renamed: Map<String, String> = emptyMap(),
    ): FileOpOutcome = transfer(sources, destination, move = false, renamed = renamed)

    fun move(
        sources: List<String>,
        destination: String,
        renamed: Map<String, String> = emptyMap(),
    ): FileOpOutcome = transfer(sources, destination, move = true, renamed = renamed)

    /**
     * نقل/نسخ مجموعة إلى مجلد، مع اسم بديل لكل مصدر حُلّ تعارضه.
     *
     * والهدف يُكتب **صراحةً لكل عنصر** لا `dest/` مجرّدة: مع حلّ التعارض صار اسم العنصر
     * في الوجهة قرارًا ([FileTargets])، وتمريره إلى `cp` هو وحده الذي يجعل ما وعدت به
     * الشاشة هو ما يقع على القرص.
     */
    private fun transfer(
        sources: List<String>,
        destination: String,
        move: Boolean,
        renamed: Map<String, String> = emptyMap(),
    ): FileOpOutcome {
        if (sources.isEmpty()) return FileOpOutcome(false, false)
        val dir = FileBrowser.normalize(destination)
        val command = if (move) "mv -f" else "cp -a"
        var executed = true
        for (source in sources) {
            val target = FileTargets.destinationFor(source, dir, renamed)
            executed = executed && RootShell.run(
                "$command ${RootShell.quote(source)} ${RootShell.quote(target)}"
            ) != null
        }
        // الإثبات: كل مصدر يجب أن يكون له مقابل في الهدف، **بالاسم الذي وُعد به**.
        val verified = executed && sources.all { source ->
            RootShell.run(
                "test -e ${RootShell.quote(FileTargets.destinationFor(source, dir, renamed))}"
            ) != null
        }
        return FileOpOutcome(executed, verified)
    }

    /**
     * كتابة نصّ في ملف — «حفظ» المحرّر الداخلي.
     *
     * والمحتوى يمرّ عبر `base64` لا عبر heredoc داخل أمر: النصّ المحفوظ قد يحوي أي
     * محرف (سطر يبدأ بـ`EOF`، أو `$`، أو علامة اقتباس)، وقاعدة المستودع أن كل قيمة تأتي
     * من خارجنا تُقتبس — وbase64 هو الاقتباس الوحيد الذي لا يفهمه shell أصلًا.
     *
     * والإثبات **مقاس**: الحجم على القرص يُقرأ بعد الكتابة ويُقارن بحجم النصّ بالبايت،
     * فلا يُعلَن حفظ لم يقع.
     */
    fun writeText(rawPath: String, text: String): FileOpOutcome {
        val path = FileBrowser.normalize(rawPath)
        val bytes = text.toByteArray(Charsets.UTF_8)
        val encoded = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        val executed = RootShell.run(
            "echo ${RootShell.quote(encoded)} | base64 -d > ${RootShell.quote(path)}"
        ) != null
        val measured = RootShell.run("stat -c %s ${RootShell.quote(path)}")
            ?.firstOrNull()?.trim()?.toLongOrNull()
        return FileOpOutcome(executed, executed && measured == bytes.size.toLong())
    }

    fun delete(paths: List<String>): FileOpOutcome {
        if (paths.isEmpty()) return FileOpOutcome(false, false)
        var executed = true
        for (path in paths) {
            executed = executed && RootShell.run("rm -rf ${RootShell.quote(path)}") != null
        }
        val verified = executed && paths.all { RootShell.run("test -e ${RootShell.quote(it)}") == null }
        return FileOpOutcome(executed, verified)
    }

    fun rename(path: String, newName: String): FileOpOutcome {
        val parent = FileBrowser.parentOf(path) ?: return FileOpOutcome(false, false)
        val target = FileBrowser.childPath(parent, newName)
        val executed = RootShell.run(
            "mv -f ${RootShell.quote(FileBrowser.normalize(path))} ${RootShell.quote(target)}"
        ) != null
        val verified = executed && RootShell.run("test -e ${RootShell.quote(target)}") != null
        return FileOpOutcome(executed, verified)
    }

    fun createDirectory(parent: String, name: String): FileOpOutcome {
        val target = FileBrowser.childPath(FileBrowser.normalize(parent), name)
        val executed = RootShell.run("mkdir -p ${RootShell.quote(target)}") != null
        val verified = executed && RootShell.run("test -d ${RootShell.quote(target)}") != null
        return FileOpOutcome(executed, verified)
    }

    /**
     * إنشاء ملف فارغ — نظير «ملف جديد» في MT.
     *
     * و`touch` بلا `-c`: إن كان الملف موجودًا تُحدَّث ساعته ولا يُفرَّغ محتواه (وإلا صار
     * «ملف جديد» على اسم موجود **ماحيةً** لبيانات المستخدم).
     */
    fun createFile(parent: String, name: String): FileOpOutcome {
        val target = FileBrowser.childPath(FileBrowser.normalize(parent), name)
        val executed = RootShell.run("touch ${RootShell.quote(target)}") != null
        val verified = executed && RootShell.run("test -f ${RootShell.quote(target)}") != null
        return FileOpOutcome(executed, verified)
    }

    /**
     * الضغط بصيغة ومستوى مختارَين من المستخدم.
     *
     * **والسلّم مقصود:** المحرّك الداخلي أولًا للصيغتين (يقرأ ما تقرأه العملية نفسها، ويعمل
     * حيث لا `tar` على الجهاز)، وإن قال إنّ مصدرًا لا يُقرأ (مسار جذري لا تراه العملية)
     * سقطنا إلى `tar` عبر الصدفة — وهي قدرة كانت موجودة ولا تُفقد بإضافة مسار داخلي.
     *
     * والافتراضي مشتقّ من **الامتداد** لا من مزاج الواجهة: فمن وعد المستخدم باسم `.tar.gz`
     * لا يكتب داخله zip.
     */
    fun compress(
        paths: List<String>,
        archivePath: String,
        format: ArchiveFormat = if (FileArchive.isZip(archivePath)) ArchiveFormat.Zip else ArchiveFormat.TarGz,
        level: CompressionLevel = CompressionLevel.Normal,
    ): FileOpOutcome {
        if (paths.isEmpty()) return FileOpOutcome(false, false)
        val archive = FileBrowser.normalize(archivePath)
        val outcome = FileArchiveEngine.create(paths, archive, format, level)
        if (format == ArchiveFormat.TarGz && outcome.failure == ArchiveFailure.UnreadableSource) {
            return tarThroughShell(paths, archive)
        }
        return outcome.toFileOpOutcome()
    }

    /**
     * `tar -C` مقصود: نضغط من داخل المجلد الأب فتُخزَّن المسارات نسبية، وإلا صار فكّ
     * الأرشيف على جهاز آخر كتابة في مسار لا وجود له.
     */
    private fun tarThroughShell(paths: List<String>, archive: String): FileOpOutcome {
        val parent = FileBrowser.parentOf(paths.first()) ?: return FileOpOutcome(false, false)
        val names = paths.map(FileBrowser::nameOf).joinToString(" ") { RootShell.quote(it) }
        val executed = RootShell.run(
            "tar -czf ${RootShell.quote(archive)} -C ${RootShell.quote(parent)} $names"
        ) != null
        val verified = executed && RootShell.run("test -e ${RootShell.quote(archive)}") != null
        return FileOpOutcome(executed, verified)
    }

    /**
     * الفكّ: المحرّك الداخلي للصيغ الأربع (مع حماية `zip-slip`)، والصدفة احتياطًا لما
     * لا تقرؤه العملية (مسار جذري).
     *
     * وzip يُفكّ داخليًّا لا بـ`unzip`، لأن `unzip` غير مضمون على كل جهاز أندرويد —
     * وميزة تعمل على بعض الأجهزة أسوأ من ميزة تقول إنها لم تستطع. و`tar`/`tar.gz` صارا
     * كذلك بعد أن كانا يعتمدان على وجود ثنائيَّة `tar` على الجهاز.
     */
    fun extract(archivePath: String, destination: String): FileOpOutcome {
        val archive = FileBrowser.normalize(archivePath)
        val dir = FileBrowser.normalize(destination)
        val outcome = FileArchiveEngine.extractArchive(archive, dir)
        if (FileArchive.isZip(archive) || outcome.failure != ArchiveFailure.UnreadableSource) {
            return outcome.toFileOpOutcome()
        }
        val prepared = RootShell.run("mkdir -p ${RootShell.quote(dir)}") != null
        val executed = prepared && RootShell.run(
            "tar -xzf ${RootShell.quote(archive)} -C ${RootShell.quote(dir)}"
        ) != null
        return FileOpOutcome(executed, verified = executed)
    }

    /**
     * مساحة نظام الملفات الذي يقع فيه المسار — المجموع والمتاح.
     *
     * تُقرأ بـ`StatFs` على المسار نفسه لا من جدول التحميلات: لوح مدير الملفات يقف على
     * `/system` مرة وعلى `/data` أخرى، وهذان نظاما ملفات مختلفان، فسطر الحالة يجب أن
     * يقيس **النظام الذي يقف عليه هذا اللوح** لا نقطة تحميل مختارة سلفًا.
     *
     * و`null` تعني «لم تُقرأ»: المسار غير موجود، أو لا نظام ملفات له. **ولا صفر**: «صفر
     * متاح» رسالة «القرص ممتلئ» وهي أسوأ رسالة كاذبة (ADR-23).
     */
    fun diskSpace(rawPath: String): DiskSpace? {
        val path = FileBrowser.normalize(rawPath)
        val stats = runCatching { android.os.StatFs(path) }.getOrNull() ?: return null
        val total = runCatching { stats.blockSizeLong * stats.blockCountLong }.getOrNull() ?: return null
        val free = runCatching { stats.blockSizeLong * stats.availableBlocksLong }.getOrNull() ?: return null
        if (total <= 0L) return null
        return DiskSpace(totalBytes = total, freeBytes = free)
    }

    /**
     * حجم عقدة على القرص بالبايت، أو `null` إن لم يُقرأ.
     *
     * ويُستعمل لقياس تقدّم مهمة جارية: حجم العنصر في الوجهة يُقرأ كل فترة ما دام في
     * الطيران، فتصير النسبة **مقيسة** لا مُخترعة (ADR-07). والمجلد يُقاس بمحتواه (`du -sb`)
     * لا بصفر، لأن صفرًا سيُقرأ «لا تقدّم» بينما النسخ يعمل.
     */
    fun nodeBytes(rawPath: String): Long? {
        val path = FileBrowser.normalize(rawPath)
        val line = RootShell.run("du -sb ${RootShell.quote(path)}")
            ?.firstOrNull()
            ?.trim()
            ?: return null
        // الشكل المتوقّع: `<bytes>\t<path>` — والحقل الأول هو الرقم، وما لا يُفهم يُعاد
        // `null` بدل تخمين رقم من مسار يشبه عددًا.
        return line.substringBefore('\t').substringBefore(' ').toLongOrNull()
    }

    /**
     * سياق SELinux للمسار، أو `null` إن لم يُعلنه الجهاز.
     *
     * يُقرأ من `ls -Zd` لأن `stat -c %C` ليس في toybox على كل إصدار. و`null` تعني
     * «لم أعرف» لا «لا سياق» — فمدراء الملفات على أندرويد لا يقولون هذه المعلومة أصلًا،
     * وهي أول ما يحتاجه من يضبط وحدة Magisk.
     */
    fun selinuxContext(path: String): String? {
        val line = RootShell.run("ls -Zd ${RootShell.quote(FileBrowser.normalize(path))}")
            ?.firstOrNull()?.trim() ?: return null
        // الشكل المتوقّع: `<context> <owner> <group> <size> <date…> <name>`
        // والحقل الأول هو السياق؛ ونشترط فيه نقطتان على الأقل (`u:object_r:system_file:s0`)
        // وإلا أعدنا `null` بدل تمرير اسم ملف عاديّ كأنه سياق SELinux.
        val first = line.split(Regex("\\s+")).firstOrNull() ?: return null
        return first.takeIf { it.count { c -> c == ':' } >= 2 }
    }
}
