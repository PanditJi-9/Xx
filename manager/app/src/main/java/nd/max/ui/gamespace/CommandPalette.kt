/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.ui.gamespace

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import nd.max.ui.component.rememberLobbyIcon
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxSize
import nd.max.ui.design.MaxSpace

/*
 * لوحة الأوامر — بحث واحد يجد لعبة أو إجراءً، بلا مغادرة الفضاء.
 *
 * لا خلفية متوهّجة ولا أيقونات ملوّنة: سطح واحد، حقل واحد، وقائمة تُقرأ.
 * الإجراءات كلها أبواب قائمة فعلًا؛ ما لا باب له لا يظهر هنا.
 */

@Immutable
data class PaletteCopy(
    val search: String,
    val hint: String,
    val empty: String
)

@Composable
fun CommandPalette(
    visible: Boolean,
    games: List<PaletteGame>,
    actions: List<PaletteAction>,
    copy: PaletteCopy,
    onDismiss: () -> Unit,
    onGame: (String) -> Unit,
    onAction: (PaletteActionId) -> Unit
) {
    if (!visible) return
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CommandPaletteBody(
            games = games,
            actions = actions,
            copy = copy,
            onDismiss = onDismiss,
            onGame = onGame,
            onAction = onAction
        )
    }
}

@Composable
private fun CommandPaletteBody(
    games: List<PaletteGame>,
    actions: List<PaletteAction>,
    copy: PaletteCopy,
    onDismiss: () -> Unit,
    onGame: (String) -> Unit,
    onAction: (PaletteActionId) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val hits = remember(query, games, actions) { PaletteSearch.filter(query, games, actions) }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    val shape = RoundedCornerShape(MaxRadius.group)
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Monolith.Obsidian.copy(alpha = 0.94f))
                .pointerInput(Unit) { detectTapGestures { onDismiss() } }
        )
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.82f)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = MaxSpace.gutter, vertical = MaxSpace.xxl)
                .clip(shape)
                .background(Monolith.Slate)
                .border(MaxSpace.hairline, Monolith.HairlineStrong, shape)
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                        onDismiss()
                        true
                    } else {
                        false
                    }
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaxSpace.lg, vertical = MaxSpace.lg),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm)
            ) {
                MonolithNotch()
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            text = copy.search,
                            style = Monolith.body,
                            color = Monolith.Faint,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = Monolith.body.copy(color = Monolith.Bone),
                        cursorBrush = SolidColor(Monolith.Ember),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focus)
                            .onPreviewKeyEvent { event ->
                                if (event.type != KeyEventType.KeyDown) {
                                    false
                                } else {
                                    when (event.key) {
                                        Key.Escape -> {
                                            onDismiss()
                                            true
                                        }
                                        Key.Enter, Key.NumPadEnter -> {
                                            hits.firstOrNull()?.let { hit ->
                                                when (hit) {
                                                    is PaletteHit.Game -> onGame(hit.game.packageName)
                                                    is PaletteHit.Action -> onAction(hit.action.id)
                                                }
                                            }
                                            true
                                        }
                                        else -> false
                                    }
                                }
                            }
                    )
                }
                Text(
                    text = copy.hint,
                    style = Monolith.label,
                    color = Monolith.Faint,
                    maxLines = 1
                )
            }
            MonolithDivider()
            if (hits.isEmpty()) {
                Text(
                    text = copy.empty,
                    style = Monolith.body,
                    color = Monolith.Muted,
                    modifier = Modifier.padding(MaxSpace.xl)
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    items(
                        items = hits,
                        key = { hit ->
                            when (hit) {
                                is PaletteHit.Game -> "g:${hit.game.packageName}"
                                is PaletteHit.Action -> "a:${hit.action.id.name}"
                            }
                        }
                    ) { hit ->
                        when (hit) {
                            is PaletteHit.Game -> PaletteGameRow(
                                game = hit.game,
                                onClick = { onGame(hit.game.packageName) }
                            )
                            is PaletteHit.Action -> PaletteActionRow(
                                action = hit.action,
                                onClick = { onAction(hit.action.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaletteGameRow(game: PaletteGame, onClick: () -> Unit) {
    val icon = rememberLobbyIcon(game.packageName).value
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(MaxRadius.chip)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MaxSize.minTouchTarget)
            .clip(shape)
            .background(if (focused) Monolith.GlassRaised else Color.Transparent)
            .border(
                MaxSpace.hairline,
                if (focused) Monolith.BoneDim else Color.Transparent,
                shape
            )
            .semantics(mergeDescendants = true) { }
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = MaxSpace.lg, vertical = MaxSpace.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.md)
    ) {
        Box(
            modifier = Modifier
                .size(MaxSize.rowIconContainer)
                .clip(RoundedCornerShape(MaxRadius.chip))
                .background(Monolith.Basalt),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                Image(
                    bitmap = icon,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(MaxSpace.xs)
                )
            } else {
                Text(
                    text = game.label.take(1).uppercase(),
                    style = Monolith.label,
                    color = Monolith.BoneDim
                )
            }
        }
        Text(
            text = game.label,
            style = Monolith.body,
            color = Monolith.Bone,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PaletteActionRow(action: PaletteAction, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(MaxRadius.chip)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MaxSize.minTouchTarget)
            .clip(shape)
            .background(if (focused) Monolith.GlassRaised else Color.Transparent)
            .border(
                MaxSpace.hairline,
                if (focused) Monolith.BoneDim else Color.Transparent,
                shape
            )
            .semantics(mergeDescendants = true) { }
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = MaxSpace.lg, vertical = MaxSpace.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.md)
    ) {
        MonolithNotch()
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = action.label,
                style = Monolith.body,
                color = Monolith.Bone,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            action.detail?.let { detail ->
                Text(
                    text = detail,
                    style = Monolith.label,
                    color = Monolith.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
