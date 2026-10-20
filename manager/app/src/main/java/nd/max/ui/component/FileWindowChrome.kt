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
 * كروم مدير الملفات: **شريط علوي واحد** يتبع النافذة النشطة، وقرص النافذتين وعدّادها بداخله.
 *
 * كان لكل شقّ شريطان: تبويبات فوق الشاشة كلها، ثم مسار وسطر حالة تحت كل شقّ. فصار شريط واحد
 * كما في MT Manager، والقواعد التي تحكمه:
 *
 * 1. **شريط واحد لا شريطان**: مسار النافذة النشطة وأوامرها في سطر، واسمها وعدّادها في سطر تحته.
 * 2. **النافذة النشطة معلنة**: مفتاح «١ | ٢» يُظهرها ويبدّل بينهما، والشقّ النشط يحمل خطًّا علويًّا.
 * 3. **النص لا يُكسر عمودًا**: المسار فتات خبز يتمرّر أفقيًّا داخل مساحته، وسطر الحالة يتمرّر
 *    كذلك، فلا تلتفّ الكلمة حرفًا حرفًا حين تضيق المساحة.
 */
package nd.max.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import nd.max.R
import nd.max.ui.design.MaxCommand
import nd.max.ui.design.MaxCommandMenu
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxSize
import nd.max.ui.design.MaxSpace
import nd.max.ui.util.DiskSpace
import nd.max.ui.util.EntryCounts
import nd.max.ui.util.FileBrowser
import nd.max.ui.util.FileFormat
import nd.max.ui.util.FileWindowsState
import nd.max.ui.util.WindowSide
import nd.max.ui.design.maxRadius

/**
 * الشريط العلوي الموحّد: مسار النافذة النشطة وأوامرها، ثم اسمها وعدّادها وقرصها.
 */
@Composable
fun FileWindowTopBar(
    windows: FileWindowsState,
    counts: EntryCounts?,
    disk: DiskSpace?,
    loading: Boolean,
    onSelectWindow: (WindowSide) -> Unit,
    onBack: () -> Unit,
    onEditPath: () -> Unit,
    onSearch: () -> Unit,
    onNavigateSegment: (String) -> Unit,
    menu: List<MaxCommand>,
    menuDescription: String,
    modifier: Modifier = Modifier,
) {
    val current = windows.activeWindow

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaxSpace.sm, vertical = MaxSpace.xs),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onBack,
                    enabled = current.canGoBack,
                    modifier = Modifier.size(MaxSize.rowIconContainer),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.max_files_back_cd),
                        tint = if (current.canGoBack) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        },
                        modifier = Modifier.size(MaxSize.iconGlyph),
                    )
                }

                TopBarCrumbs(
                    path = current.path,
                    onEditPath = onEditPath,
                    onNavigateSegment = onNavigateSegment,
                    modifier = Modifier.weight(1f),
                )

                IconButton(
                    onClick = onEditPath,
                    modifier = Modifier.size(MaxSize.rowIconContainer),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = stringResource(R.string.max_files_edit_path_title),
                        modifier = Modifier.size(MaxSize.iconGlyphSmall),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                IconButton(
                    onClick = onSearch,
                    modifier = Modifier.size(MaxSize.rowIconContainer),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = stringResource(R.string.max_files_search_open_cd),
                        modifier = Modifier.size(MaxSize.iconGlyphSmall),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                WindowSwitch(
                    windows = windows,
                    onSelect = onSelectWindow,
                )

                MaxCommandMenu(
                    commands = menu,
                    contentDescription = menuDescription,
                    modifier = Modifier.size(MaxSize.rowIconContainer),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = MaxSpace.hairline),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TopBarCounts(
                    label = current.label,
                    counts = counts,
                    loading = loading,
                    modifier = Modifier.weight(1f),
                )
                TopBarDisk(disk = disk)
            }
        }
    }
}

/** فتات المسار: كل مستوى زرّ، والأخير يفتح تعديل المسار. تتمرّر أفقيًّا ولا تلتفّ. */
@Composable
private fun TopBarCrumbs(
    path: String,
    onEditPath: () -> Unit,
    onNavigateSegment: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val crumbs = remember(path) { FileBrowser.breadcrumbs(path) }
    val scrollState = rememberScrollState()

    LaunchedEffect(path) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Row(
        modifier = modifier
            .horizontalScroll(scrollState)
            .padding(horizontal = MaxSpace.hairline),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.hairline),
    ) {
        crumbs.forEachIndexed { index, crumb ->
            val isLast = index == crumbs.lastIndex
            Surface(
                onClick = {
                    if (isLast) {
                        onEditPath()
                    } else {
                        onNavigateSegment(crumb.path)
                    }
                },
                shape = RoundedCornerShape(maxRadius(MaxRadius.chip)),
                color = if (isLast) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                } else {
                    Color.Transparent
                },
                contentColor = if (isLast) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = MaxSpace.sm, vertical = MaxSpace.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaxSpace.xs),
                ) {
                    if (index == 0) {
                        Icon(
                            imageVector = Icons.Rounded.Storage,
                            contentDescription = null,
                            modifier = Modifier.size(MaxSize.iconGlyphSmall),
                        )
                    }
                    Text(
                        text = if (index == 0 && crumb.label == "/") "/" else crumb.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isLast) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
            if (!isLast) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(MaxSize.iconGlyphSmall),
                    tint = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
    }
}

/**
 * مفتاح النافذتين: رقمان صغيران، والنشطة منهما ملوّنة. كل زرّ يحمل اسم نافذته للقارئ الصوتي.
 */
@Composable
private fun WindowSwitch(
    windows: FileWindowsState,
    onSelect: (WindowSide) -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(horizontal = MaxSpace.hairline)
            .clip(RoundedCornerShape(maxRadius(MaxRadius.chip)))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(MaxSpace.hairline),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.hairline),
    ) {
        WindowSide.entries.forEachIndexed { index, side ->
            val isSelected = windows.active == side
            Surface(
                onClick = { onSelect(side) },
                modifier = Modifier
                    .size(width = MaxSize.iconContainer, height = MaxSize.rowIconContainer)
                    .semantics {
                        contentDescription = windows.of(side).label
                        selected = isSelected
                    },
                shape = RoundedCornerShape(maxRadius(MaxRadius.chip)),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    Color.Transparent
                },
                contentColor = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = (index + 1).toString(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    )
                }
            }
        }
    }
}

/** اسم النافذة ثم عدّاداها في سطر واحد، يتمرّر أفقيًّا إن ضاق عليها المكان. */
@Composable
private fun TopBarCounts(
    label: String,
    counts: EntryCounts?,
    loading: Boolean,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier.horizontalScroll(scrollState),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.xs),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            softWrap = false,
        )
        if (counts != null) {
            StatusChip(label = stringResource(R.string.max_files_status_folders, counts.folders))
            StatusChip(label = stringResource(R.string.max_files_status_files, counts.files))
            if (counts.hidden > 0) {
                StatusChip(
                    label = stringResource(R.string.max_files_status_hidden, counts.hidden),
                    tint = MaterialTheme.colorScheme.tertiary,
                )
            }
        } else {
            Text(
                text = if (loading) {
                    stringResource(R.string.max_files_cond_loading_title)
                } else {
                    stringResource(R.string.max_files_unknown)
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (loading) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

/** قرص النافذة: نص «المستعمل من الكلّي» في سطر واحد، ومقياس صغير بجانبه. */
@Composable
private fun TopBarDisk(disk: DiskSpace?) {
    if (disk == null) {
        Text(
            text = stringResource(R.string.max_files_status_disk_unread),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    } else {
        val ratio = if (disk.totalBytes > 0) {
            disk.usedBytes.toFloat() / disk.totalBytes.toFloat()
        } else {
            0f
        }
        val usedText = FileFormat.size(disk.usedBytes) ?: stringResource(R.string.max_files_unknown)
        val totalText = FileFormat.size(disk.totalBytes) ?: stringResource(R.string.max_files_unknown)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.xs),
        ) {
            Text(
                text = stringResource(R.string.max_files_status_disk, usedText, totalText),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
            Box(
                modifier = Modifier
                    .width(MaxSize.rowIconContainer)
                    .height(MaxSpace.xs)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(MaxSpace.hairline),
                    ),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(ratio.coerceIn(0f, 1f))
                        .height(MaxSpace.xs)
                        .background(
                            color = if (ratio > 0.9f) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            shape = RoundedCornerShape(MaxSpace.hairline),
                        ),
                )
            }
        }
    }
}

/** رقاقة حالة: نص واحد في سطح مرتفع خفيف. */
@Composable
private fun StatusChip(
    label: String,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Surface(
        shape = RoundedCornerShape(MaxSpace.xs),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = MaxSpace.xs, vertical = MaxSpace.hairline),
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            maxLines = 1,
            softWrap = false,
        )
    }
}

/** صفّ أفقي من أزرار بأسماء: كل زرّ أيقونة ومعها اسمه. */
@Composable
fun FileToolButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(maxRadius(MaxRadius.chip)))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = MaxSpace.sm, horizontal = MaxSpace.hairline),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaxSpace.xs),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) tint else tint.copy(alpha = 0.38f),
            modifier = Modifier.height(MaxSize.iconGlyph),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled) tint else tint.copy(alpha = 0.38f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
