/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.core.lab

import android.content.Context

/**
 * نتائج اختبارات المختبر — **تبقى بعد إغلاق الشاشة وبعد إعادة التشغيل**.
 *
 * ### لماذا مخزن، والنتيجة إقرار المستخدم
 *
 * الاختبار يُجرَّب مرّة، والحكم يبقى أسبوعًا. ولو عاشت النتيجة في `remember` لضاعت
 * بمجرّد الخروج من الشاشة، فيعيد المستخدم كل اختبار ليعرف ما جرّبه — وهذا يجعل
 * المختبر عرضًا لا أداة. والمخزن هنا يحفظ **قرار من جرّب** لا قياسًا آليًّا: التطبيق
 * لا يسمع السماعة ولا يرى الشاشة، فلا يصحّ أن يكتب «يعمل» من نفسه (ADR-07).
 *
 * ### وما لا يُحفظ
 *
 * لا يُحفظ زمن التشغيل ولا حالة الجهاز ولا أي قراءة حيّة — تلك تُقاس في كل فتح.
 * المخزن يعرف ثلاثة أشياء فقط: أي اختبار، وما حكمه، ومتى.
 */
object LabResultStore {

    private const val PREFS = "device_lab_results"
    private const val KEY_PREFIX = "test_"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /**
     * كل نتيجة محفوظة، بمفتاح اسم الاختبار كما تكتبه الشاشة.
     *
     * وما لا يُفكّ يُسقَط بدل أن يُعاد كسجلّ فارغ: الشاشة تعرض «لم يُجرَّب» للغائب،
     * وهذا صادق؛ إعادته كسجلّ بقيمة مجهولة يجعل البطاقة بلا كلمة.
     */
    fun load(context: Context): Map<String, LabRecord> {
        val all = prefs(context).all
        val out = LinkedHashMap<String, LabRecord>(all.size)
        all.forEach { (key, value) ->
            if (!key.startsWith(KEY_PREFIX)) return@forEach
            val record = decodeLabRecord(value as? String) ?: return@forEach
            out[key.removePrefix(KEY_PREFIX)] = record
        }
        return out
    }

    /** يسجّل حكمًا واحدًا بزمنه. الكتابة `apply()` غير متزامنة فلا تحجب الواجهة. */
    fun set(context: Context, testId: String, outcome: String, atMs: Long) {
        prefs(context).edit()
            .putString(KEY_PREFIX + testId, encodeLabRecord(LabRecord(outcome, atMs)))
            .apply()
    }

    /** يمسح حكمًا واحدًا — فيعود الاختبار «لم يُجرَّب» لا «معطوب». */
    fun clear(context: Context, testId: String) {
        prefs(context).edit().remove(KEY_PREFIX + testId).apply()
    }
}
