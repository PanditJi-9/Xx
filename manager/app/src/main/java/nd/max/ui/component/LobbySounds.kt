/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.ui.component

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.delay
import nd.max.R
import nd.max.core.gamespace.LobbyCue
import nd.max.core.gamespace.LobbySoundPolicy

/*
 * أصوات اللوبي: نقر وتحريك ودخول. `SoundPool` لأن الأصوات قصيرة وتحتاج زمن استجابة منخفضًا (الحلقة
 * تطلق نبضة لكل بطاقة)، وكلها OGG صغيرة (≈٧٦KB مجتمعة) مولَّدة بالكود في `tools/gen_lobby_sounds.py`.
 *
 * - تمرّ عبر قناة **الوسائط** بصفة `USAGE_GAME`، فيتحكّم بمستواها زرّ مستوى الصوت كأي لعبة، ولا
 *   تقاطع موسيقى المستخدم (لا طلب تركيز صوتيّ).
 * - مفتاح «أصوات اللوبي» يُقرأ لحظة التشغيل لا لحظة الإنشاء، فإطفاؤه يسري فورًا بلا إعادة تحميل.
 * - حتى يُنهي `SoundPool` فكّ ترميز العيّنة تُتجاوَز النقرة بصمت (لا انتظار ولا استثناء)؛ والدخول
 *   وحده ينتظر العيّنة قليلًا ليتزامن الصوت مع الصورة ([awaitReady]).
 */

/** النقطة الوحيدة التي تعرف ملفّ كل صوت؛ بقية المنطق (الفواصل والمستوى) في [LobbyCue] النقيّ. */
private fun LobbyCue.rawRes(): Int = when (this) {
    LobbyCue.Tap -> R.raw.lobby_tap
    LobbyCue.Tick -> R.raw.lobby_tick
    LobbyCue.Swish -> R.raw.lobby_swish
    LobbyCue.On -> R.raw.lobby_on
    LobbyCue.Off -> R.raw.lobby_off
    LobbyCue.Start -> R.raw.lobby_start
    LobbyCue.Intro -> R.raw.lobby_intro
}

@Stable
class LobbySounds internal constructor(
    context: Context?,
    private val enabled: () -> Boolean
) {
    private val pool: SoundPool? = if (context == null) null else buildPool()
    private val sampleIds = HashMap<LobbyCue, Int>()
    private val ready: MutableSet<Int> = ConcurrentHashMap.newKeySet()
    private val lastPlayed = HashMap<LobbyCue, Long>()

    init {
        val p = pool
        if (p != null && context != null) {
            p.setOnLoadCompleteListener { _, sampleId, status ->
                if (status == 0) ready.add(sampleId)
            }
            for (cue in LobbyCue.entries) {
                sampleIds[cue] = p.load(context, cue.rawRes(), 1)
            }
        }
    }

    /** هل الأصوات ممكنة الآن (مفتاح المستخدم مفعَّل ومحرّك مبنيّ)؟ */
    val isEnabled: Boolean get() = pool != null && enabled()

    /**
     * يشغّل [cue] ويعيد مقبض البثّ (0 = لم يُشغَّل: مُعطَّل، أو لم تجهز العيّنة، أو داخل فاصل النوع).
     * [rate] يغيّر السرعة والنغمة معًا (0٫5..2) — يُستعمل لتمييز صعود الصفحة عن نزولها بعيّنة واحدة.
     */
    fun play(cue: LobbyCue, rate: Float = 1f): Int {
        val p = pool ?: return 0
        val sample = sampleIds[cue] ?: return 0
        if (sample !in ready) return 0
        val now = SystemClock.elapsedRealtime()
        if (!LobbySoundPolicy.allow(cue, now, lastPlayed[cue], enabled())) return 0
        lastPlayed[cue] = now
        return p.play(sample, cue.volume, cue.volume, 1, 0, rate.coerceIn(0.5f, 2f))
    }

    /** ينتظر جاهزيّة عيّنة [cue] حتى [timeoutMs]؛ يعيد `false` إن لم تجهز (الدخول يمضي صامتًا). */
    suspend fun awaitReady(cue: LobbyCue, timeoutMs: Long): Boolean {
        val sample = sampleIds[cue] ?: return false
        var waited = 0L
        while (sample !in ready) {
            if (waited >= timeoutMs) return false
            delay(POLL_MS)
            waited += POLL_MS
        }
        return true
    }

    /** يخفض بثًّا إلى الصمت على [durationMs] ثم يوقفه — تخطّي المقدّمة بلا طقطقة قطع مفاجئ. */
    suspend fun fadeOutAndStop(stream: Int, durationMs: Int) {
        val p = pool ?: return
        if (stream == 0) return
        for (step in 1..FADE_STEPS) {
            val level = (1f - step / FADE_STEPS.toFloat()) * LobbyCue.Intro.volume
            p.setVolume(stream, level, level)
            delay(durationMs / FADE_STEPS.toLong())
        }
        p.stop(stream)
    }

    internal fun release() {
        pool?.release()
    }

    private fun buildPool(): SoundPool {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        return SoundPool.Builder()
            .setMaxStreams(MAX_STREAMS)
            .setAudioAttributes(attributes)
            .build()
    }

    companion object {
        private const val MAX_STREAMS = 4
        private const val POLL_MS = 16L
        private const val FADE_STEPS = 8

        /** نسخة صامتة: الافتراضيّ خارج اللوبي (معاينات وأي مكوّن يُرسم بلا مزوِّد). */
        val Silent = LobbySounds(null) { false }
    }
}

/** مزوّد أصوات اللوبي لشجرة الشاشة؛ الافتراضيّ [LobbySounds.Silent] فلا يتعطّل مكوّن خارج اللوبي. */
val LocalLobbySounds = staticCompositionLocalOf { LobbySounds.Silent }

/**
 * ينشئ محرّك الأصوات مرّة لكل شاشة ويحرّره عند مغادرتها (يوقف أي صوت جارٍ). [enabled] يُقرأ عند كل
 * تشغيل عبر `State` فلا يُعاد التحميل حين يبدّل المستخدم المفتاح.
 */
@Composable
fun rememberLobbySounds(enabled: Boolean): LobbySounds {
    val appContext = LocalContext.current.applicationContext
    val latest = rememberUpdatedState(enabled)
    val sounds = remember(appContext) { LobbySounds(appContext) { latest.value } }
    DisposableEffect(sounds) { onDispose { sounds.release() } }
    return sounds
}
