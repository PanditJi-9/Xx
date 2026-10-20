/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.service

import nd.max.core.security.SecurityEnforcement
import nd.max.core.security.IntegrityCheckpoint
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import androidx.lifecycle.LifecycleService
import nd.max.R
import nd.max.core.audio.AudioEffectKind
import nd.max.core.audio.AudioEffectSession
import nd.max.core.audio.AudioSessionTracker
import nd.max.core.audio.AudioSoundPreset
import nd.max.core.audio.audioPresetValues
import nd.max.core.audio.scaledBy
import nd.max.core.audio.strengthFromPercent
import nd.max.core.platform.EventLog
import nd.max.ui.util.AudioEffectPrefs
import java.util.concurrent.ConcurrentHashMap

/**
 * خدمة معالجة واستمرار مؤثرات الصوت الحية في الخلفية (`AudioEffectService`).
 *
 * **محرك الجلسات الشامل (Universal Audio Engine):**
 * يضمن تطبيق المؤثرات الصوتية على كافة الهواتف (Snapdragon و MediaTek) بدون استثناء:
 * 1. يستمع لتغيرات جلسات تشغيل الصوت لحظياً عبر [AudioSessionTracker].
 * 2. يطبق المعادل ومضخم الجهير والصوت المحيطي ومضخم الصوت على كل جلسة فور بدء العزف.
 * 3. يربط الجلسة العامة (0) بالتوازي للأجهزة التي تدعم المعالجة الشاملة.
 * 4. يتيح التحديث الفوري المباشر من واجهة المستخدم دون أي تقطيع في الصوت.
 */
class AudioEffectService : LifecycleService() {

    companion object {
        var isRunning = false
            private set

        @Volatile
        var masterEnabled: Boolean = true
            private set

        @Volatile
        var activePreset: AudioSoundPreset = AudioSoundPreset.OFF
            private set

        @Volatile
        var activeIntensity: Int = 50
            private set

        @Volatile
        var manualEqBands: List<Int> = List(10) { 0 }
            private set

        @Volatile
        var bassBoostPercent: Int = 0
            private set

        @Volatile
        var virtualizerPercent: Int = 0
            private set

        @Volatile
        var loudnessGainMb: Int = 0
            private set

        const val CHANNEL_ID = "maxmanager_audio_effect_service"
        const val NOTIF_ID = 9030
        const val ACTION_START = "nd.max.service.AudioEffectService.START"
        const val ACTION_STOP = "nd.max.service.AudioEffectService.STOP"
        const val ACTION_UPDATE = "nd.max.service.AudioEffectService.UPDATE"
        const val EXTRA_PRESET = "extra_preset"
        const val EXTRA_INTENSITY = "extra_intensity"

        private val sessionEffects = ConcurrentHashMap<Int, MutableMap<AudioEffectKind, AudioEffectSession>>()

        private val trackerListener = object : AudioSessionTracker.SessionListener {
            override fun onSessionOpened(sessionId: Int, packageName: String?) {
                if (!isRunning || !masterEnabled) return
                applyCurrentSettingsToSession(sessionId)
            }

            override fun onSessionClosed(sessionId: Int) {
                releaseSession(sessionId)
            }
        }

        fun start(context: Context, preset: AudioSoundPreset = activePreset, intensity: Int = activeIntensity) {
            masterEnabled = true
            activePreset = preset
            activeIntensity = intensity
            // القرار يُحفظ قبل الإقلاع: من أُعيد جهازه يجد الصوت كما تركه (§3 من الطلب).
            AudioEffectPrefs.save(context, enabled = true, preset = preset, intensity = intensity)
            val intent = Intent(context, AudioEffectService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_PRESET, preset.token)
                putExtra(EXTRA_INTENSITY, intensity)
            }
            runCatching { context.startForegroundService(intent) }
                .onFailure { runCatching { context.startService(intent) } }
        }

        fun stop(context: Context) {
            masterEnabled = false
            activePreset = AudioSoundPreset.OFF
            AudioEffectPrefs.save(context, enabled = false, preset = AudioSoundPreset.OFF, intensity = activeIntensity)
            val intent = Intent(context, AudioEffectService::class.java).apply {
                action = ACTION_STOP
            }
            runCatching { context.stopService(intent) }
            releaseAllEffects()
        }

        fun setPreset(context: Context, preset: AudioSoundPreset, intensity: Int = 50) {
            activePreset = preset
            activeIntensity = intensity
            AudioEffectPrefs.save(context, enabled = masterEnabled, preset = preset, intensity = intensity)
            if (preset != AudioSoundPreset.OFF) {
                val values = audioPresetValues(preset).scaledBy(intensity)
                manualEqBands = values.eqGainsDb
                bassBoostPercent = values.bassPercent
                virtualizerPercent = values.virtualizerPercent
                loudnessGainMb = values.loudnessMb
            }
            if (!isRunning && preset != AudioSoundPreset.OFF) {
                start(context, preset, intensity)
            } else {
                applyCurrentSettingsToAllSessions()
            }
        }

        fun updateEqualizerBand(bandIndex: Int, gainDb: Int) {
            val updated = manualEqBands.toMutableList()
            if (bandIndex in updated.indices) {
                updated[bandIndex] = gainDb.coerceIn(-15, 15)
                manualEqBands = updated
                applyCurrentSettingsToAllSessions()
            }
        }

        fun updateAllEqualizerBands(bands: List<Int>) {
            manualEqBands = bands.map { it.coerceIn(-15, 15) }
            applyCurrentSettingsToAllSessions()
        }

        fun updateBassBoost(percent: Int) {
            bassBoostPercent = percent.coerceIn(0, 100)
            applyCurrentSettingsToAllSessions()
        }

        fun updateVirtualizer(percent: Int) {
            virtualizerPercent = percent.coerceIn(0, 100)
            applyCurrentSettingsToAllSessions()
        }

        fun updateLoudness(gainMb: Int) {
            loudnessGainMb = gainMb.coerceIn(0, 3000)
            applyCurrentSettingsToAllSessions()
        }

        fun toggleMaster(context: Context, enabled: Boolean) {
            masterEnabled = enabled
            AudioEffectPrefs.save(context, enabled = enabled, preset = activePreset, intensity = activeIntensity)
            if (enabled) {
                if (!isRunning) start(context)
                else applyCurrentSettingsToAllSessions()
            } else {
                releaseAllEffects()
            }
        }

        fun onSessionOpened(context: Context, sessionId: Int) {
            if (!isRunning || !masterEnabled) return
            applyCurrentSettingsToSession(sessionId)
        }

        fun onSessionClosed(sessionId: Int) {
            releaseSession(sessionId)
        }

        private fun releaseSession(sessionId: Int) {
            val map = sessionEffects.remove(sessionId) ?: return
            map.values.forEach { session ->
                runCatching { session.release() }
            }
        }

        private fun applyCurrentSettingsToAllSessions() {
            if (!masterEnabled) return
            // تطبيق على الجلسة العامة 0
            applyCurrentSettingsToSession(0)

            // تطبيق على كافة الجلسات النشطة المستكشفة
            val activeSessions = AudioSessionTracker.get().activeSessions.value.keys
            activeSessions.forEach { sessionId ->
                applyCurrentSettingsToSession(sessionId)
            }
        }

        private fun applyCurrentSettingsToSession(sessionId: Int) {
            if (!masterEnabled) return
            val currentMap = sessionEffects.getOrPut(sessionId) { ConcurrentHashMap<AudioEffectKind, AudioEffectSession>() }

            // 1. معادل الصوت (Equalizer)
            val hasEq = manualEqBands.any { it != 0 }
            if (hasEq) {
                runCatching {
                    val eqSession = currentMap.getOrPut(AudioEffectKind.EQUALIZER) {
                        AudioEffectSession(AudioEffectKind.EQUALIZER, Equalizer(0, sessionId), sessionId)
                    }
                    val eq = eqSession.effect as? Equalizer
                    if (eq != null) {
                        eq.enabled = true
                        val numBands = eq.numberOfBands.toInt()
                        for (i in 0 until minOf(numBands, manualEqBands.size)) {
                            val targetMb = (manualEqBands[i] * 100).coerceIn(-1500, 1500).toShort()
                            eq.setBandLevel(i.toShort(), targetMb)
                        }
                    }
                }
            } else {
                currentMap.remove(AudioEffectKind.EQUALIZER)?.let { runCatching { it.release() } }
            }

            // 2. مضخم الجهير (Bass Boost)
            if (bassBoostPercent > 0) {
                runCatching {
                    val bbSession = currentMap.getOrPut(AudioEffectKind.BASS_BOOST) {
                        AudioEffectSession(AudioEffectKind.BASS_BOOST, BassBoost(0, sessionId), sessionId)
                    }
                    val bb = bbSession.effect as? BassBoost
                    if (bb != null) {
                        bb.enabled = true
                        val strength = strengthFromPercent(bassBoostPercent).coerceIn(0, 1000).toShort()
                        bb.setStrength(strength)
                    }
                }
            } else {
                currentMap.remove(AudioEffectKind.BASS_BOOST)?.let { runCatching { it.release() } }
            }

            // 3. المحيط المجسم (Virtualizer)
            if (virtualizerPercent > 0) {
                runCatching {
                    val virtSession = currentMap.getOrPut(AudioEffectKind.VIRTUALIZER) {
                        AudioEffectSession(AudioEffectKind.VIRTUALIZER, Virtualizer(0, sessionId), sessionId)
                    }
                    val virt = virtSession.effect as? Virtualizer
                    if (virt != null) {
                        virt.enabled = true
                        val strength = strengthFromPercent(virtualizerPercent).coerceIn(0, 1000).toShort()
                        virt.setStrength(strength)
                    }
                }
            } else {
                currentMap.remove(AudioEffectKind.VIRTUALIZER)?.let { runCatching { it.release() } }
            }

            // 4. مضخم الصوت الذكي (Loudness Enhancer)
            if (loudnessGainMb > 0) {
                runCatching {
                    val loudSession = currentMap.getOrPut(AudioEffectKind.LOUDNESS) {
                        AudioEffectSession(AudioEffectKind.LOUDNESS, LoudnessEnhancer(sessionId), sessionId)
                    }
                    val loud = loudSession.effect as? LoudnessEnhancer
                    if (loud != null) {
                        loud.enabled = true
                        loud.setTargetGain(loudnessGainMb.coerceIn(0, 3000))
                    }
                }
            } else {
                currentMap.remove(AudioEffectKind.LOUDNESS)?.let { runCatching { it.release() } }
            }

            runCatching {
                EventLog.audioOp(
                    target = "service_apply_profile",
                    outcome = "applied",
                    actual = "session=$sessionId eq=${hasEq} bb=$bassBoostPercent virt=$virtualizerPercent loud=$loudnessGainMb",
                )
            }
        }

        private fun releaseAllEffects() {
            sessionEffects.values.forEach { map ->
                map.values.forEach { runCatching { it.release() } }
            }
            sessionEffects.clear()
        }
    }

    override fun onCreate() {
        super.onCreate()
        // بوّابة الخدمة: خدمة تعمل على نسخة معدَّلة تُنفّذ طلب المعدِّل بلا واجهة أصلًا.
        if (SecurityEnforcement.refuseService(this, IntegrityCheckpoint.SERVICE)) return
        isRunning = true
        startOverlayForeground(
            OverlayNotice(
                channelId = CHANNEL_ID,
                notificationId = NOTIF_ID,
                channelNameRes = R.string.audio_presets_title,
                titleRes = R.string.max_audio_studio_title,
                stopLabelRes = R.string.audio_presets_off,
                stopAction = ACTION_STOP,
                serviceClass = AudioEffectService::class.java,
            )
        )

        // بدء المراقبة المستمرة لجلسات التشغيل في النظام
        AudioSessionTracker.get().addListener(trackerListener)
        AudioSessionTracker.get().startMonitoring(this)

        // تطبيق الإعدادات الحالية على الجلسات المتوفرة
        applyCurrentSettingsToAllSessions()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val action = intent?.action
        if (action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        val token = intent?.getStringExtra(EXTRA_PRESET)
        // رمز غير معروف يُتجاهل، فلا يُطفئ مؤثّرًا نشطًا بصمت.
        val preset = token?.let { AudioSoundPreset.ofToken(it) }
        if (intent != null && preset != null) {
            val intensity = intent.getIntExtra(EXTRA_INTENSITY, activeIntensity)
            activePreset = preset
            activeIntensity = intensity
            if (preset != AudioSoundPreset.OFF) {
                val values = audioPresetValues(preset).scaledBy(intensity)
                manualEqBands = values.eqGainsDb
                bassBoostPercent = values.bassPercent
                virtualizerPercent = values.virtualizerPercent
                loudnessGainMb = values.loudnessMb
            }
        }

        applyCurrentSettingsToAllSessions()
        return START_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        AudioSessionTracker.get().removeListener(trackerListener)
        AudioSessionTracker.get().stopMonitoring(this)
        releaseAllEffects()
        super.onDestroy()
    }
}
