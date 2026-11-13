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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxSpace
import nd.max.ui.component.rememberLobbyIcon

/*
 * NEXUS — أوّل فضاءات MONOLITH: منصّة الإطلاق.
 *
 * الشاشة **بلا حالة ملكية**: تستقبل نموذجًا جاهزًا ودوالّ. كل رقم معروض جاء من
 * `NexusModel`، وما لا قيمة له يُمرَّر `null` فيُحجب — لا صفر ولا شرطة مزخرفة.
 *
 * الشيء الغنيّ الواحد هنا أيقونة اللعبة الحقيقية على بلاطة واحدة. كل ما حولها
 * هادئ: لا توهّج، لا تدرّج لوني، لا حركة مستمرّة، ولا زخرفة تنافسها.
 */

@Immutable
data class NexusGame(
    val packageName: String,
    val label: String,
    val accent: Color,
    val icon: ImageBitmap? = null,
    val lastPlayed: String? = null,
    val installedDays: String? = null,
    val monogram: String = label.take(1).uppercase()
)

@Immutable
data class NexusCopy(
    val wordmark: String,
    val nowLabel: String,
    val launch: String,
    val vault: String,
    val loadout: String,
    val career: String,
    val search: String,
    val playtimeLabel: String,
    val gamesLabel: String,
    val sessionsLabel: String,
    val streakLabel: String,
    val readyLabel: String,
    val reviewSetup: String,
    val lastPlayedLabel: String,
    val installedLabel: String,
    val emptyTitle: String,
    val emptyBody: String,
    val emptyAction: String
)

@Immutable
data class NexusModel(
    val games: List<NexusGame> = emptyList(),
    val selectedIndex: Int = 0,
    val totalPlaytime: String,
    val sessionCount: String,
    val streak: String,
    val readiness: Float = 0f,
    val readinessValue: String,
    val recentLine: String? = null
)

@Composable
fun NexusScreen(
    model: NexusModel,
    copy: NexusCopy,
    onSelect: (Int) -> Unit,
    onLaunch: () -> Unit,
    onOpenVault: () -> Unit,
    onOpenLoadout: () -> Unit,
    onOpenCareer: () -> Unit,
    onOpenSearch: () -> Unit,
    onEmptyAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selected = model.games.getOrNull(model.selectedIndex) ?: model.games.firstOrNull()
    val accent = monolithAccent(selected?.accent)
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = MaxSpace.gutter, vertical = MaxSpace.lg)
        ) {
            NexusHeader(copy = copy, gameCount = model.games.size)
            Spacer(Modifier.height(MaxSpace.xl))
            if (selected == null) {
                NexusEmpty(
                    copy = copy,
                    onAction = onEmptyAction,
                    modifier = Modifier.weight(1f)
                )
            } else {
                NexusHero(
                    game = selected,
                    copy = copy,
                    model = model,
                    accent = accent,
                    onLaunch = onLaunch,
                    onOpenSetup = onOpenLoadout,
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                )
            }
            NexusRail(
                games = model.games,
                selectedIndex = model.selectedIndex,
                onSelect = onSelect
            )
            Spacer(Modifier.height(MaxSpace.md))
            NexusFooter(
                copy = copy,
                onOpenVault = onOpenVault,
                onOpenLoadout = onOpenLoadout,
                onOpenCareer = onOpenCareer,
                onOpenSearch = onOpenSearch
            )
        }
    }
}

@Composable
private fun NexusHeader(copy: NexusCopy, gameCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MonolithNotch()
        Spacer(Modifier.width(MaxSpace.sm))
        Text(
            text = copy.wordmark,
            style = Monolith.heading,
            color = Monolith.BoneDim,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${copy.gamesLabel} · $gameCount",
            style = Monolith.label,
            color = Monolith.Muted,
            maxLines = 1
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NexusHero(
    game: NexusGame,
    copy: NexusCopy,
    model: NexusModel,
    accent: MonolithAccent,
    onLaunch: () -> Unit,
    onOpenSetup: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.lg)
        ) {
            MonolithGameSlab(
                icon = game.icon,
                label = game.label,
                accent = accent.base
            )
            Column(modifier = Modifier.weight(1f)) {
                MonolithEyebrow(text = copy.nowLabel)
                Spacer(Modifier.height(MaxSpace.xs))
                Text(
                    text = game.label,
                    style = Monolith.title,
                    color = Monolith.Bone,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() }
                )
            }
        }
        Spacer(Modifier.height(MaxSpace.lg))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm),
            verticalArrangement = Arrangement.spacedBy(MaxSpace.xs)
        ) {
            game.installedDays?.let {
                MonolithChip(text = "${copy.installedLabel} · $it", accent = accent.base, filled = false)
            }
            game.lastPlayed?.let {
                MonolithChip(text = "${copy.lastPlayedLabel} · $it", accent = accent.base, filled = true)
            }
        }
        Spacer(Modifier.height(MaxSpace.xxl))
        NexusStats(copy = copy, model = model)
        Spacer(Modifier.height(MaxSpace.xxl))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.lg)
        ) {
            val setupInteraction = remember { MutableInteractionSource() }
            val setupFocused by setupInteraction.collectIsFocusedAsState()
            val setupShape = RoundedCornerShape(MaxRadius.control)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(MaxSpace.xs),
                modifier = Modifier
                    .clip(setupShape)
                    .border(
                        MaxSpace.hairline,
                        if (setupFocused) accent.base else Color.Transparent,
                        setupShape
                    )
                    .semantics(mergeDescendants = true) {
                        contentDescription =
                            "${copy.readyLabel} ${model.readinessValue}. ${copy.reviewSetup}"
                    }
                    .clickable(
                        interactionSource = setupInteraction,
                        indication = null,
                        role = Role.Button,
                        onClick = onOpenSetup
                    )
                    .padding(horizontal = MaxSpace.xs, vertical = MaxSpace.sm)
            ) {
                MonolithReadinessRing(
                    progress = model.readiness,
                    accent = accent.base,
                    valueText = model.readinessValue,
                    label = copy.readyLabel
                )
                Text(
                    text = copy.reviewSetup,
                    style = Monolith.label,
                    color = Monolith.BoneDim,
                    maxLines = 1
                )
            }
            MonolithPrimaryButton(
                text = copy.launch,
                onClick = onLaunch,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun NexusStats(copy: NexusCopy, model: NexusModel) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val compact = maxWidth < 360.dp
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.lg)
        ) {
            NexusStat(
                value = model.totalPlaytime,
                label = copy.playtimeLabel,
                compact = compact,
                modifier = Modifier.weight(1f)
            )
            NexusStat(
                value = model.sessionCount,
                label = copy.sessionsLabel,
                compact = compact,
                modifier = Modifier.weight(1f)
            )
            NexusStat(
                value = model.streak,
                label = copy.streakLabel,
                compact = compact,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun NexusStat(
    value: String,
    label: String,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaxSpace.xs)
    ) {
        Text(
            text = value,
            style = if (compact) Monolith.heading else Monolith.numeral,
            color = Monolith.Bone,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = label,
            style = Monolith.label,
            color = Monolith.Muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun NexusRail(
    games: List<NexusGame>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    if (games.isEmpty()) return
    val shape = RoundedCornerShape(MaxRadius.control)
    val state = rememberLazyListState()
    LaunchedEffect(selectedIndex) {
        if (selectedIndex in games.indices) state.animateScrollToItem(selectedIndex)
    }
    LazyRow(
        state = state,
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm)
    ) {
        itemsIndexed(games, key = { _, game -> game.packageName }) { index, game ->
            val active = index == selectedIndex
            val accent = monolithAccent(game.accent)
            val icon = rememberLobbyIcon(game.packageName).value
            val interaction = remember { MutableInteractionSource() }
            val focused by interaction.collectIsFocusedAsState()
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(shape)
                    .background(if (active) accent.soft else Monolith.Slate)
                    .border(
                        MaxSpace.hairline,
                        when {
                            focused -> Monolith.Bone
                            active -> accent.base.copy(alpha = 0.7f)
                            else -> Monolith.Hairline
                        },
                        shape
                    )
                    .selectable(
                        interactionSource = interaction,
                        indication = null,
                        selected = active,
                        role = Role.Tab
                    ) { onSelect(index) }
                    .semantics { contentDescription = game.label },
                contentAlignment = Alignment.Center
            ) {
                if (icon != null) {
                    Image(
                        bitmap = icon,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(MaxSpace.xs)
                    )
                } else {
                    Text(
                        text = game.monogram,
                        style = Monolith.label,
                        color = if (active) Monolith.Bone else Monolith.BoneDim
                    )
                }
            }
        }
    }
}

@Composable
private fun NexusFooter(
    copy: NexusCopy,
    onOpenVault: () -> Unit,
    onOpenLoadout: () -> Unit,
    onOpenCareer: () -> Unit,
    onOpenSearch: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth < FooterCompactWidth) {
            Column(verticalArrangement = Arrangement.spacedBy(MaxSpace.sm)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm)
                ) {
                    MonolithGhostButton(
                        text = copy.vault,
                        onClick = onOpenVault,
                        modifier = Modifier.weight(1f)
                    )
                    MonolithGhostButton(
                        text = copy.loadout,
                        onClick = onOpenLoadout,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm)
                ) {
                    MonolithGhostButton(
                        text = copy.career,
                        onClick = onOpenCareer,
                        modifier = Modifier.weight(1f)
                    )
                    MonolithGhostButton(
                        text = copy.search,
                        onClick = onOpenSearch,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm)
            ) {
                MonolithGhostButton(
                    text = copy.vault,
                    onClick = onOpenVault,
                    modifier = Modifier.weight(1f)
                )
                MonolithGhostButton(
                    text = copy.loadout,
                    onClick = onOpenLoadout,
                    modifier = Modifier.weight(1f)
                )
                MonolithGhostButton(
                    text = copy.career,
                    onClick = onOpenCareer,
                    modifier = Modifier.weight(1f)
                )
                MonolithGhostButton(
                    text = copy.search,
                    onClick = onOpenSearch,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private val FooterCompactWidth = 420.dp

@Composable
private fun NexusEmpty(
    copy: NexusCopy,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        MonolithPanel(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = copy.emptyTitle,
                style = Monolith.title,
                color = Monolith.Bone,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(Modifier.height(MaxSpace.sm))
            Text(text = copy.emptyBody, style = Monolith.body, color = Monolith.Muted)
            Spacer(Modifier.height(MaxSpace.lg))
            MonolithGhostButton(text = copy.emptyAction, onClick = onAction)
        }
    }
}
