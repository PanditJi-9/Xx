/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nd.max.R
import nd.max.core.platform.HudArrangement
import nd.max.core.platform.HudField
import nd.max.core.platform.HudForm
import nd.max.core.platform.HudReading
import nd.max.ui.util.HudFilter
import nd.max.ui.util.hudChartVisible
import nd.max.ui.util.hudTextVisible
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxSize
import nd.max.ui.design.MaxSpace
import nd.max.ui.design.MAX_VALUE_UNAVAILABLE
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.ui.graphics.vector.ImageVector
import nd.max.ui.design.maxRadius

/** أقصى إطار/ثانية يُقاس عليه القوس حين لا يُعرف معدّل تحديث الشاشة. */
private const val FALLBACK_CEILING = 120f

/** كم عيّنة يحتفظ بها تاريخ الحقل في اللوحة — ما يُقرأ بنظرة في ارتفاع شريط صغير. */
private const val HUD_SERIES_SAMPLES = 40

/** سماكة خطّ الرسم. */
private const val GraphStroke = 2.5f

/** ألفة ملء المساحة تحت الخطّ — شكل لا حكم. */
private const val AreaFillAlpha = 0.20f

/** حصة عرض العمود من الخطوة بين عيّنتين. */
private const val BarWidthShare = 0.62f

/** أضيق عمود مرئي حين تتزاحم العيّنات. */
private const val MinBarWidth = 1.5f

/** هامش فوق أعلى عيّنة حين لا سقف طبيعيًّا للقياس، فلا يلتصق الشكل بالحافة. */
private const val SeriesCeilingHeadroom = 1.15f

/**
 * شكل الرسم بحسب **نوع القياس** لا بحسب الذوق.
 *
 * الخطّ لمعدّل يتحرّك كل لحظة، والملء لحمل يشغل نسبة من سقف، والأعمدة لنسبة تُقرأ حصصًا،
 * والدرج لدرجة أو تردد يقفز ثم يسكن. وهذا ما كان ناقصًا: شكل واحد لكل حقل يلغي الفرق بين
 * الأنواع ويجعل العين تقرأ الهيئة نفسها في كل مرة.
 */
private enum class HudChartShape { Line, Area, Bars, Stepped }

/**
 * مقاس زرّ واحد في صفّ أدوات اللوحة.
 *
 * **وهو أقلّ من أرضية اللمس في المستودع (٤٨) بأمر المالك** («النافذة المنبثقة كبيرة دون داعي
 * مما يؤثر على الرؤية واللعب»، تكملة ١٨٢): على لعبة ملء الشاشة صار زرّا الإخفاء والإغلاق
 * ثلثي ارتفاع اللوحة. والسند مشروح كاملًا عند [HudActions]، ومنه أنّ اللوحة كلّها تُسحب وأن
 * الإغلاق متاح من إشعار الخدمة. **ولا يُنقل هذا الرقم إلى شاشة داخل التطبيق**: هو خاصّ بسطح
 * مساحته من مساحة اللعبة.
 */
private val HudActionSize = 32.dp

private val Ink = Color(0xFFFFFFFF)
private val InkMuted = Color(0xFF9AA6AE)
private val PanelTrack = Color(0xFF2A3339)
private val RecordDot = Color(0xFFFF5A5A)

/**
 * النصّ المعروض لحقل: **الغائب شرطة لا صفرًا**.
 *
 * ودالّة مستقلّة عن Compose عن قصد: تُختبر وحدها بلا شاشة (وهي القاعدة التي تحكم
 * `status_unknown` في طبقة البيانات: ما لم يُقَس لا يُرقّم).
 */
fun hudFieldText(reading: HudReading?, field: HudField): String = when (field) {
    HudField.Frames -> reading?.frames?.let { "%.0f".format(it) } ?: MAX_VALUE_UNAVAILABLE
    HudField.Cpu -> reading?.cpu?.let { "$it%" } ?: MAX_VALUE_UNAVAILABLE
    HudField.Ram -> reading?.ramMb?.let { "$it MB" } ?: MAX_VALUE_UNAVAILABLE
    HudField.Power -> reading?.watt?.let { "%.1f W".format(it) } ?: MAX_VALUE_UNAVAILABLE
    HudField.Heat -> reading?.heat?.let { "%.0f°C".format(it) } ?: MAX_VALUE_UNAVAILABLE
    HudField.Battery -> reading?.battery?.let { "$it%" } ?: MAX_VALUE_UNAVAILABLE
    HudField.Network -> reading?.let {
        // الزوج يُعرض معًا؛ وغياب الاتجاهين شرطة، وغياب أحدهما يدلّ على «لم يُقَس بعد».
        if (it.rxBps == null && it.txBps == null) {
            MAX_VALUE_UNAVAILABLE
        } else {
            "↓${hudRateText(it.rxBps)} ↑${hudRateText(it.txBps)}"
        }
    } ?: MAX_VALUE_UNAVAILABLE
    HudField.Renderer -> reading?.renderer ?: MAX_VALUE_UNAVAILABLE
    HudField.Stability -> reading?.stability?.let { "$it%" } ?: MAX_VALUE_UNAVAILABLE
    HudField.Jank -> reading?.jankPercent?.let { percent ->
        // **ولا يُترَك `%` حرفيًّا في نصّ يُنسَّق (قِيس على جهاز، ٢٠٢٦-١١-١٧):** كان السطر
        // `"$percent% · p99 %.0f ms".format(p99)`، فقرأ `Formatter` المقطع `% ·` مواصفةَ تحويل
        // ورمى `UnknownFormatConversionException: Conversion = '·'` ⇒ كراش `FpsOverlayService`
        // عند فتح HUD (السجل: `String.format ← o.ey2.n ← FpsOverlayService.c`). فالنسبة تُركَّب
        // نصًّا، ويبقى القالب للرقم وحده.
        reading.frameP99Ms?.let { p99 -> "$percent% · p99 ${"%.0f".format(p99)} ms" } ?: "$percent%"
    } ?: MAX_VALUE_UNAVAILABLE
    HudField.CpuFreq -> reading?.cpuFreqMhz?.let { "$it MHz" } ?: MAX_VALUE_UNAVAILABLE
    HudField.SocTemp -> reading?.socTempC?.let { "%.0f°C".format(it) } ?: MAX_VALUE_UNAVAILABLE
    HudField.GpuFreq -> reading?.gpuFreqMhz?.let { "$it MHz" } ?: MAX_VALUE_UNAVAILABLE
    HudField.GpuLoad -> reading?.gpuLoadPercent?.let { "$it%" } ?: MAX_VALUE_UNAVAILABLE
    HudField.DdrFreq -> reading?.ddrFreqMhz?.let { "$it MHz" } ?: MAX_VALUE_UNAVAILABLE
    HudField.DiskIo -> reading?.let {
        if (it.diskReadBps == null && it.diskWriteBps == null) {
            MAX_VALUE_UNAVAILABLE
        } else {
            "↓${hudRateText(it.diskReadBps)} ↑${hudRateText(it.diskWriteBps)}"
        }
    } ?: MAX_VALUE_UNAVAILABLE
    HudField.BatteryVoltage -> reading?.batteryVoltageMv?.let { "%.2f V".format(it / 1000f) }
        ?: MAX_VALUE_UNAVAILABLE
}

/**
 * معدّل بايت/ثانية بصيغة قصيرة.
 *
 * **والفروق الثلاثة مقصودة:** الصفر **قياس صالح** (لا حركة تُذكر) فلا يُستبدل بشرطة؛
 * والغائب/غير المنتهي/السالب لا يُطبع له رقم؛ والوحدات تُبدّل عند العتبة لا عند التقدير.
 */
fun hudRateText(bytesPerSecond: Float?): String {
    if (bytesPerSecond == null || !bytesPerSecond.isFinite() || bytesPerSecond < 0f) {
        return MAX_VALUE_UNAVAILABLE
    }
    return when {
        bytesPerSecond < 1024f -> "%.0f B/s".format(bytesPerSecond)
        bytesPerSecond < 1024f * 1024f -> "%.1f KB/s".format(bytesPerSecond / 1024f)
        else -> "%.1f MB/s".format(bytesPerSecond / (1024f * 1024f))
    }
}

/**
 * زمن الإطار بالميلي ثانية: مقلوب الإطارات × ١٠٠٠.
 *
 * **ولماذا دالّة لا حساب داخل الرسم:** هذا هو الرقم الذي يقيسه من اعتاد تراكبات الحاسوب،
 * ودالّة مستقلّة عن Compose تعني أنّه يُختبر وحده — وفيه ثلاث حالات تُخطئ فيها القسمة المباشرة:
 * الغائب (لا قياس ⇒ شرطة لا صفرًا)، والصفر (لا يُقسم عليه، فالناتج `∞` وليس قياسًا)، والسالبة
 * (قياس مُفسَد يُعامل كالغائب). فلا يُطبع في اللوحة رقم لم يُقَس.
 */
fun hudFrameTimeText(frames: Float?): String {
    if (frames == null || frames <= 0f) return MAX_VALUE_UNAVAILABLE
    return "%.1f ms".format(1000f / frames)
}

@Composable
fun hudFieldLabel(field: HudField): String = stringResource(
    when (field) {
        HudField.Frames -> R.string.hud_field_frames
        HudField.Cpu -> R.string.hud_field_cpu
        HudField.Ram -> R.string.hud_field_ram
        HudField.Power -> R.string.hud_field_power
        HudField.Heat -> R.string.hud_field_heat
        HudField.Battery -> R.string.hud_field_battery
        HudField.Network -> R.string.hud_field_network
        HudField.Renderer -> R.string.hud_field_renderer
        HudField.Stability -> R.string.hud_field_stability
        HudField.Jank -> R.string.hud_field_jank
        HudField.CpuFreq -> R.string.hud_field_cpu_freq
        HudField.SocTemp -> R.string.hud_field_soc_temp
        HudField.GpuFreq -> R.string.hud_field_gpu_freq
        HudField.GpuLoad -> R.string.hud_field_gpu_load
        HudField.DdrFreq -> R.string.hud_field_ddr_freq
        HudField.DiskIo -> R.string.hud_field_disk_io
        HudField.BatteryVoltage -> R.string.hud_field_battery_voltage
    }
)

/**
 * اللوحة نفسها — **مُصيِّر واحد لموضعين**: النافذة العائمة فوق الألعاب، والمعاينة داخل شاشة
 * الإعدادات.
 *
 * **ولماذا واحد لا اثنان:** كانت المعاينة تُوصف بالكلام («ستظهر هكذا») واللوحة تُرسم بشيفرة
 * أخرى، فكل اختيار يُغيّر الشكل لا يُرى إلا بعد الخروج من التطبيق وتشغيل لعبة — وهو أطول حلقة
 * تغذية راجعة ممكنة في أداة إعدادات. ونفس الدالّة هنا تعني أنّ ما تراه في الشاشة **هو** ما
 * سيُعرض، لا وصفًا له. وأي فروق بين الاثنين تصير مستحيلة لا مكتشَفة.
 *
 * والسطح **داكن دائمًا** ولو كان التطبيق بثيم فاتح: هو معاينة لنافذة تُرسم فوق لعبة، فتبييضه
 * في الثيم الفاتح كان سيكذب على المستخدم.
 */
@Composable
fun HudSurface(
    form: HudForm,
    arrangement: HudArrangement,
    reading: HudReading?,
    fields: List<HudField>,
    accent: Color,
    textSizeSp: Float,
    backgroundAlpha: Float,
    framesHistory: List<Float>,
    showGraph: Boolean,
    recording: Boolean,
    onToggleRecording: (() -> Unit)? = null,
    onHide: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    filter: HudFilter = HudFilter.All
) {
    val displayCeiling = framesCeiling()
    /*
     * المنحنى يتبع **الحقل المعروض** لا حقل الإطارات وحده: كان الرسم لا يظهر إلا إذا كان
     * `Frames` بين الحقول، فمن عرض الحرارة أو الحمل أو التقطيع بقي بلا شكل يُقرأ بالعين.
     * والشكل يتبع ما يقيسه الحقل: خطٌّ لمعدّل، ومساحةٌ لحمل، وأعمدةٌ لنسبة تقطيع، ودرجٌ
     * لتردد أو حرارة تتغيّر قفزًا. ولا محور مُختلق: الرقم بجانب الشكل هو القياس (ADR-07).
     */
    val chartField = hudChartField(fields)
    val chartPossible = chartField != null
    val textVisible = hudTextVisible(filter, chartPossible)
    val chartVisible = hudChartVisible(filter, showGraph, chartPossible)
    val series = rememberHudSeries(chartField, reading, framesHistory)
    // وزرّ التسجيل جزء من **نفس الصفّ** بأمر المالك: كان صفًّا مستقلًّا بارتفاع ٤٨dp وفاصل ثابت
    // حتى وهو لا يسجّل — أي ثلث ارتفاع اللوحة مقابل إجراء لا يُستعمل في كل جلسة (تكملة ١٨٢).
    val hasActions = onHide != null || onClose != null || onToggleRecording != null
    /*
     * وموضع الأزرار يتبع شكل البيانات لا ذوقًا: الشريط والرقاقة **سطر واحد**، فصفّ رأس فوقهما
     * يُبطل سبب وجودهما — وهي في [HudArrangement.Line] **داخل الصفّ نفسه** (طرفه الأيمن في
     * العربية والأيسر في الإنجليزية، فالترتيب يقلبه مع النصّ). وما عداهما طويل أصلًا فيحملها
     * في آخر صفّ — وهو أقرب موضع للحاشية في لوحة بلا إطار.
     */
    val inlineActions = hasActions && !chartVisible &&
        arrangement == HudArrangement.Line &&
        (form == HudForm.Strip || form == HudForm.Badge)
    val actions: (@Composable () -> Unit)? = if (hasActions) {
        {
            HudActions(
                recording = recording,
                onToggleRecording = onToggleRecording,
                onHide = onHide,
                onClose = onClose
            )
        }
    } else {
        null
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(maxRadius(MaxRadius.control)))
            .background(Color.Black.copy(alpha = backgroundAlpha))
            .padding(MaxSpace.sm)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaxSpace.xs)
        ) {
            if (textVisible) {
                when (form) {
                    HudForm.Strip ->
                        HudStrip(reading, fields, accent, textSizeSp, arrangement, if (inlineActions) actions else null)

                    HudForm.Pane ->
                        HudPane(reading, fields, accent, textSizeSp, arrangement)

                    HudForm.Ring ->
                        HudRing(reading, fields, accent, textSizeSp, displayCeiling)
                    HudForm.Badge ->
                        HudBadge(reading, fields, accent, textSizeSp, arrangement, if (inlineActions) actions else null)
                }
            }

            if (chartVisible) {
                if (chartField != null) {
                    HudMetricGraph(
                        series = series,
                        accent = accent,
                        shape = hudChartShape(chartField),
                        ceiling = hudChartCeiling(chartField, displayCeiling, series)
                    )
                }
            }

            // صفّ أزرار واحد لِما هو غير مُضمَّن في صفّ البيانات — وفاصل واحد قبله يفصل
            // الرقم عن أدواته. وكان صفّين وثلاثة فواصل.
            if (actions != null && !inlineActions) {
                HorizontalDivider(color = InkMuted.copy(alpha = 0.25f))
                actions()
            }
        }
    }
}

/**
 * رقاقة بمبدأ تراكبات الحاسوب: قيمة واحدة **كبيرة** ووحدتها في السطر نفسه، وزمن الإطار تحتها،
 * وبقيّة الحقول سطرًا مضغوطًا إلى جوارها.
 *
 * **ولماذا هي شكل رابع لا تفصيل في [HudStrip]:** الشريط يقول كل شيء بالمثل فيُقرأ مسحًا؛ والرقاقة
 * تقول **رقمًا واحدًا أسرع ما يمكن قراءته من طرف العين** ثم ما يُكمله. وهي الحالة التي يقضي فيها
 * المستخدم ساعات: لعبة ملء الشاشة، لوحة في زاوية، ونظرة عابرة لا قراءة.
 *
 * وزمن الإطار (`ms`) هو ما لا يُظهره أي شكل آخر — وهو الرقم الذي يقيسه من اعتاد Fraps أو MangoHud،
 * لأن ٦٠ و٥٥ إطارًا قد يبدوان متقاربين في العدّ وهما متباعدان في الإحساس: ١٦٫٧ms مقابل ١٨٫٢ms.
 *
 * والبطل هو الإطارات إن كانت مختارة، وإلا **أوّل حقل مختار**: أن تظهر رقاقة فارغة لأنّ الإطارات
 * أُطفئت أسوأ ممّا اختاره المستخدم.
 */
@Composable
private fun HudBadge(
    reading: HudReading?,
    fields: List<HudField>,
    accent: Color,
    textSizeSp: Float,
    arrangement: HudArrangement,
    trailing: (@Composable () -> Unit)? = null
) {
    val hero = fields.firstOrNull { it == HudField.Frames } ?: fields.firstOrNull() ?: return
    val rest = fields.filter { it != hero }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm)
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = hudFieldText(reading, hero),
                    color = accent,
                    fontSize = (textSizeSp * 1.7f).sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Spacer(Modifier.width(MaxSpace.xs))
                Text(
                    text = hudFieldLabel(hero),
                    color = InkMuted,
                    fontSize = (textSizeSp * 0.62f).sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    modifier = Modifier.padding(bottom = MaxSpace.hairline)
                )
            }
            // سطر زمن الإطار لا يظهر إلا حين يكون الرقم المعروض هو الإطارات: مع حقل آخر لا معنى له.
            if (hero == HudField.Frames) {
                Text(
                    text = hudFrameTimeText(reading?.frames),
                    color = InkMuted,
                    fontSize = (textSizeSp * 0.62f).sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
            }
        }

        if (rest.isNotEmpty()) {
            // بلا مدّ (`fillMaxWidth`): الرقاقة نافذة على مقاس محتواها، والمدّ داخلها يجعل عرضها
            // يتبع الشاشة المتروكة لا الأرقام.
            if (arrangement == HudArrangement.Line) {
                Row(horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm)) {
                    rest.forEach { field ->
                        HudPair(field, hudFieldText(reading, field), accent, textSizeSp * 0.8f, false)
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(MaxSpace.hairline)) {
                    rest.forEach { field ->
                        HudPair(field, hudFieldText(reading, field), accent, textSizeSp * 0.8f, false)
                    }
                }
            }
        }

        trailing?.invoke()
    }
}

/**
 * صفّ أدوات اللوحة: **تسجيل · إخفاء · إغلاق** — صفّ واحد صغير، وبأيقونة وحدها بلا كلمة.
 *
 * **ولماذا صفّ واحد (أمر المالك، تكملة ١٨٢):** «النافذة المنبثقة كبيرة دون داعي مما يؤثر على
 * الرؤية واللعب». والقياس يؤيّده: كانت اللوحة الافتراضية **ثلاثة صفوف** — البيانات، ثم صفّ
 * تسجيل بارتفاع ٤٨dp **يُعرض دائمًا** وإن لم يُسجَّل شيء، ثم صفّ الزرّين ٤٨dp — أي نحو
 * **١٠٠dp** ارتفاعًا لرقمين، و**٩٦dp** من الزرّين في العرض. والآن صفّ واحد ٣٢dp يحمل الثلاثة.
 *
 * **وتسجيل بلا كلمة:** كان «Record/Stop» بجانب نقطته سطرًا كاملًا. والأيقونة نطقه هنا — دائرة
 * ممتلئة تشتعل حمراء حين يسجّل، ومربّع يوقف — وهو عُرف تراكب الألعاب نفسه، ووصفه المُقروء
 * كامل في `contentDescription`.
 *
 * **والفصل بين الإخفاء والإغلاق هو المقصود:** زرّ واحد يسمّى «إغلاق» ويخفي هو ما يجعل المستخدم
 * يظنّ التراكب انتهى وهو يقرأ العتاد في الخلفية. هنا: الأوّل يرجع بلمسة على الكبسولة، والثاني
 * يُنزل الإشعار ويُنهي الخدمة.
 *
 * **وبلا نصّ:** اللوحة تسكن فوق لعبة، ومساحتها من مساحة اللعبة؛ وكلمة «إخفاء» تُترجم في ٨٤ لغة
 * فيتّسع الصفّ ويضغط الرقم. أمّا الوصف المُقروء فكامل في `contentDescription`.
 *
 * **والحدّ الذي نزلناه بأمر المالك:** `MaxSize.minTouchTarget` (٤٨dp) أرضية اللمس في هذا
 * المستودع، ونزلنا هنا إلى [HudActionSize] (٣٢dp) لأن الزرّين صارا ثلثي اللوحة. وما يعوّض:
 * اللوحة **كلّها تُسحب بالإصبع** فلمسة خاطئة تُحرّك ولا تضرّ · و**الإغلاق متاح من إشعار الخدمة**
 * بلا اللوحة إطلاقًا `stopLabelRes` · و**الإخفاء يترك كبسولة** تُرجع اللوحة بلمسة.
 * **وهو حدّ لهذا السطح وحده** ولا يُنقل إلى شاشة داخل التطبيق.
 */
@Composable
private fun HudActions(
    recording: Boolean,
    onToggleRecording: (() -> Unit)?,
    onHide: (() -> Unit)?,
    onClose: (() -> Unit)?
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (onToggleRecording != null) {
            HudAction(
                icon = if (recording) Icons.Rounded.Stop else Icons.Rounded.FiberManualRecord,
                tint = if (recording) RecordDot else InkMuted,
                description = stringResource(
                    if (recording) R.string.hud_record_stop else R.string.hud_record_start
                ),
                onClick = onToggleRecording
            )
        }
        if (onHide != null) {
            HudAction(
                icon = Icons.Rounded.VisibilityOff,
                tint = Ink,
                description = stringResource(R.string.hud_action_hide_cd),
                onClick = onHide
            )
        }
        if (onClose != null) {
            HudAction(
                icon = Icons.Filled.Close,
                tint = RecordDot,
                description = stringResource(R.string.hud_action_close_cd),
                onClick = onClose
            )
        }
    }
}

/** زرّ واحد في صفّ الأدوات: دائرة بمقاس موحّد، والأيقونة أصغر منها فتبقى حولها مساحة لمس. */
@Composable
private fun HudAction(icon: ImageVector, tint: Color, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(HudActionSize)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(MaxSize.iconGlyphSmall)
        )
    }
}

/**
 * الكبسولة: ما يبقى بعد الإخفاء — دائرة صغيرة بأيقونة عين، ولمسها يُعيد اللوحة.
 *
 * **ولماذا لا يختفي كل شيء:** «إخفاء» بلا أثر يترك المستخدم بلا طريق رجوع يعرفه؛ فلا يبقى له
 * إلا فتح شاشة الإعدادات أو إيقاف الخدمة من الإشعار — وكلاهما أثقل من لمسة. والكبسولة لا تأخذ
 * من اللعبة إلا موضع ظفر، وتقول بأيقونتها إن اللوحة ما زالت تعمل.
 */
@Composable
fun HudRestoreTab(onShow: () -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(R.string.hud_action_show_cd)
    Box(
        modifier = modifier
            .size(MaxSize.minTouchTarget)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(role = Role.Button, onClick = onShow)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Visibility,
            contentDescription = null,
            tint = Ink,
            modifier = Modifier.size(MaxSize.iconGlyph)
        )
    }
}

/** شريط: أزواج «تسمية قيمة» في سطر واحد أو في عمود. */
@Composable
private fun HudStrip(
    reading: HudReading?,
    fields: List<HudField>,
    accent: Color,
    textSizeSp: Float,
    arrangement: HudArrangement,
    trailing: (@Composable () -> Unit)? = null
) {
    val pairs = fields.map { it to hudFieldText(reading, it) }
    if (pairs.isEmpty()) return
    if (arrangement == HudArrangement.Line) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm)) {
            pairs.forEach { (field, value) -> HudPair(field, value, accent, textSizeSp, false) }
            trailing?.invoke()
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(MaxSpace.hairline)) {
            pairs.forEach { (field, value) -> HudPair(field, value, accent, textSizeSp, true) }
        }
    }
}

@Composable
private fun HudPair(
    field: HudField,
    value: String,
    accent: Color,
    textSizeSp: Float,
    spread: Boolean
) {
    Row(
        modifier = if (spread) Modifier.fillMaxWidth() else Modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (spread) Arrangement.SpaceBetween else Arrangement.Start
    ) {
        Text(
            text = hudFieldLabel(field),
            color = InkMuted,
            fontSize = (textSizeSp * 0.72f).sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.width(MaxSpace.xs))
        Text(
            text = value,
            color = accent,
            fontSize = textSizeSp.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

/**
 * لوح: صفوف داخل إطار. وفي [HudArrangement.Line] تصير الحقول بلاطات متجاورة — وهي الحالة
 * التي يقرأها من يريد كل القيم دفعة واحدة على شاشة عريضة.
 */
@Composable
private fun HudPane(
    reading: HudReading?,
    fields: List<HudField>,
    accent: Color,
    textSizeSp: Float,
    arrangement: HudArrangement
) {
    if (fields.isEmpty()) return
    if (arrangement == HudArrangement.Stack) {
        Column(
            modifier = Modifier
                .border(MaxSize.hairlineBorder, PanelTrack, RoundedCornerShape(maxRadius(MaxRadius.chip)))
                .padding(MaxSpace.xs),
            verticalArrangement = Arrangement.spacedBy(MaxSpace.hairline)
        ) {
            fields.forEach { field ->
                HudPair(field, hudFieldText(reading, field), accent, textSizeSp, true)
            }
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(MaxSpace.xs)) {
            fields.forEach { field ->
                Column(
                    modifier = Modifier
                        .border(MaxSize.hairlineBorder, PanelTrack, RoundedCornerShape(maxRadius(MaxRadius.chip)))
                        .padding(MaxSpace.xs),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = hudFieldText(reading, field),
                        color = accent,
                        fontSize = textSizeSp.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = hudFieldLabel(field),
                        color = InkMuted,
                        fontSize = (textSizeSp * 0.6f).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * حلقة: قوس يمثّل الإطارات الحالية من أقصى ما تعرفه الشاشة، ورقمها في المركز، وبقيّة الحقول
 * سطرًا تحتها.
 *
 * والقوس **مقياس لا زينة**: الطول يُقارَن بمعدّل تحديث الشاشة المقروء من العرض نفسه
 * (`View.display.refreshRate`) — وحين لا يُعرف يُعلَن السقف الاحتياطي (١٢٠) في الكود لا في
 * صمت المستخدم.
 */
@Composable
private fun HudRing(
    reading: HudReading?,
    fields: List<HudField>,
    accent: Color,
    textSizeSp: Float,
    ceiling: Float
) {
    val frames = reading?.frames
    val fraction = ((frames ?: 0f) / ceiling).coerceIn(0f, 1f)
    val diameter = (textSizeSp * 4.2f).dp
    val secondary = fields.filter { it != HudField.Frames }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(diameter)) {
                val stroke = size.minDimension * 0.11f
                drawArc(
                    color = PanelTrack,
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                if (frames != null) {
                    drawArc(
                        color = accent,
                        startAngle = 135f,
                        sweepAngle = 270f * fraction,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = hudFieldText(reading, HudField.Frames),
                    color = accent,
                    fontSize = (textSizeSp * 1.35f).sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = hudFieldLabel(HudField.Frames),
                    color = InkMuted,
                    fontSize = (textSizeSp * 0.6f).sp,
                    maxLines = 1
                )
            }
        }
        if (secondary.isNotEmpty()) {
            Spacer(Modifier.height(MaxSpace.hairline))
            Row(horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm)) {
                secondary.forEach { field ->
                    HudPair(field, hudFieldText(reading, field), accent, textSizeSp * 0.8f, false)
                }
            }
        }
    }
}

/**
 * منحنى الحقل المعروض — **وشكله من نوع ما يقيسه**، لا شكل واحد لكل شيء.
 *
 * المرجع (Scene) يفصل لكل مقياس رسمًا لأنّ الأنواع لا تتشابه سطحيًّا: معدّل يتحرّك كل لحظة،
 * وحرارة تقفز درجاتٍ ثم تسكن، ونسبة تقطيعٍ تُقرأ حصصًا لا خطًّا، وترددٌ يثبت على درجة ثم
 * ينتقل. رسمُ الأربعة بخطّ واحد يُلغي هذه الفروق ويجعل العين تقرأ الهيئة نفسها في كل مرة.
 * فالشكل هنا **يعبّر عن نوع القياس**: خطٌّ لمعدّل، ومساحةٌ لحمل، وأعمدةٌ لنسبة، ودرجٌ لدرجة
 * أو تردد. وليس مقحَمًا: قيمة غائبة لا تُرسم ولا تُخترع نقطة مكانها (ADR-07).
 *
 * **ولا محور واحد في هذا الرسم** — وهذا اختيار: الرقم المجاور في اللوحة هو القياس ووحدته،
 * والرسم يصف الحركة فقط. السقف يُمرَّر محسوبًا (`hudChartCeiling`) فيصبغ الشكل ولا يُعلن.
 *
 * والطول: `Frames` يتبع نافذة الإعدادات كما كان، وبقية الحقول تحتفظ بآخر
 * [HUD_SERIES_SAMPLES] عيّنة محليًّا — لم يُغيَّر مصدر ولا خدمة ولا نموذج عرض.
 */
@Composable
private fun HudMetricGraph(
    series: List<Float>,
    accent: Color,
    shape: HudChartShape,
    ceiling: Float,
) {
    if (series.size < 2) return
    Canvas(modifier = Modifier.fillMaxWidth().height(MaxSize.sparklineHeight)) {
        val safeCeiling = if (ceiling.isFinite() && ceiling > 0f) ceiling else 1f
        val span = series.size
        val step = size.width / (span - 1).coerceAtLeast(1)
        val points = series.mapIndexed { index, value ->
            val v = if (value.isFinite()) value else safeCeiling
            Offset(x = index * step, y = size.height - (v / safeCeiling).coerceIn(0f, 1f) * size.height)
        }
        when (shape) {
            HudChartShape.Line -> drawSeriesLine(points, accent)

            HudChartShape.Area -> {
                val fill = Path().apply {
                    moveTo(points.first().x, size.height)
                    points.forEach { lineTo(it.x, it.y) }
                    lineTo(points.last().x, size.height)
                    close()
                }
                drawPath(path = fill, color = accent.copy(alpha = AreaFillAlpha))
                drawSeriesLine(points, accent)
            }

            HudChartShape.Bars -> {
                val barWidth = (step * BarWidthShare).coerceAtLeast(MinBarWidth)
                points.forEach { point ->
                    val height = (size.height - point.y).coerceAtLeast(0f)
                    drawRect(
                        color = accent,
                        topLeft = Offset(point.x - barWidth / 2f, point.y),
                        size = Size(barWidth, height)
                    )
                }
            }

            HudChartShape.Stepped -> {
                val path = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    var previous = points.first().y
                    points.drop(1).forEach { point ->
                        lineTo(point.x, previous)
                        lineTo(point.x, point.y)
                        previous = point.y
                    }
                }
                drawPath(path = path, color = accent, style = Stroke(width = GraphStroke, cap = StrokeCap.Round))
            }
        }
    }
}

/** خطّ متّصل بين النقاط — الشكل المشترك لـ[HudChartShape.Line] و[HudChartShape.Area]. */
private fun DrawScope.drawSeriesLine(points: List<Offset>, accent: Color) {
    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
        points.drop(1).forEach { lineTo(it.x, it.y) }
    }
    drawPath(path = path, color = accent, style = Stroke(width = GraphStroke, cap = StrokeCap.Round))
}

/**
 * الحقل الذي يُرسم له منحنى: **أوّل حقل معروض يقبل رقمًا**، و[HudField.Renderer] نصّ فلا شكل له.
 *
 * وهذا هو جوهر التحسين: قبل اليوم كان الرسم لا يظهر إلا إذا كان `Frames` بين الحقول، فمن
 * عرض الحرارة أو الحمل بقي بلا شكل.
 */
private fun hudChartField(fields: List<HudField>): HudField? =
    fields.firstOrNull { it != HudField.Renderer }

/** شكل الرسم بحسب نوع القياس — لا بحسب الذوق. */
private fun hudChartShape(field: HudField): HudChartShape = when (field) {
    // معدّلات تتحرّك كل لحظة: الخطّ يصف حركتها بلا ملء يوهم بثقل.
    HudField.Frames, HudField.Stability, HudField.Power,
    HudField.Battery, HudField.BatteryVoltage, HudField.Network, HudField.DiskIo -> HudChartShape.Line

    // أحمال تشغل نسبة من كلٍّ: الملء يقول «كم من السقف مشغول» بنظرة.
    HudField.Cpu, HudField.Ram, HudField.GpuLoad -> HudChartShape.Area

    // نسبة تقطيع: حصص إطارات متتالية، والأعمدة تقرأ حصصًا لا ميلًا.
    HudField.Jank -> HudChartShape.Bars

    // درجات وترددات تقفز ثم تسكن: الدرج يمنع إيهام التدرّج بين قيمتين منفصلتين.
    HudField.Heat, HudField.SocTemp, HudField.CpuFreq, HudField.GpuFreq, HudField.DdrFreq ->
        HudChartShape.Stepped

    HudField.Renderer -> HudChartShape.Line
}

/**
 * سقف الرسم: ثابت حين يكون القياس نسبةً من معروف، ومشتقًّا من العيّنة نفسها حين لا سقف طبيعي.
 *
 * ولا يُعلن كرقم على الشاشة: هو صبغة شكل فقط. وحين لا عيّنة موجبة بعد يُرجع ١ لا صفرًا،
 * فلا قسمة على صفر ولا خطّ مسطّح كاذب.
 */
private fun hudChartCeiling(field: HudField, displayCeiling: Float, series: List<Float>): Float {
    return when (field) {
        HudField.Frames -> displayCeiling
        HudField.Stability, HudField.Jank, HudField.Cpu, HudField.GpuLoad, HudField.Battery -> 100f
        else -> {
            val peak = series.filter { it.isFinite() }.maxOrNull() ?: 0f
            if (peak > 0f) peak * SeriesCeilingHeadroom else 1f
        }
    }
}

/**
 * الرقم الذي يُرسم للحقل — لا النصّ المعروض.
 *
 * `HudReading.valueOf` يعطي الشبكة والقرص اتجاه النزول وحده، وهنا يُرسم الاتجاه نفسه:
 * منحنى الشبكة/القرص يصف **النزول/القراءة**، والرقم المجاور يقول الاتجاهين معًا.
 * والغائب `null` فلا نقطة، والنصّ (`Renderer`) لا يُرسم إطلاقًا.
 */
private fun hudFieldValue(reading: HudReading?, field: HudField): Float? {
    if (reading == null) return null
    return when (field) {
        HudField.Frames -> reading.frames
        HudField.Stability -> reading.stability?.toFloat()
        HudField.Jank -> reading.jankPercent?.toFloat()
        HudField.Cpu -> reading.cpu?.toFloat()
        HudField.Heat -> reading.heat
        HudField.Ram -> reading.ramMb?.toFloat()
        HudField.Power -> reading.watt
        HudField.Battery -> reading.battery?.toFloat()
        HudField.Network -> reading.rxBps ?: reading.txBps
        HudField.Renderer -> null
        HudField.CpuFreq -> reading.cpuFreqMhz?.toFloat()
        HudField.SocTemp -> reading.socTempC
        HudField.GpuFreq -> reading.gpuFreqMhz?.toFloat()
        HudField.GpuLoad -> reading.gpuLoadPercent?.toFloat()
        HudField.DdrFreq -> reading.ddrFreqMhz?.toFloat()
        HudField.DiskIo -> reading.diskReadBps ?: reading.diskWriteBps
        HudField.BatteryVoltage -> reading.batteryVoltageMv?.toFloat()
    }
}

/**
 * تاريخ الحقل المعروض. `Frames` يبقى على نافذته المضبوطة في الإعدادات (لا يُلمس سلوكه)،
 * وبقية الحقول تُجمع هنا: `HudReading` يحمل كل المقاييس في كل نبضة، فلا حاجة إلى تغيير
 * الخدمة ولا نموذج العرض لرسمها.
 *
 * ولا يُضاف شيء حين تكون القيمة غائبة: التاريخ لا يُملأ بفراغ. والغازل الواحدة تُرسم خطًّا
 * مسطّحًا من نقطتين متماثلتين — قياسٌ واحد معروض كقياس، لا محور مُختلق.
 */
@Composable
private fun rememberHudSeries(
    field: HudField?,
    reading: HudReading?,
    framesHistory: List<Float>,
): List<Float> {
    val samples = remember(field) { mutableStateListOf<Float>() }
    val value = if (field == null || field == HudField.Frames) null else hudFieldValue(reading, field)
    LaunchedEffect(field, reading, value) {
        if (value != null) {
            samples.add(value)
            while (samples.size > HUD_SERIES_SAMPLES) samples.removeAt(0)
        }
    }
    return when {
        field == null -> emptyList()
        field == HudField.Frames -> framesHistory
        samples.isEmpty() -> emptyList()
        samples.size == 1 -> listOf(samples[0], samples[0])
        else -> samples.toList()
    }
}

/**
 * بلاطة حقل في شاشة الإعدادات: **تُظهر القيمة الحيّة وتُشغّل الحقل معًا**.
 *
 * وهي البديل المقصود لصفّ مفاتيح: صفّ المفاتيح يقول «مُشغَّل» ولا يقول ماذا ستعرض، فالمستخدم
 * يُشغّل حقلًا ثم يخرج ليرى أثره. والبلاطة تجعل كل حقل يرتدي قيمته الآن، فيُقرأ الاختيار
 * بلا خروج من الشاشة.
 */
@Composable
fun HudFieldTile(
    field: HudField,
    reading: HudReading?,
    selected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val label = hudFieldLabel(field)
    val selectedLabel = stringResource(R.string.hud_field_shown)
    val unselectedLabel = stringResource(R.string.hud_field_hidden)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(maxRadius(MaxRadius.tile)))
            .background(if (selected) scheme.primary.copy(alpha = 0.14f) else scheme.surfaceContainerLow)
            .border(
                width = if (selected) MaxSize.activeRing else MaxSize.hairlineBorder,
                color = if (selected) scheme.primary else scheme.outlineVariant,
                shape = RoundedCornerShape(maxRadius(MaxRadius.tile))
            )
            .toggleable(
                value = selected,
                role = Role.Checkbox,
                onValueChange = { onToggle() }
            )
            .semantics {
                contentDescription = label
                stateDescription = if (selected) selectedLabel else unselectedLabel
            }
            .padding(MaxSpace.sm),
        verticalArrangement = Arrangement.spacedBy(MaxSpace.hairline)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(lineBreak = LineBreak.Heading),
                color = if (selected) scheme.primary else scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier.size(MaxSize.iconGlyphSmall)
                )
            }
        }
        Text(
            text = hudFieldText(reading, field),
            style = MaterialTheme.typography.titleMedium,
            color = if (selected) scheme.onSurface else scheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun framesCeiling(): Float {
    val view = LocalView.current
    return remember(view) {
        val hz = runCatching { view.display?.refreshRate }.getOrNull()
        hz?.takeIf { it > 1f }?.coerceAtLeast(60f) ?: FALLBACK_CEILING
    }
}
