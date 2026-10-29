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

package nd.max.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import kotlin.coroutines.cancellation.CancellationException
import nd.max.core.diagnostics.DiagnosticCenter

/**
 * قاعدة ودجات Glance في التطبيق كلّها.
 *
 * لماذا وُجدت: إذا فشل رسم الودجة لا يصل المشغّل إلى محتوى جديد، فيبقى على تخطيطه
 * الأوّلي (`initialLayout`) — خانة فارغة بلا سبب ظاهر. هذه القاعدة تُغلّف `provideGlance`
 * فيُسجَّل كل فشل في تشخيص التطبيق باسم الودجة ونصّ الاستثناء، ثم يُعاد رميه كما هو
 * (فلا يتغيّر سلوك الرسم نفسه). وبذلك يظهر السبب في شاشة التشخيص بدل أن يُخمَّن.
 *
 * الودجة تكتب رسمها في [renderGlance] كما كانت تكتبه داخل `provideGlance`.
 */
abstract class MaxGlanceWidget : GlanceAppWidget() {

    final override suspend fun provideGlance(context: Context, id: GlanceId) {
        try {
            renderGlance(context, id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            runCatching {
                DiagnosticCenter.record(
                    component = "widget",
                    message = "${javaClass.simpleName} failed to render",
                    throwable = e,
                )
            }
            throw e
        }
    }

    /** رسم الودجة نفسه: كل ما كان داخل `provideGlance` يُكتب هنا. */
    protected abstract suspend fun renderGlance(context: Context, id: GlanceId)
}
