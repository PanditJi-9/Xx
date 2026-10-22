/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.core.gamespace

import kotlin.math.exp
import kotlin.math.pow

/**
 * لقطة واحدة من الدخول السينمائيّ للوبي. كل الحقول 0..1 ومحسوبة من الزمن وحده؛ الرسم لا يعرف أي
 * رقم زمنيّ (نفس فصل [LobbyDeckMath] عن رسم الحلقة).
 *
 * @property warp تقدّم الاندفاع: يتسارع حتى لحظة الوميض (تتقارب الحلقات وتطول الخطوط).
 * @property tunnel شفافية مشهد الاندفاع: 1 حتى الوميض ثم يختفي خلفه في ~١٤٠ms.
 * @property flash شدّة الوميض: يصعد في ١٦٠ms قبل الذروة ويخبو أسّيًّا بعدها.
 * @property emblem ظهور الشعار والعنوان بعد الوميض (تباطؤ في النهاية).
 * @property veil عتمة الغطاء كلّه: 1 حتى [LobbyEntranceTimeline.VEIL_OUT_START_MS] ثم يتلاشى فيظهر اللوبي.
 */
data class EntranceFrame(
    val warp: Float,
    val tunnel: Float,
    val flash: Float,
    val emblem: Float,
    val veil: Float
)

/**
 * خط زمن الدخول: اندفاع ضوئيّ ← وميض ← ظهور الشعار ← تلاشٍ إلى اللوبي.
 *
 * ### ارتباط بالصوت
 * [IMPACT_MS] و[TOTAL_MS] **مطابقان حرفيًّا** لثابتَي `tools/gen_lobby_sounds.py` (ذروة الباص والضربة
 * تقعان لحظة الوميض). يفرض `LobbyEntranceTimelineTest` هذا التطابق بقراءة السكربت، فلا ينفصل الصوت عن
 * الصورة بتعديل أحدهما فقط.
 *
 * ### ميزانية الحركة
 * مقدّمة اللوبي العاديّة ≤ ٣٦٠ms (`MaxDuration`)؛ هذا الدخول **استثناء مقصود** بطلب صريح من المالك
 * (٢٠٢٦-١٠-٢١)، ولذلك: مفتاحه في خيارات اللوبي، ويُلغى كليًّا حين يكون مقياس أنيميشن النظام صفرًا،
 * وتخطّيه بلمسة أو رجوع أو أي زرّ ريموت.
 */
object LobbyEntranceTimeline {
    /** لحظة الوميض وذروة الصوت (ms من البداية). */
    const val IMPACT_MS = 2100

    /** مدّة الدخول كاملةً، وهي أيضًا طول `lobby_intro.ogg`. */
    const val TOTAL_MS = 3400

    /** تلاشي التخطّي (يتزامن مع خفض صوت المقدّمة). */
    const val SKIP_FADE_MS = 220

    /** بداية تلاشي الغطاء عن اللوبي. */
    const val VEIL_OUT_START_MS = 2800

    private const val FLASH_RISE_MS = 160
    private const val FLASH_DECAY_MS = 560
    private const val TUNNEL_CUT_MS = 140
    private const val EMBLEM_DELAY_MS = 60
    private const val EMBLEM_MS = 760
    private const val WARP_CURVE = 2.2f
    private const val FLASH_DECAY_RATE = 3.2f

    /** لقطة عند [elapsedMs]؛ خارج المدى تُقصّ إلى أقرب طرف فلا تنتج قيمًا خارج 0..1. */
    fun frame(elapsedMs: Float): EntranceFrame {
        val t = elapsedMs.coerceIn(0f, TOTAL_MS.toFloat())
        return EntranceFrame(
            warp = warp(t),
            tunnel = tunnel(t),
            flash = flash(t),
            emblem = emblem(t),
            veil = veil(t)
        )
    }

    /** يتسارع (`p^2.2`) فيبدو الاندفاع كأنه يأخذ سرعته؛ يثبت عند 1 بعد الوميض. */
    fun warp(t: Float): Float = (t / IMPACT_MS).coerceIn(0f, 1f).pow(WARP_CURVE)

    fun tunnel(t: Float): Float =
        if (t < IMPACT_MS) 1f else (1f - (t - IMPACT_MS) / TUNNEL_CUT_MS).coerceIn(0f, 1f)

    fun flash(t: Float): Float {
        val riseStart = (IMPACT_MS - FLASH_RISE_MS).toFloat()
        return when {
            t < riseStart -> 0f
            t < IMPACT_MS -> {
                val s = (t - riseStart) / FLASH_RISE_MS
                s * s
            }
            else -> exp(-FLASH_DECAY_RATE * (t - IMPACT_MS) / FLASH_DECAY_MS)
        }
    }

    /** تباطؤ ثلاثيّ (`1-(1-e)^3`): يندفع الشعار أولًا ثم يستقرّ. */
    fun emblem(t: Float): Float {
        val e = ((t - IMPACT_MS - EMBLEM_DELAY_MS) / EMBLEM_MS).coerceIn(0f, 1f)
        return 1f - (1f - e).pow(3)
    }

    fun veil(t: Float): Float =
        1f - ((t - VEIL_OUT_START_MS) / (TOTAL_MS - VEIL_OUT_START_MS)).coerceIn(0f, 1f)
}
