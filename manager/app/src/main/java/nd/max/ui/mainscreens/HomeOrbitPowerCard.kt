/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.ui.mainscreens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import nd.max.R
import nd.max.ui.component.MaxEase
import nd.max.ui.component.MaxOrbitScene
import nd.max.ui.component.NeuralPanel
import nd.max.ui.component.NeuralPill
import nd.max.ui.component.NeuralValue
import nd.max.ui.component.neuralPalette
import nd.max.ui.design.MaxDuration
import nd.max.ui.design.MaxSpace
import nd.max.ui.theme.MonoFontFamily
import nd.max.ui.viewmodel.DashboardState

private val OrbitCardHeight = 148.dp

/**
 * «مدار الطاقة»: مشهد حيّ يظهر في الرئيسية **أثناء الشحن فقط**.
 *
 * **لماذا مشروط بالشحن لا دائم.** الرئيسية مصمَّمة بعناية (بطل ← نبض ← أفعال ← حيويات)، وإضافة بطاقة
 * ثابتة تغيّر إيقاعها لكل مستخدم في كل مرة. أمّا هنا فالحدث نفسه — توصيل الشاحن — هو ما يستحقّ
 * مشهدًا: يظهر حين يُوصَل، ويغيب حين يُفصل، وفي الوضع العادي لا شيء تغيّر. وهكذا أيضًا تبقى كلفة
 * الحركة المستمرّة محصورة في اللحظات التي يكون الجهاز فيها موصولًا بالطاقة أصلًا.
 *
 * بلا نصوص جديدة: «الشحن» من مورد موجود، والنسبة والواط أرقام. فلا ترجمة تُضاف لخمسٍ وثمانين لغة.
 */
@Composable
internal fun HomeOrbitPowerCard(dashboard: DashboardState, modifier: Modifier = Modifier) {
    val p = neuralPalette()
    val battery = dashboard.batteryPercent.coerceIn(0, 100)
    val power = dashboard.powerWatt.takeIf { it > 0.05f }
    // النص في الجهة المنطقية «البداية»، فالمشهد يُزاح إلى الجهة المقابلة في العربية حتى لا يقع الرقم فوق المدار.
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    // المركز كما كان (٠٫٣٦/٠٫٦٤). المعالجة في حجم المدار لا موضعه: انظر MaxOrbitScene.
    val sceneCenter = if (rtl) 0.36f else 0.64f

    // يُوصَل الشاحن ← تنفتح البطاقة من حجم صفر بدل أن تدفع ما تحتها دفعةً واحدة.
    val appeared = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = appeared,
        modifier = modifier,
        enter = fadeIn(tween(MaxDuration.standard, easing = MaxEase.EmphasizedDecelerate)) +
            expandVertically(tween(MaxDuration.standard, easing = MaxEase.Emphasized)),
    ) {
        NeuralPanel(accent = p.accent, contentPadding = PaddingValues(0.dp)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(OrbitCardHeight)
                    .semantics(mergeDescendants = true) {}
            ) {
                // اللون الرئيسي للمشهد هو لون الشاشة (primary) لا secondary: الأخير رماديّ باهت في
                // الثيم الافتراضي فكان القوس والنواة بلا حياة. والقمر والحبّة بلون tertiary.
                // وحين لا يصل واط (شحن موقوف/تمرير) يهدأ المشهد: لا نبضات ولا حلقات ولا صاعقة.
                MaxOrbitScene(
                    fraction = battery / 100f,
                    accent = p.accent,
                    accentAlt = p.accentAlt,
                    modifier = Modifier.matchParentSize(),
                    flowing = power != null,
                    centerX = sceneCenter,
                    starTint = p.text,
                )
                Column(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(MaxSpace.lg),
                    verticalArrangement = Arrangement.spacedBy(MaxSpace.xs),
                ) {
                    Text(
                        stringResource(R.string.charging_status_charging).uppercase(),
                        color = p.accent,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                    )
                    NeuralValue(
                        "$battery%",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontFamily = MonoFontFamily,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = p.text,
                    )
                }
                // الحبّة في الزاوية العلوية «الفيزيائية» اليسرى دائمًا: رأس القوس يصل الزاوية العلوية اليمنى
                // من نسبة ٢١٪ فصاعدًا فيتراكب معها، أمّا الزاوية اليسرى فلا يمرّ بها المدار في أي نسبة.
                // في العربية تُقلب المحاذاة منطقيًّا فتبقى الحبّة في الزاوية الفيزيائية اليسرى نفسها.
                if (power != null) {
                    // «3.9 W» رمز تقني لاتيني: في سياق RTL كان الرقم والوحدة ينعكسان فيظهر «W 3.9»
                    // (لقطة الرئيسية). `NeuralValue` يثبّت LTR لهذا السبب نفسه؛ والحبّة لا تفعل،
                    // فتُثبَّت هنا. والمحاذاة `align` تُحَلّ من اتجاه الأب فلا تتحرّك الحبّة عن زاويتها.
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        NeuralPill(
                            text = "${power.oneDecimal()} W",
                            accent = p.accentAlt,
                            filled = false,
                            dot = false,
                            modifier = Modifier
                                .align(if (rtl) Alignment.TopEnd else Alignment.TopStart)
                                .padding(MaxSpace.lg),
                        )
                    }
                }
            }
        }
    }
}
