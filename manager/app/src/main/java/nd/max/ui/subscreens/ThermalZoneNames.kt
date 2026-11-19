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
/*
 * **اسم المنطقة الحرارية: معنى المستخدم، لا اسم الشريحة.**
 *
 * ولماذا هنا لا في `ThermalUtil`: التصنيف (المجموعات وعناوين الأقسام) سلطة
 * `ThermalUtil.classifyZone` وحدها، وهذا الملف **لا يصنّف** ولا يملك حالة: يترجم نوعًا
 * خامًّا إلى الكلمة التي يفهمها المستخدم، ويعيد `null` لما لا معنى مُثبت له.
 *
 * ولماذا يُقرأ `type` مرة أخرى مع أن `ThermalZoneInfo.label` منه: `label` يمرّ في
 * `prettifyName` (شرطات سفلية تُصبح مسافات، و`tsens` تُصبح «Temperature Sensor»،
 * و`tz` تُصبح «Zone»)، فالعائلة الخامّ لم تبقَ قابلة للاسترجاع من النصّ المُهذّب.
 * فالقراءة هنا **لعقدة النواة نفسها التي يقرؤها المصنّف** — لا لمنطق تصنيف ثانٍ —
 * دفعةً واحدة عند فتح الشاشة، ونتيجتها تُحفظ في حالة الشاشة نفسها (ADR-18: لا مالك
 * ثانٍ للحالة).
 *
 * و`null` ليست نقصًا يُخفى: `ADR-07` يمنع تسمية مُختلقة، فما لا معنى له يُعرض بمعرّفه
 * الخام كما هو مع وسم «معرّف المُصنّع».
 */
package nd.max.ui.subscreens

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import java.io.File
import nd.max.R
import nd.max.core.hardware.RootFileAccess
import nd.max.core.platform.ThermalZoneInfo

/**
 * أنواع المناطق التي قرأها النواة، مفتاحها مسار المنطقة — تُقرأ **مرة** عند فتح الشاشة.
 *
 * **لا تُستدعى من الخيط الرئيسيّ:** [RootFileAccess.readMany] قد تسأل قناة الجذر حين
 * تحجب النواة العقدة عن `uid` التطبيق. والدفعة الواحدة مقصودة: ٦٦ منطقة في معاملة واحدة
 * لا ٦٦ رحلة.
 *
 * وما لم يُقرأ يغيب من الخريطة (لا سلسلة فارغة): الشاشة تميّز «نوع غير مقروء» من
 * «نوع مقروء بلا معنى معروف»، وكلاهما يُعرض بمعرّفه لا باسم مُخترع.
 */
internal fun readThermalZoneTypes(zones: List<ThermalZoneInfo>): Map<String, String> {
    if (zones.isEmpty()) return emptyMap()
    val paths = zones.map { File(it.sysfsPath, "type").absolutePath }
    val values = RootFileAccess.readMany(paths)
    return buildMap {
        zones.forEachIndexed { index, zone ->
            values.getOrNull(index)?.trim()?.takeIf { it.isNotEmpty() }?.let { type ->
                put(zone.sysfsPath, type)
            }
        }
    }
}

/**
 * معاني المناطق التي قِيست على جهاز فعليّ (`type` في `/sys/class/thermal`).
 *
 * والاسم معناه للمستخدم لا اسم الشريحة: `pmic6363-vio18` ليست «Pmic6363 Vio18» بل
 * «إدارة الطاقة»، و`quiet_therm` ليست «Quiet Therm» بل «خافض الحرارة».
 */
private enum class ThermalZoneKind {
    Processor,
    Graphics,
    Battery,
    Charger,
    PowerManagement,
    Mitigation,
    Skin,
    Modem,
    Wifi,
    Display,
    Port,
    Flash,
    Amplifier,
}

/**
 * العائلة من النوع الخامّ: أوّل مقطع حرفيّ منه.
 *
 * `pmic6363-vio18` ← `pmic` · `gpuss-0` ← `gpuss` · `quiet_therm` ← `quiet` ·
 * `soc-thermal` ← `soc` · `tsens_tz_sensor0` ← `tsens`.
 *
 * والمطابقة على العائلة لا على النصّ الكامل لأن النواة تُلحق بالاسم لاحقةً أو رقمًا
 * (`cpu0`, `battery`, `usb_therm`)؛ ولا تُطابق عائلةً غير هذه القائمة، فما لم يُقَس
 * يبقى بمعرّفه (ADR-07).
 */
private fun thermalZoneKind(raw: String?): ThermalZoneKind? {
    val token = raw?.trim()?.lowercase().orEmpty()
    if (token.isEmpty()) return null
    val family = token.takeWhile { it.isLetter() }
    return when (family) {
        // AP = application processor، وtsens = مستشعر الحرارة في SoC نفسها.
        "cpu", "ap", "tsens", "soc" -> ThermalZoneKind.Processor
        "gpu", "gpuss" -> ThermalZoneKind.Graphics
        "battery" -> ThermalZoneKind.Battery
        "charger" -> ThermalZoneKind.Charger
        // pmic + رقم/لاحقة يشمل `pmic` و`pmic6363-vio18` معًا.
        "pmic" -> ThermalZoneKind.PowerManagement
        // quiet_therm / quiet-therm: المستشعر الذي يقرؤه محرّك التخفيف ليقرّر الخنق.
        "quiet" -> ThermalZoneKind.Mitigation
        "skin" -> ThermalZoneKind.Skin
        "modem" -> ThermalZoneKind.Modem
        "wifi" -> ThermalZoneKind.Wifi
        "display" -> ThermalZoneKind.Display
        "usb" -> ThermalZoneKind.Port
        "flash" -> ThermalZoneKind.Flash
        "pa" -> ThermalZoneKind.Amplifier
        else -> null
    }
}

/**
 * الاسم العربيّ/الإنجليزيّ لمعنى المنطقة — أو `null` حين لا معنى مُثبت.
 *
 * `null` ليست فشلًا: المستدعي يعرض المعرّف الخام (أو تصنيف النواة القائم) ولا يخترع.
 */
@Composable
internal fun thermalZoneDisplayName(raw: String?): String? = when (thermalZoneKind(raw)) {
    ThermalZoneKind.Processor -> stringResource(R.string.detail_zone_cpu)
    ThermalZoneKind.Graphics -> stringResource(R.string.detail_zone_gpu)
    ThermalZoneKind.Battery -> stringResource(R.string.detail_zone_battery)
    ThermalZoneKind.Charger -> stringResource(R.string.detail_zone_charger)
    ThermalZoneKind.PowerManagement -> stringResource(R.string.detail_zone_pmic)
    ThermalZoneKind.Mitigation -> stringResource(R.string.detail_zone_mitigation)
    ThermalZoneKind.Skin -> stringResource(R.string.detail_zone_skin)
    ThermalZoneKind.Modem -> stringResource(R.string.detail_zone_modem)
    ThermalZoneKind.Wifi -> stringResource(R.string.detail_zone_wifi)
    ThermalZoneKind.Display -> stringResource(R.string.detail_zone_display)
    ThermalZoneKind.Port -> stringResource(R.string.detail_zone_usb)
    ThermalZoneKind.Flash -> stringResource(R.string.detail_zone_flash)
    ThermalZoneKind.Amplifier -> stringResource(R.string.detail_zone_pa)
    null -> null
}
