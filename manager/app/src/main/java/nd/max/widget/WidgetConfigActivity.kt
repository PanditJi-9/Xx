/*
 * Copyright (C) 2026-2027 MaxManager contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nd.max.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import nd.max.R
import nd.max.ui.design.MaxGroup
import nd.max.ui.design.MaxSwitchRow
import nd.max.ui.theme.MaxManagerTheme
import nd.max.widget.controls.ControlsReceiver
import nd.max.widget.controls.ControlsWidget
import nd.max.widget.core.ControlButton
import nd.max.widget.core.PulseStat
import nd.max.widget.core.WidgetAccent
import nd.max.widget.core.WidgetConfig
import nd.max.widget.core.WidgetConfigStore
import nd.max.widget.core.WidgetKind
import nd.max.widget.core.WidgetTheme
import nd.max.widget.core.WidgetThemeMode
import nd.max.widget.pulse.PulseReceiver
import nd.max.widget.pulse.PulseWidget
import nd.max.widget.storage.StorageReceiver
import nd.max.widget.storage.StorageWidget

/**
 * نشاط تخصيص الودجة — يُطلقه المشغّل عبر `android.appwidget.action.APPWIDGET_CONFIGURE`
 * عند إضافة ودجة، ويُعاد فتحه من زرّ «إعادة التخصيص».
 *
 * **والحراسة قبل الرسم:** لا يُقبل نشاطٌ لم يُمرَّر له `appWidgetId` صالح، ولا
 * معرّف **ليس من ناقلاتنا الثلاثة** ([widgetKindFor]) — فالنشاط مُصدَّر
 * (`exported`) لأن المشغّل عملية أخرى تطلقه، وهذا هو الحرس الذي يمنع أن يصير
 * بابًا لتمرير نيّة غريبة: لا يقرأ ولا يكتب إلا لودجة يملكها التطبيق.
 *
 * والواجهة من مكوّنات التطبيق نفسها ([MaxGroup] و[MaxSwitchRow]) لا نسخة
 * مصغّرة موازية — فالشاشة التي تُخصّص هي الشاشة التي يستعملها المستخدم أصلًا.
 */
class WidgetConfigActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appWidgetId = intent
            ?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, WidgetConfigStore.INVALID_ID)
            ?: WidgetConfigStore.INVALID_ID
        val kind = widgetKindFor(this, appWidgetId)
        if (kind == null) {
            setResult(RESULT_OK, resultIntent(appWidgetId))
            finish()
            return
        }

        val store = WidgetConfigStore(this)
        setContent {
            MaxManagerTheme {
                WidgetConfigScreen(
                    kind = kind,
                    initial = remember { store.load(appWidgetId, kind) },
                    onCancel = {
                        setResult(RESULT_CANCELED, resultIntent(appWidgetId))
                        finish()
                    },
                    onConfirm = { config ->
                        store.save(appWidgetId, config)
                        lifecycleScope.launch {
                            runCatching {
                                val manager = GlanceAppWidgetManager(this@WidgetConfigActivity)
                                widgetFor(kind).update(
                                    context = this@WidgetConfigActivity,
                                    id = manager.getGlanceIdBy(appWidgetId),
                                )
                            }.recoverCatching {
                                widgetFor(kind).updateAll(this@WidgetConfigActivity)
                            }
                            setResult(RESULT_OK, resultIntent(appWidgetId))
                            finish()
                        }
                    },
                )
            }
        }
    }

    private fun resultIntent(appWidgetId: Int): Intent =
        Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)

    private fun widgetFor(kind: WidgetKind): GlanceAppWidget = when (kind) {
        WidgetKind.Pulse -> PulseWidget()
        WidgetKind.Controls -> ControlsWidget()
        WidgetKind.Storage -> StorageWidget()
    }

    companion object {

        /** نوع الودجة من مالكها الحقيقيّ — أو `null` إن لم تكن من ودجاتنا. */
        internal fun widgetKindFor(context: Context, appWidgetId: Int): WidgetKind? {
            if (appWidgetId == WidgetConfigStore.INVALID_ID) return null
            val provider = AppWidgetManager.getInstance(context)
                .getAppWidgetInfo(appWidgetId)
                ?.provider
                ?: return null
            val cls = provider.className
            return when {
                cls.endsWith("PulseReceiver") -> WidgetKind.Pulse
                cls.endsWith("ControlsReceiver") -> WidgetKind.Controls
                cls.endsWith("StorageReceiver") -> WidgetKind.Storage
                else -> null
            }
        }
    }
}

@Composable
private fun WidgetConfigScreen(
    kind: WidgetKind,
    initial: WidgetConfig,
    onCancel: () -> Unit,
    onConfirm: (WidgetConfig) -> Unit,
) {
    var mode by remember { mutableStateOf(initial.mode) }
    var accent by remember { mutableStateOf(initial.accent) }
    var stats by remember { mutableStateOf(initial.stats) }
    var buttons by remember { mutableStateOf(initial.buttons) }
    var showPercent by remember { mutableStateOf(initial.showStoragePercent) }
    var showBar by remember { mutableStateOf(initial.showStorageBar) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            Text(
                text = stringResource(titleRes(kind)),
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(descriptionRes(kind)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(20.dp))
            SectionTitle(R.string.widget_cfg_appearance)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WidgetThemeMode.entries.forEach { candidate ->
                    FilterChip(
                        selected = mode == candidate,
                        onClick = { mode = candidate },
                        label = { Text(stringResource(modeLabelRes(candidate))) },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.widget_cfg_accent),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                WidgetAccent.entries.forEach { candidate ->
                    val isChosen = accent == candidate
                    val name = stringResource(accentLabelRes(candidate))
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .selectable(selected = isChosen, onClick = { accent = candidate })
                            .semantics { contentDescription = name }
                            .then(
                                if (isChosen) {
                                    Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                } else {
                                    Modifier
                                },
                            )
                            .padding(6.dp)
                            .background(Color(WidgetTheme.accent(candidate).day), CircleShape),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionTitle(R.string.widget_cfg_content)
            Spacer(Modifier.height(8.dp))
            MaxGroup {
                when (kind) {
                    WidgetKind.Pulse -> PulseOptions(stats = stats, onChange = { stats = it })
                    WidgetKind.Controls -> ButtonOptions(buttons = buttons, onChange = { buttons = it })
                    WidgetKind.Storage -> StorageOptions(
                        showPercent = showPercent,
                        showBar = showBar,
                        onPercent = { showPercent = it },
                        onBar = { showBar = it },
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                OutlinedButton(onClick = onCancel) {
                    Text(stringResource(R.string.widget_cfg_cancel))
                }
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = {
                        onConfirm(
                            WidgetConfig(
                                mode = mode,
                                accent = accent,
                                stats = stats,
                                buttons = buttons,
                                showStoragePercent = showPercent,
                                showStorageBar = showBar,
                            ),
                        )
                    },
                ) {
                    Text(stringResource(R.string.widget_cfg_done))
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(@StringRes res: Int) {
    Text(
        text = stringResource(res),
        style = MaterialTheme.typography.titleMedium,
    )
}

@Composable
private fun PulseOptions(stats: Set<PulseStat>, onChange: (Set<PulseStat>) -> Unit) {
    PulseStat.Ordered.forEach { stat ->
        val checked = stat in stats
        MaxSwitchRow(
            title = stringResource(stat.labelRes),
            checked = checked,
            onCheckedChange = { on ->
                onChange(if (on) stats + stat else stats - stat)
            },
            // آخر مقياس لا يُطفأ: ودجة بلا قياس ليست ودجة — والمنع هنا قبل المخزن.
            enabled = !(checked && stats.size == 1),
        )
    }
}

@Composable
private fun ButtonOptions(buttons: List<ControlButton>, onChange: (List<ControlButton>) -> Unit) {
    ControlButton.Ordered.forEach { button ->
        val checked = button in buttons
        MaxSwitchRow(
            title = stringResource(button.labelRes),
            checked = checked,
            onCheckedChange = { on ->
                onChange(
                    if (on) {
                        (ControlButton.Ordered.filter { it in buttons || it == button })
                            .take(ControlButton.MAX_SELECTED)
                    } else {
                        buttons - button
                    },
                )
            },
            enabled = checked || buttons.size < ControlButton.MAX_SELECTED,
        )
    }
}

@Composable
private fun StorageOptions(
    showPercent: Boolean,
    showBar: Boolean,
    onPercent: (Boolean) -> Unit,
    onBar: (Boolean) -> Unit,
) {
    MaxSwitchRow(
        title = stringResource(R.string.widget_cfg_storage_percent),
        checked = showPercent,
        onCheckedChange = onPercent,
    )
    MaxSwitchRow(
        title = stringResource(R.string.widget_cfg_storage_bar),
        checked = showBar,
        onCheckedChange = onBar,
    )
}

@StringRes
private fun titleRes(kind: WidgetKind): Int = when (kind) {
    WidgetKind.Pulse -> R.string.widget_pulse_name
    WidgetKind.Controls -> R.string.widget_controls_name
    WidgetKind.Storage -> R.string.widget_storage_name
}

@StringRes
private fun descriptionRes(kind: WidgetKind): Int = when (kind) {
    WidgetKind.Pulse -> R.string.widget_pulse_description
    WidgetKind.Controls -> R.string.widget_controls_description
    WidgetKind.Storage -> R.string.widget_storage_description
}

@StringRes
private fun modeLabelRes(mode: WidgetThemeMode): Int = when (mode) {
    WidgetThemeMode.System -> R.string.widget_cfg_mode_system
    WidgetThemeMode.Light -> R.string.widget_cfg_mode_light
    WidgetThemeMode.Dark -> R.string.widget_cfg_mode_dark
}

@StringRes
private fun accentLabelRes(accent: WidgetAccent): Int = when (accent) {
    WidgetAccent.Brand -> R.string.widget_cfg_accent_brand
    WidgetAccent.Teal -> R.string.widget_cfg_accent_teal
    WidgetAccent.Violet -> R.string.widget_cfg_accent_violet
    WidgetAccent.Amber -> R.string.widget_cfg_accent_amber
    WidgetAccent.Rose -> R.string.widget_cfg_accent_rose
    WidgetAccent.Green -> R.string.widget_cfg_accent_green
}
