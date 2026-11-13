/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.ui.gamespace

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxSpace

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
    val playtime: String? = null,
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
    val playtimeLabel: String,
    val sessionsLabel: String,
    val streakLabel: String,
    val readyLabel: String,
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
    modifier: Modifier = Modifier
) {
    val selected = model.games.getOrNull(model.selectedIndex) ?: model.games.firstOrNull()
    val accent = monolithAccent(selected?.accent)
    Box(modifier = modifier.fillMaxSize().background(Monolith.Obsidian)) {
        MonolithField(modifier = Modifier.fillMaxSize())
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = MaxSpace.gutter, vertical = MaxSpace.lg)
        ) {
            NexusHeader(copy = copy, totalPlaytime = model.totalPlaytime)
            Spacer(Modifier.height(MaxSpace.xl))
            if (selected == null) {
                NexusEmpty(
                    copy = copy,
                    onOpenVault = onOpenVault,
                    modifier = Modifier.weight(1f)
                )
            } else {
                NexusHero(
                    game = selected,
                    copy = copy,
                    model = model,
                    accent = accent,
                    onLaunch = onLaunch,
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
                onOpenCareer = onOpenCareer
            )
        }
    }
}

@Composable
private fun NexusHeader(copy: NexusCopy, totalPlaytime: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MonolithNotch()
        Spacer(Modifier.width(MaxSpace.sm))
        Text(
            text = copy.wordmark,
            style = Monolith.eyebrow,
            color = Monolith.BoneDim,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() }
        )
        Text(
            text = totalPlaytime,
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
            game.playtime?.let { MonolithChip(text = it, accent = accent.base, filled = true) }
            game.installedDays?.let {
                MonolithChip(text = "${copy.installedLabel} · $it", accent = accent.base, filled = false)
            }
            game.lastPlayed?.let {
                MonolithChip(text = "${copy.lastPlayedLabel} · $it", accent = accent.base, filled = false)
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
            MonolithReadinessRing(
                progress = model.readiness,
                accent = accent.base,
                valueText = model.readinessValue,
                label = copy.readyLabel
            )
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.lg)
    ) {
        NexusStat(
            value = model.totalPlaytime,
            label = copy.playtimeLabel,
            modifier = Modifier.weight(1f)
        )
        NexusStat(
            value = model.sessionCount,
            label = copy.sessionsLabel,
            modifier = Modifier.weight(1f)
        )
        NexusStat(
            value = model.streak,
            label = copy.streakLabel,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun NexusStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaxSpace.xs)
    ) {
        Text(
            text = value,
            style = Monolith.numeral,
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
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm)
    ) {
        itemsIndexed(games, key = { _, game -> game.packageName }) { index, game ->
            val active = index == selectedIndex
            val accent = monolithAccent(game.accent)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(shape)
                    .background(if (active) accent.soft else Monolith.Slate)
                    .border(
                        MaxSpace.hairline,
                        if (active) accent.base.copy(alpha = 0.7f) else Monolith.Hairline,
                        shape
                    )
                    .selectable(selected = active, role = Role.Tab) { onSelect(index) }
                    .semantics { contentDescription = game.label },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = game.monogram,
                    style = Monolith.label,
                    color = if (active) Monolith.Bone else Monolith.BoneDim
                )
            }
        }
    }
}

@Composable
private fun NexusFooter(
    copy: NexusCopy,
    onOpenVault: () -> Unit,
    onOpenLoadout: () -> Unit,
    onOpenCareer: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm)
    ) {
        MonolithGhostButton(text = copy.vault, onClick = onOpenVault, modifier = Modifier.weight(1f))
        MonolithGhostButton(text = copy.loadout, onClick = onOpenLoadout, modifier = Modifier.weight(1f))
        MonolithGhostButton(text = copy.career, onClick = onOpenCareer, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun NexusEmpty(
    copy: NexusCopy,
    onOpenVault: () -> Unit,
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
            MonolithGhostButton(text = copy.emptyAction, onClick = onOpenVault)
        }
    }
}
