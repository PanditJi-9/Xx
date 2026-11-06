/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
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
 * Opens the existing freeze manager; it never chooses or freezes a package.
 *
 * A tile has no selected package, so treating a tap as a freeze toggle would
 * invent a target. Selection, privilege checks and the recorded risk consent
 * remain in the existing screen. This is an entrance, not a global on/off state.
 */
class FreezeManagerTileService : TileService() {
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
        if (isLocked) {
            unlockAndRun { openManager() }
        } else {
            openManager()
        }
    }

    private fun openManager() {
        val intent = Intent(this, MainActivity::class.java)
            .setAction(ACTION_OPEN_FREEZER)
            .putExtra(WidgetIntents.EXTRA_OPEN_ROUTE, WidgetIntents.ROUTE_APP_FREEZER)
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
            tile.subtitle = getString(R.string.tile_freeze_open_failed)
            tile.updateTile()
        }
    }

    private fun render() {
        val tile = qsTile ?: return
        // No global frozen state exists; never report a package as frozen here.
        tile.state = Tile.STATE_INACTIVE
        tile.label = getString(R.string.tile_freeze_manager)
        tile.subtitle = getString(R.string.tile_freeze_choose_app)
        tile.updateTile()
    }

    private companion object {
        // Distinct action prevents this PendingIntent from replacing other entrances.
        const val ACTION_OPEN_FREEZER = "nd.max.action.OPEN_FREEZE_MANAGER"
    }
}
