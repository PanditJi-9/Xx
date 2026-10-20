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
import nd.max.island.core.IslandControl
import nd.max.island.engine.IslandService
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
}

/**
 * أفعال الودجات — **وكلها تمرّ من واجهات التطبيق القائمة**:
 * `MemoryBoostEngine` نفسه الذي يستدعيه الرئيس، و`IslandControl` نفسه الذي
 * يستدعيه مفتاح الاستوديو. ولا كتابة sysfs ولا أمر جذر يُخترع هنا — الودجة
 * بابٌ إلى قدرات التطبيق لا نسخة ثانية منها.
 */
class RefreshWidgetsAction : ActionCallback {

    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        PulseWidget().updateAll(context)
        ControlsWidget().updateAll(context)
        StorageWidget().updateAll(context)
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
