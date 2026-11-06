/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.policy

import android.content.Context

/**
 * إقرار المخاطر: من فتح ميزة خطرة مرةً واحدةً صار مسؤولًا عنها، ويُسجَّل الإقرار بزمنه.
 *
 * **ولماذا لا يكفي مربّع «أوافق» في كل مرة:** تكرار السؤال يُعلّم المستخدم الضغط بلا قراءة،
 * فيصير الحاجز طقسًا لا تحذيرًا. الإقرار **مرة واحدة لكل ميزة** يُقرأ عند أول فتح، ويُسجَّل
 * وقتُه ليُعرض لاحقًا («أقررت في…») بدل أن يُنسى. ولا يُلغى تلقائيًّا: من أراد التراجع
 * يستدعي [revoke] صريحًا.
 *
 * **وما لا يفعله هذا الملف:** لا يقرّر أيّ ميزة خطرة — يُخزّن الإقرار فقط. والقائمة نفسها في
 * الشاشة التي تعرض التحذير، لأن الخطورة وصفُ الفعل لا وصفُ التخزين.
 *
 * **والحدّ الصادق:** الإقرار لا يجعل الفعل آمنًا، ولا يمنح إذنًا نظاميًّا؛ الكتابة الفعلية تبقى
 * على مسار الـarbiter وصلاحيات المنصّة، وما يمنعه النظام لا يفتحه إقرارُ المستخدم.
 */
object DangerConsent {

    /** إعادة تشغيل التطبيق التلقائي للخطير بعد أن أوقفه حارس الإقلاع. */
    const val RESTORE_DANGEROUS = "restore_dangerous_auto"

    /** كتابة حدود تردد المعالج على عقد سياسة النواة (scaling_min/max_freq). */
    const val CPU_FREQ_LIMITS = "cpu_freq_limits"

    private const val PREFS = "maxmanager_danger_consent"
    private const val KEY = "acknowledged"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** زمن الإقرار بالملّي ثانية، أو صفر إن لم يُقرّ بعد. */
    fun acknowledgedAt(context: Context, id: String): Long =
        decode(prefs(context).getString(KEY, null))[id] ?: 0L

    fun isAcknowledged(context: Context, id: String): Boolean = acknowledgedAt(context, id) > 0L

    /** يسجّل الإقرار بزمنه — ويحفظ الأقدم إن كان مسجَّلًا (الإقرار الأول هو الدليل). */
    fun acknowledge(context: Context, id: String, atMs: Long = System.currentTimeMillis()) {
        if (id.isBlank() || atMs <= 0L) return
        val current = decode(prefs(context).getString(KEY, null))
        if (current[id] != null) return
        prefs(context).edit().putString(KEY, encode(current + (id to atMs))).apply()
    }

    fun revoke(context: Context, id: String) {
        val current = decode(prefs(context).getString(KEY, null))
        if (current[id] == null) return
        val next = current - id
        prefs(context).edit()
            .apply { if (next.isEmpty()) remove(KEY) else putString(KEY, encode(next)) }
            .apply()
    }

    /**
     * ترميز `id=زمن;id=زمن` مرتّبًا بالمفتاح.
     *
     * **ولماذا ليس JSON:** السجل خريطة نصّية صغيرة تُقرأ في كل مرة تُفتح الشاشة، وترميزٌ بلا
     * مكتبة لا يفشل عند تغيّر إصدار ولا يحتاج ترحيلًا. والقيم غير الصالحة تُسقَط عند الفكّ
     * ([decode]) فملفٌّ تعرّض لتعديل يدويّ لا يُسقط التطبيق.
     */
    internal fun encode(values: Map<String, Long>): String =
        values.entries
            .filter { (id, at) -> id.isNotBlank() && at > 0L }
            .sortedBy { it.key }
            .joinToString(";") { (id, at) -> "$id=$at" }

    /**
     * فكّ الترميز. كل ما لا يُفهم يُسقَط: مدخل بلا فاصل، زمن غير رقمي أو غير موجب، معرّف
     * فارغ. والفارغ أو `null` يعود خريطةً فارغة — أي «لا إقرار»، وهو الفشل الآمن.
     */
    internal fun decode(text: String?): Map<String, Long> {
        if (text.isNullOrBlank()) return emptyMap()
        val out = LinkedHashMap<String, Long>()
        text.split(';').forEach { entry ->
            val at = entry.lastIndexOf('=')
            if (at <= 0 || at == entry.lastIndex) return@forEach
            val id = entry.substring(0, at).trim()
            val ms = entry.substring(at + 1).trim().toLongOrNull()
            if (id.isEmpty() || ms == null || ms <= 0L) return@forEach
            // الأول يفوز: تكرار المعرّف لا يُعيد كتابة زمن إقرار سابق.
            out.putIfAbsent(id, ms)
        }
        return out
    }
}
