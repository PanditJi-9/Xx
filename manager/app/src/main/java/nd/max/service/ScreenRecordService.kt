/* Copyright (C) 2026 Nader Magdy. All rights reserved. */
package nd.max.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.AudioAttributes
import android.media.AudioPlaybackCaptureConfiguration
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import nd.max.R
import nd.max.ui.settings.AppLanguage

/**
 * مسجّل الشاشة الداخليّ: `MediaProjection` للصورة و`AudioPlaybackCapture` لصوت اللعب نفسه.
 *
 * **ولماذا استبدل `screenrecord`:** ذلك الملفّ التنفيذيّ يكتب ٤ ميغابت/ث و٣٠ إطارًا بلا `--size`
 * (فيهبط المرمّز إلى ٧٢٠p على لوحة ١٤٤٠×٣١٢٠)، و**لا يدعم الصوت إطلاقًا** — فلا حلّ داخله للشكويين.
 * هذا المسجّل يكتب بجودة مضبوطة وبمسار صوت النظام، والملفّ في المجلّد نفسه (`Movies/MaxManager`)
 * عبر `MediaStore` لا بكتابة مباشرة، فلا يحتاج «الوصول لكل الملفّات» ولا فحص وسائط يدويًّا.
 *
 * **حدوده المعلنة (ADR-07):** الصوت يأتي من التطبيقات التي تسمح بالالتقاط (`allowAudioPlaybackCapture`،
 * وهو الافتراضيّ لأهداف ≥ 29)؛ تطبيق يمنعه يُسجَّل صامتًا لا خطأً. والالتقاط يحتاج خدمة أمامية بنوع
 * `mediaProjection` وإقرار المستخدم في حوار النظام — وكلاهما في هذا الملفّ والمانيفست.
 */
class ScreenRecordService : Service() {

    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var recorder: MediaRecorder? = null
    private var target: Uri? = null
    private var descriptor: ParcelFileDescriptor? = null
    private var stopping = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            shutdown()
            return START_NOT_STICKY
        }
        // النوع يُعلن قبل `getMediaProjection` (شرط أندرويد ١٤+)، والإشعار هو العقد نفسه.
        startNotice()
        if (active) return START_NOT_STICKY
        val code = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
        @Suppress("DEPRECATION")
        val data = intent?.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
        if (code == 0 || data == null) {
            shutdown()
            return START_NOT_STICKY
        }
        begin(code, data)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        shutdown()
        super.onDestroy()
    }

    private fun begin(resultCode: Int, data: Intent) {
        // ١) الإذن: التوكن الذي أعاده حوار النظام للتوّ — يُستهلك مرة واحدة.
        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
        val created = manager?.let { runCatching { it.getMediaProjection(resultCode, data) }.getOrNull() }
        if (created == null) {
            shutdown()
            return
        }
        // ٢) النداء قبل `createVirtualDisplay` إلزاميّ على أندرويد ١٤+، وهو أيضًا الموضع الذي
        //    يُبلّغنا فيه النظام أن المستخدم أوقف الالتقاط من الشريط.
        created.registerCallback(
            object : MediaProjection.Callback() {
                override fun onStop() {
                    shutdown()
                }
            },
            null
        )
        projection = created

        // ٣) المقاس: أبعاد اللوحة الحقيقية، بسقف ١٩٢٠ على الضلع الأطول وبأبعاد زوجية (شرط H.264).
        val (width, height) = captureSize()
        val uri = createTarget() ?: run {
            shutdown()
            return
        }
        target = uri

        // ٤) المرمّز: ٦٠ إطارًا ثم ٣٠ — بعض مرمّزات 1440p ترفض ٦٠، فلا ينتهي الأمر بلا تسجيل.
        val ready = FPS_LADDER.any { fps ->
            runCatching { recorder = build(width, height, fps, uri) }.isSuccess
        }
        if (!ready) {
            shutdown()
            return
        }
        val started = runCatching { recorder?.start() }.isSuccess
        if (!started) {
            shutdown()
            return
        }
        virtualDisplay = runCatching {
            created.createVirtualDisplay(
                DISPLAY_NAME,
                width,
                height,
                resources.displayMetrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                recorder?.surface,
                null,
                null
            )
        }.getOrNull()
        if (virtualDisplay == null) {
            shutdown()
            return
        }
        active = true
    }

    private fun build(width: Int, height: Int, fps: Int, uri: Uri): MediaRecorder {
        val pfd = contentResolver.openFileDescriptor(uri, "w") ?: error("no descriptor for $uri")
        try {
            val capture = AudioPlaybackCaptureConfiguration
                .Builder(requireNotNull(projection))
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(AudioAttributes.USAGE_GAME)
                .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                .build()
            @Suppress("DEPRECATION")
            val made = MediaRecorder()
            made.setAudioSource(MediaRecorder.AudioSource.MIC)
            made.setAudioPlaybackCaptureConfig(capture)
            made.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            made.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            made.setVideoSize(width, height)
            made.setVideoFrameRate(fps)
            made.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            made.setVideoEncodingBitRate(BIT_RATE)
            made.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            made.setAudioChannels(2)
            made.setAudioSamplingRate(AUDIO_RATE)
            made.setAudioEncodingBitRate(AUDIO_BIT_RATE)
            made.setOutputFile(pfd.fileDescriptor)
            made.prepare()
            descriptor?.let { runCatching { it.close() } }
            descriptor = pfd
            return made
        } catch (error: Throwable) {
            runCatching { pfd.close() }
            throw error
        }
    }

    /** المجلّد نفسه الذي يقرأه معرض المقاطع (`Movies/MaxManager`)، لكن بقيد `MediaStore` لا بمسار ملفّ. */
    private fun createTarget(): Uri? = runCatching {
        val name = "Max_" + java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US)
            .format(java.util.Date()) + ".mp4"
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, SAVE_DIR)
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
    }.getOrNull()

    private fun captureSize(): Pair<Int, Int> {
        val manager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        manager.defaultDisplay.getRealMetrics(metrics)
        var width = metrics.widthPixels
        var height = metrics.heightPixels
        val longest = maxOf(width, height)
        if (longest > MAX_SIDE) {
            val scale = MAX_SIDE.toFloat() / longest
            width = (width * scale).toInt()
            height = (height * scale).toInt()
        }
        return (width - width % 2).coerceAtLeast(2) to (height - height % 2).coerceAtLeast(2)
    }

    /** الإيقاف من زرّ الإشعار أو من اللوحة أو من المستخدم في شريط النظام — مسار واحد للتنظيف. */
    private fun shutdown() {
        if (stopping) return
        stopping = true
        runCatching { virtualDisplay?.release() }
        virtualDisplay = null
        runCatching { recorder?.stop() }
        runCatching { recorder?.reset() }
        runCatching { recorder?.release() }
        recorder = null
        runCatching { projection?.stop() }
        projection = null
        finishTarget()
        active = false
        runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
        stopSelf()
        stopping = false
    }

    private fun finishTarget() {
        val uri = target
        target = null
        runCatching { descriptor?.close() }
        descriptor = null
        if (uri == null) return
        // `IS_PENDING = 0` هو إعلان «انتهى الملفّ» لمعرض الوسائط، فلا يظهر نصف مسجَّل.
        runCatching {
            contentResolver.update(
                uri,
                ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) },
                null,
                null
            )
        }
    }

    private fun startNotice() {
        val text = AppLanguage.wrap(this)
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL,
                text.getString(R.string.gs_recording_channel),
                NotificationManager.IMPORTANCE_LOW
            )
        )
        val stop = PendingIntent.getService(
            this,
            0,
            Intent(this, ScreenRecordService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(this, CHANNEL)
            .setContentTitle(text.getString(R.string.gs_recording_title))
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .addAction(0, text.getString(R.string.gs_recording_stop), stop)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        const val EXTRA_RESULT_CODE = "nd.max.extra.PROJECTION_CODE"
        const val EXTRA_RESULT_DATA = "nd.max.extra.PROJECTION_DATA"
        private const val ACTION_STOP = "nd.max.action.STOP_SCREEN_RECORD"
        private const val CHANNEL = "maxmanager_screen_record"
        private const val NOTIFICATION_ID = 2411
        private const val DISPLAY_NAME = "MaxManager"
        private const val SAVE_DIR = "Movies/MaxManager"
        private const val MAX_SIDE = 1920
        private const val BIT_RATE = 24_000_000
        private const val AUDIO_RATE = 48_000
        private const val AUDIO_BIT_RATE = 192_000
        private val FPS_LADDER = intArrayOf(60, 30)

        /** الحقيقة من الخدمة نفسها، لا من ذاكرة الواجهة: إن ماتت توقّف التسجيل معها. */
        @Volatile
        private var active = false

        fun isRecording(): Boolean = active

        fun start(context: Context, resultCode: Int, data: Intent) {
            val intent = Intent(context, ScreenRecordService::class.java)
                .putExtra(EXTRA_RESULT_CODE, resultCode)
                .putExtra(EXTRA_RESULT_DATA, data)
            runCatching { context.startForegroundService(intent) }
        }

        fun stop(context: Context) {
            runCatching {
                context.startService(
                    Intent(context, ScreenRecordService::class.java).setAction(ACTION_STOP)
                )
            }
        }
    }
}
