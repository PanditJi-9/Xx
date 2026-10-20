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

package nd.max.ui.viewmodel

import nd.max.MaxManagerProps

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import nd.max.core.hardware.TouchControlBackend
import nd.max.core.platform.PropertyUtils

/**
 * شاشة اللمس: **عرض** حالة مقبض اللمس وتبديله — ولا تملك منطق عتاد.
 *
 * والتعريف الواحد لعُقَد اللمس والقدرة ومحرّك التسوية انتقل إلى [TouchControlBackend] في
 * `core/hardware`، وهذا الصنف يقرأ منه ويرسل إليه. ولماذا: كانت هنا **نسخة ثالثة** من قائمة
 * العُقَد، ومنطق الكتابة والتحقق، ومهلة إعادة المحاولة — ومنادون آخرون (`AppMonitor`،
 * `PanelToggles`) يستعملون النسخة نفسها من طبقة العرض. فصار للمقبض مالك واحد يكتب، وللشاشة
 * دور العرض وحده (ADR-11).
 *
 * وحدّه المعلَن: لا يقرّر «هل يعمل الجهاز» بنفسه — [TouchControlBackend.discover] تقول ذلك،
 * وهل النقل المعلن من الروم يصلح للكتابة تقوله [TouchControlBackend.VendorBridge.hasWritePath]
 * (وهي `false` حتى تُثبت أوضاع محوّل المصنّع على جهاز مُقاس).
 */
class TouchBoostViewModel : ViewModel() {

    companion object {
        private const val PROP_ENABLED = MaxManagerProps.Touch.BOOST
        private const val PROP_DT2W = MaxManagerProps.Touch.DT2W
    }

    var isAvailable by mutableStateOf<Boolean?>(null)
        private set

    /**
     * هل يملك التطبيق **طريق كتابة مثبَتًا** إلى محوّل المصنّع؟
     *
     * وليس سؤال وجود: `false` مع [vendorTransportPresent] `true` تعني «النقل معلن ولا نكتب فيه»
     * — وهي الحال المقيسة على POCO X6 Pro (خدمة `vendor.xiaomi.hw.touchfeature` قائمة).
     */
    var vendorHalDetected by mutableStateOf(false)
        private set

    /** خدمة اللمس الخاصة بالروم معلنة على هذا الجهاز (قراءة فقط) — تشرح سبب عدم الكتابة. */
    var vendorTransportPresent by mutableStateOf(false)
        private set

    /** عقدة لمحكّم اللمس — من المالك الواحد، فلا يُنشأ نوع ثانٍ للعرض. */
    var gameModeNode by mutableStateOf<TouchControlBackend.Node?>(null)
        private set
    var sampleRateNode by mutableStateOf<TouchControlBackend.Node?>(null)
        private set

    /**
     * عقدة تعزيز إدخال CPU (MediaTek `touch_boost`) — تُعرض بمعناها المقيس.
     *
     * ولا تُسمى «نمط لعب» ولا «معدّل استطلاع»: نواة الجهاز المقيس ترفع بها حدود تردّد CPU لحظة
     * اللمس، ولا تمسّ وحدة اللمس. ووجودها لا يمنع وجود [gameModeNode] معها — عقدتان مختلفتان.
     */
    var inputBoostNode by mutableStateOf<TouchControlBackend.Node?>(null)
        private set
    var boostEnabled by mutableStateOf(false)
        private set
    var doubleTapNode by mutableStateOf<TouchControlBackend.Node?>(null)
        private set
    var doubleTapEnabled by mutableStateOf(false)
        private set

    /** آخر اكتشاف كما قُرئ — يشاركه العرض فيسمّي المسارات والقناة بلا إعادة قراءة. */
    var discovery by mutableStateOf<TouchControlBackend.Discovery?>(null)
        private set

    fun loadState() {
        viewModelScope.launch(Dispatchers.IO) {
            val found = TouchControlBackend.discover()
            discovery = found
            gameModeNode = found.gameMode
            sampleRateNode = found.sampleRate
            inputBoostNode = found.inputBoost
            doubleTapNode = found.doubleTap

            vendorTransportPresent = found.vendorTransportPresent
            vendorHalDetected = found.vendorHasWritePath
            val hasBoostProvider = found.hasBoostNode || vendorHalDetected
            isAvailable = hasBoostProvider || found.doubleTap != null

            if (isAvailable == true) {
                boostEnabled = PropertyUtils.get(PROP_ENABLED) == "1"
                if (boostEnabled && hasBoostProvider) applyInternal(true)

                if (doubleTapNode != null) {
                    doubleTapEnabled = PropertyUtils.get(PROP_DT2W) == "1"
                    if (doubleTapEnabled) applyDoubleTapInternal(true)
                }
            }
        }
    }

    fun setBoost(enabled: Boolean) {
        boostEnabled = enabled
        viewModelScope.launch(Dispatchers.IO) {
            val applied = applyInternal(enabled)
            if (applied) {
                PropertyUtils.set(PROP_ENABLED, if (enabled) "1" else "0")
            } else {
                boostEnabled = !enabled
            }
        }
    }

    /**
     * يعيد «هل استقرّ الطلب؟»: النجاح و«لا شيء يُفعَل» (`persistable`) يُبقيان المفتاح، والرفض
     * يُرجعه — والرفض نفسه مكتوب في السجل برمزه من [TouchControlBackend.Attempt].
     */
    private fun applyInternal(enabled: Boolean): Boolean =
        TouchControlBackend.reconcile(enabled).persistable

    fun setDoubleTapToWake(enabled: Boolean) {
        doubleTapEnabled = enabled
        viewModelScope.launch(Dispatchers.IO) {
            if (applyDoubleTapInternal(enabled)) {
                PropertyUtils.set(PROP_DT2W, if (enabled) "1" else "0")
            } else {
                doubleTapEnabled = !enabled
            }
        }
    }

    private fun applyDoubleTapInternal(enabled: Boolean): Boolean =
        TouchControlBackend.reconciler.reconcileDoubleTap(enabled)
}
