/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.ui.gamespace

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/*
 * MONOLITH — لغة Game Space الجديدة.
 *
 * ليست لوحة HUD سيبرانية: لا scanlines ولا أحرف أحادية المسافة ولا نيون بنفسجي.
 * هي آلة دقيقة: بازلت قريب من السواد، عاج هادئ للنصّ، شعرات ضوئية رفيعة،
 * وزجاج مصنوع من فرق سطوع لا من ظلّ. اللون المميّز يأتي من اللعبة نفسها،
 * والبرتقالي الجمرّي يبقى توقيع التطبيق حين لا لون.
 *
 * كل قيمة لون هنا مصدرها الوحيد؛ الشاشات لا تكتب `Color(0x…)` بنفسها.
 */
object Monolith {
    /* الأسطح — خمس درجات فقط، لكل درجة وظيفة واحدة. */
    val Obsidian: Color = Color(0xFF07080A)
    val Basalt: Color = Color(0xFF0C0E12)
    val Slate: Color = Color(0xFF14171D)
    val Glass: Color = Color(0xFF1A1E26)
    val GlassRaised: Color = Color(0xFF232833)

    /* الحبر — ثلاث درجات، لا رمادي على رمادي. */
    val Bone: Color = Color(0xFFF4F1EA)
    val BoneDim: Color = Color(0xFFC9C6BE)
    val Muted: Color = Color(0xFF9AA1AE)
    val Faint: Color = Color(0xFF6C7380)

    /* الشعرات — الحدود لا تُرى إلا حين يجب. */
    val Hairline: Color = Color(0x1FFFFFFF)
    val HairlineStrong: Color = Color(0x33FFFFFF)

    /* المعاني — لون واحد لكل معنى، ولا يُستعمل زينة. */
    val Ember: Color = Color(0xFFFF5A36)
    val EmberDeep: Color = Color(0xFFC93A1E)
    val Mint: Color = Color(0xFF59E0B0)
    val Amber: Color = Color(0xFFF5B759)
    val Ice: Color = Color(0xFF8FD7FF)
    val Danger: Color = Color(0xFFFF6B6B)

    /* الطباعة — تحريرية، واسعة، بلا زخرفة. */
    val display: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Light,
        fontSize = 44.sp,
        lineHeight = 46.sp,
        letterSpacing = (-1.2).sp
    )
    val title: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.4).sp
    )
    val heading: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp
    )
    val body: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp
    )
    val label: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.6.sp
    )
    val eyebrow: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 2.2.sp
    )
    val numeral: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Light,
        fontSize = 30.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.6).sp
    )
}

/**
 * لون اللعبة بعد تطويعه لسطح داكن: نرفع سطوعه قليلًا ونخفض تشبّعه إن كان صارخًا،
 * فلا يصبح الشريط أفتح من النصّ ولا يبتلع الحبر. القيمة الوحيدة التي تُشتقّ من الأيقونة.
 */
@Immutable
data class MonolithAccent(
    val base: Color,
    val soft: Color,
    val ink: Color
)

fun monolithAccent(source: Color?): MonolithAccent {
    val base = source ?: Monolith.Ember
    val lifted = lerp(base, Color.White, 0.18f)
    return MonolithAccent(
        base = lifted,
        soft = lifted.copy(alpha = 0.14f),
        ink = if (lifted.luminance() > 0.55f) Monolith.Obsidian else Monolith.Bone
    )
}
