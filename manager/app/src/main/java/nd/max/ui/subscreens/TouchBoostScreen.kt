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

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import nd.max.R
import nd.max.core.hardware.TouchChannel
import nd.max.core.hardware.TouchChannelReading
import nd.max.core.hardware.TouchChannelState
import nd.max.core.hardware.TouchControllerClassifier
import nd.max.core.hardware.TouchInputProbe
import nd.max.core.hardware.TouchRateChannels
import nd.max.core.hardware.TouchRateProbe
import nd.max.core.hardware.TouchRateSample
import nd.max.ui.design.MaxCollapsibleGroup
import nd.max.ui.design.MaxCondition
import nd.max.ui.design.MaxConditionKind
import nd.max.ui.design.MaxDataTrust
import nd.max.ui.design.MaxGroup
import nd.max.ui.design.MaxGroupDivider
import nd.max.ui.design.MaxMetric
import nd.max.ui.design.MaxMetricLine
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxRow
import nd.max.ui.design.MaxScreen
import nd.max.ui.design.MaxSection
import nd.max.ui.design.MaxSpace
import nd.max.ui.design.MaxSwitchRow
import nd.max.ui.design.MaxTone
import nd.max.ui.design.maxRadius
import nd.max.ui.viewmodel.TouchBoostViewModel

/**
 * Touch response.
 *
 * Rebuilt on the MaxManager Design Language. What changed and why:
 *
 *  - The old screen showed a primaryContainer hero with "ON"/"OFF" text plus
 *    three "Game / Rate / DT2W" tiles that repeated what the switches below
 *    already said. Removed: a hero that restates the controls is decoration.
 *  - The old screen used the SAME row component to mean two different things:
 *    in "Response tuning" the check icon meant "boost is on", in "Driver
 *    information" the identical icon meant "this node exists". That is a real
 *    confusion bug, not a style issue. Capability and state are now separated:
 *    switches express state, the Providers section expresses what the kernel
 *    actually exposes, with data-trust marking.
 *  - Node paths are read once by loadState(), so they are presented as
 *    Snapshot, never as live telemetry, and a missing node renders as
 *    unavailable instead of an empty line.
 *  - Disabled controls now state the reason (no writable node / no HAL)
 *    instead of being silently greyed out.
 *  - Hardcoded English UI copy was moved to string resources.
 *  - The whole row is the touch target (48dp min) via MaxSwitchRow.
 */
@Composable
fun TouchBoostScreen(
    navController: NavController,
    viewModel: TouchBoostViewModel = viewModel()
) {
    LaunchedEffect(Unit) { viewModel.loadState() }

    // معدّل النظام يُقرأ مرّة عند الفتح (قراءة عرض ثابتة لا بثّ)، والقياس حالة جلسة لا تُخزَّن.
    val context = LocalContext.current
    val refreshRate = remember(context) { TouchRateProbe.displayRefreshRate(context) }
    var measured by remember { mutableStateOf<TouchRateSample?>(null) }

    // حالة المفتاح لحظة أخذ آخر عيّنة — بدونها يقرأ المستخدم رقمًا لا يعرف ظرفه، ولا يستطيع
    // مقارنة «قبل/بعد» بنفسه. وهذه حالة جلسة لا تُخزَّن، مثل العيّنة نفسها.
    var measuredWhileBoost by remember { mutableStateOf<Boolean?>(null) }

    // قفل تمرير الصفحة ما دام الإصبع على وسادة القياس. اللمس هنا اختبارٌ لا تنقّل: بدونه يسرق
    // مُمرِّر الصفحة الإيماءة بعد أول انزلاق فتُقطع العيّنة وتتحرّك البطاقة تحت اليد.
    var padHeld by remember { mutableStateOf(false) }

    // هوية المحكّم من الواجهة العامّة لـAndroid: قراءة أجهزة الإدخال مرّة عند الفتح، ثم تصنيف
    // الاسم وحده. ولا يدخل طراز الهاتف في أيّ منهما (§٧-أ).
    val inputDevices = remember(context) { TouchInputProbe.touchDevices(context) }
    val controller = remember(inputDevices) { TouchControllerClassifier.identify(inputDevices) }
    val deviceName = controller.device?.name?.takeIf { it.isNotBlank() }

    val gameNode = viewModel.gameModeNode
    val sampleNode = viewModel.sampleRateNode
    val inputBoostNode = viewModel.inputBoostNode
    val dt2wNode = viewModel.doubleTapNode

    // الحال التي يصفها مفتاح التعزيز: نمط لعب حقيقي إن وُجد، وإلا تعزيز إدخال CPU. والواجهة
    // تقول ما وجدته لا ما تتمنّاه — لأن الجهاز المقيس (Redmi rodin) لا يعرض إلا الثانية.
    val inputBoostOnly = gameNode == null && sampleNode == null && inputBoostNode != null
    // وجود نقل المصنّع لا يعني امتلاك طريق كتابة فيه: الأول يُقاس بالخدمة المعلنة، والثاني
    // يحتاج معنى أوضاع مُثبَتًا ولا يُثبت بلا جهاز. فلكلٍّ سطره، ولا يُخلط أحدهما بالآخر.
    val halWritable = viewModel.vendorHalDetected
    val halDeclared = viewModel.vendorTransportPresent

    // القنوات الأربع تُبنى من نموذج واحد، فتستحيل شاشة تجمع رقمين في سؤال واحد.
    val channels = TouchRateChannels.snapshot(
        family = controller.family,
        sample = measured,
        vendorTransportPresent = halDeclared,
        vendorWritePathProven = halWritable
    )

    // A write provider is a verified **writable** sysfs node, or a vendor bridge that has proved
    // its write path. A merely-declared transport is NOT a provider: nothing is written into it.
    // وتعزيز إدخال CPU عقدة تحكّم مقيسة أيضًا: استبعادها هنا كان سيُطفئ المفتاح على الجهاز
    // المقيس (Redmi rodin) بعد فصلها عن «نمط اللعب» — أي «لا يعمل» جديدة باسم إصلاح.
    //
    // والشرط صار **قابلية الكتابة** لا الوجود: العقدة المقروءة تُثبت أنها تُقرأ فقط، وكتابتها
    // تُرفض على جهاز بلا جذر — فيُلمس المفتاح ويرتدّ بلا سبب. القياس في
    // `TouchControlBackend.writableProvider`، ويُقرأ مرّة عند فتح الشاشة.
    val canWriteBoost = viewModel.boostWritable

    // معدّل اللمس لا يُضبط إلا بعقدة معدّل **مقروءة وقابلة للكتابة**. ولا عقدة معدّل مثبتة اليوم
    // (`SAMPLE_RATE_NODES` فارغة عن قصد لأن وحدة القيمة غير مُثبتة)، فالسطر يبقى «غير قابل
    // للضبط» بصدق — ولا تُرسم أزرار معدّلات لا تكتب شيئًا.
    val canWriteRate = sampleNode != null && viewModel.boostWritable

    val sysfsSource = stringResource(R.string.max_touch_source_sysfs)
    val halSource = stringResource(R.string.max_touch_source_hal)
    val missing = stringResource(R.string.max_touch_node_missing)

    val condition = when (viewModel.isAvailable) {
        null -> MaxCondition(
            kind = MaxConditionKind.Loading,
            title = stringResource(R.string.max_touch_probe_title),
            detail = stringResource(R.string.max_touch_probe_detail)
        )

        // **ولا تُحجب القياسات على جهاز بلا عقدة تحكّم (§٧-ج):** الحالة تُعرض إشعارًا فوق
        // محتوى يبقى متاحًا — هوية المحكّم، والقنوات الأربع، والقياس البديل. الحجب كان يجعل
        // «لا نملك طريق كتابة» يساوي «لا شيء يُعرف»، وهو ما تمنعه الخطة نصًّا.
        else -> null
    }

    val unsupportedNotice = if (viewModel.isAvailable == false) {
        MaxCondition(
            kind = MaxConditionKind.Unsupported,
            title = stringResource(R.string.max_touch_unsupported_title),
            detail = stringResource(
                // والفرق مقصود: روم يُعلن خدمة لمس ولا نملك طريق كتابة مثبَتًا ليس كجهاز بلا شيء
                // — الأول يشرح سبب الامتناع، والثاني يشرح غياب العتاد. وكلام واحد لهما كان يكذب.
                if (halWritable || halDeclared) R.string.touch_boost_unavailable_vendor_hal
                else R.string.touch_boost_unavailable
            ),
            // The real reason, not "something went wrong": these are the exact
            // paths that were probed and did not answer.
            technicalDetail = stringResource(
                R.string.touch_boost_node_note,
                gameNode?.path ?: missing,
                sampleNode?.path ?: missing
            ),
            primaryActionLabel = stringResource(R.string.max_action_recheck),
            onPrimaryAction = { viewModel.loadState() }
        )
    } else {
        null
    }

    MaxScreen(
        title = stringResource(R.string.touch_boost_title),
        onBack = { navController.popBackStack() },
        // التمرير يتوقّف طوال وضع الإصبع/الكف على وسادة القياس، لا الإيماءة وحدها — نفس
        // عقد `AudioStudioScreen`: البطاقة لا تنزلق تحت يدٍ تقيس.
        scrollEnabled = !padHeld,
        subtitle = gameNode?.path
            ?: sampleNode?.path
            ?: inputBoostNode?.path
            ?: when {
                halWritable -> stringResource(R.string.max_touch_provider_hal)
                halDeclared -> stringResource(R.string.max_touch_provider_hal_read_only)
                else -> null
            },
        accentIcon = Icons.Rounded.TouchApp,
        accent = MaterialTheme.colorScheme.secondary,
        condition = condition,
        banner = unsupportedNotice,
        actions = {
            IconButton(onClick = { viewModel.loadState() }) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = stringResource(R.string.max_action_recheck)
                )
            }
        }
    ) {
        MaxSection(
            title = stringResource(R.string.max_touch_section_controls),
            description = stringResource(R.string.max_touch_section_controls_desc)
        ) {
            MaxGroup {
                MaxSwitchRow(
                    // على جهاز لا يعرض إلا تعزيز إدخال CPU، يُسمّى المفتاح بما يفعله: تردّد
                    // المعالج. أما حيث توجد عقدة نمط لعب حقيقية فيبقى وصفها الأصلي.
                    title = stringResource(
                        if (inputBoostOnly) R.string.max_touch_input_boost_title else R.string.touch_boost_title
                    ),
                    subtitle = stringResource(
                        if (inputBoostOnly) R.string.max_touch_input_boost_desc else R.string.touch_boost_desc
                    ),
                    checked = viewModel.boostEnabled,
                    onCheckedChange = viewModel::setBoost,
                    icon = Icons.Rounded.Bolt,
                    iconTone = MaxTone.Accent,
                    enabled = canWriteBoost,
                    lockedReason = stringResource(R.string.max_touch_boost_locked),
                    onStateDescription = stringResource(R.string.max_touch_state_on),
                    offStateDescription = stringResource(R.string.max_touch_state_off)
                )

                MaxGroupDivider()

                // Shown even when unsupported, so the user learns the device
                // lacks the node instead of wondering where the setting went.
                MaxSwitchRow(
                    title = stringResource(R.string.touch_dt2w_title),
                    subtitle = stringResource(R.string.touch_dt2w_desc),
                    checked = viewModel.doubleTapEnabled,
                    onCheckedChange = viewModel::setDoubleTapToWake,
                    icon = Icons.Rounded.Nightlight,
                    iconTone = MaxTone.Neutral,
                    enabled = dt2wNode != null,
                    lockedReason = stringResource(R.string.max_touch_dt2w_locked),
                    onStateDescription = stringResource(R.string.max_touch_state_on),
                    offStateDescription = stringResource(R.string.max_touch_state_off)
                )
            }

            // Honest explanation of the revert behaviour the ViewModel already
            // implements: a rejected write flips the switch back.
            Text(
                text = stringResource(R.string.max_touch_revert_notice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = MaxSpace.groupPadding)
            )
        }

        // ── معدّل اللمس: قياس واحد يُعرض، ومصادره تُطوى في التفاصيل ───────────────────────
        //
        // **لماذا هذا الشكل (بلاغ المالك ٢٠٢٦-١٠-٢٠):** كانت الشاشة تعرض «Touch rates» بأربع
        // قنوات ثم «Providers» بمسارات العُقَد — قسمين يقولان الشيء نفسه ثلاث مرّات: «Not
        // supported · Source: sysfs probe». فيقرأ المستخدم جدول تشخيص لا شاشة إعداد، ولا يجد
        // ما جاء من أجله: معدّل اللمس. فالقياس صار في الصدر، والمصادر خلف طويّة واحدة.
        //
        // **وحدّ لا يُخترق:** لا أزرار لمعدّلات «مدعومة» إلا حيث توجد عقدة تُقرأ وتُكتب فعلًا
        // (`canWriteRate`). فزرٌّ لا يكتب رقمًا هو وعدٌ كاذب — وهو نفس ما مُنع في `TouchRateChannels`.
        MaxSection(
            title = stringResource(R.string.max_touch_rate_title),
            description = stringResource(R.string.max_touch_rate_desc)
        ) {
            MaxGroup {
                MaxMetricLine(
                    metric = MaxMetric(
                        label = stringResource(R.string.max_touch_rate_measured_label),
                        value = measured?.eventsPerSecond?.let { "%.0f".format(it) },
                        unit = stringResource(R.string.max_touch_rate_unit_events),
                        trust = if (measured != null) MaxDataTrust.Snapshot else MaxDataTrust.Loading,
                        source = stringResource(R.string.max_touch_rate_measured_source),
                        note = if (measured != null) {
                            stringResource(R.string.max_touch_rate_single_sample)
                        } else {
                            stringResource(R.string.max_touch_rate_not_measured)
                        }
                    )
                )
            }

            // سببٌ واحد واضح بدل أربعة صفوف «Not supported»: لا معدّل قابل للضبط هنا.
            Text(
                text = stringResource(
                    if (canWriteRate) R.string.max_touch_rate_adjustable else R.string.max_touch_rate_fixed
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = MaxSpace.groupPadding)
            )

            TouchRatePad(
                onPressChange = { padHeld = it },
                onSample = {
                    measured = it
                    measuredWhileBoost = viewModel.boostEnabled
                }
            )

            if (measured != null) {
                MaxGroup {
                    MaxMetricLine(
                        metric = MaxMetric(
                            label = stringResource(R.string.max_touch_rate_sample_state_label),
                            value = stringResource(
                                if (measuredWhileBoost == true) R.string.max_touch_state_on
                                else R.string.max_touch_state_off
                            ),
                            trust = MaxDataTrust.Snapshot,
                            source = stringResource(R.string.max_touch_rate_measured_source),
                            note = stringResource(R.string.max_touch_rate_sample_state_note)
                        )
                    )
                }
            }

            MaxCollapsibleGroup(
                title = stringResource(R.string.max_touch_details_title),
                summary = stringResource(R.string.max_touch_details_summary)
            ) {
                // القنوات الأربع تبقى كاملة — لكن لمن فتح التفاصيل، لا لكل من دخل الشاشة.
                MaxGroup {
                    channels.forEachIndexed { index, reading ->
                        if (index > 0) MaxGroupDivider()
                        MaxMetricLine(metric = touchChannelMetric(reading))
                    }
                }

                MaxGroup {
                    MaxMetricLine(
                        metric = MaxMetric(
                            label = stringResource(R.string.max_touch_rate_native_label),
                            value = refreshRate?.let { "%.0f".format(it) },
                            unit = "Hz",
                            trust = if (refreshRate != null) MaxDataTrust.Snapshot
                            else MaxDataTrust.Unsupported,
                            source = stringResource(R.string.max_touch_rate_native_source),
                            note = if (refreshRate == null) missing
                            else stringResource(R.string.max_touch_rate_panel_note)
                        )
                    )
                }

                // المسارات: صفّ لكل عقدة مقروءة — والغائب يُذكر بسبب، لا يُحذف.
                MaxGroup {
                    MaxMetricLine(
                        metric = MaxMetric(
                            label = stringResource(R.string.max_touch_node_game),
                            value = gameNode?.path,
                            trust = if (gameNode != null) MaxDataTrust.Snapshot else MaxDataTrust.Unsupported,
                            source = sysfsSource,
                            note = if (gameNode == null) missing else null
                        )
                    )
                    MaxGroupDivider()
                    MaxMetricLine(
                        metric = MaxMetric(
                            label = stringResource(R.string.max_touch_node_sample),
                            value = sampleNode?.path,
                            trust = if (sampleNode != null) MaxDataTrust.Snapshot else MaxDataTrust.Unsupported,
                            source = sysfsSource,
                            note = if (sampleNode == null) missing else null
                        )
                    )
                    MaxGroupDivider()
                    MaxMetricLine(
                        metric = MaxMetric(
                            label = stringResource(R.string.max_touch_node_input_boost),
                            value = inputBoostNode?.path,
                            trust = if (inputBoostNode != null) MaxDataTrust.Snapshot else MaxDataTrust.Unsupported,
                            source = sysfsSource,
                            note = if (inputBoostNode == null) missing
                            else stringResource(R.string.max_touch_node_input_boost_note)
                        )
                    )
                    MaxGroupDivider()
                    MaxMetricLine(
                        metric = MaxMetric(
                            label = stringResource(R.string.max_touch_node_dt2w),
                            value = dt2wNode?.path,
                            trust = if (dt2wNode != null) MaxDataTrust.Snapshot else MaxDataTrust.Unsupported,
                            source = sysfsSource,
                            note = if (dt2wNode == null) missing else null
                        )
                    )
                    if (halWritable || halDeclared) {
                        MaxGroupDivider()
                        MaxMetricLine(
                            metric = MaxMetric(
                                label = stringResource(R.string.max_touch_provider_hal),
                                value = halSource,
                                trust = if (halWritable) MaxDataTrust.Snapshot else MaxDataTrust.Unsupported,
                                source = halSource,
                                note = stringResource(
                                    if (halWritable) R.string.max_touch_provider_hal_detail
                                    else R.string.max_touch_provider_hal_declared_detail
                                )
                            )
                        )
                    }
                }

                // هوية المحكّم أيضاً تفصيل: تُقرأ عند الحاجة، ولا تزاحم معدّل اللمس.
                MaxGroup {
                    MaxMetricLine(
                        metric = MaxMetric(
                            label = stringResource(R.string.max_touch_controller_input),
                            value = deviceName,
                            trust = if (deviceName != null) MaxDataTrust.Snapshot
                            else MaxDataTrust.Unsupported,
                            source = stringResource(R.string.max_touch_controller_source),
                            note = if (deviceName == null) {
                                stringResource(R.string.max_touch_controller_none)
                            } else {
                                null
                            }
                        )
                    )
                    MaxGroupDivider()
                    MaxMetricLine(
                        metric = MaxMetric(
                            label = stringResource(R.string.max_touch_controller_family),
                            value = controller.family.brand
                                ?: stringResource(R.string.max_touch_controller_unknown),
                            trust = when {
                                deviceName == null -> MaxDataTrust.Unsupported
                                controller.identified -> MaxDataTrust.Snapshot
                                else -> MaxDataTrust.Unreadable
                            },
                            source = stringResource(R.string.max_touch_controller_source),
                            note = when {
                                deviceName == null ->
                                    stringResource(R.string.max_touch_controller_none_note)
                                controller.identified ->
                                    stringResource(R.string.max_touch_controller_family_note)
                                else -> stringResource(R.string.max_touch_controller_unknown_note)
                            }
                        )
                    )
                    if (controller.idsLabel != null) {
                        MaxGroupDivider()
                        MaxMetricLine(
                            metric = MaxMetric(
                                label = stringResource(R.string.max_touch_controller_ids),
                                value = controller.idsLabel,
                                trust = MaxDataTrust.Snapshot,
                                source = stringResource(R.string.max_touch_controller_source)
                            )
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.max_touch_rate_limits),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = MaxSpace.groupPadding)
                )
            }
        }

        MaxSection(title = stringResource(R.string.max_touch_section_about)) {
            MaxGroup {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = MaxSpace.rowPaddingHorizontal,
                            vertical = MaxSpace.rowPaddingVertical
                        ),
                    verticalArrangement = Arrangement.spacedBy(MaxSpace.sm)
                ) {
                    Text(
                        text = stringResource(
                            if (inputBoostOnly) R.string.max_touch_input_boost_info else R.string.touch_boost_info
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (inputBoostOnly) {
                            stringResource(
                                R.string.max_touch_input_boost_node_note,
                                inputBoostNode?.path ?: missing
                            )
                        } else {
                            stringResource(
                                R.string.touch_boost_node_note,
                                gameNode?.path ?: missing,
                                sampleNode?.path ?: missing
                            )
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            MaxGroup {
                MaxRow(
                    title = stringResource(R.string.touch_engine_safety_title),
                    subtitle = stringResource(R.string.touch_engine_safety_desc),
                    icon = Icons.Rounded.Shield,
                    iconTone = MaxTone.Caution
                )
            }
        }
    }
}

/**
 * يحوّل قناة من نموذج [TouchRateChannels] إلى سطر عرض — والترجمة هنا وحدها.
 *
 * والقيمة `null` تمرّ كما هي: «مجهول» يبقى شرطةً مع سبب، ولا يصير صفرًا ولا رقمًا من قناة أخرى.
 */
@Composable
private fun touchChannelMetric(reading: TouchChannelReading): MaxMetric {
    val label = stringResource(
        when (reading.channel) {
            TouchChannel.HARDWARE_SAMPLING -> R.string.max_touch_channel_hardware_label
            TouchChannel.DOCUMENTED_LIMIT -> R.string.max_touch_channel_official_label
            TouchChannel.ANDROID_EVENTS -> R.string.max_touch_rate_measured_label
            TouchChannel.EXPERIMENTAL_VENDOR -> R.string.max_touch_channel_experimental_label
        }
    )
    val source = stringResource(
        when (reading.channel) {
            TouchChannel.HARDWARE_SAMPLING -> R.string.max_touch_channel_hardware_source
            TouchChannel.DOCUMENTED_LIMIT -> R.string.max_touch_channel_official_source
            TouchChannel.ANDROID_EVENTS -> R.string.max_touch_rate_measured_source
            TouchChannel.EXPERIMENTAL_VENDOR -> R.string.max_touch_channel_experimental_source
        }
    )
    val note = stringResource(
        when (reading.note) {
            TouchRateChannels.NOTE_VERIFIED_ENTRY -> R.string.max_touch_channel_verified_entry
            TouchRateChannels.NOTE_NO_PUBLIC_READ_PATH -> R.string.max_touch_channel_hardware_note
            TouchRateChannels.NOTE_NO_DOCUMENTED_ENTRY -> R.string.max_touch_channel_official_note
            TouchRateChannels.NOTE_NOT_MEASURED -> R.string.max_touch_rate_not_measured
            TouchRateChannels.NOTE_SAMPLE_TOO_SHORT -> R.string.max_touch_rate_too_short
            TouchRateChannels.NOTE_SINGLE_SAMPLE -> R.string.max_touch_rate_single_sample
            TouchRateChannels.NOTE_VENDOR_WRITE_PATH_PROVEN ->
                R.string.max_touch_channel_experimental_available
            TouchRateChannels.NOTE_VENDOR_CONTRACT_UNPROVEN ->
                R.string.max_touch_channel_experimental_note
            else -> R.string.max_touch_channel_experimental_absent
        }
    )
    val trust = when (reading.state) {
        TouchChannelState.MEASURED,
        TouchChannelState.DOCUMENTED,
        TouchChannelState.AVAILABLE -> MaxDataTrust.Snapshot

        TouchChannelState.NOT_MEASURED -> MaxDataTrust.Loading

        TouchChannelState.NOT_READABLE,
        TouchChannelState.NOT_REGISTERED,
        TouchChannelState.REFUSED_CONTRACT,
        TouchChannelState.UNAVAILABLE -> MaxDataTrust.Unsupported
    }
    val unit = when (reading.unit) {
        TouchRateChannels.UNIT_HZ -> "Hz"
        TouchRateChannels.UNIT_EVENTS_PER_SECOND -> stringResource(R.string.max_touch_rate_unit_events)
        else -> null
    }
    return MaxMetric(
        label = label,
        value = reading.value?.let { "%.0f".format(it) },
        unit = unit,
        trust = trust,
        source = source,
        note = note
    )
}

/**
 * وسادة القياس: تعدّ أحداث اللمس التي يسلّمها Android لهذه الشاشة، وتُعلن العيّنة عند رفع الإصبع.
 *
 * **ولماذا في الواجهة لا في محرّك خلفي:** القياس على `PointerEvent` الواصل إلى هذه الشاشة، لا
 * على العتاد؛ فمحلّه من يستقبله. والعدّاد حالة جلسة لا تُخزَّن — ومن أراد عيّنة أخرى يلمسها من
 * جديد. ولا يكتب شيئًا: قياس لا إعداد.
 *
 * **ولا يسمح للصفحة بأن تسحب الإيماءة منه:** الحدث يُقرأ ويُستهلك في [PointerEventPass.Initial]
 * كي لا يرى مُمرِّر الصفحة حركةً غير مستهلكة فيبدأ السحب. و[onPressChange] يقفل تمرير الصفحة
 * كلها طوال اللمس — وهذا هو الفرق بين «بطاقة تقيس» و«بطاقة تنزلق تحت الإصبع».
 */
@Composable
private fun TouchRatePad(
    onPressChange: (Boolean) -> Unit,
    onSample: (TouchRateSample) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    var events by remember { mutableIntStateOf(0) }
    var startMs by remember { mutableLongStateOf(0L) }
    var endMs by remember { mutableLongStateOf(0L) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MaxSpace.rowPaddingHorizontal)
            .height(88.dp)
            .clip(RoundedCornerShape(maxRadius(MaxRadius.group)))
            .background(scheme.surfaceVariant)
            .pointerInput(Unit) {
                awaitEachGesture {
                    // المرور الأول: نقرأ ونستهلك قبل أن يصل الحدث إلى مُمرِّر الصفحة في المرور
                    // الرئيسي. و`requireUnconsumed = false` لأن هذه الوسادة ملك الإيماءة وحدها.
                    val down = awaitFirstDown(
                        requireUnconsumed = false,
                        pass = PointerEventPass.Initial
                    )
                    down.consume()
                    onPressChange(true)
                    events = 1
                    startMs = down.uptimeMillis
                    endMs = down.uptimeMillis
                    try {
                        var anyPressed = true
                        while (anyPressed) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            event.changes.forEach { it.consume() }
                            anyPressed = event.changes.any { it.pressed }
                            if (anyPressed) {
                                events += 1
                                endMs = event.changes.maxOfOrNull { it.uptimeMillis } ?: endMs
                            }
                        }
                    } finally {
                        // حتى لو أُلغي اللمس (خروج من الشاشة، استيلاء نظام)، لا يبقى التمرير
                        // مقفولًا بعد رفع اليد.
                        onPressChange(false)
                    }
                    if (endMs > startMs) {
                        onSample(TouchRateSample(events, endMs - startMs))
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.max_touch_rate_pad_hint),
            style = MaterialTheme.typography.labelLarge,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = MaxSpace.groupPadding)
        )
    }
}
