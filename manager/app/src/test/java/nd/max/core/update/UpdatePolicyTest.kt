package nd.max.core.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * أحكام السياسة الخمسة — هذا هو الملفّ الذي يمنع صنفَي العطب الأخطر في هذه الميزة:
 *
 * 1. **حجبٌ لا يقع** (أو يقع على رقم مجهول): النسخة القديمة تبقى تعمل، أو التطبيق يُقفل
 *    على مستخدمٍ نظامه ردّ قراءة الحزمة.
 * 2. **إزعاج دائم**: نافذة تعود في كل فتحة لأن التخطّي لم يُسجَّل، أو لا تعود أبدًا لأن
 *    التخطّي سُجِّل لإصدارٍ واحد ظنًّا أنه لكل الإصدارات.
 *
 * ولا يُقاس أيٌّ من الاثنين على جهازٍ إلا بنشر نسخة حقيقية — فيُقاسان هنا على الدالّة.
 */
class UpdatePolicyTest {

    private fun manifest(
        latest: Long? = null,
        floor: Long? = null,
        force: Boolean = false,
        maintenance: UpdateMaintenance? = null,
    ) = UpdateManifest(
        latest = latest?.let {
            UpdateRelease(code = it, name = it.toString(), url = "https://example.com/$it.apk", force = force)
        },
        minimum = floor?.let { UpdateFloor(code = it) },
        maintenance = maintenance,
    )

    @Test
    fun `no manifest and no newer release both mean nothing to show`() {
        assertEquals(UpdateVerdict.NONE, UpdatePolicy.decide(100L, null).verdict)
        assertEquals(UpdateVerdict.NONE, UpdatePolicy.decide(100L, manifest(latest = 100L)).verdict)
        assertEquals(UpdateVerdict.NONE, UpdatePolicy.decide(150L, manifest(latest = 100L)).verdict)
    }

    @Test
    fun `a newer release is optional, and force makes it required`() {
        assertEquals(UpdateVerdict.OPTIONAL, UpdatePolicy.decide(100L, manifest(latest = 200L)).verdict)
        assertEquals(UpdateVerdict.REQUIRED, UpdatePolicy.decide(100L, manifest(latest = 200L, force = true)).verdict)
    }

    @Test
    fun `skipping is per release, so the next version still shows`() {
        val twoHundred = manifest(latest = 200L)
        assertEquals(UpdateVerdict.NONE, UpdatePolicy.decide(100L, twoHundred, skippedCode = 200L).verdict)
        // و2.1 تعود للظهور: التخطّي كان لها وحدها.
        assertEquals(UpdateVerdict.OPTIONAL, UpdatePolicy.decide(100L, manifest(latest = 210L), skippedCode = 200L).verdict)
        // والتخطّي لا يُسقط «required» أبدًا: من فرض إصدارًا لا يُغلق بزر.
        assertEquals(
            UpdateVerdict.REQUIRED,
            UpdatePolicy.decide(100L, manifest(latest = 200L, force = true), skippedCode = 200L).verdict,
        )
    }

    @Test
    fun `below the floor is blocked even when there is nothing newer to install`() {
        val decision = UpdatePolicy.decide(100L, manifest(latest = 100L, floor = 150L))
        assertEquals(UpdateVerdict.BLOCKED, decision.verdict)
        assertTrue(decision.blocking)
        assertEquals(150L, decision.floorCode)
        // وإصدارٌ أحدث موجود ⇒ القرار يحمله ليصير الحجب قابلًا للحلّ بزرّ واحد.
        val withRelease = UpdatePolicy.decide(100L, manifest(latest = 200L, floor = 150L))
        assertEquals(UpdateVerdict.BLOCKED, withRelease.verdict)
        assertEquals(200L, withRelease.release?.code)
        assertTrue(withRelease.hasNewer)
    }

    @Test
    fun `an unreadable local version makes no claim at all`() {
        // رقم مجهول (‑1): «غير معروف» ليست «قديم» ولا «يحتاج تحديثًا» — فلا حجب ولا إعلان.
        assertEquals(UpdateVerdict.NONE, UpdatePolicy.decide(-1L, manifest(latest = 100L, floor = 150L)).verdict)
        assertEquals(UpdateVerdict.NONE, UpdatePolicy.decide(-1L, manifest(latest = 200L, floor = 150L)).verdict)
        // أمّا التعليق فهو حكمٌ **لا يعتمد على إصدار المستخدم**، فيبقى يظهر معه.
        assertEquals(
            UpdateVerdict.MAINTENANCE,
            UpdatePolicy.decide(-1L, manifest(maintenance = UpdateMaintenance(active = true, blocking = true))).verdict,
        )
    }

    @Test
    fun `maintenance suspends the app and carries the newer release as the way out`() {
        val blocking = UpdatePolicy.decide(
            100L,
            manifest(latest = 200L, maintenance = UpdateMaintenance(active = true, blocking = true)),
        )
        assertEquals(UpdateVerdict.MAINTENANCE, blocking.verdict)
        assertTrue(blocking.blocking)
        assertEquals(200L, blocking.release?.code)

        val notice = UpdatePolicy.decide(
            100L,
            manifest(latest = 100L, maintenance = UpdateMaintenance(active = true, blocking = false)),
        )
        assertEquals(UpdateVerdict.MAINTENANCE, notice.verdict)
        assertFalse(notice.blocking)
        assertNull(notice.release)
    }

    @Test
    fun `locale notes ride along with every verdict that shows a message`() {
        val raw = """
            { "minimum": { "code": 150, "notes": { "en": "Too old.", "ar": "قديمة جدًّا." } },
              "latest":  { "code": 200, "url": "https://example.com/2.0.apk",
                           "notes": { "en": "New stuff.", "ar": "جديد." } } }
        """.trimIndent()
        val manifest = UpdateManifests.parse(raw)!!
        val blocked = UpdatePolicy.decide(100L, manifest)
        assertEquals("قديمة جدًّا.", UpdateManifests.localized(blocked.message, "ar"))
        val optional = UpdatePolicy.decide(160L, manifest)
        assertEquals(UpdateVerdict.OPTIONAL, optional.verdict)
        assertEquals("جديد.", UpdateManifests.localized(optional.message, "ar"))
    }

    @Test
    fun `a floor equal to the local version is the last allowed build, not a block`() {
        assertEquals(UpdateVerdict.NONE, UpdatePolicy.decide(150L, manifest(latest = 150L, floor = 150L)).verdict)
    }
}
