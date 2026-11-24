package com.maxmanager.control

import com.maxmanager.control.data.CloudProfileRepository
import com.maxmanager.control.data.CloudProfileVerifier
import com.maxmanager.control.data.GameSessionBenchmark
import com.maxmanager.control.data.SessionBenchmarkStore
import com.maxmanager.control.data.SignedCloudProfile
import com.maxmanager.control.domain.AutomationAction
import com.maxmanager.control.domain.AutomationEngine
import com.maxmanager.control.domain.DeviceRuntimeContext
import com.maxmanager.control.ui.MicroHudEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MaxLegendaryUnitTest {

    @Test
    fun cloudProfile_verifiesSha256AndMatchesSoc() {
        val payload = """{"package":"com.tencent.ig","refresh_rate_hz":120}"""
        val sig = CloudProfileVerifier.sha256Hex(payload)
        val repo = CloudProfileRepository()

        val valid = SignedCloudProfile("p1", "mt6899", "com.tencent.ig", "DragonX", payload, sig)
        val tampered = valid.copy(id = "p2", payloadJson = """{"package":"com.tencent.ig","refresh_rate_hz":60}""")

        assertTrue(repo.addVerified(valid))
        assertFalse(repo.addVerified(tampered))
        assertEquals(1, repo.findBestForDevice("mt6899", "com.tencent.ig").size)
    }

    @Test
    fun microHud_parsesWattsAndComputesJitter() {
        val hud = MicroHudEngine()
        hud.recordPingSample(42)
        hud.recordPingSample(50)
        val raw = """{"power_mw":4850,"battery_soc":74,"battery_temp_dc":382,"ram_avail_mb":3200,"pinned_threads":4,"frozen_apps":18,"qos_active":true}"""
        val frame = hud.parseDaemonHudJson(raw, currentPingMs = 46, bypassActive = true)

        assertEquals(4.85, frame.watts, 0.01)
        assertEquals(38.2, frame.tempCelsius, 0.01)
        assertTrue(frame.jitterMs > 0.0)
        assertTrue(hud.formatCompactStrip(frame).contains("BYPASS"))
    }

    @Test
    fun automationEngine_triggersExpectedActions() {
        val engine = AutomationEngine()
        val actions = engine.evaluate(
            DeviceRuntimeContext(
                isGameForeground = true,
                activePackage = "com.tencent.ig",
                batterySoc = 75,
                isCharging = true,
                isBluetoothAudioConnected = true
            )
        )
        assertTrue(actions.contains(AutomationAction.ENABLE_LOW_LATENCY_AUDIO_QOS))
        assertTrue(actions.contains(AutomationAction.FREEZE_BACKGROUND_APPS))
        assertTrue(actions.contains(AutomationAction.ENABLE_BYPASS_CHARGE))

        val lowBatActions = engine.evaluate(
            DeviceRuntimeContext(
                isGameForeground = true,
                activePackage = "com.tencent.ig",
                batterySoc = 12,
                isCharging = false,
                isBluetoothAudioConnected = false
            )
        )
        assertTrue(lowBatActions.contains(AutomationAction.DISABLE_PRELOAD))
        assertTrue(lowBatActions.contains(AutomationAction.FORCE_REFRESH_60HZ))
    }

    @Test
    fun sessionBenchmark_comparesTwoSessionsAccurately() {
        val store = SessionBenchmarkStore()
        store.record(GameSessionBenchmark("s1", "com.tencent.ig", "Stock", 1800, 5.4, 6.8, 1900, 9.5, 0.0))
        store.record(GameSessionBenchmark("s2", "com.tencent.ig", "Legendary", 1800, 4.6, 5.9, 2400, 3.2, 85.0))

        val diff = store.compare("s1", "s2")
        assertNotNull(diff)
        assertTrue(diff!!.wattsDelta < 0.0)
        assertTrue(diff.jitterDeltaMs < 0.0)
        assertEquals(500, diff.minRamDeltaMb)
        assertTrue(diff.summaryVerdict.contains("lower power"))
        assertTrue(store.exportJson().contains("Legendary"))
    }
}
