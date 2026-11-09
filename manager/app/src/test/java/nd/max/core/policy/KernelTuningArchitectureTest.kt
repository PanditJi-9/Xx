/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.policy

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KernelTuningArchitectureTest {
    private fun root(): File {
        var directory: File? = File(System.getProperty("user.dir")).canonicalFile
        repeat(8) {
            val here = directory ?: return@repeat
            if (File(here, "tools/kt_balance.py").isFile) return here
            directory = here.parentFile
        }
        error("Repository root not found")
    }

    private fun source(path: String) = File(root(), "manager/app/src/main/java/nd/max/$path").readText()

    @Test
    fun eachManualDomainKeepsItsRecordedConsentGate() {
        for ((file, id) in listOf("StorageTuningSection" to "STORAGE_TUNING", "GovernorTuningSection" to "GOVERNOR_TUNING", "HapticTuningSection" to "HAPTIC_TUNING", "PanelColorTuningSection" to "PANEL_COLOR_TUNING")) {
            val section = source("ui/subscreens/$file.kt")
            assertTrue(section.contains("id = DangerConsent.$id"))
            assertTrue(section.contains("viewModel.prepare(control, value)"))
            assertFalse(section.contains("RootFileAccess.write"))
        }
    }

    @Test
    fun injectedModelsAreRequestedWithHiltAndOnlyExistingScreensHostControls() {
        val screen = source("ui/subscreens/DisplayStudioScreen.kt")
        assertTrue(screen.contains("DisplayStudioViewModel = hiltViewModel()"))
        assertTrue(screen.contains("PanelColorTuningSection()"))
        assertTrue(source("ui/subscreens/GovSettingsScreen.kt").contains("StorageTuningSection()"))
        assertTrue(source("ui/subscreens/GovSettingsScreen.kt").contains("GovernorTuningSection()"))
        assertTrue(source("ui/subscreens/DeviceLabScreen.kt").contains("HapticTuningSection()"))
    }

    @Test
    fun globalIoChoiceIsReplacedNotDuplicated() {
        val screen = source("ui/subscreens/GovSettingsScreen.kt")
        val viewModel = source("ui/viewmodel/TweakViewmodel.kt")
        assertFalse(screen.contains("updateIoScheduler"))
        assertFalse(viewModel.contains("fun updateIoScheduler"))
        assertFalse(viewModel.contains("loadIOSchedulersInternal"))
    }

    @Test
    fun sunlightUsesActualReadbackAndDoesNotEnableItDuringScreenLoad() {
        val viewModel = source("ui/viewmodel/DisplayStudioViewModel.kt")
        val tile = source("TileService/SunlightTileService.kt")
        assertFalse(viewModel.contains("if (sunlightEnabled) apply(sunlightNode, true)"))
        assertTrue(viewModel.contains("sunlightEnabled = sunlightNode?.let(DisplayFeatureNodes::isEnabled)"))
        assertTrue(tile.contains("DisplayFeatureNodes.writeGuarded"))
        assertTrue(tile.contains("DangerConsent.PANEL_SUNLIGHT"))
        assertTrue(tile.contains("withContext(Dispatchers.IO)"))
        assertTrue(tile.contains("scope.cancel()"))
    }

    @Test
    fun governorPresetsExtendTheExistingStoreWithoutAutomaticPerAppWriter() {
        val store = source("ui/util/ProfilePresetStore.kt")
        val viewModel = source("ui/viewmodel/KernelTuningViewModel.kt")
        assertTrue(store.contains("putString(\"governor.\$profile\""))
        assertTrue(viewModel.contains("ProfilePresetStore.setGovernorSnapshot"))
        assertTrue(viewModel.contains("ProfilePresetStore.governorSnapshot"))
        assertTrue(viewModel.contains("DangerConsent.isAcknowledged(context, consent)"))
        assertFalse(viewModel.contains("delay("))
        assertFalse(viewModel.contains("HardwareControlArbiter()"))
    }
}
