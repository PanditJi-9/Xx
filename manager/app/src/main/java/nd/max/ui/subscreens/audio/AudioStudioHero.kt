/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
/*
 * استوديو الصوت — **بطاقة البطل**: أوّل ما يلقاه القارئ، وما تُقال فيه حقيقةٌ واحدة كلّ مرّة.
 *
 * ───────────────────────── Attribution (Apache-2.0) ─────────────────────────
 * The hero-card layout (a gradient band over the screen's own readings, with a state capsule on
 * it) is adapted from the **DolbyUI** interface — branch `rodin` of
 * `Digimend-X-Rodin/packages_apps_DolbyUI` (a fork of `swiitch-OFF-Lab/packages_apps_DolbyUI`),
 * licensed **Apache-2.0** as stated in its own file headers, and used with the copyright holder's
 * permission. This is a rework, not a copy — and the banner here draws measured levels only,
 * where the source drew a fixed decorative shape; the credit is in `docs/PROVENANCE.md`.
 *
 * ────────────────────── لماذا وُجدت (الشكوى التي وُلدت منها) ──────────────────────
 * الشاشة كانت تبدأ بعنوان قسم ورقمين، ثمّ بشريط تبويبات داخل التمرير، ثمّ قائمة. فما يراه المستخدم
 * أوّلًا **لا يقول شيئًا عن الصوت**: لا شكل، ولا حالة، ولا قراءة حيّة — ويحتاج أن يقرأ سطرين ليصل
 * إلى ما جاء من أجله. والبطاقة هنا تفعل العكس: تُعرض **حالة الجهارة كاملة في شاشة واحدة** (شكلٌ من
 * قراءةٍ حقيقيّة · الجهاز النشط وحالته · حالة الالتقاط)، ثمّ يقرّر القارئ أيّ تبويب
 * يريد قبل أن يمرّر شيئًا.
 *
 * ────────────────────── وما تُعرض فيه مُقاس لا مُتخيَّل ──────────────────────
 * الشريط يُرسم من **ثلاث قراءات مختلفة، ولا رابعة**: (١) **الطيف الحيّ** إن كان الالتقاط جاريًا —
 * وهو المقروء فعلًا من `AudioSpectrumFrame.bands`؛ (٢) **المنحنى المستهدف** إن لم يكن، وهو ما
 * طلبناه من المعادل (`eqCurveNodesOf`) لا استجابةً مقيَّلة — والوسم يقول أيّهما يُرى فلا يُقرأ
 * أحدهما مكان الآخر؛ (٣) **قاعدةٌ ساكنة** حين لا قراءة أصلًا. **ولا شكل رابع مصنوع** (وهو ما تفعله
 * واجهة المصدر: شكلٌ ثابت يُرسم دائمًا فيُقرأ قراءةً وهو ليس منها).
 *
 * والرسم هنا **للعرض لا للتحرير**: لا كتابة من هذه البطاقة. الطيفُ يُشغَّل من قسمه في «النظام» لأن
 * طلب إذن التسجيل يجب أن يقع بلمسٍ صريح داخل قسمه بشرحه (أمر المالك) — فالبطاقة **تُرشد** إلى هناك
 * بسطرٍ صريح، ولا تزرع مفتاحًا ثانيًا لشيءٍ واحد.
 */
/*
 * ───────────── إعادة تصميم كاملة (تبويب «مستويات») ─────────────
 * اللقطة القديمة: نطاقٌ تدرّجيٌّ ارتفاعه ١٤٨dp **فارغٌ إلا خطًّا مستويًا** (المنحنى المستهدف حين تكون كل
 * النطاقات صفرًا)، فيبدو كأنّ الشاشة معطوبة؛ ثم اسم الجهاز بلا أيّ رمز؛ ثم «شغّل الالتقاط…» **سطرٌ يشبه
 * العنوان** لا زرًّا. والآن:
 *  ١) رأسٌ مضغوط: دائرة رمز الجهاز (سمّاعة/مكبّر) + اسمه + وسم الحالة.
 *  ٢) **لوحة الإشارة لا تظهر إلا حين يكون فيها شيءٌ يُرى**: طيفٌ حيّ مقروء، أو منحنى مستهدفٌ غير مستوٍ.
 *     ولا خطّ مستويًا فارغًا يُرسم (ولا شكلٌ مصنوع — قاعدة البطاقة الأصليّة باقية: مقيسٌ لا مُتخيَّل).
 *  ٣) الإرشاد إلى الالتقاط **زرٌّ حقيقيّ** بسهمٍ ينعكس مع الاتّجاه، ويختفي حين يعمل الالتقاط.
 */
package nd.max.ui.subscreens.audio

import android.media.AudioDeviceInfo
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import nd.max.R
import nd.max.core.audio.AudioEqSnapshot
import nd.max.core.audio.EqCurveNode
import nd.max.core.audio.eqCurveNodesOf
import nd.max.ui.design.MaxAlpha
import nd.max.ui.design.MaxCapsule
import nd.max.ui.design.MaxCardShell
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxSize
import nd.max.ui.design.MaxSpace
import nd.max.ui.design.MaxTone
import nd.max.ui.design.MaxWaveformBanner
import nd.max.ui.viewmodel.AudioStudioUiState
import nd.max.ui.design.maxRadius

/** ارتفاع لوحة الإشارة — رقمٌ واحد، فلا يقفز الشكل بين الطيف والمنحنى. */
private val signalPanelHeight = 92.dp

/**
 * بطاقة المخرج.
 *
 * @param onCaptureHint يُنادى حين يلمس المستخدم زرّ «الالتقاط في تبويب النظام» — والانتقال شأن الشاشة.
 */
@Composable
internal fun AudioHeroCard(
    state: AudioStudioUiState,
    onCaptureHint: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val capture = state.spectrumRunning
    val device = state.activeDevice

    val bands = state.spectrum?.bands.orEmpty()
    val liveSpectrum = capture && bands.isNotEmpty()
    val nodes = remember(state.eq) { eqNodesOf(state.eq) }
    // منحنى كلّ نقاطه صفر ليس «شكلًا» — خطٌّ مستوٍ يوحي بالعطب، فلا يُرسم.
    val shapedCurve = nodes.size >= 2 && nodes.any { it.y != 0f }

    MaxCardShell(
        contentPadding = MaxSpace.lg,
        verticalArrangement = Arrangement.spacedBy(MaxSpace.md),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.md),
        ) {
            val tone = if (device != null) MaxTone.Accent else MaxTone.Inactive
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (device != null) scheme.primaryContainer else scheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = deviceIconOf(device?.typeCode),
                    contentDescription = null,
                    tint = if (device != null) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
                    modifier = Modifier.size(MaxSize.iconGlyph + MaxSpace.xs),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(MaxSpace.hairline),
            ) {
                Text(
                    text = device?.productName ?: stringResource(R.string.max_audio_engine_no_device_desc),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.max_audio_engine_device),
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                )
            }

            MaxCapsule(
                text = stringResource(
                    when {
                        capture -> R.string.max_audio_spectrum_live
                        device != null -> R.string.max_audio_engine_live
                        else -> R.string.max_audio_engine_no_device
                    },
                ),
                tone = when {
                    capture -> MaxTone.Accent
                    device != null -> MaxTone.Positive
                    else -> tone
                },
            )
        }

        if (liveSpectrum || shapedCurve) {
            Column(verticalArrangement = Arrangement.spacedBy(MaxSpace.xs)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(signalPanelHeight)
                        .clip(RoundedCornerShape(maxRadius(MaxRadius.tile)))
                        .background(scheme.surfaceContainerHigh),
                ) {
                    if (liveSpectrum) {
                        MaxWaveformBanner(
                            levels = bands,
                            live = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(signalPanelHeight)
                                .padding(horizontal = MaxSpace.md, vertical = MaxSpace.sm),
                        )
                    } else {
                        CurveSketch(
                            nodes = nodes,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(signalPanelHeight)
                                .padding(horizontal = MaxSpace.md, vertical = MaxSpace.sm),
                        )
                    }
                }
                // الوسم يقول **ما يُرى بالضبط**: طيفٌ مقروء الآن، أو ما طلبناه من المعادل.
                Text(
                    text = stringResource(
                        if (liveSpectrum) R.string.max_audio_spectrum_live else R.string.max_audio_eq_curve_title,
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = MaxSpace.xs),
                )
            }
        }

        // والزرّ يظهر **حين يكون ذا معنى فقط**: مع التقاطٍ جارٍ لا يحتاج المستخدم أن يُدَلّ عليه.
        if (!capture) {
            CaptureAction(onClick = onCaptureHint)
        }
    }
}

/** الإرشاد إلى الالتقاط الحيّ — زرٌّ بسهمٍ ينعكس في RTL (يشير إلى اليسار في العربيّة). */
@Composable
private fun CaptureAction(onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(maxRadius(MaxRadius.row)),
        color = scheme.secondaryContainer.copy(alpha = 0.55f),
        contentColor = scheme.onSecondaryContainer,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(MaxSize.minTouchTarget + MaxSpace.xs)
                .padding(horizontal = MaxSpace.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.md),
        ) {
            Icon(
                imageVector = Icons.Rounded.Waves,
                contentDescription = null,
                modifier = Modifier.size(MaxSize.iconGlyph),
            )
            Text(
                text = stringResource(R.string.max_audio_hero_capture_hint),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(MaxSize.iconGlyph),
            )
        }
    }
}

/** منحنى مستهدف صغير بتنعيم بيزيه — للعرض لا للتحرير (التحرير في تبويب المعادل). */
@Composable
private fun CurveSketch(nodes: List<EqCurveNode>, modifier: Modifier = Modifier) {
    val ink = MaterialTheme.colorScheme.primary
    val inkEnd = MaterialTheme.colorScheme.tertiary
    val baseline = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val centerY = h / 2f
        drawLine(
            color = baseline.copy(alpha = MaxAlpha.borderStrong),
            start = Offset(0f, centerY),
            end = Offset(w, centerY),
            strokeWidth = MaxSize.hairlineBorder.toPx(),
        )
        if (nodes.size < 2) return@Canvas
        val points = nodes.map { node ->
            Offset(
                x = node.x.coerceIn(0f, 1f) * w,
                y = centerY - node.y.coerceIn(-1f, 1f) * centerY * 0.8f,
            )
        }
        val curve = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 0 until points.lastIndex) {
                val a = points[i]
                val b = points[i + 1]
                val midX = (a.x + b.x) / 2f
                cubicTo(midX, a.y, midX, b.y, b.x, b.y)
            }
        }
        val area = Path().apply {
            addPath(curve)
            lineTo(points.last().x, centerY)
            lineTo(points.first().x, centerY)
            close()
        }
        drawPath(path = area, color = ink.copy(alpha = 0.16f))
        drawPath(
            path = curve,
            brush = Brush.horizontalGradient(listOf(ink, inkEnd)),
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

/** سمّاعة للأجهزة الشخصيّة (سلكيّة/بلوتوث/USB)، ومكبّر لما سواها. */
private fun deviceIconOf(typeCode: Int?) = when (typeCode) {
    AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
    AudioDeviceInfo.TYPE_WIRED_HEADSET,
    AudioDeviceInfo.TYPE_USB_HEADSET,
    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
    AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
    -> Icons.Rounded.Headphones

    else -> Icons.Rounded.Speaker
}

/**
 * نقاط المنحنى المستهدف من لقطة المعادل — **والقياس على JVM** (`eqCurveNodesOf`)، فلا حساب في
 * طبقة الواجهة ولا مقياس مصنوع هنا.
 */
private fun eqNodesOf(snapshot: AudioEqSnapshot?): List<EqCurveNode> =
    snapshot?.bands?.let { eqCurveNodesOf(it) } ?: emptyList()
