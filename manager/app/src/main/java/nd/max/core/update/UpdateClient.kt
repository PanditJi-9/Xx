/*
 * Copyright (C) 2026-2027 Zexshia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nd.max.core.update

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.security.MessageDigest
import java.util.Locale

/** سبب العطب كما تحتاجه الواجهة — لكلٍّ نصّه الخاص، ولا يُدمج النوعان في «فشل». */
enum class UpdateError {
    /** لم نصل إلى الخادم: انقطاع، مهلة، لا شبكة. */
    NETWORK,

    /** وصلنا وردّ ردًّا غير ‎200. */
    HTTP,

    /** وصل شيء لا يُقرأ — رابط مكسور أو ملفّ ليس مانيفست. */
    FORMAT,

    /** المانيفست تجاوز الحدّ المعقول — لا نُحمّل ملفًّا بحجم غير متوقّع. */
    TOO_LARGE,

    /** بصمة الملفّ المخزَّل لا تطابق المعلنة — يُحذف ولا يُعرض. */
    INTEGRITY,

    /** فشل التثبيت نفسه (صلاحية أو مساحة أو رفض النظام). */
    INSTALL,

    /** لا رابط تحديث مضبوط في هذه النسخة من البناء. */
    NOT_CONFIGURED,
}

/** عطبٌ مصنَّف: النوع يُترجَم إلى نصّ في الواجهة، والرسالة التقنية للسجلّ لا للمستخدم. */
class UpdateFault(
    val error: UpdateError,
    message: String,
    cause: Throwable? = null,
) : IOException(message, cause)

/**
 * كل ما يلمس الشبكة في مسار التحديث — **وفيه كل ما لا يُترك للواجهة**:
 *
 * 1. **مهلة لا انتظار أبديّ:** اتصال ١٠ ثوانٍ وقراءة ٢٠. تطبيق يُفتح ويجد معلّقًا معلّقًا
 *    على خادمٍ لا يردّ أسوأ من تطبيق لا يتحقّق.
 * 2. **حدّ حجم للـمانيفست** ([MAX_MANIFEST_BYTES]): الملفّ عقدٌ صغير؛ ولو صار رابطًا خاطئًا
 *    أو خبيثًا فتحميلُ جيجابايتاتٍ بصمتٍ عطبٌ لا ميزة.
 * 3. **بصمة SHA-256 للـAPK قبل أن يُعرض أي زرّ تثبيت** ([sha256Hex]): الرابط في المانيفست
 *    قابل للتغيير من عندنا، وHTTPS يحمي الطريق لا محتوى الملفّ. ولذلك يُتحقّق **بعد التنزيل
 *    وقبل التثبيت**، وعند الاختلاف يُحذف الملفّ فورًا — فلا يبقى على الجهاز ملفّ مغشوش يمكن
 *    أن يثبّته أحد لاحقًا من مُثبِّت النظام.
 *
 * والاختبارات على هذا الفصل تعمل على JVM بخادم محلي ([UpdateTransportTest]) — لأن أكثر ما
 * يمكن أن يُخطئ هنا (ردّ ليس 200 · ملفّ يقصّ في المنتصف · بصمة لا تطابق) لا يصنعه خادم
 * حقيقي بالطلب، ويصنعه خادم اختبارٍ بـ٣ أسطر.
 */
class UpdateTransport(
    private val connectTimeoutMs: Int = DEFAULT_CONNECT_TIMEOUT_MS,
    private val readTimeoutMs: Int = DEFAULT_READ_TIMEOUT_MS,
    private val userAgent: String = USER_AGENT,
) {

    fun fetchManifest(url: String, maxBytes: Int = MAX_MANIFEST_BYTES): String {
        val connection = open(url)
        try {
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) throw UpdateFault(UpdateError.HTTP, "HTTP $code")
            val declared = connection.contentLengthLong
            if (declared > maxBytes) throw UpdateFault(UpdateError.TOO_LARGE, "declared $declared bytes")
            val stream = connection.inputStream
            val sink = ByteArrayOutputStream(INITIAL_BUFFER_BYTES)
            val buffer = ByteArray(READ_BUFFER_BYTES)
            while (true) {
                val read = stream.read(buffer)
                if (read <= 0) break
                sink.write(buffer, 0, read)
                if (sink.size() > maxBytes) throw UpdateFault(UpdateError.TOO_LARGE, "over $maxBytes bytes")
            }
            return sink.toString(Charsets.UTF_8.name())
        } catch (fault: UpdateFault) {
            throw fault
        } catch (error: IOException) {
            throw UpdateFault(UpdateError.NETWORK, "read failed: ${error.message}", error)
        } finally {
            runCatching { connection.disconnect() }
        }
    }

    /**
     * ينزّل إلى [target] ويتحقّق من [expectedSha256] (إن كانت غير فارغة).
     *
     * **والملفّ الجزئيّ يُحذف في كل طريق فشل** — لا يُترك ملفٌ نصفه مكتوب ليكون «تحديثًا»
     * في المحاولة القادمة. و`onProgress(bytesDone, totalBytes)`: المجموع `0` إذا لم يعلنه
     * الخادم (فالواجهة تعرض «جارٍ التنزيل» بلا نسبة بدل نسبة كاذبة).
     */
    fun download(
        url: String,
        target: File,
        expectedSha256: String,
        onProgress: (Long, Long) -> Unit = { _, _ -> },
    ): File {
        target.parentFile?.mkdirs()
        val connection = open(url)
        try {
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) throw UpdateFault(UpdateError.HTTP, "HTTP $code")
            val total = connection.contentLengthLong.coerceAtLeast(0L)
            var done = 0L
            connection.inputStream.use { input ->
                FileOutputStream(target).use { raw ->
                    val output = raw.buffered(READ_BUFFER_BYTES)
                    val buffer = ByteArray(READ_BUFFER_BYTES)
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        done += read
                        onProgress(done, total)
                    }
                    output.flush()
                }
            }
            // **والقطع وسط البثّ ليس فشلًا معلنًا دائمًا:** خادمٌ يعلن طولًا ثم يقفل قبل
            // تمامه قد يُنهي العميل القراءة بنجاح. وهذا مُقاس في `UpdateTransportTest`:
            // بلا هذا السطر كان ملفٌ من ١٦ بايت من أصل ٤٠٩٦ "ينجح"، ويصل `pm install`
            // فيرفضه بخطأ لا يقول لمستخدمٍ أن التحميل كان ناقصًا. والفحص رخيص: مجموع
            // معلن (لا صفر) ومقروء أقلّ منه ⇒ لم يكتمل. والخادم الذي لا يعلن طولًا يبقى
            // في حماية البصمة وحدها، وهذا مذكور في وثيقة المضيف.
            if (total > 0L && done != total) {
                throw UpdateFault(UpdateError.NETWORK, "incomplete: $done of $total bytes")
            }
            val expected = expectedSha256.trim().lowercase(Locale.US)
            if (expected.isNotEmpty()) {
                val actual = sha256Hex(target)
                if (actual != expected) {
                    target.delete()
                    throw UpdateFault(
                        UpdateError.INTEGRITY,
                        "sha256 mismatch: expected $expected, got $actual",
                    )
                }
            }
            return target
        } catch (fault: UpdateFault) {
            target.delete()
            throw fault
        } catch (error: IOException) {
            target.delete()
            throw UpdateFault(UpdateError.NETWORK, "download failed: ${error.message}", error)
        } finally {
            runCatching { connection.disconnect() }
        }
    }

    private fun open(url: String): HttpURLConnection {
        val parsed = try {
            URL(url)
        } catch (error: MalformedURLException) {
            throw UpdateFault(UpdateError.FORMAT, "bad url: $url", error)
        }
        return try {
            (parsed.openConnection() as HttpURLConnection).apply {
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", userAgent)
                setRequestProperty("Accept", "application/json, application/octet-stream")
            }
        } catch (error: IOException) {
            throw UpdateFault(UpdateError.NETWORK, "connect failed: ${error.message}", error)
        }
    }

    companion object {
        const val DEFAULT_CONNECT_TIMEOUT_MS = 10_000
        const val DEFAULT_READ_TIMEOUT_MS = 20_000
        const val MAX_MANIFEST_BYTES = 256 * 1024
        const val USER_AGENT = "MaxManager-Updater"

        private const val READ_BUFFER_BYTES = 64 * 1024
        private const val INITIAL_BUFFER_BYTES = 8 * 1024

        /** بصمة الملفّ — هي القيمة نفسها التي يكتبها المالك في `sha256` بالمانيفست. */
        fun sha256Hex(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(READ_BUFFER_BYTES)
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    digest.update(buffer, 0, read)
                }
            }
            val bytes = digest.digest()
            val hex = StringBuilder(bytes.size * 2)
            for (byte in bytes) hex.append(String.format(Locale.US, "%02x", byte))
            return hex.toString()
        }
    }
}
