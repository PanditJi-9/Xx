/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * ألوان الواجهات الصوتية السيبرانية، كلها مأخوذة من مخطّط Material 3 النشط في الإعدادات.
 *
 * - [accent]: لون التشغيل والحالة الفعّالة (primary)، بدل الأحمر الثابت القديم.
 * - [accentDeep]: الطرف الثاني للتدرّجات (tertiary).
 * - [accentCool]: لمسة التمييز الثانوية للمؤشرات والنصوص الحيّة (secondary).
 */
internal data class CyberPalette(
    val accent: Color,
    val accentDeep: Color,
    val accentCool: Color,
)

@Composable
internal fun cyberPalette(): CyberPalette {
    val scheme = MaterialTheme.colorScheme
    return CyberPalette(
        accent = scheme.primary,
        accentDeep = scheme.tertiary,
        accentCool = scheme.secondary,
    )
}
