/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */
/*
 * الجزيرة الديناميكية — **قطع البطاقة**: الشعار، والنبضة الصوتية، والبطارية.
 *
 * ### ولماذا في ملفّ واحد
 *
 * هذه الثلاثة ليست زخرفة متفرّقة بل **العناصر التي تجعل الكبسولة تُقرأ بطاقةً من رومات
 * المصانع** كما طلب المالك، وتشترك في صفتين: كلّها تُرسم داخل **كبسولة سوداء** بارتفاع لا
 * يزيد على بضعة عشرات الديبنسات، وكلّها تحتاج لونًا واحدًا («الحبر») يتغيّر بحسب خلفية
 * الكبسولة. فملفّ واحد يجمعها، والقاعدة فيه واحدة: **الحبر يُمرَّر ولا يُخترع**، فمن اختار
 * كبسولة سوداء يرث حبرًا أبيض تلقائيًّا في القطع الثلاث كلها.
 *
 * ### والشعار يُقرأ ولا يُختلق
 *
 * شعار التطبيق يأتي من `PackageManager` بالحزمة التي مرّرها المستمع (`IslandNotice.packageName`).
 * وما لم تُقرأ حزمته (حجب رؤية الحزم) يسقط إلى **حرف الاسم الأول** — وهو سقوط صريح لا فراغ،
 * ولا يُرسم شعار عام لجميع التطبيقات: شعارٌ واحد لكل الإشعارات يكذب في كل واحدة.
 */
package nd.max.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxSize
import nd.max.ui.design.MaxSpace
import kotlin.math.roundToInt
import nd.max.ui.design.maxRadius

/**
 * شعار التطبيق بحزمته — يُقرأ على خيط الخلفية، و`null` سقوط صريح لا خطأ.
 *
 * **ولماذا `produceState` لا `remember`:** قراءة الشعار عمل نظام (I/O) يجب ألّا يقع داخل
 * التركيب، والمفتاح هو الحزمة وحدها: إشعاران من التطبيق نفسه لا يعيدان قراءة الشعار.
 */
@Composable
fun rememberIslandAppIcon(packageName: String?): ImageBitmap? {
    val context = LocalContext.current
    val sizePx = with(LocalDensity.current) { MaxSize.iconGlyphSmall.toPx().roundToInt() }
        .coerceAtLeast(1)
    return produceState<ImageBitmap?>(initialValue = null, packageName, sizePx) {
        value = if (packageName.isNullOrBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    context.packageManager.getApplicationIcon(packageName)
                        .toBitmap(width = sizePx, height = sizePx)
                        .asImageBitmap()
                }.getOrNull()
            }
        }
    }.value
}

/**
 * شعار التطبيق في مربّع صغير — أو **حرف الاسم الأول** حين لا يُقرأ الشعار.
 *
 * والقصد هو ما تفعله رومات المصانع: أيقونة صغيرة بجانب نصّ الإشعار، لا نقطة ملوّنة عامة.
 */
@Composable
fun IslandAppBadge(
    packageName: String?,
    label: String?,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val icon = rememberIslandAppIcon(packageName)
    val initial = remember(label) { label?.trim()?.take(1)?.uppercase().orEmpty() }
    Box(
        modifier = modifier
            .size(MaxSize.iconGlyph)
            .clip(RoundedCornerShape(maxRadius(MaxRadius.chip)))
            .background(tint.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(MaxSize.iconGlyph)
            )
        } else if (initial.isNotEmpty()) {
            Text(
                text = initial,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = tint
            )
        }
    }
}

/**
 * نبضة صوتية من ثلاثة أعمدة — **تُحرَّك ما دام يُشغَّل، وتُسكَّن بلا كذب**.
 *
 * العمود المتحرّك يقول «يُشغَّل الآن»، وهو معلومة صادقة لأن المصدر نفسه لا يعرض جلسة
 * موقوفة (`IslandLive.publishMedia` تُسقط ما ليس نشطًا). ومن أطفأ الحركة يرى الأعمدة ساكنة
 * لا محذوفة: الشكل نفسه بلا ادّعاء زمن.
 */
@Composable
fun IslandEqualizer(tint: Color, animated: Boolean, modifier: Modifier = Modifier) {
    // نسب الأعمدة ثابتة الشكل — والحركة تُنوّع الارتفاع حولها بالدور، فلا تبدو الأعمدة كتلة واحدة.
    val fractions = listOf(0.45f, 1f, 0.68f)
    val heights: List<Float> = if (animated) {
        val transition = rememberInfiniteTransition(label = "islandEqualizer")
        fractions.mapIndexed { index, base ->
            transition.animateFloat(
                initialValue = base * 0.35f,
                targetValue = base,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 380 + index * 110, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "islandBar$index"
            ).value
        }
    } else {
        fractions
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.hairline)
    ) {
        heights.forEach { fraction ->
            Box(
                modifier = Modifier
                    .width(MaxSpace.hairline)
                    .height(MaxSpace.md * fraction)
                    .clip(RoundedCornerShape(MaxSpace.hairline))
                    .background(tint)
            )
        }
    }
}

/**
 * بطارية صغيرة بنسبة حقيقية — تُرسم بحجم ثابت داخل الكبسولة.
 *
 * والنسبة تُحرَّك (٠٫٣٢s) عند التغيير بدل أن تقفز، لأن النسبة تتحدّث من مستمع البطارية بينما
 * الكبسولة تُعرض؛ والقفزة المفاجئة في عنصر صغير تُقرأ عطبًا.
 */
@Composable
fun IslandBattery(percent: Int, tint: Color, animated: Boolean, modifier: Modifier = Modifier) {
    val target = (percent.coerceIn(0, 100) / 100f)
    val level = if (animated) {
        animateFloatAsState(targetValue = target, animationSpec = tween(320), label = "islandBattery").value
    } else {
        target
    }
    val body = tint.copy(alpha = 0.38f)
    Canvas(modifier = modifier.size(width = MaxSpace.xxl, height = MaxSpace.md)) {
        val stroke = size.height * 0.10f
        val tipW = size.width * 0.08f
        val bodyW = size.width - tipW - stroke
        val radius = CornerRadius(size.height * 0.30f, size.height * 0.30f)
        // الجسم: إطار شفّاف يُظهر الامتلاء حتى ١٠٠٪ داخله.
        drawRoundRect(
            color = body,
            size = Size(bodyW, size.height),
            cornerRadius = radius,
            style = Stroke(width = stroke)
        )
        val innerW = (bodyW - stroke * 2f).coerceAtLeast(0f) * level
        if (innerW > 0f) {
            drawRoundRect(
                color = tint,
                topLeft = Offset(stroke, stroke),
                size = Size(innerW, size.height - stroke * 2f),
                cornerRadius = CornerRadius(size.height * 0.18f, size.height * 0.18f)
            )
        }
        // الطرف: مستطيل صغير خارج الجسم كطرف بطارية حقيقي.
        drawRoundRect(
            color = body,
            topLeft = Offset(bodyW + stroke, size.height * 0.32f),
            size = Size(tipW, size.height * 0.36f),
            cornerRadius = CornerRadius(tipW / 2f, tipW / 2f)
        )
    }
}
