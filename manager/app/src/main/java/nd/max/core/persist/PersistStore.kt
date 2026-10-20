/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.core.persist

import android.content.Context

/**
 * «حفظ خياراتك وتطبيقها بعد إعادة التشغيل» — **المفتاح وحالة الحارس**، بلا تنفيذ.
 *
 * ### ما هو، وما ليس
 *
 * ليس مخزنًا ثانيًا للإعدادات: كل إعداد يبقى في مصدره (تفضيلات التراكب، خصائص GPU Studio،
 * خصائص RAM/UFS، وملفّ الذاكرة وUFS). ويحفظ هذا الملفّ أشياءَ ثلاثة لا رابع:
 *
 * 1. **قرار المستخدم** (الميزة مفعَّلة أم لا).
 * 2. **زمن آخر جولة** — بوابة تكرار في الإقلاع الواحد، لا سجلًّا يُعرض.
 * 3. **حالة [BootLoopGuard]**: هويّة الإقلاع السابق، وعدّ الإقلاعات السريعة، ومفتاح
 *    «الخطير التلقائي» الذي يُطفأ عند الحلقة.
 *
 * وسجلّ «آخر استعادة» **أُزيل بأمر المالك**: القسم يعرض ما يفعله لا ما فعله. ويبقى الدليل في
 * سطر واحد في `MaxManager.log` (`EVENT=PERSIST_RESTORE`) — لا في واجهة تُقرأ كل مرّة.
 *
 * ### والخطير لا يُطفأ صامتًا
 *
 * `dangerousAuto` يبدأ **مفتوحًا**. وحين يبلغ عدّ الإقلاعات السريعة حدّ الحارس يُغلق، ويُكتب
 * سببه (`bootloop`) ليُعرض في الشاشة. وإعادةُ فتحه من المستخدم تُصفّر العدّ وتمسح السبب —
 * فلا يبقى جهازٌ مطفأً بعد عطبٍ زال، ولا يُفتح تلقائيًّا بلا قرار.
 */
object PersistStore {

    private const val PREFS = "maxmanager_persist"

    private const val KEY_ENABLED = "preserve_restore_enabled"
    private const val KEY_LAST_RUN = "restore_last_run_ms"
    private const val KEY_BOOT_ID = "restore_boot_id"
    private const val KEY_BOOT_AT = "restore_boot_at_ms"
    private const val KEY_FAST_STREAK = "restore_fast_boot_streak"
    private const val KEY_DANGEROUS_AUTO = "restore_dangerous_auto"
    private const val KEY_DANGEROUS_REASON = "restore_dangerous_auto_reason"

    /** سبب إغلاق التلقائي للخطير — رمز لا جملة، وتُترجمه الشاشة. */
    const val REASON_BOOTLOOP = "bootloop"

    /** الافتراضيّ **مفتوح** بنصّ الطلب: «اجعله مفعّلًا افتراضيًّا، مع توضيح وظيفته». */
    fun enabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    /** زمن آخر جولة (أيًّا كان مصدرها) — تُقرأ في بوابة التكرار وحدها. */
    fun lastRunAtMs(context: Context): Long = prefs(context).getLong(KEY_LAST_RUN, 0L)

    fun markRun(context: Context, atMs: Long) {
        prefs(context).edit().putLong(KEY_LAST_RUN, atMs).apply()
    }

    /**
     * يقرأ الإقلاع الحالي ويحفظ حكم [BootLoopGuard]، ويعيد القرار ليعرف المنفّذ.
     *
     * ويُنادى **قبل** بوابة التكرار: الإقلاع يُحسب ولو كانت الميزة مطفأة، وإلا لما كان لمن
     * أطفأها ثم أعاد فتحها بعد حلقة أيُّ تاريخ يقيس عليه.
     */
    fun observeBoot(
        context: Context,
        nowMs: Long = System.currentTimeMillis(),
        currentBootId: String? = BootLoopGuard.currentBootId(),
    ): BootLoopGuard.Decision {
        val p = prefs(context)
        val previousId = p.getString(KEY_BOOT_ID, null).orEmpty()
        val previousAt = p.getLong(KEY_BOOT_AT, 0L)
        val streak = p.getInt(KEY_FAST_STREAK, 0)
        if (currentBootId.isNullOrBlank()) {
            return BootLoopGuard.Decision(newBoot = false, streak = streak, trip = false)
        }
        val decision = BootLoopGuard.evaluate(previousId, currentBootId, previousAt, nowMs, streak)
        if (!decision.newBoot) return decision
        p.edit()
            .putString(KEY_BOOT_ID, currentBootId)
            .putLong(KEY_BOOT_AT, nowMs)
            .putInt(KEY_FAST_STREAK, decision.streak)
            .apply()
        if (decision.trip) {
            p.edit()
                .putBoolean(KEY_DANGEROUS_AUTO, false)
                .putString(KEY_DANGEROUS_REASON, REASON_BOOTLOOP)
                .apply()
        }
        return decision
    }

    /** هل تُستعاد البنود الخطيرة تلقائيًّا؟ الافتراضيّ نعم. */
    fun dangerousAutoEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DANGEROUS_AUTO, true)

    /** سبب إغلاق التلقائي (`bootloop` أو فارغ) — للعرض فقط، لا قرار. */
    fun dangerousAutoOffReason(context: Context): String? =
        prefs(context).getString(KEY_DANGEROUS_REASON, null)?.takeIf(String::isNotBlank)

    /**
     * يفتح/يغلق التلقائي للخطير بقرار المستخدم. وفتحُه **يُصفّر الحارس**: من أكّد أن جهازه
     * يعمل لا يُعاقَب بعدّ إقلاعات قديم، ومن أعاده إلى حلقة يُعاد إغلاقه بالقياس نفسه.
     */
    fun setDangerousAutoEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit()
            .putBoolean(KEY_DANGEROUS_AUTO, enabled)
            .putString(KEY_DANGEROUS_REASON, if (enabled) null else REASON_BOOTLOOP)
            .apply()
        if (enabled) {
            prefs(context).edit().putInt(KEY_FAST_STREAK, 0).apply()
        }
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

/**
 * حكم بند واحد. الحالات الأربع كما هي: لا خامسة.
 *
 * - `Applied`: طُلب الفعل **وأثبته قياس** (خدمة تعمل، أو قراءة حيّة تطابق المحفوظ).
 * - `Skipped`: لم يُطلب الفعل (الميزة مطفأة)، أو تعذّر بأمان وعُرف سببه.
 * - `Failed`: طُلب ولم يثبت — والسبب رمز مكتوب.
 * - `Pending`: بند محفوظ وإعادته من جهة كتابة أخرى (المحرّك بالجذر عند قيامه).
 */
enum class PersistStatus { Applied, Skipped, Failed, Pending }

/**
 * حكم بند في جولة استعادة.
 *
 * @param detail **رمز** لا جملة: يُكتب في السجلّ كما وُلد (`service-started` · `read-back-ok` ·
 *   `bootloop-guard`)، ولا يُترجَم في الواجهة لأن الواجهة لا تعرض السجلّ.
 */
data class PersistEntry(
    val id: String,
    val status: PersistStatus,
    val detail: String,
    val atMs: Long,
)
