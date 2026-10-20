/*
 * Copyright (C) 2026-2027 Zexshia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nd.max.TileService

import nd.max.MaxManagerProps

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.topjohnwu.superuser.Shell
import nd.max.R
import nd.max.core.gamespace.BypassVerdict
import nd.max.core.platform.PropertyUtils

class BypassChgTileService : TileService() {

    private val BYPASS_PROP = MaxManagerProps.Conf.BYPASS_CHARGE
    private val PATH_PROP = MaxManagerProps.Conf.BYPASS_PATH
    private val BYPASSCHG_FILE = "/data/adb/.config/MaxManager/bypasschgconfig/bypasschg"

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val tile = qsTile ?: return

        if (tile.state == Tile.STATE_UNAVAILABLE) return

        val wantOn = tile.state != Tile.STATE_ACTIVE
        setBypassChg(if (wantOn) "1" else "0")

        // **الحالة تُقرأ بعد الكتابة، لا تُدَّعى قبلها:** كان هنا `tile.state = Tile.STATE_ACTIVE`
        // مباشرةً بعد النداء — فيبقى المظهر «مفعّلًا» بلا أثر على جهاز بلا جذر أو روم يرفض `set`.
        // والآن: إن لم تُقرأ القيمة المطلوبة نفسها فالبلاطة تعرض الواقع وتقول «لم يُطبَّق».
        val applied = BypassVerdict.applied(
            requestedOn = wantOn,
            readBack = PropertyUtils.get(BYPASS_PROP, "")
        )
        if (applied) {
            tile.state = if (wantOn) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            updateSubtitle(tile, getString(if (wantOn) R.string.status_active else R.string.status_inactive))
        } else {
            // القراءة تُعيد الحقيقة (وقد تكون الخصيصة غير موجودة أصلًا ⇒ UNSUPPORTED).
            updateTileState()
            updateSubtitle(tile, getString(R.string.status_not_applied))
        }

        tile.updateTile()
    }

    private fun setBypassChg(value: String) {
        // كتابة مُتحقَّقة (ثلاث طرق متصاعدة داخل `setAndConfirm`)، ثم الملفّ **أفضل جهد**: مسار
        // القراءة في هذا التطبيق هو الخصيصة، والملفّ تقرؤه الوحدة. فلا يُبنى الحكم على الملفّ أبدًا.
        PropertyUtils.setAndConfirm(BYPASS_PROP, value)
        Shell.cmd("echo $value > $BYPASSCHG_FILE").exec()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val bypassPath = PropertyUtils.get(PATH_PROP, "")

        if (bypassPath == "UNSUPPORTED") {
            tile.state = Tile.STATE_UNAVAILABLE
            updateSubtitle(tile, getString(R.string.status_not_supported))
        } else {
            val isEnabled = PropertyUtils.get(BYPASS_PROP, "0") == "1"
            tile.state = if (isEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            updateSubtitle(tile, getString(if (isEnabled) R.string.status_active else R.string.status_inactive))
        }

        tile.updateTile()
    }

    private fun updateSubtitle(tile: Tile, text: String) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            tile.subtitle = text
        }
    }

    override fun onTileAdded() {
        super.onTileAdded()
        updateTileState()
    }
}
