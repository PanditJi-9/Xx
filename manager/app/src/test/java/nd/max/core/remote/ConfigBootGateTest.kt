/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.core.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * الحالات المضادّة ١ · ٣ · ١١ · ١٢ من §٥.٤ — وهي جوهر «صفر تلاعب» عند الفشل:
 * **لا مسار واحد ينتهي بشاشة خطأ حاجزة أو بميزة تُطفأ بلا سبب.**
 */
class ConfigBootGateTest {

    private val server = TestKeys.generate()
    private val attacker = TestKeys.generate()

    private fun storedConfig(revision: Long = 7, at: Long = TestConfigs.NOW): StoredConfig =
        StoredConfig(
            envelope = TestConfigs.envelope(TestConfigs.payloadJson(revision = revision), server),
            storedAtMillis = at,
            lastAttemptMillis = at,
            lastOutcome = "accepted",
        )

    private fun decide(
        fetched: FetchAttempt,
        stored: StoredConfig?,
        now: Long = TestConfigs.NOW,
    ): BootDecision = ConfigBootGate.decide(now, fetched, stored, server.publicBase64)

    @Test
    fun `a config that just arrived is used and named as the server's`() {
        val envelope = TestConfigs.envelope(
            TestConfigs.payloadJson(revision = 9, features = """{"maxai":{"enabled":false}}"""),
            server,
        )
        val decision = decide(FetchAttempt.Received(envelope, "\"etag-1\""), storedConfig(revision = 7))
        assertEquals("المصدر", ConfigOrigin.SERVER, decision.origin)
        assertEquals("الإصدار", 9L, decision.revision)
        assertTrue("الطازج طازج", decision.freshness == ConfigFreshness.FRESH)
        assertNull("ولا عطب في الطريق", decision.code)
        assertFalse("ولا طلب ثانٍ في الخلفية", decision.refreshInBackground)
        assertEquals("والإصدار القديم لم يُمنع", false, decision.config.features.getValue("maxai").enabled)
    }

    @Test
    fun `no server at all leaves the last good config in charge`() {
        val decision = decide(FetchAttempt.Failed(RemoteFault.NETWORK.code), storedConfig(revision = 7))
        assertEquals("مصدر الحكم: الذاكرة", ConfigOrigin.MEMORY, decision.origin)
        assertEquals("7", decision.revision.toString())
        assertEquals("والعطب مسمّى", RemoteFault.NETWORK.code, decision.code)
        assertTrue("والطلب يُعاد في الخلفية", decision.refreshInBackground)
    }

    @Test
    fun `the free project being paused behaves like any other outage`() {
        // §٤.٣: أخطر ما في الخطّة المجّانية وقفةُ السكون. والسلوك المطلوب: يعمل كل شيء من
        // الذاكرة/الافتراضي، والمعطوب يُعلن معطوبًا.
        val paused = decide(FetchAttempt.Failed(RemoteFault.SERVER.code), storedConfig(revision = 4))
        assertEquals(ConfigOrigin.MEMORY, paused.origin)
        assertEquals(RemoteFault.SERVER.code, paused.code)

        val pausedFresh = decide(FetchAttempt.Failed(RemoteFault.SERVER.code), stored = null)
        assertEquals("وبلا ذاكرة: الافتراضي المحلي", ConfigOrigin.DEFAULT, pausedFresh.origin)
        assertEquals(RemoteFault.SERVER.code, pausedFresh.code)
    }

    @Test
    fun `a forged config never beats the memory`() {
        val forged = TestConfigs.envelope(TestConfigs.payloadJson(revision = 99), attacker)
        val decision = decide(FetchAttempt.Received(forged, null), storedConfig(revision = 7))
        assertEquals(
            "تلاعب ⇒ يُسقط ويُسجَّل، والذاكرة تبقى الحكم",
            ConfigOrigin.MEMORY,
            decision.origin,
        )
        assertEquals(RemoteRejection.SIGNATURE.code, decision.code)
        assertEquals(7L, decision.revision)
    }

    @Test
    fun `a downgrade attempt falls back to the memory it tried to replace`() {
        val older = TestConfigs.envelope(TestConfigs.payloadJson(revision = 2), server)
        val decision = decide(FetchAttempt.Received(older, null), storedConfig(revision = 7))
        assertEquals(ConfigOrigin.MEMORY, decision.origin)
        assertEquals(RemoteRejection.REVISION.code, decision.code)
        assertEquals(7L, decision.revision)
    }

    @Test
    fun `an empty device lands on the local defaults, with the flags the app already had`() {
        val decision = decide(FetchAttempt.NotAttempted, stored = null)
        assertEquals("لا خادم ولا ذاكرة ⇒ الافتراضي", ConfigOrigin.DEFAULT, decision.origin)
        assertEquals("وعمره: لا شيء", ConfigFreshness.ABSENT, decision.freshness)
        assertEquals("ولا إصدار", 0L, decision.revision)
        assertNull("ولا عطب وقع — لم نحاول", decision.code)
        // ولا تُفقد السلامة: الافتراضي = سلوك اليوم (كل السطوح كما هي في الحزمة).
        RemoteFlags.ALL.forEach { flag ->
            assertTrue(
                "المفتاح ${flag.id} يجب أن يكون على حاله في الحزمة (${flag.enabledByDefault})",
                RemoteFlags.isOn(decision.config, flag.id, versionCode = 999) == flag.enabledByDefault,
            )
        }
    }

    @Test
    fun `a stale memory is still used, but it says it is stale`() {
        val decision = decide(
            FetchAttempt.Failed(RemoteFault.TIMEOUT.code),
            storedConfig(at = TestConfigs.NOW - 48 * 3_600_000L),
        )
        assertEquals(ConfigOrigin.MEMORY, decision.origin)
        assertEquals("العمر يُعلن ولا يُخفى", ConfigFreshness.STALE, decision.freshness)
    }

    @Test
    fun `the memory decides its own ttl, and a wild one is clamped`() {
        val envelope = TestConfigs.envelope(
            TestConfigs.payloadJson(
                revision = 3,
                stats = """{"collect":false,"sample_rate":100,"ttl_hours":2}""",
            ),
            server,
        )
        val memory = StoredConfig(
            envelope = envelope,
            storedAtMillis = TestConfigs.NOW - 3 * 3_600_000L,
            lastAttemptMillis = TestConfigs.NOW,
            lastOutcome = "accepted",
        )
        assertEquals(
            "‏TTL الموقَّع هو الحاكم في عمر الذاكرة",
            ConfigFreshness.STALE,
            decide(FetchAttempt.NotAttempted, memory).freshness,
        )
    }

    @Test
    fun `a memory whose bytes were edited by hand is dropped for the defaults`() {
        // العدوان الواقعي: تغيير بايتات الحمولة مع إبقاء التوقيع القديم. والتوقيع هو الذي يمسكه
        // لا تخمين الصيغة — وهذا بالضبط سبب إعادة التحقّق من الذاكرة في كل إقلاع (§٥.١).
        val edited = storedConfig().copy(
            envelope = storedConfig().envelope.copy(payload = "AAAA"),
        )
        val decision = decide(FetchAttempt.NotAttempted, edited)
        assertEquals("ذاكرة مبدَّلة ⇒ الافتراضي", ConfigOrigin.DEFAULT, decision.origin)
        assertEquals(
            "والرمز يقول: توقيع، لا صيغة",
            RemoteRejection.SIGNATURE.code,
            decision.code,
        )
    }

    @Test
    fun `a memory that is not even base64 is named as a format problem`() {
        val broken = storedConfig().copy(
            envelope = storedConfig().envelope.copy(payload = "not base64 !!"),
        )
        val decision = decide(FetchAttempt.NotAttempted, broken)
        assertEquals(ConfigOrigin.DEFAULT, decision.origin)
        assertEquals(RemoteRejection.MALFORMED.code, decision.code)
    }

    @Test
    fun `an unset key keeps both paths shut without pretending the signature failed`() {
        val decision = ConfigBootGate.decide(
            nowMillis = TestConfigs.NOW,
            fetched = FetchAttempt.Received(
                TestConfigs.envelope(TestConfigs.payloadJson(revision = 1), server),
                null,
            ),
            stored = storedConfig(),
            publicKeyBase64 = "",
        )
        assertEquals(ConfigOrigin.DEFAULT, decision.origin)
        assertEquals(RemoteRejection.NOT_CONFIGURED.code, decision.code)
    }
}
