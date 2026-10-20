/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.core.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * الحالات المضادّة ٥ · ٦ · ١٢ من §٥.٤ — على ملفّ حقيقيّ في مجلّد مؤقّت.
 *
 * والدعوى الجوهرية هنا: **الذاكرة المحلية ليست ثقة**. ملفّ يُقرأ منها يُعاد التحقّق من توقيعه في
 * كل إقلاع، والتالف يُعامل غيابًا لا «إصلاحًا جزئيًّا»، ورجوع الساعة يُعلن «غير قابل للقياس» لا
 * «طازج».
 */
class ConfigStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val server = TestKeys.generate()

    private fun file(): File = File(folder.root, ConfigStore.FILE_NAME)

    private fun stored(revision: Long = 3, at: Long = TestConfigs.NOW): StoredConfig {
        val payload = TestConfigs.payloadJson(revision = revision)
        return StoredConfig(
            envelope = TestConfigs.envelope(payload, server),
            storedAtMillis = at,
            lastAttemptMillis = at,
            lastOutcome = "accepted",
        )
    }

    @Test
    fun `what is written is read back unchanged`() {
        val store = ConfigStore(file())
        assertTrue("الكتابة يجب أن تنجح", store.write(stored(revision = 3)))
        val read = store.read()
        assertNotNull("ما كُتب يُقرأ", read)
        assertEquals("الإصدار", 3L, read!!.envelope.revision)
        assertEquals("وقت الحفظ", TestConfigs.NOW, read.storedAtMillis)
        assertEquals(
            "بايتات الحمولة بالحرف — لا إعادة تسلسل بين الكتابة والقراءة",
            stored().envelope.payload,
            read.envelope.payload,
        )
    }

    @Test
    fun `a corrupt file is treated as missing, not as a half config`() {
        val store = ConfigStore(file())
        store.write(stored())
        file().writeText("{\"format\":1,\"revision\":", Charsets.UTF_8) // مقطوع في المنتصف
        assertNull("ملفّ مقطوع ⇒ كأنه غير موجود", store.read())

        file().writeText("not json at all", Charsets.UTF_8)
        assertNull("نصّ لا يُقرأ ⇒ غياب", store.read())

        file().writeText("""{"format":99,"payload":"x","signature":"y"}""", Charsets.UTF_8)
        assertNull("صيغة ملفّ أحدث ⇒ غياب لا تخمين", store.read())
    }

    @Test
    fun `an empty payload in the file is still absent`() {
        val store = ConfigStore(file())
        file().writeText(
            """{"format":1,"schema":1,"revision":4,"payload":"","signature":""}""",
            Charsets.UTF_8,
        )
        assertNull("غلاف بلا حمولة ولا توقيع ليس إعدادًا", store.read())
    }

    @Test
    fun `writing never leaves a half file behind`() {
        val store = ConfigStore(file())
        store.write(stored(revision = 1))
        store.write(stored(revision = 2))
        assertEquals("الكتابة الثانية تستبدل الأولى", 2L, store.read()!!.envelope.revision)
        val leftovers = folder.root.listFiles()!!.filter { it.name.endsWith(".tmp") }
        assertTrue("لا ملفّ مؤقّت يبقى بعد النجاح: $leftovers", leftovers.isEmpty())
    }

    @Test
    fun `a failed attempt does not erase the last good config`() {
        val store = ConfigStore(file())
        store.write(stored(revision = 5))
        assertTrue(store.noteAttempt(TestConfigs.NOW + 1_000L, RemoteFault.NETWORK.code))
        val read = store.read()!!
        assertEquals("الغلاف لم يُمسّ", 5L, read.envelope.revision)
        assertEquals("وقت المحاولة سُجّل", TestConfigs.NOW + 1_000L, read.lastAttemptMillis)
        assertEquals("ورمزها سُجّل", RemoteFault.NETWORK.code, read.lastOutcome)
    }

    @Test
    fun `an attempt on an empty memory writes nothing`() {
        val store = ConfigStore(file())
        assertFalse("لا محاولة تُسجَّل في ذاكرة فارغة", store.noteAttempt(TestConfigs.NOW, "x"))
        assertNull(store.read())
    }

    @Test
    fun `a clock that went backwards is unmeasurable, never fresh`() {
        val stored = stored(at = TestConfigs.NOW)
        assertNull(
            "ساعة رجعت ⇒ لا عمر يُقاس",
            stored.ageMillis(TestConfigs.NOW - 60_000L),
        )
        assertEquals(
            "وتُعلن غير قابلة للقياس لا طازجة",
            ConfigFreshness.UNMEASURABLE,
            stored.freshness(TestConfigs.NOW - 60_000L, ttlHours = 12),
        )
    }

    @Test
    fun `age decides fresh from stale inside the ttl`() {
        val stored = stored(at = TestConfigs.NOW)
        assertEquals(
            "داخل الـTTL",
            ConfigFreshness.FRESH,
            stored.freshness(TestConfigs.NOW + 3_600_000L, ttlHours = 12),
        )
        assertEquals(
            "خارج الـTTL",
            ConfigFreshness.STALE,
            stored.freshness(TestConfigs.NOW + 13 * 3_600_000L, ttlHours = 12),
        )
    }

    @Test
    fun `clearing the app data loses the memory and nothing else`() {
        val store = ConfigStore(file())
        store.write(stored())
        assertTrue("الحذف ينجح", store.clear())
        assertNull("مسح البيانات = لا ذاكرة", store.read())
        assertTrue("وحذف ذاكرةٍ غائبة نجاحٌ لا عطب", store.clear())
    }

    @Test
    fun `the stored envelope goes through the same admission again`() {
        val store = ConfigStore(file())
        store.write(stored(revision = 8))
        val read = store.read()!!
        val admitted = RemoteConfigs.admit(read.envelope, server.publicBase64, 0L, TestConfigs.NOW)
        assertTrue(
            "ما في الذاكرة يُقبل بالعقد نفسه — فالفحص واحد في المسارين",
            admitted is Admission.Admitted,
        )
    }

    @Test
    fun `a memory signed by someone else is refused on the way out`() {
        val store = ConfigStore(file())
        val foreign = TestConfigs.envelope(TestConfigs.payloadJson(revision = 8), TestKeys.generate())
        store.write(stored().copy(envelope = foreign))
        val read = store.read()!!
        assertEquals(
            "ذاكرة معدَّلة بمفتاح آخر تُرفض عند الإقلاع لا عند الكتابة",
            RemoteRejection.SIGNATURE,
            (
                RemoteConfigs.admit(read.envelope, server.publicBase64, 0L, TestConfigs.NOW)
                    as Admission.Rejected
                ).reason,
        )
    }
}
