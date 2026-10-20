/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.core.access

/**
 * حالة السطح في الشاشة التعريفية (§٥ — الشاشة الترويجية) — **مشتقّة من [FeatureAccess] وحده**،
 * بلا قراءة ثانية للإعداد وبلا حكم موازٍ.
 *
 * ولماذا تعدادٌ ثالث؟ لأن [FeatureAccess] مفرداتُ حكمٍ (ما قرّرته اللوحة والخادم والمالك)،
 * وهذه مفرداتُ **إعلانٍ للمستخدم**: «متاح» و«مجّانيّ الآن» و«يحتاج مفتاحًا» و«موقوف حاليًّا».
 * فلو كتبت الشاشة الحكم بنفسها لتفرّع الحكمان يومًا — وهي عين العلّة التي أُنشئت
 * `FeatureAccessRules` لأجلها.
 *
 * **والترجمة متطابقة لكنها ليست زائدة**: كل قيمة واجهة لها قيمة حكم واحدة، وكل قيمة حكم لها
 * قيمة واجهة واحدة (والاختبار يقيس الاتجاهين) — فلا صفُّ شاشةٍ لحكمٍ لم يُتَّخذ.
 */
enum class PromoState {
    /** يعمل الآن: ميزة عامّة (`on`)، أو مفتاحٌ سارٍ فعّلها. */
    AVAILABLE,

    /** مجّانيّ الآن للجميع: نافذة المالك السارية — والواجهة تُعلن تاريخ نهايتها. */
    FREE_NOW,

    /** ظاهرٌ يحتاج مفتاحًا: يُعرَض ولا يُخفى، وسبيله شاشة الترخيص. */
    NEEDS_KEY,

    /** أطفأه المالك: لا يُعلَّق على الشاشة. */
    OFF;

    companion object {
        fun of(access: FeatureAccess): PromoState = when (access) {
            FeatureAccess.OPEN -> AVAILABLE
            FeatureAccess.PROMO -> FREE_NOW
            FeatureAccess.LOCKED -> NEEDS_KEY
            FeatureAccess.OFF -> OFF
        }
    }
}
