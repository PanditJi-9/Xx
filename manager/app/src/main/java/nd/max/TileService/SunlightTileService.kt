/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.TileService

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import nd.max.MaxManagerProps
import nd.max.R
import nd.max.core.display.DisplayFeatureNode
import nd.max.core.display.DisplayFeatureNodes
import nd.max.core.hardware.WriteVerification
import nd.max.core.platform.PropertyUtils

/**
 * Quick Settings tile: the panel's outdoor-visibility (sunlight/HBM) mode.
 *
 * The node is vendor-specific and may be absent; a missing node renders
 * [Tile.STATE_UNAVAILABLE] and the tile never writes to a guessed path. After a
 * tap the node is read again, and the persisted property is only updated when
 * the write was confirmed (`MATCHED`) — so the screen and the tile cannot
 * disagree after a failed write.
 *
 * The resolve step runs a root probe, so it is cached for the service lifetime:
 * a sysfs path does not appear or vanish between two tile refreshes.
 */
class SunlightTileService : TileService() {

    private var cachedNode: DisplayFeatureNode? = null

    override fun onStartListening() {
        super.onStartListening()
        render()
    }

    override fun onClick() {
        super.onClick()
        val node = node()
        if (node == null) {
            render()
            return
        }
        val live = DisplayFeatureNodes.isEnabled(node)
        val currentlyOn = live ?: (PropertyUtils.get(MaxManagerProps.Display.SUNLIGHT_MODE) == "1")
        val outcome = DisplayFeatureNodes.write(node, !currentlyOn)
        if (outcome == WriteVerification.Outcome.MATCHED) {
            PropertyUtils.set(
                MaxManagerProps.Display.SUNLIGHT_MODE,
                if (currentlyOn) "0" else "1"
            )
        }
        render(outcome)
    }

    /**
     * The resolve step runs a root probe, so a found node is cached for the
     * service lifetime. A **missing** node is *not* cached: root may be granted
     * after the tile was first shown, and a tile that stays "not supported"
     * until the process dies would hide a feature that now works.
     */
    private fun node(): DisplayFeatureNode? {
        cachedNode?.let { return it }
        return DisplayFeatureNodes.resolve(DisplayFeatureNodes.SUNLIGHT)
            ?.also { cachedNode = it }
    }

    private fun render(outcome: WriteVerification.Outcome? = null) {
        val tile = qsTile ?: return
        val node = node()
        if (node == null) {
            tile.state = Tile.STATE_UNAVAILABLE
            tile.label = getString(R.string.sunlight_mode)
            tile.subtitle = getString(R.string.status_not_supported)
            tile.updateTile()
            return
        }

        val live = DisplayFeatureNodes.isEnabled(node)
        tile.state = when {
            live == null -> Tile.STATE_UNAVAILABLE
            live -> Tile.STATE_ACTIVE
            else -> Tile.STATE_INACTIVE
        }
        tile.label = getString(R.string.sunlight_mode)
        tile.subtitle = when {
            outcome == WriteVerification.Outcome.WRITE_FAILED ||
                outcome == WriteVerification.Outcome.DIFFERS -> getString(R.string.status_not_applied)
            live == null -> getString(R.string.status_not_applied)
            live -> getString(R.string.tile_sunlight_on)
            else -> getString(R.string.tile_sunlight_off)
        }
        tile.updateTile()
    }
}
