/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.audio

import android.content.Context
import android.media.AudioManager
import android.media.AudioManager.AudioPlaybackCallback
import android.media.AudioPlaybackConfiguration
import android.os.Handler
import android.os.Looper
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import nd.max.core.platform.EventLog
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * تتبع وإدارة جلسات الصوت النشطة — **الحل الجذري والحي لعزل جلسات أندرويد**.
 *
 * المنصات الحديثة (Android 10+) تعزل عزف التطبيقات (Spotify, YouTube, الألعاب) في جلسات
 * مستقلة. هذا الصنف يجمع:
 * 1. استشعار التشغيل الفوري عبر [AudioPlaybackCallback] لكل صوت يبدأ في النظام.
 * 2. استخراج معرفات الجلسات الحية عبر واجهات المنصة واستعلامات dumpsys audio.
 * 3. بث مشغلات الوسائط (AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION).
 * 4. جلسة المعاينة المحلية (preview session) لقراءة عتاد الجهاز دوماً.
 */
@Singleton
class AudioSessionTracker @Inject constructor() {

    private val sessionsMap = ConcurrentHashMap<Int, String?>()
    private val _activeSessions = MutableStateFlow<Map<Int, String?>>(emptyMap())
    val activeSessions: StateFlow<Map<Int, String?>> = _activeSessions.asStateFlow()

    @Volatile
    private var previewSession: Int? = null

    private val listeners = ConcurrentHashMap.newKeySet<SessionListener>()

    @Volatile
    private var isMonitoring = false
    private var playbackCallback: AudioPlaybackCallback? = null

    interface SessionListener {
        fun onSessionOpened(sessionId: Int, packageName: String?)
        fun onSessionClosed(sessionId: Int)
    }

    fun addListener(listener: SessionListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: SessionListener) {
        listeners.remove(listener)
    }

    /**
     * يُسجّل جلسة جديدة فُتحت من تطبيق وسائط أو من مستشعر التشغيل الحي.
     */
    fun onSessionOpened(sessionId: Int, packageName: String?) {
        if (sessionId <= 0) return
        sessionsMap[sessionId] = packageName
        _activeSessions.value = sessionsMap.toMap()
        runCatching {
            EventLog.audioOp(
                target = "audio_session_open",
                outcome = "tracked",
                actual = "session=$sessionId pkg=${packageName ?: "unknown"}",
            )
        }
        listeners.forEach { runCatching { it.onSessionOpened(sessionId, packageName) } }
    }

    /**
     * يُلغي تسجيل جلسة أُغلقت عند توقف العزف أو إغلاق التطبيق.
     */
    fun onSessionClosed(sessionId: Int) {
        if (sessionId <= 0) return
        sessionsMap.remove(sessionId)
        _activeSessions.value = sessionsMap.toMap()
        runCatching {
            EventLog.audioOp(
                target = "audio_session_close",
                outcome = "removed",
                actual = "session=$sessionId",
            )
        }
        listeners.forEach { runCatching { it.onSessionClosed(sessionId) } }
    }

    /**
     * بدء مراقبة تشغيل الصوت في النظام تلقائياً عبر [AudioPlaybackCallback].
     */
    fun startMonitoring(context: Context) {
        if (isMonitoring) return
        val am = runCatching { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }.getOrNull() ?: return
        val handler = runCatching { Handler(Looper.getMainLooper()) }.getOrNull() ?: return

        val callback = object : AudioPlaybackCallback() {
            override fun onPlaybackConfigChanged(configs: List<AudioPlaybackConfiguration>) {
                super.onPlaybackConfigChanged(configs)
                handlePlaybackConfigs(configs, context)
            }
        }
        playbackCallback = callback
        runCatching {
            am.registerAudioPlaybackCallback(callback, handler)
            isMonitoring = true
        }

        // استعلام مبدئي للجلسات النشطة حالياً
        refreshActiveSessions(context)
    }

    /**
     * إيقاف مراقبة تشغيل الصوت.
     */
    fun stopMonitoring(context: Context) {
        if (!isMonitoring) return
        val am = runCatching { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }.getOrNull()
        playbackCallback?.let { cb ->
            runCatching { am?.unregisterAudioPlaybackCallback(cb) }
        }
        playbackCallback = null
        isMonitoring = false
    }

    /**
     * معالجة تغيرات تكوينات التشغيل النشطة من المنصة.
     */
    private fun handlePlaybackConfigs(configs: List<AudioPlaybackConfiguration>?, context: Context) {
        if (configs == null) return
        val activeIds = mutableSetOf<Int>()

        for (config in configs) {
            val sid = extractSessionId(config)
            if (sid > 0) {
                activeIds.add(sid)
                if (!sessionsMap.containsKey(sid)) {
                    val pkg = extractPackageName(config, context)
                    onSessionOpened(sid, pkg)
                }
            }
        }

        // فحص الجلسات المحذوفة التي لم تعد قيد التشغيل
        val toRemove = sessionsMap.keys.filter { it != previewSession && it !in activeIds }
        toRemove.forEach { onSessionClosed(it) }
    }

    /**
     * استخراج رقم الجلسة من [AudioPlaybackConfiguration] عبر الانعكاس والواجهات المتاحة.
     */
    private fun extractSessionId(config: AudioPlaybackConfiguration): Int {
        return runCatching {
            val cls = config.javaClass
            // محاولة الطرق المعتمدة في إصدارات أندرويد المختلفة
            val getSessionId = runCatching { cls.getMethod("getSessionId") }.getOrNull()
                ?: runCatching { cls.getMethod("getClientAudioSessionId") }.getOrNull()
            val id = getSessionId?.invoke(config) as? Int
            if (id != null && id > 0) return@runCatching id

            // فحص الحقول المباشرة كبديل
            val field = runCatching { cls.getDeclaredField("mSessionId").apply { isAccessible = true } }.getOrNull()
            (field?.get(config) as? Int) ?: 0
        }.getOrDefault(0)
    }

    /**
     * استخراج اسم الحزمة المرتبطة بجلسة التشغيل.
     */
    private fun extractPackageName(config: AudioPlaybackConfiguration, context: Context): String? {
        return runCatching {
            val cls = config.javaClass
            val getUid = runCatching { cls.getMethod("getClientUid") }.getOrNull()
            val uid = getUid?.invoke(config) as? Int
            if (uid != null && uid > 0) {
                context.packageManager.getPackagesForUid(uid)?.firstOrNull()
            } else null
        }.getOrNull()
    }

    /**
     * استعلام وتحديث شامل لكافة الجلسات عبر المنصة ومخرجات الروت.
     */
    fun refreshActiveSessions(context: Context): List<Int> {
        val found = mutableListOf<Int>()
        val am = runCatching { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }.getOrNull()
        if (am != null) {
            val configs = runCatching { am.activePlaybackConfigurations }.getOrNull()
            if (configs != null) {
                for (config in configs) {
                    val sid = extractSessionId(config)
                    if (sid > 0) {
                        found.add(sid)
                        if (!sessionsMap.containsKey(sid)) {
                            val pkg = extractPackageName(config, context) ?: "media_app"
                            onSessionOpened(sid, pkg)
                        }
                    }
                }
            }
        }

        // مسار الروت الاحتياطي لضمان التقاط مشغلات الألعاب والتطبيقات المباشرة
        val rootSessions = refreshViaRoot()
        found.addAll(rootSessions)
        return found.distinct()
    }

    /**
     * يضمن وجود جلسة محلية للمعاينة حتى لو لم يكن هناك مشغل وسائط قيد العزف،
     * ليتمكن المعادل من قراءة نطاقات التردد والأنماط من عتاد الجهاز بلا رفض.
     */
    fun ensurePreviewSession(context: Context): Int {
        val existing = previewSession
        if (existing != null && existing > 0) return existing
        val manager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val generated = runCatching { manager?.generateAudioSessionId() }.getOrNull()
        val valid = if (generated != null && generated > 0) generated else DEFAULT_PREVIEW_SESSION
        previewSession = valid
        return valid
    }

    /**
     * يستخرج معرفات الجلسات من مخرجات dumpsys media.audio_flinger بصورة نقية.
     */
    internal fun parseAudioFlingerDump(lines: List<String>): List<Int> {
        val found = LinkedHashSet<Int>()
        val regex = Regex("""(?i)(?:session(?:\s*id)?|audio_session_t)\s*[:=]?\s*(\d+)""")
        for (line in lines) {
            val match = regex.find(line) ?: continue
            val id = match.groupValues[1].toIntOrNull() ?: continue
            if (id > 0) {
                found.add(id)
            }
        }
        return found.toList()
    }

    /**
     * يستطلع الجلسات النشطة عبر الروت للتطبيقات التي لا ترسل بث فتح الجلسة (كاليوتيوب والألعاب).
     */
    fun refreshViaRoot(): List<Int> {
        val hasRoot = runCatching { Shell.isAppGrantedRoot() == true }.getOrDefault(false)
        if (!hasRoot) return emptyList()
        val dump = runCatching {
            Shell.cmd("dumpsys media.audio_flinger 2>/dev/null").exec().out
        }.getOrNull() ?: return emptyList()

        val parsed = parseAudioFlingerDump(dump)
        val added = mutableListOf<Int>()
        for (id in parsed) {
            if (id != previewSession && !sessionsMap.containsKey(id)) {
                added.add(id)
                onSessionOpened(id, "media_track")
            }
        }
        return added
    }

    /**
     * يعيد الجلسة الأكثر فاعلية حالياً (أول جلسة تشغيل نشطة، أو جلسة المعاينة).
     */
    fun effectiveSession(context: Context): Int {
        val active = sessionsMap.keys.firstOrNull()
        if (active != null && active > 0) return active
        return ensurePreviewSession(context)
    }

    companion object {
        private const val DEFAULT_PREVIEW_SESSION = 1001

        @Volatile
        private var singletonInstance: AudioSessionTracker? = null

        fun get(): AudioSessionTracker = singletonInstance ?: synchronized(this) {
            singletonInstance ?: AudioSessionTracker().also { singletonInstance = it }
        }
    }

    init {
        singletonInstance = this
    }
}
