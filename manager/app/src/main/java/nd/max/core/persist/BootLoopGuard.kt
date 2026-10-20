/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.core.persist

import java.io.File

/**
 * حارس **حلقة الإقلاع** — حساب نقيّ، بلا أندرويد وبلا ساعة داخلية.
 *
 * ### السؤال الذي يجيبه، والسؤال الذي لا يجيبه
 *
 * لا يسأل «هل هذا الإقلاع بطيء؟» بل: **«هل الإقلاع السابق عاش أقلّ من دقيقتين؟»** — وهذا هو
 * الفرق بين إقلاع طبيعي بطيء وحلقة: الحلقة تكرّر الإقلاع نفسه قبل أن يبلغ النظام واجهته.
 * ومن هنا جاءت نافذة [FAST_REBOOT_WINDOW_MS].
 *
 * ### ولماذا `boot_id` لا فرق الزمن وحده
 *
 * `BOOT_COMPLETED` و`USER_UNLOCKED` يصلان في الإقلاع نفسه بفاصل ثوانٍ، فلو حُسب الفرق الزمني
 * وحده لَعُدّ الإقلاع الثاني «إقلاعًا سريعًا» — فيُطفأ الحفظ الآمن على جهاز سليم. و
 * `/proc/sys/kernel/random/boot_id` **يتغيّر مرّة لكل إقلاع**، فمقارنة الهويّة تفصل الحالتين
 * بلا تخمين. وما لم تُقرأ الهويّة لا يُحصى شيء: الصمت أأمن من حارس يتّهم جهازًا سليمًا.
 *
 * ### وما لا يفعله
 *
 * لا يقرّر **ماذا** يُعطَّل؛ ذلك في [PersistStore] و[PersistRestorer]. ولا يكتب ولا يقرأ حالة:
 * [evaluate] دالّة، و[PersistStore.observeBoot] هي التي تحفظ حكمها.
 */
object BootLoopGuard {

    /**
     * عمر الإقلاع السابق الذي يُعدّ «سريعًا»: دقيقتان.
     *
     * وهي نافذة الإقلاع نفسه أيضًا — فالثواني التي تفصل `BOOT_COMPLETED` عن `USER_UNLOCKED`
     * تقع داخل النافذة، لكن `boot_id` هو الذي يفصلها لا الزمن.
     */
    const val FAST_REBOOT_WINDOW_MS = 120_000L

    /** إقلاعان سريعان متتاليان يكفيان لإعلان الحلقة: الثالث هو الذي يوقف التلقائي. */
    const val FAST_REBOOTS_TO_TRIP = 2

    /** هويّة الإقلاع كما تنشرها النواة. مسار ثابت في Linux ولا يحتاج جذرًا. */
    private const val BOOT_ID_PATH = "/proc/sys/kernel/random/boot_id"

    /**
     * حكم قياس واحد.
     *
     * @param newBoot هل هذا أول قياس بعد إقلاع جديد (فيلزم حفظ الهويّة والزمن)؟
     * @param streak عدد الإقلاعات السريعة المتتالية بعد هذا القياس.
     * @param trip هل بلغ العدّ حدّ إيقاف الاستعادة التلقائية للخطير؟
     */
    data class Decision(val newBoot: Boolean, val streak: Int, val trip: Boolean)

    /**
     * يقارن قياس الإقلاع السابق بالحالي.
     *
     * - هويّة غير مقروءة في أيٍّ من الطرفين ⇒ لا حكم ([Decision.newBoot] = false): نقص قياس.
     * - الهويّة نفسها ⇒ ما زلنا في الإقلاع نفسه، والعدّ لا يتغيّر.
     * - هويّة جديدة وفرق موجب داخل النافذة ⇒ إقلاع سريع، والعدّ يزيد.
     * - هويّة جديدة وفرق خارج النافذة (أو غير موجب) ⇒ إقلاع طبيعي، والعدّ يُصفَّر.
     */
    fun evaluate(
        previousBootId: String,
        currentBootId: String,
        previousBootAtMs: Long,
        nowMs: Long,
        streak: Int,
    ): Decision {
        if (previousBootId.isBlank() || currentBootId.isBlank()) {
            // أول قياس في عمر التثبيت: يُسجَّل، ولا يُعدّ إقلاعًا سريعًا — لا سابق يُقارَن به.
            return if (previousBootId.isBlank() && currentBootId.isNotBlank()) {
                Decision(newBoot = true, streak = 0, trip = false)
            } else {
                Decision(newBoot = false, streak = streak.coerceAtLeast(0), trip = false)
            }
        }
        if (previousBootId == currentBootId) {
            return Decision(newBoot = false, streak = streak.coerceAtLeast(0), trip = false)
        }
        val gap = nowMs - previousBootAtMs
        // ساعة رجعت أو تاريخ غير مقروء: ليس دليل سرعة، فالعدّ يُصفَّر بدل أن يتّهم.
        val fast = gap in 1 until FAST_REBOOT_WINDOW_MS
        val next = if (fast) streak.coerceAtLeast(0) + 1 else 0
        return Decision(newBoot = true, streak = next, trip = next >= FAST_REBOOTS_TO_TRIP)
    }

    /** هويّة الإقلاع الحالية، أو `null` إن لم تُقرأ — والفارق يُعلَن ولا يُخمَّن. */
    fun currentBootId(): String? = runCatching { File(BOOT_ID_PATH).readText().trim() }
        .getOrNull()
        ?.takeIf(String::isNotBlank)
}
