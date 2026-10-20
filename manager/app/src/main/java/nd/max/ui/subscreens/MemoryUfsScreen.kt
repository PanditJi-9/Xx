/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.subscreens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeveloperBoard
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.navigation.NavController
import nd.max.R
import nd.max.core.hardware.HardwareDeviceOverlay
import nd.max.core.hardware.MemoryUfsHardwareBackend
import nd.max.core.hardware.ThermalGuard
import nd.max.core.maxai.MemoryStall
import nd.max.ui.component.StudioTonalButton
import nd.max.ui.design.MaxChoiceRow
import nd.max.ui.design.MaxCollapsibleGroup
import nd.max.ui.design.MaxCondition
import nd.max.ui.design.MaxConditionKind
import nd.max.ui.design.MaxGroup
import nd.max.ui.design.MaxRow
import nd.max.ui.design.MaxScreen
import nd.max.ui.design.MaxSection
import nd.max.ui.design.MaxSliderRow
import nd.max.ui.design.MaxSpace
import nd.max.ui.design.MaxTone
import nd.max.ui.settings.rememberAdvancedMode
import nd.max.ui.theme.MonoValueStyleSmall
import nd.max.ui.util.MemoryUfsModel
import nd.max.ui.viewmodel.MemoryUfsOperation
import nd.max.ui.viewmodel.MemoryUfsOutcome
import nd.max.ui.viewmodel.MemoryUfsUiState
import nd.max.ui.viewmodel.MemoryUfsViewModel

/**
 * استوديو **الذاكرة وUFS** — تردّدات الذاكرة (DVFS/عرض النطاق) وتردّدات التخزين (UFS/eMMC/NVMe).
 *
 * ما تفعله هذه الشاشة
 * -------------------
 * تقرأ كل جهاز `devfreq` صنّفه المحرّك ذاكرةً أو تخزينًا، وتعرض عليه بروفايل سقف وأرضية
 * **بالدرجات التي أعلنتها النواة وحدها**، وتُمرّر الكتابة من المُحكِّم بمعاملة كاملة (خط أساس،
 * ثم كتابة، ثم قراءة مرتجعة، ثم استرجاع عند الانحراف) — فلا قيمة مُخترعة، ولا كتابة بلا استرجاع.
 *
 * وما تفعله **أكثر** من المرجع
 * --------------------------
 * المشروع المرجعيّ (Rodin Essential) يكتب مسارين مكتوبين بالاسم على شريحة MediaTek واحدة،
 * وواجهته تعرض تحكّمًا واحدًا للذاكرة وواحدًا لـUFS. وهنا:
 *
 * 1. **الاكتشاف لا الاسم:** العُقَد تُعدَّد وتُصنَّف من اسم ما أعلنته النواة، فيعمل التحكّم على
 *    MediaTek وQualcomm وExynos وبأي كيرنل عامّ يُصدّر العقدة.
 * 2. **كل جهاز بمقبضه:** نواة تُصدّر ثلاثة أجهزة ذاكرة تعطي ثلاثة صفوف ومفتاحًا لكلٍّ
 *    (`mem_frequency:<device>`) — مع خيار «طبّق على الكلّ» لمن يريد تطبيقًا واحدًا.
 * 3. **أرضية لا سقفًا فقط:** أرضية تردّد الذاكرة تقصّر زمن التوقّف عند نقص عرض النطاق، وهي
 *    مقبضٌ لم يُعرض في المرجع.
 * 4. **قياسُ الخنق:** ضغط الذاكرة (`PSI`) والخنق الحراري (`ThermalStatus`) معروضان هنا، ويُقترح
 *    منهما **اقتراح أرضية** بزرّ واحد — والاقتراح لا يُطبَّق من تلقاء نفسه.
 * 5. **صدق الفشل:** كل رفض يخرج برمزه (وحدة غير مُثبتة · سلّم غير معلن · عقدة للقراءة فقط)
 *    بجانب الجهاز نفسه، لا برسالة عامّة.
 *
 * وحدودها المُعلنة: هذا موضع **كتابة** عتاد، والمُخاطرة فيه «متقدّمة» في شجرة الوجهات؛
 * والسلوك على جهاز حقيقيّ (أيّ كيرنل يُصدّر أيّ عقدة، وهل تقبل الكتابة) يبقى `needs device`.
 */
@Composable
fun MemoryUfsScreen(
    navController: NavController,
    viewModel: MemoryUfsViewModel = hiltViewModel(),
) {
    LifecycleResumeEffect(Unit) {
        viewModel.load()
        onPauseOrDispose { viewModel.pausePolling() }
    }

    val state = viewModel.state
    val advanced = rememberAdvancedMode()
    val memoryDevice = viewModel.selectedMemory
    val storageDevice = viewModel.selectedStorage
    val hasDevices = state.devices.isNotEmpty()

    var floorIndex by remember { mutableStateOf(0f) }
    LaunchedEffect(state.floorNode, memoryDevice?.path) {
        floorIndex = floorIndexFor(memoryDevice, state.floorNode)
    }

    val condition = when {
        state.loading -> MaxCondition(
            kind = MaxConditionKind.Loading,
            title = stringResource(R.string.memufs_scanning_title),
            detail = stringResource(R.string.memufs_scanning_detail),
        )

        !hasDevices -> MaxCondition(
            kind = MaxConditionKind.Unsupported,
            title = stringResource(R.string.memufs_unsupported_title),
            // والسبب رمزٌ مقيس من الاكتشاف لا جملة، وثلاثُ حالاتٍ لا اثنتان كما في العُقَد:
            // «عدّدتُ فلم أجد» · «لم أستطع العدّ» · «لم يكن الصنف في هذه النواة أصلًا» — والأخير
            // وحده يُعلن غيابًا مقيسًا، فلا يُطلب جذرٌ لا يُغيّر شيئًا.
            detail = when {
                state.reason == MemoryUfsHardwareBackend.REASON_ROOT_ABSENT ->
                    stringResource(R.string.memufs_unsupported_detail_absent)

                state.scanned -> stringResource(R.string.memufs_unsupported_detail_scanned)
                else -> stringResource(R.string.memufs_unsupported_detail_unreadable)
            },
            technicalDetail = "/sys/class/devfreq",
            primaryActionLabel = stringResource(R.string.max_action_recheck),
            onPrimaryAction = { viewModel.pausePolling(); viewModel.load() },
        )

        else -> null
    }

    val banner = operationBanner(viewModel, state.operation)

    MaxScreen(
        title = stringResource(R.string.memufs_title),
        onBack = { navController.popBackStack() },
        accentIcon = Icons.Rounded.Memory,
        subtitle = stringResource(R.string.memufs_subtitle),
        condition = condition,
        banner = banner,
    ) {
        state.notice?.let { notice ->
            if (notice == MemoryUfsViewModel.NOTICE_DRIFTED) {
                MaxSection(title = stringResource(R.string.memufs_drifted_title)) {
                    MaxGroup {
                        MaxRow(
                            title = stringResource(R.string.memufs_drifted_detail),
                            icon = Icons.Rounded.DeveloperBoard,
                            iconTone = MaxTone.Caution,
                        )
                    }
                }
            }
        }

        // ── ١. الحالة: ما قيس الآن ─────────────────────────────────────────────────────────────
        MaxSection(
            title = stringResource(R.string.memufs_section_state),
            description = stringResource(R.string.memufs_section_state_desc),
        ) {
            MaxGroup {
                MaxRow(
                    title = stringResource(R.string.memufs_row_pressure),
                    subtitle = stringResource(R.string.memufs_row_pressure_desc),
                    icon = Icons.Rounded.Memory,
                    trailing = { MonoText(pressureText(state.stall)) },
                )
                MaxRow(
                    title = stringResource(R.string.memufs_row_thermal),
                    subtitle = stringResource(R.string.memufs_row_thermal_desc),
                    icon = Icons.Rounded.DeveloperBoard,
                    trailing = { MonoText(thermalText(state.thermal)) },
                )
                MaxRow(
                    title = stringResource(R.string.memufs_row_devices),
                    subtitle = stringResource(R.string.memufs_row_devices_desc),
                    icon = Icons.Rounded.Storage,
                    trailing = {
                        MonoText(
                            stringResource(
                                R.string.memufs_devices_value,
                                viewModel.memoryDevices.size,
                                viewModel.storageDevices.size,
                            ),
                        )
                    },
                )
            }
        }

        // ── ٢. الذاكرة ─────────────────────────────────────────────────────────────────────────
        DeviceSection(
            kind = MemoryUfsHardwareBackend.Kind.MEMORY,
            devices = viewModel.memoryDevices,
            selected = memoryDevice,
            state = state,
            advanced = advanced,
            floorIndex = floorIndex,
            onFloorIndexChange = { floorIndex = it },
            onFloorCommit = { viewModel.setFloor(floorNodeFor(memoryDevice, floorIndex)) },
            onSelect = viewModel::selectMemory,
            onMode = viewModel::setMode,
            onApply = { viewModel.apply(MemoryUfsHardwareBackend.Kind.MEMORY) },
            adviceFor = viewModel::floorAdviceFor,
        )

        // ── ٣. التخزين ─────────────────────────────────────────────────────────────────────────
        DeviceSection(
            kind = MemoryUfsHardwareBackend.Kind.STORAGE,
            devices = viewModel.storageDevices,
            selected = storageDevice,
            state = state,
            advanced = advanced,
            floorIndex = 0f,
            onFloorIndexChange = {},
            onFloorCommit = {},
            onSelect = viewModel::selectStorage,
            onMode = viewModel::setMode,
            onApply = { viewModel.apply(MemoryUfsHardwareBackend.Kind.STORAGE) },
            adviceFor = viewModel::floorAdviceFor,
        )

        // ── ٤. تعلّم أجهزة لم يُعرف صنفها ───────────────────────────────────────────────────────────
        //
        // وهذا هو الفرق الجوهري عن مشروع مرجعيّ يكتب مسارات جهازٍ بعينه في سياسته: لا نضيف اسمًا
        // إلى الشفرة، بل **نعلن ما لا نعرفه** ويعلّمنا صاحبه — فيعمل التصنيف على جهاز لم نره بعد.
        if (state.unclassified.isNotEmpty()) {
            MaxSection(
                title = stringResource(R.string.memufs_teach_title, state.unclassified.size),
                description = stringResource(R.string.memufs_teach_desc),
            ) {
                state.unclassified.forEach { unknown ->
                    MaxGroup {
                        MaxRow(
                            title = unknown.directory,
                            subtitle = stringResource(
                                R.string.memufs_teach_facts,
                                unknown.label,
                                unknown.ladderSize,
                                stringResource(
                                    if (unknown.writable) R.string.memufs_teach_writable else R.string.memufs_teach_readonly,
                                ),
                            ),
                            icon = Icons.Rounded.DeveloperBoard,
                            iconTone = MaxTone.Caution,
                        )
                        MemoryUfsActionRow {
                            StudioTonalButton(
                                onClick = { viewModel.teachDevice(unknown.directory, MemoryUfsHardwareBackend.Kind.MEMORY) },
                            ) {
                                Text(stringResource(R.string.memufs_teach_memory))
                            }
                            StudioTonalButton(
                                onClick = { viewModel.teachDevice(unknown.directory, MemoryUfsHardwareBackend.Kind.STORAGE) },
                            ) {
                                Text(stringResource(R.string.memufs_teach_storage))
                            }
                        }
                    }
                }
            }
        }

        // ── ٤ب. ما رُصد ولم يُقرأ ─────────────────────────────────────────────────────────────────
        //
        // وليس تكرارًا للسابق: ذاك عُقَد أجابت ونقصها **اسم صنفها** (فعلُ المستخدم نقرةُ تعليم)،
        // وهذه لم تُجب بشيء (فعلُه عبورُ مجلّد محجوب أو مسارٌ مصحَّح). ولا تُعرض هنا أزرار تعليم
        // عن قصد: لا شيء قيسَ يُعلَّم — وهذا نصّ ما يجعل القسمين مختلفين لا متكررين.
        if (state.unreadable.isNotEmpty()) {
            MaxSection(
                title = stringResource(R.string.memufs_unreadable_title, state.unreadable.size),
                description = stringResource(R.string.memufs_unreadable_desc),
            ) {
                MaxGroup {
                    state.unreadable.forEach { directory ->
                        MaxRow(
                            title = directory,
                            subtitle = stringResource(R.string.memufs_unreadable_facts),
                            icon = Icons.Rounded.VisibilityOff,
                            iconTone = MaxTone.Caution,
                        )
                    }
                }
            }
        }

        // ── ٥. تصنيفاتك، وما لم يُقبل منها ────────────────────────────────────────────────────────
        if (state.taughtDevices.isNotEmpty() || state.overlayRejections.isNotEmpty()) {
            MaxSection(
                title = stringResource(R.string.memufs_taught_title),
                description = stringResource(R.string.memufs_taught_desc),
            ) {
                if (state.taughtDevices.isNotEmpty()) {
                    MaxGroup {
                        state.taughtDevices.forEach { device ->
                            MemoryUfsActionRow {
                                StudioTonalButton(
                                    onClick = { viewModel.forgetDevice(device) },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(stringResource(R.string.memufs_taught_forget, device))
                                }
                            }
                        }
                    }
                }
                if (state.overlayRejections.isNotEmpty()) {
                    MaxGroup {
                        MaxRow(
                            title = stringResource(R.string.memufs_overlay_rejected_title),
                            subtitle = stringResource(R.string.memufs_overlay_rejected_desc),
                            icon = Icons.Rounded.DeveloperBoard,
                            iconTone = MaxTone.Caution,
                        )
                        state.overlayRejections.take(OVERLAY_REJECTION_ROWS).forEach { rejection ->
                            MaxRow(
                                title = stringResource(
                                    R.string.memufs_overlay_rejected_line,
                                    rejection.line,
                                    rejection.text,
                                ),
                                subtitle = overlayReasonText(rejection.reason),
                            )
                        }
                    }
                }
            }
        }

        // ── ٦. التراجع العام ───────────────────────────────────────────────────────────────────
        MaxSection(
            title = stringResource(R.string.memufs_section_revert),
            description = stringResource(R.string.memufs_section_revert_desc),
        ) {
            MaxGroup {
                MaxRow(
                    title = stringResource(R.string.memufs_apply_all),
                    subtitle = stringResource(R.string.memufs_apply_all_desc),
                    icon = Icons.Rounded.Storage,
                    trailing = {
                        Switch(
                            checked = state.applyToAll,
                            onCheckedChange = viewModel::setApplyToAll,
                        )
                    },
                )
                MemoryUfsActionRow {
                    StudioTonalButton(
                        onClick = viewModel::revert,
                        enabled = !state.applying,
                    ) {
                        Text(stringResource(R.string.memufs_revert))
                    }
                }
            }
        }
    }
}

/**
 * قسم نوعٍ واحد (ذاكرة أو تخزين): اختيار الجهاز، والبروفايل، والأرضية، والتنفيذ، والتفصيل المتقدّم.
 *
 * ومكتوبٌ مرّة واحدة للنوعين لأن الفرق بينهما **بيانات** (النوع والمفتاح) لا منطق — ونسخةٌ
 * ثانية من المنطق تعني قاعدةً تُصلَح في أحدهما وتُنسى في الآخر.
 */
@Composable
private fun DeviceSection(
    kind: MemoryUfsHardwareBackend.Kind,
    devices: List<MemoryUfsHardwareBackend.Device>,
    selected: MemoryUfsHardwareBackend.Device?,
    state: MemoryUfsUiState,
    advanced: Boolean,
    floorIndex: Float,
    onFloorIndexChange: (Float) -> Unit,
    onFloorCommit: () -> Unit,
    onSelect: (String) -> Unit,
    onMode: (MemoryUfsModel.Mode) -> Unit,
    onApply: () -> Unit,
    adviceFor: (MemoryUfsHardwareBackend.Device) -> Long?,
) {
    val isMemory = kind == MemoryUfsHardwareBackend.Kind.MEMORY
    val title = stringResource(if (isMemory) R.string.memufs_section_memory else R.string.memufs_section_storage)
    val icon = if (isMemory) Icons.Rounded.Memory else Icons.Rounded.Storage
    val lock = MemoryUfsModel.lock(selected)

    MaxSection(
        title = title,
        description = if (devices.isEmpty()) {
            // «لم يُوجد» ليست «لم يُقرأ»: العدّاد فرغ بعد قراءة قائمة، فالغياب مُعلن لا مُدّعى.
            stringResource(R.string.memufs_kind_none)
        } else {
            stringResource(R.string.memufs_kind_count, devices.size)
        },
    ) {
        if (devices.isEmpty()) return@MaxSection

        MaxGroup {
            devices.forEach { device ->
                MaxChoiceRow(
                    title = MemoryUfsModel.deviceLabel(device),
                    selected = selected?.path == device.path,
                    onSelect = { onSelect(device.path) },
                    subtitle = listOfNotNull(
                        MemoryUfsModel.frequencyText(device, device.currentFreq)
                            ?.let { stringResource(R.string.memufs_device_current, it) },
                        MemoryUfsModel.rangeText(device)
                            ?.let { stringResource(R.string.memufs_device_range, it) },
                    ).joinToString(" · ").takeIf(String::isNotBlank),
                    enabled = devices.size > 1,
                )
            }
        }

        MaxGroup {
            if (lock != null) {
                MaxRow(
                    title = stringResource(R.string.memufs_locked_title),
                    subtitle = lockText(lock),
                    icon = icon,
                    iconTone = MaxTone.Caution,
                )
                return@MaxGroup
            }

            val device = selected ?: return@MaxGroup
            MaxChoiceRow(
                title = stringResource(R.string.memufs_mode_capability),
                subtitle = stringResource(R.string.memufs_mode_capability_desc),
                selected = state.mode == MemoryUfsModel.Mode.CAPABILITY,
                onSelect = { onMode(MemoryUfsModel.Mode.CAPABILITY) },
            )
            MaxChoiceRow(
                title = stringResource(R.string.memufs_mode_sustained),
                subtitle = stringResource(R.string.memufs_mode_sustained_desc),
                selected = state.mode == MemoryUfsModel.Mode.SUSTAINED,
                onSelect = { onMode(MemoryUfsModel.Mode.SUSTAINED) },
            )
            MaxChoiceRow(
                title = stringResource(R.string.memufs_mode_balanced),
                selected = state.mode == MemoryUfsModel.Mode.BALANCED,
                onSelect = { onMode(MemoryUfsModel.Mode.BALANCED) },
            )
            MaxChoiceRow(
                title = stringResource(R.string.memufs_mode_efficiency),
                selected = state.mode == MemoryUfsModel.Mode.EFFICIENCY,
                onSelect = { onMode(MemoryUfsModel.Mode.EFFICIENCY) },
            )
        }

        // الأرضية للذاكرة وحدها: مقبضُ التخزين الأرضي نادرًا ما يقبل كتابةً، ولأن تُعرض شرائح
        // لا تعمل شيءٌ لم يُقصَد — يُعلَن النقص بدل ادّعاء قدرة.
        if (isMemory && lock == null && selected != null) {
            val device = selected
            val steps = (device.ladder.size - 1).coerceAtLeast(0)
            val node = floorNodeFor(device, floorIndex)
            MaxGroup {
                MaxSliderRow(
                    title = stringResource(R.string.memufs_floor_title),
                    subtitle = stringResource(R.string.memufs_floor_desc),
                    value = floorIndex,
                    valueRange = 0f..device.ladder.size.toFloat(),
                    steps = steps,
                    valueText = node?.let { MemoryUfsModel.frequencyText(device, it) }
                        ?: stringResource(R.string.memufs_floor_none),
                    onValueChange = onFloorIndexChange,
                    onValueChangeFinished = onFloorCommit,
                )
                val advice = adviceFor(device)
                if (advice != null) {
                    MaxRow(
                        title = stringResource(
                            R.string.memufs_advice_title,
                            MemoryUfsModel.frequencyText(device, advice).orEmpty(),
                        ),
                        subtitle = stringResource(R.string.memufs_advice_desc),
                        icon = icon,
                        iconTone = MaxTone.Accent,
                        onClick = {
                            onFloorIndexChange(floorIndexFor(device, advice))
                            onFloorCommit()
                        },
                    )
                }
            }
        }

        MaxGroup {
            MemoryUfsActionRow {
                StudioTonalButton(
                    onClick = onApply,
                    enabled = lock == null && selected != null && !state.applying,
                ) {
                    Text(stringResource(R.string.memufs_apply))
                }
                if (state.applyToAll && devices.size > 1) {
                    Text(
                        text = stringResource(R.string.memufs_applies_to_all, devices.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (advanced) {
            MaxCollapsibleGroup(
                title = stringResource(R.string.memufs_advanced_title),
                summary = stringResource(R.string.memufs_advanced_summary, selected?.evidence?.size ?: 0),
            ) {
                devices.forEach { device ->
                    MaxRow(title = MemoryUfsModel.deviceLabel(device), subtitle = device.path)
                    MaxRow(
                        title = stringResource(R.string.memufs_device_ladder),
                        trailing = { MonoText(MemoryUfsModel.ladderText(device) ?: "-") },
                    )
                    MaxRow(
                        title = stringResource(R.string.memufs_device_governor),
                        trailing = { MonoText(device.governor ?: "-") },
                    )
                    MaxRow(
                        title = stringResource(R.string.memufs_device_unit),
                        trailing = { MonoText(device.frequencyUnit.name.lowercase()) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MonoText(text: String) {
    Text(
        text = text,
        style = MonoValueStyleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun lockText(lock: MemoryUfsModel.Lock): String = stringResource(
    when (lock) {
        MemoryUfsModel.Lock.NO_DEVICE -> R.string.memufs_locked_no_device
        MemoryUfsModel.Lock.UNIT_UNPROVEN -> R.string.memufs_locked_unit
        MemoryUfsModel.Lock.NO_PUBLISHED_LADDER -> R.string.memufs_locked_ladder
        MemoryUfsModel.Lock.RANGE_READ_ONLY -> R.string.memufs_locked_readonly
        MemoryUfsModel.Lock.NO_CLOCK_READING -> R.string.memufs_locked_no_reading
    },
)

@Composable
private fun pressureText(stall: MemoryStall.Sample): String {
    val full = stall.fullPercent
    if (!stall.measured || full == null) return stringResource(R.string.memufs_value_unmeasured)
    // والنافذة تُذكر مع الرقم: نسبةٌ بلا نافذتها لا تقول عن أي مدى زمني تتكلّم.
    // والرقم يُمرَّر **خامًا** لا منسَّقًا: النصّ يحمل `%1$.1f` فيطلب رقمًا، وتمرير نصّ مُنسَّق
    // مسبقًا يرمي IllegalFormatConversionException عند أول قراءة PSI ناجحة فيُغلق التطبيق.
    return stringResource(R.string.memufs_pressure_value, full, stall.windowSeconds)
}

@Composable
private fun thermalText(pressure: ThermalGuard.Pressure): String = stringResource(
    when (pressure) {
        ThermalGuard.Pressure.UNKNOWN -> R.string.memufs_value_unmeasured
        ThermalGuard.Pressure.NONE -> R.string.memufs_thermal_none
        ThermalGuard.Pressure.LIGHT -> R.string.memufs_thermal_light
        ThermalGuard.Pressure.MODERATE -> R.string.memufs_thermal_moderate
        ThermalGuard.Pressure.SEVERE -> R.string.memufs_thermal_severe
        ThermalGuard.Pressure.CRITICAL -> R.string.memufs_thermal_critical
        ThermalGuard.Pressure.EMERGENCY -> R.string.memufs_thermal_emergency
        ThermalGuard.Pressure.SHUTDOWN -> R.string.memufs_thermal_shutdown
    },
)

/** شريط الحالة غير الحاجب: تطبيق، ثم نتيجة مقيسة، أو رفض برمزه. */
@Composable
private fun operationBanner(viewModel: MemoryUfsViewModel, operation: MemoryUfsOperation?): MaxCondition? =
    when (operation) {
        null -> null

        MemoryUfsOperation.Applying -> MaxCondition(
            kind = MaxConditionKind.Applying,
            title = stringResource(R.string.memufs_applying_title),
            detail = stringResource(R.string.memufs_applying_detail),
        )

        is MemoryUfsOperation.Finished -> MaxCondition(
            kind = if (operation.anyVerified) MaxConditionKind.Applied else MaxConditionKind.Failed,
            title = stringResource(
                if (operation.anyVerified) R.string.memufs_applied_title else R.string.memufs_failed_title,
            ),
            // والتفصيل يقول **ما قُرئ** لا ما طُلب: أوّل نتيجة فاشلة تُسمّى بسببها، ونتيجة ناجحة
            // تُسمّى بقيمتها المقروءة — وفي الحالتين لا يُبنى هنا نصٌّ لم يُقس.
            detail = outcomeDetail(operation.outcomes),
            primaryActionLabel = stringResource(R.string.max_action_dismiss),
            onPrimaryAction = viewModel::dismissOperation,
        )

        is MemoryUfsOperation.Refused -> MaxCondition(
            kind = MaxConditionKind.Failed,
            title = stringResource(R.string.memufs_refused_title),
            detail = reasonText(operation.code),
            technicalDetail = operation.deviceLabel,
            primaryActionLabel = stringResource(R.string.max_action_dismiss),
            onPrimaryAction = viewModel::dismissOperation,
        )

        // التعليم والنسيان حالةٌ ثالثة: لا «طُبّق على العتاد» ولا «فشل كتابة» — تصنيفٌ في ملفّنا.
        is MemoryUfsOperation.Overlay -> MaxCondition(
            kind = when (operation.action) {
                MemoryUfsOperation.Overlay.Action.TAUGHT -> MaxConditionKind.Applied
                MemoryUfsOperation.Overlay.Action.FORGOTTEN -> MaxConditionKind.Applied
                MemoryUfsOperation.Overlay.Action.REFUSED -> MaxConditionKind.Failed
            },
            title = stringResource(
                when (operation.action) {
                    MemoryUfsOperation.Overlay.Action.TAUGHT -> R.string.memufs_taught_done_title
                    MemoryUfsOperation.Overlay.Action.FORGOTTEN -> R.string.memufs_forgotten_title
                    MemoryUfsOperation.Overlay.Action.REFUSED -> R.string.memufs_teach_refused_title
                },
            ),
            detail = operation.code?.let { reasonText(it) }
                ?: stringResource(R.string.memufs_taught_done_detail, operation.device),
            technicalDetail = operation.device,
            primaryActionLabel = stringResource(R.string.max_action_dismiss),
            onPrimaryAction = viewModel::dismissOperation,
        )
    }

@Composable
private fun outcomeDetail(outcomes: List<MemoryUfsOutcome>): String {
    val failed = outcomes.firstOrNull { !it.verified }
    if (failed != null) {
        return stringResource(
            R.string.memufs_failed_detail,
            failed.deviceLabel,
            reasonText(failed.reason ?: MemoryUfsViewModel.WRITE_FAILED),
        )
    }
    val first = outcomes.firstOrNull() ?: return stringResource(R.string.memufs_nothing_reported)
    return stringResource(
        R.string.memufs_applied_detail,
        outcomes.size,
        first.deviceLabel,
        // وما لم يُقرأ يُعرض بلا ادّعاء: شرطة بدل رقم مخترع.
        first.live ?: "-",
    )
}

@Composable
private fun reasonText(code: String): String = stringResource(
    when (code) {
        MemoryUfsViewModel.NO_PROVEN_RANGE -> R.string.memufs_reason_no_range
        MemoryUfsViewModel.NO_DEVICE_CODE -> R.string.memufs_reason_no_device
        MemoryUfsViewModel.NOTHING_TO_REVERT -> R.string.memufs_reason_nothing_to_revert
        "manual-lock" -> R.string.memufs_reason_manual_lock
        "frequency-unit-unproven" -> R.string.memufs_locked_unit
        "range-not-writable" -> R.string.memufs_locked_readonly
        MemoryUfsHardwareBackend.PROBE_NAME_INVALID -> R.string.memufs_reason_probe_name
        MemoryUfsHardwareBackend.PROBE_KIND_UNSUPPORTED -> R.string.memufs_reason_probe_kind
        MemoryUfsHardwareBackend.PROBE_NOT_INTERFACE -> R.string.memufs_reason_probe_interface
        HardwareDeviceOverlay.REASON_GRAPHICS_OWNED -> R.string.memufs_reason_graphics_owned
        MemoryUfsViewModel.PROBE_FAILED -> R.string.memufs_reason_probe_failed
        MemoryUfsViewModel.OVERLAY_NOT_SAVED -> R.string.memufs_reason_overlay_not_saved
        else -> R.string.memufs_reason_write_failed
    },
)

/** سطر الملفّ المرفوض: سببُه بالرمز لا بجملة تُبنى هنا. */
@Composable
private fun overlayReasonText(reason: String): String = stringResource(
    when (reason) {
        HardwareDeviceOverlay.REASON_SHAPE -> R.string.memufs_overlay_reason_shape
        HardwareDeviceOverlay.REASON_NAME_INVALID -> R.string.memufs_overlay_reason_name
        HardwareDeviceOverlay.REASON_UNKNOWN_KIND -> R.string.memufs_overlay_reason_kind
        HardwareDeviceOverlay.REASON_GRAPHICS_OWNED -> R.string.memufs_overlay_reason_graphics
        HardwareDeviceOverlay.REASON_CONFLICT -> R.string.memufs_overlay_reason_conflict
        HardwareDeviceOverlay.REASON_LIMIT -> R.string.memufs_overlay_reason_limit
        HardwareDeviceOverlay.REASON_UNREADABLE -> R.string.memufs_overlay_reason_unreadable
        HardwareDeviceOverlay.REASON_TOO_LARGE -> R.string.memufs_overlay_reason_too_large
        else -> R.string.memufs_overlay_reason_unknown
    },
)

/** أقصى عدد أسطر مرفوضة تُعرض: الملفّ القصير يكفي، وقائمة لا تنتهي تُخفي المهمّ. */
private const val OVERLAY_REJECTION_ROWS = 6

/**
 * موضع الأرضية على الشريط: صفر = «بلا أرضيّة»، و`i+1` = الدرجة `i` من السلّم المُعلن.
 *
 * والموضع يُحسب من السلّم لا من نسبة مئوية: الشريط يقف على درجات وجودية، فما لا يمكن كتابته
 * لا يمكن اختياره.
 */
private fun floorIndexFor(device: MemoryUfsHardwareBackend.Device?, node: Long?): Float {
    if (device == null || node == null) return 0f
    val index = device.ladder.indexOf(node)
    return if (index < 0) 0f else (index + 1).toFloat()
}

private fun floorNodeFor(device: MemoryUfsHardwareBackend.Device?, index: Float): Long? {
    if (device == null) return null
    val position = index.toInt()
    if (position <= 0) return null
    return device.ladder.getOrNull(position - 1)
}

/**
 * صفّ أزرار داخل مجموعة: يحمل الحشو نفسه الذي تحمله صفوف `MaxRow`، فلا يلتصق الزرّ بحافة البطاقة
 * ولا يضغط على ما يليه.
 */
@Composable
private fun MemoryUfsActionRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MaxSpace.rowPaddingHorizontal, vertical = MaxSpace.rowPaddingVertical),
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}
