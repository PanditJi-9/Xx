/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.core.access

import nd.max.core.license.LicenseState
import nd.max.core.license.LicenseStatus
import nd.max.core.remote.FeatureState
import nd.max.core.remote.RemoteConfig
import nd.max.core.remote.RemoteFlags

/**
 * حكم الوصول النهائيّ لسطح: **دمجُ ما قرّرته اللوحة (`FeatureState`) بما قرّره الخادم لحظة
 * الشراء (`LicenseState`) وبما منحه المالك للجميع (`PromoWindow`)** — في دالّة نقية واحدة.
 *
 * ولماذا دالّة واحدة لا فحصٌ في كل شاشة: كل شاشةٍ تفحص بنفسها = كل شاشةٍ تخطئ بنفسها، ونسخةٌ
 * قديمة من الفحص تبقى في مكان نُسي. وهنا الموضع الواحد الذي يُقرأ منه الحكم في [RemoteAccess]
 * وتُقاس كل مصفوفة حالاته في `tools/test_license_jvm.py` بلا أندرويد.
 */
enum class FeatureAccess {
    /** يعمل: إمّا ميزة عامّة (`on`)، وإمّا مفتاح سارٍ فعّلها. */
    OPEN,

    /** مجّانيّ الآن للجميع: نافذة المالك السارية — تُفتح وتُعلن «مجّاني حتى…». */
    PROMO,

    /** ظاهرٌ مقفل: يلزم مفتاح — الشاشة تعرض الشراء والتفعيل. */
    LOCKED,

    /** أطفأه المالك (`off`). */
    OFF,
}

object FeatureAccessRules {

    /** الحكم الصريح بعناصره — هو ما تُقاس عليه المصفوفة كاملة. */
    fun access(featureState: FeatureState, license: LicenseState, promo: PromoWindow?): FeatureAccess =
        when (featureState) {
            FeatureState.ON -> FeatureAccess.OPEN
            FeatureState.OFF -> FeatureAccess.OFF
            FeatureState.SUBSCRIBE -> when {
                // المجّانيّ يتقدّم على المفتاح: نافذة المالك تفتح للجميع، ولمن له مفتاح أيضًا
                // (وعدّاده متوقّفٌ أصلًا — الإيقاف المؤقّت يُبنى ها هنا في الخادم لا في العرض).
                promo != null -> FeatureAccess.PROMO
                license.status == LicenseStatus.ACTIVE -> FeatureAccess.OPEN
                else -> FeatureAccess.LOCKED
            }
        }

    /** الحكم من العقد نفسه: حالة المفتاح من الإعداد، والترخيص من الذاكرة الموقّعة، والنافذة من الوقت. */
    fun access(
        config: RemoteConfig?,
        license: LicenseState,
        featureId: String,
        versionCode: Int,
        nowMillis: Long,
    ): FeatureAccess = access(
        featureState = RemoteFlags.state(config, featureId, versionCode),
        license = license,
        promo = config?.activePromoAt(nowMillis),
    )
}
