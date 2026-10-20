/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.subscreens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import nd.max.R
import nd.max.core.gamespace.ComboVerdict
import nd.max.core.gamespace.GameApp
import nd.max.core.gamespace.GameCombo
import nd.max.core.gamespace.GameComboCatalog
import nd.max.core.gamespace.GameComboCodec
import nd.max.core.gamespace.GameComboStore
import nd.max.core.gamespace.GameComboVetting
import nd.max.ui.component.AppIconImage
import nd.max.ui.component.LobbyPalette
import nd.max.ui.component.canvasScaleLabel
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxSize
import nd.max.ui.design.MaxSpace
import nd.max.ui.util.AppConfig
import nd.max.ui.util.canvasScaleOptions
import nd.max.ui.util.comboSnapshot
import nd.max.ui.util.comboValue
import nd.max.ui.util.getSupportedRefreshRates
import nd.max.ui.design.maxRadius

/**
 * §1.1 — **ورقة المجموعات (Combos)**: إنشاء وتعديل وتطبيق وحذف، بأحكام صريحة لا بوعود.
 *
 * ### كيف «تُطبَّق» مجموعة — والصدق في الطريق
 *
 * عند التطبيق تُعرض المجموعة على الجهاز أوّلًا ([GameComboVetting]): مفاتيحها وقيمها تُقابَل
 * بالكتالوج وبقدرات الجهاز المقروءة الآن (معدلات التحديث المدعومة ومعاملات الدقة المدعومة)،
 * ثم **المقبول وحده يُسلَّم** إلى المسار الواحد `updateSetting` — فيمرّ بقاعدة مالك المقبض
 * (`gpu_profile` ← `gpu_max_freq`) وحدود العتاد كما يمرّ أي إعداد مفرد في ملفّ اللعبة، ولا
 * كتابة sysfs من الواجهة (ADR-11). ويُعرض بعدها سطر صادق: كم قُبل من كم، وما ليس مدعومًا على
 * هذا الجهاز، وما رُفض — **ولا يُكتب شيء ثم يُقال «تم»**. والمحرّك يطبّق ما سُلِّم عند واجهة
 * اللعبة، وهو حدّ `gaming_runtime_boundary` المكتوب أسفل الورقة كما هو في ورقة الملفّ.
 *
 * ### ونموذج التحرير = نموذج الملفّ نفسه
 *
 * صفوف المحرّر هي صفوف ملفّ اللعبة ذاتها ([LobbyOptionRow] والمكوّنات المشتركة معها): لمس قيمة
 * يضيفها إلى المجموعة، ولمس المضافة يُسقطها — فالمجموعة **اختيارات**، وما لم يُختر لا يُكتب
 * ولا يُلمس أصلًا. والمفاتيح المجهولة (لو حُرّر المخزن بيد) لا تظهر في المحرّر لكنها تُذكر
 * «مرفوضة» عند التطبيق — لا يُهرس ما لم نفهمه.
 */

/** أقسام المحرّر: كل مفتاح في موضعه من واجهة الملفّ، فلا يتعلّم المستخدم شاشتين. */
internal val ComboKeysBySection: Map<GameProfileSection, List<String>> = mapOf(
    GameProfileSection.Touch to listOf("touch_boost", "haptic_feedback"),
    GameProfileSection.Performance to listOf("perf_lite_mode", "cpu_boost", "kill_bg_apps", "game_preload"),
    GameProfileSection.Gpu to listOf("gpu_profile", "force_hw_ui"),
    GameProfileSection.Show to listOf("refresh_rate", "resolution_downscale"),
    GameProfileSection.Net to listOf("wifi_no_sleep"),
    GameProfileSection.Function to listOf("dnd_on_gaming", "disable_notifs", "bypass_charging"),
)

/** ما يُقال عند فشل الحفظ أو اكتمال ناقص — ومعه صمتٌ مقصود عند النجاح: الصفّ يظهر حالًا. */
private enum class ComboNotice { SaveFailed, NameRequired, EmptyCombo }

@Composable
internal fun GameCombosDialog(
    app: GameApp,
    config: AppConfig,
    onUpdate: (key: String, value: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var combos by remember { mutableStateOf(GameComboStore.load(context)) }
    var draft by remember { mutableStateOf<GameCombo?>(null) }
    var draftName by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf<ComboNotice?>(null) }
    var deleteTarget by remember { mutableStateOf<GameCombo?>(null) }
    var verdicts by remember { mutableStateOf<Map<String, ComboVerdict>>(emptyMap()) }

    // قراءتان تُقاسان هنا ولا تُفترضان: قائمة معدلات التحديث الفعلية للمخزن، ومعاملات الدقة
    // المدعومة من ضلع الشاشة — وهي عين ما تعرضه ورقة الملفّ، فتظل الصورتان واحدة.
    val nativeShortSide = remember {
        val m = context.resources.displayMetrics
        minOf(m.widthPixels, m.heightPixels)
    }
    val deviceValues = remember(nativeShortSide) {
        mapOf(
            "refresh_rate" to getSupportedRefreshRates(context).toSet(),
            "resolution_downscale" to canvasScaleOptions(nativeShortSide, null)
                .filter { it.supported }
                .map { it.stored }
                .toSet()
        )
    }

    fun commit(next: List<GameCombo>) {
        if (GameComboStore.save(context, next)) {
            combos = next
            notice = null
        } else {
            notice = ComboNotice.SaveFailed
        }
    }

    fun applyCombo(combo: GameCombo) {
        val verdict = GameComboVetting.vet(combo, deviceValues)
        verdict.accepted.forEach { (key, value) -> onUpdate(key, value) }
        verdicts = verdicts + (combo.id to verdict)
    }

    fun beginCreate() {
        draft = GameCombo(id = "", name = "", entries = config.comboSnapshot(GameComboCatalog.ALL_KEYS))
        draftName = ""
        notice = null
    }

    fun beginEdit(combo: GameCombo) {
        draft = combo
        draftName = combo.name
        notice = null
    }

    fun saveDraft() {
        val current = draft ?: return
        val name = draftName.trim()
        when {
            name.isEmpty() -> notice = ComboNotice.NameRequired
            current.entries.isEmpty() -> notice = ComboNotice.EmptyCombo
            else -> {
                val stored = current.copy(id = current.id.ifEmpty { GameCombo.newId() }, name = name)
                val next = if (combos.any { it.id == stored.id }) {
                    combos.map { if (it.id == stored.id) stored else it }
                } else {
                    combos + stored
                }
                commit(next)
                draft = null
                draftName = ""
            }
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.86f)
                    .clip(RoundedCornerShape(topStart = SheetCorner, topEnd = SheetCorner))
                    .background(LobbyPalette.Surface)
                    // يبتلع اللمس حتى لا تُغلق الورقة من داخلها.
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
            ) {
                ComboHeader(app = app, editing = draft != null, onBack = { draft = null }, onDismiss = onDismiss)
                val editing = draft
                if (editing == null) {
                    ComboListContent(
                        modifier = Modifier.weight(1f),
                        combos = combos,
                        verdicts = verdicts,
                        notice = notice,
                        onApply = ::applyCombo,
                        onEdit = ::beginEdit,
                        onDelete = { deleteTarget = it },
                        onCreate = ::beginCreate
                    )
                } else {
                    ComboEditorContent(
                        modifier = Modifier.weight(1f),
                        draft = editing,
                        name = draftName,
                        onName = { draftName = it },
                        onToggle = { key, value ->
                            val entries = LinkedHashMap(editing.entries)
                            if (entries[key] == value) entries.remove(key) else entries[key] = value
                            draft = editing.copy(entries = entries)
                        },
                        onSave = ::saveDraft,
                        onCancel = { draft = null; notice = null },
                        config = config,
                        nativeShortSide = nativeShortSide,
                        notice = notice
                    )
                }
            }
        }
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.combos_delete_title)) },
            text = { Text(stringResource(R.string.combos_delete_body, target.name)) },
            confirmButton = {
                TextButton(onClick = {
                    commit(combos.filterNot { it.id == target.id })
                    verdicts = verdicts - target.id
                    deleteTarget = null
                }) { Text(stringResource(R.string.combos_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.combos_cancel)) }
            }
        )
    }
}

@Composable
private fun ComboHeader(app: GameApp, editing: Boolean, onBack: () -> Unit, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MaxSpace.xl, vertical = MaxSpace.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.md)
    ) {
        AppIconImage(packageName = app.packageName, size = 36.dp, contentDescription = null)
        Text(
            text = stringResource(if (editing) R.string.combos_title_edit else R.string.combos_title, app.label),
            color = LobbyPalette.Ink,
            fontSize = 20.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (editing) {
            Text(
                text = stringResource(R.string.combos_back),
                color = LobbyPalette.Muted,
                fontSize = 14.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(maxRadius(MaxRadius.control)))
                    .clickable(role = Role.Button, onClick = onBack)
                    .padding(horizontal = MaxSpace.sm, vertical = MaxSpace.xs)
            )
        }
        val closeLabel = stringResource(R.string.lobby_close_settings)
        Box(
            modifier = Modifier
                .size(MaxSize.minTouchTarget)
                .clickable(role = Role.Button, onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Close, contentDescription = closeLabel, tint = LobbyPalette.Muted)
        }
    }
}

@Composable
private fun ComboListContent(
    modifier: Modifier = Modifier,
    combos: List<GameCombo>,
    verdicts: Map<String, ComboVerdict>,
    notice: ComboNotice?,
    onApply: (GameCombo) -> Unit,
    onEdit: (GameCombo) -> Unit,
    onDelete: (GameCombo) -> Unit,
    onCreate: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MaxSpace.lg, vertical = MaxSpace.md),
        verticalArrangement = Arrangement.spacedBy(MaxSpace.md)
    ) {
        Text(text = stringResource(R.string.combos_desc), color = LobbyPalette.Muted, fontSize = 13.sp)

        if (combos.isEmpty()) {
            Text(text = stringResource(R.string.combos_empty), color = LobbyPalette.Muted, fontSize = 14.sp)
        }

        combos.forEach { combo ->
            LobbySectionCard(title = combo.name) {
                Text(
                    text = stringResource(R.string.combos_entries_count, combo.entries.size),
                    color = LobbyPalette.Muted,
                    fontSize = 13.sp
                )
                // محتوى المجموعة بأسماء المفاتيح — يُقرأ ولا يُخمَّن.
                Text(
                    text = combo.entries.keys.map { comboKeyLabel(it) }.joinToString(" · "),
                    color = LobbyPalette.Muted,
                    fontSize = 13.sp
                )
                verdicts[combo.id]?.let { verdict -> ComboVerdictBlock(verdict) }
                Row(horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm)) {
                    ComboAction(
                        icon = Icons.Rounded.PlayArrow,
                        label = stringResource(R.string.combos_apply),
                        tint = LobbyPalette.Cyan,
                        onClick = { onApply(combo) }
                    )
                    ComboAction(
                        icon = Icons.Rounded.Edit,
                        label = stringResource(R.string.combos_edit),
                        tint = LobbyPalette.Ink,
                        onClick = { onEdit(combo) }
                    )
                    ComboAction(
                        icon = Icons.Rounded.DeleteOutline,
                        label = stringResource(R.string.combos_delete),
                        tint = LobbyPalette.RedBright,
                        onClick = { onDelete(combo) }
                    )
                }
            }
        }

        notice?.let {
            Text(text = noticeText(it), color = LobbyPalette.RedBright, fontSize = 13.sp)
        }

        OutlinedButton(
            onClick = onCreate,
            border = BorderStroke(MaxSize.hairlineBorder, LobbyPalette.Hairline),
            modifier = Modifier.heightIn(min = MaxSize.minTouchTarget)
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, tint = LobbyPalette.Ink, modifier = Modifier.size(18.dp))
            Text(
                text = stringResource(R.string.combos_new),
                color = LobbyPalette.Ink,
                modifier = Modifier.padding(start = MaxSpace.sm)
            )
        }

        Text(
            text = stringResource(R.string.gaming_runtime_boundary),
            color = LobbyPalette.Muted,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = MaxSpace.xs)
        )
    }
}

@Composable
private fun ComboEditorContent(
    modifier: Modifier = Modifier,
    draft: GameCombo,
    name: String,
    onName: (String) -> Unit,
    onToggle: (String, String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    config: AppConfig,
    nativeShortSide: Int,
    notice: ComboNotice?
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MaxSpace.lg, vertical = MaxSpace.md),
        verticalArrangement = Arrangement.spacedBy(MaxSpace.md)
    ) {
        // حقل الاسم بنمط حقل البحث في «إدارة الألعاب» نفسه — لا نمط حقل جديد يُخترع.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MaxSize.minTouchTarget)
                .clip(RoundedCornerShape(maxRadius(MaxRadius.control)))
                .background(LobbyPalette.Panel)
                .padding(horizontal = MaxSpace.md),
            contentAlignment = Alignment.CenterStart
        ) {
            if (name.isEmpty()) {
                Text(text = stringResource(R.string.combos_name_hint), color = LobbyPalette.Muted, fontSize = 15.sp)
            }
            BasicTextField(
                value = name,
                onValueChange = { onName(it.take(GameComboCodec.MAX_NAME_LENGTH)) },
                singleLine = true,
                textStyle = TextStyle(color = LobbyPalette.Ink, fontSize = 15.sp),
                cursorBrush = SolidColor(LobbyPalette.RedBright),
                modifier = Modifier.fillMaxWidth()
            )
        }
        Text(text = stringResource(R.string.combos_pick_hint), color = LobbyPalette.Muted, fontSize = 12.sp)

        GameProfileSection.entries.forEach { section ->
            val keys = ComboKeysBySection[section].orEmpty()
            if (keys.isNotEmpty()) {
                LobbySectionCard(title = stringResource(section.headRes)) {
                    keys.forEach { key ->
                        ComboValueRow(
                            key = key,
                            selected = draft.entries[key],
                            current = config.comboValue(key),
                            nativeShortSide = nativeShortSide,
                            onToggle = { value -> onToggle(key, value) }
                        )
                    }
                }
            }
        }

        notice?.let {
            Text(text = noticeText(it), color = LobbyPalette.RedBright, fontSize = 13.sp)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(MaxSpace.md)) {
            OutlinedButton(
                onClick = onCancel,
                border = BorderStroke(MaxSize.hairlineBorder, LobbyPalette.Hairline),
                modifier = Modifier.heightIn(min = MaxSize.minTouchTarget)
            ) {
                Text(text = stringResource(R.string.combos_cancel), color = LobbyPalette.Ink)
            }
            Box(
                modifier = Modifier
                    .heightIn(min = MaxSize.minTouchTarget)
                    .clip(RoundedCornerShape(maxRadius(MaxRadius.control)))
                    .background(LobbyPalette.Red)
                    .clickable(role = Role.Button, onClick = onSave)
                    .padding(horizontal = MaxSpace.xl),
                contentAlignment = Alignment.Center
            ) {
                Text(text = stringResource(R.string.combos_save), color = LobbyPalette.Ink, fontSize = 15.sp)
            }
        }

        Text(
            text = stringResource(R.string.gaming_runtime_boundary),
            color = LobbyPalette.Muted,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = MaxSpace.xs)
        )
    }
}

/** صفّ قيمة داخل المحرّر: الخيارات من مجال المفتاح نفسه، والاختيار يعني «في المجموعة». */
@Composable
private fun ComboValueRow(
    key: String,
    selected: String?,
    current: String,
    nativeShortSide: Int,
    onToggle: (String) -> Unit
) {
    val context = LocalContext.current
    val defaultLabel = stringResource(R.string.default_label)
    val options: List<Pair<String, String>> = when (key) {
        "gpu_profile" -> listOf(
            "default" to defaultLabel,
            "power" to stringResource(R.string.lobby_gpu_power),
            "balanced" to stringResource(R.string.lobby_gpu_balanced),
            "gaming" to stringResource(R.string.lobby_gpu_gaming),
            "performance" to stringResource(R.string.lobby_gpu_performance)
        )

        "touch_boost" -> listOf(
            "default" to stringResource(R.string.lobby_touch_follow),
            "true" to stringResource(R.string.lobby_touch_responsive),
            "false" to stringResource(R.string.lobby_touch_battery)
        )

        "refresh_rate" -> getSupportedRefreshRates(context)
            .map { rate -> rate to if (rate.equals("default", ignoreCase = true)) defaultLabel else "${rate}Hz" }

        "resolution_downscale" -> canvasScaleOptions(nativeShortSide, selected ?: current)
            .filter { it.supported }
            .map { it.stored to canvasScaleLabel(it) }

        else -> listOf(
            "default" to defaultLabel,
            "true" to stringResource(R.string.lobby_state_on),
            "false" to stringResource(R.string.lobby_state_off)
        )
    }
    LobbyOptionRow(
        title = comboKeyLabel(key),
        options = options,
        // `null` تعني «ليست في المجموعة» — ولا رقاقة مضيئة، فالاختيار ليس قيمةً هنا بل عضوية.
        selected = selected.orEmpty(),
        onSelect = onToggle
    )
}

@Composable
private fun ComboVerdictBlock(verdict: ComboVerdict) {
    // التسمية قبل الجمع: `map` مضمَّنة فتسمح بنداء composable، و`joinToString` ليست.
    val unsupportedLabels = verdict.unsupported.map { comboKeyLabel(it) }
    val rejectedLabels = verdict.rejected.map { comboKeyLabel(it) }
    Column(verticalArrangement = Arrangement.spacedBy(MaxSpace.xs)) {
        Text(
            text = stringResource(R.string.combos_delivered, verdict.acceptedCount, verdict.totalCount),
            color = LobbyPalette.Ink,
            fontSize = 13.sp
        )
        if (unsupportedLabels.isNotEmpty()) {
            Text(
                text = stringResource(R.string.combos_unsupported, unsupportedLabels.joinToString(", ")),
                color = LobbyPalette.RedBright,
                fontSize = 13.sp
            )
        }
        if (rejectedLabels.isNotEmpty()) {
            Text(
                text = stringResource(R.string.combos_rejected, rejectedLabels.joinToString(", ")),
                color = LobbyPalette.RedBright,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun ComboAction(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .heightIn(min = MaxSize.minTouchTarget)
            .clip(RoundedCornerShape(maxRadius(MaxRadius.control)))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = MaxSpace.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.xs)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Text(text = label, color = tint, fontSize = 14.sp)
    }
}

@Composable
private fun noticeText(notice: ComboNotice): String = stringResource(
    when (notice) {
        ComboNotice.SaveFailed -> R.string.combos_save_failed
        ComboNotice.NameRequired -> R.string.combos_name_required
        ComboNotice.EmptyCombo -> R.string.combos_empty_combo
    }
)

/** اسم المفتاح للمستخدم من نفس نصوص ورقة الملفّ؛ وما لا اسم له يُعرض بمعرّفه — لا يُخفى بند. */
@Composable
private fun comboKeyLabel(key: String): String {
    val res = when (key) {
        "touch_boost" -> R.string.lobby_row_touch
        "haptic_feedback" -> R.string.lobby_row_haptics
        "perf_lite_mode" -> R.string.lobby_row_lite
        "cpu_boost" -> R.string.lobby_row_cpuboost
        "kill_bg_apps" -> R.string.lobby_row_killbg
        "game_preload" -> R.string.lobby_row_preload
        "force_hw_ui" -> R.string.lobby_row_hwui
        "wifi_no_sleep" -> R.string.lobby_row_wifi
        "dnd_on_gaming" -> R.string.lobby_row_dnd
        "disable_notifs" -> R.string.lobby_row_notifs
        "bypass_charging" -> R.string.lobby_row_bypass
        "gpu_profile" -> R.string.lobby_row_gpu
        "refresh_rate" -> R.string.lobby_row_refresh
        "resolution_downscale" -> R.string.lobby_row_downscale
        else -> null
    }
    return if (res != null) stringResource(res) else key
}

/** نصف قطر الورقة نفسه في ورقة الملفّ — قيمتان متساويتان اليوم، ومرجعٌ واحد يمنع انحرافهما. */
private val SheetCorner = 28.dp
