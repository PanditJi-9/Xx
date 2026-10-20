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
 * العناية بالتخزين — **قرارات التنظيف وحدها**، بلا مسار ولا ملفّ ولا صلاحية.
 *
 * هذه العائلة تحلّ محلّ «التنظيف الفائق» (`STORAGE-CARE-01`): شاشة شاملة تجمع التنظيف
 * والتحليل وإدارة التطبيقات، وقرارات كل قسم عند القسم الذي يملكه — هنا قرارات التنظيف،
 * و`StorageScanModel` يحكم التحليل، و`AppStorageReader` يحكم قسم التطبيقات — فلا تُخلط
 * قرارات ثلاثة أقسام في صنف واحد.
 *
 * **وثلاثة عقود مثبّتة هنا بالنصّ:**
 *
 * 1. **`InstallerFiles` و`EmptyFolders` مطفأتان افتراضيًّا.** وهما الفئتان الوحيدتان
 *    اللتان قد تكونان **ملكًا للمستخدم** (ملفّ نزّله، مجلدٌ أنشأه)، فالتأشير عليهما فعل
 *    واعٍ لا افتراض. و**`CorpseData` ليست ثالثتهما:** بقايا تطبيقٍ أُزيل ليست ملك أحد —
 *    التطبيق ذهب وبقيت بياناته — فهي مُشّرّة افتراضيًّا كالكاش.
 * 2. **فئات الجذر الأربع** (`AppCaches` · `SharedCaches` · `CorpseData` · `SystemLogs`)
 *    تُخفى عند غياب الجذر: مسارها خارج ما يقرؤه uid التطبيق أصلًا (لا قياس ولا حذف)،
 *    وعرضها كصفوف معطّلة دعوة دائمة لتحذير لا يمتلئ أبدًا. ويبقى **سطر النطاق** أعلى
 *    الشاشة يقول ما يوسّعه الجذر بالاسم.
 * 3. **`null` ليست صفرًا.** فئة فشل قياسها لا تُحسب في المجموع، ويُعلَن أن المجموع ناقص
 *    ([CleanTotal.hasUnmeasured]) — ولا يُكتب «0 B» على فئة فيها ملفات، ولا يُخفى النقص.
 *
 * و**صفر نقلٍ من أيّ مرجعٍ خارجي في هذه العائلة** — وقراءةُ الأفكار لا تُسجَّل اعتمادًا: لا تُسمّى
 * المراجع في `Credits`/`THIRD_PARTY_NOTICES` ولا في تعليقات الكود إلا بنقل كود فعلي أو أمرٍ من
 * المالك صريحًا (ADR-77).
 */
package nd.max.ui.util

/** فئة تنظيف واحدة: ثابت تخزينها، وتأشيرها الافتراضي، وصلاحيتها. */
enum class StorageCareCategory(
    /** الثابت: لا يتغيّر بتغيّر نصّ ولا مسار (نفس عقد `ScreenUsageStore`). */
    val key: String,
    /**
     * هل تُشّر عند فتح الشاشة؟
     *
     * والتأشير **لا يُخزَّن**: هو حالة جلسة واحدة تُبنى من هذه القيمة في كل فتح، فلا تعود
     * فئةٌ أطفأها المستخدم مرّةً معلَّمةً صامتة في جولة لاحقة.
     */
    val defaultChecked: Boolean,
    /** هل تحتاج جذرًا للقياس **والحذف** معًا؟ */
    val needsRoot: Boolean,
) {
    /** كاش التطبيقات في التخزين الداخلي (`/data/data/<pkg>/cache`). */
    AppCaches("app_caches", defaultChecked = true, needsRoot = true),

    /** كاش التخزين المشترك الذي تتركه التطبيقات المثبّتة (`Android/data/<pkg>/cache`). */
    SharedCaches("shared_caches", defaultChecked = true, needsRoot = true),

    /**
     * بقايا التطبيقات المُزالة: مجلدات تحت `Android/data` و`Android/obb` أسماؤها حزم لم
     * تعد مثبّتة.
     *
     * **ومُشّرّة افتراضيًّا لأنها ليست ملك أحد:** التطبيق أُزيل فعلًا وبقيت بياناته وحدها،
     * ولا يقرؤها أحد بعد اليوم. وهذا هو الفرق بينها وبين `InstallerFiles`/`EmptyFolders`:
     * هاتان قد تكونان للمستخدم، وهذه لا.
     */
    CorpseData("corpse_data", defaultChecked = true, needsRoot = true),

    /** مجلدات الصور المصغّرة — تُعاد بناؤها عند الحاجة، فهي أأمن ما يُحذف. */
    Thumbnails("thumbnails", defaultChecked = true, needsRoot = false),

    /** ملفات APK في التنزيلات — **قد تكون ملك المستخدم، فمطفأة افتراضيًّا**. */
    InstallerFiles("installer_files", defaultChecked = false, needsRoot = false),

    /** مجلدات فارغة خارج `Android/` — **مطفأة افتراضيًّا**، ولا تُحذف إلا بتأشير. */
    EmptyFolders("empty_folders", defaultChecked = false, needsRoot = false),

    /** آثار ANR وtombstones وصندوق النظام — خارج ما يقرؤه التطبيق بلا جذر. */
    SystemLogs("system_logs", defaultChecked = true, needsRoot = true),

    /**
     * ملفات لها **نفس المحتوى** — لا اسمها.
     *
     * **ومطفأة افتراضيًّا بعد أن كانت مُشّرّة (تكملةٌ لهذه الجولة):** الحذف هنا **يحذف ملفّات
     * المستخدم فعلًا** (`cleanDuplicates` يُبقي نسخة ويرمي الباقي)، والمحتوى المتطابق ليس دليل
     * ملكيّة — نسختان من صورة واحدة قد تكونان مقصودتين. فالقاعدة المعلنة في هذا الملف («APK
     * والمجلدات الفارغة قد تكون ملك المستخدم») تنطبق عليها بالحرف، وإدراجها في الافتراضيّ كان
     * يضع حذفًا حقيقيًّا تحت زرٍّ واحد بلا اختيار. لا يُحذف ملفّ إلّا بتأشير صريح.
     */
    Duplicates("duplicates", defaultChecked = false, needsRoot = false),

    /** رابط رمزيّ إلى هدف غير موجود: لا حذف للهدف (غير موجود أصلًا)، والحجم صفر بالتعريف. */
    BrokenSymlinks("broken_symlinks", defaultChecked = false, needsRoot = true),

    /**
     * `.db`/`.sqlite`: تُقمَّم (VACUUM) عبر `sqlite3` — لا تُمسح البيانات.
     *
     * **ومطفأة افتراضيًّا:** التنفيذ يعتمد على `sqlite3` في صدفة مميَّزة، وهي لا توجد على كل
     * جهاز — والقاعدة أن ما لا يمكن التحقق من تنفيذه لا يُشّر افتراضيًّا ثم يُقال «تعذّر».
     */
    Databases("databases", defaultChecked = false, needsRoot = true),

    /** ما أُزيل وذهب إلى السلة: للسلة معنى الاسترجاع، فمحوها تدميرٌ لا تنظيف. مطفأة. */
    RecycleBin("recycle_bin", defaultChecked = false, needsRoot = true),
}

/**
 * قياس فئة واحدة.
 *
 * @param bytes المساحة بالبايت، أو `null` إن **فشل القياس** — لا صفرًا. وهذا الفرق هو كل
 *        الفرق بين «لا شيء» و«لم أعرف»، وقد بُني عليه المجموع كله.
 * @param count عدد العناصر المقيسة. مفيد لفئة **حجمها صفر بطبيعتها** (`EmptyFolders`): عدد
 *        المجلدات هو كل الخبر، فلا يُعرض لها «0 B».
 */
data class CleanMeasurement(
    val category: StorageCareCategory,
    val bytes: Long?,
    val count: Int,
)

/** مجموعة قياسات الشاشة، والفشل فيها لا يُسقط الجلسة. */
data class CleanMeasurementSet(val rows: List<CleanMeasurement> = emptyList()) {

    fun of(category: StorageCareCategory): CleanMeasurement? = rows.firstOrNull { it.category == category }

    /** هل فشل قياس واحدة على الأقل؟ يغذّي سطر الصدق في الشاشة («الفئة — وسبب قصير»). */
    val anyFailed: Boolean get() = rows.any { it.bytes == null }
}

/**
 * مجموع المختار كما يُعرض على الزرّ.
 *
 * @param bytes ما قيس فعلًا من المختار.
 * @param hasUnmeasured هل في المختار فئة فشل قياسها؟ — فالمجموع **حدّ أدنى** لا مجموع،
 *        ويُقال ذلك في الشاشة بدل أن يُقدَّم كأنه الكل.
 */
data class CleanTotal(val bytes: Long, val hasUnmeasured: Boolean) {
    val hasAnything: Boolean get() = bytes > 0L
}

/**
 * هدف واحد داخل فئة تنظيف — **السطر الذي يقول ما سيُحذف قبل أن يُحذف**.
 *
 * وُجد بأمر المالك: «الضغط على كاش التخزين المشترك لا يظهر لي بالضبط ما هو الذي سيُمسح
 * منها أي الملفات». فالشفافية ليست شرحًا في مستند، بل صفوفٌ تُقرأ: اسم التطبيق (أو المجلد)
 * ومقاسه ومساره.
 *
 * @param label اسم العرض — اسم تطبيق، أو اسم مجلد/ملفّ.
 * @param path المسار الكامل حين يكون معروفًا. القرار في النموذج لا في الشاشة.
 * @param packageName اسم الحزمة إن كان الهدف تطبيقًا — **للأيقونة وحدها**؛ ومن لا حزمة له
 *        (مجلد، ملفّ APK) يأخذ رمز نوعه.
 * @param bytes المقاس، أو `null` إن فشل القياس — نفس عقد `null` ليست صفرًا.
 */
data class CareTarget(
    val label: String,
    val path: String? = null,
    val packageName: String? = null,
    val bytes: Long? = null,
)

/**
 * سرد فئة كما يُعرض: صفوفٌ معروضة، وما طُوي تحتها مجموعًا، والمقيس من الكل.
 *
 * **والطيّ لا يُخفي رقمًا:** ما زاد عن حدّ العرض يُعرض صفًّا واحدًا (عدده ومجموعه)،
 * والمجموع الكلي محسوب من **كل** الصفوف لا من المعروضة وحدها.
 *
 * @param failed تعذّر السرد أصلًا (لا جذر، أو فشل أمر) — وهو غير «سرد فارغ»: الشاشة
 *        تقول «تعذّرت القراءة» ولا تقول «لا شيء هنا».
 */
data class CareTargetList(
    val rows: List<CareTarget> = emptyList(),
    val restCount: Int = 0,
    val restBytes: Long = 0L,
    val measuredBytes: Long = 0L,
    val anyUnmeasured: Boolean = false,
    val failed: Boolean = false,
) {
    /** لا صفوف ولا مطويّات وبلا فشل: الفئة خالية فعلًا. */
    val isEmpty: Boolean get() = !failed && rows.isEmpty() && restCount == 0

    val totalItems: Int get() = rows.size + restCount
}

object StorageCareModel {

    /** كم صفًّا يُعرض من سرد الفئة قبل طيّ الباقي — عشرون تكفي ورقةً وتُقرأ. */
    const val TARGET_ROWS = 20

    /** المجموعة الآمنة: كل ما هو مُشّر افتراضيًّا — الكاش وبقايا المُزالة والصور والسجلات. */
    fun safeSelection(): Set<StorageCareCategory> =
        StorageCareCategory.entries.filter { it.defaultChecked }.toSet()

    /**
     * هل تُعرض هذه الفئة الآن؟
     *
     * العقد يقول: المقفل **يُعرض ويقول ما ينقصه**، لا يختفي. لكن هنا استثناءٌ مقصود
     * ومكتوب: فئات الجذر الأربع مساراتها خارج ما يقرؤه uid التطبيق أصلًا، فعرضها كصفوف
     * معطّلة يعني صفوفًا **لا يمكن أن تمتلئ أبدًا في هذا الوضع** — أي دعوة دائمة لتحذير.
     * ولذلك تُخفى هذه الأربع، ويبقى **سطر النطاق** أعلى الشاشة يقول للمستخدم إن الجذر
     * يوسّع التنظيف، ويسمّي ما يضيفه.
     */
    fun visibleAt(rootAvailable: Boolean, category: StorageCareCategory): Boolean =
        rootAvailable || !category.needsRoot

    /**
     * المجموع المُعلن على الزرّ.
     *
     * وقياسان مقصودان:
     * - فئة **مطفأة** لا تدخل المجموع ولو قيست (الزرّ يقول ما سيُحذف لا ما هو موجود).
     * - فئة **فشل قياسها** لا تدخل الرقم، وإنما ترفع `hasUnmeasured`.
     */
    fun total(
        selection: Set<StorageCareCategory>,
        measurements: CleanMeasurementSet,
    ): CleanTotal {
        var sum = 0L
        var unmeasured = false
        for (category in selection) {
            val row = measurements.of(category) ?: continue
            val bytes = row.bytes
            if (bytes == null) unmeasured = true else sum += bytes
        }
        return CleanTotal(bytes = sum, hasUnmeasured = unmeasured)
    }

    /**
     * هل تُعرض هذه الفئة بعدد لا بحجم؟ (المجلدات الفارغة حجمها صفر بالتعريف).
     *
     * والتمييز هنا لا في الشاشة: لو عرضت الشاشة «0 B» لمجلدات فارغة لقالت للمستخدم إن هناك
     * ما يُحرَّر وهو لا يحرّر شيئًا — والعدد (٤ مجلدات) هو الخبر الصحيح.
     */
    fun showsCount(category: StorageCareCategory): Boolean =
        category == StorageCareCategory.EmptyFolders || category == StorageCareCategory.BrokenSymlinks

    /**
     * يثبّت أهداف فئة في قائمة عرض: **الأكبر أولًا، ثم بالاسم**، وما زاد عن [limit] يُطوى.
     *
     * وثلاثة قرارات مقيسة:
     * - **غير المقيس آخرًا** لا أولًا: `null` تعني «لم أعرف» لا «صفر»، فترتيبها في المقدّمة
     *   يوهم بأنها الأكبر. والقيمة الحارسة `Long.MIN_VALUE` تُنزلها وحدها بلا فرع ثانٍ.
     * - **التعادل يُكسر بالاسم**: قائمتان بنفس الحجم يجب أن تنتجا نفس الترتيب في كل فتح،
     *   وإلا بدت القائمة تتغيّر من نفسها.
     * - **والمطويّ يُجمع**: عدده ومجموعه يُعرضان في صفّ واحد، فلا يختفي رقم.
     */
    fun foldTargets(rows: List<CareTarget>, limit: Int = TARGET_ROWS): CareTargetList {
        val ranked = rows.sortedWith(
            compareByDescending<CareTarget> { it.bytes ?: Long.MIN_VALUE }.thenBy { it.label },
        )
        val safeLimit = limit.coerceAtLeast(0)
        val shown = ranked.take(safeLimit)
        val rest = ranked.drop(safeLimit)
        return CareTargetList(
            rows = shown,
            restCount = rest.size,
            restBytes = rest.sumOf { it.bytes ?: 0L },
            measuredBytes = rows.sumOf { it.bytes ?: 0L },
            anyUnmeasured = rows.any { it.bytes == null },
        )
    }

    /** سردٌ تعذّر أصلًا — «لم أقرأ» لا «لا شيء». */
    fun failedTargets(): CareTargetList = CareTargetList(failed = true)

    /**
     * سطر واحد من مخرج `du -sk`: `1234\t/path/to/dir`.
     *
     * والوحدة كيلوبايت (١٠٢٤ بايتًا) كما يعرّفها `-k`، والتحويل هنا لا في طبقة التنفيذ:
     * دالّة نقيّة تُقاس في JVM بمخرجات حقيقية من toybox، ومن سطر مشوّه تعود `null` ولا
     * تُخترع له قيمة. (والسطر الذي لا مسار فيه ليس قياسًا: `du` قد يطبع أسطرًا لا تخصّ
     * هدفًا.)
     */
    fun parseDuRow(line: String): Pair<String, Long>? {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return null
        val split = trimmed.indexOfFirst { it == '\t' || it == ' ' }
        if (split <= 0) return null
        val kilobytes = trimmed.take(split).toLongOrNull() ?: return null
        if (kilobytes < 0L) return null
        val path = trimmed.drop(split + 1).trim()
        if (path.isEmpty()) return null
        return path to kilobytes * 1024L
    }

    /**
     * اسم الحزمة من مسار كاش: `/data/data/<pkg>/cache` أو `…/Android/data/<pkg>/cache`.
     *
     * **والشكل هو الحكم لا القائمة:** سرد `du` يأتي من قالب ثابت يعرفه المحرّك (`<parent>/<pkg>/cache`)،
     * فما لا ينتهي بـ`/cache` وباسم حزمة قبله لا يُنسب إلى تطبيق — ويُعرض بمساره وحدها
     * ولا يُنسب خطأً لتطبيق لم يُسمَّ.
     */
    fun packageOfCachePath(path: String): String? {
        val parts = path.trim().trimEnd('/').split('/')
        if (parts.size < 3) return null
        if (parts.last() != "cache") return null
        val parent = parts[parts.size - 2]
        // و`data`/`cache` ليستا حزمتين: القالب سليم لا يعني أن كل ما يسبق `cache` اسم حزمة.
        return parent.takeIf { it.isNotBlank() && it != "data" && it != "cache" }
    }
}
