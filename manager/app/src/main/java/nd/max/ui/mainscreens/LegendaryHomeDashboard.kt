/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.mainscreens

import nd.max.ui.design.MaxSize

import androidx.compose.material3.MaterialTheme

import androidx.compose.foundation.shape.CircleShape

import nd.max.ui.theme.MonoValueStyleSmall

import nd.max.ui.theme.MonoValueStyleFrequency

import nd.max.ui.theme.MonoValueStyleLarge

import nd.max.ui.design.MaxAlpha

import nd.max.ui.component.neuralClickable

import nd.max.ui.component.NeuralValue

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nd.max.core.module.ModuleBootstrap
import nd.max.ui.util.RootUtils
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxSpace
import nd.max.R
import nd.max.core.maxai.MaxAiState
import nd.max.core.privilege.PrivilegeLevel
import nd.max.ui.component.NeuralSectionHeader
import nd.max.ui.component.NeuralSparkline
import nd.max.ui.component.NeuralIconChip
import nd.max.ui.component.NeuralPanel
import nd.max.ui.component.NeuralPill
import nd.max.ui.component.neuralPalette
import nd.max.ui.navigation.MaxDestination
import nd.max.ui.viewmodel.DaemonHealth
import nd.max.ui.viewmodel.DashboardState
import nd.max.ui.viewmodel.HomeUiState
import nd.max.ui.viewmodel.MemoryBoostState
import nd.max.ui.util.LoadSample
import kotlin.math.roundToInt
import nd.max.ui.design.maxRadius

/**
 * The MAX "Now" dashboard.
 *
 * Question the screen answers: how is the device right now, and what can I do about it in one tap?
 * Reading order (طلب المالك): every block earns its place once, and nothing repeats another block.
 *
 *  0. header   — brand, engine state, guide, power and settings.
 *  1. hero     — the first card, compact: device, access row, temperature, three one-line tiles, device overview.
 *  2. pulse    — CPU and GPU: load, clock and the frequency history beneath them.
 *  3. activity — what your settings are doing now, right under CPU and GPU: a plain summary, the changes,
 *      and one door per row. It always shows something: an empty state says the device runs on defaults.
 *  4. actions  — Boost (measured before/after) and GameSpace (the lobby), side by side, below the activity.
 *  5. vitals   — the readings no other block shows: CPU, GPU and surface temperature, and network speed.
 *  6. focus    — storage almost full (hideable), or heat / memory when those are the real problem.
 *  7. memory   — RAM, ZRAM and storage capacity, each row a door.
 *  8. cleaner  — storage fullness and the Ultra Cleaner action.
 *  9. deck     — the control platform: the destinations people reach for, in compact rows.
 *
 * **بلا حركة على البطاقات:** لا بانرات متحركة ولا دخول متتابع ولا تبديل مشهد بانزلاق.
 *
 * Every color comes from MaterialTheme through neuralPalette(), so the Settings theme drives the entire screen.
 */
@Composable
internal fun LegendaryHomeDashboard(
    timeLeftMinutes: Int? = null,
    ui: HomeUiState,
    dashboard: DashboardState,
    maxAi: MaxAiState,
    deviceName: String,
    // بطاقات المنصة تُمرَّر ولا تُكتب هنا (أمر المالك): القاعدة في `homeDeckSelection` وحدها.
    deckEntries: List<HomeDeckEntry>,
    onOpenDeck: (HomeDeckEntry) -> Unit,
    modifier: Modifier = Modifier,
    onNavigate: (String) -> Unit,
    onReboot: () -> Unit,
    onSettings: () -> Unit,
    /** طبقة الامتياز المكتشفة — من `PrivilegeManager` الموجود لا من قراءة ثانية. */
    accessLevel: PrivilegeLevel,
    /** حالة «تعزيز الذاكرة» الأخيرة، تُعرض على بلاطته. */
    boost: MemoryBoostState,
    onBoost: () -> Unit,
    /** `?` في الرأس: يعيد جولة أول فتح بطلب صريح. */
    onShowGuide: () -> Unit,
    /** بطاقة «التخزين يكاد يمتلئ»: تظهر حين تُستحقّ ولم يُخفِها المستخدم (`HomeFocusModel`). */
    storageCardVisible: Boolean = false,
    onHideStorage: () -> Unit = {},
    /** فتح نافذة وضع الوصول من شارة البطل. */
    onAccess: () -> Unit = {},
    /** بعد كتابة الوحدة بلا إنترنت: تُعاد قراءة الحالة من مصدرها، فلا يبقى الصفّ والزرّ قديمين. */
    onModuleWritten: () -> Unit = {},
) {
    // «مفعّل» إثبات أنّ المسار المختار يعمل بشرطيه، لا زينة بصرية (أمر المالك ٢٠٢٦-١١-١٦):
    // جذر بلا وحدة = نصف منظومة، وShizuku لا يصل هنا إلا بإذنه ممنوحًا. القرار في `HomeActivityRules`.
    val active = HomeActivityRules.active(accessLevel, ui.moduleInstalled, ui.moduleUpdatePending)
    // ترتيب حسب الخطورة: بطاقة الخلل الحقيقي تصعد تحت البطل، وإلا تبقى في موضعها المحدّد.
    val focusFirst = HomeFocusModel.focusKind(
        heatC = deviceHeatC(dashboard) ?: 0,
        ramFraction = fractionOf(dashboard.ramUsedMb, dashboard.ramTotalMb),
        storageVisible = storageCardVisible,
    ) != null
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(MaxSpace.lg)) {
        HomeHeader(
            active = active,
            accessLevel = accessLevel,
            moduleInstalled = ui.moduleInstalled,
            daemonHealth = ui.daemonHealth,
            moduleUpdatePending = ui.moduleUpdatePending,
            onReboot = onReboot,
            onShowGuide = onShowGuide,
            onModuleWritten = onModuleWritten,
        )
        // المرساة تُعلَّق من الجذر (`LocalHomeTourTargets`) لا من وسيط في التوقيع: نفس المرساة
        // تعيش في أربع شاشات، وتوقيعٌ يحملها كان يجعل كل شاشة تعرف بجولة تعرضها أخرى.
        HomeTourAnchor(HomeTourTarget.Hero) {
            HomeHeroCard(
                deviceName = deviceName,
                dashboard = dashboard,
                accessLevel = accessLevel,
                // من مسارها الحقيقي (`MaxAiState.aiEnabled`) لا مخمَّنة من وجود الشاشة.
                aiEnabled = maxAi.aiEnabled,
                // نظرة الجهاز: المالك الوحيد لفكرة «نظرة على الجهاز» (طلب المالك).
                onOverview = { onNavigate(MaxDestination.DeviceInfo.route) },
                onMaxAi = { onNavigate(MaxDestination.MaxAi.route) },
                onAccess = onAccess,
            )
        }
        // «مدار الطاقة» (أنيميشن البطّارية أثناء الشحن) — **مُخفٰى بأمر المالك ٢٠٢٦-١٠-٢١**
        // («أخفِ انيميشن البطاريه في الشاشة الرئيسية»): صار غير مستدعًى هنا، والملفّ
        // [HomeOrbitPowerCard] بقي كما هو فلا يُحذف عملٌ مُنجز (إرجاعه بسطر واحد)، وشرطُه
        // القديم كان الشحن وحده.
        if (focusFirst) {
            FocusCard(dashboard, onNavigate, storageCardVisible, onHideStorage)
        }
        HomeTourAnchor(HomeTourTarget.Pulse) {
            HardwarePulseCards(
                dashboard = dashboard,
                onCpu = { onNavigate(MaxDestination.CpuCoreControl.route) },
                onGpu = { onNavigate(MaxDestination.GpuStudio.route) },
            )
        }
        // **ترتيب المالك (2026-11-20):** المعالج والرسوم جنبًا إلى جنب ← الضغط الحراري
        // (هامش الخنق في لوح القراءات) ← امتلاء المساحة (مصفوفة الذاكرة) ← النشاط الحالي ← الباقي.
        // كان النشاط يسبق القراءات والمصفوفة، فيُقرأ سطرُ «ماذا يفعل» قبل الأرقام التي يفسّرها.
        Column(verticalArrangement = Arrangement.spacedBy(MaxSpace.md)) {
            NeuralSectionHeader(
                title = stringResource(R.string.home_section_live),
                caption = stringResource(R.string.home_section_live_caption),
            )
            HomeTourAnchor(HomeTourTarget.Vitals) {
                HomeVitalsGrid(dashboard = dashboard, onNavigate = onNavigate)
            }
            timeLeftMinutes?.let { minutes ->
                Text(
                    stringResource(
                        R.string.home_time_left,
                        stringResource(R.string.home_duration_hm, minutes / 60, minutes % 60),
                    ),
                    color = neuralPalette().muted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                )
            }
        }
        HomeTourAnchor(HomeTourTarget.Memory) {
            MemoryMatrixCard(dashboard = dashboard, onNavigate = onNavigate)
        }
        UnifiedActivityCard(maxAi = maxAi, onNavigate = onNavigate)
        // صفّ الفعل **أسفل** CPU/GPU وبعد «النشاط الحالي» (أمر المالك ٢٠٢٦-١١-٢٣): النشاط
        // يفصل بين الأرقام التي تُقرأ وبين الفعلين، فلا يُقرأ الزرّ قبل معرفة ما يجري الآن.
        // وكان فوق النبض منذ تدقيق ٢٠٢٦-١١-١٨، فعاد إلى ما طلبه المالك بعد أن صار الفعل تابعه.
        // و`HomeShapeContractTest` و`HomeTourModel` يقرآن هذا الترتيب فيُحدَّثان معه.
        Column(verticalArrangement = Arrangement.spacedBy(MaxSpace.md)) {
            NeuralSectionHeader(title = stringResource(R.string.home_section_actions))
            HomeTourAnchor(HomeTourTarget.Actions) {
                HomeActionRow(
                    boost = boost,
                    onBoost = onBoost,
                    onGameSpace = { onNavigate(MaxDestination.GameLobby.route) },
                )
            }
        }
        if (!focusFirst) {
            FocusCard(dashboard, onNavigate, storageCardVisible, onHideStorage)
        }
        HomeTourAnchor(HomeTourTarget.Deck) {
            CommandDeck(entries = deckEntries, onOpen = onOpenDeck)
        }
    }
}



/**
 * Brand, one state, three actions: the brand itself opens the free-text window.
 * The engine state is said once: it used to be a pill
 * ("active/idle", dot and color) *and* a subtitle line saying the same thing in words,
 * which is the definition of a screen that repeats itself. The pill keeps more
 * information (color, dot, localized word), so the duplicate line went.
 */
@Composable
private fun HomeHeader(
    active: Boolean,
    accessLevel: PrivilegeLevel,
    moduleInstalled: Boolean,
    daemonHealth: DaemonHealth,
    moduleUpdatePending: Boolean,
    onReboot: () -> Unit,
    onShowGuide: () -> Unit,
    onModuleWritten: () -> Unit,
) {
    val p = neuralPalette()
    // نافذة الكتابة تُفتح من الكلمة نفسها، فحالتها هنا.
    var noteOpen by rememberSaveable { mutableStateOf(false) }
    // ونافذة «لماذا غير مفعّل؟» تُفتح من الشارة نفسها — سببٌ لا إجراء.
    var reasonOpen by rememberSaveable { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                "MAX",
                color = p.text,
                fontSize = 26.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.4.sp,
                // الكلمة زرّ: تفتح نافذة الكتابة الحرّة.
                modifier = Modifier.neuralClickable(onClick = { noteOpen = true }, role = Role.Button),
            )
        }
        // الشارة زرّ الآن: الضغط يشرح **سبب** الحالة لا يبدّلها (أمر المالك: «نافذة منبثقة بالسبب»).
        HomeStatusChip(
            text = stringResource(if (active) R.string.home_active else R.string.home_not_active),
            accent = if (active) p.ok else p.danger,
            modifier = Modifier.neuralClickable(
                onClick = { reasonOpen = true },
                role = Role.Button,
            ),
        )
        Spacer(Modifier.width(8.dp))
        // **`?` قبل الزرّ بأمر §5.1:** الجولة تُطلب من الرأس لا من داخل البطاقة، ومن
        // أتمّها ثم أرادها يعود إلى هنا — فلا بحث في الشاشة عن مفتاح إعادة العرض.
        HeaderButton(
            Icons.Rounded.HelpOutline,
            stringResource(R.string.home_banner_restore),
            onShowGuide
        )
        Spacer(Modifier.width(8.dp))
        HeaderButton(Icons.Rounded.PowerSettingsNew, stringResource(R.string.max_home_power), onReboot)
    }
    if (noteOpen) {
        HomeNoteWindow(onClose = { noteOpen = false })
    }
    if (reasonOpen) {
        HomeStatusReasonDialog(
            active = active,
            accessLevel = accessLevel,
            moduleInstalled = moduleInstalled,
            daemonHealth = daemonHealth,
            moduleUpdatePending = moduleUpdatePending,
            onClose = { reasonOpen = false },
            onModuleWritten = onModuleWritten,
        )
    }
}

@Composable
private fun HeaderButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    val p = neuralPalette()
    // 13dp لم تكن على سلّم نصف القطر؛ مرامي التحكّم هو نفسه نصف قطر التحكّم في العقد.
    val shape = RoundedCornerShape(maxRadius(MaxRadius.control))
    Box(
        Modifier
            .size(38.dp)
            .clip(shape)
            .background(p.tile)
            .border(BorderStroke(1.dp, p.border), shape)
            .neuralClickable(onClick, role = Role.Button),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, description, Modifier.size(18.dp), tint = p.muted)
    }
}

/**
 * The signature block. One headline reading (heat plus a plain-language verdict)
 * over three pressure meters, because the first question is never "what is CPU
 * load" but "is anything under pressure, and is the device hot". Straight bars
 * beat a dial here: they share one baseline, so three values are comparable at a
 * glance and every label has room to breathe in either writing direction.
 */
/**
 * CPU/GPU live hardware pair for the home screen.
 *
 * The card intentionally shows load as the headline and the *actual current clock*
 * underneath it. The sparkline is frequency history, not load history, so a user can
 * immediately see clock stepping/boost behavior instead of getting a second copy of
 * the same percentage graph.
 */
@Composable
private fun HardwarePulseCards(
    dashboard: DashboardState,
    onCpu: () -> Unit,
    onGpu: () -> Unit
) {
    // العرض الضيّق (النافذة العائمة ١٧٨dp مثلًا) كان يترك المقياسين جنبًا إلى جنب بعرض ~٨٩dp
    // لكل بطاقة: العنوان يُقصّ والأرقام تخرج من إطارها. أقلّ من 300dp ⇒ تكديس عموديّ كامل العرض.
    // وقيس على جهاز المالك (2026-11-20): شاشة 375dp بسلك 520 ⇒ المحتوى بعد الحشو ~335dp،
    // وكانت العتبة 340dp فتُكدَّس البطاقتان وتخالف أمر المالك «GPU وCPU جنبًا إلى جنب».
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val stacked = maxWidth < 300.dp
        if (stacked) {
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(MaxSpace.sm),
            ) {
                GpuPulseCard(dashboard, onGpu, Modifier.fillMaxWidth())
                CpuPulseCard(dashboard, onCpu, Modifier.fillMaxWidth())
            }
        } else {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm),
            ) {
                GpuPulseCard(dashboard, onGpu, Modifier.weight(1f))
                CpuPulseCard(dashboard, onCpu, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun GpuPulseCard(
    dashboard: DashboardState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FrequencyMetricCard(
        title = stringResource(R.string.detail_zone_gpu),
        percent = dashboard.gpuLoadPercent,
        frequencyMhz = dashboard.gpuFreqMhz,
        ceilingMhz = dashboard.gpuCeilingMhz,
        floorMhz = dashboard.gpuMinMhz,
        samples = dashboard.loadSamples,
        isGpu = true,
        // **التردد الجاري هو الرقم الأول، والسقف بجانبه موسومًا.** كان الصدر يعرض السقف وحده
        // (520) فقرأه المالك على أنه «التردد الحالي» وقارنه بـ260 في شاشة GPU فبدا الرقمان
        // متناقضين — وهما كمّيتان مختلفتان. صار الصدر «الآن 260» ثم «السقف 520» في صفٍّ واحد.
        topMhz = dashboard.gpuFreqMhz?.takeIf { it > 0 },
        topLabel = stringResource(R.string.home_freq_now),
        secondaryTopMhz = dashboard.gpuCeilingMhz?.takeIf { it > 0 },
        secondaryTopLabel = stringResource(R.string.home_gpu_max_freq),
        rangeMaxMhz = dashboard.gpuMaxSupportedMhz,
        accent = neuralPalette().accentAlt,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun CpuPulseCard(
    dashboard: DashboardState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FrequencyMetricCard(
        title = stringResource(R.string.detail_zone_cpu),
        percent = dashboard.cpuLoadPercent,
        frequencyMhz = dashboard.cpuTopCoreMhz.takeIf { it > 0 },
        ceilingMhz = dashboard.cpuCeilingMhz.takeIf { it > 0 },
        floorMhz = dashboard.cpuMinMhz?.takeIf { it > 0 },
        samples = dashboard.loadSamples,
        isGpu = false,
        // «الآن» تسمية التردد الجاري لأنويتها العليا — كان رقمًا عاريًا يُقرأ كأنه سقف.
        topMhz = dashboard.cpuTopCoreMhz.takeIf { it > 0 },
        topLabel = stringResource(R.string.home_freq_now),
        accent = neuralPalette().accent,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun FrequencyMetricCard(
    title: String,
    percent: Int?,
    frequencyMhz: Int?,
    ceilingMhz: Int?,
    floorMhz: Int?,
    samples: List<LoadSample>,
    isGpu: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    topMhz: Int? = null,
    topLabel: String? = null,
    rangeMaxMhz: Int? = null,
    secondaryTopMhz: Int? = null,
    secondaryTopLabel: String? = null,
) {
    val p = neuralPalette()
    val current = frequencyMhz?.takeIf { it > 0 }
    val history = samples.mapNotNull { sample ->
        val value = if (isGpu) sample.gpuMhz else sample.cpuMhz
        value?.takeIf { it > 0 }?.toFloat()
    }
    // مقياس الرسم = المدى المقيس أو المعلن، لا صفر مصنوع (انظر التعليق في `HardwarePulseCards`).
    val rangeCeiling = max(
        ceilingMhz?.takeIf { it > 0 }?.toFloat() ?: 0f,
        max(current?.toFloat() ?: 0f, history.maxOrNull() ?: 0f)
    )
    val graph = if (rangeCeiling > 0f) {
        samples.mapNotNull { sample ->
            val value = if (isGpu) sample.gpuMhz else sample.cpuMhz
            value?.takeIf { it > 0 }?.toFloat()?.div(rangeCeiling)
        }.takeLast(36)
    } else emptyList()
    val floor = floorMhz?.takeIf { it > 0 }
    val floorFraction = floor?.toFloat()?.div(rangeCeiling)?.takeIf { it.isFinite() }?.coerceIn(0f, 1f)
    val rangeMax = rangeMaxMhz?.takeIf { it > 0 } ?: ceilingMhz?.takeIf { it > 0 }
    val shape = RoundedCornerShape(maxRadius(MaxRadius.group))
    val unknown = "\u2014"

    Column(
        modifier
            .clip(shape)
            .background(p.tile.copy(alpha = .92f))
            .border(MaxSize.hairlineBorder, accent.copy(alpha = MaxAlpha.borderStrong), shape)
            .neuralClickable(onClick, role = Role.Button)
            .padding(MaxSpace.lg),
        verticalArrangement = Arrangement.spacedBy(MaxSpace.sm),
    ) {
        // الرأس: النقطة والاسم وسهم الباب، والنسبة الكبيرة (القراءة الرئيسية للبطاقة) على اليمين.
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // العنوان المرن يأخذ ما تبقّى، والنسبة تُقاس أولًا بعرضها الكامل: بلا `weight`
            // هنا كان عنوان GPU الأطول يزاحم «100%» فتختفي النسبة (قيس باللقطة ٢٠٢٦-١١-٢٣).
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(MaxSpace.sm).clip(CircleShape).background(accent))
                Spacer(Modifier.width(MaxSpace.xs))
                Text(
                    title,
                    modifier = Modifier.weight(1f),
                    color = p.muted,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(MaxSpace.xs))
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    null,
                    Modifier.size(MaxSize.iconGlyphSmall),
                    tint = accent,
                )
            }
            Spacer(Modifier.width(MaxSpace.sm))
            NeuralValue(
                percent?.coerceIn(0, 100)?.let { "$it%" } ?: unknown,
                style = MonoValueStyleLarge,
                color = p.text,
                // الحركة في هذه البطاقة للخطّ وحده: نبضة الرسم عند كل قراءة. تدحرج الرقم فوقها
                // يفتح عشرة `AnimatedContent` كل دقيقتَي استطلاع ويوهم بدقّة لا يملكها المستشعر.
                ticker = false,
            )
        }
        // سطر التردد: الصدر موسوم دائمًا — «السقف» للـGPU (الحدّ الحيّ) و«الآن» للـCPU (التردد
        // الجاري لأنويتها العليا). بلا التسمية كان الرقم المجرّد يُقرأ كأنه حالة البطاقة نفسها.
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            if (topLabel != null) {
                Text(
                    topLabel,
                    color = p.muted,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
                Spacer(Modifier.width(MaxSpace.xs))
            }
            NeuralValue(
                formatHardwareFrequency(topMhz?.takeIf { it > 0 } ?: current),
                style = MonoValueStyleFrequency,
                color = p.text,
                maxLines = 1,
                ticker = false,
            )
            // قراءة ثانية موسومة في **نفس الصفّ**: «الآن» و«السقف» معًا بدل رقم واحد يُقرأ على
            // أنه الآخر. بهذا تعرض الرئيسية التردد الجاري (كما شاشة GPU) ولا يبقى السقف وحده.
            if (secondaryTopLabel != null) {
                Spacer(Modifier.weight(1f))
                Text(
                    secondaryTopLabel,
                    color = p.muted,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
                Spacer(Modifier.width(MaxSpace.xs))
                NeuralValue(
                    formatHardwareFrequency(secondaryTopMhz?.takeIf { it > 0 }),
                    style = MonoValueStyleFrequency,
                    color = p.text,
                    maxLines = 1,
                    ticker = false,
                )
            }
        }
        // فاصل شعريّ يفصل القراءة عن الرسم.
        Box(Modifier.fillMaxWidth().height(MaxSize.hairlineBorder).background(p.border))
        NeuralSparkline(
            samples = graph,
            accent = accent,
            floorFraction = floorFraction,
            modifier = Modifier.fillMaxWidth().height(40.dp),
        )
        // المدى: طرفان **موسومان** بدل رقمين عاريين (تدقيق الفيديو ٢٠٢٦-١١-١٨). الرقمان كانا
        // يعنيان شيئًا مختلفًا على البطاقتين بلا اسم: CPU = أرضية + سقف معلن، GPU = أرضية + أقصى
        // مدعوم في الكتالوج؛ وبينهما «السقف» الحيّ أعلى البطاقة — فثلاثة «أقصى» في صفٍّ واحد.
        // التسمية تفصل الثلاثة، ولا يُحذف رقم طلبه المالك. السطر يُحجز دائمًا والمجهول شرطة (ADR-07).
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            // الأقصى في جهة البداية (يمين في العربية) والأدنى في الطرف المقابل — الأرقام تُقرأ
            // تصاعديًّا من اليسار، فيبقى الأدنى يسارًا في الاتجاهين كما في الرسم نفسه.
            RangeEnd(
                label = stringResource(R.string.home_freq_max_label),
                value = formatHardwareFrequency(rangeMax),
                horizontalAlignment = Alignment.Start,
            )
            RangeEnd(
                label = stringResource(R.string.home_freq_min_label),
                value = formatHardwareFrequency(floor),
                horizontalAlignment = Alignment.End,
            )
        }
    }
}

/** طرف مدى موسوم: تسمية صغيرة فوق رقم أحادي — تُقرأ كوحدة واحدة لا كرقم عائم. */
@Composable
private fun RangeEnd(
    label: String,
    value: String,
    horizontalAlignment: Alignment.Horizontal,
) {
    val p = neuralPalette()
    Column(horizontalAlignment = horizontalAlignment) {
        Text(label, color = p.muted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        NeuralValue(value, style = MonoValueStyleSmall, color = p.text, maxLines = 1, ticker = false)
    }
}

private fun formatHardwareFrequency(mhz: Int?): String {
    val value = mhz?.takeIf { it > 0 } ?: return "—"
    return if (value >= 1000) {
        val ghz = value / 1000f
        if (ghz >= 10f) "${ghz.toInt()} GHz"
        else "${"%.1f".format(java.util.Locale.US, ghz)} GHz"
    } else "$value MHz"
}


/**
 * لا تظهر إلا حين توجد مشكلة حقيقية، فحضورها نفسه يعني شيئًا.
 *
 * **الفرز كله في `HomeFocusModel.focusKind`:** اختيار الخلل وموضع البطاقة (تحت البطل أم في موضعها) يقرآن القاعدة
 * نفسها، فلا تقول البطاقة شيئًا والترتيب شيئًا آخر. والإخفاء لبطاقة التخزين وحدها.
 */
@Composable
private fun FocusCard(
    dashboard: DashboardState,
    onNavigate: (String) -> Unit,
    storageVisible: Boolean,
    onHideStorage: () -> Unit,
) {
    val p = neuralPalette()
    val heat = deviceHeatC(dashboard) ?: 0
    val ram = fractionOf(dashboard.ramUsedMb, dashboard.ramTotalMb)
    val kind = HomeFocusModel.focusKind(heat, ram, storageVisible) ?: return

    val title: String
    val body: String
    val accent: Color
    val icon: ImageVector
    val route: String
    val hideAction: (() -> Unit)?
    when (kind) {
        HomeFocusKind.HEAT -> {
            title = stringResource(R.string.home_focus_heat)
            body = stringResource(R.string.home_focus_heat_desc)
            accent = p.danger
            icon = Icons.Rounded.Thermostat
            route = MaxDestination.ThermalDetail.route
            hideAction = null
        }
        HomeFocusKind.STORAGE -> {
            title = stringResource(R.string.home_focus_storage)
            body = stringResource(R.string.home_focus_storage_desc)
            accent = p.warn
            icon = Icons.Rounded.Storage
            route = MaxDestination.StorageDetail.route
            hideAction = onHideStorage
        }
        HomeFocusKind.MEMORY -> {
            title = stringResource(R.string.home_focus_memory)
            body = stringResource(R.string.home_focus_memory_desc)
            accent = p.accentAlt
            icon = Icons.Rounded.Memory
            route = MaxDestination.ZramManager.route
            hideAction = null
        }
    }
    NeuralPanel(accent = accent, onClick = { onNavigate(route) }, verticalSpacing = MaxSpace.sm) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            NeuralIconChip(icon, accent, size = MaxSize.rowIconContainer)
            Spacer(Modifier.width(MaxSpace.md))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(MaxSpace.hairline)) {
                Text(
                    title,
                    color = p.text,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    body,
                    color = p.muted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (hideAction != null) {
                Spacer(Modifier.width(MaxSpace.sm))
                NeuralPill(
                    text = stringResource(R.string.home_focus_hide),
                    accent = p.muted,
                    compact = true,
                    onClick = hideAction,
                )
            }
        }
    }
}

/**
 * حالة المحرّك في الرأس: بهندسة أزرار الرأس نفسها (٣٨dp، نصف القطر ذاته، حدّ بالعرض ذاته)، فلا تطفو
 * كبسولة قصيرة بين مربعات مستديرة. اللون يحمل الحالة، والكلمة تحملها أيضًا.
 */
@Composable
private fun HomeStatusChip(text: String, accent: Color, modifier: Modifier = Modifier) {
    val p = neuralPalette()
    val shape = RoundedCornerShape(maxRadius(MaxRadius.control))
    Row(
        Modifier
            .height(38.dp)
            .clip(shape)
            .background(accent.copy(alpha = .14f))
            .border(BorderStroke(1.dp, accent.copy(alpha = .40f)), shape)
            .then(modifier)
            .padding(horizontal = MaxSpace.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(MaxSpace.sm).clip(CircleShape).background(accent))
        Spacer(Modifier.width(MaxSpace.xs))
        Text(text, color = p.text, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

/**
 * نافذة **سبب** لا إجراء (أمر المالك: «وعند الضغط عليه نافذة منبثقة بالسبب»).
 *
 * ## حالتان لا نافذة واحدة (أمر المالك ٢٠٢٦-١١-٢٠)
 *
 * «يجب أن تظهر Why not active? عندما يكون الذر مكتوب فيه غير مفعل ولكن عندما يكون مكتوب مفعل تختلف
 * الرسالة مثل كل شيء يعمل». فالنافذة التي تُفتح من شارة **مفعّلة** تقول «كل شيء يعمل»؛ ونصّ
 * «لماذا غير مفعّل؟» لا يظهر إلا في حالة عدم التفعيل. والنصّان يخرجان من [HomeStatusCopy] المقيسة
 * في `HomeActivityRulesTest`، فلا يعودان يتّحدان بتعديل عابر.
 *
 * وتعرض الشروط الثلاثة دائمًا، والمحقَّق منها بعلامة صح والمعطّل بعلامة رفض: إخفاء غير المحقَّق كان سيترك
 * المستخدم يظنّ الشرط غير مطلوب، وهو عكس ما طُلب. ويُضاف إليها سطر الخادم الخلفيّ بحالاته الثلاث
 * — «يعمل ويردّ» / «يعمل لكنه يرفض هويّة التطبيق» / «لا يجيب» — لأن «الخادم فشل» كانت رسالة كاذبة
 * كلّفت المالك اعتذارًا للمستخدمين.
 */
@Composable
private fun HomeStatusReasonDialog(
    active: Boolean,
    accessLevel: PrivilegeLevel,
    moduleInstalled: Boolean,
    daemonHealth: DaemonHealth,
    moduleUpdatePending: Boolean,
    onClose: () -> Unit,
    onModuleWritten: () -> Unit,
) {
    val p = neuralPalette()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // الزر لا يُعرض إلا لمن يستطيع الاستفادة: جذر ممنوح، ولا وحدة، وحِمل موجود في الحزمة.
    // ومن لا جذر عنده لا يرى زرًّا يفشل — وهذا نصّ أمر المالك «مراعاة لعدم إزعاج الحالات الأخرى».
    // **ومفاتيح `remember` ليست زينة:** بعد كتابة الوحدة تُقرأ الحالة من جديد، ولو بقي الحساب
    // محفوظًا من أول تركيب لبقي الزرّ معروضًا و«الوحدة مثبّتة» سالبًا بعد نجاح الكتابة.
    var written by remember { mutableStateOf(false) }
    val canBootstrap = remember(moduleInstalled, moduleUpdatePending, written) {
        RootUtils.isRootGranted() && !moduleInstalled && !moduleUpdatePending &&
            !written && ModuleBootstrap.hasPayload(context)
    }
    var busy by remember { mutableStateOf(false) }
    var outcome by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(HomeStatusCopy.titleRes(active))) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MaxSpace.sm)) {
                Text(
                    stringResource(HomeStatusCopy.bodyRes(active)),
                    color = p.text,
                    style = MaterialTheme.typography.bodySmall,
                )
                HomeStatusReasonRow(R.string.home_status_row_root, accessLevel == PrivilegeLevel.ROOT)
                HomeStatusReasonRow(R.string.home_status_row_module, moduleInstalled)
                if (moduleUpdatePending) {
                    // ملفّات الوحدة مكتوبة ويراها مدير الجذر، لكن سكربتاتها لا تعمل قبل الإقلاع.
                    // فالسطر يمنع قراءة «مثبّتة» كأنها عاملة الآن.
                    HomeStatusReasonRow(R.string.home_status_row_module_pending, satisfied = false)
                }
                HomeStatusReasonRow(R.string.home_status_row_shizuku, accessLevel == PrivilegeLevel.SHIZUKU)
                // سؤال الخادم لا يُطرح إلا في وضع الروت (أمر المالك ٢٠٢٦-١١-٢٢): في Shizuku
                // يعمل التطبيق بمساره المباشر، وعرض «الخادم لا يجيب» هناك عطبٌ موهوم.
                HomeStatusCopy.daemonRowFor(accessLevel, daemonHealth)?.let { labelRes ->
                    HomeStatusReasonRow(labelRes, HomeStatusCopy.daemonVerdict(daemonHealth))
                }
                outcome?.let { line ->
                    Text(line, color = p.text, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) { Text(stringResource(R.string.home_status_close)) }
        },
        dismissButton = if (!canBootstrap) null else {
            {
                TextButton(
                    enabled = !busy,
                    onClick = {
                        busy = true
                        scope.launch {
                            val report = withContext(Dispatchers.IO) { ModuleBootstrap.install(context) }
                            outcome = when (report) {
                                is ModuleBootstrap.Report.Written -> {
                                    // الصدق الفوري: الزرّ يختفي، والحالة تُعاد قراءتها من مصدرها
                                    // (الوحدة + علامة الإقلاع + الخادم) بدل الاعتماد على نجاح الكتابة.
                                    written = true
                                    onModuleWritten()
                                    context.getString(R.string.home_status_enable_module_written)
                                }
                                is ModuleBootstrap.Report.Failed -> context.getString(
                                    R.string.home_status_enable_module_failed,
                                    report.detail,
                                )
                                is ModuleBootstrap.Report.MissingPayload -> context.getString(
                                    R.string.home_status_enable_module_failed,
                                    report.reason,
                                )
                                ModuleBootstrap.Report.NoRoot ->
                                    context.getString(R.string.home_status_enable_module_no_root)
                                ModuleBootstrap.Report.AlreadyInstalled ->
                                    context.getString(R.string.home_status_enable_module_exists)
                            }
                            busy = false
                        }
                    },
                ) {
                    Text(
                        stringResource(
                            if (busy) R.string.home_status_enable_module_running
                            else R.string.home_status_enable_module,
                        ),
                    )
                }
            }
        },
    )
}

/** سطر شرط واحد: علامة تُقرأ بلا لون وحده (لون ≠ معنى وحيد — ADR-07). */
@Composable
private fun HomeStatusReasonRow(labelRes: Int, satisfied: Boolean?) {
    val p = neuralPalette()
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = when (satisfied) {
                true -> Icons.Rounded.CheckCircle
                false -> Icons.Rounded.Cancel
                null -> Icons.Rounded.HelpOutline
            },
            contentDescription = null,
            tint = when (satisfied) {
                true -> p.ok
                false -> p.danger
                null -> p.muted
            },
            modifier = Modifier.size(MaxSize.iconGlyph),
        )
        Text(
            text = stringResource(labelRes),
            color = p.text,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
