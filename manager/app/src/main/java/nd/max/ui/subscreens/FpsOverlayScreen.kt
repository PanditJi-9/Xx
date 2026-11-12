/*
 * Copyright (C) 2026-2027 Zexshia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * **Screen card — archetype: Instrument · object: the frame timeline ·
 * carrying element: the plot.** One series, one axis, one reading: the person
 * came to watch frames fall, not to browse settings. Controls sit under the
 * plot as a short legend of what is measured, never above it.
 *
 * States: overlay permission missing → the enable row says so · no frame
 * samples → the graph is absent while readings stay · preview hidden → the
 * restore tab, which is the window's own collapsed state.
 */

package nd.max.ui.subscreens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PictureInPicture
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.annotation.StringRes
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.navigation.NavController
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nd.max.R
import nd.max.core.platform.FpsMonitorUtil
import nd.max.core.platform.FpsReadMode
import nd.max.core.platform.HUD_OWNER_SCREEN
import nd.max.core.platform.HudArrangement
import nd.max.core.platform.HudChannel
import nd.max.core.platform.HudChannelProbe
import nd.max.core.platform.HudChannelStatus
import nd.max.core.platform.HudField
import nd.max.core.platform.HudForm
import nd.max.core.platform.HudLive
import nd.max.core.platform.HudPreset
import nd.max.core.platform.HudReading
import nd.max.core.platform.HudRecorder
import nd.max.core.platform.HudSampler
import nd.max.core.platform.HudTally
import nd.max.core.platform.acceptsArrangement
import nd.max.service.FpsOverlayService
import nd.max.ui.navigation.MaxDestination
import nd.max.ui.component.HudFieldTile
import nd.max.ui.component.HudRestoreTab
import nd.max.ui.component.HudSurface
import nd.max.ui.design.MaxChoiceRow
import nd.max.ui.design.MaxCondition
import nd.max.ui.design.MaxConditionKind
import nd.max.ui.design.MaxDataTrust
import nd.max.ui.design.MaxGroup
import nd.max.ui.design.MaxGroupDivider
import nd.max.ui.design.MaxMetric
import nd.max.ui.design.MaxMetricLine
import nd.max.ui.design.MaxNavigationRow
import nd.max.ui.design.MaxRow
import nd.max.ui.design.MaxScreen
import nd.max.ui.design.MaxSection
import nd.max.ui.design.MaxSegmented
import nd.max.ui.design.MaxSliderRow
import nd.max.ui.design.MaxSpace
import nd.max.ui.design.MaxSwitchRow
import nd.max.ui.design.MaxTone
import nd.max.ui.component.AccentSwatches
import nd.max.ui.component.ScreenAccentProvider
import nd.max.ui.component.parseAccentHex
import nd.max.ui.util.FpsOverlayPrefs
import nd.max.ui.util.HudFilter
import nd.max.ui.util.MonitorPrefs
import nd.max.ui.util.MonitorSpec
import nd.max.ui.util.presetOrNull
import nd.max.ui.util.withPreset

/**
 * لوحة الأداء — إعدادها.
 *
 * ### ما بُني من الصفر، ولماذا
 *
 * الشاشة السابقة قسّمت نفسها إلى «تراكب · مصدر · عرض · مقاييس» وقدّمت لكل خيار صفًّا: مفتاحًا
 * لمقياس، ومقطعًا لشكل، ومقطعًا لاتجاه. والعطب ليس في الألوان بل في **حلقة التغذية الراجعة**:
 * لا شيء في الشاشة كان يقول ما ستراه، فالمستخدم يختار شكلًا ثم يخرج إلى لعبة ليرى أثره — ثم
 * يعود ليعدّل. فصار المحور هنا ثلاثة أسئلة مرتّبة كما يسألها المستخدم:
 *
 * 1. **الحالة** — ما تعرضه اللوحة **الآن** (معاينة حيّة، نفس المُصيِّر الذي يُرسم فوق اللعبة).
 * 2. **الحقول** — ماذا يُعرض، وكل حقل يرتدي قيمته الحيّة في بلاطته.
 * 3. **الشكل** — كيف يُعرض.
 *
 * ثم فصلان للأسباب لا للشكل: **المصدر** (لماذا لا أرى رقمًا؟) و**الجلسة** (ما قِسته، وأين يخرج).
 *
 * ### ورأس المال الفعليّ: زرّ التسجيل صار يسجّل
 *
 * في نسخة سابقة كان في اللوحة زرّ تسجيل يقلب **متغيّرًا منطقيًّا داخل الواجهة** ولا يكتب شيئًا:
 * نقطة حمراء تتوهّج وملفّ لا يوجد. وذلك أسوأ من غياب الزرّ، لأنّ المستخدم يظنّ أنّه قاس بينما
 * لم يُحفظ له رقم واحد. والآن التسجيل جلسة حقيقية ([HudRecorder]) بإحصاء وتصدير CSV.
 *
 * ### وحدود مُعلنة
 *
 * - المعاينة تعمل بمُشغِّل **واحد** مشترك مع التراكب؛ فإن كان التراكب يعمل فالشاشة تقرأ منه ولا
 *   تُشغّل قارئًا ثانيًا (وإلا فكل قراءة تُنفَّذ مرّتين على جهاز يُقاس أداؤه).
 * - والإحصاء من **الحلقة الأخيرة** لا من الجلسة كلّها إن تجاوزت السقف، ويُقال ذلك في وصف الفصل.
 */
@Composable
fun FpsOverlayScreen(navController: NavController) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()

    var state by remember { mutableStateOf(FpsOverlayPrefs.load(context)) }
    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var modeIndex by remember {
        FpsMonitorUtil.init(context)
        mutableIntStateOf(FpsMonitorUtil.currentMode.ordinal)
    }
    // M4: الجذر لا يقرأ اللقطة الحيّة. `HudLive.snapshot` يتغيّر مع كل عيّنة (كل ثانية بحدّها
    // الأدنى)، فكانت الشاشة كلها — صفوف الضبط، ومنتقي الحقول، وثلاثة أقسام لا تعرض رقمًا حيًّا —
    // تُعاد تركيبها مع كل عيّنة لأن الجذر قرأ اللقطة. هنا تُشتقّ القيم الثلاث التي تحتاجها
    // الأقسام الثابتة وحدها، فلا تُعاد تركيبتها إلا حين تتغيّر فعلًا؛ والمعاينة وحدها تقرأ
    // اللقطة داخل تركيبها (`HudPreviewCard`). وهذا ما يُقاس على الجهاز بعدد إعادات التركيب.
    val recording by remember { derivedStateOf { HudLive.snapshot.value.recording } }
    val samplerOwner by remember { derivedStateOf { HudLive.snapshot.value.owner } }
    val framesAnswered by remember { derivedStateOf { HudLive.snapshot.value.reading?.framesAnswered } }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        hasOverlayPermission = Settings.canDrawOverlays(context)
        if (hasOverlayPermission && state.enabled) startPanel(context)
    }

    // المعاينة تشارك القارئ دائمًا: العضويّة تُوسّع الحقول ولا تُضاعف القراءة، فالأرقام حيّة
    // حتى والتراكب يعمل — وبلا شرط «من سبق» الذي كان يُجمّد أحد الطرفين عند خروج الآخر.
    LifecycleResumeEffect(Unit) {
        hasOverlayPermission = Settings.canDrawOverlays(context)
        HudSampler.start(
            context = context,
            owner = HUD_OWNER_SCREEN,
            fields = { FpsOverlayPrefs.load(context).fields },
            intervalMs = FpsOverlayPrefs.load(context).intervalMs.toLong(),
        )
        onPauseOrDispose { HudSampler.stop(HUD_OWNER_SCREEN) }
    }

    fun requestOverlayPermission() {
        permissionLauncher.launch(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
        )
    }

    fun persist(newState: FpsOverlayPrefs.State) {
        state = newState
        FpsOverlayPrefs.save(context, newState)
        if (newState.enabled && hasOverlayPermission) startPanel(context)
    }

    fun setEnabled(enabled: Boolean) {
        if (enabled && !hasOverlayPermission) {
            requestOverlayPermission()
            return
        }
        persist(state.copy(enabled = enabled))
        if (!enabled) stopPanel(context)
    }

    // الترتيب هو ترتيب [HudForm] حرفيًّا (`ordinal` يُقرأ ويُكتب به)، فلا يُعاد ترتيبها هنا.
    val formOptions = listOf(
        stringResource(R.string.hud_form_strip),
        stringResource(R.string.hud_form_pane),
        stringResource(R.string.hud_form_ring),
        stringResource(R.string.hud_form_badge)
    )
    val arrangementOptions = listOf(
        stringResource(R.string.hud_arrangement_line),
        stringResource(R.string.hud_arrangement_stack)
    )
    // ترتيب الاختصارين هو ترتيب [HudPreset] حرفيًّا (`ordinal` يُقرأ هنا).
    val presetOptions = listOf(
        stringResource(R.string.hud_preset_compact),
        stringResource(R.string.hud_preset_expanded)
    )
    val sourceOptions = listOf(
        stringResource(R.string.fps_overlay_mode_surfaceflinger),
        stringResource(R.string.fps_overlay_mode_kernel),
        stringResource(R.string.fps_overlay_mode_dumpsys)
    )
    // المسبار يُقرأ مرّة عند فتح الشاشة: العقد لا تظهر وتختفي بين رسمتين.
    val channels = remember(context) { HudChannelProbe.probe(context) }
    val sourceNotes = listOf(
        stringResource(R.string.hud_source_surfaceflinger_note),
        stringResource(R.string.hud_source_kernel_note),
        stringResource(R.string.hud_source_dumpsys_note)
    )

    ScreenAccentProvider(scheme.tertiary) {
        MaxScreen(
            title = stringResource(R.string.fps_overlay_title),
            onBack = { navController.popBackStack() },
            accentIcon = Icons.Rounded.Speed,
            accent = scheme.tertiary,
            banner = if (hasOverlayPermission) {
                null
            } else {
                MaxCondition(
                    kind = MaxConditionKind.PermissionRequired,
                    title = stringResource(R.string.max_fps_permission_title),
                    detail = stringResource(R.string.max_fps_permission_detail),
                    primaryActionLabel = stringResource(R.string.max_fps_permission_action),
                    onPrimaryAction = { requestOverlayPermission() }
                )
            }
        ) {
            // ── ١. الحالة: ما تراه الآن ─────────────────────────────────────
            MaxSection(
                title = stringResource(R.string.hud_section_state),
                description = stringResource(R.string.hud_section_state_desc)
            ) {
                // المعاينة تحمل أدوات النافذة الثلاث نفسها، وتقرأ اللقطة الحيّة وحدها داخل
                // تركيبها (`HudPreviewCard`) فلا تسحب الشاشة كلها مع كل عيّنة (M4).
                HudPreviewCard(
                    state = state,
                    onToggleRecording = {
                        if (HudRecorder.isLive) HudRecorder.stop(context) else HudRecorder.start(context)
                    }
                )

                MaxGroup {
                    MaxSwitchRow(
                        title = stringResource(R.string.fps_overlay_enable),
                        subtitle = if (!hasOverlayPermission) {
                            stringResource(R.string.fps_overlay_needs_permission)
                        } else if (state.enabled) {
                            stringResource(R.string.hud_status_running)
                        } else {
                            stringResource(R.string.hud_status_stopped)
                        },
                        checked = state.enabled && hasOverlayPermission,
                        onCheckedChange = { setEnabled(it) },
                        icon = Icons.Rounded.PictureInPicture,
                        iconTone = MaxTone.Accent
                    )
                }
            }

            // ── ٢. الحقول: ماذا يُعرض ───────────────────────────────────────
            MaxSection(
                title = stringResource(R.string.hud_section_fields),
                description = stringResource(R.string.hud_section_fields_desc)
            ) {
                // §2: «وضع مصغّر يعرض المعلومات الأساسية فقط، ووضع موسّع للتفاصيل» — لقطة إعداد
                // بلمسة، لا نمط يُفرض: التظليل يتبع المطابقة الحرفية للحالة، ولا يظهر اختصار
                // لم يُطبَّق (فبعد أي تعديل فردي يعود المنتقي بلا تظليل = «مخصّص» بصدق).
                MaxSegmented(
                    options = presetOptions,
                    selectedIndex = state.presetOrNull()?.ordinal ?: -1,
                    onSelect = { persist(state.withPreset(HudPreset.entries[it])) }
                )
                Text(
                    text = stringResource(R.string.hud_preset_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = MaxSpace.groupPadding)
                )
                Text(
                    text = stringResource(
                        R.string.hud_fields_count,
                        state.fields.size,
                        HudField.entries.size
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = MaxSpace.groupPadding)
                )
                HudFieldGridLive(
                    selected = state.fields,
                    onToggle = { field ->
                        val next = state.fields.toMutableSet()
                        if (!next.add(field)) next.remove(field)
                        persist(state.copy(fields = next))
                    }
                )
            }

            // ── ٢·٥. ما يقرؤه هذا الجهاز فعلًا ─────────────────────────────
            MaxSection(
                title = stringResource(R.string.hud_channels_title),
                description = stringResource(R.string.hud_channels_desc)
            ) {
                MaxGroup {
                    channels.forEachIndexed { index, channel ->
                        if (index > 0) MaxGroupDivider()
                        MaxMetricLine(
                            MaxMetric(
                                label = stringResource(channelLabel(channel.channel)),
                                value = stringResource(channelStatus(channel.status)),
                                trust = when (channel.status) {
                                    HudChannelStatus.Readable -> MaxDataTrust.Live
                                    HudChannelStatus.Denied -> MaxDataTrust.Unreadable
                                    HudChannelStatus.Missing -> MaxDataTrust.Unsupported
                                },
                                source = channel.source
                            )
                        )
                    }
                }
            }

            // ── ٣. الشكل ───────────────────────────────────────────────────
            MaxSection(title = stringResource(R.string.hud_section_shape)) {
                MaxSegmented(
                    options = formOptions,
                    selectedIndex = state.form.ordinal,
                    onSelect = { persist(state.copy(form = HudForm.entries[it])) }
                )
                MaxSegmented(
                    options = arrangementOptions,
                    selectedIndex = state.arrangement.ordinal,
                    onSelect = { persist(state.copy(arrangement = HudArrangement.entries[it])) },
                    enabled = state.form.acceptsArrangement()
                )
                if (!state.form.acceptsArrangement()) {
                    Text(
                        text = stringResource(R.string.hud_arrangement_locked),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = MaxSpace.groupPadding)
                    )
                }

                AccentSwatches(
                    selectedHex = state.colorHex,
                    onSelect = { hex -> persist(state.copy(colorHex = hex)) }
                )

                MaxGroup {
                    MaxSliderRow(
                        title = stringResource(R.string.fps_overlay_text_size),
                        value = state.textSizeSp,
                        onValueChange = { state = state.copy(textSizeSp = it) },
                        valueText = "${state.textSizeSp.toInt()} sp",
                        valueRange = 10f..24f,
                        steps = 6,
                        onValueChangeFinished = { persist(state) }
                    )
                    MaxGroupDivider()
                    MaxSliderRow(
                        title = stringResource(R.string.fps_overlay_bg_alpha),
                        value = state.backgroundAlpha,
                        onValueChange = { state = state.copy(backgroundAlpha = it) },
                        valueText = "${(state.backgroundAlpha * 100).toInt()} %",
                        valueRange = 0f..1f,
                        steps = 9,
                        onValueChangeFinished = { persist(state) }
                    )
                    MaxGroupDivider()
                    MaxSliderRow(
                        title = stringResource(R.string.fps_overlay_width_scale),
                        value = state.widthScale,
                        onValueChange = { state = state.copy(widthScale = it) },
                        valueText = "×${"%.1f".format(state.widthScale)}",
                        valueRange = 0.6f..2f,
                        steps = 13,
                        onValueChangeFinished = { persist(state) }
                    )
                    MaxGroupDivider()
                    MaxSwitchRow(
                        title = stringResource(R.string.hud_graph_title),
                        subtitle = stringResource(R.string.hud_graph_desc),
                        checked = state.showGraph,
                        onCheckedChange = { persist(state.copy(showGraph = it)) }
                    )
                    if (state.showGraph) {
                        MaxSegmented(
                            options = FpsOverlayPrefs.GRAPH_SPANS.map {
                                stringResource(R.string.hud_graph_span, it)
                            },
                            selectedIndex = FpsOverlayPrefs.GRAPH_SPANS.indexOf(state.graphSpan)
                                .coerceAtLeast(0),
                            onSelect = { persist(state.copy(graphSpan = FpsOverlayPrefs.GRAPH_SPANS[it])) }
                        )
                    }
                    MaxGroupDivider()
                    MaxSegmented(
                        options = HudFilter.entries.map { filter ->
                            stringResource(
                                when (filter) {
                                    HudFilter.Text -> R.string.hud_filter_text
                                    HudFilter.Chart -> R.string.hud_filter_chart
                                    HudFilter.All -> R.string.hud_filter_all
                                }
                            )
                        },
                        selectedIndex = HudFilter.entries.indexOf(state.filter).coerceAtLeast(0),
                        onSelect = { persist(state.copy(filter = HudFilter.entries[it])) }
                    )
                    MaxGroupDivider()
                    MaxSwitchRow(
                        title = stringResource(R.string.hud_snap_title),
                        subtitle = stringResource(R.string.hud_snap_desc),
                        checked = state.snapEdges,
                        onCheckedChange = { persist(state.copy(snapEdges = it)) }
                    )
                }
            }

            // ── ٣·٥. الإيقاع: كم مرّة تُقرأ الأرقام ──────────────────────────
            MaxSection(
                title = stringResource(R.string.hud_interval_title),
                description = stringResource(R.string.hud_interval_desc)
            ) {
                MaxSegmented(
                    options = FpsOverlayPrefs.INTERVAL_CHOICES.map {
                        stringResource(R.string.hud_interval_ms, it)
                    },
                    selectedIndex = FpsOverlayPrefs.INTERVAL_CHOICES.indexOf(state.intervalMs)
                        .coerceAtLeast(0),
                    onSelect = { index ->
                        val chosen = FpsOverlayPrefs.INTERVAL_CHOICES[index]
                        persist(state.copy(intervalMs = chosen))
                        // الإيقاع يسري على الحلقة القائمة الآن — لا انتظار إعادة تشغيل الخدمة.
                        HudSampler.start(
                            context = context,
                            owner = HUD_OWNER_SCREEN,
                            fields = { FpsOverlayPrefs.load(context).fields },
                            intervalMs = chosen.toLong(),
                        )
                    }
                )
            }

            // ── ٤. المصدر: لماذا لا أرى رقمًا؟ ──────────────────────────────
            MaxSection(
                title = stringResource(R.string.max_fps_section_source),
                description = stringResource(R.string.fps_overlay_read_mode_desc)
            ) {
                MaxGroup {
                    FpsReadMode.entries.forEachIndexed { index, mode ->
                        if (index > 0) MaxGroupDivider()
                        MaxChoiceRow(
                            title = sourceOptions[index],
                            subtitle = sourceNotes[index],
                            selected = modeIndex == index,
                            onSelect = {
                                modeIndex = index
                                FpsMonitorUtil.setMode(context, mode)
                            }
                        )
                    }
                }
                val health = when {
                    samplerOwner == null -> null
                    framesAnswered == true ->
                        stringResource(R.string.hud_source_answered, sourceOptions[modeIndex])

                    else -> stringResource(R.string.hud_source_silent)
                }
                if (health != null) {
                    Text(
                        text = health,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = MaxSpace.groupPadding)
                    )
                }
                Text(
                    text = stringResource(R.string.max_fps_source_fallback_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = MaxSpace.groupPadding)
                )
            }

            // ── ٥. الجلسة: ما قِسته، وأين يخرج ─────────────────────────────
            MaxSection(
                title = stringResource(R.string.hud_section_session),
                description = stringResource(R.string.hud_section_session_desc)
            ) {
                MaxGroup {
                    MaxSwitchRow(
                        title = if (recording) {
                            stringResource(R.string.hud_session_stop)
                        } else {
                            stringResource(R.string.hud_session_start)
                        },
                        subtitle = stringResource(R.string.hud_session_record_note),
                        checked = recording,
                        onCheckedChange = { on ->
                            if (on) HudRecorder.start(context) else HudRecorder.stop(context)
                        }
                    )
                }

                val tally = HudRecorder.tally()
                if (tally == null) {
                    Text(
                        text = stringResource(R.string.hud_session_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = MaxSpace.groupPadding)
                    )
                } else {
                    SessionStats(
                        tally = tally,
                        trust = if (recording) MaxDataTrust.Live else MaxDataTrust.Snapshot
                    )
                    MaxGroup {
                        MaxNavigationRow(
                            title = stringResource(R.string.hud_session_export),
                            subtitle = stringResource(R.string.hud_session_export_note),
                            onClick = { exportSession(context, scope) }
                        )
                        MaxGroupDivider()
                        MaxNavigationRow(
                            title = stringResource(R.string.hud_session_clear),
                            onClick = { HudRecorder.clear() }
                        )
                    }
                }

                // الباب إلى السجلّ الكامل: الشاشة المنفصلة تجمع الحيّ والمحفوظ،
                // ويبقى هنا الملخّص السريع بلا تكرار قائمة كاملة في قسم إعداد.
                MaxGroup {
                    MaxNavigationRow(
                        title = stringResource(R.string.game_sessions_open),
                        subtitle = stringResource(R.string.game_sessions_open_note),
                        onClick = { navController.navigate(MaxDestination.GameSessions.route) }
                    )
                }
            }

            // ── ٦. المراقبات الإضافية: نوافذ مستقلّة، خدمة واحدة ───────────
            MaxSection(
                title = stringResource(R.string.hud_section_monitors),
                description = stringResource(R.string.hud_section_monitors_desc)
            ) {
                HudMonitorsSection(
                    onChanged = {
                        if (state.enabled && hasOverlayPermission) startPanel(context)
                    }
                )
            }
        }
    }
}

/**
 * معاينة النافذة — **وحدها تقرأ اللقطة الحيّة** (M4).
 *
 * كانت الشاشة تقرأ `HudLive.snapshot` على جذرها، فكل عيّنة إطارات تُنتج لقطة جديدة فتُعيد تركيب
 * أقسام الضبط كلها — ومنتقي الحقول وثلاثة أقسام لا تحمل رقمًا حيًّا. هنا انتقلت القراءة إلى هذا
 * التركيب وحده، فما يُعاد تركيبه مع العيّنة هو اللوحة وسطرها، لا الشاشة.
 *
 * والإخفاء هنا **ليس تمثيلًا**: يطوي المعاينة إلى الكبسولة ذاتها التي تبقى فوق اللعبة. والتسجيل
 * حقيقيّ كذلك: يشغّل `HudRecorder` نفسه. والعرض بمقاس المحتوى لا بعرض الشاشة (أمر المالك: النافذة
 * كانت كبيرة دون داعي)، و`IntrinsicSize.Max` تعني «بعرض محتواها ويُقصّ إن لم يتّسع».
 */
@Composable
private fun HudPreviewCard(
    state: FpsOverlayPrefs.State,
    onToggleRecording: () -> Unit,
) {
    val snapshot by HudLive.snapshot.collectAsState()
    var previewHidden by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MaxSpace.groupPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (previewHidden) {
            HudRestoreTab(onShow = { previewHidden = false })
        } else {
            HudSurface(
                form = state.form,
                arrangement = state.arrangement,
                reading = snapshot.reading,
                fields = state.orderedFields,
                accent = remember(state.colorHex) { parseAccentHex(state.colorHex) },
                textSizeSp = state.textSizeSp,
                backgroundAlpha = state.backgroundAlpha,
                framesHistory = snapshot.framesHistory.takeLast(state.graphSpan),
                showGraph = state.showGraph,
                // المعاينة تقرأ المرشّح نفسه الذي تقرؤه النافذة فوق اللعبة
                // (`FpsOverlayService` يمرّر `hud.filter`): كانت تفترض `All` فتعرض
                // نصًّا أو رسمًا أخفاه المستخدم، فتكذب في الشيء الذي جاءت لتصفه.
                filter = state.filter,
                recording = snapshot.recording,
                onToggleRecording = onToggleRecording,
                onHide = { previewHidden = true },
                // والإغلاق هنا لا يُغلق شيئًا — لا خدمة في هذه الشاشة — ووجوده مقصود:
                // من رأى الزرّ في موضعه هنا لا يبحث عنه فوق اللعبة.
                onClose = {},
                modifier = Modifier.width(IntrinsicSize.Max)
            )
        }
        Spacer(Modifier.size(MaxSpace.xs))
        Text(
            text = stringResource(R.string.hud_preview_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** شبكة الحقول تقرأ اللقطة داخل تركيبها بدل جذر الشاشة (M4). */
@Composable
private fun HudFieldGridLive(
    selected: Set<HudField>,
    onToggle: (HudField) -> Unit,
) {
    val snapshot by HudLive.snapshot.collectAsState()
    HudFieldGrid(reading = snapshot.reading, selected = selected, onToggle = onToggle)
}

/**
 * شبكة الحقول: بلاطتان في الصفّ.
 *
 * والصفّ الأخير يُكمَّل بفراغ موزون لا بمحاذاة يسار — فبلاطة واحدة في صفّ نصفُه فارغ تبدو عطبًا.
 */
@Composable
private fun HudFieldGrid(
    reading: HudReading?,
    selected: Set<HudField>,
    onToggle: (HudField) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MaxSpace.groupPadding),
        verticalArrangement = Arrangement.spacedBy(MaxSpace.xs)
    ) {
        HudField.entries.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaxSpace.xs)
            ) {
                pair.forEach { field ->
                    HudFieldTile(
                        field = field,
                        reading = reading,
                        selected = field in selected,
                        onToggle = { onToggle(field) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** إحصاء الجلسة — صفوف القراءة القياسية نفسها التي تُعرض بها قراءات الشاشات الأخرى. */
@Composable
private fun SessionStats(tally: HudTally, trust: MaxDataTrust) {
    MaxGroup {
        MaxMetricLine(
            MaxMetric(
                label = stringResource(R.string.hud_session_samples),
                value = tally.samples.toString(),
                trust = trust
            )
        )
        MaxMetricLine(
            MaxMetric(
                label = stringResource(R.string.hud_session_span),
                value = sessionClock(tally.spanMs),
                trust = trust
            )
        )
        MaxMetricLine(
            MaxMetric(
                label = stringResource(R.string.hud_session_frames_avg),
                value = tally.framesAverage?.let { "%.1f".format(it) },
                unit = "FPS",
                trust = trust
            )
        )
        MaxMetricLine(
            MaxMetric(
                label = stringResource(R.string.hud_session_frames_range),
                value = if (tally.framesLow != null && tally.framesHigh != null) {
                    "%.0f – %.0f".format(tally.framesLow, tally.framesHigh)
                } else {
                    null
                },
                unit = "FPS",
                trust = trust
            )
        )
        MaxMetricLine(
            MaxMetric(
                label = stringResource(R.string.hud_session_heat_peak),
                value = tally.heatPeak?.let { "%.1f".format(it) },
                unit = "°C",
                trust = trust
            )
        )
        MaxMetricLine(
            MaxMetric(
                label = stringResource(R.string.hud_session_power_avg),
                value = tally.powerAverage?.let { "%.2f".format(it) },
                unit = "W",
                trust = trust
            )
        )
        if (tally.droppedSamples > 0) {
            MaxMetricLine(
                MaxMetric(
                    label = stringResource(R.string.hud_session_dropped),
                    value = tally.droppedSamples.toString(),
                    trust = MaxDataTrust.Snapshot,
                    note = stringResource(R.string.hud_session_dropped_note)
                )
            )
        }
    }
}

/** مدّة بصيغة `د:ث` — تنسيق واحد يُقارَن رأسيًّا. */
private fun sessionClock(ms: Long): String {
    val seconds = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(seconds / 60, seconds % 60)
}

/**
 * الجلسة ملفًّا، ثم حصّة للمشاركة.
 *
 * وتُكتب في `cacheDir` لا في التخزين المشترك: ملفّ قياس مؤقّت لا يستحقّ أن يُلوّث مجلّد
 * التنزيلات، ويُشارَك بالمسار الذي يُصرّح به `file_paths.xml` (`cache-path`).
 */
private fun exportSession(context: Context, scope: CoroutineScope) {
    val body = HudRecorder.csv()
    if (body == null) {
        Toast.makeText(context, context.getString(R.string.hud_session_empty), Toast.LENGTH_SHORT)
            .show()
        return
    }
    scope.launch {
        val share = withContext(Dispatchers.IO) {
            runCatching {
                val folder = File(context.cacheDir, "hud").apply { mkdirs() }
                val file = File(folder, "maxmanager-hud-${System.currentTimeMillis()}.csv")
                file.writeText(body)
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    file
                )
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/csv"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    },
                    context.getString(R.string.hud_session_export)
                )
            }.getOrNull()
        }
        if (share == null) {
            Toast.makeText(
                context,
                context.getString(R.string.hud_session_export_failed),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            context.startActivity(share)
        }
    }
}

private fun startPanel(context: Context) {
    ContextCompat.startForegroundService(
        context,
        Intent(context, FpsOverlayService::class.java)
    )
}

private fun stopPanel(context: Context) {
    context.startService(
        Intent(context, FpsOverlayService::class.java)
            .setAction(FpsOverlayService.ACTION_STOP)
    )
}

/**
 * مراقبات إضافية فوق المراقب الأوّل.
 *
 * **والأوّل غير معروض هنا بقصد:** له قسمه الكامل أعلاه (الحقول/الشكل/الحجم/الموضع)، وعرضه
 * مرة ثانية كبطاقة كان سيجعل إعدادًا واحدًا في مكانين. هذه البطاقات **للإضافات فقط**.
 *
 * **والحدّ يُقال لا يُخفى:** عند بلوغ [MonitorPrefs.MAX_EXTRA] يُعطَّل صفّ الإضافة بسببه
 * الظاهر، بدل أن يُقبل الضغط ثم لا يحدث شيء.
 */
@Composable
private fun HudMonitorsSection(onChanged: () -> Unit) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    var monitors by remember { mutableStateOf(MonitorPrefs.load(context)) }

    fun refresh() {
        monitors = MonitorPrefs.load(context)
        onChanged()
    }

    fun update(spec: MonitorSpec) {
        MonitorPrefs.update(context, spec)
        refresh()
    }

    val formOptions = listOf(
        stringResource(R.string.hud_form_strip),
        stringResource(R.string.hud_form_pane),
        stringResource(R.string.hud_form_ring),
        stringResource(R.string.hud_form_badge)
    )
    val arrangementOptions = listOf(
        stringResource(R.string.hud_arrangement_line),
        stringResource(R.string.hud_arrangement_stack)
    )

    if (monitors.isEmpty()) {
        Text(
            text = stringResource(R.string.hud_monitors_empty),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = MaxSpace.groupPadding)
        )
    }

    monitors.forEachIndexed { index, spec ->
        MaxGroup {
            MaxRow(
                title = stringResource(R.string.hud_monitor_title, index + 2),
                subtitle = stringResource(R.string.hud_monitor_own_settings),
                icon = Icons.Rounded.Speed,
                iconTone = MaxTone.Accent
            )
            MaxGroupDivider()
            MaxNavigationRow(
                title = stringResource(R.string.hud_monitor_delete),
                subtitle = stringResource(R.string.hud_monitor_delete_desc),
                onClick = {
                    MonitorPrefs.remove(context, spec.id)
                    refresh()
                }
            )
        }

        HudFieldGrid(
            reading = null,
            selected = spec.fields,
            onToggle = { field ->
                val next = spec.fields.toMutableSet()
                if (!next.add(field)) next.remove(field)
                update(spec.copy(fields = next))
            }
        )

        MaxSegmented(
            options = formOptions,
            selectedIndex = spec.form.ordinal,
            onSelect = { update(spec.copy(form = HudForm.entries[it])) }
        )
        MaxSegmented(
            options = arrangementOptions,
            selectedIndex = spec.arrangement.ordinal,
            onSelect = { update(spec.copy(arrangement = HudArrangement.entries[it])) },
            enabled = spec.form.acceptsArrangement()
        )

        AccentSwatches(
            selectedHex = spec.colorHex,
            onSelect = { hex -> update(spec.copy(colorHex = hex)) }
        )

        MonitorTuning(spec = spec, onCommit = ::update)

        MaxGroup {
            MaxSwitchRow(
                title = stringResource(R.string.hud_graph_title),
                subtitle = stringResource(R.string.hud_graph_desc),
                checked = spec.showGraph,
                onCheckedChange = { update(spec.copy(showGraph = it)) }
            )
            if (spec.showGraph) {
                MaxSegmented(
                    options = FpsOverlayPrefs.GRAPH_SPANS.map {
                        stringResource(R.string.hud_graph_span, it)
                    },
                    selectedIndex = FpsOverlayPrefs.GRAPH_SPANS.indexOf(spec.graphSpan)
                        .coerceAtLeast(0),
                    onSelect = {
                        update(spec.copy(graphSpan = FpsOverlayPrefs.GRAPH_SPANS[it]))
                    }
                )
            }
            MaxGroupDivider()
            MaxSegmented(
                options = HudFilter.entries.map { filter ->
                    stringResource(
                        when (filter) {
                            HudFilter.Text -> R.string.hud_filter_text
                            HudFilter.Chart -> R.string.hud_filter_chart
                            HudFilter.All -> R.string.hud_filter_all
                        }
                    )
                },
                selectedIndex = HudFilter.entries.indexOf(spec.filter).coerceAtLeast(0),
                onSelect = { update(spec.copy(filter = HudFilter.entries[it])) }
            )
            MaxGroupDivider()
            MaxSegmented(
                options = FpsOverlayPrefs.INTERVAL_CHOICES.map {
                    stringResource(R.string.hud_interval_ms, it)
                },
                selectedIndex = FpsOverlayPrefs.INTERVAL_CHOICES.indexOf(spec.intervalMs)
                    .coerceAtLeast(0),
                onSelect = {
                    update(spec.copy(intervalMs = FpsOverlayPrefs.INTERVAL_CHOICES[it]))
                }
            )
            MaxGroupDivider()
            MaxSwitchRow(
                title = stringResource(R.string.hud_snap_title),
                subtitle = stringResource(R.string.hud_snap_desc),
                checked = spec.snapEdges,
                onCheckedChange = { update(spec.copy(snapEdges = it)) }
            )
        }

        Spacer(Modifier.height(MaxSpace.sm))
    }

    val canAdd = monitors.size < MonitorPrefs.MAX_EXTRA
    MaxGroup {
        MaxNavigationRow(
            title = stringResource(R.string.hud_monitor_add),
            subtitle = stringResource(R.string.hud_monitor_add_desc),
            enabled = canAdd,
            lockedReason = if (canAdd) {
                null
            } else {
                stringResource(R.string.hud_monitor_max, MonitorPrefs.MAX_MONITORS)
            },
            onClick = {
                if (canAdd) {
                    MonitorPrefs.add(
                        context,
                        MonitorSpec(
                            id = MonitorPrefs.newId(),
                            form = FpsOverlayPrefs.load(context).form,
                            arrangement = FpsOverlayPrefs.load(context).arrangement,
                            colorHex = FpsOverlayPrefs.load(context).colorHex,
                            textSizeSp = FpsOverlayPrefs.load(context).textSizeSp,
                            backgroundAlpha = FpsOverlayPrefs.load(context).backgroundAlpha,
                            widthScale = FpsOverlayPrefs.load(context).widthScale,
                            snapEdges = FpsOverlayPrefs.load(context).snapEdges
                        )
                    )
                    refresh()
                }
            }
        )
    }
}

/**
 * أشرطة الحجم والشفافية والعرض لهذا المراقب.
 *
 * **والحفظ عند انتهاء السحب لا مع كل بكسل:** كل تغيير يمرّ بـ`onChanged` الذي يوقظ الخدمة
 * لتزامن النوافذ؛ كتابة كل بكسل تعني عشرات نداءات الخدمة في السحب الواحد. فالقيمة تُعرض حيّة
 * محليًّا، وتُثبَّت مرة واحدة عند الإفلات.
 */
@Composable
private fun MonitorTuning(spec: MonitorSpec, onCommit: (MonitorSpec) -> Unit) {
    var textSize by remember(spec.id) { mutableStateOf(spec.textSizeSp) }
    var alpha by remember(spec.id) { mutableStateOf(spec.backgroundAlpha) }
    var width by remember(spec.id) { mutableStateOf(spec.widthScale) }

    MaxGroup {
        MaxSliderRow(
            title = stringResource(R.string.fps_overlay_text_size),
            value = textSize,
            onValueChange = { textSize = it },
            valueText = "${textSize.toInt()} sp",
            valueRange = 10f..24f,
            steps = 6,
            onValueChangeFinished = { onCommit(spec.copy(textSizeSp = textSize)) }
        )
        MaxGroupDivider()
        MaxSliderRow(
            title = stringResource(R.string.fps_overlay_bg_alpha),
            value = alpha,
            onValueChange = { alpha = it },
            valueText = "${(alpha * 100).toInt()} %",
            valueRange = 0f..1f,
            steps = 9,
            onValueChangeFinished = { onCommit(spec.copy(backgroundAlpha = alpha)) }
        )
        MaxGroupDivider()
        MaxSliderRow(
            title = stringResource(R.string.fps_overlay_width_scale),
            value = width,
            onValueChange = { width = it },
            valueText = "×${"%.1f".format(width)}",
            valueRange = 0.6f..2f,
            steps = 13,
            onValueChangeFinished = { onCommit(spec.copy(widthScale = width)) }
        )
    }
}


/** اسم قناة القراءة — مفتاح نصّ واحد لكل قناة، فلا تُبنى الأسماء من `name`. */
@StringRes
private fun channelLabel(channel: HudChannel): Int = when (channel) {
    HudChannel.CpuFreq -> R.string.hud_channel_cpu_freq
    HudChannel.SocTemp -> R.string.hud_channel_soc_temp
    HudChannel.GpuFreq -> R.string.hud_channel_gpu_freq
    HudChannel.GpuLoad -> R.string.hud_channel_gpu_load
    HudChannel.DdrFreq -> R.string.hud_channel_ddr_freq
    HudChannel.DiskIo -> R.string.hud_channel_disk_io
    HudChannel.BatteryVoltage -> R.string.hud_channel_battery_voltage
}

/** حالة القناة بكلمة صريحة: تُقرأ · غير موجودة · موجودة والقراءة مرفوضة. */
@StringRes
private fun channelStatus(status: HudChannelStatus): Int = when (status) {
    HudChannelStatus.Readable -> R.string.hud_channel_readable
    HudChannelStatus.Missing -> R.string.hud_channel_missing
    HudChannelStatus.Denied -> R.string.hud_channel_denied
}
