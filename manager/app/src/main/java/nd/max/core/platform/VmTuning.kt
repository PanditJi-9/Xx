/*
 * Copyright (C) 2026-2027 MaxManager contributors
 *
 * Licensed under the Apache License, Version 2.0.
 */
package nd.max.core.platform

import nd.max.core.hardware.RootFileAccess

/**
 * مقابض `/proc/sys/vm` التي تُضبط فعلًا على نواة أندرويد — **بحدود لا بعدد**.
 *
 * **ولماذا حدود هنا لا في الشاشة:** القيمة الخارجة عن المدى ليست «قيمة قويّة»، هي نواة تتوقّف عن
 * الكتابة إلى القرص أو عن قتل عمليّة تنفد ذاكرتها. فالقصّ الصامت ممنوع، والرفض المُعلن هو السلوك:
 * `dirty_ratio = 0` مثلًا يُلغي خنق الكتابة القذرة كلّيًا.
 *
 * **والاختيار من FKM لا نسخًا:** مجموعة `/proc/sys/vm` عنده ١٧ مسارًا، وأغلبها مكرّر المعنى؛ هنا
 * ما يُغيّر سلوكًا ملحوظًا ويوجد على نواة AOSP، والباقي في تقرير الاستخراج لا في الكود.
 *
 * @property min أدنى قيمة مقبولة (شاملة) — مشتقّة من معنى المعامل في النواة.
 * @property max أعلى قيمة مقبولة (شاملة).
 * @property step خطوة زرّ واحد في الشاشة — مشتقّة من مقياس المعامل لا من الذوق.
 */
enum class VmTunable(val path: String, val min: Long, val max: Long, val step: Long) {
    /** ميل النواة لتبديل الصفحات؛ ٢٠٠ هو أقصى ما تقبله النواة الحديثة. */
    SWAPPINESS("/proc/sys/vm/swappiness", 0, 200, 10),

    /** نسبة الذاكرة القذرة التي يبدأ عندها التباطؤ. الصفر يُلغي الخنق ⇒ مرفوض. */
    DIRTY_RATIO("/proc/sys/vm/dirty_ratio", 1, 100, 5),

    /** نسبة البدء المبكر للكتابة الخلفية. */
    DIRTY_BACKGROUND_RATIO("/proc/sys/vm/dirty_background_ratio", 1, 100, 5),

    /** دوريّة كاتب الخلفية بالسنتي ثانية (١٠٠ = ثانية واحدة). */
    DIRTY_WRITEBACK_CENTISECS("/proc/sys/vm/dirty_writeback_centisecs", 1, 60_000, 100),

    /** ميل إعادة تدوير الـinode/dentry؛ ١٠٠ متوازن، والأعلى يفضّل الاحتفاظ بالكاش. */
    VFS_CACHE_PRESSURE("/proc/sys/vm/vfs_cache_pressure", 1, 1_000, 10),

    /** احتياطي الذاكرة الحرّة بالكيلوبايت — **الخطر الأعلى**: الكبير جدًّا يخنق التخصيص. */
    MIN_FREE_KBYTES("/proc/sys/vm/min_free_kbytes", 1_024, 1_048_576, 1024),

    /** احتياطي إضافيّ يستعمله أندرويد فوق `min_free_kbytes` (غير موجود على كل نواة). */
    EXTRA_FREE_KBYTES("/proc/sys/vm/extra_free_kbytes", 0, 1_048_576, 1024),

    /** نسبة التجاوز المسموح بها عند `overcommit_memory=2` فقط. */
    OVERCOMMIT_RATIO("/proc/sys/vm/overcommit_ratio", 0, 100, 5),

    /** مقياس علامات الماء؛ ١٠ هو الأدنى الذي تقبله النواة عمليًّا. */
    WATERMARK_SCALE_FACTOR("/proc/sys/vm/watermark_scale_factor", 10, 1_000, 10),

    /** ١ = اسمح لعمليّة التخصيص بقتل نفسها عند نفاد الذاكرة. */
    OOM_KILL_ALLOCATING_TASK("/proc/sys/vm/oom_kill_allocating_task", 0, 1, 1),
}

/** نتيجة محاولة كتابة — لا `Boolean` واحد يخلط الرفض بالفشل. */
enum class VmWriteOutcome {
    /** كُتبت القيمة، وأعادت القراءة نفسها. */
    APPLIED,

    /** القيمة خارج حدود [VmTunable] — **لم تُكتب**. */
    REJECTED_RANGE,

    /** النصّ ليس عددًا صحيحًا، أو محاولة على مدخل غير موجود. */
    REJECTED_INVALID,

    /** العقدة غير موجودة أو غير قابلة للكتابة بأي طبقة امتياز. */
    REJECTED_NOT_WRITABLE,

    /** قَبِل المسار الكتابة ولم تعُد القراءة القيمة ⇒ لا يُدَّعى نجاح. */
    UNVERIFIED,
}

/**
 * ضبط `/proc/sys/vm` عبر **مسار الكتابة الوحيد** ([RootFileAccess]، الذي يحمل إيصالًا بطبقته).
 *
 * والقاعدة: لا كتابة بلا تحقّق قراءة-بعد-كتابة. النواة قد تُقرّب القيمة أو ترفضها بلا خطأ،
 * و«قَبِل الملفّ» ليس «تغيّرت القيمة».
 */
class VmTuning(
    private val read: (String) -> String?,
    private val write: (String, String) -> Boolean,
) {

    fun current(tunable: VmTunable): Long? = parse(read(tunable.path))

    fun snapshot(): Map<VmTunable, Long?> = VmTunable.entries.associateWith(::current)

    /**
     * يكتب [value] في [tunable] ويتحقّق.
     *
     * ولا يمسّ شيئًا إن كانت القيمة خارج المدى أو المدخل غير موجود — الفحص **قبل** الكتابة.
     */
    fun set(tunable: VmTunable, value: Long): VmWriteOutcome {
        if (value < tunable.min || value > tunable.max) return VmWriteOutcome.REJECTED_RANGE
        val before = read(tunable.path) ?: return VmWriteOutcome.REJECTED_NOT_WRITABLE
        if (parse(before) == null) return VmWriteOutcome.REJECTED_INVALID
        val accepted = runCatching { write(tunable.path, value.toString()) }.getOrDefault(false)
        if (!accepted) return VmWriteOutcome.REJECTED_NOT_WRITABLE
        return if (current(tunable) == value) VmWriteOutcome.APPLIED else VmWriteOutcome.UNVERIFIED
    }

    companion object {
        /** السقف الذي تحتمله النواة فعلًا لمعامل ٣٢ بت. */
        const val MAX_LONG = 2_147_483_647L

        /**
         * يفكّ نصّ عقدة نواة. **لا يقبل** `"12abc"` ولا `"-"` ولا `""` — العقدة التي تُقرأ نصًّا
         * مفهومًا تُكتب، وما لا يُفهم لا يُبنى عليه قرار.
         */
        fun parse(raw: String?): Long? {
            val text = raw?.trim().orEmpty()
            if (text.isEmpty()) return null
            val negative = text.startsWith('-')
            val digits = if (negative) text.substring(1) else text
            if (digits.isEmpty() || digits.any { it !in '0'..'9' }) return null
            val magnitude = digits.toLongOrNull() ?: return null
            if (magnitude > MAX_LONG) return null
            return if (negative) -magnitude else magnitude
        }

        /** النسخة الحقيقية — القراءة والكتابة تمرّان بـ[RootFileAccess] وحده. */
        fun system(): VmTuning = VmTuning(
            read = { path -> RootFileAccess.read(path) },
            write = { path, value -> RootFileAccess.write(path, value) },
        )
    }
}
