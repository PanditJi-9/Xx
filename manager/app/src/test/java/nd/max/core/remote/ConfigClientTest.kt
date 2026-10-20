/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.core.remote

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * النقل نفسه — **بخادم HTTP حقيقيّ على ١٢٧.٠.٠.١ لا بتبديل صنفّ**.
 *
 * ولماذا خادم حقيقيّ: أكثر ما يمكن أن يُخطئ في هذا الملفّ لا يصنعه خادمٌ مُبدَّل صنفًا — `304`
 * بلا جسم، وترويسة `Content-Length` تكذب، وردٌّ أكبر من السقف، وانقطاع الاتصال. وهذه كلها
 * أسطرٌ من `com.sun.net.httpserver` على المضيف، وتقيس **العميل كما يُشحن**.
 *
 * والحدّ المُعلن: هذا الخادم ليس Supabase. ما يُثبت هنا: العقد البروتوكولي وقراءة الحالات —
 * لا RLS ولا سياسات المشروع الحقيقي (وهي تُنفَّذ في SQL عند المالك، §١٣).
 */
class ConfigClientTest {

    private lateinit var server: HttpServer
    private var handler: ((HttpExchange) -> Unit) = {}

    @Volatile
    private var lastRequest: String? = null

    @Before
    fun start() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            lastRequest = buildString {
                append(exchange.requestMethod).append(' ').append(exchange.requestURI)
                exchange.requestHeaders["If-None-Match"]?.let { append(" if-none-match=").append(it.joinToString()) }
                exchange.requestHeaders["apikey"]?.let { append(" apikey=").append(it.joinToString()) }
            }
            handler(exchange)
        }
        server.start()
    }

    @After
    fun stop() {
        server.stop(0)
    }

    private fun baseUrl() = "http://127.0.0.1:${server.address.port}"

    private fun client() = ConfigClient(baseUrl(), apiKey = "public-anon-key", connectTimeoutMs = 2_000, readTimeoutMs = 2_000)

    private fun reply(exchange: HttpExchange, code: Int, body: String, etag: String? = null) {
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        etag?.let { exchange.responseHeaders.add("ETag", it) }
        exchange.sendResponseHeaders(code, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }

    private fun row(envelope: SignedEnvelope): String {
        val json = """{"schema":${envelope.schema},"revision":${envelope.revision},"payload":"${envelope.payload}","signature":"${envelope.signature}","updated_at":"2026-01-20T10:00:00Z"}"""
        return "[$json]"
    }

    private fun sample(): SignedEnvelope =
        TestConfigs.envelope(TestConfigs.payloadJson(revision = 12), TestKeys.generate())

    @Test
    fun `a published row arrives with its etag and reads as a row of one`() {
        val envelope = sample()
        handler = { reply(it, 200, row(envelope), "\"rev-12\"") }
        val outcome = client().fetch()
        assertTrue("يجب أن يُقرأ تغيير", outcome is FetchOutcome.Changed)
        val changed = outcome as FetchOutcome.Changed
        assertEquals("المفتاح العامّ يُمرَّر في الترويسة", true, lastRequest!!.contains("apikey=public-anon-key"))
        assertEquals("الإصدار كما وصل", 12L, changed.envelope.revision)
        assertEquals("وETag محفوظ", "\"rev-12\"", changed.etag)
        assertEquals(
            "والطلب قراءة لا كتابة",
            true,
            lastRequest!!.startsWith("GET "),
        )
    }

    @Test
    fun `a 304 is not a change and carries no body to read`() {
        handler = { reply(it, 304, "") }
        val outcome = client().fetch(etag = "\"rev-12\"")
        assertEquals("‏304 = لا تغيّر ⇒ صفر جسم", FetchOutcome.Unchanged, outcome)
        assertTrue(
            "و`If-None-Match` أُرسل فعلًا",
            lastRequest!!.contains("if-none-match=[\"rev-12\"]") || lastRequest!!.contains("if-none-match=\"rev-12\""),
        )
    }

    @Test
    fun `a rejected key is named as such, not as a network problem`() {
        handler = { reply(it, 401, """{"message":"Invalid API key"}""") }
        val fault = runCatching { client().fetch() }.exceptionOrNull() as RemoteFetchException
        assertEquals("‏401 رمزها", RemoteFault.UNAUTHORIZED, fault.fault)
    }

    @Test
    fun `a policy refusal is named separately from a server outage`() {
        handler = { reply(it, 403, "forbidden") }
        assertEquals(
            "‏403 ليست 401",
            RemoteFault.FORBIDDEN,
            (runCatching { client().fetch() }.exceptionOrNull() as RemoteFetchException).fault,
        )
        handler = { reply(it, 503, "paused") }
        assertEquals(
            "‏5xx تسمّى باسمها (وقفة السكون مثلًا)",
            RemoteFault.SERVER,
            (runCatching { client().fetch() }.exceptionOrNull() as RemoteFetchException).fault,
        )
        handler = { reply(it, 404, "not found") }
        assertEquals(
            "‏404: العرض غير منشور — جراءة تشخيص مختلفة",
            RemoteFault.NOT_FOUND,
            (runCatching { client().fetch() }.exceptionOrNull() as RemoteFetchException).fault,
        )
    }

    @Test
    fun `a body over the cap is refused instead of being read`() {
        val huge = "x".repeat(ConfigClient.MAX_BODY_BYTES + 1024)
        handler = { reply(it, 200, huge) }
        assertEquals(
            "جسم أكبر من العقد لا يُحمَّل",
            RemoteFault.TOO_LARGE,
            (runCatching { client().fetch() }.exceptionOrNull() as RemoteFetchException).fault,
        )
    }

    @Test
    fun `a body that is not our row is refused as a format problem`() {
        handler = { reply(it, 200, """{"not":"an array"}""") }
        assertEquals(
            "شكل غير متوقّع ⇒ MALFORMED",
            RemoteFault.FORMAT,
            (runCatching { client().fetch() }.exceptionOrNull() as RemoteFetchException).fault,
        )

        handler = { reply(it, 200, "[]") }
        assertEquals(
            "صفٌّ مفقود ⇒ MALFORMED (الإعداد لم يُنشر بعد)",
            RemoteFault.FORMAT,
            (runCatching { client().fetch() }.exceptionOrNull() as RemoteFetchException).fault,
        )

        val empty = sample().copy(signature = "")
        handler = { reply(it, 200, row(empty)) }
        assertEquals(
            "غلاف بلا توقيع لا يُمرَّر إلى الحكم",
            RemoteFault.FORMAT,
            (runCatching { client().fetch() }.exceptionOrNull() as RemoteFetchException).fault,
        )
    }

    @Test
    fun `a server that is not there is a network fault`() {
        server.stop(0)
        val fault = runCatching { client().fetch() }.exceptionOrNull() as RemoteFetchException
        assertEquals("لا خادم ⇒ NETWORK", RemoteFault.NETWORK, fault.fault)
        // (ولا يُعاد التشغيل هنا: `@After` يتعامل مع خادمٍ مُغلق أصلًا بلا استثناء.)
    }

    @Test
    fun `an unset base url is a build gap, named as one`() {
        val client = ConfigClient("", "key")
        assertEquals(
            "بلا رابط: NOT_CONFIGURED لا «شبكة»",
            RemoteFault.NOT_CONFIGURED,
            (runCatching { client.fetch() }.exceptionOrNull() as RemoteFetchException).fault,
        )
    }

    @Test
    fun `the payload bytes that were signed are the bytes that arrive`() {
        val envelope = sample()
        handler = { reply(it, 200, row(envelope)) }
        val changed = client().fetch() as FetchOutcome.Changed
        val decoded = Base64.getDecoder().decode(changed.envelope.payload)
        assertEquals(
            "‏base64 ذهابًا وإيابًا بلا تغيير بايت",
            String(decoded, Charsets.UTF_8),
            String(TestConfigs.bytes(envelope), Charsets.UTF_8),
        )
    }
}
