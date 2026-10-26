/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.core.gamespace

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

/**
 * حلقات التوصيل التي لا يراها المُصرِّف: المطبِّق يدخل ويخرج من الخدمة ويستردّ عند الإقلاع والإيقاف،
 * ووثيقة البروفايلات تتبع الوضع، ولا يُطبَّق شيء من مسارين معًا.
 */
class InAppProfileWiringTest {
    private lateinit var sourceRoot: File

    @Before
    fun locate() {
        val found = listOf(
            File("src/main/java/nd/max"), File("app/src/main/java/nd/max"),
            File("../app/src/main/java/nd/max"), File("manager/app/src/main/java/nd/max")
        ).firstOrNull { it.isDirectory }
        assumeTrue("Cannot locate nd.max sources", found != null)
        sourceRoot = found!!
    }

    private fun text(relative: String) = File(sourceRoot, relative).readText()

    @Test
    fun `the panel service syncs recovers and releases the in-app profile`() {
        val service = text("service/GamePanelService.kt")
        assertTrue(service.contains("InAppProfileApplier.recover(applicationContext)"))
        assertTrue(service.contains("InAppProfileApplier.sync(applicationContext, trackedPkg)"))
        assertTrue(service.contains("InAppProfileApplier.release(appContext)"))
    }

    @Test
    fun `the applier yields to the root companion and saves a snapshot before the first write`() {
        val applier = text("core/gamespace/InAppProfileApplier.kt")
        assertTrue(applier.contains("InAppProfileRules.isActive("))
        val saveAt = applier.indexOf("putString(KEY_SAVED")
        val writeAt = applier.indexOf("InAppProfileRules.putCommand(write.ref, write.value)")
        assertTrue("snapshot must be persisted", saveAt > 0)
        assertTrue("the snapshot must be committed before any write", saveAt in 1 until writeAt)
        assertTrue("commit (not apply) so it survives process death", applier.contains(".commit()"))
        assertTrue(
            "a failed restore must never be followed by a fresh apply",
            applier.contains("if (!restoreLeftovers(context)) return false")
        )
    }

    @Test
    fun `the profile document follows the mode and never asks root in the non-root modes`() {
        val repo = text("core/gamespace/GameProfileRepository.kt")
        assertTrue(repo.contains("private fun usesRootDocument()"))
        assertTrue(repo.contains("ExecutionMode.SHIZUKU, ExecutionMode.BASIC -> false"))
        assertTrue("readDocument must go through the mode-aware io()", repo.contains("GameProfileDocument.parse(io().read()"))
        assertTrue("no direct root read may bypass the mode", !repo.contains("GameProfileDocument.parse(RootFileAccess.read(path)"))
        val app = text("MaxManagerApplication.kt")
        val install = app.indexOf("GameProfileRepository.install(this)")
        val load = app.indexOf("ExecutionModeStore.load(this)")
        assertTrue(install in 0 until load)
    }
}
