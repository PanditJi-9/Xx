/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.subscreens

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaCodecList
import android.media.MediaRecorder
import android.os.BatteryManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Display
import android.view.WindowManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nd.max.R
import nd.max.core.net.WifiChannel
import nd.max.ui.util.WifiChannelReader
import nd.max.ui.design.MaxGroup
import nd.max.ui.design.MaxGroupDivider
import nd.max.ui.design.MaxNavigationRow
import nd.max.ui.design.MaxRow
import nd.max.ui.design.MaxScreen
import nd.max.ui.design.MaxSection
import nd.max.ui.design.MaxSpace
import nd.max.ui.design.MaxSwitchRow
import nd.max.ui.design.MaxTone
import nd.max.ui.theme.MonoValueStyleSmall
import java.util.Locale
import kotlin.math.PI
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Device lab — the interactive counterpart to Diagnostics.
 *
 * Diagnostics answers "what does this device expose?". This screen answers
 * "does the hardware actually work?" using only public platform APIs: no root,
 * no Shizuku, no system-settings write. Every test is deliberately one tap
 * away, and every result is the system's own report rather than a cached
 * estimate.
 *
 * The reference teardown showed this is the one large surface MaxManager did
 * not have: screen/touch/button/vibration/speaker/flash/sensor tests. The
 * implementations below are clean-room, written against the Android SDK; no
 * reference code, layout or asset is reused.
 */
@Composable
fun DeviceLabScreen(navController: NavHostController) {
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var openTest by remember { mutableStateOf<LabTest?>(null) }

    val speakerError = stringResource(R.string.device_lab_speaker_error)
    val torchError = stringResource(R.string.device_lab_torch_error)
    val player = remember { StereoTonePlayer(context) }
    var earpieceRoute by remember { mutableStateOf<Int?>(null) }

    DisposableEffect(Unit) {
        onDispose { player.stop() }
    }

    MaxScreen(
        title = stringResource(R.string.device_lab_title),
        onBack = { navController.popBackStack() },
        subtitle = stringResource(R.string.device_lab_subtitle),
        accentIcon = Icons.Rounded.Science,
        snackbarHostState = snackbar,
    ) {
        MaxSection(title = stringResource(R.string.device_lab_section_screen)) {
            MaxGroup {
                MaxNavigationRow(
                    title = stringResource(R.string.device_lab_screen_title),
                    subtitle = stringResource(R.string.device_lab_screen_desc),
                    icon = Icons.Rounded.TouchApp,
                    iconTone = MaxTone.Accent,
                    onClick = { openTest = LabTest.SCREEN },
                )
                MaxGroupDivider()
                MaxNavigationRow(
                    title = stringResource(R.string.device_lab_touch_title),
                    subtitle = stringResource(R.string.device_lab_touch_desc),
                    icon = Icons.Rounded.TouchApp,
                    iconTone = MaxTone.Accent,
                    onClick = { openTest = LabTest.TOUCH },
                )
            }
        }

        MaxSection(
            title = stringResource(R.string.device_lab_section_display),
            description = stringResource(R.string.device_lab_display_desc),
        ) {
            DisplayCapabilities()
        }

        MaxSection(
            title = stringResource(R.string.device_lab_section_codecs),
            description = stringResource(R.string.device_lab_codecs_desc),
        ) {
            CodecCapabilities()
        }

        MaxSection(title = stringResource(R.string.device_lab_section_controls)) {
            MaxGroup {
                MaxNavigationRow(
                    title = stringResource(R.string.device_lab_buttons_title),
                    subtitle = stringResource(R.string.device_lab_buttons_desc),
                    icon = Icons.Rounded.Keyboard,
                    onClick = { openTest = LabTest.BUTTONS },
                )
                MaxGroupDivider()
                VibrationTestRows()
            }
        }

        MaxSection(title = stringResource(R.string.device_lab_section_audio)) {
            MaxGroup {
                MaxRow(
                    title = stringResource(R.string.device_lab_speaker_title),
                    subtitle = stringResource(R.string.device_lab_speaker_desc),
                    icon = Icons.AutoMirrored.Rounded.VolumeUp,
                    iconTone = MaxTone.Accent,
                )
                SpeakerActionRow(
                    label = stringResource(R.string.device_lab_play_left),
                    onClick = {
                        if (!player.play(left = 1f, right = 0f)) {
                            scope.launch { snackbar.showSnackbar(speakerError) }
                        }
                    },
                )
                MaxGroupDivider()
                SpeakerActionRow(
                    label = stringResource(R.string.device_lab_play_right),
                    onClick = {
                        if (!player.play(left = 0f, right = 1f)) {
                            scope.launch { snackbar.showSnackbar(speakerError) }
                        }
                    },
                )
                MaxGroupDivider()
                SpeakerActionRow(
                    label = stringResource(R.string.device_lab_play_both),
                    onClick = {
                        if (!player.play(left = 1f, right = 1f)) {
                            scope.launch { snackbar.showSnackbar(speakerError) }
                        }
                    },
                )
                MaxGroupDivider()
                SpeakerActionRow(
                    label = stringResource(R.string.device_lab_play_earpiece),
                    onClick = {
                        if (!player.play(left = 1f, right = 1f, earpiece = true)) {
                            scope.launch { snackbar.showSnackbar(speakerError) }
                        } else {
                            earpieceRoute = player.lastRoutedDeviceType
                        }
                    },
                )
                earpieceRoute?.let { type ->
                    MaxGroupDivider()
                    LabValueRow(
                        title = stringResource(R.string.device_lab_routed_to),
                        value = stringResource(outputTypeLabel(type)),
                    )
                }
                MaxGroupDivider()
                SpeakerActionRow(
                    label = stringResource(R.string.device_lab_stop),
                    onClick = { player.stop() },
                )
            }
        }

        MaxSection(
            title = stringResource(R.string.device_lab_outputs_title),
            description = stringResource(R.string.device_lab_outputs_desc),
        ) {
            AudioOutputsTest()
        }

        MaxSection(
            title = stringResource(R.string.device_lab_section_wifi),
            description = stringResource(R.string.device_lab_wifi_desc),
        ) {
            WifiChannels()
        }

        MaxSection(title = stringResource(R.string.device_lab_section_hardware)) {
            TorchTest(
                onError = { scope.launch { snackbar.showSnackbar(torchError) } },
            )
            MicrophoneTest()
            SensorTest()
            BatteryTest()
        }
    }

    when (openTest) {
        LabTest.SCREEN -> ScreenTestDialog(onDismiss = { openTest = null })
        LabTest.TOUCH -> TouchTestDialog(onDismiss = { openTest = null })
        LabTest.BUTTONS -> ButtonTestDialog(onDismiss = { openTest = null })
        null -> Unit
    }
}

private enum class LabTest { SCREEN, TOUCH, BUTTONS }

// ---------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------

@Composable
private fun ScreenTestDialog(onDismiss: () -> Unit) {
    val colors = remember {
        listOf(
            Color.White,
            Color(0xFFFF0000),
            Color(0xFF00FF00),
            Color(0xFF0000FF),
            Color.Black,
            Color(0xFF808080),
        )
    }
    val labels = listOf(
        R.string.device_lab_color_white,
        R.string.device_lab_color_red,
        R.string.device_lab_color_green,
        R.string.device_lab_color_blue,
        R.string.device_lab_color_black,
        R.string.device_lab_color_grey,
    )
    var index by remember { mutableIntStateOf(0) }
    val current = colors[index]
    val label = stringResource(labels[index])
    val onColor = if (current.luminance() > 0.5f) Color.Black else Color.White
    val hint = stringResource(R.string.device_lab_screen_hint)
    val close = stringResource(R.string.device_lab_close)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(current)
                .clickable { index = (index + 1) % colors.size }
                .systemBarsPadding()
        ) {
            Text(
                text = label,
                color = onColor,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(MaxSpace.lg),
            )
            Text(
                text = hint,
                color = onColor,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(MaxSpace.lg),
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(MaxSpace.sm),
            ) {
                Icon(Icons.Rounded.Close, contentDescription = close, tint = onColor)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Touch
// ---------------------------------------------------------------------------

@Composable
private fun TouchTestDialog(onDismiss: () -> Unit) {
    var pointers by remember { mutableStateOf<Map<PointerId, Offset>>(emptyMap()) }
    var maxPointers by remember { mutableIntStateOf(0) }
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val hint = stringResource(R.string.device_lab_touch_hint)
    val maxText = stringResource(R.string.device_lab_touch_max, maxPointers)
    val reset = stringResource(R.string.device_lab_reset)
    val close = stringResource(R.string.device_lab_close)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val pressed = event.changes
                                .filter { it.pressed }
                                .associate { it.id to it.position }
                            pointers = pressed
                            if (pressed.size > maxPointers) maxPointers = pressed.size
                        }
                    }
                }
                .systemBarsPadding()
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val radius = MaxSpace.xxl.toPx()
                val dot = MaxSpace.xs.toPx()
                val stroke = MaxSpace.hairline.toPx()
                pointers.values.forEach { position ->
                    drawCircle(
                        color = primary.copy(alpha = 0.35f),
                        radius = radius,
                        center = position,
                        style = Stroke(width = stroke),
                    )
                    drawCircle(color = tertiary, radius = dot, center = position)
                }
            }
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(MaxSpace.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(MaxSpace.xs),
            ) {
                Text(
                    text = stringResource(R.string.device_lab_fingers, pointers.size),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = maxText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(MaxSpace.lg),
                horizontalArrangement = Arrangement.Center,
            ) {
                TextButton(onClick = { maxPointers = 0 }) {
                    Icon(Icons.Rounded.RestartAlt, contentDescription = null)
                    Text(reset)
                }
                TextButton(onClick = onDismiss) {
                    Text(close)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Buttons
// ---------------------------------------------------------------------------

@Composable
private fun ButtonTestDialog(onDismiss: () -> Unit) {
    var lastKey by remember { mutableStateOf<Key?>(null) }
    var count by remember { mutableIntStateOf(0) }
    val focusRequester = remember { FocusRequester() }
    val hint = stringResource(R.string.device_lab_buttons_hint)
    val noKey = stringResource(R.string.device_lab_no_key)
    val close = stringResource(R.string.device_lab_close)

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val keyText = lastKey?.let { key ->
        runCatching { android.view.KeyEvent.keyCodeToString(key.nativeKeyCode) }
            .getOrDefault(key.toString())
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .focusRequester(focusRequester)
                .focusable()
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) {
                        lastKey = event.key
                        count += 1
                        true
                    } else {
                        false
                    }
                }
                .clickable { focusRequester.requestFocus() }
                .systemBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(MaxSpace.xxl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(MaxSpace.md),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Keyboard,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = keyText?.let { stringResource(R.string.device_lab_last_key, it) } ?: noKey,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.device_lab_key_count, count),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(MaxSpace.lg),
            ) {
                Text(close)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Vibration
// ---------------------------------------------------------------------------

@Composable
private fun VibrationTestRows() {
    val context = LocalContext.current
    val vibrator = remember { vibratorOf(context) }
    val available = vibrator?.hasVibrator() == true
    val unavailable = stringResource(R.string.device_lab_unavailable)

    LabActionRow(
        title = stringResource(R.string.device_lab_vibrate_short),
        icon = Icons.Rounded.Vibration,
        enabled = available,
        lockedReason = if (available) null else unavailable,
        onClick = {
            vibrator?.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
        },
    )
    MaxGroupDivider()
    LabActionRow(
        title = stringResource(R.string.device_lab_vibrate_long),
        icon = Icons.Rounded.Vibration,
        enabled = available,
        lockedReason = if (available) null else unavailable,
        onClick = {
            vibrator?.vibrate(VibrationEffect.createOneShot(600, VibrationEffect.DEFAULT_AMPLITUDE))
        },
    )
    MaxGroupDivider()
    LabActionRow(
        title = stringResource(R.string.device_lab_vibrate_pattern),
        icon = Icons.Rounded.Vibration,
        enabled = available,
        lockedReason = if (available) null else unavailable,
        onClick = {
            vibrator?.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0, 100, 100, 200, 100, 400),
                    intArrayOf(0, 255, 0, 180, 0, 120),
                    -1,
                )
            )
        },
    )
}

@Suppress("DEPRECATION")
private fun vibratorOf(context: Context): Vibrator? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        context.getSystemService(Vibrator::class.java)
    }

// ---------------------------------------------------------------------------
// Speaker
// ---------------------------------------------------------------------------

@Composable
private fun SpeakerActionRow(label: String, onClick: () -> Unit) {
    MaxRow(
        title = label,
        icon = Icons.AutoMirrored.Rounded.VolumeUp,
        iconTone = MaxTone.Inactive,
        onClick = onClick,
    )
}

/**
 * A two-channel sine tone written straight to an [AudioTrack].
 *
 * A mono [android.media.ToneGenerator] cannot prove which speaker works, which
 * is the point of the test. The buffer is generated once per request, played
 * statically and released on the next request or on dispose.
 *
 * **The earpiece route is a request, not a claim.** Android may ignore it (for
 * example while a call is active or on devices without an earpiece). After
 * playback starts the player reads `AudioTrack.routedDevice` — the platform's
 * own routing report — and the screen shows what it actually selected. When the
 * platform reports nothing, the row says so instead of assuming the request
 * worked.
 */
private class StereoTonePlayer(context: Context) {
    private val audioManager = context.applicationContext.getSystemService(AudioManager::class.java)
    private var previousMode: Int? = null
    private var track: AudioTrack? = null

    /** The type of the device Android actually routed to, or `null` if unreported. */
    var lastRoutedDeviceType: Int? = null
        private set

    fun play(
        left: Float,
        right: Float,
        durationMs: Int = 1800,
        earpiece: Boolean = false,
    ): Boolean {
        stop()
        return runCatching {
            // Communication mode plus speakerphone off is the public way to
            // request the earpiece; the mode is restored in [stop].
            if (earpiece) {
                audioManager?.let { manager ->
                    previousMode = manager.mode
                    manager.mode = AudioManager.MODE_IN_COMMUNICATION
                    @Suppress("DEPRECATION")
                    manager.isSpeakerphoneOn = false
                }
            }
            val sampleRate = 44_100
            val frames = sampleRate * durationMs / 1000
            val samples = ShortArray(frames * 2)
            val amplitude = Short.MAX_VALUE * 0.30
            for (frame in 0 until frames) {
                val value = (sin(2.0 * PI * 440.0 * frame / sampleRate) * amplitude).toInt().toShort()
                samples[frame * 2] = (value * left).toInt().toShort()
                samples[frame * 2 + 1] = (value * right).toInt().toShort()
            }
            val usage = if (earpiece) {
                AudioAttributes.USAGE_VOICE_COMMUNICATION
            } else {
                AudioAttributes.USAGE_MEDIA
            }
            val contentType = if (earpiece) {
                AudioAttributes.CONTENT_TYPE_SPEECH
            } else {
                AudioAttributes.CONTENT_TYPE_MUSIC
            }
            val created = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(usage)
                        .setContentType(contentType)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(samples.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            created.write(samples, 0, samples.size)
            created.play()
            lastRoutedDeviceType = runCatching { created.routedDevice?.type }.getOrNull()
            track = created
            true
        }.getOrDefault(false)
    }

    fun stop() {
        track?.let { player ->
            runCatching { player.stop() }
            runCatching { player.release() }
        }
        track = null
        lastRoutedDeviceType = null
        previousMode?.let { mode ->
            runCatching { audioManager?.mode = mode }
            previousMode = null
        }
    }
}

// ---------------------------------------------------------------------------
// Audio outputs
// ---------------------------------------------------------------------------

/**
 * The outputs Android reports right now, kept live by [AudioDeviceCallback].
 *
 * This is a read-only inventory: plugging a headset, connecting Bluetooth
 * audio or attaching a USB DAC adds a row without reopening the screen. A
 * device that reports nothing shows "none" — an empty list is never presented
 * as "no audio hardware exists".
 */
@Composable
private fun AudioOutputsTest() {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(AudioManager::class.java) }
    var outputs by remember { mutableStateOf(audioOutputs(audioManager)) }

    DisposableEffect(audioManager) {
        val callback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
                outputs = audioOutputs(audioManager)
            }

            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
                outputs = audioOutputs(audioManager)
            }
        }
        runCatching { audioManager?.registerAudioDeviceCallback(callback, null) }
        onDispose { runCatching { audioManager?.unregisterAudioDeviceCallback(callback) } }
    }

    MaxGroup {
        val unavailable = stringResource(R.string.device_lab_value_unavailable)
        if (outputs.isEmpty()) {
            LabValueRow(
                title = stringResource(R.string.device_lab_outputs_title),
                value = stringResource(R.string.device_lab_output_none),
            )
        } else {
            outputs.forEachIndexed { index, output ->
                if (index > 0) MaxGroupDivider()
                LabValueRow(
                    title = stringResource(outputTypeLabel(output.type)),
                    value = output.name.ifBlank { unavailable },
                )
            }
        }
    }
}

private data class AudioOutput(val type: Int, val name: String)

private fun audioOutputs(manager: AudioManager?): List<AudioOutput> =
    runCatching {
        manager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS)?.map { device ->
            AudioOutput(device.type, device.productName?.toString().orEmpty())
        }.orEmpty()
    }.getOrDefault(emptyList())

private fun outputTypeLabel(type: Int): Int = when (type) {
    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> R.string.device_lab_output_speaker
    AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> R.string.device_lab_output_earpiece
    AudioDeviceInfo.TYPE_WIRED_HEADSET -> R.string.device_lab_output_wired_headset
    AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> R.string.device_lab_output_wired_headphones
    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
    AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> R.string.device_lab_output_bluetooth
    AudioDeviceInfo.TYPE_USB_DEVICE,
    AudioDeviceInfo.TYPE_USB_HEADSET -> R.string.device_lab_output_usb
    else -> R.string.device_lab_output_other
}

// ---------------------------------------------------------------------------
// Flashlight
// ---------------------------------------------------------------------------

@Composable
private fun TorchTest(onError: () -> Unit) {
    val context = LocalContext.current
    val manager = remember { context.getSystemService(CameraManager::class.java) }
    val torchId = remember {
        runCatching {
            manager.cameraIdList.firstOrNull { id ->
                manager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        }.getOrNull()
    }
    var on by remember { mutableStateOf(false) }
    val available = torchId != null
    val unavailable = stringResource(R.string.device_lab_unavailable)

    DisposableEffect(Unit) {
        onDispose {
            if (torchId != null) runCatching { manager.setTorchMode(torchId, false) }
        }
    }

    MaxGroup {
        MaxSwitchRow(
            title = stringResource(R.string.device_lab_torch_title),
            subtitle = if (available) {
                stringResource(R.string.device_lab_torch_desc)
            } else {
                unavailable
            },
            icon = Icons.Rounded.FlashOn,
            iconTone = MaxTone.Caution,
            checked = on,
            enabled = available,
            lockedReason = if (available) null else unavailable,
            onCheckedChange = { next ->
                if (torchId == null) {
                    onError()
                } else {
                    runCatching { manager.setTorchMode(torchId, next) }
                        .onSuccess { on = next }
                        .onFailure { onError() }
                }
            },
        )
    }
}

// ---------------------------------------------------------------------------
// Microphone
// ---------------------------------------------------------------------------

/** معدّل العيّنة القياسي: 44.1 kHz أحادي 16-bit، وهو ما يقبله كل جهاز تقريبًا. */
private const val MIC_SAMPLE_RATE = 44_100

/** أرضية العرض: -90 dBFS تعني «صمت» في عدّاد 16-bit، لا «صفر طاقة». */
private const val MIC_FLOOR_DB = -90f

/**
 * مستوى الميكروفون — قياس حيّ بلا تسجيل.
 *
 * **ولماذا لا يُحفظ شيء:** الغرض أن يعرف المستخدم أن الميكروفون يلتقط، لا أن يُنتج ملفًّا. فالعيّنات
 * تُقرأ وتُطرح بعد حساب RMS ولا تُكتب في ذاكرة ولا تُرسل. والإيقاف يستدعي `stop` و`release` في
 * `finally`، فحتى الخروج المفاجئ من الشاشة لا يترك التقاطًا يعمل في الخلفية.
 *
 * **والحدّ يُعلن:** القيمة dBFS نسبةً إلى أقصى ما يمثّله 16-bit، لا مستوى صوت مطلقًا؛ ولا تُقارن
 * بين جهازين لأن حساسية الميكروفون تختلف. والإذن يُطلب عند أوّل استعمال بضغطة صريحة.
 */
@Composable
private fun MicrophoneTest() {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var on by remember { mutableStateOf(false) }
    var levelDb by remember { mutableStateOf<Float?>(null) }
    var peakDb by remember { mutableStateOf<Float?>(null) }
    var unavailable by remember { mutableStateOf(false) }
    val unavailableText = stringResource(R.string.device_lab_unavailable)
    val deniedText = stringResource(R.string.device_lab_mic_denied)

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { allowed -> granted = allowed }

    LaunchedEffect(on) {
        if (!on) {
            levelDb = null
            return@LaunchedEffect
        }
        peakDb = null
        unavailable = false
        withContext(Dispatchers.IO) {
            val minBuffer = AudioRecord.getMinBufferSize(
                MIC_SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            if (minBuffer <= 0) {
                unavailable = true
                on = false
                return@withContext
            }
            val record = runCatching {
                AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    MIC_SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBuffer * 2
                )
            }.getOrNull()
            if (record == null || record.state != AudioRecord.STATE_INITIALIZED) {
                runCatching { record?.release() }
                unavailable = true
                on = false
                return@withContext
            }
            val buffer = ShortArray(minBuffer)
            var lastUiAt = 0L
            try {
                record.startRecording()
                while (isActive) {
                    val read = record.read(buffer, 0, buffer.size)
                    if (read <= 0) continue
                    var sum = 0.0
                    for (index in 0 until read) {
                        val sample = buffer[index] / 32768.0
                        sum += sample * sample
                    }
                    val rms = sqrt(sum / read)
                    val db = (20.0 * log10(rms.coerceAtLeast(1e-7)))
                        .toFloat()
                        .coerceIn(MIC_FLOOR_DB, 0f)
                    val now = System.nanoTime()
                    // الكتابة إلى حالة الواجهة عشر مرّات في الثانية: أسرع من ذلك لا يُقرأ بالعين.
                    if (now - lastUiAt >= 100_000_000L) {
                        lastUiAt = now
                        levelDb = db
                        peakDb = maxOf(peakDb ?: db, db)
                    }
                }
            } finally {
                runCatching { record.stop() }
                runCatching { record.release() }
            }
        }
    }

    MaxGroup {
        if (!granted) {
            MaxNavigationRow(
                title = stringResource(R.string.device_lab_mic_allow),
                subtitle = stringResource(R.string.device_lab_mic_allow_desc),
                icon = Icons.Rounded.Mic,
                iconTone = MaxTone.Accent,
                onClick = { launcher.launch(Manifest.permission.RECORD_AUDIO) }
            )
            MaxGroupDivider()
        }
        val level = levelDb
        val peak = peakDb
        MaxSwitchRow(
            title = stringResource(R.string.device_lab_mic_title),
            subtitle = when {
                !granted -> deniedText
                unavailable -> unavailableText
                on && level != null && peak != null ->
                    stringResource(R.string.device_lab_mic_level, level, peak)
                else -> stringResource(R.string.device_lab_mic_desc)
            },
            icon = Icons.Rounded.Mic,
            iconTone = MaxTone.Accent,
            checked = on,
            enabled = granted && !unavailable,
            lockedReason = if (granted) null else deniedText,
            onCheckedChange = { next -> on = next }
        )
        if (on && level != null) {
            MicLevelBar(level = level)
        }
    }
}

/** شريط المستوى: -90..0 dBFS إلى 0..1 من العرض — الرقم نفسه الذي في السطر، بلا مقياس موازٍ. */
@Composable
private fun MicLevelBar(level: Float) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    val accent = MaterialTheme.colorScheme.primary
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MaxSpace.groupPadding)
            .height(10.dp)
    ) {
        val fraction = ((level - MIC_FLOOR_DB) / -MIC_FLOOR_DB).coerceIn(0f, 1f)
        drawRect(color = track)
        if (fraction > 0f) {
            drawRect(color = accent, size = Size(size.width * fraction, size.height))
        }
    }
}

// ---------------------------------------------------------------------------
// Sensors
// ---------------------------------------------------------------------------
@Composable
private fun SensorTest() {
    val context = LocalContext.current
    var accel by remember { mutableStateOf<String?>(null) }
    var gyro by remember { mutableStateOf<String?>(null) }
    var light by remember { mutableStateOf<String?>(null) }
    var proximity by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        val manager = context.getSystemService(SensorManager::class.java)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val values = event.values
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> if (values.size >= 3) {
                        accel = formatVector(values, "m/s²")
                    }
                    Sensor.TYPE_GYROSCOPE -> if (values.size >= 3) {
                        gyro = formatVector(values, "rad/s")
                    }
                    Sensor.TYPE_LIGHT -> if (values.isNotEmpty()) {
                        light = String.format(Locale.getDefault(), "%.1f lx", values[0])
                    }
                    Sensor.TYPE_PROXIMITY -> if (values.isNotEmpty()) {
                        proximity = String.format(Locale.getDefault(), "%.1f cm", values[0])
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        listOf(
            Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_GYROSCOPE,
            Sensor.TYPE_LIGHT,
            Sensor.TYPE_PROXIMITY,
        ).forEach { type ->
            manager.getDefaultSensor(type)?.let {
                manager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
            }
        }
        onDispose { manager.unregisterListener(listener) }
    }

    MaxGroup {
        MaxRow(
            title = stringResource(R.string.device_lab_sensors_title),
            subtitle = stringResource(R.string.device_lab_sensors_desc),
            icon = Icons.Rounded.Sensors,
            iconTone = MaxTone.Accent,
        )
        MaxGroupDivider()
        SensorValueRow(R.string.device_lab_accel, accel)
        MaxGroupDivider()
        SensorValueRow(R.string.device_lab_gyro, gyro)
        MaxGroupDivider()
        SensorValueRow(R.string.device_lab_light, light)
        MaxGroupDivider()
        SensorValueRow(R.string.device_lab_proximity, proximity)
    }
}

private fun formatVector(values: FloatArray, unit: String): String =
    String.format(
        Locale.getDefault(),
        "%.2f · %.2f · %.2f %s",
        values[0],
        values[1],
        values[2],
        unit,
    )

@Composable
private fun SensorValueRow(titleRes: Int, value: String?) {
    MaxRow(
        title = stringResource(titleRes),
        trailing = {
            Text(
                text = value ?: stringResource(R.string.device_lab_value_unavailable),
                style = MonoValueStyleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    )
}

// ---------------------------------------------------------------------------
// Battery
// ---------------------------------------------------------------------------

private data class BatterySnapshot(
    val level: Int,
    val status: Int,
    val plugged: Int,
    val temperatureC: Float,
    val voltageMv: Int,
    val currentUa: Int,
    val health: Int,
) {
    companion object {
        fun from(context: Context, intent: Intent?): BatterySnapshot? {
            if (intent == null) return null
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val percent = if (level >= 0 && scale > 0) level * 100 / scale else -1
            val manager = context.getSystemService(BatteryManager::class.java)
            return BatterySnapshot(
                level = percent,
                status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1),
                plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1),
                temperatureC = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE) / 10f,
                voltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1),
                currentUa = manager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
                    ?: Int.MIN_VALUE,
                health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1),
            )
        }
    }
}

@Composable
private fun BatteryTest() {
    val context = LocalContext.current
    val filter = remember { IntentFilter(Intent.ACTION_BATTERY_CHANGED) }
    var snapshot by remember {
        mutableStateOf(BatterySnapshot.from(context, context.registerReceiver(null, filter)))
    }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                snapshot = BatterySnapshot.from(context, intent)
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        onDispose { context.unregisterReceiver(receiver) }
    }

    val data = snapshot
    val unavailable = stringResource(R.string.device_lab_value_unavailable)
    val statusRes = when (data?.status) {
        BatteryManager.BATTERY_STATUS_CHARGING -> R.string.device_lab_status_charging
        BatteryManager.BATTERY_STATUS_DISCHARGING -> R.string.device_lab_status_discharging
        BatteryManager.BATTERY_STATUS_FULL -> R.string.device_lab_status_full
        BatteryManager.BATTERY_STATUS_NOT_CHARGING -> R.string.device_lab_status_not_charging
        else -> null
    }
    val pluggedRes = when (data?.plugged) {
        BatteryManager.BATTERY_PLUGGED_AC -> R.string.device_lab_plugged_ac
        BatteryManager.BATTERY_PLUGGED_USB -> R.string.device_lab_plugged_usb
        BatteryManager.BATTERY_PLUGGED_WIRELESS -> R.string.device_lab_plugged_wireless
        else -> R.string.device_lab_plugged_none
    }
    val healthRes = when (data?.health) {
        BatteryManager.BATTERY_HEALTH_GOOD -> R.string.device_lab_health_good
        BatteryManager.BATTERY_HEALTH_OVERHEAT -> R.string.device_lab_health_overheat
        BatteryManager.BATTERY_HEALTH_DEAD -> R.string.device_lab_health_dead
        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> R.string.device_lab_health_over_voltage
        BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> R.string.device_lab_health_failure
        BatteryManager.BATTERY_HEALTH_COLD -> R.string.device_lab_health_cold
        else -> R.string.device_lab_health_unknown
    }

    MaxGroup {
        MaxRow(
            title = stringResource(R.string.device_lab_battery_title),
            subtitle = stringResource(R.string.device_lab_battery_desc),
            icon = Icons.Rounded.BatteryChargingFull,
            iconTone = MaxTone.Positive,
        )
        MaxGroupDivider()
        LabValueRow(
            title = stringResource(R.string.device_lab_battery_level),
            value = data?.level?.takeIf { it >= 0 }?.let { "$it%" } ?: unavailable,
        )
        MaxGroupDivider()
        LabValueRow(
            title = stringResource(R.string.device_lab_battery_status),
            value = statusRes?.let { stringResource(it) } ?: unavailable,
        )
        MaxGroupDivider()
        LabValueRow(
            title = stringResource(R.string.device_lab_battery_plugged),
            value = stringResource(pluggedRes),
        )
        MaxGroupDivider()
        LabValueRow(
            title = stringResource(R.string.device_lab_battery_temp),
            value = data?.temperatureC
                ?.takeIf { it > -100f && it < 100f }
                ?.let { String.format(Locale.getDefault(), "%.1f °C", it) }
                ?: unavailable,
        )
        MaxGroupDivider()
        LabValueRow(
            title = stringResource(R.string.device_lab_battery_voltage),
            value = data?.voltageMv
                ?.takeIf { it > 0 }
                ?.let { String.format(Locale.getDefault(), "%d mV", it) }
                ?: unavailable,
        )
        MaxGroupDivider()
        LabValueRow(
            title = stringResource(R.string.device_lab_battery_current),
            value = data?.currentUa
                ?.takeIf { it != 0 && it != Int.MIN_VALUE }
                ?.let { String.format(Locale.getDefault(), "%d mA", it / 1000) }
                ?: unavailable,
        )
        MaxGroupDivider()
        LabValueRow(
            title = stringResource(R.string.device_lab_battery_health),
            value = stringResource(healthRes),
        )
    }
}

// ---------------------------------------------------------------------------
// Small shared pieces
// ---------------------------------------------------------------------------

@Composable
private fun LabActionRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    lockedReason: String?,
    onClick: () -> Unit,
) {
    MaxRow(
        title = title,
        icon = icon,
        iconTone = MaxTone.Accent,
        enabled = enabled,
        onClick = onClick,
        subtitle = lockedReason,
    )
}

@Composable
private fun LabValueRow(title: String, value: String) {
    MaxRow(
        title = title,
        trailing = {
            Text(
                text = value,
                style = MonoValueStyleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    )
}

// ---------------------------------------------------------------------------
// Display capabilities
// ---------------------------------------------------------------------------

/**
 * ما يُعلنه هذا اللوح عن نفسه: الدقّة والتحديث، وأنواع HDR، واللون الواسع.
 *
 * **ولماذا هنا مع أن معدّل التحديث يُقرأ في مكان آخر:** `AppMonitor` يقرأه ليضبط كل تطبيق، وهذا
 * القسم يقرأه ليقول للمستخدم **ما يدعمه عتاده**. الأول قرار، والثاني جواب. وما لا يُعلنه النظام
 * يُكتب «غير معلوم» بدل صفر أو قائمة فارغة تُقرأ كأنها «لا شيء».
 */
@Composable
private fun DisplayCapabilities() {
    val context = LocalContext.current
    val display = remember {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.display
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(WindowManager::class.java).defaultDisplay
            }
        }.getOrNull()
    }
    val none = stringResource(R.string.device_lab_display_none)

    if (display == null) {
        MaxGroup {
            LabValueRow(
                title = stringResource(R.string.device_lab_display_mode_title),
                value = none,
            )
        }
        return
    }

    val mode = remember(display) { runCatching { display.mode }.getOrNull() }
    val modes = remember(display) { runCatching { display.supportedModes.toList() }.getOrDefault(emptyList()) }
    val hdr = remember(display) { runCatching { display.hdrCapabilities }.getOrNull() }
    val wideGamut = remember(display) { runCatching { display.isWideColorGamut }.getOrNull() }

    val resolutions = modes.map { it.physicalWidth to it.physicalHeight }.distinct()
    val rates = modes.map { it.refreshRate }.distinct().sorted()
    val modesText = if (modes.isEmpty()) {
        none
    } else {
        resolutions.take(3).joinToString(" / ") { (width, height) -> "$width×$height" } +
            " · " + rates.joinToString("/") { it.toInt().toString() } + " Hz"
    }

    val hdrText = hdr?.let { capabilities ->
        val names = capabilities.supportedHdrTypes.map(::hdrTypeName).filter { it.isNotEmpty() }
        if (names.isEmpty()) none else names.joinToString(" · ")
    } ?: none

    val luminance = hdr?.let {
        stringResource(
            R.string.device_lab_display_nits,
            it.desiredMaxLuminance.toInt(),
            it.desiredMaxAverageLuminance.toInt(),
        )
    } ?: none

    MaxGroup {
        LabValueRow(
            title = stringResource(R.string.device_lab_display_mode_title),
            value = mode?.let { "${it.physicalWidth} × ${it.physicalHeight} · ${it.refreshRate.toInt()} Hz" }
                ?: none,
        )
        MaxGroupDivider()
        LabValueRow(
            title = stringResource(R.string.device_lab_display_modes_title),
            value = modesText,
        )
        MaxGroupDivider()
        LabValueRow(
            title = stringResource(R.string.device_lab_display_hdr_title),
            value = hdrText,
        )
        MaxGroupDivider()
        LabValueRow(
            title = stringResource(R.string.device_lab_display_luminance_title),
            value = luminance,
        )
        MaxGroupDivider()
        LabValueRow(
            title = stringResource(R.string.device_lab_display_gamut_title),
            value = when (wideGamut) {
                true -> stringResource(R.string.yes)
                false -> stringResource(R.string.no)
                null -> none
            },
        )
    }
}

/** أسماء أنواع HDR كما تُعلنها المنصّة — أسماء تقنية لا تُترجم. */
private fun hdrTypeName(type: Int): String = when (type) {
    Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION -> "Dolby Vision"
    Display.HdrCapabilities.HDR_TYPE_HDR10 -> "HDR10"
    Display.HdrCapabilities.HDR_TYPE_HLG -> "HLG"
    Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS -> "HDR10+"
    else -> ""
}

// ---------------------------------------------------------------------------
// Video decoders
// ---------------------------------------------------------------------------

/** سقف الصفوف المعروضة: قائمة المُفكّكات قد تبلغ العشرات، والأسئلة الحقيقية ثلاثة أو أربعة. */
private const val MAX_CODEC_ROWS = 8

/** مُفكّك واحد كما يُعلنه النظام: الصيغة وأكبر إطار يقبله. */
private data class VideoDecoder(
    val mime: String,
    val maxWidth: Int,
    val maxHeight: Int,
    val maxFps: Int?,
)

/**
 * ما يستطيع هذا الجهاز فكّه فعلًا.
 *
 * **والفرق بين هذا وبين «يدعم AV1» في ورقة المواصفات:** هذه قراءة من `MediaCodecList` لجهازك
 * أنت — وحده يعلن مفكّكاته وأكبر إطار لكل صيغة. ولذلك تُقرأ في `Dispatchers.IO` وتُعرض بحالة
 * تحميل صريحة، لا تُحسب في التكوين.
 *
 * **والصيغة التي تُعلن بلا حدود تقرأ:** `videoCapabilities` قد ترمي على نوع غير مرئي أو غير
 * مهيّأ، فكل قراءة في `runCatching`، وما لم يُقرأ يُسقَط الصفّ بدل اختراع حدّ.
 */
@Composable
private fun CodecCapabilities() {
    var decoders by remember { mutableStateOf<List<VideoDecoder>?>(null) }

    LaunchedEffect(Unit) {
        decoders = withContext(Dispatchers.IO) { readVideoDecoders() }
    }

    val none = stringResource(R.string.device_lab_codec_none)
    val list = decoders

    MaxGroup {
        when {
            list == null -> LabValueRow(
                title = stringResource(R.string.device_lab_codec_reading),
                value = "…",
            )

            list.isEmpty() -> LabValueRow(
                title = stringResource(R.string.device_lab_section_codecs),
                value = none,
            )

            else -> {
                list.take(MAX_CODEC_ROWS).forEachIndexed { index, decoder ->
                    if (index > 0) MaxGroupDivider()
                    LabValueRow(
                        title = codecLabel(decoder.mime),
                        value = buildString {
                            append("${decoder.maxWidth}×${decoder.maxHeight}")
                            decoder.maxFps?.let { append(" · $it fps") }
                        },
                    )
                }
                val hidden = list.size - MAX_CODEC_ROWS
                if (hidden > 0) {
                    MaxGroupDivider()
                    LabValueRow(
                        title = stringResource(R.string.device_lab_codec_more, hidden),
                        value = "",
                    )
                }
            }
        }
    }
}

private fun readVideoDecoders(): List<VideoDecoder> {
    val codecs = runCatching {
        MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
    }.getOrDefault(emptyArray())

    val byMime = LinkedHashMap<String, VideoDecoder>()
    codecs.filterNot { it.isEncoder }.forEach { info ->
        info.supportedTypes.filter { it.startsWith("video/") }.forEach { mime ->
            val capabilities = runCatching { info.getCapabilitiesForType(mime) }.getOrNull()
                ?: return@forEach
            val video = runCatching { capabilities.videoCapabilities }.getOrNull()
                ?: return@forEach
            val width = runCatching { video.supportedWidths.upper }.getOrNull()
                ?: return@forEach
            val height = runCatching { video.supportedHeights.upper }.getOrNull()
                ?: return@forEach
            val fps = runCatching {
                video.getSupportedFrameRatesFor(width, height).upper.toInt()
            }.getOrNull()

            val current = byMime[mime]
            // أعلى دقّة تُعلنها أي شريحة مفكّك لهذه الصيغة — لا أوّل واحدة في القائمة.
            if (current == null || width.toLong() * height > current.maxWidth.toLong() * current.maxHeight) {
                byMime[mime] = VideoDecoder(mime, width, height, fps)
            }
        }
    }
    return byMime.values.sortedBy { it.mime }
}

/** الاسم المتعارف عليه للصيغة؛ ما لا اسم له يُعرض باسمها الفرعي كما هو. */
private fun codecLabel(mime: String): String = when {
    mime.endsWith("avc") -> "H.264 / AVC"
    mime.endsWith("hevc") -> "HEVC / H.265"
    mime.endsWith("av01") -> "AV1"
    mime.endsWith("vp9") -> "VP9"
    mime.endsWith("vp8") -> "VP8"
    mime.endsWith("mp4v-es") -> "MPEG-4"
    mime.contains("dolby-vision") -> "Dolby Vision"
    mime.contains("dolby") -> "Dolby"
    else -> mime.substringAfter('/')
}

// ---------------------------------------------------------------------------
// Wi-Fi channels
// ---------------------------------------------------------------------------

/**
 * قنوات Wi-Fi كما يراها الجهاز الآن — قراءة، لا كتابة.
 *
 * **ولماذا الإذن يُطلب بضغطة:** أندرويد يحجب نتائج المسح عن تطبيقٍ بلا إذن الموقع،
 * وطلبُ الإذن عند فتح الشاشة يُفاجئ المستخدم بسببٍ لا علاقة له بما جاء يفعله. هنا
 * يُعرض السبب أولًا، ولا يُطلب الإذن إلا بضغطة صريحة على السطر نفسه.
 *
 * **ولا توصية بقناة:** الأسطر تقول ما قِيس (القناة المتّصل بها، وعدد الشبكات على
 * القناة الأكثر ازدحامًا). «الأفضل» حكمٌ يحتاج عرض القناة والتداخل، وليس عدّ الشبكات.
 */
@Composable
private fun WifiChannels() {
    val context = LocalContext.current
    var state by remember { mutableStateOf(WifiChannelReader.read(context)) }
    val askLocation = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        state = WifiChannelReader.read(context)
    }
    val summary = state.summary

    MaxGroup {
        if (state.blocker == WifiChannelReader.Blocker.NoPermission) {
            LabActionRow(
                title = stringResource(R.string.device_lab_wifi_grant),
                icon = Icons.Rounded.Wifi,
                enabled = true,
                lockedReason = stringResource(R.string.device_lab_wifi_denied),
                onClick = { askLocation.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
            )
        } else if (state.blocker != null || summary == null) {
            LabValueRow(
                title = stringResource(R.string.device_lab_wifi_connected),
                value = stringResource(blockerLabel(state.blocker)),
            )
        } else {
            val connected = summary.connected
            LabValueRow(
                title = stringResource(R.string.device_lab_wifi_connected),
                value = if (connected == null) {
                    stringResource(R.string.device_lab_wifi_no_frequency)
                } else {
                    stringResource(
                        R.string.device_lab_wifi_value_connected,
                        connected.channel,
                        bandLabel(connected.band),
                    )
                },
            )
            MaxGroupDivider()
            LabValueRow(
                title = stringResource(R.string.device_lab_wifi_seen),
                value = stringResource(R.string.device_lab_wifi_value_seen, summary.total, summary.twoFour),
            )
            summary.busiest.firstOrNull()?.let { busiest ->
                MaxGroupDivider()
                LabValueRow(
                    title = stringResource(R.string.device_lab_wifi_busiest),
                    value = stringResource(
                        R.string.device_lab_wifi_value_busiest,
                        busiest.channel,
                        busiest.networks,
                    ),
                )
            }
            MaxGroupDivider()
            LabActionRow(
                title = stringResource(R.string.device_lab_wifi_refresh),
                icon = Icons.Rounded.RestartAlt,
                enabled = true,
                lockedReason = null,
                onClick = { state = WifiChannelReader.read(context) },
            )
        }
    }
}

@Composable
private fun bandLabel(band: WifiChannel.Band): String = stringResource(
    when (band) {
        WifiChannel.Band.TwoFourGhz -> R.string.device_lab_band_2g
        WifiChannel.Band.FiveGhz -> R.string.device_lab_band_5g
        WifiChannel.Band.SixGhz -> R.string.device_lab_band_6g
        WifiChannel.Band.Unknown -> R.string.device_lab_band_unknown
    }
)

@Composable
private fun blockerLabel(blocker: WifiChannelReader.Blocker?): Int = when (blocker) {
    WifiChannelReader.Blocker.LocationOff -> R.string.device_lab_wifi_location_off
    WifiChannelReader.Blocker.WifiOff -> R.string.device_lab_wifi_off
    WifiChannelReader.Blocker.Unreadable -> R.string.device_lab_wifi_unreadable
    WifiChannelReader.Blocker.NoPermission, null -> R.string.device_lab_wifi_unreadable
}
