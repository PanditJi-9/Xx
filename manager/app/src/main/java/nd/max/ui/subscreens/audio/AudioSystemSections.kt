/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.subscreens.audio

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import nd.max.R
import nd.max.core.audio.AudioLatencyReading
import nd.max.core.audio.AudioLowLatencySupport
import nd.max.core.audio.AudioRouteDevice
import nd.max.core.audio.AudioRouteSnapshot
import nd.max.core.audio.AudioSessionTracker
import nd.max.core.audio.audioBufferMillisText
import nd.max.service.AudioEffectService
import nd.max.ui.design.MaxCapsule
import nd.max.ui.design.MaxConfirmDialog
import nd.max.ui.design.MaxGroup
import nd.max.ui.design.MaxGroupDivider
import nd.max.ui.design.MaxRow
import nd.max.ui.design.MaxSection
import nd.max.ui.design.MaxSpace
import nd.max.ui.design.MaxTone

/**
 * قسم جلسات الصوت النشطة — يعرض تدفقات التطبيقات الحية في النظام المربوطة بمحرك المعالجة الشامل.
 */
@Composable
internal fun AudioActiveSessionsSection(
    tracker: AudioSessionTracker = remember { AudioSessionTracker.get() },
) {
    val activeSessions by tracker.activeSessions.collectAsState()

    MaxSection(
        title = stringResource(R.string.audio_system_active_sessions_title),
        description = stringResource(R.string.audio_system_active_sessions_desc),
        collapsible = true,
        initiallyExpanded = true,
    ) {
        if (activeSessions.isEmpty()) {
            MaxGroup {
                MaxRow(
                    title = stringResource(R.string.audio_system_no_active_sessions),
                    subtitle = stringResource(R.string.audio_status_universal_active),
                    icon = Icons.Rounded.GraphicEq,
                    iconTone = MaxTone.Neutral,
                    trailing = {
                        MaxCapsule(
                            text = "STANDBY",
                            tone = MaxTone.Neutral,
                        )
                    },
                )
            }
        } else {
            MaxGroup {
                activeSessions.entries.forEachIndexed { index, entry ->
                    if (index > 0) MaxGroupDivider()
                    val sessionId = entry.key
                    val pkgName = entry.value ?: stringResource(R.string.max_audio_stream_media)
                    MaxRow(
                        title = pkgName,
                        subtitle = "Audio Session #$sessionId",
                        icon = Icons.Rounded.Waves,
                        iconTone = MaxTone.Positive,
                        trailing = {
                            MaxCapsule(
                                text = "DSP LIVE",
                                tone = MaxTone.Positive,
                            )
                        },
                    )
                }
            }
        }
    }
}

/**
 * قسم ملف الإخراج العتادي — مراقبة مخرج الصوت الفعلي (سماعات الرأس، مكبرات الصوت، بلوتوث).
 *
 * **وأُزيل من هنا ادّعاءٌ لم يكن مقيسًا** (`48.0 kHz · 24-bit PCM Low-Latency`): كان يُعرض
 * لمجرّد أن التوجيه «مدعوم»، فيقرأ المستخدم أرقامًا لم تُقرأ من جهازه. والسطر الآن يعرض
 * **ما أعلنته المنصّة فعلًا** من [latency] أو يقول «غير مقروء» — والغياب لا يُملأ برقم شائع.
 */
@Composable
internal fun AudioDeviceProfileSection(
    snapshot: AudioRouteSnapshot?,
    latency: AudioLatencyReading?,
) {
    MaxSection(
        title = stringResource(R.string.audio_system_device_profile_title),
        description = stringResource(R.string.audio_system_device_profile_desc),
        collapsible = true,
        initiallyExpanded = true,
    ) {
        val activeDevice = snapshot?.active
        val icon = when {
            activeDevice?.typeToken?.contains("head") == true -> Icons.Rounded.Headphones
            activeDevice?.typeToken?.contains("bluetooth") == true -> Icons.Rounded.Headphones
            else -> Icons.Rounded.Speaker
        }

        MaxGroup {
            MaxRow(
                title = activeDevice?.productName?.takeIf { it.isNotBlank() }
                    ?: activeDevice?.typeToken
                    ?: stringResource(R.string.max_audio_device_speaker),
                subtitle = bufferSubtitle(latency),
                icon = icon,
                iconTone = MaxTone.Accent,
                trailing = {
                    MaxCapsule(
                        text = if (activeDevice != null) "CONNECTED" else "DEFAULT",
                        tone = MaxTone.Accent,
                    )
                },
            )
        }
    }
}

/**
 * زمن المسار الصوتي — **قراءة محسوبة لا قياس رحلة**.
 *
 * الرقم من إعلان المنصّة (`PROPERTY_OUTPUT_FRAMES_PER_BUFFER` و`PROPERTY_OUTPUT_SAMPLE_RATE`)،
 * وحكم المسار من `AudioTrack.getPerformanceMode()` بعد بناءٍ لحظيّ يُحرَّر فورًا. وما لم يُقرأ
 * يُقال «غير مقروء» ولا يُكمَّل برقم شائع (ADR-07).
 *
 * **ولا زرّ «تحسين» هنا:** لا واجهة نظام عامّة تكتب زمن المسار الصوتي كله — المسار يختاره HAL
 * والمنصّة لكل تدفّق. فما لا يمكن التحقّق من أثره لا يُعرض كأنه نُفِّذ، وتُقال الحدود بدلًا منه.
 */
@Composable
internal fun AudioLatencySection(reading: AudioLatencyReading?) {
    MaxSection(
        title = stringResource(R.string.audio_latency_title),
        description = stringResource(R.string.audio_latency_desc),
        collapsible = true,
        initiallyExpanded = false,
    ) {
        MaxGroup {
            MaxRow(
                title = stringResource(R.string.audio_latency_buffer_title),
                subtitle = bufferSubtitle(reading),
                icon = Icons.Rounded.GraphicEq,
                iconTone = if (reading?.bufferMeasured == true) MaxTone.Positive else MaxTone.Neutral,
            )
            MaxGroupDivider()
            MaxRow(
                title = stringResource(R.string.audio_latency_low_latency_title),
                subtitle = when (reading?.lowLatency) {
                    AudioLowLatencySupport.SUPPORTED ->
                        stringResource(R.string.audio_latency_low_latency_supported)
                    AudioLowLatencySupport.FALLBACK ->
                        stringResource(R.string.audio_latency_low_latency_fallback)
                    AudioLowLatencySupport.UNREADABLE, null ->
                        stringResource(R.string.audio_latency_unreadable)
                },
                icon = Icons.Rounded.Waves,
                iconTone = if (reading?.lowLatency == AudioLowLatencySupport.SUPPORTED) {
                    MaxTone.Positive
                } else {
                    MaxTone.Neutral
                },
            )
            MaxGroupDivider()
            MaxRow(
                title = stringResource(R.string.audio_latency_av_sync_title),
                subtitle = stringResource(R.string.audio_latency_av_sync_unavailable),
                icon = Icons.Rounded.Speaker,
                iconTone = MaxTone.Neutral,
            )
        }
    }
}

/** سطر مخزن الخَرْج: الأرقام الثلاثة كما قُرئت، أو سبب غيابها — ولا يُخترع رقم. */
@Composable
private fun bufferSubtitle(reading: AudioLatencyReading?): String {
    val rate = reading?.sampleRateHz
    val frames = reading?.framesPerBuffer
    val millis = audioBufferMillisText(reading?.bufferMillis)
    if (rate == null || frames == null || millis == null) {
        return stringResource(R.string.audio_latency_unreadable)
    }
    return stringResource(R.string.audio_latency_buffer_value, rate, frames, millis)
}

/**
 * قسم استعادة الضبط الافتراضي — تصفير كافة مؤثرات الصوت والمعادل بلمسة واحدة.
 */
@Composable
internal fun AudioResetSection(
    context: Context,
    onResetCompleted: () -> Unit = {},
) {
    var showConfirm by remember { mutableStateOf(false) }

    MaxSection(
        title = stringResource(R.string.audio_system_reset_all),
        description = stringResource(R.string.audio_system_reset_all_desc),
        collapsible = true,
    ) {
        MaxGroup {
            MaxRow(
                title = stringResource(R.string.audio_system_reset_all),
                subtitle = stringResource(R.string.audio_system_reset_all_desc),
                icon = Icons.Rounded.RestartAlt,
                iconTone = MaxTone.Caution,
                onClick = { showConfirm = true },
            )
        }
    }

    if (showConfirm) {
        MaxConfirmDialog(
            visible = true,
            title = stringResource(R.string.audio_system_reset_all),
            message = stringResource(R.string.audio_system_reset_all_desc),
            confirmLabel = stringResource(R.string.audio_system_reset_all),
            onConfirm = {
                showConfirm = false
                AudioEffectService.updateAllEqualizerBands(listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0))
                AudioEffectService.updateBassBoost(0)
                AudioEffectService.updateVirtualizer(0)
                AudioEffectService.updateLoudness(0)
                onResetCompleted()
            },
            onDismiss = { showConfirm = false },
            destructive = true,
            icon = Icons.Rounded.RestartAlt,
        )
    }
}
