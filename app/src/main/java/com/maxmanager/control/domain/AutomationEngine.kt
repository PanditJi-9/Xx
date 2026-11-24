package com.maxmanager.control.domain

data class DeviceRuntimeContext(
    val isGameForeground: Boolean,
    val activePackage: String,
    val batterySoc: Int,
    val isCharging: Boolean,
    val isBluetoothAudioConnected: Boolean
)

sealed interface AutomationCondition {
    data object GameForeground : AutomationCondition
    data object BluetoothAudioConnected : AutomationCondition
    data class BatteryBelow(val thresholdPct: Int) : AutomationCondition
    data class BatteryAtLeast(val thresholdPct: Int) : AutomationCondition
    data object ChargerConnected : AutomationCondition
    data class PackageMatches(val packageName: String) : AutomationCondition
}

enum class AutomationAction {
    ENABLE_BYPASS_CHARGE,
    DISABLE_BYPASS_CHARGE,
    FREEZE_BACKGROUND_APPS,
    UNFREEZE_BACKGROUND_APPS,
    ENABLE_LOW_LATENCY_AUDIO_QOS,
    DISABLE_PRELOAD,
    FORCE_REFRESH_60HZ,
    ENABLE_INPUT_BOOST
}

data class AutomationRule(
    val id: String,
    val name: String,
    val enabled: Boolean = true,
    val conditions: List<AutomationCondition>,
    val actions: List<AutomationAction>
)

class AutomationEngine(
    private val rules: MutableList<AutomationRule> = defaultRules().toMutableList()
) {
    fun addRule(rule: AutomationRule) {
        rules.removeAll { it.id == rule.id }
        rules += rule
    }

    fun evaluate(ctx: DeviceRuntimeContext): Set<AutomationAction> {
        val triggered = linkedSetOf<AutomationAction>()
        for (rule in rules) {
            if (!rule.enabled) continue
            if (rule.conditions.all { matches(it, ctx) }) {
                triggered.addAll(rule.actions)
            }
        }
        return triggered
    }

    private fun matches(c: AutomationCondition, ctx: DeviceRuntimeContext): Boolean = when (c) {
        AutomationCondition.GameForeground -> ctx.isGameForeground
        AutomationCondition.BluetoothAudioConnected -> ctx.isBluetoothAudioConnected
        is AutomationCondition.BatteryBelow -> ctx.batterySoc < c.thresholdPct
        is AutomationCondition.BatteryAtLeast -> ctx.batterySoc >= c.thresholdPct
        AutomationCondition.ChargerConnected -> ctx.isCharging
        is AutomationCondition.PackageMatches -> ctx.activePackage == c.packageName
    }

    companion object {
        fun defaultRules(): List<AutomationRule> = listOf(
            AutomationRule(
                id = "bt_game_competitive",
                name = "Competitive Gaming + Bluetooth",
                conditions = listOf(
                    AutomationCondition.GameForeground,
                    AutomationCondition.BluetoothAudioConnected
                ),
                actions = listOf(
                    AutomationAction.ENABLE_LOW_LATENCY_AUDIO_QOS,
                    AutomationAction.FREEZE_BACKGROUND_APPS,
                    AutomationAction.ENABLE_INPUT_BOOST
                )
            ),
            AutomationRule(
                id = "bypass_above_60",
                name = "Smart Bypass Above 60%",
                conditions = listOf(
                    AutomationCondition.GameForeground,
                    AutomationCondition.ChargerConnected,
                    AutomationCondition.BatteryAtLeast(60)
                ),
                actions = listOf(AutomationAction.ENABLE_BYPASS_CHARGE)
            ),
            AutomationRule(
                id = "low_battery_guard",
                name = "Emergency Battery Guard (<15%)",
                conditions = listOf(AutomationCondition.BatteryBelow(15)),
                actions = listOf(
                    AutomationAction.DISABLE_PRELOAD,
                    AutomationAction.DISABLE_BYPASS_CHARGE,
                    AutomationAction.FORCE_REFRESH_60HZ
                )
            )
        )
    }
}
