/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.ui.gamespace

import androidx.compose.runtime.Immutable

/*
 * بحث لوحة الأوامر — نموذج نقي بلا Compose ولا سياق، ليُقاس على JVM.
 *
 * القاعدة: الألعاب تسبق الإجراءات دائمًا، والاستعلام الفارغ لا يخفي الإجراءات خلف
 * عشرات الألعاب. لا تصنيف غامض ولا «نتائج مقترحة» مُصنَّعة.
 */

@Immutable
data class PaletteGame(
    val packageName: String,
    val label: String
)

@Immutable
data class PaletteAction(
    val id: PaletteActionId,
    val label: String,
    val detail: String? = null
)

enum class PaletteActionId { Launch, Vault, Loadout, Career, Sessions, Close }

sealed interface PaletteHit {
    data class Game(val game: PaletteGame) : PaletteHit
    data class Action(val action: PaletteAction) : PaletteHit
}

object PaletteSearch {
    const val LIMIT = 12
    private const val GAMES_BEFORE_ACTIONS = 6

    fun filter(
        query: String,
        games: List<PaletteGame>,
        actions: List<PaletteAction>,
        limit: Int = LIMIT
    ): List<PaletteHit> {
        val max = limit.coerceAtLeast(0)
        if (max == 0) return emptyList()
        val term = query.trim()
        if (term.isEmpty()) {
            return (
                games.take(GAMES_BEFORE_ACTIONS).map { PaletteHit.Game(it) } +
                    actions.map { PaletteHit.Action(it) }
                ).take(max)
        }
        val matchedGames = games.filter { game ->
            game.label.contains(term, ignoreCase = true) ||
                game.packageName.contains(term, ignoreCase = true)
        }
        val matchedActions = actions.filter { action ->
            action.label.contains(term, ignoreCase = true) ||
                action.detail?.contains(term, ignoreCase = true) == true
        }
        return (
            matchedGames.map { PaletteHit.Game(it) } +
                matchedActions.map { PaletteHit.Action(it) }
            ).take(max)
    }
}
