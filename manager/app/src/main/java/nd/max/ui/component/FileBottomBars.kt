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

/**
 * أشرطة الأسفل في مدير الملفات: **أدواته · تحديده · حافظته · مهامه**.
 *
 * التصميم:
 * 1. شريط أدوات مرتفع بحواف علوية منحنية وأزرار بلمسات تفاعلية واضحة.
 * 2. تمييز زر إنشاء مجلد جديد بحاوية خاصة تجعل الإجراء الأهم بارزًا.
 * 3. شريط تحديد ديناميكي يحتوي رقاقة لعدد العناصر وإجراءات التعديل والحذف السريعة.
 * 4. شريط حافظة عائم بأسلوب بطاقة أنيقة مع زر لصق سريع وإلغاء.
 * 5. أشرطة تقدم للمهام الخلفية بنسب حقيقية وأزرار إلغاء.
 */
package nd.max.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import nd.max.R
import nd.max.ui.design.MAX_VALUE_UNAVAILABLE
import nd.max.ui.design.MaxCommand
import nd.max.ui.design.MaxCommandMenu
import nd.max.ui.design.MaxProgressStrip
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxSize
import nd.max.ui.design.MaxSpace
import nd.max.ui.design.MaxTone
import nd.max.ui.util.FileTask
import nd.max.ui.util.FileTaskQueue
import nd.max.ui.design.maxRadius

/** شريط أدوات النافذة: صعود · تحديث · مجلد جديد · بحث · تحديد · درج. */
@Composable
fun FileToolBar(
    upEnabled: Boolean,
    onUp: () -> Unit,
    onRefresh: () -> Unit,
    onNewFolder: () -> Unit,
    onSearch: () -> Unit,
    onSelect: () -> Unit,
    onDrawer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = maxRadius(MaxRadius.inset), topEnd = maxRadius(MaxRadius.inset)),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp,
        border = BorderStroke(MaxSize.hairlineBorder, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaxSpace.xs, vertical = MaxSpace.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ToolSlot(Modifier.weight(1f)) {
                FileToolButton(
                    label = stringResource(R.string.max_files_tool_up),
                    icon = Icons.Rounded.ArrowUpward,
                    onClick = onUp,
                    enabled = upEnabled,
                )
            }
            ToolSlot(Modifier.weight(1f)) {
                FileToolButton(
                    label = stringResource(R.string.max_files_tool_refresh),
                    icon = Icons.Rounded.Refresh,
                    onClick = onRefresh,
                )
            }
            ToolSlot(Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(maxRadius(MaxRadius.chip)))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                ) {
                    FileToolButton(
                        label = stringResource(R.string.max_files_action_new_folder),
                        icon = Icons.Rounded.CreateNewFolder,
                        onClick = onNewFolder,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            ToolSlot(Modifier.weight(1f)) {
                FileToolButton(
                    label = stringResource(R.string.max_files_tool_search),
                    icon = Icons.Rounded.Search,
                    onClick = onSearch,
                )
            }
            ToolSlot(Modifier.weight(1f)) {
                FileToolButton(
                    label = stringResource(R.string.max_files_tool_select),
                    icon = Icons.Rounded.Checklist,
                    onClick = onSelect,
                )
            }
            ToolSlot(Modifier.weight(1f)) {
                FileToolButton(
                    label = stringResource(R.string.max_files_tool_drawer),
                    icon = Icons.Rounded.Menu,
                    onClick = onDrawer,
                )
            }
        }
    }
}

@Composable
private fun ToolSlot(modifier: Modifier, content: @Composable () -> Unit) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        content()
    }
}

/**
 * شريط التحديد: العدد · نسخ · قصّ · حذف · تسمية · وقائمة البقيّة.
 */
@Composable
fun FileSelectionBar(
    count: Int,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    menu: List<MaxCommand>,
    menuDescription: String,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = maxRadius(MaxRadius.inset), topEnd = maxRadius(MaxRadius.inset)),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 4.dp,
        border = BorderStroke(MaxSize.hairlineBorder, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaxSpace.sm, vertical = MaxSpace.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(MaxRadius.pill),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.padding(horizontal = MaxSpace.xs),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = MaxSpace.sm, vertical = MaxSpace.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaxSpace.xs),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Checklist,
                        contentDescription = null,
                        modifier = Modifier.size(MaxSize.iconGlyphSmall),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = stringResource(R.string.max_files_selected_count, count),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            TextButton(onClick = onClear) {
                Text(
                    text = stringResource(R.string.max_files_action_done),
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SelectionAction(
                    label = stringResource(R.string.max_files_action_copy),
                    icon = Icons.Rounded.ContentCopy,
                    onClick = onCopy,
                )
                SelectionAction(
                    label = stringResource(R.string.max_files_action_cut),
                    icon = Icons.Rounded.ContentCut,
                    onClick = onCut,
                )
                SelectionAction(
                    label = stringResource(R.string.max_files_action_rename),
                    icon = Icons.Rounded.DriveFileRenameOutline,
                    onClick = onRename,
                )
                SelectionAction(
                    label = stringResource(R.string.max_files_action_delete),
                    icon = Icons.Rounded.Delete,
                    onClick = onDelete,
                    destructive = true,
                )
                MaxCommandMenu(
                    commands = menu,
                    contentDescription = menuDescription,
                    modifier = Modifier.size(NarrowButton),
                )
            }
        }
    }
}

@Composable
private fun SelectionAction(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    destructive: Boolean = false,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FileToolButton(
            label = label,
            icon = icon,
            onClick = onClick,
            tint = if (destructive) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

/**
 * شريط الحافظة: ما نُسخ · من أين · وزر لصق فوري في المجلد الحالي.
 */
@Composable
fun FileClipboardBar(
    count: Int,
    origin: String,
    onPaste: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MaxSpace.sm, vertical = MaxSpace.xs),
        shape = RoundedCornerShape(maxRadius(MaxRadius.control)),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        border = BorderStroke(MaxSize.hairlineBorder, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)),
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaxSpace.md, vertical = MaxSpace.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm),
        ) {
            Icon(
                imageVector = Icons.Rounded.ContentPaste,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(MaxSize.iconGlyph),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.max_files_clipboard_summary, count),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = origin,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Button(
                onClick = onPaste,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                shape = RoundedCornerShape(maxRadius(MaxRadius.chip)),
            ) {
                Text(
                    text = stringResource(R.string.max_files_action_paste),
                    fontWeight = FontWeight.SemiBold,
                )
            }
            IconButton(onClick = onClear) {
                Icon(
                    imageVector = Icons.Rounded.ClearAll,
                    contentDescription = stringResource(R.string.max_files_clipboard_clear),
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }
    }
}

/**
 * شريط المهام الخلفية: حتى ثلاث مهمات مع نسبة حقيقية وإلغاء.
 */
@Composable
fun FileTaskStrip(
    tasks: List<FileTask>,
    labelFor: @Composable (FileTask) -> String,
    detailFor: (FileTask) -> String?,
    onCancel: (Long) -> Unit,
    onClearFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visible = FileTaskQueue.visible(tasks)
    if (visible.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = MaxSpace.md, vertical = MaxSpace.sm),
        verticalArrangement = Arrangement.spacedBy(MaxSpace.sm),
    ) {
        visible.forEach { task ->
            MaxProgressStrip(
                title = labelFor(task),
                percent = task.percent,
                detail = detailFor(task),
                tone = when {
                    task.isRunning -> MaxTone.Accent
                    task.state == nd.max.ui.util.FileTaskState.Done -> MaxTone.Positive
                    task.state == nd.max.ui.util.FileTaskState.Failed -> MaxTone.Critical
                    else -> MaxTone.Neutral
                },
                cancelLabel = stringResource(R.string.max_files_task_cancel).takeIf { task.isRunning },
                onCancel = { onCancel(task.id) }.takeIf { task.isRunning },
            )
        }
        if (visible.any { !it.isRunning }) {
            TextButton(onClick = onClearFinished) {
                Text(text = stringResource(R.string.max_files_task_clear_finished))
            }
        }
    }
}

/** نسبة مقروءة أو شرطة — تُستدعى من الشاشة فلا تُخترع نسبة في الواجهة. */
fun taskPercentText(percent: Int?): String = percent?.let { "$it%" } ?: MAX_VALUE_UNAVAILABLE

private val NarrowButton = 40.dp
