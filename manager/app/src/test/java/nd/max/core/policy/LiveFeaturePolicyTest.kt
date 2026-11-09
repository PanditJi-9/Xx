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

/** ADR-93: الغائب لا يُوصف في بطاقة؛ التحذير يرافق فعلًا حقيقيًّا على مسار الإقرار. */
class LiveFeaturePolicyTest {
    private fun repoRoot(): File {
        var directory: File? = File(System.getProperty("user.dir")).canonicalFile
        repeat(8) {
            val candidate = directory ?: return@repeat
            if (File(candidate, "tools/kt_balance.py").isFile) return candidate
            directory = candidate.parentFile
        }
        throw AssertionError("Could not locate the repository root")
    }

    private fun source(relative: String): String = File(repoRoot(), relative).readText()

    @Test
    fun absentFeatureCardsAreNotReachable() {
        val diagnostics = source("manager/app/src/main/java/nd/max/ui/mainscreens/DiagnosticsScreen.kt")
        val removed = listOf("ExcludedCapabilitiesCard", "MaxBlockedFeatureCard")
        removed.forEach { name -> assertFalse("Retired card: $name", diagnostics.contains(name)) }
        assertFalse(File(repoRoot(), "manager/app/src/main/java/nd/max/ui/component/ExcludedCapabilitiesCard.kt").exists())
        assertFalse(File(repoRoot(), "manager/app/src/main/java/nd/max/ui/design/MaxBlockedFeature.kt").exists())
    }

    @Test
    fun absentFeatureDescriptionsAreRemovedFromShippedLocales() {
        val keys = Regex("""name="(?:blocked_(?:voltage|lmk|prop)_[^"]*|excluded_[^"]*|diagnostics_(?:excluded_[^"]*|bootimage_[^"]*))"""")
        val resources = File(repoRoot(), "manager/app/src/main/res")
        resources.listFiles().orEmpty().filter { it.isDirectory && it.name.startsWith("values") }.forEach { folder ->
            val strings = File(folder, "strings.xml")
            if (strings.isFile) assertFalse("Retired description in ${folder.name}", keys.containsMatchIn(strings.readText()))
        }
    }

    @Test
    fun lmkActionsKeepTheConsentGateAndKnownBaseline() {
        val section = source("manager/app/src/main/java/nd/max/ui/subscreens/LmkSection.kt")
        assertTrue(section.contains("id = DangerConsent.MEMORY_LMK"))
        assertTrue(section.contains("io.apply(target, expected)"))
        assertTrue(section.contains("pendingAction.set(target, current)"))
        assertTrue(section.contains("pendingAction.take()"))
        assertTrue(section.contains("opened?.let { LmkTuning.scale(it, 75) }"))
        assertTrue(section.contains("opened?.let { LmkTuning.scale(it, 125) }"))
        assertTrue(section.contains("if (current == null && outcome == null) return"))
    }

    @Test
    fun lmkUsesTheInjectedArbiterAndTheExistingCatalogIdentity() {
        val backend = source("manager/app/src/main/java/nd/max/core/platform/LmkTuning.kt")
        val viewModel = source("manager/app/src/main/java/nd/max/ui/viewmodel/MemoryUfsViewModel.kt")
        assertTrue(backend.contains("arbiter.submit("))
        assertTrue(backend.contains("HardwareControlKey.MEMORY_LMK_MINFREE"))
        assertTrue(backend.contains("arbiter.release(key, token, restore = false)"))
        assertTrue(viewModel.contains("LmkTuning.Io.system(arbiter)"))
        assertFalse(backend.contains("HardwareControlArbiter()"))
    }
}
