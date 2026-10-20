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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    val snapshot by HudLive.snapshot.collectAsState()

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
            fields = { FpsOverlayPrefs.load(context).fields }
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
                // المعاينة تحمل أدوات النافذة الثلاث نفسها. والإخفاء فيها **ليس تمثيلًا**: يطوي
                // المعاينة إلى الكبسولة ذاتها التي تبقى فوق اللعبة — فيعرف المستخدم ما سيتبقّى على
                // شاشته قبل أن يُطفئ التراكب ويجرّب في لعبة. والتسجيل هنا حقيقيّ كالإخفاء: يشغّل
                // `HudRecorder` نفسه الذي يعمل فوق اللعبة.
                //
                // و**العرض بمقاس المحتوى لا بعرض الشاشة** (أمر المالك: «النافذة كبيرة دون داعي
                // مما يؤثر على الرؤية واللعب»): كانت المعاينة تُمدّ (`fillMaxWidth`) فتبدو شريطًا
                // أسودَ بعرض الشاشة بينما نافذة اللعب بعرض أرقامها — معاينة تكذب في المقاس الذي
                // جاءت لتصفه. و`IntrinsicSize.Max` تعني «بعرض محتواها، وإذا لم يتّسع في المعروض
                // يُقصّ عليه» فتطابق النافذة في الحالتين.
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
                            recording = snapshot.recording,
                            onToggleRecording = {
                                if (HudRecorder.isLive) HudRecorder.stop() else HudRecorder.start()
                            },
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
                        color = scheme.onSurfaceVariant
                    )
                }

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
                HudFieldGrid(
                    reading = snapshot.reading,
                    selected = state.fields,
                    onToggle = { field ->
                        val next = state.fields.toMutableSet()
                        if (!next.add(field)) next.remove(field)
                        persist(state.copy(fields = next))
                    }
                )
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
                    MaxSwitchRow(
                        title = stringResource(R.string.hud_snap_title),
                        subtitle = stringResource(R.string.hud_snap_desc),
                        checked = state.snapEdges,
                        onCheckedChange = { persist(state.copy(snapEdges = it)) }
                    )
                }
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
                    snapshot.owner == null -> null
                    snapshot.reading?.framesAnswered == true ->
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
                        title = if (snapshot.recording) {
                            stringResource(R.string.hud_session_stop)
                        } else {
                            stringResource(R.string.hud_session_start)
                        },
                        subtitle = stringResource(R.string.hud_session_record_note),
                        checked = snapshot.recording,
                        onCheckedChange = { on ->
                            if (on) HudRecorder.start() else HudRecorder.stop()
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
                        trust = if (snapshot.recording) MaxDataTrust.Live else MaxDataTrust.Snapshot
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
            }
        }
    }
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
