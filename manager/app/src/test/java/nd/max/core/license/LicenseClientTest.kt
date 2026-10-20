/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.core.license

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import nd.max.core.remote.TestKeys
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets

/**
 * النقل: طلبٌ واحد `POST` إلى دالّة واحدة — **بخادم HTTP حقيقيّ على ١٢٧.٠.٠.١ لا بتبديل صنفّ**.
 *
 * وأهمّ ما يُقاس هنا سطران لا يُرى أثرهما إلّا في حالة نادرة: ① أن **البصمة** هي التي تُرسل لا
 * المفتاح، ② وأن **زمن الخادم يُقرأ من ردّ الرفض أيضًا** — وهو ما يقوم عليه مانع رجوع الساعة.
 */
class LicenseClientTest {

    private lateinit var server: HttpServer
    private var handler: ((HttpExchange) -> Unit) = {}

    @Volatile
    private var lastRequest: String? = null

    @Volatile
    private var lastBody: String? = null

    @Before
    fun start() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            lastRequest = "${exchange.requestMethod} ${exchange.requestURI}"
            lastBody = exchange.requestBody.readBytes().toString(StandardCharsets.UTF_8)
            handler(exchange)
        }
        server.start()
    }

    @After
    fun stop() {
        server.stop(0)
    }

    private fun client() = LicenseClient(
        baseUrl = "http://127.0.0.1:${server.address.port}",
        apiKey = "public-anon-key",
        connectTimeoutMs = 2_000,
        readTimeoutMs = 2_000,
    )

    private fun reply(exchange: HttpExchange, code: Int, body: String) {
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        exchange.sendResponseHeaders(code, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }

    private val serverKey = TestKeys.generate()

    private fun envelopeJson(): String {
        val envelope = LicenseTestSupport.envelope(LicenseTestSupport.payload(), serverKey)
        return """{"schema":1,"license":"${envelope.license}","payload":"${envelope.payload}","signature":"${envelope.signature}"}"""
    }

    @Test
    fun `a granted certificate arrives with the server clock`() {
        val iso = LicenseTestSupport.iso(LicenseTestSupport.NOW)
        handler = { reply(it, 200, """{"ok":true,"cert":${envelopeJson()},"server_time":"$iso"}""") }
        val outcome = client().redeem("a".repeat(64), LicenseTestSupport.DEVICE, "Pixel", "210")
        val granted = outcome as LicenseCallOutcome.Granted
        assertEquals(LicenseTestSupport.NOW, granted.serverMillis)
        assertTrue("الغلاف كما وصل", granted.envelope.payload.isNotEmpty())
        assertTrue("الطلب كتابةٌ إلى الدالّة", lastRequest!!.startsWith("POST /functions/v1/maxctl"))
    }

    @Test
    fun `the wire carries the fingerprint, never the raw key`() {
        handler = { reply(it, 200, """{"ok":false,"error":"not_found","server_time":"${LicenseTestSupport.iso(LicenseTestSupport.NOW)}"}""") }
        client().redeem("56a4e107d113d2874ba23e27d2f6a2c775bbf22035378b6458ce9e4873db5247", LicenseTestSupport.DEVICE, "", "210")
        assertTrue("البصمة هي المُرسلة — والجسم: $lastBody", lastBody!!.contains("56a4e107d113d2874ba23e27d2f6a2c775bbf22035378b6458ce9e4873db5247"))
        assertFalse("ولا مفتاح خامّ في الجسم", lastBody!!.contains("MAXM-"))
    }

    @Test
    fun `a denial carries the server clock too`() {
        val iso = LicenseTestSupport.iso(LicenseTestSupport.NOW + 5000)
        handler = { reply(it, 200, """{"ok":false,"error":"expired","server_time":"$iso"}""") }
        val outcome = client().validate(LicenseTestSupport.LICENSE_ID, LicenseTestSupport.DEVICE, "210")
        val denied = outcome as LicenseCallOutcome.Denied
        assertEquals("expired", denied.code)
        assertEquals(
            "زمن الرفض هو مرساة منع رجوع الساعة",
            LicenseTestSupport.NOW + 5000,
            denied.serverMillis,
        )
    }

    @Test
    fun `a denial without a clock reads as zero, not as now`() {
        handler = { reply(it, 200, """{"ok":false,"error":"device_unknown"}""") }
        val denied = client().validate(LicenseTestSupport.LICENSE_ID, LicenseTestSupport.DEVICE, "210")
            as LicenseCallOutcome.Denied
        assertEquals(0L, denied.serverMillis)
    }

    @Test
    fun `an ok response without a certificate is a format fault, not an empty grant`() {
        handler = { reply(it, 200, """{"ok":true,"server_time":"${LicenseTestSupport.iso(LicenseTestSupport.NOW)}"}""") }
        val fault = runCatching { client().redeem("a".repeat(64), LicenseTestSupport.DEVICE, "", "210") }
            .exceptionOrNull() as LicenseFetchException
        assertEquals(LicenseFault.FORMAT, fault.fault)
    }

    @Test
    fun `http failures are classified by who failed`() {
        handler = { reply(it, 500, "boom") }
        assertEquals(
            LicenseFault.SERVER,
            (runCatching { client().validate(LicenseTestSupport.LICENSE_ID, LicenseTestSupport.DEVICE, "210") }
                .exceptionOrNull() as LicenseFetchException).fault,
        )
        handler = { reply(it, 404, "no function") }
        assertEquals(
            "‏404 تعني غالبًا `functions deploy` ناقصًا — وتُقرأ عطبًا من العميل لا من الخادم",
            LicenseFault.CLIENT,
            (runCatching { client().validate(LicenseTestSupport.LICENSE_ID, LicenseTestSupport.DEVICE, "210") }
                .exceptionOrNull() as LicenseFetchException).fault,
        )
    }

    @Test
    fun `a broken body is a format fault`() {
        handler = { reply(it, 200, "not json at all") }
        assertEquals(
            LicenseFault.FORMAT,
            (runCatching { client().validate(LicenseTestSupport.LICENSE_ID, LicenseTestSupport.DEVICE, "210") }
                .exceptionOrNull() as LicenseFetchException).fault,
        )
    }

    @Test
    fun `a build without a server url says so before opening a socket`() {
        val fault = runCatching { LicenseClient("", "").validate("id", "d", "1") }
            .exceptionOrNull() as LicenseFetchException
        assertEquals(LicenseFault.NOT_CONFIGURED, fault.fault)
    }

    @Test
    fun `a real server-signed certificate survives the whole wire trip`() {
        val iso = LicenseTestSupport.iso(LicenseTestSupport.NOW)
        handler = { reply(it, 200, """{"ok":true,"cert":${envelopeJson()},"server_time":"$iso"}""") }
        val granted = client().validate(LicenseTestSupport.LICENSE_ID, LicenseTestSupport.DEVICE, "210")
            as LicenseCallOutcome.Granted
        val admitted = LicenseCertificates.admit(
            granted.envelope,
            serverKey.publicBase64,
            LicenseTestSupport.DEVICE,
            LicenseTestSupport.NOW,
        )
        assertTrue("ما وصل عبر السلك يُقبل بالتحقّق الكامل", admitted is LicenseAdmission.Accepted)
    }
}
