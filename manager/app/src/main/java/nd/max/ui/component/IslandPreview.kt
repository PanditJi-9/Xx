/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */
/*
 * الجزيرة الديناميكية — **معاينة الوضع والبطاقات**: مسرحٌ يُسحب فيه، ومعرضٌ يتحرّك.
 *
 * ### ولماذا مسرح لا صورة
 *
 * طلب المالك: «تحريكه بأعلى وأسفل أو يمين ويسار لكي يقوم المستخدم بوضعه في المكان المناسب».
 * والمكان لا يُملأ في حقل رقم: من يكتب ٣٥٪ لا يرى أين تقع. فالمعاينة **هي مكان الوضع**: مسرح
 * يمثّل أعلى [IslandGeometry.PREVIEW_TOP_FRACTION] من الشاشة (وهي نفسها أقصى إزاحة رأسية،
 * فلا تكذب حافّة المسرح)، خلفيّته خلفية الجهاز الحقيقية (`WallpaperCache`) مع سقوط إلى تدرّج
 * من الثيم، وفوقه شريط حالة تمثيلي بالوقت والبطارية الحقيقيين — لأن «وسط الأعلى» لا يُقرأ
 * بلا شريط حالة يجعله وسطًا.
 *
 * ### والمعاينة ليست صورة ثانية
 *
 * الكبسولة المرسومة هنا هي `IslandCapsule` نفسها التي تُرسم فوق التطبيقات، بالمفاتيح نفسها.
 * والفرق الوحيد المعلن أن نسب العرض تُقاس من **عرض المسرح** لا من عرض الشاشة (المسرح أضيق
 * بحشوة الصفحة)، والنتيجة أن النسب صحيحة في الإطار لا أن المقاس البكسلي مطابق — وهذا ما
 * يقوله سطر الشاشة بنصّه، فلا يُوعد المستخدم بغير ما يراه.
 *
 * ### والسحب يقيّد بـ`IslandGeometry` لا بحدّ محليّ
 *
 * حدّ الإزاحة الأقصى يعتمد على **عرض الكبسولة اللحظيّ** (يتغيّر مع المحتوى)، فالحساب يقع في
 * `IslandGeometry.clampOffsetXF` — الدالّة النقيّة نفسها التي ستُقاس عليها الخدمة عند البناء
 * النهائي، فلا يستطيع مستوى أن يسمح بما يمنعه آخر.
 *
 * ### ومعرض البطاقات يعيد النبضة الحقيقية
 *
 * «هادئة ← بطاقة ← هادئة» بدورة واحدة تجري بمفاتيح المستخدم نفسها (اللون، المقاس، الشكل،
 * المدّة): فمن أطفأ الحركة يرى البطاقات الثلاث ثابتة بلا ادّعاء زمن، ومن أطفأ النقطة الهادئة
 * يرى الفراغ الذي اختاره. وليس تسجيلًا ولا صورة محفوظة: نفس دالّة الرسم ونفس الأرقام.
 */
package nd.max.ui.component

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.text.format.DateFormat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.delay
import nd.max.R
import nd.max.ui.design.MaxDuration
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxSpace
import nd.max.ui.overlay.DisplayCutouts
import nd.max.ui.util.IslandContent
import nd.max.ui.util.IslandGeometry
import nd.max.ui.util.IslandPrefs
import nd.max.ui.util.WallpaperCache
import java.util.Date
import kotlin.math.roundToInt
import nd.max.ui.design.maxRadius

/** دورة معرض البطاقات: خطوة واحدة تشرح النبضة الحقيقية بلا استهلاك مدّتها الكاملة (٨ ثوانٍ). */
private const val GALLERY_STEP_MS = 1_600L

/** نبضة ساعة الشريط التمثيلي: الدقيقة تكفي — والثواني تُقرأ حركةً لا وقتًا. */
private const val CLOCK_TICK_MS = 20_000L

/** سطور إخفات على الخلفية: أعلى المسرح وأسفله فقط، والوسط يبقى صورةً حقيقية. */
private const val SCRIM_TOP_ALPHA = 0.38f
private const val SCRIM_BOTTOM_ALPHA = 0.18f

/** نسبة شحن العيّنة في بطاقة الشحن — رقم ثابت معلن، لا قراءة تُدَّعى. */
private const val DEMO_CHARGE_PERCENT = 85

/**
 * مسرح المعاينة: خلفية الجهاز، وشريط حالة تمثيلي، وكبسولة تُسحب وتُحفظ عند الإفلات.
 *
 * @param content ما تعرضه الجزيرة الآن — يُمرَّر من الشاشة كي يبقى المسرح رسمًا لا مصدر بيانات.
 * @param onOffsetsChange يُنادى بعد الإفلات بالنسبتين النهائيتين (المحبوستين) فتُحفظا.
 */
@Composable
fun IslandStage(
    content: IslandContent,
    prefs: IslandPrefs.State,
    onOffsetsChange: (offsetXf: Float, offsetYf: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val scheme = MaterialTheme.colorScheme
    val haptics = rememberMaxHaptics()
    val wallpaper = WallpaperCache.bitmapState.value
    LaunchedEffect(Unit) { WallpaperCache.init(context) }

    var stageSize by remember { mutableStateOf(IntSize.Zero) }
    var capsuleSize by remember { mutableStateOf(IntSize.Zero) }
    var dragXf by remember { mutableStateOf(prefs.offsetXf) }
    var dragYf by remember { mutableStateOf(prefs.offsetYf) }
    var dragging by remember { mutableStateOf(false) }
    // أساس الجزيرة بقياس الجهاز الحقيقي: الحساب نفسه الذي تحسبه الخدمة فوق التطبيقات، فيُرى المكان نفسه.
    val view = LocalView.current
    val screenWidthPx = context.resources.displayMetrics.widthPixels
    val screenHeightPx = context.resources.displayMetrics.heightPixels
    val camera = IslandGeometry.cameraCutout(DisplayCutouts.of(view), screenWidthPx, screenHeightPx)
    val anchorPx = IslandGeometry.anchorYPx(
        prefs.position,
        DisplayCutouts.statusBarPx(context.resources),
        camera?.bottom
    )
    val anchorShare = if (screenHeightPx > 0) anchorPx.toFloat() / screenHeightPx else 0f

    // القيم المحفوظة تُتبع وهي ساكنة، ولا تُقتلع الكبسولة من تحت الإصبع أثناء السحب: الشاشة
    // تُعيد التركيب كل نبضة (`IslandLive.config`)، فبلا هذا الشرط يقفز ما تسحبه إلى ما حُفظ.
    LaunchedEffect(prefs.offsetXf, prefs.offsetYf, dragging) {
        if (!dragging) {
            dragXf = prefs.offsetXf
            dragYf = prefs.offsetYf
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(
                IslandGeometry.previewAspect(configuration.screenWidthDp, configuration.screenHeightDp)
            )
            .clip(RoundedCornerShape(maxRadius(MaxRadius.group)))
            .background(
                Brush.verticalGradient(listOf(scheme.surfaceVariant, scheme.surface))
            )
            .onSizeChanged { stageSize = it }
    ) {
        // الخلفية الحقيقية إن قُرئت: المعاينة على خلفية المستخدم نفسه تكشف ما لا تكشفه خلفية
        // مصمتة — كبسولة سوداء على خلفية سوداء تختفي، وهذا يجب أن يُرى هنا لا على الشاشة.
        wallpaper?.let { bitmap ->
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        }
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = SCRIM_TOP_ALPHA),
                            Color.Transparent,
                            Color.Black.copy(alpha = SCRIM_BOTTOM_ALPHA)
                        )
                    )
                )
        )
        StageStatusBar(
            modifier = Modifier
                .align(Alignment.TopCenter)
        )

        // الأساس والإزاحة معًا كنسبة من الشاشة، ثم يُسقطان على المسرح: ما تراه هنا هو ما يقع فوق التطبيقات.
        val xPx = dragXf * stageSize.width
        val yPx = IslandGeometry.previewYFractionOf(anchorShare + dragYf) * stageSize.height
        // قصّة الكاميرا كما هي على هذا الجهاز: الكبسولة تستقرّ أسفلها، فلا بدّ أن تُرى هنا أيضًا.
        val cut = camera
        if (cut != null && stageSize.width > 0 && screenWidthPx > 0 && screenHeightPx > 0) {
            val camLeft = cut.left.toFloat() / screenWidthPx * stageSize.width
            val camWidth = (cut.right - cut.left).toFloat() / screenWidthPx * stageSize.width
            val camTop = IslandGeometry.previewYFractionOf(cut.top.toFloat() / screenHeightPx) * stageSize.height
            val camBottom = IslandGeometry.previewYFractionOf(cut.bottom.toFloat() / screenHeightPx) * stageSize.height
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset { IntOffset(camLeft.roundToInt(), camTop.roundToInt()) }
                    .width(with(density) { camWidth.toDp() })
                    .height(with(density) { (camBottom - camTop).toDp() })
                    .clip(RoundedCornerShape(MaxRadius.pill))
                    .background(Color.Black)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset { IntOffset(xPx.roundToInt(), yPx.roundToInt()) }
                .onSizeChanged { capsuleSize = it }
                .pointerInput(stageSize, capsuleSize) {
                    detectDragGestures(
                        onDragStart = { dragging = true },
                        onDragCancel = { dragging = false },
                        onDragEnd = {
                            dragging = false
                            // نبضة واحدة عند الاستقرار: الحفظ يُعلَن باللمس قبل أن يُرى بالعين.
                            haptics.select()
                            onOffsetsChange(dragXf, dragYf)
                        }
                    ) { change, amount ->
                        change.consume()
                        if (stageSize.width > 0 && stageSize.height > 0) {
                            dragXf = IslandGeometry.clampOffsetXF(
                                dragXf + amount.x / stageSize.width,
                                stageSize.width,
                                capsuleSize.width
                            )
                            dragYf = IslandGeometry.clampOffsetY(
                                dragYf + amount.y / stageSize.height * IslandGeometry.PREVIEW_TOP_FRACTION
                            )
                        }
                    }
                }
        ) {
            IslandCapsule(
                content = content,
                prefs = prefs,
                widthBasis = with(density) { stageSize.width.toDp() }
            )
        }
    }
}

/**
 * شريط الحالة التمثيلي: الوقت الحقيقي بصيغة النظام، ونسبة البطارية الحقيقية.
 *
 * **ولماذا حقيقي لا مصور:** شريطٌ بأرقام مخترعة يجعل المعاينة تكذب في اللحظة نفسها التي تُقرأ
 * فيها للاطمئنان. والقراءتان كلتاهما رخيصتان: الوقت من `DateFormat` بصيغة الجهاز، والبطارية من
 * بثّ النظام اللاصق (`ACTION_BATTERY_CHANGED`) بلا تسجيل مستقبل — قراءة واحدة عند التركيب.
 */
@Composable
private fun StageStatusBar(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val battery = remember { readStageBattery(context) }
    val timeText by produceState(initialValue = stageClockText(context)) {
        while (true) {
            delay(CLOCK_TICK_MS)
            value = stageClockText(context)
        }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MaxSpace.lg, vertical = MaxSpace.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = timeText,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        battery?.let { reading ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaxSpace.xs)
            ) {
                Text(
                    text = "${reading.percent}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
                IslandBattery(
                    percent = reading.percent,
                    tint = if (reading.charging) ChargingGreen else Color.White,
                    animated = false
                )
            }
        }
    }
}

/**
 * معرض البطاقات — النبضة الحقيقية بدورة قصيرة، أو البطاقات الثلاث ثابتة عند إسكات الحركة.
 *
 * والعيّنات من موارد التطبيق نفسها (`dynamic_island_*`)، فلا نصّ نموذجيّ ثانٍ يُترجم مرّتين،
 * ولا رقم يُخترع: نسبة الشحن عيّنة ثابتة معلَنة، والعيّنات كلها موسومة كتجربة في نصوصها.
 */
@Composable
fun IslandCardGallery(prefs: IslandPrefs.State, modifier: Modifier = Modifier) {
    val animationsEnabled = rememberAnimationsEnabled()
    val playing = prefs.animate && animationsEnabled
    val demoPackage = LocalContext.current.packageName
    val noticeApp = stringResource(R.string.dynamic_island_title)
    val noticeTitle = stringResource(R.string.dynamic_island_test_title)
    val noticeText = stringResource(R.string.dynamic_island_test_text)
    val mediaTitle = stringResource(R.string.dynamic_island_demo_media_title)
    val mediaArtist = stringResource(R.string.dynamic_island_demo_media_artist)
    val steps = remember(demoPackage, noticeApp, noticeTitle, noticeText, mediaTitle, mediaArtist) {
        listOf(
            IslandContent.Hidden,
            // والبطاقة التجريبية من التطبيق نفسه: شعاره يُقرأ حقيقةً فيظهر شعارًا لا حرف أوّل —
            // وهي البطاقة التي جاء المستخدم ليراها قبل التفعيل، فلا تُعرض بأنقص مما ستكون عليه.
            IslandContent.Transient(
                appLabel = noticeApp,
                title = noticeTitle,
                text = noticeText,
                packageName = demoPackage
            ),
            IslandContent.Hidden,
            IslandContent.Media(title = mediaTitle, artist = mediaArtist),
            IslandContent.Hidden,
            IslandContent.Charging(DEMO_CHARGE_PERCENT)
        )
    }

    if (!playing) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(MaxSpace.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // بلا حركة تُعرض البطاقات الثلاث ثابتة: الحركة شرحٌ لتغيّر، ولا شيء يتغيّر هنا.
            CHARGED_PHASES.forEach { index -> IslandCapsule(content = steps[index], prefs = prefs) }
        }
        return
    }

    var phase by remember { mutableIntStateOf(NOTICE_PHASE) }
    LaunchedEffect(steps) {
        while (true) {
            delay(GALLERY_STEP_MS)
            phase = (phase + 1) % steps.size
        }
    }
    AnimatedContent(
        targetState = phase,
        modifier = modifier.fillMaxWidth(),
        transitionSpec = {
            (
                fadeIn(tween(MaxDuration.standard)) +
                    slideInVertically(tween(MaxDuration.standard)) { -it / 3 }
                ) togetherWith (
                fadeOut(tween(MaxDuration.quick)) +
                    slideOutVertically(tween(MaxDuration.quick)) { it / 3 }
                )
        },
        contentAlignment = Alignment.Center,
        label = "islandCardGallery"
    ) { index ->
        IslandCapsule(content = steps[index], prefs = prefs)
    }
}

/** البطاقات غير الهادئة في سلّم المراحل: الإشعار والوسائط والشحن. */
private val CHARGED_PHASES = listOf(1, 3, 5)

/** مرحلة البداية: الإشعار — أوّل ما يفتح المستخدم الشاشة لأجله. */
private const val NOTICE_PHASE = 1

private data class StageBattery(val percent: Int, val charging: Boolean)

private fun stageClockText(context: Context): String =
    DateFormat.getTimeFormat(context).format(Date())

/** قراءة واحدة من بثّ البطارية اللاصق؛ و`null` حين لا يُعلن النظام شيئًا (لا صفر مكذوب). */
private fun readStageBattery(context: Context): StageBattery? = runCatching {
    val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    // والقراءة الآمنة نفسها هنا: `intent` قابل للعدم (بثّ لاصق لا يضمن إرسالًا)، فرابعه لا يُستثنى.
    val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
    if (level < 0 || scale <= 0) {
        null
    } else {
        StageBattery(
            percent = level * 100 / scale,
            charging = plugged != 0
        )
    }
}.getOrNull()
