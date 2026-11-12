/*
 * Copyright (C) 2026-2027 Zexshia
 * Licensed under the Apache License, Version 2.0 (see LICENSE).
 */

package nd.max.ui.util

/**
 * بوّابة إعادة الرسم: تفصل معدّل وصول العيّنات عن معدّل تحديث المراقب.
 *
 * المسجّل يقرأ كل ثانية، والمراقب قد يريد كل ثلاث ثوانٍ. بدون بوّابة صريحة يصير
 * «الفاصل» وعدًا في الإعدادات لا أثر له في الشاشة. والبوّابة صافية: مدخلها الزمن
 * ومخرجها نعم/لا، فتُختبر بلا ساعة ولا جهاز.
 */
class MonitorGate(intervalMs: Int = DEFAULT_INTERVAL_MS) {

    var intervalMs: Int = intervalMs.coerceAtLeast(0)
        private set

    private var lastAcceptedMs = UNSET

    /** هل يمرّ التحديث الآن؟ الأوّل يمرّ دائمًا، وما بعده بعد انقضاء الفاصل. */
    fun accept(nowMs: Long): Boolean {
        if (lastAcceptedMs != UNSET && nowMs - lastAcceptedMs < intervalMs) return false
        lastAcceptedMs = nowMs
        return true
    }

    /** تغيير الفاصل أثناء العمل: يبقى آخر قبول، ويُقاس التالي من جديد. */
    fun updateInterval(value: Int) {
        intervalMs = value.coerceAtLeast(0)
    }

    fun reset() {
        lastAcceptedMs = UNSET
    }

    companion object {
        const val DEFAULT_INTERVAL_MS = 1000

        /** قيمة لا تصلها ساعة الجهاز، فلا يُحسب أوّل قبول ضدّ فاصل وهمي. */
        private const val UNSET = Long.MIN_VALUE / 2
    }
}
