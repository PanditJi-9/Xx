/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.spoof

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences

/**
 * مخزن IMEI لكلّ تطبيق: ملفّ تفضيلات واحد يكتبه التطبيق ويقرؤه خطّاف LSPosed داخل عملية التطبيق الهدف.
 *
 * ### لماذا ملفّ قابل للقراءة للجميع
 * الخطّاف يعمل داخل عملية التطبيق الهدف لا عمليتنا، فلا يصل إلى مجلّد بياناتنا الخاصّ. الإطار (LSPosed) يسمح
 * لوحدته وحدها بـ`MODE_WORLD_READABLE`؛ وأيّ تطبيق عادي يُرفض فيه ذلك بـ`SecurityException`.
 *
 * ### الصدق (ADR-07)
 * إن رُفض الوضع فالوحدة **غير مفعّلة في LSPosed**، والملفّ الذي نكتبه خاصّ لا يقرؤه الخطّاف أبدًا. لذلك
 * [hookReachable] تقول ذلك للواجهة بدل أن يُعرض مفتاحٌ يعمل ظاهرًا ولا أثر له.
 *
 * لا قراءة لأيّ IMEI حقيقي هنا؛ القيم مولَّدة عشوائيًّا ([ImeiSpoof.generate]).
 */
class ImeiSpoofStore(context: Context) {
    private val prefs: SharedPreferences
    val hookReachable: Boolean

    init {
        val app = context.applicationContext
        var opened: SharedPreferences
        var reachable: Boolean
        try {
            opened = openWorldReadable(app)
            reachable = true
        } catch (_: SecurityException) {
            opened = app.getSharedPreferences(ImeiSpoof.PREFS_NAME, Context.MODE_PRIVATE)
            reachable = false
        }
        prefs = opened
        hookReachable = reachable
    }

    /** IMEI هذا التطبيق إن كان التزييف مفعّلًا وقيمته سليمة، وإلا `null`. */
    fun imeiFor(packageName: String): String? =
        prefs.getString(ImeiSpoof.key(packageName), null)?.takeIf(ImeiSpoof::isValid)

    /** يفعّل التزييف ويُبقي القيمة الحالية إن كانت سليمة (فلا يتغيّر IMEI التطبيق عند كلّ تشغيل). */
    fun enable(packageName: String): String {
        imeiFor(packageName)?.let { return it }
        return regenerate(packageName)
    }

    /** يولّد قيمة جديدة لهذا التطبيق ويحفظها. */
    fun regenerate(packageName: String): String {
        val fresh = ImeiSpoof.generate()
        prefs.edit().putString(ImeiSpoof.key(packageName), fresh).apply()
        return fresh
    }

    /** يُطفئ التزييف ويمحو القيمة، فيرى التطبيق IMEI الحقيقي من جديد. */
    fun disable(packageName: String) {
        prefs.edit().remove(ImeiSpoof.key(packageName)).apply()
    }

    @Suppress("DEPRECATION")
    @SuppressLint("WorldReadableFiles")
    private fun openWorldReadable(context: Context): SharedPreferences =
        context.getSharedPreferences(ImeiSpoof.PREFS_NAME, Context.MODE_WORLD_READABLE)
}
