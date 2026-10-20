/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.core.gamespace

/**
 * حجم مقبض اللوحة الجانبية. [scale] يُضرب في أبعاد الرسم كلها (الجسم والشريط والهالة) فيبقى الشكل
 * نفسه بنسب ثابتة، والنافذة تقيس محتواها فتتّسع أو تضيق بلا حساب ثانٍ.
 */
enum class HandleSize(val scale: Float) {
    Small(0.85f),
    Medium(1f),
    Large(1.25f)
}

/**
 * حساسية السحب نحو الداخل لفتح اللوحة. [distanceFactor] يُضرب في مسافة العتبة: أصغر = أسهل فتحًا.
 * الاسم يُخزَّن لا الرقم، فاسم لا يُعرف يسقط إلى [Balanced] ولا يُغيَّر جهاز محفوظ بإعادة ترتيب.
 */
enum class PanelSensitivity(val distanceFactor: Float) {
    Low(1.35f),
    Balanced(1f),
    High(0.7f)
}

/**
 * هندسة المقبض النقيّة (بلا أندرويد فتُقاس في JVM).
 *
 * **الإزاحة تُقاس نحو الداخل من الحافة لا من أعلى يسار الشاشة:** المقبض يُربط بالحافة نفسها عبر جاذبية
 * النافذة (يسار/يمين مطلقتان)، فلا يعتمد موضعه على مقاس شاشة قد يكون قديمًا أثناء تدوير أو تبدّل عرض —
 * وهو سبب ظهوره أحيانًا في منتصف الشاشة حين كان يُحسب بالبكسل المطلق.
 */
object PanelHandleGeometry {

    /**
     * الحافة التي يلتصق بها مقبض تُرك مركزه عند [centerX] على شاشة عرضها [screenWidth]. القرار بالمركز
     * لا بالحافة (ما يتوقّعه الإصبع)، وعرض غير معروف (≤ 0) يُبقي الحافة الحالية [current] بدل تخمين.
     */
    fun sideFor(centerX: Float, screenWidth: Float, current: PanelSide): PanelSide = when {
        screenWidth <= 0f -> current
        centerX < screenWidth / 2f -> PanelSide.Start
        else -> PanelSide.End
    }

    /**
     * الإزاحة نحو الداخل من حافة [side] لنافذة حافتها اليسرى [left] وعرضها [width] على شاشة عرضها
     * [screenWidth]. صفر = ملتصقة بالحافة، وموجب = مبتعدة نحو الداخل، وسالب = تجاوز طفيف للخارج.
     */
    fun inwardFrom(side: PanelSide, left: Float, width: Float, screenWidth: Float): Float =
        if (side == PanelSide.Start) left else screenWidth - width - left
}
