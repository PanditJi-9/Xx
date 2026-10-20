/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.core.access

import nd.max.core.license.LicenseState
import nd.max.core.license.LicenseStates
import nd.max.core.license.LicenseTestSupport
import nd.max.core.remote.Admission
import nd.max.core.remote.FeatureState
import nd.max.core.remote.RemoteConfig
import nd.max.core.remote.RemoteConfigs
import nd.max.core.remote.TestConfigs
import nd.max.core.remote.TestKeys
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * مصفوفة الوصول كاملة — وهي **الموضع الواحد** الذي يُدمج فيه قرار اللوحة (`FeatureState`) مع
 * الترخيص الموقّع مع نافذة المجّاني. كل خلية من الاثنتي عشرة تُقاس هنا بلا أندرويد.
 */
class FeatureAccessTest {

    private val server = TestKeys.generate()

    private val none = LicenseState()

    private val active: LicenseState = LicenseStates.evaluate(
        LicenseTestSupport.record(LicenseTestSupport.payload(), server),
        LicenseTestSupport.NOW,
    )

    private val expired: LicenseState = LicenseStates.evaluate(
        LicenseTestSupport.record(
            LicenseTestSupport.payload(expiresAt = LicenseTestSupport.iso(LicenseTestSupport.NOW + 1000)),
            server,
        ),
        LicenseTestSupport.NOW + 2000,
    )

    private val promo = PromoWindow(LicenseTestSupport.NOW - 1000, LicenseTestSupport.NOW + 1000, "عيد")

    // ── المصفوفة المباشرة ────────────────────────────────────────────────────────────────────

    @Test
    fun `a public feature opens for everyone, licence or not`() {
        assertEquals(FeatureAccess.OPEN, FeatureAccessRules.access(FeatureState.ON, none, null))
        assertEquals(FeatureAccess.OPEN, FeatureAccessRules.access(FeatureState.ON, active, null))
        assertEquals(FeatureAccess.OPEN, FeatureAccessRules.access(FeatureState.ON, expired, null))
        assertEquals(FeatureAccess.OPEN, FeatureAccessRules.access(FeatureState.ON, none, promo))
    }

    @Test
    fun `a disabled feature is off even with a valid licence`() {
        assertEquals(FeatureAccess.OFF, FeatureAccessRules.access(FeatureState.OFF, none, null))
        assertEquals(FeatureAccess.OFF, FeatureAccessRules.access(FeatureState.OFF, active, null))
        assertEquals(FeatureAccess.OFF, FeatureAccessRules.access(FeatureState.OFF, active, promo))
    }

    @Test
    fun `a subscription feature is locked without a valid licence`() {
        assertEquals(FeatureAccess.LOCKED, FeatureAccessRules.access(FeatureState.SUBSCRIBE, none, null))
        assertEquals(FeatureAccess.LOCKED, FeatureAccessRules.access(FeatureState.SUBSCRIBE, expired, null))
    }

    @Test
    fun `a valid licence opens it, and a promo opens it for everyone`() {
        assertEquals(FeatureAccess.OPEN, FeatureAccessRules.access(FeatureState.SUBSCRIBE, active, null))
        assertEquals(FeatureAccess.PROMO, FeatureAccessRules.access(FeatureState.SUBSCRIBE, none, promo))
        assertEquals(FeatureAccess.PROMO, FeatureAccessRules.access(FeatureState.SUBSCRIBE, expired, promo))
        assertEquals(
            "والمجّانيّ يتقدّم على المفتاح: النافذة تفتح للجميع حتى لمن له مفتاح",
            FeatureAccess.PROMO,
            FeatureAccessRules.access(FeatureState.SUBSCRIBE, active, promo),
        )
    }

    // ── الدمج من العقد الموقّع نفسه ──────────────────────────────────────────────────────────

    private fun config(features: String, promos: String? = null): RemoteConfig {
        val payload = TestConfigs.payloadJson(
            revision = 7,
            features = features,
            promos = promos,
        )
        val admitted = RemoteConfigs.admit(
            TestConfigs.envelope(payload, server),
            server.publicBase64,
            0L,
            TestConfigs.NOW,
        )
        return (admitted as Admission.Admitted).config
    }

    private fun access(config: RemoteConfig, license: LicenseState, versionCode: Int = 999): FeatureAccess =
        FeatureAccessRules.access(config, license, "maxai", versionCode, TestConfigs.NOW)

    @Test
    fun `the whole decision comes from the signed config and the stored certificate`() {
        val subscribed = config("""{"maxai":{"state":"subscribe"}}""")
        assertEquals("بلا مفتاح ولا نافذة: مقفل", FeatureAccess.LOCKED, access(subscribed, none))
        assertEquals("ومع مفتاح سارٍ: مفتوح", FeatureAccess.OPEN, access(subscribed, active))

        val withPromo = config(
            """{"maxai":{"state":"subscribe"}}""",
            promos = """[{"from":"${TestConfigs.iso(-3_600_000)}","to":"${TestConfigs.iso(3_600_000)}","note":"عيد"}]""",
        )
        assertEquals(
            "والنافذة السارية تفتحه للجميع وتُعلن نفسها",
            FeatureAccess.PROMO,
            access(withPromo, none),
        )
    }

    @Test
    fun `a version floor hides the surface entirely`() {
        val floor = config("""{"maxai":{"state":"subscribe","min_version_code":999}}""")
        assertEquals(
            "نسخة لا تفهم المفتاح لا تراه أصلًا — ولا شاشة شراء لميزة لا تعرفها",
            FeatureAccess.OFF,
            access(floor, active, versionCode = 1),
        )
        assertEquals(FeatureAccess.OPEN, access(floor, active, versionCode = 999))
    }

    @Test
    fun `a promo that is not active right now changes nothing`() {
        val future = config(
            """{"maxai":{"state":"subscribe"}}""",
            promos = """[{"from":"${TestConfigs.iso(86_400_000)}","to":"${TestConfigs.iso(2 * 86_400_000)}","note":"غدًا"}]""",
        )
        assertEquals(FeatureAccess.LOCKED, access(future, none))
        assertEquals(FeatureAccess.OPEN, access(future, active))
    }
}
