/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.TileService

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import nd.max.R
import nd.max.service.FpsOverlayService

/**
 * FPS overlay from the quick settings panel.
 *
 * **The order of the three checks is deliberate:** overlay permission first,
 * because starting the service without it would run a foreground notification
 * for a window that cannot be drawn. Only then the live service state, read
 * from the companion flag — never from a stored preference, which can disagree
 * with reality after a process death or a system-initiated stop.
 */
class FpsOverlayTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        render()
    }

    override fun onTileAdded() {
        super.onTileAdded()
        render()
    }

    override fun onClick() {
        super.onClick()

        if (!Settings.canDrawOverlays(this)) {
            openOverlaySettings()
            return
        }

        val wasRunning = FpsOverlayService.isRunning
        runCatching {
            if (wasRunning) {
                FpsOverlayService.stop(this)
            } else {
                ContextCompat.startForegroundService(
                    this,
                    Intent(this, FpsOverlayService::class.java)
                )
            }
        }

        // The service starts/stops on another thread; re-read after a short
        // delay so the tile shows the state the system actually reached.
        Handler(Looper.getMainLooper()).postDelayed({ render() }, 350L)
    }

    private fun openOverlaySettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
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
        }
    }

    private fun render() {
        val tile = qsTile ?: return

        tile.label = getString(R.string.tile_fps_overlay)
        when {
            !Settings.canDrawOverlays(this) -> {
                tile.state = Tile.STATE_UNAVAILABLE
                tile.subtitle = getString(R.string.tile_fps_overlay_permission)
            }

            FpsOverlayService.isRunning -> {
                tile.state = Tile.STATE_ACTIVE
                tile.subtitle = getString(R.string.tile_fps_overlay_on)
            }

            else -> {
                tile.state = Tile.STATE_INACTIVE
                tile.subtitle = getString(R.string.tile_fps_overlay_off)
            }
        }
        tile.updateTile()
    }
}
