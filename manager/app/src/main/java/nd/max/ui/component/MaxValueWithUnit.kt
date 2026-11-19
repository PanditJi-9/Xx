/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection

/**
 * رقم ووحدته على **سطر نصّي واحد** — لا `Row` يحاذي الأسفل.
 *
 * العلّة التي وُلدت لها (تدقيق فيديو المالك ٢٠٢٦-١١-١٨): `Row(verticalAlignment = Bottom)` بين
 * `NeuralValue` الكبير و`Text` الوحدة الصغيرة يجعل الوحدة تجلس على **أسفل الصندوق** لا على خطّ
 * الأساس، فتظهر `°C` منزلقة إلى أسفل الرقم — خصوصًا حين يكون الرقم نفسه متحرّكًا (`MaxRollingTicker`
 * يرسم `Row` من خانات، فلا يملك خطّ أساس واحدًا يُحاذى إليه أصلًا). الحلّ الصحيح طباعيّ لا تخطيطيّ:
 * الرقم والوحدة نصّ واحد بنمطين.
 *
 * وثاني علّة: قيمة لاتينية وسط جملة عربية تُعاد ترتيبها (يُقرأ `C° 44`). العزل هنا
 * `LocalLayoutDirection.Ltr` — نفس ما يفعله `NeuralValue` — لأن محتوى هذه الأداة **رمز وحدة**
 * (`°C`, `%`, `W`, `GHz`) لا كلمة عربية: الرمز يُقرأ من اليسار في كل لغة.
 *
 * **لا تُستخدم لترجمة وحدةٍ عربية** (مثل «غيغابايت»): العزل يقلب ترتيبها. تلك تُكتب جملةً عادية.
 */
@Composable
fun MaxValueWithUnit(
    value: String,
    unit: String?,
    style: TextStyle,
    color: Color,
    unitColor: Color,
    modifier: Modifier = Modifier,
    unitStyle: TextStyle = MaterialTheme.typography.labelLarge,
    textAlign: TextAlign? = null,
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Text(
            text = buildAnnotatedString {
                append(value)
                if (!unit.isNullOrBlank()) {
                    // مسافة غير كاسرة ورفيعة: لا ينفصل الرمز عن رقمه عند أي قياس.
                    append('\u00A0')
                    withStyle(
                        SpanStyle(
                            fontSize = unitStyle.fontSize,
                            fontWeight = unitStyle.fontWeight,
                            letterSpacing = unitStyle.letterSpacing,
                            fontFamily = unitStyle.fontFamily,
                            color = unitColor,
                        )
                    ) { append(unit) }
                }
            },
            modifier = modifier,
            style = style,
            color = color,
            maxLines = 1,
            textAlign = textAlign,
        )
    }
}

/**
 * نفس المعالجة لمن لا يستطيع تمرير مكوّن (نصّ جاهز يُبنى في طبقة عرض أو نموذج):
 * يعزل «رقم + وحدة لاتينية» ككتلة واحدة داخل فقرة عربية.
 *
 * العلّة مقيسة في تسجيل المالك (تبويب «الشاشة»، ٢٠٢٦-١١-١٩): بطاقة الملخّص رسمت
 * `in 5.72` و`dpi 520` بينما الصفوف التفصيلية رسمت `5.72 in` و`520 dpi` — الفرق أن
 * الأخيرة تمرّ عبر `MaxMetricLine` الذي يعزل بـ`\u2066…\u2069`. القاعدة في Unicode:
 * الفاصلة بين رقم ولاتينيّ داخل فقرة RTL تُحلّ إلى اتجاه الفقرة، فينتقل الرقم إلى
 * يمين الوحدة. العزل LRI/PDI يثبّت الكتلة كما كُتبت.
 */
fun maxIsolatedValueUnit(value: String, unit: String?): String =
    if (unit.isNullOrBlank()) value else "\u2066$value $unit\u2069"
