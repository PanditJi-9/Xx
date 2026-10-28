/*
 * Copyright (C) 2026-2027 MaxManager contributors
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

package nd.max.widget.core

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
// `updateAll` / `provideContent`: امتدادات في `GlanceAppWidgetKt` — لا يراها المُصرّف بلا استيراد صريح.
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.Intent
import android.os.Build
import nd.max.island.core.IslandControl
import nd.max.island.engine.IslandService
import nd.max.service.FpsOverlayService
import nd.max.ui.util.MemoryBoostEngine
import nd.max.widget.controls.ControlsWidget
import nd.max.widget.pulse.PulseWidget
import nd.max.widget.storage.StorageWidget

/**
 * عقود النوايا التي تحملها الودجات إلى التطبيق — اسم واحد يُقرأ في
 * `MainActivity` ويُكتب هنا، فلا يفترق كاتبُ المفتاح وقارئه.
 */
object WidgetIntents {
    const val EXTRA_OPEN_ROUTE = "nd.max.widget.OPEN_ROUTE"
    const val ROUTE_STORAGE = "storage_care"
    const val ROUTE_CONTROL = "control"
    const val ROUTE_ISLAND = "dynamic_island"
    const val ROUTE_GAME_LOBBY = "game_lobby"
    const val ROUTE_GPU_STUDIO = "gpustudio"
    const val ROUTE_FPS_OVERLAY = "fpsoverlay"
    const val ROUTE_FPS_GO = "fpsgoscreen"
    const val ROUTE_CPU_CONTROL = "cpucorecontrol"
    const val ROUTE_THERMAL = "thermal_detail"
    const val ROUTE_BATTERY = "hub_power"
    const val ROUTE_DISPLAY = "displaystudio"
    const val ROUTE_TOUCH = "touchboost"
    const val ROUTE_AUDIO = "audio_studio"
    const val ROUTE_NETWORK = "networkscheduler"
    const val ROUTE_APP_FREEZER = "debloatfreeze"
    const val ROUTE_SETEDIT = "setedit"
    const val ROUTE_DEVICE_INFO = "device_info"
}

/**
 * أفعال الودجات — **وكلها تمرّ من واجهات التطبيق القائمة**:
 * `MemoryBoostEngine` نفسه الذي يستدعيه الرئيس، و`IslandControl` نفسه الذي
 * يستدعيه مفتاح الاستوديو. ولا كتابة sysfs ولا أمر جذر يُخترع هنا — الودجة
 * بابٌ إلى قدرات التطبيق لا نسخة ثانية منها.
 */
class RefreshWidgetsAction : ActionCallback {

    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        runCatching { PulseWidget().updateAll(context) }
        runCatching { ControlsWidget().updateAll(context) }
        runCatching { StorageWidget().updateAll(context) }
    }
}

/**
 * تشغيل أو إطفاء تراكب عداد الإطارات (FPS Overlay) بضغطة واحدة من الودجة.
 */
class ToggleFpsOverlayAction : ActionCallback {

    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val running = FpsOverlayService.isRunning
        if (running) {
            FpsOverlayService.stop(context)
        } else {
            runCatching {
                val intent = Intent(context, FpsOverlayService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }
        withContext(Dispatchers.IO) {
            kotlinx.coroutines.delay(200L)
        }
        RefreshWidgetsAction().onAction(context, glanceId, parameters)
    }
}

/** «تحرير الذاكرة»: يقيس، يوقف عمليات الخلفية، يقيس — ويُخزَّن الناتج للعرض. */
class BoostAction : ActionCallback {

    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val outcome = withContext(Dispatchers.IO) {
            runCatching {
                MemoryBoostEngine.boost { MemoryBoostEngine.availableMb(context) }
            }.getOrNull()
        }
        val appWidgetId = runCatching { GlanceAppWidgetManager(context).getAppWidgetId(glanceId) }
            .getOrDefault(WidgetConfigStore.INVALID_ID)
        if (appWidgetId != WidgetConfigStore.INVALID_ID) {
            WidgetConfigStore(context).saveLastBoost(
                appWidgetId,
                LastBoost(
                    atMs = System.currentTimeMillis(),
                    freedMb = outcome?.freedMb,
                    executed = outcome?.executed == true,
                ),
            )
        }
        PulseWidget().updateAll(context)
        ControlsWidget().updateAll(context)
    }
}

/**
 * تشغيل/إطفاء جزيرة الاستوديو. والحالة تُقرأ من المصدر الذي تقرأ منه الشاشة
 * ([IslandService.isActive]) لا من نيّة المستخدم — لأن الجزيرة قد تكون حيّة
 * بخدمة الوصول وحدها بعد إقلاع، فيكذب «مطفأ» على مفتاحٍ يقرأ رغبةً مخزَّنة.
 */
class IslandToggleAction : ActionCallback {

    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val active = runCatching { IslandService.isActive() }.getOrDefault(false)
        runCatching {
            if (active) IslandControl.stop(context) else IslandControl.start(context)
        }
        ControlsWidget().updateAll(context)
    }
}
