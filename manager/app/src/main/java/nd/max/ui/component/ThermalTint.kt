/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.component

import androidx.compose.ui.graphics.Color
import nd.max.core.gamespace.GameThermalProfiles
import nd.max.core.gamespace.ThermalPanelState
import nd.max.core.gamespace.ThermalPhase

/**
 * لون لوحة اللعبة حسب **بروفايل الحرارة** المختار.
 *
 * الألوان هي نفسها ألوان شاشة الإعدادات وبطاقات القائمة (أخضر موفّر · أزرق متوازن · برتقالي ألعاب ·
 * أحمر أداء · بنفسجي مخصّص)، فيتّفق ما يراه المستخدم في كل موضع.
 *
 * قاعدتان مقصودتان:
 *  ١. **الافتراضي لا يُلوَّن**: يبقى مظهر اللوحة كما هو (لون الـHUD أو لون وضع النظام)، فلا يتغيّر شيء
 *     لمن لا يستعمل بروفايلًا.
 *  ٢. **ما لم يُطبَّق لا يُلوَّن**: حين يرفض المحرّك الطلب ([ThermalPhase.Failed]) لا تُعرض اللوحة بلون
 *     بروفايل لا يعمل فعلًا — اللون جزء من الحقيقة التي تعرضها اللوحة، ورسالة الرفض تظهر في بطاقته.
 *     أما الانتظار والتطبيق والتأكيد فتأخذ لون البروفايل فورًا ليكون اللمس محسوسًا.
 *
 * ولا يتجاوز لونَ البروفايل شيء: لا حمل المعالج ولا الحرارة (كان تجاوز «التحذير» يُحمِّر اللوحة كلها
 * ويخفي لون البروفايل، ويتذبذب عند العتبة فيبدو عشوائيًا). والحرارة تبقى إشارةً غير لونية: نبض المقبض أسرع.
 */
internal object ThermalTint {

    /** لون معرّف بروفايل؛ المجهول والافتراضي رمادي محايد (يُستعمل في أيقونة البطاقة لا في الحافّة). */
    fun colorOf(id: String): Color = when (id) {
        "power" -> Color(0xFF36C77B)
        "balanced" -> Color(0xFF3D8EFF)
        "gaming" -> Color(0xFFFF9800)
        "performance" -> Color(0xFFFF4444)
        "custom" -> Color(0xFFB07CFF)
        else -> Color(0xFFB4B8BF)
    }

    /** لون اللوحة من الحالة، أو `null` حين يبقى المظهر المعتاد (افتراضي، أو طلب مرفوض). */
    fun of(thermal: ThermalPanelState): Color? {
        if (thermal.phase == ThermalPhase.Failed) return null
        val id = GameThermalProfiles.normalize(thermal.selected)
        if (id == GameThermalProfiles.DEFAULT) return null
        return colorOf(id)
    }
}
