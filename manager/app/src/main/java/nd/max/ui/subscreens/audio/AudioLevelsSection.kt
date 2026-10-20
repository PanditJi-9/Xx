/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
/*
 * استوديو الصوت — **بطاقة مستويات الدفقات** (إعادة كتابة كاملة لتبويب «مستويات»).
 *
 * ───────────── ما كان خطأً في الصيغة القديمة (من لقطة الشاشة لا من الذوق) ─────────────
 * ① **الرقم كان معكوسًا:** `levelText` يُرجع «٢٣ / ٣٠» ونصٌّ كهذا في سياق عربيّ (RTL) يُرسم «٣٠ / ٢٣»
 *    فيُقرأ «ثلاثون من ثلاثةٍ وعشرين». والآن المستوى الحاليّ رقمٌ كبير، والسقف رقمٌ صغيرٌ هادئ بجانبه،
 *    ويُرسمان بلا اتّجاهٍ مُتأثّر بالسياق (LTR) فلا ينقلب ترتيبهما.
 * ② **مقبض الشريط كان يشبه زرّ «إيقاف مؤقّت»** (مستطيلٌ بخطّين) فيُظنّ أنّه يُوقف شيئًا. صار دائرةً
 *    معروفة، وفوق المسار خطّ ملء واحد بلون التمييز.
 * ③ **كل الدفقات صفوفٌ متطابقة بلا أيقونات** — فلا يُعرف الوسائط من الرنين إلا بقراءة الاسم. لكل دفق الآن
 *    رمزه، ويُخفَّض لونه حين يكون مكتومًا أو بلا قراءة.
 * ④ **الشريط لا يُمسَك أثناء التمرير الرأسيّ:** يُستعمل `detectHorizontalDragGestures` فلا يأخذ إلا السحب
 *    الأفقيّ، ويبقى التمرير الرأسيّ للصفحة حرًّا.
 *
 * **ولم يتغيّر العقد:** لا كتابة أثناء السحب (الحالة المحلّية وحدها تتحرّك)، والكتابة عند الإفلات عبر
 * [onCommit]، والقيمة المعروضة بعدها هي ما قرأته المنصّة. ودفقٌ بلا قراءة يُعرض مُعطَّلًا **وبسببه**.
 */
package nd.max.ui.subscreens.audio

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.RingVolume
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import nd.max.R
import nd.max.core.audio.AudioStreamCatalog
import nd.max.core.audio.AudioStreamReading
import nd.max.ui.design.MAX_VALUE_UNAVAILABLE
import nd.max.ui.design.MaxAlpha
import nd.max.ui.design.MaxCardShell
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxSize
import nd.max.ui.design.MaxSpace
import nd.max.ui.design.MaxTone
import nd.max.ui.design.container
import nd.max.ui.design.content
import nd.max.ui.design.maxRadius

/** ارتفاع منطقة اللمس للشريط — لا ينزل عن حدّ اللمس المُعلن (48dp) إلا قليلًا، والمسار نفسه أنحف. */
private val trackTouchHeight = 44.dp
private val trackThickness = 12.dp
private val thumbRadius = 11.dp
private val thumbGap = 3.dp

/**
 * بطاقة واحدة لكل الدفقات، بفاصلٍ هادئ بين صفٍّ وصف.
 *
 * @param dbOf يُعيد نصّ الديسيبل المُنسَّق لدفقٍ برمزه (أو `null`) — يأتي من المنصّة لا من حساب هنا.
 * @param footnote ملاحظة العقد («القيمة المعروضة جواب الجهاز لا جواب إصبعك») — صغيرةٌ في أسفل البطاقة
 *   بدل أن تكون وصفًا طويلًا فوق العنوان.
 */
@Composable
internal fun AudioStreamLevelsCard(
    readings: List<AudioStreamReading>,
    dbOf: (String) -> String?,
    footnote: String?,
    onCommit: (String, Int) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    MaxCardShell(
        contentPadding = 0.dp,
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        readings.forEachIndexed { index, reading ->
            if (index > 0) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = MaxSpace.lg),
                    color = scheme.outlineVariant.copy(alpha = MaxAlpha.border),
                )
            }
            StreamLevelRow(
                reading = reading,
                db = dbOf(reading.token),
                onCommit = onCommit,
            )
        }
        if (!footnote.isNullOrBlank()) {
            Text(
                text = footnote,
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = MaxSpace.lg, end = MaxSpace.lg, top = MaxSpace.xs, bottom = MaxSpace.md),
            )
        }
    }
}

@Composable
private fun StreamLevelRow(
    reading: AudioStreamReading,
    db: String?,
    onCommit: (String, Int) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current

    val maxLevel = reading.maxLevel ?: 0
    val live = AudioStreamCatalog.fractionOf(reading.level, reading.maxLevel)
    val readable = reading.level != null && maxLevel > 0 && live != null
    val muted = reading.muted == true

    var dragged by remember(reading.token) { mutableStateOf<Float?>(null) }
    var lastTick by remember(reading.token) { mutableIntStateOf(-1) }

    val shownFraction = dragged ?: live ?: 0f
    val shownLevel: Int? = if (readable) {
        dragged?.let { AudioStreamCatalog.levelOf(fraction = it, maxLevel = maxLevel) } ?: reading.level
    } else {
        null
    }

    val tone = when {
        !readable -> MaxTone.Inactive
        muted -> MaxTone.Neutral
        else -> MaxTone.Accent
    }

    val subtitle = listOfNotNull(
        when {
            !readable -> stringResource(R.string.max_audio_levels_unreadable)
            muted -> stringResource(R.string.max_audio_stream_muted)
            else -> null
        },
        db,
    ).joinToString("  ·  ").ifBlank { null }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MaxSpace.lg, vertical = MaxSpace.md),
        verticalArrangement = Arrangement.spacedBy(MaxSpace.xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.md),
        ) {
            Box(
                modifier = Modifier
                    .size(MaxSize.iconContainer)
                    .clip(RoundedCornerShape(maxRadius(MaxRadius.control)))
                    .background(tone.container()),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = streamIcon(reading.token),
                    contentDescription = null,
                    tint = tone.content(),
                    modifier = Modifier.size(MaxSize.iconGlyph),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(MaxSpace.hairline),
            ) {
                Text(
                    text = stringResource(streamTitle(reading.token)),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (readable) scheme.onSurface else scheme.onSurface.copy(alpha = MaxAlpha.disabledContent),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            LevelReadout(level = shownLevel, maxLevel = maxLevel, active = readable && !muted)
        }

        LevelTrack(
            fraction = shownFraction,
            enabled = readable,
            muted = muted,
            description = if (shownLevel != null) {
                stringResource(R.string.audio_levels_a11y_value, shownLevel, maxLevel)
            } else {
                stringResource(R.string.max_audio_levels_unreadable)
            },
            onChange = { fraction ->
                dragged = fraction
                // نبضة خفيفة عند عبور كل درجة، فيُحسّ الإصبع بالدرجات لا بالنظر وحده.
                val step = AudioStreamCatalog.levelOf(fraction = fraction, maxLevel = maxLevel)
                if (step != lastTick) {
                    if (lastTick != -1) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    lastTick = step
                }
            },
            onFinish = { fraction ->
                dragged = null
                lastTick = -1
                if (!readable || reading.level == null) return@LevelTrack
                val target = AudioStreamCatalog.levelOf(fraction = fraction, maxLevel = maxLevel)
                if (target != reading.level) onCommit(reading.token, target)
            },
            onCancel = {
                dragged = null
                lastTick = -1
            },
        )
    }
}

/**
 * القراءة: **المستوى الحاليّ كبيرٌ، والسقف صغيرٌ بجانبه** — وفي سياق LTR صريح كي لا ينقلب ترتيب الرقمين
 * في الواجهة العربيّة (انظر ① أعلى الملف).
 */
@Composable
private fun LevelReadout(level: Int?, maxLevel: Int, active: Boolean) {
    val scheme = MaterialTheme.colorScheme
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        if (level == null) {
            Text(
                text = MAX_VALUE_UNAVAILABLE,
                style = MaterialTheme.typography.titleLarge,
                color = scheme.onSurface.copy(alpha = MaxAlpha.disabledContent),
            )
        } else {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = level.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (active) scheme.onSurface else scheme.onSurfaceVariant,
                )
                Text(
                    text = "/$maxLevel",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = FontFamily.Monospace,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = MaxSpace.hairline, bottom = MaxSpace.xs),
                )
            }
        }
    }
}

/**
 * الشريط: مسارٌ واحد وملءٌ واحد ومقبضٌ دائريّ، **ويحترم اتّجاه الواجهة** — في العربيّة يبدأ الملء من اليمين.
 *
 * السحب الأفقيّ وحده يُمسَك (والنقرة تضع القيمة عند موضعها)، فلا يسرق الشريط التمرير الرأسيّ.
 */
@Composable
private fun LevelTrack(
    fraction: Float,
    enabled: Boolean,
    muted: Boolean,
    description: String,
    onChange: (Float) -> Unit,
    onFinish: (Float) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val trackColor = scheme.surfaceContainerHighest
    val gapColor = scheme.surfaceContainerLow
    val fillColor = when {
        !enabled -> scheme.outline.copy(alpha = MaxAlpha.borderStrong)
        muted -> scheme.outline
        else -> scheme.primary
    }

    // مراجع حيّة: الإيماءة لا تُعاد عند كل خطوة، فلا ينقطع السحب في منتصفه.
    val latestChange by rememberUpdatedState(onChange)
    val latestFinish by rememberUpdatedState(onFinish)
    val latestCancel by rememberUpdatedState(onCancel)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(trackTouchHeight)
            .semantics {
                contentDescription = description
                progressBarRangeInfo = ProgressBarRangeInfo(fraction.coerceIn(0f, 1f), 0f..1f)
                if (!enabled) disabled()
            }
            .pointerInput(enabled, rtl) {
                if (!enabled) return@pointerInput
                val inset = (thumbRadius + thumbGap).toPx()
                fun valueAt(x: Float): Float {
                    val span = (size.width - inset * 2f).coerceAtLeast(1f)
                    val raw = ((x - inset) / span).coerceIn(0f, 1f)
                    return if (rtl) 1f - raw else raw
                }
                detectTapGestures { offset ->
                    val value = valueAt(offset.x)
                    latestChange(value)
                    latestFinish(value)
                }
            }
            .pointerInput(enabled, rtl) {
                if (!enabled) return@pointerInput
                val inset = (thumbRadius + thumbGap).toPx()
                var current = 0f
                fun valueAt(x: Float): Float {
                    val span = (size.width - inset * 2f).coerceAtLeast(1f)
                    val raw = ((x - inset) / span).coerceIn(0f, 1f)
                    return if (rtl) 1f - raw else raw
                }
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        current = valueAt(offset.x)
                        latestChange(current)
                    },
                    onDragEnd = { latestFinish(current) },
                    onDragCancel = { latestCancel() },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        current = valueAt(change.position.x)
                        latestChange(current)
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val thickness = trackThickness.toPx()
            val radius = thickness / 2f
            val thumbR = thumbRadius.toPx()
            val gapR = thumbGap.toPx()
            val inset = thumbR + gapR
            val cy = size.height / 2f
            val span = size.width - inset * 2f
            val f = fraction.coerceIn(0f, 1f)
            // الموضع على المحور: في RTL يبدأ من الحافة اليمنى.
            val thumbX = if (rtl) size.width - inset - span * f else inset + span * f

            drawRoundRect(
                color = trackColor,
                topLeft = Offset(0f, cy - radius),
                size = Size(size.width, thickness),
                cornerRadius = CornerRadius(radius, radius),
            )

            val fillStart = if (rtl) thumbX else 0f
            val fillEnd = if (rtl) size.width else thumbX
            if (fillEnd > fillStart) {
                drawRoundRect(
                    color = fillColor,
                    topLeft = Offset(fillStart, cy - radius),
                    size = Size(fillEnd - fillStart, thickness),
                    cornerRadius = CornerRadius(radius, radius),
                )
            }

            // حلقة بلون البطاقة تفصل المقبض عن الملء، ثم المقبض نفسه.
            drawCircle(color = gapColor, radius = thumbR + gapR, center = Offset(thumbX, cy))
            drawCircle(color = fillColor, radius = thumbR, center = Offset(thumbX, cy))
        }
    }
}

/** رمز الدفق — جدولٌ ثابت لا يرسم، فيُعرف الدفق من شكله قبل اسمه. */
private fun streamIcon(token: String): ImageVector = when (token) {
    "media" -> Icons.Rounded.MusicNote
    "call" -> Icons.Rounded.Call
    "ring" -> Icons.Rounded.RingVolume
    "notification" -> Icons.Rounded.Notifications
    "alarm" -> Icons.Rounded.Alarm
    "system" -> Icons.Rounded.PhoneAndroid
    else -> Icons.Rounded.GraphicEq
}
