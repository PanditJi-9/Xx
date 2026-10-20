/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import nd.max.R
import nd.max.core.audio.AudioSoundPreset
import nd.max.ui.design.MaxCardSpec
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxSize
import nd.max.ui.design.MaxSpace
import nd.max.ui.design.maxRadius

val EQUALIZER_BAND_LABELS = listOf(
    "31Hz", "62Hz", "125Hz", "250Hz", "500Hz", "1k", "2k", "4k", "8k", "16k"
)

/** نطاق الديسيبل للمعادل. الكتابة والرسم يقرآن منه معًا، فالنقطة تتبع الإصبع بلا قفزة. */
private const val EQ_MIN_DB = -15
private const val EQ_MAX_DB = 15

/**
 * تسميات المحور الأفقيّ بلا «Hz» — «500Hz250Hz125Hz» كانت تتلاصق فلا تُقرأ. والوحدة معروفة من سياق المعادل.
 */
private val EQ_AXIS_LABELS = listOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")

/** ارتفاع منطقة الرسم، وعرض عمود الديسيبل على جانبها. الرسم والمحور يقرآن الرقم نفسه فيتطابقان. */
private val EQ_PLOT_HEIGHT = 180.dp
private val EQ_AXIS_WIDTH = 28.dp

/** هامش من حافتي الرسم: يُبقي أقصى نقطة داخل الإطار، ويجعل اللمس عند الحافة ممكنًا. */
private const val EQ_PLOT_PAD = 0.08f

/** نقرتان على النطاق نفسه خلال هذه المدّة (ms) تُعيدانه إلى 0 dB. */
private const val EQ_DOUBLE_TAP_MS = 320L

/** ترتيب الأنماط المعروضة. أسماؤها تأتي من موارد المنصّة المترجمة عبر `presetLabel`. */
private val EQ_PRESET_ORDER = listOf(
    AudioSoundPreset.OFF,
    AudioSoundPreset.BASS,
    AudioSoundPreset.LOUD,
    AudioSoundPreset.GAME,
    AudioSoundPreset.MOVIE,
    AudioSoundPreset.SPEECH,
    AudioSoundPreset.WIDE,
)

/** ديسيبل ← موضع رأسي نسبي (0 = أعلى الرسم). */
private fun eqGainToNorm(gainDb: Int): Float {
    val span = (EQ_MAX_DB - EQ_MIN_DB).toFloat()
    return EQ_PLOT_PAD + ((EQ_MAX_DB - gainDb) / span) * (1f - 2f * EQ_PLOT_PAD)
}

/** موضع رأسي بالبكسل ← ديسيبل صحيح، بالتحويل العكسي نفسه تمامًا. */
private fun eqGainFromY(y: Float, height: Float): Int {
    val norm = (y / height).coerceIn(EQ_PLOT_PAD, 1f - EQ_PLOT_PAD)
    val fraction = (norm - EQ_PLOT_PAD) / (1f - 2f * EQ_PLOT_PAD)
    return (EQ_MAX_DB - fraction * (EQ_MAX_DB - EQ_MIN_DB))
        .roundToInt()
        .coerceIn(EQ_MIN_DB, EQ_MAX_DB)
}

/** ذاكرة النقر المزدوج بين الإيماءات. لا تُعرض، فلا تحتاج حالة Compose. */
private class EqTapMemory {
    var band: Int = -1
    var atMs: Long = 0L
}

/**
 * معادل الترددات بالمنحنى الانسيابي (Cyber Spline Equalizer).
 *
 * - منحنى بيزيه ناعم يربط نطاقات التردد العشرة، مع تعبئة تدرجية متوهجة.
 * - **السحب يُمسك النطاق الذي لُمس أوّلًا**، ويكتب ارتفاعه حيًّا. فلا ينتقل إلى جارٍ إن انحرف الإصبع
 *   أفقيًّا. النقرة الواحدة تضع النطاق عند موضعها، والنقرتان المتتاليتان تعيدانه إلى 0 dB.
 * - [onInteractionChange] يُعلن بداية الإيماءة ونهايتها، فالشاشة الحاوية توقف تمريرها طوالها.
 * - الأنماط تُعرض بأسمائها المترجمة عبر [presetLabel].
 */
@Composable
fun CyberCurveEqualizer(
    bands: List<Int>,
    enabled: Boolean,
    onBandChange: (bandIndex: Int, gainDb: Int) -> Unit,
    activePreset: AudioSoundPreset,
    onPresetSelect: (AudioSoundPreset) -> Unit,
    presetLabel: @Composable (AudioSoundPreset) -> String,
    onInteractionChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var activeBandIndex by remember { mutableIntStateOf(-1) }
    val tapMemory = remember { EqTapMemory() }
    // مراجع حيّة: الإيماءة مرتبطة بـ`pointerInput(enabled)` وحدها، فلا تُعاد تشغيلها مع كل خطوة dB.
    // إعادة تشغيلها في منتصف السحب هي ما كان يُفلت السحب، ويترك التمرير يأخذه.
    val latestBands by rememberUpdatedState(bands)
    val latestOnBandChange by rememberUpdatedState(onBandChange)
    val latestOnInteraction by rememberUpdatedState(onInteractionChange)
    val haptic = LocalHapticFeedback.current
    val tapSlopPx = with(LocalDensity.current) { MaxSpace.md.toPx() }

    // ألوان التمييز من مخطّط Material 3 النشط في الإعدادات، لا أحمر ثابت
    val (accent, accentDeep, accentCool) = cyberPalette()
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(maxRadius(MaxCardSpec.radius)))
            .border(
                MaxSize.hairlineBorder,
                if (enabled) accent.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                RoundedCornerShape(maxRadius(MaxCardSpec.radius)),
            ),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = MaxSpace.md, horizontal = MaxSpace.sm),
        ) {
            // شريط الأنماط الصوتية السريعة
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaxSpace.xs),
            ) {
                items(EQ_PRESET_ORDER) { preset ->
                    val isSelected = activePreset == preset
                    Surface(
                        onClick = { onPresetSelect(preset) },
                        shape = RoundedCornerShape(maxRadius(MaxRadius.chip)),
                        color = if (isSelected) accent.copy(alpha = 0.22f)
                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            // مساحة اللمس لا تنزل عن الحدّ المعلن لكل عنصر قابل للنقر.
                            .defaultMinSize(minHeight = MaxSize.minTouchTarget)
                            .border(
                                MaxSize.hairlineBorder,
                                if (isSelected) accent else Color.Transparent,
                                RoundedCornerShape(maxRadius(MaxRadius.chip)),
                            ),
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = MaxSpace.sm),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = presetLabel(preset),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) accent else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(MaxSpace.md))

            // **المحور الترددي LTR دائمًا** حتى في الواجهة العربيّة: الرسم يضع النطاق ٠ (31Hz) عند اليسار
            // ويحسب اللمس من اليسار، بينما صفّ التسميات كان ينعكس في RTL فيضع «31Hz» عند اليمين — فتقع
            // التسمية تحت نقطةٍ ليست لها. ومن منظور المعادلات: الترددات المنخفضة يسارًا في كل لغة.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth().height(EQ_PLOT_HEIGHT)) {
            EqDbAxis(modifier = Modifier.width(EQ_AXIS_WIDTH).fillMaxHeight())
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(maxRadius(MaxRadius.tile)))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    // المفتاح `enabled` وحده: أي مفتاح يتغيّر مع القيم كان يُعيد تشغيل الإيماءة في
                    // منتصف السحب، فينقطع السحب ويأخذه التمرير.
                    .pointerInput(enabled) {
                        if (!enabled) return@pointerInput
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val plotHeight = size.height.toFloat()
                            val slotWidth = size.width.toFloat() / EQUALIZER_BAND_LABELS.size
                            // النطاق يُحسم عند اللمس الأوّل، ولا يتبدّل حتى رفع الإصبع.
                            val band = (down.position.x / slotWidth).toInt()
                                .coerceIn(0, EQUALIZER_BAND_LABELS.lastIndex)
                            var lastGain = latestBands.getOrElse(band) { 0 }
                            var moved = false

                            fun writeGain(y: Float) {
                                val gain = eqGainFromY(y, plotHeight)
                                if (gain == lastGain) return
                                // عبور 0 dB يُحسّ باهتزازة خفيفة، فموضع الاستواء يُعرف باللمس لا بالنظر.
                                if ((gain == 0) != (lastGain == 0)) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                lastGain = gain
                                latestOnBandChange(band, gain)
                            }

                            activeBandIndex = band
                            latestOnInteraction(true)
                            try {
                                writeGain(down.position.y)
                                down.consume()

                                var pressed = true
                                while (pressed) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    pressed = change.pressed
                                    if (pressed) {
                                        change.consume()
                                        if ((change.position - down.position).getDistance() > tapSlopPx) {
                                            moved = true
                                        }
                                        writeGain(change.position.y)
                                    }
                                }

                                if (!moved) {
                                    val isDoubleTap = band == tapMemory.band &&
                                        down.uptimeMillis - tapMemory.atMs < EQ_DOUBLE_TAP_MS
                                    if (isDoubleTap) {
                                        tapMemory.band = -1
                                        if (lastGain != 0) {
                                            lastGain = 0
                                            latestOnBandChange(band, 0)
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    } else {
                                        tapMemory.band = band
                                        tapMemory.atMs = down.uptimeMillis
                                    }
                                }
                            } finally {
                                activeBandIndex = -1
                                latestOnInteraction(false)
                            }
                        }
                    },
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val hairline = MaxSize.hairlineBorder.toPx()

                    // خطوط الشبكة الأفقية للديسيبل. الخط الأوسط (0 dB) أثقل.
                    listOf(
                        eqGainToNorm(EQ_MAX_DB),
                        eqGainToNorm(EQ_MAX_DB / 2),
                        0.5f,
                        eqGainToNorm(EQ_MIN_DB / 2),
                        eqGainToNorm(EQ_MIN_DB),
                    ).forEach { step ->
                        val y = h * step
                        drawLine(
                            color = if (step == 0.5f) gridColor.copy(alpha = 0.4f) else gridColor,
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = if (step == 0.5f) hairline * 2f else hairline,
                        )
                    }

                    // مواقع نقاط التردد العشر، بالتحويل نفسه الذي تُكتب به الإيماءة
                    val slotWidth = w / EQUALIZER_BAND_LABELS.size
                    val points = List(EQUALIZER_BAND_LABELS.size) { i ->
                        val gainDb = bands.getOrElse(i) { 0 }
                        Offset(
                            x = (i * slotWidth) + (slotWidth / 2f),
                            y = h * eqGainToNorm(gainDb),
                        )
                    }

                    // بناء منحنى بيزيه الانسيابي (Cubic Bezier Spline)
                    val curvePath = Path().apply {
                        moveTo(points[0].x, points[0].y)
                        for (i in 0 until points.size - 1) {
                            val p0 = points[i]
                            val p1 = points[i + 1]
                            val controlX = (p0.x + p1.x) / 2f
                            cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
                        }
                    }

                    // مسار التعبئة المضيئة أسفل المنحنى
                    val fillPath = Path().apply {
                        addPath(curvePath)
                        lineTo(points.last().x, h)
                        lineTo(points.first().x, h)
                        close()
                    }

                    if (enabled) {
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                listOf(
                                    accent.copy(alpha = 0.32f),
                                    accentDeep.copy(alpha = 0.08f),
                                    Color.Transparent,
                                ),
                                startY = 0f,
                                endY = h,
                            ),
                        )
                    }

                    // خط المنحنى النيوني البارز
                    drawPath(
                        path = curvePath,
                        brush = if (enabled) {
                            Brush.horizontalGradient(listOf(accent, accentDeep, accentCool))
                        } else {
                            Brush.linearGradient(listOf(Color.Gray.copy(alpha = 0.4f), Color.Gray.copy(alpha = 0.4f)))
                        },
                        style = Stroke(
                            width = if (enabled) (MaxSize.hairlineBorder * 3f).toPx() else (MaxSize.hairlineBorder * 2f).toPx(),
                            cap = StrokeCap.Round,
                        ),
                    )

                    // نقاط التحكم العشر. الحجم بالكثافة لا بالبكسل الثابت، فتبقى لمسًا واضحًا على كل شاشة.
                    points.forEachIndexed { i, pt ->
                        val isTouched = i == activeBandIndex
                        val nodeColor = if (enabled) (if (isTouched) accentCool else accent) else Color.Gray

                        // هالة خارجية عند لمس النقطة
                        if (isTouched && enabled) {
                            drawCircle(
                                color = accent.copy(alpha = 0.45f),
                                radius = MaxSpace.lg.toPx(),
                                center = pt,
                            )
                        }

                        // النقطة المركزية
                        drawCircle(
                            color = nodeColor,
                            radius = if (isTouched) MaxSpace.sm.toPx() else MaxSpace.xs.toPx(),
                            center = pt,
                        )
                    }
                }

                // قيمة النطاق الذي تحت الإصبع
                if (activeBandIndex in EQUALIZER_BAND_LABELS.indices && enabled) {
                    val gain = bands.getOrElse(activeBandIndex) { 0 }
                    val label = if (gain > 0) "+$gain dB" else "$gain dB"
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = MaxSpace.xs)
                            .background(accent.copy(alpha = 0.85f), RoundedCornerShape(MaxRadius.chip))
                            .padding(horizontal = MaxSpace.sm, vertical = MaxSpace.hairline),
                    ) {
                        Text(
                            text = "${EQUALIZER_BAND_LABELS[activeBandIndex]}: $label",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }
            } // نهاية صفّ الرسم

            Spacer(Modifier.height(MaxSpace.xs))

            // تسميات الترددات أسفل المعادل — بعد عمود الديسيبل بعرضه ليتحاذى كل اسمٍ مع نقطته
            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(Modifier.width(EQ_AXIS_WIDTH))
                EQUALIZER_BAND_LABELS.forEachIndexed { idx, label ->
                    val isTouched = idx == activeBandIndex
                    Text(
                        text = EQ_AXIS_LABELS.getOrElse(idx) { label },
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = if (isTouched) FontWeight.Bold else FontWeight.Medium,
                        color = if (isTouched && enabled) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            } // Column
            } // CompositionLocalProvider (LTR)

            Spacer(Modifier.height(MaxSpace.sm))

            // تلميح الإيماءة: الاكتشاف لا يجب أن يعتمد على التخمين
            Text(
                text = stringResource(R.string.audio_eq_gesture_hint),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * عمود الديسيبل: «+15 / 0 / −15» عند المواضع نفسها التي تُرسم بها النقاط ([eqGainToNorm]) — فمن يرى النقطة
 * يعرف ارتفاعها من غير أن ينتظر اللمس ليُظهر الشارةَ العائمة.
 */
@Composable
private fun EqDbAxis(modifier: Modifier = Modifier) {
    val ink = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
    Box(modifier = modifier) {
        listOf(EQ_MAX_DB, 0, EQ_MIN_DB).forEach { db ->
            Text(
                text = if (db > 0) "+$db" else "$db",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = ink,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(y = EQ_PLOT_HEIGHT * eqGainToNorm(db) - 6.dp)
                    .padding(end = MaxSpace.xs),
            )
        }
    }
}
