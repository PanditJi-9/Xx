/*
 * Copyright (C) 2026-2027 MaxManager contributors
 *
 * Licensed under the Apache License, Version 2.0.
 */
package nd.max.core.hardware

import java.util.concurrent.ConcurrentHashMap

/** A light-weight, subsystem-agnostic desired-vs-live guard. */
class DriftGuard<T> {
    private data class Entry<T>(val desired: T, val writer: (T) -> Boolean, val reader: () -> T?)
    private val entries = ConcurrentHashMap<String, Entry<T>>()

    fun own(key: String, desired: T, writer: (T) -> Boolean, reader: () -> T?) {
        entries[key] = Entry(desired, writer, reader)
    }

    fun release(key: String) {
        entries.remove(key)
    }

    fun checkAndRepair(): List<VerificationResult<T>> {
        return entries.mapNotNull { (key, entry) ->
            val live = runCatching { entry.reader() }.getOrNull()
            if (live == entry.desired) return@mapNotNull null
            val result = VerifiedControl.apply(entry.desired, entry.writer, entry.reader)
            // المفتاح يُلحق بالخطأ لأن `VerificationResult` لا يحمله: مع عدّة مفاتيح منجرفة
            // كان الخطأ "write rejected" بلا اسم، فلا يُعرف أيّها الذي لم يُصلح.
            if (!result.successful) {
                val detail = result.error ?: "drift repair failed"
                return@mapNotNull result.copy(error = "$key: $detail")
            }
            result
        }
    }

    fun clear() = entries.clear()
}
