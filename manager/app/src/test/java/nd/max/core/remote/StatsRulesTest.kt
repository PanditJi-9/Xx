/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * **قواعد قناة الإحصاءات** (`MAX-CONTROL-01` §١٠.٣–§١٠.٤) — تُقاس على JVM في أجزاء الثانية:
 * الموافقة قبل كل شيء، والإيقاع (يوم/أسبوع)، والتباعد (١ · ٤ ساعات)، وإسقاط اليوم الثالث،
 * **والرسالة نفسها بايتًا بايتًا** — وهي التي تعرضها شاشة الموافقة حرفيًّا.
 */
class StatsRulesTest {

    private val hour = 60 * 60 * 1000L
    private val day = 24 * hour
    private val now = 1_800_000_000_000L

    private fun sentAgo(
        age: Long,
        version: String = "42",
        env: String = "none",
        health: String = "{}",
    ): StatsState =
        StatsState(
            lastSentAt = now - age,
            lastSentVersion = version,
            lastSentEnv = env,
            lastSentHealth = health,
        )

    private fun due(
        state: StatsState,
        version: String = "42",
        env: String = "none",
        consent: Boolean = true,
        collect: Boolean = true,
        health: String = "{}",
    ): StatsDue = StatsRules.due(state, consent, collect, version, env, now, health)

    @Test
    fun `no consent means no signal even when the server asks`() {
        assertEquals(StatsDue.Skip(StatsSkip.CONSENT_OFF), due(StatsState(), consent = false))
    }

    @Test
    fun `no signal when the server did not ask even with consent`() {
        assertEquals(StatsDue.Skip(StatsSkip.COLLECT_OFF), due(StatsState(), collect = false))
    }

    @Test
    fun `the first signal with both yeses is due`() {
        assertEquals(StatsDue.Send, due(StatsState()))
    }

    @Test
    fun `nothing changed today means skip`() {
        assertEquals(StatsDue.Skip(StatsSkip.RECENT), due(sentAgo(3 * hour)))
    }

    @Test
    fun `nothing changed within the week means skip`() {
        assertEquals(StatsDue.Skip(StatsSkip.UNCHANGED), due(sentAgo(2 * day)))
    }

    @Test
    fun `a week without change renews the signal`() {
        assertEquals(StatsDue.Send, due(sentAgo(8 * day)))
    }

    @Test
    fun `a new version is a real event and passes the week gate`() {
        assertEquals(StatsDue.Send, due(sentAgo(2 * hour, version = "41"), version = "42"))
    }

    @Test
    fun `a new control env is a real event and passes the week gate`() {
        assertEquals(StatsDue.Send, due(sentAgo(2 * hour, env = "none"), env = "root_other"))
    }

    @Test
    fun `new health counters are a real event and pass the week gate`() {
        val counters = "{\"cpu\":{\"confirmed\":3,\"rolled_back\":1,\"unknown\":0}}"
        assertEquals(StatsDue.Send, due(sentAgo(2 * hour), health = counters))
        // ونفس الأعداد ⇒ لا حدث: القاعدة «تخطّي ما لا يتغيّر» تشمل العدّادات نفسها.
        assertEquals(StatsDue.Skip(StatsSkip.RECENT), due(sentAgo(2 * hour, health = counters), health = counters))
    }

    @Test
    fun `the first failure backs off one hour`() {
        val failed = StatsState(attempts = 1, lastAttemptAt = now - 30 * 60 * 1000L)
        assertEquals(StatsDue.Skip(StatsSkip.BACKOFF), due(failed))
        assertEquals(StatsDue.Send, due(failed.copy(lastAttemptAt = now - 61 * 60 * 1000L)))
    }

    @Test
    fun `the second failure backs off four hours`() {
        val failed = StatsState(attempts = 2, lastAttemptAt = now - 2 * hour)
        assertEquals(StatsDue.Skip(StatsSkip.BACKOFF), due(failed))
        assertEquals(StatsDue.Send, due(failed.copy(lastAttemptAt = now - 5 * hour)))
    }

    @Test
    fun `three failures drop the signal for the rest of the day`() {
        val failed = StatsState(attempts = 3, lastAttemptAt = now - 6 * hour)
        assertEquals(StatsDue.Skip(StatsSkip.EXHAUSTED), due(failed))
        assertEquals(StatsDue.Send, due(failed.copy(lastAttemptAt = now - 25 * hour)))
    }

    @Test
    fun `consent is checked before any pacing rule`() {
        val failed = StatsState(attempts = 3, lastAttemptAt = now - hour)
        assertEquals(StatsDue.Skip(StatsSkip.CONSENT_OFF), due(failed, consent = false))
    }

    @Test
    fun `a country code is two letters or the safe default`() {
        assertEquals("SA", StatsRules.countryOf("sa"))
        assertEquals("DE", StatsRules.countryOf(" de "))
        assertEquals("XX", StatsRules.countryOf("SAU"))
        assertEquals("XX", StatsRules.countryOf("S1"))
        assertEquals("XX", StatsRules.countryOf(null))
        assertEquals("XX", StatsRules.countryOf(""))
    }

    @Test
    fun `the control env names only what the app can tell apart`() {
        assertEquals("none", StatsRules.envOf("NONE"))
        assertEquals("shizuku", StatsRules.envOf("SHIZUKU"))
        assertEquals("root_other", StatsRules.envOf("ROOT"))
        assertEquals("none", StatsRules.envOf(null))
        assertEquals("none", StatsRules.envOf("MAGISK-UNKNOWN"))
    }

    @Test
    fun `the payload is the exact literal the screen shows and the wire carries`() {
        val payload = StatsRules.trackPayload(
            installId = "0f8fad5b-d9cb-469f-a165-70867728950e",
            country = "sa",
            version = "42",
            manufacturer = "Xiaomi",
            model = "24069PC21G",
            sdk = 34,
            release = "14",
            env = "shizuku",
        )
        assertEquals(
            "{\"p_install\":\"0f8fad5b-d9cb-469f-a165-70867728950e\"," +
                "\"p_country\":\"SA\"," +
                "\"p_version\":\"42\"," +
                "\"p_manufacturer\":\"Xiaomi\"," +
                "\"p_model\":\"24069PC21G\"," +
                "\"p_sdk\":34," +
                "\"p_release\":\"14\"," +
                "\"p_env\":\"shizuku\"," +
                "\"p_health\":{}}",
            payload,
        )
        assertEquals(9, Regex("\"p_[a-z_]+\"\\s*:").findAll(payload).count())
    }

    @Test
    fun `the health object rides raw and anything else is dropped`() {
        val counters = "{\"cpu\":{\"confirmed\":3,\"rolled_back\":1,\"unknown\":0}}"
        val withCounters = StatsRules.trackPayload(
            installId = "id",
            country = "sa",
            version = "42",
            manufacturer = "X",
            model = "Y",
            sdk = 34,
            release = "14",
            env = "none",
            health = counters,
        )
        assertTrue("العدّادات تصل ككائن لا كسلسلة مهرَّبة", withCounters.contains("\"p_health\":$counters}"))
        // وما ليس كائن JSON صريحًا يُسقَط إلى كائن فارغ — لا نصّ حرّ يعبر السلك.
        listOf("garbage", "[1,2]", "\"str\"", "{\"a\":1} ".repeat(600)).forEach { bad ->
            val payload = StatsRules.trackPayload(
                installId = "id", country = "sa", version = "42", manufacturer = "X",
                model = "Y", sdk = 34, release = "14", env = "none", health = bad,
            )
            assertTrue("نصّ غير صالح عبر السلك: ${bad.take(20)}", payload.endsWith("\"p_health\":{}}"))
        }
    }

    @Test
    fun `quotes and control characters never break the payload`() {
        val payload = StatsRules.trackPayload(
            installId = "id",
            country = "xx",
            version = "4\"2",
            manufacturer = "a\\b",
            model = "line\nbreak",
            sdk = 1,
            release = "14\t",
            env = "not-allowed",
        )
        assertEquals(
            "{\"p_install\":\"id\"," +
                "\"p_country\":\"XX\"," +
                "\"p_version\":\"4\\\"2\"," +
                "\"p_manufacturer\":\"a\\\\b\"," +
                "\"p_model\":\"line\\nbreak\"," +
                "\"p_sdk\":1," +
                "\"p_release\":\"14\\t\"," +
                "\"p_env\":\"none\"," +
                "\"p_health\":{}}",
            payload,
        )
    }

    @Test
    fun `the payload truncates exactly where the schema truncates`() {
        val payload = StatsRules.trackPayload(
            installId = "id",
            country = "XX",
            version = "v".repeat(50),
            manufacturer = "m".repeat(80),
            model = "x".repeat(80),
            sdk = 34,
            release = "r".repeat(50),
            env = "none",
        )
        assertTrue(payload.contains("\"p_version\":\"${"v".repeat(20)}\""))
        assertTrue(payload.contains("\"p_manufacturer\":\"${"m".repeat(40)}\""))
        assertTrue(payload.contains("\"p_model\":\"${"x".repeat(40)}\""))
        assertTrue(payload.contains("\"p_release\":\"${"r".repeat(20)}\""))
    }

    @Test
    fun `the forget request carries the install id and nothing else`() {
        assertEquals("{\"p_install\":\"abc\"}", StatsRules.forgetPayload("abc"))
    }
}
