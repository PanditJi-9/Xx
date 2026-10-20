/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.receiver

import nd.max.core.security.SecurityEnforcement
import nd.max.core.security.IntegrityCheckpoint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import nd.max.core.audio.AudioSessionTracker
import nd.max.service.AudioEffectService

/**
 * مستقبل بث جلسات الصوت — يستقبل أحداث بدء وانتهاء تشغيل الصوت من كافة مشغلات
 * الوسائط على مستوى النظام (Spotify, YouTube Music, Deezer, VLC, Poweramp...).
 */
class AudioSessionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (SecurityEnforcement.refuseReceiver(context, IntegrityCheckpoint.RECEIVER)) return
        if (intent == null) return
        val action = intent.action ?: return
        val sessionId = intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, AudioEffect.ERROR)
        if (sessionId <= 0) return

        when (action) {
            AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION -> {
                val pkg = intent.getStringExtra(AudioEffect.EXTRA_PACKAGE_NAME)
                AudioSessionTracker.get().onSessionOpened(sessionId, pkg)
                AudioEffectService.onSessionOpened(context, sessionId)
            }
            AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION -> {
                AudioSessionTracker.get().onSessionClosed(sessionId)
                AudioEffectService.onSessionClosed(sessionId)
            }
        }
    }
}
