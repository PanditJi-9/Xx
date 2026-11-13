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
 * آلة دقيقة، لا لوحة HUD: بازلت قريب من السواد، عاج هادئ للنصّ، شعرات رفيعة،
 * ولون اللعبة نفسها هو الشيء الوحيد المسموح له أن يلمع. لا scanlines، ولا نيون
 * بنفسجي، ولا ظلّ ملوّن، ولا تدرّج بين لونين.
 *
 * كل قيمة لون هنا مصدرها الوحيد؛ الشاشات لا تكتب `Color(0x…)` بنفسها.
 */
object Monolith {
    /* الأسطح — خمس درجات محايدة، لكل درجة وظيفة واحدة. */
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
    val Ember: Color = Color(0xFFFF6A45)
    val EmberDeep: Color = Color(0xFFC93A1E)
    val Mint: Color = Color(0xFF59E0B0)
    val Amber: Color = Color(0xFFF5B759)
    val Ice: Color = Color(0xFF8FD7FF)
    val Danger: Color = Color(0xFFFF6B6B)

    /*
     * الطباعة — تحريرية بلا صراخ. العنوان الكبير وحده يقبل تتبّعًا سالبًا،
     * والبقية تتنفّس بلا أحرف متباعدة ولا uppercase مصطنع.
     */
    val display: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Light,
        fontSize = 40.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.8).sp
    )
    val title: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 26.sp,
        lineHeight = 31.sp,
        letterSpacing = (-0.2).sp
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
        fontSize = 15.sp,
        lineHeight = 22.sp
    )
    val label: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.1.sp
    )
    val eyebrow: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.3.sp
    )
    val numeral: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Light,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp
    )
}

/**
 * لون اللعبة بعد تطويعه لسطح داكن: نرفع سطوعه قليلًا، ونحفظ نسخة شفّافة للتحديد.
 * لا يُستعمل اللون نصًّا على سطح فاتح ولا خلفية لنصّ إلا بعد فحص التباين في الشاشة.
 */
@Immutable
data class MonolithAccent(
    val base: Color,
    val soft: Color,
    val ink: Color
)

fun monolithAccent(source: Color?): MonolithAccent {
    val base = source ?: Monolith.Ember
    val lifted = lerp(base, Color.White, 0.14f)
    return MonolithAccent(
        base = lifted,
        soft = lifted.copy(alpha = 0.12f),
        ink = if (lifted.luminance() > 0.55f) Monolith.Obsidian else Monolith.Bone
    )
}
