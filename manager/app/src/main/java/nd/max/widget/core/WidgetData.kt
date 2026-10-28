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
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import nd.max.core.platform.FpsMonitorUtil
import nd.max.core.platform.ThermalUtil
import java.util.Locale

/**
 * قراءات الودجة — **فوريّة كلها**: لا شبكة، ولا كتلة انتظار، ولا عملية خارجية.
 * والودجة تُرسم في عملية المشغّل عند الطلب، فكل ما يُنادى داخل [provideGlance]
 * محسوب عليه زمن، ولهذا مسموح هنا فقط ما يقرأ من كاش النظام أو ملفّ جهاز.
 *
 * والمصادر هي **مصادر التطبيق نفسه** لا قراءات موازية: الحرارة من [ThermalUtil]
 * (بتنقيحها ونطاقها الصالح)، والمعالج والذاكرة من [FpsMonitorUtil] — فلا
 * تتباعد لغة الودجة عن لغة الشاشة إن تغيّر مصدرٌ لاحقًا.
 */
object WidgetData {

    /** بطارية: النسبة والاتصال بالشاحن، من بثّ النظام اللاصق (لا مستمع دائم). */
    data class BatteryVitals(val percent: Int?, val charging: Boolean)

    /** تخزين البيانات: المتاح والإجمالي، ونسبة الاستهلاك منهما. */
    data class StorageVitals(val freeBytes: Long, val totalBytes: Long) {
        val usedPercent: Int
            get() = if (totalBytes <= 0L) 0
            else (((totalBytes - freeBytes) * 100L) / totalBytes).toInt().coerceIn(0, 100)

        val usedFraction: Float get() = usedPercent / 100f
    }

    fun battery(context: Context): BatteryVitals {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val percent = if (level >= 0 && scale > 0) level * 100 / scale else null
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        return BatteryVitals(percent = percent, charging = charging)
    }

    fun temperatureC(context: Context): Float? =
        runCatching { ThermalUtil.readBatteryTemperatureC(context) }
            .getOrNull()
            ?.takeIf { it > 0f }

    fun cpuLoad(): Int? = runCatching { FpsMonitorUtil.getCpuLoad() }.getOrNull()

    /** (المستخدم، الإجمالي) بالميغابايت. */
    fun ramMb(context: Context): Pair<Int, Int>? = runCatching {
        val info = FpsMonitorUtil.getRamInfo(context)
        info.usedMb to info.totalMb
    }.getOrNull()

    fun storage(): StorageVitals? = runCatching {
        val stat = StatFs(Environment.getDataDirectory().path)
        StorageVitals(freeBytes = stat.availableBytes, totalBytes = stat.totalBytes)
    }.getOrNull()

    fun isFpsServiceRunning(): Boolean = nd.max.service.FpsOverlayService.isRunning

    fun fpsValue(): Float = runCatching { FpsMonitorUtil.getFps() }.getOrDefault(0f)

    fun powerWatt(): Float = runCatching { FpsMonitorUtil.getPowerWatt() }.getOrDefault(0f)

    fun gpuLoad(): Int? = runCatching {
        val kgsl = java.io.File("/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage")
        if (kgsl.exists()) {
            kgsl.readText().trim().trimEnd('%').toIntOrNull()
        } else {
            val ged = java.io.File("/sys/module/ged/parameters/gpu_idle")
            if (ged.exists()) {
                val idle = ged.readText().trim().toIntOrNull()
                idle?.let { (100 - it).coerceIn(0, 100) }
            } else null
        }
    }.getOrNull()

    fun gpuFreqMhz(): Int? = runCatching {
        val kgslClock = java.io.File("/sys/class/kgsl/kgsl-3d0/gpuclk")
        if (kgslClock.exists()) {
            val hz = kgslClock.readText().trim().toLongOrNull() ?: 0L
            (hz / 1_000_000L).toInt().takeIf { it > 0 }
        } else {
            val devfreq = java.io.File("/sys/class/devfreq/13000000.mali/cur_freq")
            if (devfreq.exists()) {
                val hz = devfreq.readText().trim().toLongOrNull() ?: 0L
                (hz / 1_000_000L).toInt().takeIf { it > 0 }
            } else null
        }
    }.getOrNull()
}

/**
 * تنسيقات نقيّة — بلا `Context` وبلا جهاز، فتُختبر في JVM.
 * واللغة تُمرَّر معاملًا (افتراضها لغة الجهاز) كي يكون الاختبار حتميًّا.
 */
object WidgetFormat {

    /** «لا قياس» ليست «صفر»: الشرطة تقول «لم يُقرأ»، والصفر رقم. */
    const val DASH = "—"

    fun gbFromBytes(bytes: Long, locale: Locale = Locale.getDefault()): String =
        String.format(locale, "%.1f", bytes / 1_073_741_824.0)

    fun gbFromMb(mb: Int, locale: Locale = Locale.getDefault()): String =
        String.format(locale, "%.1f", mb / 1024.0)

    fun percentOf(value: Int?): String = if (value == null) DASH else "$value%"

    fun degreesOf(value: Float?, locale: Locale = Locale.getDefault()): String =
        if (value == null) DASH else String.format(locale, "%.0f°", value)

    fun ramPercent(usedMb: Int, totalMb: Int): Int =
        if (totalMb <= 0) 0 else (usedMb * 100 / totalMb).coerceIn(0, 100)
}
