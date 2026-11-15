/* Copyright (C) 2026 Nader Magdy. All rights reserved. */
package nd.max.ui.subscreens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.ViewSidebar
import androidx.compose.ui.graphics.vector.ImageVector
import nd.max.R

/**
 * صفحات اللوبي بترتيب الشريط الجانبيّ: الرئيسية أولًا ثم الأقسام، وكلّها صفحات في تمرير رأسيّ واحد.
 * السحب الرأسيّ ولمس الشريط يصلان إلى الصفحة نفسها، فلا مسار ثانٍ ولا شريط ثانٍ للأقسام
 * (طلب المالك ٢٠٢٦-١١-١٥). أجسام الأقسام هي `LobbySectionContent` نفسها بلا نسخ.
 */
internal enum class LobbyPage(
    val section: LobbySection?,
    val labelRes: Int,
    val icon: ImageVector,
) {
    Home(null, R.string.gs_nav_home, Icons.Outlined.Home),
    Library(LobbySection.Library, R.string.gs_nav_games, Icons.Outlined.SportsEsports),
    Performance(LobbySection.Insights, R.string.gs_nav_perf, Icons.Outlined.Speed),
    Tools(LobbySection.Panel, R.string.gs_nav_tools, Icons.Outlined.ViewSidebar),
    Features(LobbySection.Loadout, R.string.gs_nav_features, Icons.Outlined.StarBorder),
    Profile(LobbySection.Game, R.string.lobby_tile_profile, Icons.Outlined.Person),
    Settings(LobbySection.Lobby, R.string.gs_nav_settings, Icons.Outlined.Settings);

    companion object {
        /** صفحة القسم؛ كل قسم له صفحة واحدة بالضبط. */
        fun of(section: LobbySection): LobbyPage = entries.first { it.section == section }

        /** الصفحة بترتيبها في المُصفِّح، مع تثبيت الفهرس داخل المدى. */
        fun at(index: Int): LobbyPage = entries[index.coerceIn(0, entries.lastIndex)]
    }
}
