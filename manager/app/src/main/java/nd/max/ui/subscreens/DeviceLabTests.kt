/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.subscreens

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.ui.graphics.vector.ImageVector
import nd.max.R
import nd.max.ui.design.MaxTone

/**
 * **كل اختبار يعرضه المختبر، في مكان واحد.**
 *
 * الشاشة كانت تعرض الاختبارات بأربع طرق مختلفة: حوارات تُفتح من صفوف، وأقسامًا تُقرأ بالتمرير،
 * وبطاقات بلا نتيجة. فمن يفتح المختبر ليعرف «هل جهازي سليم؟» لا يجد جوابًا: يرى قائمة قراءات
 * وأزرارًا، ولا يرى **ما جُرِّب وما لم يُجرَّب**.
 *
 * وهذا الجدول يجعل السؤال واحدًا: لكل قطعة بطاقة، وللبطاقة نتيجة. والنتيجة **يضعها المستخدم**
 * لأنه الوحيد الذي يسمع السماعة ويرى الشاشة — والتطبيق لا يدّعي أنه يعرف. وما يقيسه التطبيق
 * فعلًا (الشحن · الميكروفون · المستشعرات · الشبكة) يعرض قراءته في قسمه، ولا يمنح نفسه وسم نجاح.
 */
enum class LabTestId(
    @StringRes val titleRes: Int,
    val icon: ImageVector,
) {
    SCREEN(R.string.device_lab_screen_title, Icons.Rounded.Science),
    TOUCH(R.string.device_lab_touch_title, Icons.Rounded.TouchApp),
    BUTTONS(R.string.device_lab_buttons_title, Icons.Rounded.Keyboard),
    VIBRATION(R.string.device_lab_vibration_title, Icons.Rounded.Vibration),
    SPEAKER(R.string.device_lab_speaker_title, Icons.AutoMirrored.Rounded.VolumeUp),
    MICROPHONE(R.string.device_lab_mic_title, Icons.Rounded.Mic),
    TORCH(R.string.device_lab_torch_title, Icons.Rounded.FlashOn),
    SENSORS(R.string.device_lab_sensors_title, Icons.Rounded.Sensors),
    BATTERY(R.string.device_lab_battery_title, Icons.Rounded.BatteryChargingFull),
    CHARGING(R.string.device_lab_section_charge, Icons.Rounded.BatteryChargingFull),
    CAMERAS(R.string.device_lab_section_cameras, Icons.Rounded.CameraAlt),
    RADIOS(R.string.device_lab_section_radios, Icons.Rounded.Wifi),
    WIFI(R.string.device_lab_section_wifi, Icons.Rounded.Wifi),
}

/**
 * **نتيجةٌ يضعها من جرّب، لا استنتاجًا من التطبيق.**
 *
 * ثلاث حالات لا رابعة: لم يُجرَّب · يعمل · معطوب. ولا «نجاح» تلقائي عند فتح الحوار، لأن فتح
 * الشاشة الحمراء ليس دليلًا على سلامة الشاشة. واللون يخدم الكلمة ولا يغني عنها (ADR-07).
 */
enum class LabOutcome(@StringRes val labelRes: Int, val tone: MaxTone) {
    NOT_RUN(R.string.device_lab_outcome_not_run, MaxTone.Inactive),
    PASSED(R.string.device_lab_outcome_passed, MaxTone.Positive),
    FAILED(R.string.device_lab_outcome_failed, MaxTone.Caution),
}

/**
 * يحوّل ما حُفظ إلى ما تعرضه البطاقة — **ومن لا يُعرف يُسقَط**.
 *
 * مفتاح مجهول (اختبار حُذف من الجدول) أو رمز نتيجة مجهول (نسخة أقدم كتبت رمزًا آخر)
 * لا يُعرضان كحكم: يسقطان، فتعود البطاقة «لم يُجرَّب». وهذا أسلم من عرض كلمة لا معنى لها.
 */
internal fun labOutcomes(records: Map<String, String>): Map<LabTestId, LabOutcome> {
    val byName = LabTestId.entries.associateBy { it.name }
    val out = LinkedHashMap<LabTestId, LabOutcome>(records.size)
    records.forEach { (testId, outcome) ->
        val test = byName[testId] ?: return@forEach
        val verdict = LabOutcome.entries.firstOrNull { it.name == outcome } ?: return@forEach
        out[test] = verdict
    }
    return out
}

/**
 * **ملخّص المختبر: كم يعمل، وكم معطوب، وكم لم يُجرَّب بعد.**
 *
 * والشاشة كانت تعرض ثلاث عشرة بطاقة، ومن يفتحها لا يعرف من نظرة واحدة هل انتهى أم بقي
 * شيء. والعدّ لا يُقاس من التطبيق: هو حصيلة أحكام المستخدم، فحسابه جمعٌ لا استنتاج.
 */
data class LabTally(val works: Int, val faulty: Int, val untried: Int)

/**
 * يجمع أحكام البطاقات — وكل اختبار في الجدول يُحتسب مرّة واحدة.
 *
 * والغائب عن الخريطة `NOT_RUN` لا صفرًا ولا خطأً: لم يُجرَّب بعد، وهذا ما يقوله العدّ.
 */
fun labTally(outcomes: Map<LabTestId, LabOutcome>): LabTally {
    var works = 0
    var faulty = 0
    var untried = 0
    LabTestId.entries.forEach { test ->
        when (outcomes[test] ?: LabOutcome.NOT_RUN) {
            LabOutcome.PASSED -> works++
            LabOutcome.FAILED -> faulty++
            LabOutcome.NOT_RUN -> untried++
        }
    }
    return LabTally(works = works, faulty = faulty, untried = untried)
}

/**
 * **أين تعيش أداة كل اختبار في الصفحة.**
 *
 * ثلاث بطاقات لها نافذة كاملة ([LabTestId.dialog])، والعشر الباقية أدواتها صفوفٌ في هذه
 * الصفحة نفسها (اهتزاز · نغمات · مصباح · ميكروفون · مستشعرات · بطارية · قنوات واي فاي …).
 * فمن ضغط بطاقةً ثم أراد أن يجري اختبارها لم يكن أمامه إلا أن يغلق الحوار ويبحث عنها
 * بالتمرير — وهو عطب «أضغط فلا يحدث شيء» نفسه.
 *
 * والقسم عنوانٌ لا مؤشّر رسم: الخريطة تُقاس في `DeviceLabTestsTest`، فإن نُقلت أداة من قسم
 * إلى آخر فشل الاختبار بدل أن ينزل الزرّ إلى عنوانٍ لا أداة تحته.
 */
internal enum class LabToolSection { Screen, Controls, Audio, Wifi, Cameras, Radios, Charge, Hardware }

/**
 * الاختبار ← القسم الذي فيه أداته. والغائب عن الخريطة لا يُترك بلا جواب: كل `LabTestId`
 * مذكور، والحارس يفشل إن أُضيف اختبار ولم يُسند.
 */
internal fun LabTestId.toolSection(): LabToolSection = when (this) {
    LabTestId.SCREEN, LabTestId.TOUCH -> LabToolSection.Screen
    LabTestId.BUTTONS, LabTestId.VIBRATION -> LabToolSection.Controls
    LabTestId.SPEAKER -> LabToolSection.Audio
    LabTestId.WIFI -> LabToolSection.Wifi
    LabTestId.CAMERAS -> LabToolSection.Cameras
    LabTestId.RADIOS -> LabToolSection.Radios
    LabTestId.CHARGING -> LabToolSection.Charge
    LabTestId.MICROPHONE, LabTestId.TORCH, LabTestId.SENSORS, LabTestId.BATTERY ->
        LabToolSection.Hardware
}
