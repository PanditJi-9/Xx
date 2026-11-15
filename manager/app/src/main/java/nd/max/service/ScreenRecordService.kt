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
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.view.Surface
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import nd.max.R
import nd.max.ui.settings.AppLanguage
import java.nio.ByteBuffer

/**
 * مسجّل الشاشة الداخليّ: `MediaProjection` للصورة و`AudioPlaybackCapture` لصوت اللعب نفسه.
 *
 * **ولماذا استبدل `screenrecord`:** ذلك الملفّ التنفيذيّ يكتب ٤ ميغابت/ث و٣٠ إطارًا بلا `--size`
 * (فيهبط المرمّز إلى ٧٢٠p على لوحة ١٤٤٠×٣١٢٠)، و**لا يدعم الصوت إطلاقًا** — فلا حلّ داخله للشكويين.
 * هذا المسجّل يكتب بجودة مضبوطة وبمسار صوت النظام، والملفّ في المجلّد نفسه (`Movies/MaxManager`)
 * عبر `MediaStore` لا بكتابة مباشرة، فلا يحتاج «الوصول لكل الملفّات» ولا فحص وسائط يدويًّا.
 *
 * **ولماذا لا `MediaRecorder`:** لا يملك `setAudioPlaybackCaptureConfig` أصلًا (قِيس في مرجع
 * `android.media.MediaRecorder`: كل متاحه `setAudioSource/Encoder/Channels/EncodingBitRate/
 * AudioProfile/SamplingRate`)، فصوت الالتقاط لا يُركَب عليه. لذلك المسار: صورة H.264 من سطح
 * `MediaCodec`، وصوت AAC من `AudioRecord` المُلتقِط، ثم `MediaMuxer` يجمع المسارين في ملفّ واحد.
 *
 * **حدوده المعلنة (ADR-07):** الصوت يأتي من التطبيقات التي تسمح بالالتقاط (`allowAudioPlaybackCapture`،
 * وهو الافتراضيّ لأهداف ≥ 29)؛ تطبيق يمنعه يُسجَّل صامتًا لا خطأً. والالتقاط يحتاج خدمة أمامية بنوع
 * `mediaProjection` وإقرار المستخدم في حوار النظام — وكلاهما في هذا الملفّ والمانيفست.
 */
class ScreenRecordService : Service() {

    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var inputSurface: Surface? = null
    private var videoCodec: MediaCodec? = null
    private var audioCodec: MediaCodec? = null
    private var audioRecord: AudioRecord? = null
    private var muxer: MediaMuxer? = null
    private var videoThread: Thread? = null
    private var audioThread: Thread? = null
    private var target: Uri? = null
    private var descriptor: ParcelFileDescriptor? = null

    private val muxLock = Any()
    private var videoFormat: MediaFormat? = null
    private var audioFormat: MediaFormat? = null
    private var videoTrack = -1
    private var audioTrack = -1
    private var muxerStarted = false
    private var wroteSamples = false

    @Volatile
    private var running = false

    /** يُرفع إن لم يبدأ الصوت (سماح التقاط مرفوض أو مسجّل مشغول) فتنتهي المهلة ويبدأ الفيديو وحده. */
    @Volatile
    private var audioUnavailable = false

    /** يُقرأ من خيطَي الاستنزاف، فـ`@Volatile` شرط (بلاها قد يبقى الصوت في حلقة قراءة أبديّة). */
    @Volatile
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

        // ٣) الهدف: صفّ `MediaStore` بدل مسار ملفّ، ثم واصف يُكتب عليه الملفّ النهائيّ.
        val uri = createTarget() ?: run {
            shutdown()
            return
        }
        target = uri
        val pfd = runCatching { contentResolver.openFileDescriptor(uri, "w") }.getOrNull()
        if (pfd == null) {
            shutdown()
            return
        }
        descriptor = pfd

        // ٤) المراميز: ٦٠ إطارًا ثم ٣٠ — بعض مرمّزات 1440p ترفض ٦٠، فلا ينتهي الأمر بلا تسجيل.
        val (width, height) = captureSize()
        val prepared = FPS_LADDER.any { fps -> runCatching { openPipeline(width, height, fps, pfd) }.isSuccess }
        if (!prepared) {
            shutdown()
            return
        }
        val surface = inputSurface
        val video = videoCodec
        if (surface == null || video == null) {
            shutdown()
            return
        }

        // ٥) المرآة: كل ما تعرضه اللوحة يُسلَّم لسطح المرمّز مباشرةً (بلا نسخ في الذاكرة).
        virtualDisplay = runCatching {
            created.createVirtualDisplay(
                DISPLAY_NAME,
                width,
                height,
                resources.displayMetrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                surface,
                null,
                null
            )
        }.getOrNull()
        if (virtualDisplay == null) {
            shutdown()
            return
        }

        running = true
        videoThread = Thread({ drainVideo(video) }, "MaxManager-record-video").also { it.start() }
        val audio = audioRecord
        val audioEnc = audioCodec
        if (audio != null && audioEnc != null) {
            audioThread = Thread({ drainAudio(audio, audioEnc) }, "MaxManager-record-audio").also { it.start() }
        } else {
            audioUnavailable = true
        }
        active = true
    }

    /** يجهّز الجامع والمرمّزين. يُرمى استثناء إن رفض المرمّز المقاس، فيُجرَّب الإطار التالي. */
    private fun openPipeline(width: Int, height: Int, fps: Int, pfd: ParcelFileDescriptor) {
        // محاولة سابقة (٦٠ إطارًا رفضها المرمّز) قد تركت جامعًا ومرمّزًا وواصفًا مفتوحًا: تُحرَّر قبل
        // بناء الجديد، وإلا بقي الواصف نفسه مملوكًا لجامعَين فسقطت الكتابة.
        runCatching { videoCodec?.release() }
        videoCodec = null
        runCatching { inputSurface?.release() }
        inputSurface = null
        runCatching { audioCodec?.release() }
        audioCodec = null
        runCatching { audioRecord?.release() }
        audioRecord = null
        runCatching { muxer?.release() }
        muxer = null
        audioUnavailable = false

        muxer = MediaMuxer(pfd.fileDescriptor, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        val videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height)
        videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
        videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
        videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, fps)
        videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_SECONDS)
        val video = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        video.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        val surface = video.createInputSurface()
        video.start()
        videoCodec = video
        inputSurface = surface

        // الصوت اختياريّ بحقّه: فشله لا يُسقط التسجيل، لكنه يُعلَن (`audioUnavailable`).
        if (!openAudio()) audioUnavailable = true
    }

    private fun openAudio(): Boolean {
        val proj = projection ?: return false
        return try {
            val capture = AudioPlaybackCaptureConfiguration.Builder(proj)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(AudioAttributes.USAGE_GAME)
                .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                .build()
            val format = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(AUDIO_RATE)
                .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
                .build()
            val minBytes = AudioRecord.getMinBufferSize(
                AUDIO_RATE,
                AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(PCM_BYTES)
            val record = AudioRecord.Builder()
                .setAudioFormat(format)
                .setBufferSizeInBytes(minBytes * 2)
                .setAudioPlaybackCaptureConfig(capture)
                .build()
            if (record.state != AudioRecord.STATE_INITIALIZED) {
                runCatching { record.release() }
                return false
            }
            val spec = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, AUDIO_RATE, CHANNELS)
                .apply {
                    setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                    setInteger(MediaFormat.KEY_BIT_RATE, AUDIO_BIT_RATE)
                    setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, PCM_BYTES * 2)
                }
            val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            codec.configure(spec, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()
            audioRecord = record
            audioCodec = codec
            true
        } catch (error: Throwable) {
            runCatching { audioCodec?.release() }
            audioCodec = null
            runCatching { audioRecord?.release() }
            audioRecord = null
            false
        }
    }

    /** مسار الصورة: كل عيّنة يخرجها المرمّز تُكتب في الجامع بمجرّد أن يصبح جاهزًا. */
    private fun drainVideo(codec: MediaCodec) {
        val info = MediaCodec.BufferInfo()
        while (running) {
            val index = runCatching { codec.dequeueOutputBuffer(info, TIMEOUT_US) }
                .getOrDefault(MediaCodec.INFO_TRY_AGAIN_LATER)
            when {
                index == MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> registerTrack(true, codec.outputFormat)
                index >= 0 -> {
                    val buffer = runCatching { codec.getOutputBuffer(index) }.getOrNull()
                    val isConfig = info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0
                    if (buffer != null && info.size > 0 && !isConfig) writeSample(true, buffer, info)
                    runCatching { codec.releaseOutputBuffer(index, false) }
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
                }
            }
        }
    }

    /** مسار الصوت: PCM من الالتقاط ← مرمّز AAC ← الجامع، بتوقيت متراكم من عدد الأُطر. */
    private fun drainAudio(record: AudioRecord, codec: MediaCodec) {
        val info = MediaCodec.BufferInfo()
        val pcm = ByteArray(PCM_BYTES)
        var frames = 0L
        runCatching { record.startRecording() }
        while (running) {
            val read = runCatching { record.read(pcm, 0, pcm.size) }.getOrDefault(0)
            // `read <= 0` بعد `stopping` يعني أن الالتقاط توقّف: الخروج لا الدوران على معالج مشغول.
            if (read <= 0) {
                if (stopping) break
                continue
            }
            var offset = 0
            while (offset < read && running) {
                val index = runCatching { codec.dequeueInputBuffer(TIMEOUT_US) }.getOrDefault(-1)
                if (index < 0) break
                val input = runCatching { codec.getInputBuffer(index) }.getOrNull() ?: break
                input.clear()
                val chunk = minOf(read - offset, input.remaining())
                input.put(pcm, offset, chunk)
                codec.queueInputBuffer(index, 0, chunk, frames * 1_000_000L / AUDIO_RATE, 0)
                frames += chunk / BYTES_PER_FRAME
                offset += chunk
            }
            drainAudioEncoder(codec, info)
        }
        runCatching { record.stop() }
    }

    private fun drainAudioEncoder(codec: MediaCodec, info: MediaCodec.BufferInfo) {
        while (true) {
            val index = runCatching { codec.dequeueOutputBuffer(info, 0) }.getOrDefault(MediaCodec.INFO_TRY_AGAIN_LATER)
            if (index == MediaCodec.INFO_TRY_AGAIN_LATER) return
            if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                registerTrack(false, codec.outputFormat)
                continue
            }
            if (index < 0) continue
            val buffer = runCatching { codec.getOutputBuffer(index) }.getOrNull()
            val isConfig = info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0
            if (buffer != null && info.size > 0 && !isConfig) writeSample(false, buffer, info)
            runCatching { codec.releaseOutputBuffer(index, false) }
            if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) return
        }
    }

    private fun registerTrack(isVideo: Boolean, format: MediaFormat) {
        synchronized(muxLock) {
            if (isVideo) {
                if (videoTrack < 0) videoFormat = format
            } else {
                if (audioTrack < 0) audioFormat = format
            }
            // لا يبدأ الجامع هنا إلا بالمسارين معًا: مسار الفيديو يُعلن تنسيقه أولًا دائمًا، ولو بدأ
            // الجامع عنده وحده لضاع صوت التسجيل كلّه بلا أن يظهر خطأ. مسار «الصوت تعذّر» ينتهي في
            // `awaitMuxStart` بعد المهلة.
            if (videoFormat != null && audioFormat != null) startMuxerLocked()
        }
    }

    /** يبدأ الجامع مرّة واحدة؛ ويبدأ بالفيديو وحده إن تأخّر الصوت أو تعذّر. */
    private fun startMuxerLocked() {
        if (muxerStarted) return
        val out = muxer ?: return
        val video = videoFormat ?: return
        val audio = audioFormat
        val started = runCatching {
            videoTrack = out.addTrack(video)
            if (audio != null) audioTrack = out.addTrack(audio)
            out.start()
            true
        }.getOrDefault(false)
        if (started) {
            muxerStarted = true
            audioUnavailable = audio == null
        }
    }

    private fun writeSample(isVideo: Boolean, buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        if (!awaitMuxStart()) return
        synchronized(muxLock) {
            if (!muxerStarted) return
            val track = if (isVideo) videoTrack else audioTrack
            val out = muxer
            if (out == null || track < 0) return
            runCatching {
                buffer.position(info.offset)
                buffer.limit(info.offset + info.size)
                out.writeSampleData(track, buffer, info)
                wroteSamples = true
            }
        }
    }

    /**
     * لا `wait`/`notify` هنا: `Any()` في كوتلن لا يحملهما. العيّنة محفوظة في ذاكرة المرمّز ما دام
     * مقبضها لم يُحرَّر، فيصحّ الانتظار القصير بين المحاولات، وبعد المهلة يبدأ الجامع بالفيديو وحده.
     */
    private fun awaitMuxStart(): Boolean {
        val deadline = System.currentTimeMillis() + MUX_WAIT_MS
        while (true) {
            synchronized(muxLock) {
                if (muxerStarted) return true
                val now = System.currentTimeMillis()
                // المسارَان معًا = الجودة المطلوبة. والصوت وحده قد يتأخّر أو يتعذّر، فله مسارات ثلاث:
                // أُعلن تنسيقه · أُعلن تعذّره · مضت المهلة ولم يصل (فلا يُسقط الصمت التسجيل كلّه).
                if (videoFormat != null && (audioFormat != null || audioUnavailable)) {
                    startMuxerLocked()
                    return muxerStarted
                }
                if (videoFormat != null && now >= deadline) {
                    audioUnavailable = true
                    startMuxerLocked()
                    return muxerStarted
                }
                if (videoFormat == null && now >= deadline) return false
            }
            runCatching { Thread.sleep(5) }
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
        audioUnavailable = true
        // **الترتيب مقصود:** `running` تبقى صحيحة حتى تُستنزف آخر العيّنات. لو أُسقطت الآن لخرجت
        // حلقتا الاستنزاف فورًا وضاع آخر ما التقطته الشاشة (نحو ثانية كاملة بلا صورة في نهاية الملف).
        runCatching { virtualDisplay?.release() }
        virtualDisplay = null
        runCatching { videoCodec?.signalEndOfInputStream() }
        runCatching { audioRecord?.stop() }
        runCatching { videoThread?.join(700) }
        runCatching { audioThread?.join(700) }
        videoThread = null
        audioThread = null
        running = false

        runCatching { videoCodec?.stop() }
        runCatching { videoCodec?.release() }
        videoCodec = null
        runCatching { audioCodec?.stop() }
        runCatching { audioCodec?.release() }
        audioCodec = null
        runCatching { audioRecord?.release() }
        audioRecord = null
        runCatching { inputSurface?.release() }
        inputSurface = null

        synchronized(muxLock) {
            val out = muxer
            val usable = muxerStarted && wroteSamples
            if (out != null) {
                if (usable) runCatching { out.stop() }
                runCatching { out.release() }
            }
            muxer = null
            muxerStarted = false
            // وتُصفَّر أرقام المسارات وتنسيقاتها معه: الخدمة تبقى حيّةً لحظةً بعد `stopSelf`، فإن بدأت
            // جلسة ثانية قبل `onDestroy` لمنع رقمُ مسارٍ قديم (≥ 0) `registerTrack` من تسجيل تنسيق
            // الجلسة الجديدة، فلا يبدأ الجامع إطلاقًا ويُسجَّل فراغ. التصفير يقطع هذا الاحتمال.
            videoFormat = null
            audioFormat = null
            videoTrack = -1
            audioTrack = -1
        }

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
        val wrote = wroteSamples
        target = null
        wroteSamples = false
        runCatching { descriptor?.close() }
        descriptor = null
        if (uri == null) return
        if (!wrote) {
            // لا عيّنات ⇒ لا ملفّ: تُحذف البطاقة العالقة بدل أن يظهر مقطعٌ فارغ في المعرض.
            runCatching { contentResolver.delete(uri, null, null) }
            return
        }
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
        private const val CHANNELS = 2
        private const val AUDIO_BIT_RATE = 192_000
        private const val BYTES_PER_FRAME = 4
        private const val PCM_BYTES = 8 * 1024
        private const val I_FRAME_SECONDS = 1
        private const val TIMEOUT_US = 10_000L
        private const val MUX_WAIT_MS = 1_500L
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
