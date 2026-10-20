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
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * الحكم الفعليّ لمفتاح — وهو آخر ما يفصل بين «اللوحة كتبت شيئًا» و«جهاز المستخدم تغيّر».
 *
 * والقاعدتان اللتان تُقاسان هنا: ① الافتراضي عند غياب الخادم **سلوك اليوم**، فلا يُغيّر السقوطُ
 * شيئًا؛ ② `min_version_code` يمنع مفتاحًا لمن لا يعرفه، فلا يُشترى مفتاح لتطبيقٍ لا يفهمه.
 */
class RemoteFlagsTest {

    private val server = TestKeys.generate()

    private fun config(features: String, revision: Long = 5): RemoteConfig {
        val payload = TestConfigs.payloadJson(revision = revision, features = features)
        val admitted = RemoteConfigs.admit(
            TestConfigs.envelope(payload, server),
            server.publicBase64,
            0L,
            TestConfigs.NOW,
        )
        return (admitted as Admission.Admitted).config
    }

    @Test
    fun `every known flag names a surface that exists in this tree`() {
        assertTrue("السِجلّ لا يكون فارغًا", RemoteFlags.ALL.isNotEmpty())
        RemoteFlags.ALL.forEach { flag ->
            assertTrue("معرّف فارغ", flag.id.isNotBlank())
            assertTrue("سطح غير مسمّى للمفتاح ${flag.id}", flag.surface.contains("/"))
        }
        assertEquals(
            "لا معرّف مكرّر",
            RemoteFlags.ALL.size,
            RemoteFlags.ALL.map { it.id }.toSet().size,
        )
    }

    @Test
    fun `with no config at all each flag keeps the state the app ships with`() {
        RemoteFlags.ALL.forEach { flag ->
            assertEquals(
                "المفتاح ${flag.id} بلا خادم يجب أن يبقى كما في الحزمة",
                flag.enabledByDefault,
                RemoteFlags.isOn(null, flag.id, versionCode = 1),
            )
        }
    }

    @Test
    fun `the server can turn a flag off and that is all it can do`() {
        val off = config("""{"maxai":{"enabled":false}}""")
        assertFalse("أُطفئ من اللوحة", RemoteFlags.isOn(off, "maxai", versionCode = 999))
        assertTrue(
            "وغيره لم يُمَسّ",
            RemoteFlags.isOn(off, "gamespace", versionCode = 999),
        )
    }

    @Test
    fun `a flag with a version floor is invisible to builds below it`() {
        val config = config("""{"maxai":{"enabled":true,"min_version_code":210}}""")
        assertFalse(
            "نسخة أقدم لا تُشغّل مفتاحًا لا تعرفه",
            RemoteFlags.isOn(config, "maxai", versionCode = 209),
        )
        assertTrue(
            "والنسخة التي تفهمه تُشغّله",
            RemoteFlags.isOn(config, "maxai", versionCode = 210),
        )
    }

    @Test
    fun `a flag nobody declared is never on`() {
        val config = config("""{"ghost":{"enabled":true}}""")
        assertFalse(
            "مفتاح ليس في السِجلّ لا يُشغّل شيئًا ولو قال الخادم `true`",
            RemoteFlags.isOn(config, "ghost", versionCode = 999),
        )
    }

    @Test
    fun `the local default mentions every flag by name`() {
        val defaults = RemoteFlags.defaultConfig()
        RemoteFlags.ALL.forEach { flag ->
            assertTrue(
                "الافتراضي يجب أن يذكر ${flag.id} صراحةً لا أن يعتمد على الغياب",
                defaults.features.containsKey(flag.id),
            )
        }
        assertTrue("ولا يعطي نفسه إصدارًا لم يحصل عليه", defaults.revision == 0L)
        assertTrue("ولا يدّعي إعلانًا", defaults.announcementAt(TestConfigs.NOW) == null)
    }

    // ── معنى «اشتراك» ونوافذ المجّاني (جولة LICENSE-KEYS-01) ─────────────────────────────────

    @Test
    fun `subscribe is a state of its own, not off`() {
        val subscribed = config("""{"maxai":{"state":"subscribe"}}""")
        assertEquals(
            "«اشتراك» حالة ثالثة تُقرأ باسمها لا تُدمج في الإطفاء",
            FeatureState.SUBSCRIBE,
            RemoteFlags.state(subscribed, "maxai", versionCode = 999),
        )
        assertTrue(
            "والسطح يبقى ظاهرًا (مقفلًا بمفتاح) — الإخفاء للمطفأ وحده",
            RemoteFlags.isOn(subscribed, "maxai", versionCode = 999),
        )
    }

    @Test
    fun `the written state wins over the legacy enabled field`() {
        val conflicted = config("""{"maxai":{"enabled":true,"state":"off"}}""")
        assertEquals(
            "نصّ الحالة مقدَّم، فإعداد اللوحة الجديد لا يُقرأ نصفه",
            FeatureState.OFF,
            RemoteFlags.state(conflicted, "maxai", versionCode = 999),
        )
    }

    @Test
    fun `a legacy payload without state still reads enabled`() {
        val legacy = config("""{"maxai":{"enabled":true}}""")
        assertEquals(
            "إعدادٌ قديم صالح لا يتعطّل: غياب النصّ يسقط على `enabled`",
            FeatureState.ON,
            RemoteFlags.state(legacy, "maxai", versionCode = 999),
        )
    }

    @Test
    fun `promo windows are resolved to millis and invalid ones are dropped`() {
        val withPromos = promoConfig(
            promos = """[
                {"from":"${TestConfigs.iso(-3_600_000)}","to":"${TestConfigs.iso(3_600_000)}","note":"عيد"},
                {"from":"${TestConfigs.iso(86_400_000)}","to":"${TestConfigs.iso(-86_400_000)}","note":"منقلبة"},
                {"from":"ليس وقتًا","to":"${TestConfigs.iso(86_400_000)}","note":"مكسورة"}
            ]""",
        )
        assertEquals("نافذة واحدة صالحة من ثلاث", 1, withPromos.promos.size)
        assertNotNull(
            "والسارية تُقرأ في وقتها",
            withPromos.activePromoAt(TestConfigs.NOW),
        )
        assertNull(
            "وما بعد نهايتها لا",
            withPromos.activePromoAt(TestConfigs.NOW + 2 * 3_600_000),
        )
    }

    private fun promoConfig(promos: String, revision: Long = 9): RemoteConfig {
        val payload = TestConfigs.payloadJson(revision = revision, promos = promos)
        val admitted = RemoteConfigs.admit(
            TestConfigs.envelope(payload, server),
            server.publicBase64,
            0L,
            TestConfigs.NOW,
        )
        return (admitted as Admission.Admitted).config
    }

    @Test
    fun `a date-only promo window is read as UTC midnight instead of being dropped`() {
        // العطب الذي أُصلح: لوح التحكّم يكتب `2026-10-14` (تاريخ بشري)، والخادم يقبله بـ`Date.parse`
        // ويحسب به إيقاف عدّاد المدفوع، بينما `Instant.parse` كانت ترفضه فيُهمَل في التطبيق —
        // فيُحسب في طرف ولا يُفتح في الطرف الآخر. وهذا الاختبار يثبّت القبول في الطرفين معًا.
        val start = LocalDate.of(2026, 10, 14)
        val end = LocalDate.of(2026, 10, 20)
        val config = promoConfig(promos = """[{"from":"$start","to":"$end","note":"عطلة"}]""")
        assertEquals("النافذة تُقرأ لا تُهمَل", 1, config.promos.size)
        val window = config.promos.single()
        assertEquals(
            "البداية منتصف ليل UTC لليوم المكتوب",
            start.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            window.fromMillis,
        )
        assertEquals(
            "والنهاية منتصف ليل يوم النهاية — النافذة نصف مفتوحة",
            end.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            window.toMillis,
        )
        // ولحظة كاملة ما زالت تُقرأ كما هي: التوسيع لم يُلغِ الدقّة ولا استبدلها.
        val precise = promoConfig(promos = """[{"from":"2026-10-14T05:00:00Z","to":"2026-10-14T06:00:00Z"}]""")
        assertEquals("واللحظة الكاملة تُقرأ", 1, precise.promos.size)
    }
}
