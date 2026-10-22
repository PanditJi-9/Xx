/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.core.gamespace

/**
 * أصوات اللوبي. اسم كل قيمة بحروف صغيرة هو اسم ملفّها `res/raw/lobby_<name>.ogg`
 * (يفرض الاختبار وجود الملفات)، ومولّدها `tools/gen_lobby_sounds.py`.
 *
 * @property minGapMs أقصر فاصل بين تشغيلين من **النوع نفسه**: يمنع تكدّس نبضة الحلقة عند سحبة سريعة
 *   أو نقرتين متلاحقتين فتنقلب الأصوات ضجيجًا.
 * @property volume مستوى التشغيل 0..1 فوق مستوى الوسائط؛ نبضة الحلقة أخفّ لأنها تتكرّر كثيرًا.
 */
enum class LobbyCue(val minGapMs: Long, val volume: Float) {
    /** لمس زرّ أو تبويب أو شريحة خيار. */
    Tap(minGapMs = 45L, volume = 0.70f),

    /** عبور بطاقة لعبة في الحلقة أثناء السحب. */
    Tick(minGapMs = 60L, volume = 0.45f),

    /** انتقال بين المسرح ومركز التحكّم (يُشغَّل أسرع صعودًا وأبطأ نزولًا). */
    Swish(minGapMs = 140L, volume = 0.55f),

    /** تفعيل مفتاح (نغمتان صاعدتان). */
    On(minGapMs = 70L, volume = 0.70f),

    /** إيقاف مفتاح (نغمتان هابطتان). */
    Off(minGapMs = 70L, volume = 0.70f),

    /** زرّ «ابدأ»: مسح صاعد وضربة. */
    Start(minGapMs = 350L, volume = 0.90f),

    /** الدخول السينمائيّ (٣٫٤ث)؛ مرّة واحدة لكل فتح، فلا فاصل. */
    Intro(minGapMs = 0L, volume = 0.85f)
}

/** قرار التشغيل النقيّ: مفتاح الأصوات + فاصل النوع. الزمن يُمرَّر من ساعة رتيبة (elapsedRealtime). */
object LobbySoundPolicy {
    fun allow(cue: LobbyCue, nowMs: Long, lastPlayedMs: Long?, enabled: Boolean): Boolean {
        if (!enabled) return false
        if (lastPlayedMs == null) return true
        val gap = nowMs - lastPlayedMs
        // فارق سالب = الساعة رجعت (إعادة تشغيل/تعديل) فلا نحجب الصوت إلى الأبد.
        return gap < 0L || gap >= cue.minGapMs
    }
}
