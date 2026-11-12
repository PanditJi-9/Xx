/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
/*
 * **ورقة «سجّل لعبة» — اختيار اللعبة التي تُسجَّل الآن.**
 *
 * طلب المالك: «اجعل هناك زر لتسجيل لعبة معينة». والزرّ لا يسجّل «ما يظهر الآن» فقط، بل
 * يختار صاحب الجلسة قبل بدئها — فالتسجيل يبدأ بالحزمة المختارة نفسها
 * (`HudRecorder.start(context, pkg)`) لا بما تصادف أنه في المقدمة بعد ثانية.
 *
 * **وثلاثة قرارات هنا:**
 *
 * 1. **القائمة من مصدر المكتبة الواحد** (`GameLibraryAccess.apps`): كل تطبيق له مشغّل،
 *    والألعاب تُعلَّم وتُقدَّم (`detectedGame`). لا قائمة ثانية تفترق عن مكتبة الألعاب.
 * 2. **والبحث يرشّح بالاسم والحزمة معًا:** من يبحث عن `com.pkg` يجده، ومن يبحث باسم
 *    عربي يجده — والترشيح لا يغيّر الترتيب.
 * 3. **والاختيار يفعل شيئين بالترتيب:** يبدأ التسجيل بالحزمة، ثم يفتح اللعبة. الترتيب
 *    مقصود: لو فُتحت أولًا لضاعت أول ثانية قبل أن يعرف المسجّل صاحبها.
 */
package nd.max.ui.subscreens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nd.max.R
import nd.max.core.gamespace.GameApp
import nd.max.core.gamespace.GameLibraryAccess
import nd.max.ui.component.CustomBottomSheet
import nd.max.ui.design.MaxGroup
import nd.max.ui.design.MaxGroupDivider
import nd.max.ui.design.MaxRow

/** أقصى ارتفاع للقائمة قبل أن تبدأ بالتمرير — تبقى الورقة وحقل البحث مرئيَّين. */
private val GAME_PICKER_MAX_HEIGHT = 420.dp

/** بطاقة واحدة في القائمة: ما يُعرض وما يُشغَّل. */
internal data class GamePickerItem(val packageName: String, val label: String, val isGame: Boolean)

/** ترشيح لا يغيّر الترتيب: الاسم أو الحزمة، بأي حالة أحرف. */
internal fun filterGamePicker(apps: List<GamePickerItem>, query: String): List<GamePickerItem> {
    val q = query.trim()
    if (q.isEmpty()) return apps
    return apps.filter {
        it.label.contains(q, ignoreCase = true) || it.packageName.contains(q, ignoreCase = true)
    }
}

/** ترتيب واحد: الألعاب أولًا، ثم الاسم، ثم الحزمة — فالقائمة لا تتقلّب بين فتحة وأخرى. */
internal fun orderGamePicker(apps: List<GameApp>): List<GamePickerItem> = apps
    .distinctBy { it.packageName }
    .sortedWith(
        compareByDescending<GameApp> { it.detectedGame }
            .thenBy { it.label.lowercase() }
            .thenBy { it.packageName }
    )
    .map { GamePickerItem(it.packageName, it.label, it.detectedGame) }

@Composable
internal fun GamePickerSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onPick: (GamePickerItem) -> Unit,
) {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }

    // المسح على IO مرة عند الفتح: `queryIntentActivities` يقرأ كل أنشطة المشغّل، وهي بطيئة
    // بما يكفي لتُجمّد أول إطار لو وقعت على الخيط الرئيسي.
    val apps by produceState<List<GamePickerItem>?>(initialValue = null, visible) {
        if (!visible) return@produceState
        value = withContext(Dispatchers.IO) {
            runCatching { orderGamePicker(GameLibraryAccess.apps(context)) }.getOrDefault(emptyList())
        }
    }
    val shown = remember(apps, query) { apps?.let { filterGamePicker(it, query) } }

    CustomBottomSheet(visible = visible, onDismiss = onDismiss) {
        Text(
            text = stringResource(R.string.game_sessions_pick_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = stringResource(R.string.game_sessions_pick_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = { Text(stringResource(R.string.game_sessions_pick_search)) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
        )

        val list = shown
        when {
            list == null -> Text(
                text = stringResource(R.string.game_sessions_loading),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            list.isEmpty() -> Text(
                text = stringResource(R.string.game_sessions_pick_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            else -> LazyColumn(modifier = Modifier.heightIn(max = GAME_PICKER_MAX_HEIGHT)) {
                items(list, key = { it.packageName }) { app ->
                    MaxGroup {
                        MaxRow(
                            title = app.label,
                            subtitle = if (app.isGame) {
                                app.packageName + " · " + stringResource(R.string.game_sessions_pick_game_tag)
                            } else {
                                app.packageName
                            },
                            icon = if (app.isGame) Icons.Rounded.SportsEsports else null,
                            onClick = { onPick(app) }
                        )
                    }
                    MaxGroupDivider()
                }
            }
        }
    }
}
