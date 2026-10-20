package nd.max.core.update

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.io.File
import java.net.InetSocketAddress
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/**
 * طبقة النقل مقابل **خادمٍ محليّ حقيقي** (لا شبكة خارجية، ولا محاكي).
 *
 * ولماذا لا تكفي المراجعة بالقراءة هنا: العطب الذي كُتب هذا الفصل لمنعه — «ملفٌ نصفه
 * منزَّل يُثبَّت»، «بصمة لا تطابق ومع ذلك يُعرض زرّ التثبيت» — لا يظهر إلا عند انقطاع
 * حقيقي في منتصف البثّ. والخادم المحلي يصنع الانقطاع بالضبط، فتُقاس الحماية عليه.
 */
class UpdateTransportTest {

    private lateinit var server: HttpServer
    private lateinit var dir: File
    private val transport = UpdateTransport(connectTimeoutMs = 2_000, readTimeoutMs = 5_000)

    @Before
    fun setUp() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        dir = File(System.getProperty("java.io.tmpdir"), "maxmanager-update-test-${System.nanoTime()}")
        assertTrue(dir.mkdirs())
    }

    @After
    fun tearDown() {
        server.stop(0)
        dir.deleteRecursively()
    }

    private fun url(path: String) = "http://127.0.0.1:${server.address.port}$path"

    private fun context(path: String, body: ByteArray, code: Int = 200) {
        server.createContext(path) { exchange: HttpExchange ->
            exchange.sendResponseHeaders(code, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
    }

    @Test
    fun `a 200 returns the body and a 404 is an http fault, not a network one`() {
        context("/manifest", """{"schema":1}""".toByteArray())
        context("/missing", "nope".toByteArray(), code = 404)
        server.start()

        assertEquals("""{"schema":1}""", transport.fetchManifest(url("/manifest")))
        try {
            transport.fetchManifest(url("/missing"))
            fail("expected UpdateFault")
        } catch (fault: UpdateFault) {
            assertEquals(UpdateError.HTTP, fault.error)
        }
    }

    @Test
    fun `a manifest larger than the cap is refused without being read whole`() {
        context("/big", "12345".toByteArray())
        server.start()
        try {
            transport.fetchManifest(url("/big"), maxBytes = 4)
            fail("expected UpdateFault")
        } catch (fault: UpdateFault) {
            assertEquals(UpdateError.TOO_LARGE, fault.error)
        }
    }

    @Test
    fun `a matching checksum keeps the file and reports progress against the declared length`() {
        val body = "apk-bytes-here".toByteArray()
        context("/apk", body)
        server.start()

        val target = File(dir, "update.apk")
        val expected = UpdateTransport.sha256Hex(File(dir, "src.bin").apply { writeBytes(body) })
        var lastTotal = -1L
        var lastDone = -1L
        val result = transport.download(url("/apk"), target, expected) { done, total ->
            lastDone = done
            lastTotal = total
        }

        assertEquals(target, result)
        assertTrue(target.exists())
        assertEquals(body.size.toLong(), lastDone)
        assertEquals(body.size.toLong(), lastTotal)
    }

    @Test
    fun `a checksum mismatch deletes the file so nothing installable survives`() {
        context("/apk", "tampered".toByteArray())
        server.start()

        val target = File(dir, "update.apk")
        try {
            transport.download(url("/apk"), target, "00".repeat(32))
            fail("expected UpdateFault")
        } catch (fault: UpdateFault) {
            assertEquals(UpdateError.INTEGRITY, fault.error)
        }
        assertFalse("الشيء الوحيد الذي يجوز أن يبقى من ملفٍّ مغشوش هو لا شيء", target.exists())
    }

    @Test
    fun `a truncated download leaves no partial file behind`() {
        val declared = 4096
        server.createContext("/torn") { exchange ->
            exchange.sendResponseHeaders(200, declared.toLong())
            exchange.responseBody.use { it.write(ByteArray(16)) } // يعلن 4096 ويكتب 16 ثم يقطع
        }
        server.start()

        val target = File(dir, "update.apk")
        try {
            transport.download(url("/torn"), target, "")
            fail("expected UpdateFault")
        } catch (fault: UpdateFault) {
            assertEquals(UpdateError.NETWORK, fault.error)
        }
        assertFalse(target.exists())
    }

    @Test
    fun `sha256 matches the published vector`() {
        val file = File(dir, "abc.bin").apply { writeBytes("abc".toByteArray()) }
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            UpdateTransport.sha256Hex(file),
        )
    }
}
