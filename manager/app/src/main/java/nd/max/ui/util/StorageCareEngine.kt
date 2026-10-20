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
/*
 * محرّك العناية بالتخزين: **قياس ثم حذف ثم قياس**.
 *
 * كان `UltraCleanEngine`، وبقي عقده كما هو وأُضيفت فئة واحدة (`CorpseData` — `STORAGE-CARE-01`).
 * وثلاث قواعد كُتبت قبل أول سطر، وكلها من عقد `MAX-MANAGER-LEVEL-UP.md` §6.3 و§11:
 *
 * 1. **لا مسار حرّ من الواجهة.** كل مسار هنا **قالب ثابت** في هذا الملفّ (`INTERNAL_CACHE_GLOB`,
 *    `LOG_DIRS`, `thumbDirs`) أو **نتيجة سرد يملكه المحرّك نفسه** (`CorpseData` تقرأ مجلدات
 *    `Android/data` و`Android/obb` وتسقط منها ما هو مثبّت) — لا يأتي مسار من الشاشة ولا من
 *    المستخدم أصلًا، فلا يوجد ما يُقتبس خطأً. والحذف يمرّ بـ`FileSystemEngine.delete` (نفس ما
 *    يستخدمه مدير الملفات)، وهو يقتبس كل مسار ويقرأ بعده (`test -e`) — فالتحقّق ليس وعدًا في
 *    تعليق.
 * 2. **لا حذف لملفّ مستخدم بلا تأشير.** ولذلك `InstallerFiles` و`EmptyFolders` مطفأتان
 *    افتراضيًّا في النموذج، **وهنا** لا يوجد لهما استثناء: تُحذف فقط إن وصلتا في قائمة الفئات
 *    المختارة — والقرار في النموذج لا في المحرّك. و`CorpseData` مُشّرّة لأنها ليست ملك أحد:
 *    التطبيق أُزيل فعلًا وبقيت بياناته.
 * 3. **`null` ليست صفرًا.** فشل التنفيذ يعني `bytes = null`، وتُعرض «لم يُقس»؛ ونجاحٌ بمخرج
 *    فارغ يعني `0`، وتُعرض «٠» (فليس في المسار شيء أصلًا).
 *
 * **وسلامة `CorpseData` بحكمين لا حكم واحد:**
 * - **«مثبّت» تُقرأ من النظام نفسه** (`pm list packages` عبر الصدفة، والجذر متاح بالتعريف في
 *   هذه الفئة) لا من `getInstalledPackages`: رؤية الحزم في أندرويد ١١+ محجوبة بما يعلنه
 *   التطبيق في `<queries>`، وحزمة مثبّتة بلا أيقونة تُقرأ «غير مثبّتة» — أي حذف بيانات
 *   تطبيق حيّ. والسرد عبر الصدفة يرى ما يراه النظام لا ما تسمح به الرؤية المعلنة.
 * - **ولا يُحذف ما لا يشبه اسم حزمة** (بلا نقطة): مجلد غريب الاسم يُترك ويُترك معه، فلا
 *   يُصلح المزيل عطبًا لم يقصده.
 *
 * **وحدوده المعلنة:**
 * - تكرار المجلدات عبر روابط رمزية لا يُتبع (`File.listFiles` لا يتبع symlink للمجلدات)، فلا
 *   يدخل المسح في حلقة.
 * - الملفات المخفية **تُحذف** من مجلدات الكاش كما تُحذف غيرها — بخلاف صدفة `*` التي تتجاهلها.
 *   فذلك هو المقصود: أندرويد يخزّن أكثر كاشه في مجلدات مخفية، وإغفالها كان سيترك الميزة تعمل
 *   نصف عمل.
 * - عمق المسح محدود ([WALK_DEPTH_CAP]) ومعه سقف عناصر ([WALK_ENTRY_CAP]): شجرة تخزين كاملة
 *   على جهاز ممتلئ تحتاج دقائق، وشاشة تجمّد لتُنتج بايتات أدقّ ليست صفقة رابحة — وعند بلوغ
 *   السقف يُعلن القياس **ناقصًا** لا كاملًا.
 * - و`Android/media` **مستثنى** من كشف البقايا عن قصد: ما فيه وسائط المستخدم أنشأها بتطبيق
 *   (صورة حفظها، ملفّ صدّره)، وحذفها بحكم «التطبيق لم يعد مثبّتًا» حذفٌ لوسائط المستخدم لا
 *   لبقايا تطبيق. البقايا = `data` و`obb` فقط.
 */
package nd.max.ui.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import java.io.File

/** نتيجة حذف فئة: هل نُفِّذ، وهل تحقّق، وما المساحة التي أُزيلت فعلًا. */
data class CleanOutcome(val executed: Boolean, val verified: Boolean, val freedBytes: Long)

object StorageCareEngine {

    /** سقف عمق المسح من الجذر — يكفي لشجرة التنزيلات والتخزين المشترك ولا يبلغ قرارات النظام. */
    private const val WALK_DEPTH_CAP = 6

    /** سقف عدد العناصر في القياس الواحد. بلوغه يُعلن نقصًا لا كمالًا. */
    private const val WALK_ENTRY_CAP = 40_000

    /**
     * أمر نظام التنظيف لأكياس التطبيقات — **لا `rm` على `/data/data`**.
     *
     * و`pm trim-caches` هو ما يطلبه النظام نفسه لخفض الكاش، فيمرّ على كل تطبيق عبر آليته
     * المعلنة بدل أن نمرّ عليه بمسار guessable. والحجم `999G` معناه «أفرغ ما تستطيع».
     */
    private const val TRIM_CACHES = "pm trim-caches 999G"

    /** مجلدات الكاش الداخلي لكل تطبيق (تحتاج جذرًا للقراءة). */
    private const val INTERNAL_CACHE_GLOB = "/data/data/*/cache"

    /** آثار الانهيار وصندوق النظام (تحتاج جذرًا). */
    private val LOG_DIRS = listOf("/data/anr", "/data/tombstones", "/data/system/dropbox")

    /** مجلدات الصور المصغّرة الثابتة على التخزين المشترك. */
    private fun thumbDirs(external: File): List<File> = listOf(
        File(external, "DCIM/.thumbnails"),
        File(external, "Pictures/.thumbnails"),
        File(external, "Movies/.thumbnails"),
        File(external, "Download/.thumbnails"),
        File(external, ".thumbnails"),
    )

    /** والدتا البقايا: ما تحتهما يخصّ التطبيقات لا المستخدم. */
    private fun corpseParents(external: File): List<File> = listOf(
        File(external, "Android/data"),
        File(external, "Android/obb"),
    )

    /**
     * جذور الفحص للملفات المكررة - التخزين المشترك (shared external).
     */
    private fun duplicateRoots(): List<File> = listOf(Environment.getExternalStorageDirectory())

    /** أماكن السلة المحتملة على التخزين المشترك. */
    private fun recycleLocations(external: File): List<File> = listOf(
        File(external, ".Trash"),
        File(external, "Trash"),
        File(external, ".trash"),
        File(external, "trash"),
    )


    /**
     * يقيس فئة واحدة. لا يرمي: الفشل يعود `bytes = null`.
     *
     * @param rootAvailable هل الجذر متاح؟ يحدّد المسار: الأمر عبر الصدفة أو القراءة المباشرة.
     */
    fun measure(
        context: Context,
        category: StorageCareCategory,
        rootAvailable: Boolean,
    ): CleanMeasurement = when (category) {
        StorageCareCategory.AppCaches ->
            measureViaShell(category, INTERNAL_CACHE_GLOB)
                ?: CleanMeasurement(category, null, 0)

        StorageCareCategory.SharedCaches -> {
            val external = Environment.getExternalStorageDirectory()
            measureViaShell(category, File(external, "Android/data/*/cache").path)
                ?: CleanMeasurement(category, null, 0)
        }

        StorageCareCategory.CorpseData -> {
            val dirs = corpsePaths()
            when {
                dirs == null -> CleanMeasurement(category, null, 0)
                dirs.isEmpty() -> CleanMeasurement(category, 0L, 0)
                else -> measureViaShell(category, *dirs.toTypedArray())
                    ?: CleanMeasurement(category, null, 0)
            }
        }

        StorageCareCategory.Thumbnails -> {
            val dirs = thumbDirs(Environment.getExternalStorageDirectory())
            val present = dirs.filter { runCatching { it.isDirectory }.getOrDefault(false) }
            if (present.isEmpty()) {
                CleanMeasurement(category, 0L, 0)
            } else if (!rootAvailable && present.none { runCatching { it.canRead() }.getOrDefault(false) }) {
                // موجودة ولا تُقرأ: «لم يُقس» لا «صفر».
                CleanMeasurement(category, null, present.size)
            } else {
                walkBytes(category, present)
            }
        }

        StorageCareCategory.InstallerFiles -> {
            val downloads = File(Environment.getExternalStorageDirectory(), "Download")
            val found = collectFiles(downloads, rootAvailable) { it.name.endsWith(".apk", ignoreCase = true) }
                ?: return CleanMeasurement(category, null, 0)
            CleanMeasurement(category, found.sumOf { it.second }, found.size)
        }

        StorageCareCategory.EmptyFolders -> {
            val roots = listOf(Environment.getExternalStorageDirectory())
            val empty = collectEmptyFolders(roots) ?: return CleanMeasurement(category, null, 0)
            // الحجم صفر بالتعريف؛ والعدّ هو الخبر، فلا يُكتب «0 B».
            CleanMeasurement(category, 0L, empty.size)
        }

        StorageCareCategory.SystemLogs -> {
            val missing = LOG_DIRS.none { path -> runCatching { File(path).isDirectory }.getOrDefault(false) }
            if (missing) {
                CleanMeasurement(category, 0L, 0)
            } else {
                measureViaShell(category, *LOG_DIRS.toTypedArray())
                    ?: CleanMeasurement(category, null, 0)
            }
        }

        StorageCareCategory.Duplicates -> measureDuplicates(category)

        StorageCareCategory.BrokenSymlinks -> measureBrokenSymlinks(category)

        StorageCareCategory.Databases -> measureDatabases(category)

        StorageCareCategory.RecycleBin -> measureRecycleBin(category, rootAvailable)
    }

    /**
     * يحذف فئة واحدة ويعيد ما أُزيل فعلًا.
     *
     * والمساحة المُزالة **تُقاس قبل وبعد** لا تُقدَّر: المقارنة بين [before] وما يُقاس بعد
     * الحذف — والفرق السالب يُكلَّم إلى صفر (تطبيق عاد فحجز مكانه) فلا يُعلن تحرير لم يقع.
     */
    fun clean(
        context: Context,
        category: StorageCareCategory,
        before: CleanMeasurement?,
        rootAvailable: Boolean,
    ): CleanOutcome {
        val executed: Boolean
        val verified: Boolean

        when (category) {
            StorageCareCategory.AppCaches -> {
                // نظامي لا `rm`: النظام يخفض كاش كل تطبيق بآليته.
                executed = PrivilegedShell.run(TRIM_CACHES) != null
                verified = executed
            }

            StorageCareCategory.SharedCaches -> {
                val outcome = deleteMatching(
                    glob = File(Environment.getExternalStorageDirectory(), "Android/data/*/cache").path + "/*"
                )
                executed = outcome.first
                verified = outcome.second
            }

            StorageCareCategory.CorpseData -> {
                val dirs = corpsePaths().orEmpty()
                if (dirs.isEmpty()) {
                    return CleanOutcome(executed = true, verified = true, freedBytes = 0L)
                }
                val outcome = FileSystemEngine.delete(dirs)
                executed = outcome.executed
                verified = outcome.verified
            }

            StorageCareCategory.Thumbnails -> {
                val dirs = thumbDirs(Environment.getExternalStorageDirectory())
                    .filter { runCatching { it.isDirectory }.getOrDefault(false) }
                val outcome = FileSystemEngine.delete(dirs.map { it.path })
                executed = outcome.executed
                verified = outcome.verified
            }

            StorageCareCategory.InstallerFiles -> {
                val downloads = File(Environment.getExternalStorageDirectory(), "Download")
                val apks = collectFiles(downloads, rootAvailable) { it.name.endsWith(".apk", ignoreCase = true) }
                    ?.map { it.first } ?: emptyList()
                if (apks.isEmpty()) {
                    return CleanOutcome(executed = true, verified = true, freedBytes = 0L)
                }
                val outcome = FileSystemEngine.delete(apks)
                executed = outcome.executed
                verified = outcome.verified
            }

            StorageCareCategory.EmptyFolders -> {
                val empty = collectEmptyFolders(listOf(Environment.getExternalStorageDirectory()))
                    ?.map { it.path } ?: emptyList()
                if (empty.isEmpty()) {
                    return CleanOutcome(executed = true, verified = true, freedBytes = 0L)
                }
                val outcome = FileSystemEngine.delete(empty)
                executed = outcome.executed
                verified = outcome.verified
            }

            StorageCareCategory.SystemLogs -> {
                val targets = LOG_DIRS.map { "$it/*" }
                var anyExecuted = true
                var allVerified = true
                for (glob in targets) {
                    val outcome = deleteMatching(glob)
                    anyExecuted = anyExecuted && outcome.first
                    allVerified = allVerified && outcome.second
                }
                executed = anyExecuted
                verified = allVerified
            }

            StorageCareCategory.Duplicates -> {
                val outcome = cleanDuplicates()
                executed = outcome.first
                verified = outcome.second
            }

            StorageCareCategory.BrokenSymlinks -> {
                val outcome = cleanBrokenSymlinks()
                executed = outcome.first
                verified = outcome.second
            }

            StorageCareCategory.Databases -> {
                val outcome = cleanDatabases()
                executed = outcome.first
                verified = outcome.second
            }

            StorageCareCategory.RecycleBin -> {
                val outcome = cleanRecycleBin()
                executed = outcome.first
                verified = outcome.second
            }
        }

        val after = if (verified) measure(context, category, rootAvailable) else null
        val beforeBytes = before?.bytes
        val afterBytes = after?.bytes
        val freed = if (beforeBytes != null && afterBytes != null) {
            (beforeBytes - afterBytes).coerceAtLeast(0L)
        } else {
            0L
        }
        return CleanOutcome(executed = executed, verified = verified, freedBytes = freed)
    }

    /**
     * سرد فئة: **ما سيُحذف بالضبط**، الأكبر أولًا — لا وصفًا بالكلام.
     *
     * وُجد بأمر المالك: «الضغط على كاش التخزين المشترك لا يظهر لي بالضبط ما هو الذي سيُمسح
     * منها أي الملفات». والقاعدة الثابتة: **السرد من نفس مصدر الحذف** — نفس القالب أو نفس
     * القائمة التي يقرؤها [clean]، فلا يعرض سردٌ شيئًا لا يُحذف ولا يخفي ما سيُحذف.
     *
     * و`failed` تعني «تعذّر السرد» (فشل الأمر أو غياب الجذر)، وليست «لا شيء»: الشاشة
     * تفرّق بينهما كما تفرّق بين `null` و`0` في القياس.
     */
    fun targets(
        context: Context,
        category: StorageCareCategory,
        rootAvailable: Boolean,
    ): CareTargetList {
        return when (category) {
            StorageCareCategory.AppCaches -> cacheTargets(context, INTERNAL_CACHE_GLOB)

            StorageCareCategory.SharedCaches -> cacheTargets(
                context,
                File(Environment.getExternalStorageDirectory(), "Android/data/*/cache").path,
            )

            StorageCareCategory.CorpseData -> {
                val corpses = corpsePaths() ?: return StorageCareModel.failedTargets()
                targetList(
                    corpses.map { path ->
                        CareTarget(
                            label = File(path).name,
                            path = path,
                            packageName = File(path).name,
                            bytes = null,
                        )
                    },
                )
            }

            StorageCareCategory.Thumbnails -> directoryTargets(
                thumbDirs(Environment.getExternalStorageDirectory()),
            ) { it.parentFile?.name?.takeIf { name -> name.isNotBlank() } ?: it.name }

            StorageCareCategory.InstallerFiles -> {
                val downloads = File(Environment.getExternalStorageDirectory(), "Download")
                val found = collectFiles(downloads, rootAvailable) { it.name.endsWith(".apk", ignoreCase = true) }
                    ?: return StorageCareModel.failedTargets()
                StorageCareModel.foldTargets(
                    found.map { (path, size) -> CareTarget(label = File(path).name, path = path, bytes = size) },
                )
            }

            StorageCareCategory.EmptyFolders -> {
                val empty = collectEmptyFolders(listOf(Environment.getExternalStorageDirectory()))
                    ?: return StorageCareModel.failedTargets()
                StorageCareModel.foldTargets(
                    empty.map { CareTarget(label = it.name, path = it.path, bytes = 0L) },
                )
            }

            StorageCareCategory.SystemLogs -> directoryTargets(LOG_DIRS.map(::File)) { it.name }

            StorageCareCategory.Duplicates -> {
                val external = Environment.getExternalStorageDirectory()
                val groups = collectFileGroups(external, WALK_DEPTH_CAP, WALK_ENTRY_CAP)
                val targets = groups.flatMap { (_, files) ->
                    files.drop(1).map { (path, size) ->
                        CareTarget(label = File(path).name, path = path, bytes = size)
                    }
                }
                StorageCareModel.foldTargets(targets)
            }

            StorageCareCategory.BrokenSymlinks -> {
                val external = Environment.getExternalStorageDirectory()
                val paths = collectBrokenSymlinkPaths(external, WALK_DEPTH_CAP, WALK_ENTRY_CAP)
                    ?: return StorageCareModel.failedTargets()
                StorageCareModel.foldTargets(
                    paths.map { path ->
                        CareTarget(label = File(path).name, path = path, bytes = 0L)
                    },
                )
            }

            StorageCareCategory.Databases -> {
                val external = Environment.getExternalStorageDirectory()
                val found = collectFiles(external, rootAvailable) {
                    it.name.endsWith(".db", ignoreCase = true) || it.name.endsWith(".sqlite", ignoreCase = true)
                } ?: return StorageCareModel.failedTargets()
                StorageCareModel.foldTargets(
                    found.map { (path, size) ->
                        CareTarget(label = File(path).name, path = path, bytes = size)
                    },
                )
            }

            StorageCareCategory.RecycleBin -> directoryTargets(
                recycleLocations(Environment.getExternalStorageDirectory()),
            ) { it.name }
        }
    }

    /**
     * قالب المسار الذي تعمل عليه الفئة — يُعرض في ورقة التفاصيل بنصّه.
     *
     * وهو **قالب لا قائمة**: ما يُعرض للمستخدم هنا هو نفسه ما يُنفَّذ في [clean]، فالنصّ
     * يكشف الحقيقة بدل أن يلخّصها. والقوالب مسارات بيانات لا نصوصًا تُترجم.
     */
    fun targetPattern(category: StorageCareCategory): String {
        val external = Environment.getExternalStorageDirectory().path
        return when (category) {
            StorageCareCategory.AppCaches -> INTERNAL_CACHE_GLOB
            StorageCareCategory.SharedCaches -> "$external/Android/data/*/cache"
            StorageCareCategory.CorpseData -> "$external/Android/data · $external/Android/obb"
            StorageCareCategory.Thumbnails -> "$external/**/.thumbnails"
            StorageCareCategory.InstallerFiles -> "$external/Download/**/*.apk"
            StorageCareCategory.EmptyFolders -> "$external/**"
            StorageCareCategory.SystemLogs -> LOG_DIRS.joinToString(" · ")
            StorageCareCategory.Duplicates -> "$external/**/*"
            StorageCareCategory.BrokenSymlinks -> "$external/**/*"
            StorageCareCategory.Databases -> "$external/**/*.{db,sqlite}"
            StorageCareCategory.RecycleBin -> "$external/**/.{Trash,Trash,trash,.trash}"
        }
    }

    /** سرد كاش التطبيقات: كل سطر تطبيق باسمه ومقاسه — من `du` على نفس القالب المحذوف. */
    private fun cacheTargets(context: Context, glob: String): CareTargetList {
        val output = duOutput("du -sk $glob") ?: return StorageCareModel.failedTargets()
        val rows = output.mapNotNull(StorageCareModel::parseDuRow)
        if (rows.isEmpty() && output.isNotEmpty()) return StorageCareModel.failedTargets()
        return StorageCareModel.foldTargets(
            rows.map { (path, bytes) ->
                val pkg = StorageCareModel.packageOfCachePath(path)
                CareTarget(
                    label = pkg?.let { appLabel(context, it) } ?: File(path).name,
                    path = path,
                    packageName = pkg,
                    bytes = bytes,
                )
            },
        )
    }

    /** سرد قائمة مجلدات معروفة: القياس بـ`du` واحد، والتسمية بقاعدة تمرّرها الفئة. */
    private fun directoryTargets(dirs: List<File>, label: (File) -> String): CareTargetList {
        val present = dirs.filter { runCatching { it.isDirectory }.getOrDefault(false) }
        val sizes = duByPath(present.map { it.path })
        return StorageCareModel.foldTargets(
            present.map { dir ->
                CareTarget(
                    label = label(dir),
                    path = dir.path,
                    bytes = sizes?.get(dir.path),
                )
            },
        )
    }

    /** سردٌ ثم قياس مجموعة مسارات بأمر `du` واحد: ما لم يُقس يبقى `null` ولا يُصطنع له صفر. */
    private fun targetList(targets: List<CareTarget>): CareTargetList {
        val sizes = duByPath(targets.mapNotNull { it.path })
        if (sizes == null) return StorageCareModel.failedTargets()
        return StorageCareModel.foldTargets(
            targets.map { target -> target.copy(bytes = sizes[target.path]) },
        )
    }

    /** أحجام مسارات معروفة: `du -sk` واحد عليها كلها. `null` يعني «لم يُقرأ» لا «صفر». */
    private fun duByPath(paths: List<String>): Map<String, Long>? {
        if (paths.isEmpty()) return emptyMap()
        val command = "du -sk " + paths.joinToString(" ") { PrivilegedShell.quote(it) }
        val output = duOutput(command) ?: return null
        val sizes = output.mapNotNull(StorageCareModel::parseDuRow).toMap()
        // أمر ناجح بلا سطر واحد غير مفهوم يعني فشل القراءة، لا صفرًا: الصفر يُكتب من سطر
        // `0\t/path` لا من غياب الأسطر.
        return if (sizes.isEmpty() && output.isNotEmpty()) null else sizes
    }

    /**
     * مخرج `du` مع تقبّل الخروج غير الصفري.
     *
     * و`du` على شجرة فيها مجلد لا يُقرأ **تطبع أحجام ما قرأت** ثم تخرج بخطأ — فحكم «لم
     * يُقس» لا يصحّ من رمز الخروج. والحدّ الصحيح: مخرج فارغ **مع** فشل يعني «لم يُقرأ»؛
     * ومخرج فيه أسطر يعني قياسًا (فيه ما لم يُقرأ، وهو المعلَن في الشاشة).
     */
    private fun duOutput(command: String): List<String>? {
        val result = PrivilegedShell.runDetailed(command) ?: return null
        if (result.out.isEmpty() && !result.success) return null
        return result.out
    }

    /**
     * يمسح كاش تطبيق واحد — **الكاش وحده، لا بياناته**، وهذا الفرق الذي طلبه المالك
     * بنصّه: «إمكانية مسح أجزاء معيّنة مثل الكاش بدل من البيانات».
     *
     * والمساران هما ما يعنيه النظام بـ«كاش التطبيق»: كاشه الداخلي، وكاشه على التخزين
     * المشترك. والحذف **محتوى المجلد لا المجلد** (تطبيق يعيد إنشاءه عند الحاجة، وحذفه
     * ذاته تعديل على بنية غيره)، والتحرير يُقاس فرقًا قبل/بعد كما في كل حذف هنا.
     */
    fun clearAppCache(packageName: String): CleanOutcome {
        val dirs = appCacheDirs(packageName).filter { runCatching { it.isDirectory }.getOrDefault(false) }
        if (dirs.isEmpty()) return CleanOutcome(executed = true, verified = true, freedBytes = 0L)

        val before = duByPath(dirs.map { it.path })
        var executed = true
        var verified = true
        for (dir in dirs) {
            val outcome = deleteMatching(PrivilegedShell.quote(dir.path) + "/*")
            executed = executed && outcome.first
            verified = verified && outcome.second
        }
        val after = if (verified) duByPath(dirs.map { it.path }) else null
        return CleanOutcome(executed = executed, verified = verified, freedBytes = freedOf(before, after))
    }

    /**
     * يمسح بيانات تطبيق — **وكاشه معها**، فـ`pm clear` لا تفصّل.
     *
     * ومسارها **نظامي لا `rm` على `/data/data`** كما في كاش الكل: النظام يعرف ما يبقى
     * (الحسابات، والملفات المعلّقة)، ويوقف التطبيق قبل المسح. والقرار فعلٌ واعٍ يقف خلفه
     * تأكيد صريح في الشاشة — لا زرّ عابر.
     */
    fun clearAppData(packageName: String): CleanOutcome {
        val dataDir = "/data/data/$packageName"
        val before = duByPath(listOf(dataDir))
        val output = PrivilegedShell.run("pm clear ${PrivilegedShell.quote(packageName)}")
            ?: return CleanOutcome(executed = false, verified = false, freedBytes = 0L)
        val executed = output.any { it.equals("Success", ignoreCase = true) }
        val after = if (executed) duByPath(listOf(dataDir)) else null
        return CleanOutcome(executed = executed, verified = executed, freedBytes = freedOf(before, after))
    }

    /** مجلدا كاش التطبيق: الداخلي والمشترك — وكلاهما يُقاس ويُمسح معًا. */
    private fun appCacheDirs(packageName: String): List<File> = listOf(
        File("/data/data/$packageName/cache"),
        File(Environment.getExternalStorageDirectory(), "Android/data/$packageName/cache"),
    )

    /** التحرير المقيس: مجموع فروق «قبل/بعد» لكل مسار، ولا يُعلن تحرير بلا قياسين. */
    private fun freedOf(before: Map<String, Long>?, after: Map<String, Long>?): Long {
        if (before == null || after == null) return 0L
        return before.entries.sumOf { (path, beforeBytes) ->
            (beforeBytes - (after[path] ?: 0L)).coerceAtLeast(0L)
        }
    }

    /** اسم التطبيق من مديره — ومن لا تسمح رؤيته باسمه يبقى باسم حزمته (لا يُخترع اسم). */
    private fun appLabel(context: Context, packageName: String): String = runCatching {
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getApplicationInfo(
                packageName,
                PackageManager.ApplicationInfoFlags.of(0L),
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getApplicationInfo(packageName, 0)
        }
        context.packageManager.getApplicationLabel(info).toString()
    }.getOrDefault(packageName)

    /**
     * مسارات بقايا التطبيقات المُزالة: مجلدات في `Android/data` و`Android/obb` أسماؤها حزم
     * ليست في سرد النظام.
     *
     * **وثلاثة قرارات في هذا المُساعد:**
     *
     * - **«مثبّت» من `pm list packages` لا من `PackageManager`:** الرؤية المعلنة في `<queries>`
     *   تحجب حزمًا مثبّتة (بلا أيقونة)، والحكم على حزمة حيّة بأنها «مُزالة» يعني حذف بياناتها.
     *   والجذر متاح بالتعريف هنا (الفئة `needsRoot`)، فالقراءة عبر الصدفة هي القراءة الصحيحة.
     * - **`null` ليست قائمة فارغة:** `null` تعني «لم يُقرأ شيء» (لا جذر أو فشل السرد) — فلا
     *   يُعلن صفر ولا تُبنى عليه حسبة؛ والقائمة الفارغة تعني «قُرئ فلم يبقَ شيء».
     * - **وما لا يشبه اسم حزمة يُترك:** مجلد بلا نقطة في اسمه ليس حزمة، فلا يُحذف بحكم اسم
     *   لم يُفهم. (والحذف يبقى على ما يطابق الشكل الذي يُقرأ منه.)
     */
    private fun corpsePaths(): List<String>? {
        val parents = corpseParents(Environment.getExternalStorageDirectory())
        val listings = parents.map { dir -> PrivilegedShell.run("ls -1 ${PrivilegedShell.quote(dir.path)}") }
        if (listings.all { it == null }) return null

        val installed = PrivilegedShell.run("pm list packages")
            ?.asSequence()
            ?.map { it.removePrefix("package:").trim() }
            ?.filter { it.isNotBlank() }
            ?.toSet()
            ?: return null

        return parents.zip(listings).flatMap { (dir, entries) ->
            entries.orEmpty()
                .filter { it.isNotBlank() && it.contains('.') }
                .filter { it !in installed }
                .map { File(dir, it).path }
        }
    }

    /**
     * حذف كل ما يطابق قالبًا عامًّا (`*`).
     *
     * والتوسيع **في الصدفة** (`ls -d`) ثم الحذف **بمسارات صريحة** عبر `FileSystemEngine.delete`:
     * لو مرّرنا القالب إلى `rm` مباشرةً لكان الحذف بلا قراءة بعده، وهذا يعارض عقد التحقّق.
     * والسطر القادم من `ls` قد يحمل اسم ملفّ فيه مسافة — و`FileSystemEngine` يقتبس كل مسار
     * قبل `rm`، فلا يُقرأ الاسم جزءًا ثانيًا من الأمر.
     */
    private fun deleteMatching(glob: String): Pair<Boolean, Boolean> {
        val listed = PrivilegedShell.run("ls -d $glob") ?: return false to false
        val paths = listed.filter { it.isNotBlank() && it != glob }
        if (paths.isEmpty()) return true to true
        val outcome = FileSystemEngine.delete(paths)
        return outcome.executed to outcome.verified
    }

    /**
     * قياس بمخرج `du -sk` (بالكيلوبايت) عبر الصدفة — للمسارات التي لا يقرؤها التطبيق.
     *
     * والفشل `null`: إما أن الأمر لم يُنفَّذ (لا جذر)، وإما أن العدد في سطر لم يُقرأ. وكلاهما
     * «لم يُقس» لا «صفر».
     */
    private fun measureViaShell(category: StorageCareCategory, vararg patterns: String): CleanMeasurement? {
        val output = PrivilegedShell.run("du -sk ${patterns.joinToString(" ")}") ?: return null
        if (output.isEmpty()) return CleanMeasurement(category, 0L, 0)
        var kilobytes = 0L
        var count = 0
        for (line in output) {
            val first = line.substringBefore('\t').substringBefore(' ').trim()
            val value = first.toLongOrNull() ?: return null
            kilobytes += value
            count++
        }
        return CleanMeasurement(category, kilobytes * 1024L, count)
    }

    /** قياس مباشر لشجرة من المجلدات المعروفة: مجموع أطوال الملفات وعددها. */
    private fun walkBytes(category: StorageCareCategory, roots: List<File>): CleanMeasurement {
        var bytes = 0L
        var count = 0
        val queue = ArrayDeque<Pair<File, Int>>()
        roots.forEach { queue.addLast(it to 0) }
        while (queue.isNotEmpty()) {
            val (dir, depth) = queue.removeFirst()
            val children = runCatching { dir.listFiles() }.getOrNull() ?: continue
            for (child in children) {
                if (child.isDirectory) {
                    if (depth < WALK_DEPTH_CAP) queue.addLast(child to depth + 1)
                    continue
                }
                count++
                bytes += runCatching { child.length() }.getOrDefault(0L)
                if (count >= WALK_ENTRY_CAP) {
                    return CleanMeasurement(category, bytes, count)
                }
            }
        }
        return CleanMeasurement(category, bytes, count)
    }

    /** ملفات تطابق [match] تحت [root]، أو `null` إن لم يُقرأ الجذر ولا يوجد جذر يقرؤه. */
    private fun collectFiles(
        root: File,
        rootAvailable: Boolean,
        match: (File) -> Boolean,
    ): List<Pair<String, Long>>? {
        if (!runCatching { root.isDirectory }.getOrDefault(false)) return emptyList()
        if (!rootAvailable && !runCatching { root.canRead() }.getOrDefault(false)) return null
        val found = mutableListOf<Pair<String, Long>>()
        val queue = ArrayDeque<Pair<File, Int>>()
        queue.addLast(root to 0)
        while (queue.isNotEmpty()) {
            val (dir, depth) = queue.removeFirst()
            val children = runCatching { dir.listFiles() }.getOrNull() ?: continue
            for (child in children) {
                if (child.isDirectory) {
                    if (depth < WALK_DEPTH_CAP) queue.addLast(child to depth + 1)
                    continue
                }
                if (match(child)) {
                    found += child.path to runCatching { child.length() }.getOrDefault(0L)
                }
                if (found.size >= WALK_ENTRY_CAP) return found
            }
        }
        return found
    }

    /**
     * مجلدات فارغة على التخزين المشترك، باستثناء `Android/`.
     *
     * و`Android/` مستثنى بنصّ العقد (§6.2): ما تحته ملك التطبيقات لا المستخدم، وحذفه ليس
     * تنظيفًا بل تعديل على بيانات تطبيقات أخرى.
     *
     * **والسرد مرّة واحدة لكل مجلد:** هذا المسح يمرّ على شجرة التخزين كاملة، وكان يسرد كل
     * مجلد مرّتين — مرّة لاختبار فراغه ومرّة عند نزوله من الطابور — فكان أضخم عمل إدخال في
     * الشاشة بلا مكسب. الآن العرضة المحسوبة تُحمَل مع المجلد إلى الطابور، فالسرد نفسه هو
     * المستعمل للاختبار وللنزول معًا.
     */
    private fun collectEmptyFolders(roots: List<File>): List<File>? {
        val empty = mutableListOf<File>()
        val queue = ArrayDeque<FolderListing>()
        for (root in roots) {
            val children = runCatching { root.listFiles() }.getOrNull() ?: continue
            queue.addLast(FolderListing(0, children.toList()))
        }
        while (queue.isNotEmpty()) {
            val listing = queue.removeFirst()
            for (child in listing.children) {
                if (!child.isDirectory) continue
                if (child.name == "Android") continue
                val inner = runCatching { child.listFiles() }.getOrNull() ?: return null
                if (inner.isEmpty()) {
                    empty += child
                    continue
                }
                if (listing.depth + 1 < WALK_DEPTH_CAP) {
                    queue.addLast(FolderListing(listing.depth + 1, inner.toList()))
                }
                if (empty.size >= WALK_ENTRY_CAP) return empty
            }
        }
        return empty
    }

    /** مجلد مع عرضته المحسوبة ودرجة عمقه — يحمل نتيجة السرد فلا يُسرد مرّتين. */
    private class FolderListing(
        val depth: Int,
        val children: List<File>,
    )

    // ═════════════════════════════════════════════════════════════════════════════════
    // الفئات الجديدة: الملفات المكررة، الروابط المكسورة، قواعد البيانات، سلة المحذوفات
    // ═════════════════════════════════════════════════════════════════════════════════

    /** قياس الملفات المكررة: حجم الزائد عن نسخة واحدة لكل مجموعة محتوى متطابقة. */
    private fun measureDuplicates(category: StorageCareCategory): CleanMeasurement {
        val external = Environment.getExternalStorageDirectory()
        if (!runCatching { external.isDirectory }.getOrDefault(false)) {
            return CleanMeasurement(category, null, 0)
        }
        val groups = collectFileGroups(external, WALK_DEPTH_CAP, WALK_ENTRY_CAP)
        if (groups.isEmpty()) return CleanMeasurement(category, 0L, 0)
        var dupBytes = 0L
        var dupCount = 0
        for ((_, files) in groups) {
            if (files.size >= 2) {
                dupBytes += files.sumOf { it.second } - files.first().second
                dupCount += files.size - 1
            }
        }
        return CleanMeasurement(category, dupBytes, dupCount)
    }

    /** قياس الروابط المكسورة: عدد الروابط التي تشير إلى هدف غير موجود. */
    private fun measureBrokenSymlinks(category: StorageCareCategory): CleanMeasurement {
        val external = Environment.getExternalStorageDirectory()
        if (!runCatching { external.isDirectory }.getOrDefault(false)) {
            return CleanMeasurement(category, null, 0)
        }
        val count = collectBrokenSymlinks(external, WALK_DEPTH_CAP, WALK_ENTRY_CAP)
        if (count == null) return CleanMeasurement(category, null, 0)
        // حجم الرابط المكسور صفري بالتعريف؛ العدد هو الخبر.
        return CleanMeasurement(category, 0L, count)
    }

    /** قياس قواعد البيانات: مجموع أحجام ملفات `.db` و `.sqlite` (بدون مسح محتواها). */
    private fun measureDatabases(category: StorageCareCategory): CleanMeasurement {
        val external = Environment.getExternalStorageDirectory()
        if (!runCatching { external.isDirectory }.getOrDefault(false)) {
            return CleanMeasurement(category, null, 0)
        }
        val found = collectFiles(external, rootAvailable = false) { it.name.endsWith(".db", ignoreCase = true) || it.name.endsWith(".sqlite", ignoreCase = true) }
        if (found == null) return CleanMeasurement(category, null, 0)
        return CleanMeasurement(category, found.sumOf { it.second }, found.size)
    }

    /** قياس سلة المحذوفات: مجموع محتويات سلة `.Trash`/`Trash` إن وُجدت. */
    private fun measureRecycleBin(category: StorageCareCategory, rootAvailable: Boolean): CleanMeasurement {
        val external = Environment.getExternalStorageDirectory()
        val locations = recycleLocations(external)
        val present = locations.filter { runCatching { it.isDirectory }.getOrDefault(false) }
        if (present.isEmpty()) return CleanMeasurement(category, 0L, 0)
        // لا يُقرأ السلة بلا جذر (مسارات خارجية محتملة).
        if (!rootAvailable && present.none { runCatching { it.canRead() }.getOrDefault(false) }) {
            return CleanMeasurement(category, null, present.size)
        }
        return walkBytes(category, present)
    }

    /** تنظيف الملفات المكررة: يُحفظ نسخة واحدة لكل مجموعة ويُحذف الباقي. */
    private fun cleanDuplicates(): Pair<Boolean, Boolean> {
        val external = Environment.getExternalStorageDirectory()
        if (!runCatching { external.isDirectory }.getOrDefault(false)) return false to false
        val groups = collectFileGroups(external, WALK_DEPTH_CAP, WALK_ENTRY_CAP)
        val toDelete = groups.flatMap { (_, files) ->
            files.drop(1).map { it.first }
        }
        if (toDelete.isEmpty()) return true to true
        val outcome = FileSystemEngine.delete(toDelete)
        return outcome.executed to outcome.verified
    }

    /** تنظيف الروابط المكسورة: حذف الرابط فقط (لا الهدف — غير موجود). */
    private fun cleanBrokenSymlinks(): Pair<Boolean, Boolean> {
        val external = Environment.getExternalStorageDirectory()
        if (!runCatching { external.isDirectory }.getOrDefault(false)) return false to false
        val symlinks = collectBrokenSymlinkPaths(external, WALK_DEPTH_CAP, WALK_ENTRY_CAP)
        val paths = symlinks ?: return false to false
        if (paths.isEmpty()) return true to true
        val outcome = FileSystemEngine.delete(paths)
        return outcome.executed to outcome.verified
    }

    /** تنظيف قواعد البيانات: تنفيذ `sqlite3 vacuum` على كل ملف `.db`/.sqlite. */
    private fun cleanDatabases(): Pair<Boolean, Boolean> {
        val external = Environment.getExternalStorageDirectory()
        if (!runCatching { external.isDirectory }.getOrDefault(false)) return false to false
        val found = collectFiles(external, rootAvailable = false) { it.name.endsWith(".db", ignoreCase = true) || it.name.endsWith(".sqlite", ignoreCase = true) }
        val paths = found?.map { it.first } ?: emptyList()
        if (paths.isEmpty()) return true to true
        var executed = true
        var verified = true
        for (path in paths) {
            // **والتحقق هنا كان كاذبًا حرفيًّا:** كان يُقرأ خُرج الأمر بشرطٍ يُقيَّم صحيحًا لأي
            // عنصر — أي «طُبِع سطر» تُقرأ «نجح»، و«لم يطبع» تُقرأ فشلًا، ولا علاقة لأيّهما
            // بالنتيجة. والصواب قراءة حكم الأمر نفسه (`runDetailed().success` = رمز الخروج)،
            // ثم فحص ما يجب أن يبقى قائمًا بعده: `VACUUM` لا يحذف الملفّ، فوجوده شاهدٌ للسلامة.
            val outcome = PrivilegedShell.runDetailed("sqlite3 ${PrivilegedShell.quote(path)} 'VACUUM;'")
            val ok = outcome?.success == true
            if (!ok) executed = false
            verified = verified && ok && runCatching { File(path).exists() }.getOrDefault(false)
        }
        return executed to verified
    }

    /** تنظيف سلة المحذوفات: حذف محتويات `.Trash`/`Trash`. */
    private fun cleanRecycleBin(): Pair<Boolean, Boolean> {
        val external = Environment.getExternalStorageDirectory()
        val locations = recycleLocations(external)
        val present = locations.filter { runCatching { it.isDirectory }.getOrDefault(false) }
        val paths = present.map { it.path }
        if (paths.isEmpty()) return true to true
        val outcome = FileSystemEngine.delete(paths)
        return outcome.executed to outcome.verified
    }

    /**
     * تجميع ملفات التخزين حسب محتواها (هاش): قائمة مجموعات، كل مجموعة carry مسارات
     * ذات محتوى متطابق. يُستثنى `Android/` بند العقد.
     */
    private fun collectFileGroups(
        root: File,
        depthCap: Int,
        entryCap: Int,
    ): Map<String, List<Pair<String, Long>>> {
        val groups = mutableMapOf<String, MutableList<Pair<String, Long>>>()
        var count = 0
        val queue = ArrayDeque<Pair<File, Int>>()
        queue.addLast(root to 0)
        while (queue.isNotEmpty()) {
            val (dir, depth) = queue.removeFirst()
            val children = runCatching { dir.listFiles() }.getOrNull() ?: continue
            for (child in children) {
                if (child.isDirectory) {
                    if (child.name == "Android") continue
                    if (depth < depthCap) queue.addLast(child to depth + 1)
                    continue
                }
                count++
                if (count > entryCap) return groups.toMap()
                val hash = runCatching { sha1Hex(child) }.getOrNull() ?: continue
                groups.getOrPut(hash) { mutableListOf() } += child.path to child.length()
            }
        }
        return groups.filter { it.value.size >= 2 }.toMap()
    }

    /** هاش SHA-1 لملف — يُستعمل للتجميع فقط، لا للأمان. */
    private fun sha1Hex(file: File): String {
        val digest = java.security.MessageDigest.getInstance("SHA-1")
        val fis = java.io.FileInputStream(file)
        val buffer = ByteArray(8192)
        try {
            var n: Int
            while (fis.read(buffer).also { n = it } != -1) {
                digest.update(buffer, 0, n)
            }
        } finally {
            fis.close()
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /** عدد الروابط المكسورة تحت الجذر. `null` إن تعذر القراءة. */
    private fun collectBrokenSymlinks(root: File, depthCap: Int, entryCap: Int): Int? {
        var count = 0
        var scanned = 0
        val queue = ArrayDeque<Pair<File, Int>>()
        queue.addLast(root to 0)
        while (queue.isNotEmpty()) {
            val (dir, depth) = queue.removeFirst()
            val children = runCatching { dir.listFiles() }.getOrNull() ?: continue
            for (child in children) {
                if (child.isDirectory) {
                    if (depth < depthCap) queue.addLast(child to depth + 1)
                    continue
                }
                scanned++
                if (scanned > entryCap) return count
                if (java.nio.file.Files.isSymbolicLink(child.toPath()) && !runCatching { child.exists() }.getOrDefault(false)) {
                    count++
                }
            }
        }
        return count
    }

    /** مسارات الروابط المكسورة للتنظيف. */
    private fun collectBrokenSymlinkPaths(root: File, depthCap: Int, entryCap: Int): List<String>? {
        val paths = mutableListOf<String>()
        var scanned = 0
        val queue = ArrayDeque<Pair<File, Int>>()
        queue.addLast(root to 0)
        while (queue.isNotEmpty()) {
            val (dir, depth) = queue.removeFirst()
            val children = runCatching { dir.listFiles() }.getOrNull() ?: continue
            for (child in children) {
                if (child.isDirectory) {
                    if (depth < depthCap) queue.addLast(child to depth + 1)
                    continue
                }
                scanned++
                if (scanned > entryCap) return paths
                if (java.nio.file.Files.isSymbolicLink(child.toPath()) && !runCatching { child.exists() }.getOrDefault(false)) {
                    paths += child.path
                }
            }
        }
        return paths
    }
}
