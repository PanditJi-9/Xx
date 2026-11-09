/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.util

import nd.max.core.hardware.HardwareControlKey
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GovernorPresetValidationTest {
    private val key = HardwareControlKey.governorTunable("policy0", "schedutil", "up_rate_limit_us")

    @Test
    fun validGovernorSnapshotIsAcceptedWithoutInventedSource() {
        assertTrue(ProfilePresetStore.validGovernorSnapshot(mapOf(key to "2000")))
    }

    @Test
    fun emptyOrUnboundedSnapshotsAreRejected() {
        assertFalse(ProfilePresetStore.validGovernorSnapshot(emptyMap()))
        val values = (0..128).associate { HardwareControlKey.governorTunable("policy$it", "schedutil", "rate_limit_us") to "2000" }
        assertFalse(ProfilePresetStore.validGovernorSnapshot(values))
    }

    @Test
    fun rawPathsAndOtherFamiliesCannotBeRestoredAsGovernorPresets() {
        assertFalse(ProfilePresetStore.validGovernorSnapshot(mapOf("/sys/evil" to "1")))
        assertFalse(ProfilePresetStore.validGovernorSnapshot(mapOf(HardwareControlKey.storageQueue("sda", "scheduler") to "1")))
    }

    @Test
    fun unsafeNumericFormsAreRejected() {
        listOf("", "1;id", "+1", "-1", "1.5", "abc", "99999999999999999999").forEach { value ->
            assertFalse(value, ProfilePresetStore.validGovernorSnapshot(mapOf(key to value)))
        }
    }
}
