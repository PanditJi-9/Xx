/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.TileService

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import nd.max.R
import nd.max.core.battery.BatteryHistoryStore

/**
 * Quick Settings tile: turn battery-history recording on or off.
 *
 * **Why a tile:** recording is a background behaviour the user should be able
 * to check and stop from the notification shade, without opening a screen to
 * discover it is still running. The tile reads the store, writes the toggle and
 * then **re-reads** it: the label always describes the alarm state that exists,
 * never the tap we just performed.
 *
 * No permission, no root: the only thing it changes is this app's own
 * preference and its own fifteen-minute alarm.
 */
class BatteryHistoryTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        render()
    }

    override fun onClick() {
        super.onClick()
        runCatching { BatteryHistoryStore.setEnabled(this, !BatteryHistoryStore.isEnabled(this)) }
        render()
    }

    private fun render() {
        val tile = qsTile ?: return
        val recording = BatteryHistoryStore.isEnabled(this)
        tile.state = if (recording) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.tile_battery_history)
        tile.subtitle = getString(
            if (recording) R.string.tile_battery_history_on
            else R.string.tile_battery_history_off
        )
        tile.updateTile()
    }
}
