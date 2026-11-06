/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.persist

import android.content.Context
import org.json.JSONObject

/**
 * لقطة القيم السابقة: ما كانت عليه العقد **قبل** أن يكتبها التطبيق، ليمكن ردّها.
 *
 * **ولماذا لقطة واحدة لا سجلّ:** المطلوب فعل تراجع لا تاريخ يُقرأ؛ فتُحفظ آخر
 * مجموعة تغيّرت فقط، وتُستبدل في كل جولة تكتب شيئًا. والجولة التي لا تغيّر
 * شيئًا لا تمسح اللقطة القديمة — لأنها ما زالت تصف آخر تغيير وقع فعلًا.
 *
 * **ولا تُحفظ القيمة الفارغة:** عقدة كانت فارغة قبل الكتابة لا معنى لردّها فارغة
 * (بعض العقد لا تقبل الفراغ فترفض الكتابة، فيبدو التراجع فاشلًا بلا سبب).
 */
object PersistSnapshot {

    private const val PREFS = "maxmanager_persist_snapshot"
    private const val KEY = "last"
    private const val FORMAT = 1

    /** حدّ أعلى يحمي التفضيلات من لقطة ضخمة على أجهزة كثيرة العقد. */
    const val MAX_ENTRIES = 60

    /** لقطة مقروءة: زمنها والقيم السابقة كما كانت. */
    data class Snapshot(val atMs: Long, val values: Map<String, String>)

    private val current = LinkedHashMap<String, String>()

    /** يبدأ جولة كتابة جديدة. لا يمسح اللقطة المحفوظة — الاستبدال في [commit] وحده. */
    @Synchronized
    fun begin() {
        current.clear()
    }

    /** يسجّل القيمة السابقة لعقدة على وشك التغيير. */
    @Synchronized
    fun record(path: String, previous: String?) {
        val old = previous?.trim().orEmpty()
        if (old.isEmpty()) return
        if (current.size >= MAX_ENTRIES) return
        if (path in current) return
        current[path] = old
    }

    /**
     * يثبّت ما جُمع. جولة بلا تغيير **لا تمسح** اللقطة السابقة.
     */
    @Synchronized
    fun commit(context: Context, atMs: Long = System.currentTimeMillis()) {
        if (current.isEmpty()) return
        prefs(context).edit().putString(KEY, encode(current, atMs)).apply()
        current.clear()
    }

    fun load(context: Context): Snapshot? = decode(prefs(context).getString(KEY, null))

    fun clear(context: Context) {
        prefs(context).edit().remove(KEY).apply()
    }

    // ── الترميز: نقيّ يُختبر بلا أندرويد ───────────────────────────────────────────────

    internal fun encode(values: Map<String, String>, atMs: Long): String {
        val root = JSONObject()
        root.put("v", FORMAT)
        root.put("at", atMs)
        val bag = JSONObject()
        values.entries.take(MAX_ENTRIES).forEach { (path, old) -> bag.put(path, old) }
        root.put("values", bag)
        return root.toString()
    }

    internal fun decode(text: String?): Snapshot? {
        if (text.isNullOrBlank()) return null
        return runCatching {
            val root = JSONObject(text)
            if (root.optInt("v", -1) != FORMAT) return null
            val at = root.optLong("at", 0L)
            if (at <= 0L) return null
            val bag = root.optJSONObject("values") ?: return null
            val values = LinkedHashMap<String, String>()
            bag.keys().forEach { key ->
                val value = bag.optString(key).trim()
                if (key.startsWith("/") && value.isNotEmpty()) values[key] = value
            }
            if (values.isEmpty()) null else Snapshot(at, values)
        }.getOrNull()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
