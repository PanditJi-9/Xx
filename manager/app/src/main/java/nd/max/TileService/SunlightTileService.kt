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
import dagger.hilt.android.EntryPointAccessors
import nd.max.core.persist.PersistArbiterEntryPoint
import nd.max.core.hardware.NodeTuning
import nd.max.core.policy.DangerConsent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var busy = false

    override fun onStartListening() {
        super.onStartListening()
        scope.launch { renderSample() }
    }

    override fun onClick() {
        super.onClick()
        if (busy) return
        busy = true
        scope.launch {
            try {
                val outcome = withContext(Dispatchers.IO) {
                    val node = node() ?: return@withContext WriteVerification.Outcome.READBACK_UNAVAILABLE
                    val currentlyOn = DisplayFeatureNodes.isEnabled(node) ?: return@withContext WriteVerification.Outcome.READBACK_UNAVAILABLE
                    // First enablement is performed from Display Studio, where the warning is visible.
                    if (!currentlyOn && !DangerConsent.isAcknowledged(this@SunlightTileService, DangerConsent.PANEL_SUNLIGHT)) {
                        return@withContext WriteVerification.Outcome.WRITE_FAILED
                    }
                    val arbiter = runCatching {
                        EntryPointAccessors.fromApplication(applicationContext, PersistArbiterEntryPoint::class.java).arbiter()
                    }.getOrNull() ?: return@withContext WriteVerification.Outcome.WRITE_FAILED
                    val written = DisplayFeatureNodes.writeGuarded(node, !currentlyOn, NodeTuning(arbiter))
                    if (written == WriteVerification.Outcome.MATCHED) {
                        PropertyUtils.set(MaxManagerProps.Display.SUNLIGHT_MODE, if (currentlyOn) "0" else "1")
                    }
                    written
                }
                renderSample(outcome)
            } finally {
                busy = false
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
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

    private suspend fun renderSample(outcome: WriteVerification.Outcome? = null) {
        val sample = withContext(Dispatchers.IO) {
            val node = node()
            Triple(node, node?.let(DisplayFeatureNodes::isEnabled), DangerConsent.isAcknowledged(this@SunlightTileService, DangerConsent.PANEL_SUNLIGHT))
        }
        render(sample.first != null, sample.second, sample.third, outcome)
    }

    private fun render(present: Boolean, live: Boolean?, acknowledged: Boolean, outcome: WriteVerification.Outcome?) {
        val tile = qsTile ?: return
        if (!present) {
            tile.state = Tile.STATE_UNAVAILABLE
            tile.label = getString(R.string.sunlight_mode)
            tile.subtitle = getString(R.string.status_not_supported)
            tile.updateTile()
            return
        }

        tile.state = when {
            live == null -> Tile.STATE_UNAVAILABLE
            live -> Tile.STATE_ACTIVE
            else -> Tile.STATE_INACTIVE
        }
        tile.label = getString(R.string.sunlight_mode)
        tile.subtitle = when {
            live == false && !acknowledged -> getString(R.string.panel_sunlight_consent_needed)
            outcome == WriteVerification.Outcome.WRITE_FAILED ||
                outcome == WriteVerification.Outcome.DIFFERS -> getString(R.string.status_not_applied)
            live == null -> getString(R.string.status_not_applied)
            live -> getString(R.string.tile_sunlight_on)
            else -> getString(R.string.tile_sunlight_off)
        }
        tile.updateTile()
    }
}
