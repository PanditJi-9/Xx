/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.core.gamespace

/**
 * حساب القصّ **قبل** لمس أي وسيط: نقيّ فيُقاس في JVM بلا جهاز ولا ملفّ.
 *
 * والقواعد هنا ليست تجميلًا: أقصر مقطع يُحفظ ثانية واحدة (وإلا صار الملفّ لقطةً عابرة لا
 * مقطعًا)، والمقابض تتحرّك بنصف ثانية (دقّة كافية بلا تقطيع مجهري)، والحدود تُقصّ لا تُرمى
 * — فمقبض جرّه المستخدم إلى ما بعد النهاية يُعاد إلى النهاية بدل أن يُرفض الطلب كله.
 */
object ClipTrimMath {

    /** أقصر ما يُحفظ: ثانية واحدة. */
    const val MIN_KEEP_MS: Long = 1_000L

    /** خطوة المقابض: نصف ثانية. */
    const val STEP_MS: Long = 500L

    /** هل في المقطع ما يكفي للقصّ؟ المدة المجهولة ليست صفرًا — لكنها أيضًا ليست صالحة هنا. */
    fun canTrim(durationMs: Long?): Boolean = durationMs != null && durationMs >= MIN_KEEP_MS * 2

    /** بداية صالحة داخل `[0, duration - MIN_KEEP_MS]`. */
    fun clampStart(startMs: Long, durationMs: Long): Long {
        val maxStart = (durationMs - MIN_KEEP_MS).coerceAtLeast(0L)
        return snap(startMs).coerceIn(0L, maxStart)
    }

    /** نهاية صالحة داخل `[start + MIN_KEEP_MS, duration]`. */
    fun clampEnd(endMs: Long, startMs: Long, durationMs: Long): Long {
        val minEnd = (startMs + MIN_KEEP_MS).coerceAtMost(durationMs)
        val maxEnd = durationMs.coerceAtLeast(minEnd)
        return snap(endMs).coerceIn(minEnd, maxEnd)
    }

    /** ما بقي بعد القصّ — لا يقلّ عن صفر حتى لو جاءت الأطراف معكوسة. */
    fun keptMs(startMs: Long, endMs: Long): Long = (endMs - startMs).coerceAtLeast(0L)

    /** موضع المللي ثانية على شريط من `0f` إلى `1f`. */
    fun fraction(ms: Long, durationMs: Long): Float =
        if (durationMs <= 0L) 0f else (ms.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)

    /** الموضع المعاكس، مع نفس التقريب بالنصف ثانية. */
    fun fromFraction(fraction: Float, durationMs: Long): Long {
        if (durationMs <= 0L) return 0L
        val raw = (fraction.coerceIn(0f, 1f) * durationMs).toLong()
        return snap(raw).coerceIn(0L, durationMs)
    }

    /** تقريب لأقرب خطوة — والطرح لا يستخدم `%` كي لا ينكسر على القيم السالبة. */
    private fun snap(ms: Long): Long {
        if (ms <= 0L) return 0L
        return ((ms + STEP_MS / 2) / STEP_MS) * STEP_MS
    }
}
