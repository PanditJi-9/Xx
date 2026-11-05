/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.TileService

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import nd.max.R
import nd.max.core.platform.SettingsAccess

/**
 * Quick Settings tile: the system night-light switch.
 *
 * **What it changes:** the platform key `secure/night_display_activated`, the
 * same one the display screen reads and writes — one key, two entry points, no
 * private copy of the state. Writing it needs `WRITE_SECURE_SETTINGS` (declared)
 * or the root fallback that [SettingsAccess.put] already performs, so a device
 * without either shows the failed write instead of pretending it worked.
 *
 * **Why the state is re-read after the tap:** the label must describe the
 * setting that now exists, never the intent of the tap. A write that was
 * refused leaves the key unchanged, and the tile then says "not applied" rather
 * than flipping to a state the system did not accept.
 */
class NightLightTileService : TileService() {

    private companion object {
        const val NAMESPACE = "secure"
        const val KEY = "night_display_activated"
    }

    override fun onStartListening() {
        super.onStartListening()
        render()
    }

    override fun onClick() {
        super.onClick()
        val current = read()
        // Unknown state turns the light on: that is the useful direction of a
        // single tap, and the re-read below decides what the tile reports.
        val requested = if (current == "1") "0" else "1"
        runCatching { SettingsAccess.put(NAMESPACE, KEY, requested) }
        render(requested = requested)
    }

    /**
     * The key as the platform reports it, or `null` when it cannot be read.
     *
     * No shell fallback here on purpose: a read runs on every `onStartListening`
     * (each time the shade opens), and a root probe per shade-open would cost
     * more than the answer is worth. [SettingsAccess.getDirect] already returns
     * `null` on a refusal, which is reported as an unknown state.
     */
    private fun read(): String? = runCatching {
        SettingsAccess.getDirect(NAMESPACE, KEY)
    }.getOrNull()

    private fun render(requested: String? = null) {
        val tile = qsTile ?: return
        val state = read()
        tile.state = when (state) {
            "1" -> Tile.STATE_ACTIVE
            "0" -> Tile.STATE_INACTIVE
            else -> Tile.STATE_UNAVAILABLE
        }
        tile.label = getString(R.string.tile_night_light)
        tile.subtitle = when {
            state == null -> getString(R.string.tile_night_unknown)
            // A tap that left the key on its old value is reported as a failed
            // apply, even when the write call itself returned true.
            requested != null && state != requested -> getString(R.string.status_not_applied)
            state == "1" -> getString(R.string.tile_night_on)
            else -> getString(R.string.tile_night_off)
        }
        tile.updateTile()
    }
}
