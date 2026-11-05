/*
 * Copyright (C) 2026-2027 MaxManager contributors
 *
 * Licensed under the Apache License, Version 2.0.
 */
package nd.max.TileService

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import nd.max.R
import nd.max.core.platform.StatusBarIcons

/**
 * بلاط الإعدادات السريعة: «شريط حالة نظيف».
 *
 * **الآلية من ZenBubble:** الشريط النظيف لا يحتاج شريطًا بديلًا — يكفي `icon_blacklist` يحمل كل
 * الرموز المعروفة. وهنا يُقلَب الحال بضغطة، ويبقى المجهول (رموز الروم) كما هو.
 *
 * **ولماذا بلاط لا شاشة:** الإجراء لحظيّ ويُطلب من لوحة الإشعارات؛ وضغطتان في الإعدادات لكل مرّة
 * كانا سيجعلان الميزة غير مستعملة. ولا يُصدَّر البلاط إلا بصلاحية النظام المعتادة.
 */
class StatusBarTileService : TileService() {

    private val icons = StatusBarIcons.system()

    override fun onStartListening() {
        super.onStartListening()
        render()
    }

    override fun onClick() {
        super.onClick()
        val ok = runCatching { icons.setAllHidden(!icons.allHidden()) }.getOrDefault(false)
        render(success = ok)
    }

    private fun render(success: Boolean = true) {
        val tile = qsTile ?: return
        val clean = runCatching { icons.allHidden() }.getOrDefault(false)
        tile.state = if (clean) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.tile_statusbar_clean)
        tile.subtitle = getString(
            if (!success) R.string.tile_statusbar_denied
            else if (clean) R.string.tile_statusbar_on
            else R.string.tile_statusbar_off,
        )
        tile.updateTile()
    }
}
