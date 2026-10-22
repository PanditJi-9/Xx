/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.core.gamespace

/**
 * قرار واحد يقرأه مكانان: واجهة Game Space والخدمة.
 *
 * **العطب الذي يمنعه:** كانت الواجهة تقول «اللوحة مفعّلة» بلا فحص صلاحية التراكب، والخدمة
 * تحاول `addView` فتفشل وتُغلق صامتة. فيرى المستخدم مفتاحًا مفعّلًا ولا نافذة، ويستنتج أن
 * الميزة كلها لا تعمل. الشرط هنا صريح: **لا بند منح إلا إذا كانت هناك لعبة مفعّلة فعلًا
 * والصلاحية ناقصة** — فلا إزعاج لمن لا يستعمل اللوحة، ولا صمت لمن يستعملها.
 */
object PanelOverlayGate {

    /**
     * @param enabledPackages حزم الألعاب التي فُعّلت لها اللوحة (أي واحدة تكفي).
     * @param canDrawOverlays نتيجة `Settings.canDrawOverlays` لهذا التطبيق الآن.
     */
    fun needsGrant(enabledPackages: Collection<String>, canDrawOverlays: Boolean): Boolean =
        enabledPackages.isNotEmpty() && !canDrawOverlays
}
