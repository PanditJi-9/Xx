/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.TileService

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import nd.max.MainActivity
import nd.max.R
import nd.max.widget.core.WidgetIntents

/**
 * مدخل «لوبي الألعاب» من الإعدادات السريعة — **باب لا مفتاح**.
 *
 * لا يغيّر حالة ولا يختار لعبة ولا يكتب عتادًا: يفتح الوجهة القائمة عبر النيّة نفسها التي
 * يستعملها الاختصار المثبَّت (`WidgetIntents.ROUTE_GAME_LOBBY`)، فتظلّ الشاشة مصدر الحالة
 * الوحيد. وحالته `INACTIVE` لأنها ليست مفتاحًا ثنائيًّا؛ والضغط عليها يفتح ولا يقلب شيئًا.
 */
class GameLobbyTileService : TileService() {

    override fun onTileAdded() {
        super.onTileAdded()
        render()
    }

    override fun onStartListening() {
        super.onStartListening()
        render()
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, MainActivity::class.java)
            .setAction(ACTION_OPEN_LOBBY)
            .putExtra(WidgetIntents.EXTRA_OPEN_ROUTE, WidgetIntents.ROUTE_GAME_LOBBY)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startActivityAndCollapse(
                    PendingIntent.getActivity(
                        this,
                        0,
                        intent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
        }.onFailure {
            val tile = qsTile ?: return@onFailure
            tile.subtitle = getString(R.string.tile_game_lobby_failed)
            tile.updateTile()
        }
    }

    private fun render() {
        val tile = qsTile ?: return
        // ليست مفتاحًا: لا حالة تشغيل تُدّعى هنا.
        tile.state = Tile.STATE_INACTIVE
        tile.label = getString(R.string.game_lobby_title)
        tile.subtitle = getString(R.string.tile_game_lobby_open)
        tile.updateTile()
    }

    private companion object {
        // فعل مميّز حتى لا يستبدل PendingIntent بابًا آخر إلى MainActivity.
        const val ACTION_OPEN_LOBBY = "nd.max.action.OPEN_GAME_LOBBY"
    }
}
