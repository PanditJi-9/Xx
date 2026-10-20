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
 * قائمة المدخلات في مدير الملفات — تصميم احترافي غني مستوحى من MT Manager.
 *
 * المزايا:
 * 1. شارات نوع مميزة وملونة بحسب نوع الملف (APK, صور, فيديو, صوت, أرشيف, مستندات, برمجيات).
 * 2. اسم الملف بارز بوضوح، مع سطر فرعي يجمع التاريخ وصلاحيات الملف بدقة.
 * 3. عمود حجم الملف بخط أحادي المسافة (Monospace) للمحاذاة الرأسية الدقيقة.
 * 4. تمييز بصري فائق للصفوف المحددة بحواف دائرية وبطاقات ملونة واضحة.
 * 5. دعم كامل للسحب الأفقي للتحديد السريع والنقر المطول لفتح قائمة الأوامر.
 */
package nd.max.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import nd.max.ui.design.MAX_VALUE_UNAVAILABLE
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxSize
import nd.max.ui.design.MaxSpace
import nd.max.ui.util.FileEntry
import nd.max.ui.util.FileFormat
import nd.max.ui.util.FileKind
import nd.max.ui.util.FileSearchFilters
import nd.max.ui.util.FileSelection
import nd.max.ui.design.maxRadius

/**
 * قائمة القراءة الوحيدة في الشاشة.
 */
@Composable
fun FileEntryList(
    entries: List<FileEntry>,
    selection: FileSelection,
    selecting: Boolean,
    onOpen: (FileEntry) -> Unit,
    onToggleSelection: (FileEntry) -> Unit,
    onLongPress: (FileEntry, Offset) -> Unit,
    onSwipeSelect: (FileEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rowOrigins = remember { mutableStateMapOf<String, Offset>() }

    LazyColumn(modifier = modifier.fillMaxWidth()) {
        items(items = entries, key = { entry -> entry.path }) { entry ->
            FileEntryRow(
                entry = entry,
                selected = entry.path in selection.paths,
                selecting = selecting,
                onOpen = { onOpen(entry) },
                onToggleSelection = { onToggleSelection(entry) },
                onLongPress = { local ->
                    onLongPress(entry, (rowOrigins[entry.path] ?: Offset.Zero) + local)
                },
                onSwipeSelect = { onSwipeSelect(entry) },
                onPositioned = { origin -> rowOrigins[entry.path] = origin },
            )
        }
    }
}

@Composable
private fun FileEntryRow(
    entry: FileEntry,
    selected: Boolean,
    selecting: Boolean,
    onOpen: () -> Unit,
    onToggleSelection: () -> Unit,
    onLongPress: (Offset) -> Unit,
    onSwipeSelect: () -> Unit,
    onPositioned: (Offset) -> Unit,
) {
    val background = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        Color.Transparent
    }
    val threshold = with(LocalDensity.current) { SwipeSelectThreshold.toPx() }
    val badge = entryBadgeStyle(entry)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MaxSpace.sm, vertical = MaxSpace.hairline)
            .onGloballyPositioned { coordinates -> onPositioned(coordinates.positionInRoot()) }
            .then(
                if (selected) {
                    Modifier
                        .background(background, RoundedCornerShape(maxRadius(MaxRadius.chip)))
                        .border(
                            BorderStroke(MaxSize.hairlineBorder, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                            RoundedCornerShape(maxRadius(MaxRadius.chip)),
                        )
                } else {
                    Modifier.background(background)
                }
            )
            .pointerInput(entry.path, selecting) {
                detectTapGestures(
                    onTap = { if (selecting) onToggleSelection() else onOpen() },
                    onLongPress = { offset -> onLongPress(offset) },
                )
            }
            .pointerInput(entry.path, selecting) {
                var travelled = 0f
                detectHorizontalDragGestures(
                    onDragStart = { travelled = 0f },
                    onDragCancel = { travelled = 0f },
                    onDragEnd = {
                        if (abs(travelled) >= threshold) onToggleSelection()
                        travelled = 0f
                    },
                ) { _, delta -> travelled += delta }
            }
            .heightIn(min = ROW_MIN_HEIGHT)
            .padding(horizontal = MaxSpace.sm, vertical = RowVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm),
    ) {
        if (selecting) {
            Checkbox(
                checked = selected,
                onCheckedChange = null,
                modifier = Modifier.size(CheckboxSize),
            )
        }

        Box(
            modifier = Modifier
                .size(MaxSize.rowIconContainer)
                .background(badge.containerColor, RoundedCornerShape(maxRadius(MaxRadius.chip))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = badge.icon,
                contentDescription = null,
                tint = badge.iconTint,
                modifier = Modifier.size(MaxSize.iconGlyph),
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (entry.isDirectory) FontWeight.SemiBold else FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaxSpace.xs),
            ) {
                Text(
                    text = FileFormat.date(entry.modifiedEpochSec) ?: MAX_VALUE_UNAVAILABLE,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "·",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                )
                Text(
                    text = if (entry.isDirectory) "—" else FileFormat.size(entry.sizeBytes) ?: MAX_VALUE_UNAVAILABLE,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    maxLines = 1,
                )
                entry.permissions?.let { perms ->
                    Text(
                        text = "·",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    )
                    Text(
                        text = perms.octal,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        maxLines = 1,
                    )
                }
            }
        }

    }
}

private data class EntryBadgeStyle(
    val icon: ImageVector,
    val iconTint: Color,
    val containerColor: Color,
)

@Composable
private fun entryBadgeStyle(entry: FileEntry): EntryBadgeStyle {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return when {
        entry.isDirectory -> EntryBadgeStyle(
            icon = Icons.Rounded.Folder,
            iconTint = if (isDark) Color(0xFFFFCA28) else Color(0xFFF57F17),
            containerColor = if (isDark) Color(0xFFFFCA28).copy(alpha = 0.16f) else Color(0xFFFFF8E1),
        )
        entry.isSymlink -> EntryBadgeStyle(
            icon = Icons.Rounded.Link,
            iconTint = if (isDark) Color(0xFF4DD0E1) else Color(0xFF0097A7),
            containerColor = if (isDark) Color(0xFF4DD0E1).copy(alpha = 0.16f) else Color(0xFFE0F7FA),
        )
        entry.kind == FileKind.BlockDevice || entry.kind == FileKind.CharDevice -> EntryBadgeStyle(
            icon = Icons.Rounded.Memory,
            iconTint = if (isDark) Color(0xFFB39DDB) else Color(0xFF512DA8),
            containerColor = if (isDark) Color(0xFFB39DDB).copy(alpha = 0.16f) else Color(0xFFEDE7F6),
        )
        else -> when (FileSearchFilters.classify(entry.name, isDirectory = false)) {
            FileSearchFilters.Kind.APK -> EntryBadgeStyle(
                icon = Icons.Rounded.Android,
                iconTint = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32),
                containerColor = if (isDark) Color(0xFF81C784).copy(alpha = 0.16f) else Color(0xFFE8F5E9),
            )
            FileSearchFilters.Kind.ARCHIVE -> EntryBadgeStyle(
                icon = Icons.Rounded.FolderZip,
                iconTint = if (isDark) Color(0xFFFFB74D) else Color(0xFFE65100),
                containerColor = if (isDark) Color(0xFFFFB74D).copy(alpha = 0.16f) else Color(0xFFFFF3E0),
            )
            FileSearchFilters.Kind.IMAGE -> EntryBadgeStyle(
                icon = Icons.Rounded.Image,
                iconTint = if (isDark) Color(0xFFBA68C8) else Color(0xFF7B1FA2),
                containerColor = if (isDark) Color(0xFFBA68C8).copy(alpha = 0.16f) else Color(0xFFF3E5F5),
            )
            FileSearchFilters.Kind.VIDEO -> EntryBadgeStyle(
                icon = Icons.Rounded.Movie,
                iconTint = if (isDark) Color(0xFFE57373) else Color(0xFFC62828),
                containerColor = if (isDark) Color(0xFFE57373).copy(alpha = 0.16f) else Color(0xFFFFEBEE),
            )
            FileSearchFilters.Kind.AUDIO -> EntryBadgeStyle(
                icon = Icons.Rounded.MusicNote,
                iconTint = if (isDark) Color(0xFF4DB6AC) else Color(0xFF00796B),
                containerColor = if (isDark) Color(0xFF4DB6AC).copy(alpha = 0.16f) else Color(0xFFE0F2F1),
            )
            FileSearchFilters.Kind.DOCUMENT -> EntryBadgeStyle(
                icon = Icons.Rounded.Description,
                iconTint = if (isDark) Color(0xFF64B5F6) else Color(0xFF1565C0),
                containerColor = if (isDark) Color(0xFF64B5F6).copy(alpha = 0.16f) else Color(0xFFE3F2FD),
            )
            else -> EntryBadgeStyle(
                icon = Icons.Rounded.InsertDriveFile,
                iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            )
        }
    }
}

private val ROW_MIN_HEIGHT = 56.dp
private val RowVerticalPadding = MaxSpace.xs
private val CheckboxSize = 20.dp
private val SizeColumnWidth = 72.dp
private val SwipeSelectThreshold = 48.dp
