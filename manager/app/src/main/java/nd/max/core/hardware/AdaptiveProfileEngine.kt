/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.hardware

/** Vendor-neutral adaptive policy. It only decides a profile; a caller owns application. */
class AdaptiveProfileEngine(
    private val coolThresholdC: Float = 38f,
    private val warmThresholdC: Float = 44f,
    private val hotThresholdC: Float = 48f,
    private val hysteresisC: Float = 1.5f,
) {
    enum class Profile { POWER, BALANCED, GAMING, PERFORMANCE }
    data class Input(val temperatureC: Float?, val batteryPct: Int?, val charging: Boolean, val loadPct: Int?)
    data class Decision(val profile: Profile, val reason: String, val changed: Boolean)

    private var last: Profile? = null

    @Synchronized
    fun decide(input: Input): Decision {
        val t = input.temperatureC
        val battery = input.batteryPct
        val load = input.loadPct
        // الحكم والسبب يُقرَّران في `when` واحد عن قصد: كانا سلسلتين متوازيتين، فانزاحا —
        // الفرع الأعلى (`PERFORMANCE`) جاء بعد `GAMING` فصار **غير قابل للبلوغ** لأن شرطه
        // أضيق، بينما وصفتُه في السبب تبقى "high-load" على بروفايل `GAMING`. سلسلة واحدة
        // تُقرن فيها كل حالة بسببها تجعل هذا الانزياح مستحيلًا بنيويًّا.
        //
        // والترتيب: الأضيق قبل الأوسع. `PERFORMANCE` (حمل ≥ ٨٠ مع شحن أو بطارية كافية)
        // أضيق من `GAMING` (حمل ≥ ٥٥)، فلو تأخّر لالتهمه `GAMING` كما كان.
        val (next, reason) = when {
            t != null && t >= hotThresholdC -> Profile.POWER to "temperature-hot"
            battery != null && battery <= CRITICAL_BATTERY && !input.charging ->
                Profile.POWER to "battery-critical"
            t != null && t >= warmThresholdC -> Profile.BALANCED to "temperature-warm"
            load != null && load >= PERFORMANCE_LOAD &&
                (input.charging || (battery ?: 100) >= MIN_BATTERY) ->
                Profile.PERFORMANCE to "high-load"
            load != null && load >= GAMING_LOAD -> Profile.GAMING to "medium-load"
            t != null && t <= coolThresholdC - hysteresisC && load != null && load < COOL_LOAD ->
                Profile.BALANCED to "stable"
            else -> (last ?: Profile.BALANCED) to "stable"
        }
        val changed = next != last
        last = next
        return Decision(next, reason, changed)
    }

    @Synchronized fun reset() { last = null }

    private companion object {
        /** حملٌ يبلغه اللعب وحده؛ أعلى من [GAMING_LOAD] فشرطُه أضيق. */
        const val PERFORMANCE_LOAD = 80
        const val GAMING_LOAD = 55
        /** تحت هذا الحمل مع برودة كافية يعود التوازن صريحًا. */
        const val COOL_LOAD = 45
        const val MIN_BATTERY = 30
        const val CRITICAL_BATTERY = 12
    }
}
