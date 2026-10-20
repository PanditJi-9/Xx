/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.hardware

/**
 * نافذة تردّد الرسّام الواحدة، بمصدرين لا يُخلطان:
 *
 * - **الحدّان الديناميكيان** (`min_freq` / `max_freq`): ما **يُسمح** به الرسّام الآن. يتغيّران بالسياسة
 *   أو بضبط المستخدم، فلا يُخزَّنان من قراءة الإقلاع.
 * - **قدرة العتاد** (أول وآخر سلّم `available_frequencies`): ما **يستطيعه** الرسّام، لا ما يُسمح به.
 *
 * كل حقل `null` حين لا يُقرأ بوحدة موثوقة، فلا يُخترع رقم.
 */
data class GpuFrequencyWindow(
    val dynamicMinMhz: Int?,
    val dynamicMaxMhz: Int?,
    val capabilityMinMhz: Int?,
    val capabilityMaxMhz: Int?,
) {
    companion object {
        /** نافذة الجهاز في هذه اللحظة من لقطته الحيّة. */
        fun of(device: GpuHardwareBackend.Device): GpuFrequencyWindow = GpuFrequencyWindow(
            dynamicMinMhz = GpuHardwareBackend.frequencyMHz(device, device.minFreq)?.toInt(),
            dynamicMaxMhz = GpuHardwareBackend.frequencyMHz(device, device.maxFreq)?.toInt(),
            capabilityMinMhz = GpuHardwareBackend.frequencyMHz(device, device.provenMinFreq)?.toInt(),
            capabilityMaxMhz = GpuHardwareBackend.frequencyMHz(device, device.provenMaxFreq)?.toInt(),
        )
    }
}
