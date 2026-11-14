/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.core.gamespace

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.provider.MediaStore
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** ما انتهى إليه القصّ — والنجاح يحمل عنوان الملفّ الجديد لا وعدًا به. */
sealed interface ClipTrimResult {
    data class Saved(val uri: Uri, val name: String, val durationMs: Long) : ClipTrimResult
    data class Failed(val reason: ClipTrimFailure) : ClipTrimResult
}

/** أين وقف القصّ بالضبط — الرسالة تُشتقّ منه لا من «فشل» مبهم. */
enum class ClipTrimFailure { Unreadable, Unwritable, Muxing }

/**
 * قصّ مقطع إلى **نسخة جديدة** عبر `MediaExtractor`/`MediaMuxer` — بلا مكتبة خارجية،
 * وبلا مساس بالأصل: الحذف قرار المستخدم من صفّه، لا أثر جانبي للقصّ.
 *
 * ### والاختيار المقصود
 *
 * الناتج يُكتب في `MediaStore` بصفّ **معلّق** (`IS_PENDING`) داخل `Movies/MaxManager`،
 * ثم يُعلَن عند اكتماله: الصفّ المعلّق لا يظهر نصفَ ملفّ في المعرض، والإدراج في مجموعة
 * الفيديو يملكه التطبيق فلا يحتاج إذن كتابة إضافيًّا (minSdk 29).
 *
 * ### وحدّ لا يُخفى
 *
 * دقّة القصّ تعتمد على «الإطار المفتاحي السابق» لنقطة البداية: الفيديو يبدأ من أقرب إطار
 * قابل للفكّ لا من المللي ثانية حرفيًّا. وهذا سلوك `MediaExtractor` نفسه، **ولم يُقَس على
 * جهاز** في هذه الدفعة؛ ما قيس هنا هو الحساب النقيّ ([ClipTrimMath]) فقط.
 */
object ClipTrimmer {

    private const val RELATIVE_PATH = "Movies/MaxManager"
    private const val MIME_TYPE = "video/mp4"
    private const val FALLBACK_BUFFER_BYTES = 1 shl 20

    /**
     * يقصّ ويحفظ نسخة. **دالّة حاجبة** تُنادى من `Dispatchers.IO` لا من الخيط الرئيسي.
     *
     * @param source عنوان المقطع الأصلي (قراءة فقط).
     * @param startMs بداية النسخة — تُقصّ مسبقًا عبر [ClipTrimMath].
     * @param endMs نهايتها.
     */
    fun trim(context: Context, source: Uri, startMs: Long, endMs: Long): ClipTrimResult {
        val resolver = context.contentResolver
        val startUs = startMs.coerceAtLeast(0L) * 1_000L
        val endUs = endMs.coerceAtLeast(startMs) * 1_000L
        val extractor = MediaExtractor()
        var pending: Uri? = null
        var stage = ClipTrimFailure.Unreadable
        return try {
            extractor.setDataSource(context, source, null)
            val tracks = (0 until extractor.trackCount).mapNotNull { index ->
                val format = extractor.getTrackFormat(index)
                val mime = runCatching { format.getString(MediaFormat.KEY_MIME) }.getOrNull().orEmpty()
                if (mime.startsWith("video/") || mime.startsWith("audio/")) index to format else null
            }
            // مقطع بلا مسار صوت/صورة ليس مقطعًا: يُقال ذلك صراحةً بدل ملفّ فارغ يُحفظ.
            if (tracks.isEmpty()) return ClipTrimResult.Failed(ClipTrimFailure.Unreadable)

            stage = ClipTrimFailure.Unwritable
            val name = outputName()
            val target = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, pendingValues(name))
                ?: return ClipTrimResult.Failed(ClipTrimFailure.Unwritable)
            pending = target
            val descriptor = resolver.openFileDescriptor(target, "rw")
                ?: throw IllegalStateException("trim output could not be opened")

            stage = ClipTrimFailure.Muxing
            // **الإغلاق قبل `stop()` خطأ:** `stop` يكتب فهرس الملفّ عبر الواصف، فإغلاق
            // `use` قبله كان يترك المقطع بلا ذيل صالح. ولذلك يبدأ المازج ويُغلق داخل النطاق نفسه.
            descriptor.use { file ->
                val built = MediaMuxer(file.fileDescriptor, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                try {
                    val muxTracks = tracks.associate { (index, format) -> index to built.addTrack(format) }
                    built.start()
                    tracks.forEach { (index, format) ->
                        copyTrack(extractor, built, index, muxTracks.getValue(index), format, startUs, endUs)
                    }
                    built.stop()
                } finally {
                    runCatching { built.release() }
                }
            }

            resolver.update(
                target,
                ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) },
                null,
                null
            )
            val size = readSize(resolver, target)
            if (size <= 0L) {
                runCatching { resolver.delete(target, null, null) }
                pending = null
                ClipTrimResult.Failed(ClipTrimFailure.Muxing)
            } else {
                pending = null
                ClipTrimResult.Saved(target, name, ClipTrimMath.keptMs(startMs, endMs))
            }
        } catch (_: Exception) {
            pending?.let { runCatching { resolver.delete(it, null, null) } }
            ClipTrimResult.Failed(stage)
        } finally {
            runCatching { extractor.release() }
        }
    }

    /**
     * نسخ مسار واحد بين البداية والنهاية.
     *
     * وإزاحة الزمن لكل مسار على حدة (`baseUs` = أول عيّنة تُقرأ) حتى يبدأ الصوت والصورة من
     * الصفر معًا، ولا يظهر أحدهما متقدّمًا على الآخر بسبب اختلاف موضع الإطار المفتاحي.
     */
    private fun copyTrack(
        extractor: MediaExtractor,
        muxer: MediaMuxer,
        sourceTrack: Int,
        muxTrack: Int,
        format: MediaFormat,
        startUs: Long,
        endUs: Long
    ) {
        extractor.selectTrack(sourceTrack)
        extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        val maxInput = runCatching { format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE) }.getOrDefault(0)
        val buffer = ByteBuffer.allocateDirect(if (maxInput > 0) maxInput else FALLBACK_BUFFER_BYTES)
        val info = MediaCodec.BufferInfo()
        var baseUs = -1L
        while (true) {
            val sampleTime = extractor.sampleTime
            if (sampleTime < 0L || sampleTime > endUs) break
            val size = extractor.readSampleData(buffer, 0)
            if (size < 0) break
            if (baseUs < 0L) baseUs = sampleTime
            info.set(0, size, (sampleTime - baseUs).coerceAtLeast(0L), extractor.sampleFlags)
            muxer.writeSampleData(muxTrack, buffer, info)
            if (!extractor.advance()) break
        }
    }

    private fun pendingValues(name: String): ContentValues = ContentValues().apply {
        put(MediaStore.Video.Media.DISPLAY_NAME, name)
        put(MediaStore.Video.Media.MIME_TYPE, MIME_TYPE)
        put(MediaStore.Video.Media.RELATIVE_PATH, RELATIVE_PATH)
        put(MediaStore.Video.Media.IS_PENDING, 1)
    }

    private fun outputName(): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "Max_${stamp}_trim.mp4"
    }

    private fun readSize(resolver: ContentResolver, uri: Uri): Long = runCatching {
        resolver.query(uri, arrayOf(MediaStore.Video.Media.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else 0L
        } ?: 0L
    }.getOrDefault(0L)
}
