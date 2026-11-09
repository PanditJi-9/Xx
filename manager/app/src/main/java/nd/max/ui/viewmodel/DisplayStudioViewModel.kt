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
import nd.max.core.display.DisplayFeatureNode
import nd.max.core.display.NightLightSchedule
import nd.max.core.display.DisplayFeatureNodes
import nd.max.core.hardware.RootFileAccess
import nd.max.core.hardware.HardwareControlArbiter
import nd.max.core.hardware.NodeTuning
import nd.max.core.hardware.WriteVerification
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext
import nd.max.core.policy.DangerConsent

import android.content.ContentResolver
import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import nd.max.core.platform.PropertyUtils
import nd.max.ui.util.XiaomiVendorHalUtil

/** Keeps the view model's existing name while the shared type lives in core. */
private typealias FeatureNode = DisplayFeatureNode

/**
 * Display panel feature nodes, like touch nodes, are vendor-specific rather than
 * a common kernel API. Each feature probes a short list of real-world OEM sysfs
 * paths and only exposes the toggle if a matching node is actually present.
 *
 * The node lists themselves live in [DisplayFeatureNodes] so the Quick Settings
 * tile and this screen probe the same paths.
 */
@HiltViewModel
class DisplayStudioViewModel @Inject constructor(
    arbiter: HardwareControlArbiter,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {
    private val nodeTuning = NodeTuning(arbiter)

    companion object {
        private val SUNLIGHT_CANDIDATES = DisplayFeatureNodes.SUNLIGHT
        private val SILKY_CANDIDATES = DisplayFeatureNodes.SILKY
        private val VIDEO_ENH_CANDIDATES = DisplayFeatureNodes.VIDEO_ENHANCE
        private val HDR_CANDIDATES = DisplayFeatureNodes.HDR

        private const val PROP_SUNLIGHT = MaxManagerProps.Display.SUNLIGHT_MODE
        private const val PROP_SILKY = MaxManagerProps.Display.SILKY_BRIGHTNESS
        private const val PROP_VIDEO = MaxManagerProps.Display.VIDEO_ENHANCE
        private const val PROP_HDR = MaxManagerProps.Display.HDR_ENABLE

        /** Preset window/transition/animator durations, in the order shown to the user. "Off" is 0. */
        val ANIMATION_SCALE_PRESETS = listOf(0f, 0.5f, 1f, 1.5f, 2f)
        /** Preset font scales, in the order shown to the user. 1.0 is the system default. */
        val FONT_SCALE_PRESETS = listOf(0.85f, 1f, 1.15f, 1.3f)
        /** Preset screen-off timeouts in seconds, in the order shown to the user. */
        val SCREEN_TIMEOUT_PRESETS = listOf(15, 30, 60, 120, 300, 600, 1800)

        /** مفتاح إعداد المنصّة الذي يقرؤه مدير النوافذ — مصدره واحد، ويحرسه اختبار. */
        internal const val WINDOW_BLUR_KEY = "disable_window_blurs"

        /**
         * ما يُكتب عند إيقاف/إعادة ضبابية النوافذ — **قائمة صريحة يقفلها اختبار**.
         *
         * سطران لا سطر: الإعداد العامّ هو ما يبقى بعد إعادة التشغيل، و`cmd window disable-blur`
         * هو ما يُطبَّق **الآن** (بعض النسخ تقرأ الإعداد عند الإقلاع فقط، فلا يكفي وحده). والإطفاء
         * يكتب `0` في الاثنين معًا — فلا تبقى الضبابية موقوفة بعد إرجاع الخيار.
         */
        internal fun blurWriteCommands(disabled: Boolean): List<String> {
            val value = if (disabled) "1" else "0"
            return listOf(
                "settings put global $WINDOW_BLUR_KEY $value",
                "cmd window disable-blur $value",
            )
        }
    }

    var animationScale by mutableStateOf(1f); private set
    var fontScale by mutableStateOf(1f); private set
    var screenTimeoutSeconds by mutableStateOf(30); private set
    var nightLightEnabled by mutableStateOf(false); private set

    /**
     * جدولة الوضع الليلي: نافذة زمنية تكتب المفتاح نفسه الذي يكتبه المفتاح اليدوي.
     *
     * **ولماذا الحالة هنا لا في الشاشة وحدها:** الشاشة تُغلق، والمنبّه يبقى؛ فالقيم
     * تُقرأ من `NightLightSchedule` عند كل فتح، لا من `remember` يُصفَّر.
     */
    var nightScheduleEnabled by mutableStateOf(false); private set
    var nightStartMinute by mutableStateOf(NightLightSchedule.DEFAULT_START_MINUTE); private set
    var nightEndMinute by mutableStateOf(NightLightSchedule.DEFAULT_END_MINUTE); private set
    var colorInversionEnabled by mutableStateOf(false); private set

    /**
     * ضبابية النوافذ الخلفية — إعداد المنصّة `disable_window_blurs` + أمر مدير النوافذ الفوريّ.
     *
     * **ولماذا:** الضبابية الخلفية (خلف شريط الحالة ولوحة الإشعارات وقوائم النظام) تُرسم على
     * معالج الرسوميات في كل إطار، وإيقافها خفضُ حمل حقيقيّ على الأجهزة المتوسطة — فهو خيار
     * عرضٍ قبل أن يكون خيار أداء: يُقدَّم بوصفه «حرارة أقل وواجهة أبسط» لا «تسريعًا».
     *
     * **والآلية آلية أندرويد نفسها** — مفتاح عامّ في `Settings.Global` يُقرأ عند الإقلاع،
     * وأمر `cmd window disable-blur` يُطبَّق فورًا — وليست عقدة خاصّة بمزوّد؛ وما لا يدعمها
     * يتجاهل الأمرين فلا يقع ضرر، وما يدعمها يعود كما كان بإرجاع الخيار (يكتب `0`).
     */
    var windowBlurDisabled by mutableStateOf(false); private set

    var isLoaded by mutableStateOf(false)
        private set
    // Set when none of the sysfs feature nodes exist but the device declares
    // Xiaomi's vendor display HAL instead — see XiaomiVendorHalUtil.
    var vendorHalDetected by mutableStateOf(false)
        private set

    var sunlightNode by mutableStateOf<FeatureNode?>(null); private set
    var silkyNode by mutableStateOf<FeatureNode?>(null); private set
    var videoEnhanceNode by mutableStateOf<FeatureNode?>(null); private set
    var hdrNode by mutableStateOf<FeatureNode?>(null); private set

    var sunlightEnabled by mutableStateOf<Boolean?>(null); private set
    var sunlightBusy by mutableStateOf(false); private set
    var sunlightOutcome by mutableStateOf<WriteVerification.Outcome?>(null); private set
    var silkyEnabled by mutableStateOf(false); private set
    var videoEnhanceEnabled by mutableStateOf(false); private set
    var hdrEnabled by mutableStateOf(false); private set

    fun loadState(context: Context) {
        val resolver = context.contentResolver
        viewModelScope.launch(Dispatchers.IO) {
            // أربع قوائم مرشّحين: كانت ثمانية نداءات وجود متتابعة (`test -e` عبر صدفة لكل
            // مرشّح)، وصارت نداءً واحدًا لكل قائمة عبر الدفعة الأصلية.
            sunlightNode = DisplayFeatureNodes.resolve(SUNLIGHT_CANDIDATES)
            silkyNode = RootFileAccess.firstExisting(SILKY_CANDIDATES) { it.path }
            videoEnhanceNode = RootFileAccess.firstExisting(VIDEO_ENH_CANDIDATES) { it.path }
            hdrNode = RootFileAccess.firstExisting(HDR_CANDIDATES) { it.path }

            if (sunlightNode == null && silkyNode == null && videoEnhanceNode == null && hdrNode == null) {
                vendorHalDetected = XiaomiVendorHalUtil.hasDisplayFeatureHal()
            }

            sunlightEnabled = sunlightNode?.let(DisplayFeatureNodes::isEnabled)
            silkyEnabled = PropertyUtils.get(PROP_SILKY) == "1"
            videoEnhanceEnabled = PropertyUtils.get(PROP_VIDEO) == "1"
            hdrEnabled = PropertyUtils.get(PROP_HDR) == "1"

            if (silkyEnabled) apply(silkyNode, true)
            if (videoEnhanceEnabled) apply(videoEnhanceNode, true)
            if (hdrEnabled) apply(hdrNode, true)

            animationScale = readSetting(resolver, "global", "window_animation_scale")?.toFloatOrNull() ?: 1f
            fontScale = readSetting(resolver, "system", "font_scale")?.toFloatOrNull() ?: 1f
            screenTimeoutSeconds = ((readSetting(resolver, "system", "screen_off_timeout")?.toLongOrNull() ?: 30_000L) / 1000L).toInt()
            nightLightEnabled = readSetting(resolver, "secure", "night_display_activated") == "1"
            nightScheduleEnabled = NightLightSchedule.isEnabled(context)
            nightStartMinute = NightLightSchedule.startMinute(context)
            nightEndMinute = NightLightSchedule.endMinute(context)
            colorInversionEnabled = readSetting(resolver, "secure", "accessibility_display_inversion_enabled") == "1"
            windowBlurDisabled = readSetting(resolver, "global", WINDOW_BLUR_KEY) == "1"

            isLoaded = true
        }
    }

    /**
     * قراءة إعداد واحد — **بالواجهة المباشرة أوّلًا، والصدفة احتياطًا**.
     *
     * ** ولماذا (عطب سرعة مُبلَّغ عنه في فتح الشاشات):** كانت `Shell.cmd("settings get …")`
     * لكل مفتاح — **خمس رحلات صدفة** في كل فتح لهذه الشاشة (ولادة عملية + تفسير صدفة لكل واحدة)،
     * وهي رحلات تُصطفّ على صدَفة الجذر الواحدة في التطبيق. والإعداد نفسه تقرؤه واجهة أندرويد
     * بلا جذر وبلا صدفة (`Settings`)، فالقراءة المباشرة تُلغي الخمس.
     *
     * **وحدّها معلن:** إن رفضت المنصّة المفتاح (‏`SecurityException` في المفاتيح المحميّة) عادت
     * الصدفة كما كانت حرفيًّا — فالسلوك على جهاز لا تُقرأ فيه الواجهة **محفوظ**، لا مُفترَض.
     * و`null` من الواجهة يعني «غير مضبوط» — وهي بالضبط ما تعنيه `settings get` بـ`null`.
     */
    private fun readSetting(resolver: ContentResolver, namespace: String, key: String): String? {
        val direct = runCatching {
            when (namespace) {
                "global" -> Settings.Global.getString(resolver, key)
                "system" -> Settings.System.getString(resolver, key)
                "secure" -> Settings.Secure.getString(resolver, key)
                else -> null
            }
        }
        if (direct.isSuccess) {
            return direct.getOrNull()?.takeIf { it.isNotEmpty() && it != "null" }
        }
        return nd.max.core.privilege.RootCommands.cmd("settings get $namespace $key").exec().out.joinToString("").trim()
            .takeIf { it.isNotEmpty() && it != "null" }
    }


    private fun apply(node: FeatureNode?, enabled: Boolean) {
        node?.let {
            // الطبقة المصرَّحة (ADR-11): قناة الجذر ثم رقصة chmod ثم قراءة تحقّق — بدل صدفة
            // خام تُخفي `2>/dev/null` كل سبب فشل (`NT-13`). المنطق نفسه في
            // [DisplayFeatureNodes.write] لأن بلاطة الإعدادات السريعة تكتب العقدة نفسها.
            DisplayFeatureNodes.write(it, enabled)
        }
    }

    fun setSunlight(enabled: Boolean) {
        val node = sunlightNode ?: return
        if (sunlightBusy || sunlightEnabled == null) return
        sunlightBusy = true
        viewModelScope.launch {
            try {
                sunlightOutcome = kotlinx.coroutines.withContext(Dispatchers.IO) {
                    if (enabled && !DangerConsent.isAcknowledged(appContext, DangerConsent.PANEL_SUNLIGHT)) {
                        WriteVerification.Outcome.WRITE_FAILED
                    } else {
                        DisplayFeatureNodes.writeGuarded(node, enabled, nodeTuning)
                    }
                }
                sunlightEnabled = kotlinx.coroutines.withContext(Dispatchers.IO) { DisplayFeatureNodes.isEnabled(node) }
                if (sunlightOutcome == WriteVerification.Outcome.MATCHED) {
                    kotlinx.coroutines.withContext(Dispatchers.IO) { PropertyUtils.set(PROP_SUNLIGHT, if (enabled) "1" else "0") }
                }
            } finally {
                sunlightBusy = false
            }
        }
    }

    fun setSilky(enabled: Boolean) {
        silkyEnabled = enabled
        viewModelScope.launch(Dispatchers.IO) {
            apply(silkyNode, enabled)
            PropertyUtils.set(PROP_SILKY, if (enabled) "1" else "0")
        }
    }

    fun setVideoEnhance(enabled: Boolean) {
        videoEnhanceEnabled = enabled
        viewModelScope.launch(Dispatchers.IO) {
            apply(videoEnhanceNode, enabled)
            PropertyUtils.set(PROP_VIDEO, if (enabled) "1" else "0")
        }
    }

    fun setHdr(enabled: Boolean) {
        hdrEnabled = enabled
        viewModelScope.launch(Dispatchers.IO) {
            apply(hdrNode, enabled)
            PropertyUtils.set(PROP_HDR, if (enabled) "1" else "0")
        }
    }

    fun updateAnimationScale(scale: Float) {
        animationScale = scale
        viewModelScope.launch(Dispatchers.IO) {
            nd.max.core.privilege.RootCommands.cmd("settings put global window_animation_scale $scale",
            "settings put global transition_animation_scale $scale",
            "settings put global animator_duration_scale $scale").exec()
        }
    }

    fun updateFontScale(scale: Float) {
        fontScale = scale
        viewModelScope.launch(Dispatchers.IO) {
            nd.max.core.platform.SettingsAccess.put("system", "font_scale", scale.toString())
        }
    }

    fun setScreenTimeout(seconds: Int) {
        screenTimeoutSeconds = seconds
        viewModelScope.launch(Dispatchers.IO) {
            nd.max.core.platform.SettingsAccess.put("system", "screen_off_timeout", (seconds * 1000).toString())
        }
    }

    fun setNightLight(enabled: Boolean) {
        nightLightEnabled = enabled
        viewModelScope.launch(Dispatchers.IO) {
            nd.max.core.platform.SettingsAccess.put("secure", "night_display_activated", if (enabled) "1" else "0")
        }
    }

    /**
     * تشغيل الجدولة أو إيقافها.
     *
     * التطبيق الفوري داخل `NightLightSchedule.setEnabled` لا هنا: الرحلة إلى القرص
     * والمنبّه والكتابة كلها في الخيط الخلفي، والشاشة ترى القيمة المطلوبة في الحال.
     */
    fun setNightScheduleEnabled(context: Context, enabled: Boolean) {
        nightScheduleEnabled = enabled
        viewModelScope.launch(Dispatchers.IO) {
            NightLightSchedule.setEnabled(context.applicationContext, enabled)
        }
    }

    fun setNightStartMinute(context: Context, minute: Int) {
        nightStartMinute = minute
        viewModelScope.launch(Dispatchers.IO) {
            NightLightSchedule.setWindow(context.applicationContext, minute, nightEndMinute)
        }
    }

    fun setNightEndMinute(context: Context, minute: Int) {
        nightEndMinute = minute
        viewModelScope.launch(Dispatchers.IO) {
            NightLightSchedule.setWindow(context.applicationContext, nightStartMinute, minute)
        }
    }

    fun setColorInversion(enabled: Boolean) {
        colorInversionEnabled = enabled
        viewModelScope.launch(Dispatchers.IO) {
            nd.max.core.platform.SettingsAccess.put("secure", "accessibility_display_inversion_enabled", if (enabled) "1" else "0")
        }
    }

    fun applyWindowBlurDisabled(disabled: Boolean) {
        windowBlurDisabled = disabled
        viewModelScope.launch(Dispatchers.IO) {
            nd.max.core.privilege.RootCommands.cmd(*blurWriteCommands(disabled).toTypedArray()).exec()
        }
    }
}
