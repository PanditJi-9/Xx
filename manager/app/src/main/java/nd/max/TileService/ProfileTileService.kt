/*
 * Copyright (C) 2026-2027 Zexshia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nd.max.TileService

import nd.max.MaxManagerProps

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nd.max.R
import nd.max.core.platform.PropertyUtils
import nd.max.ui.util.RootUtils

class ProfileTileService : TileService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        /**
         * نيّة «افتح شاشة الملفّ» من الشبكة — يقرؤها `MainActivity` ويحوّلها إلى مسار `now`،
         * فالمستخدم يبدّل من الشاشة التي تملك القرار لا من نافذة مالكها خدمة.
         */
        const val ACTION_OPEN_PROFILE = "nd.max.action.OPEN_PROFILE"

        private const val AI_PROP = MaxManagerProps.Conf.AI_ENABLED
    }

    override fun onCreate() {
        super.onCreate()
        serviceScope.launch {
            RootUtils.isRootGranted()
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    /**
     * الضغط يفتح شاشة الملفّ داخل التطبيق — ولا يُطبَّق ملفّ من هنا إطلاقًا.
     *
     * **والعطب الذي أُزيل (بلاغ المالك ٢٠٢٦-١١-١٥):** كانت الشبكة تُنشئ `AlertDialog` وتُظهره بـ
     * `showDialog`، أي **نافذة مالكها خدمة** لا نشاط؛ وهذا مسار مهجور منذ أندرويد ١٠، وبقيّة نافذة
     * بلا رمز صالح تُترك فوق الواجهة فتتجمّد. وكان الضغط يستدعي `ProfileApplier.apply` مباشرةً،
     * وهو **مالك ثانٍ** للقرار يتجاوز `maxAiEngine.requestManualProfile` وبوّابته، ويحتمل انتظارًا
     * غير محدود في صفّ الجذر (`RootCommands.commandLock`) — وأيّ منهما كافٍ لتسمّر التطبيق.
     *
     * فصار للقرار مالك واحد: تُفتح شاشة `now` حيث `HomeViewmodel.applyProfile` ← المحرّك،
     * بلا تنفيذ جذريّ ولا نافذة في هذه الخدمة. والشبكة تبقى **قارئة** فقط في [updateTileState].
     */
    override fun onClick() {
        super.onClick()
        openProfileScreen()
    }

    private fun openProfileScreen() {
        val intent = Intent(this, nd.max.MainActivity::class.java)
            .setAction(ACTION_OPEN_PROFILE)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // من أندرويد ١٤ لا تُفتح واجهة من شبكة بنيّة مباشرة: الوسيط `PendingIntent`.
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

    private fun updateTileState() {
        val tile = qsTile ?: return
        serviceScope.launch {
            val aiEnabled = PropertyUtils.get(AI_PROP, "0")

            if (aiEnabled != "0") {
                withContext(Dispatchers.Main) {
                    tile.state = Tile.STATE_UNAVAILABLE
                    updateSubtitle(tile, getString(R.string.str_auto_mode))
                    tile.updateTile()
                }
            } else {
                val profileResStr = RootUtils.getCurrentProfileRes()

                withContext(Dispatchers.Main) {
                    tile.state = Tile.STATE_ACTIVE
                    updateSubtitle(tile, getString(profileResStr))
                    tile.updateTile()
                }
            }
        }
    }

    private fun updateSubtitle(tile: Tile, text: String) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            tile.subtitle = text
        }
    }
}
