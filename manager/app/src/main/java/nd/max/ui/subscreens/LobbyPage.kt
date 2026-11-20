/* Copyright (C) 2026 Nader Magdy. All rights reserved. */
package nd.max.ui.subscreens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.ViewSidebar
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
    Home(null, R.string.gs_nav_home, Icons.Rounded.Home),
    Library(LobbySection.Library, R.string.gs_nav_games, Icons.Rounded.SportsEsports),
    Performance(LobbySection.Insights, R.string.gs_nav_perf, Icons.Rounded.Speed),
    Tools(LobbySection.Panel, R.string.gs_nav_tools, Icons.Rounded.ViewSidebar),
    Features(LobbySection.Loadout, R.string.gs_nav_features, Icons.Rounded.StarBorder),
    Profile(LobbySection.Game, R.string.lobby_tile_profile, Icons.Rounded.Person),
    Settings(LobbySection.Lobby, R.string.gs_nav_settings, Icons.Rounded.Settings);

    companion object {
        /** صفحة القسم؛ كل قسم له صفحة واحدة بالضبط. */
        fun of(section: LobbySection): LobbyPage = entries.first { it.section == section }

        /** الصفحة بترتيبها في المُصفِّح، مع تثبيت الفهرس داخل المدى. */
        fun at(index: Int): LobbyPage = entries[index.coerceIn(0, entries.lastIndex)]
    }
}
