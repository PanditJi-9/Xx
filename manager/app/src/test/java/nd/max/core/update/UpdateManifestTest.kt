package nd.max.core.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * عقد المانيفست المنشور: ما نقبله، وما نرفضه، وكيف نختار النصّ.
 *
 * هذه اختبارات **على نصٍّ لا على شبكة** — لأن العطب الذي تُثبِّته هنا هو عطب النشر:
 * ملفّ كُتب بيد المالك وفيه حقل باسم مختلف، أو قناةٌ أكبر من `latest`، أو ملاحظات
 * بلغةٍ غير مذكورة. ولا يمكن اكتشاف أيٍّ منها على جهاز لم تُنشر فيه نسخة أحدث فعلًا.
 */
class UpdateManifestTest {

    private val minimal = """
        { "schema": 1,
          "latest": { "code": 200, "name": "2.0", "url": "https://example.com/2.0.apk",
                      "sha256": "abc", "size": 1234,
                      "notes": { "en": "Fixes and speed.", "ar": "إصلاحات وسرعة." } } }
    """.trimIndent()

    @Test
    fun `a minimal file with only latest parses and resolves`() {
        val manifest = UpdateManifests.parse(minimal)
        assertTrue(manifest != null)
        val release = UpdateManifests.releaseFor(manifest!!, UpdateManifests.STABLE)
        assertEquals(200L, release?.code)
        assertEquals("2.0", release?.name)
        assertEquals(1234L, release?.size)
    }

    @Test
    fun `a channel entry overrides latest and stable is the fallback`() {
        val raw = """
            { "schema": 1,
              "latest":  { "code": 200, "url": "https://example.com/stable.apk" },
              "channels": { "beta": { "code": 250, "url": "https://example.com/beta.apk" } } }
        """.trimIndent()
        val manifest = UpdateManifests.parse(raw)!!
        assertEquals(250L, UpdateManifests.releaseFor(manifest, "beta")?.code)
        // قناة مجهولة تسقط إلى stable، لا إلى «لا شيء».
        assertEquals(200L, UpdateManifests.releaseFor(manifest, "nightly")?.code)
    }

    @Test
    fun `unknown fields are ignored so tomorrow's schema does not break today's build`() {
        val raw = """
            { "schema": 2, "subscriptions": { "pro": true }, "campaigns": [1, 2],
              "latest": { "code": 200, "url": "https://example.com/2.0.apk", "price": "x" } }
        """.trimIndent()
        assertEquals(200L, UpdateManifests.parse(raw)?.latest?.code)
    }

    @Test
    fun `a broken file yields null instead of a half-zero model`() {
        assertNull(UpdateManifests.parse(""))
        assertNull(UpdateManifests.parse("{"))
        assertNull(UpdateManifests.parse("""{ "latest": { "code": "two hundred" } }"""))
        assertNull(UpdateManifests.parse("null"))
    }

    @Test
    fun `a release without a usable https url is not offered`() {
        val raw = """{ "latest": { "code": 200, "url": "http://example.com/2.0.apk" } }"""
        assertNull(UpdateManifests.releaseFor(UpdateManifests.parse(raw)!!, UpdateManifests.STABLE))

        val zero = """{ "latest": { "code": 0, "url": "https://example.com/2.0.apk" } }"""
        assertNull(UpdateManifests.releaseFor(UpdateManifests.parse(zero)!!, UpdateManifests.STABLE))
    }

    @Test
    fun `notes fall through complete tag to base tag to english before any value`() {
        val notes = mapOf("zh-CN" to "简体", "en" to "English", "de" to "Deutsch")
        assertEquals("简体", UpdateManifests.localized(notes, "zh-CN"))
        assertEquals("简体", UpdateManifests.localized(notes, "zh"))
        assertEquals("English", UpdateManifests.localized(notes, "fr"))
        assertEquals("Deutsch", UpdateManifests.localized(mapOf("de" to "Deutsch"), "ar"))
        assertNull(UpdateManifests.localized(emptyMap(), "ar"))
    }

    @Test
    fun `an unusable floor entry does not invent a block`() {
        val raw = """{ "minimum": { "code": 0, "notes": { "en": "x" } } }"""
        val manifest = UpdateManifests.parse(raw)!!
        assertEquals(0L, manifest.minimum?.code)
        assertFalse(UpdatePolicy.decide(100L, manifest).verdict == UpdateVerdict.BLOCKED)
    }
}
