/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential. See LICENSE at the repository root.
 */
package nd.max.core.daemon

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Source wiring guards, not Android execution or a measurement of device power. */
class FootprintContractTest {
    private fun source(path: String): String {
        val candidates = listOf("src/main/java/nd/max", "app/src/main/java/nd/max", "manager/app/src/main/java/nd/max")
        val root = candidates.map(::File).firstOrNull { it.isDirectory }
            ?: error("MaxManager source tree is required; this test must not silently skip")
        return File(root, path).readText().replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("(?m)^\\s*//.*$"), "")
    }

    @Test
    fun `file observer callbacks signal rather than perform privileged reads`() {
        val root = source("ui/util/RootUtil.kt")
        val callback = root.substringAfter("override fun onEvent").substringBefore("observer.startWatching()")
        assertTrue(callback.contains("trySend(Unit)"))
        for (blocked in listOf("readRootFile(", "RootFileAccess.", "getCurrentProfileRes(")) {
            assertFalse("FileObserver callback must not block on $blocked", callback.contains(blocked))
        }
        assertTrue(root.contains("buffer(Channel.CONFLATED)"))
        assertTrue(root.contains("distinctUntilChanged()"))
    }

    @Test
    fun `watchers read completed writes and register before the initial read signal`() {
        val root = source("ui/util/RootUtil.kt")
        val watcher = root.substringAfter("private fun observeApiFileChanges")
        assertTrue(watcher.contains("FileObserver.CLOSE_WRITE"))
        assertTrue(watcher.contains("FileObserver.MOVED_TO"))
        assertTrue(watcher.contains("FileObserver.DELETE_SELF"))
        assertFalse(watcher.contains("FileObserver.MODIFY"))
        val registration = watcher.indexOf("observer.startWatching()")
        val initialSignal = watcher.indexOf("trySend(Unit)", registration)
        assertTrue(registration >= 0 && initialSignal > registration)
    }

    @Test
    fun `a lost watch has a bounded fallback and cancellation cleans it up`() {
        val root = source("ui/util/RootUtil.kt")
        assertTrue(root.contains("API_WATCH_FALLBACK_MS = 60_000L"))
        val fallback = root.substringAfter("val fallback = launch").substringBefore("awaitClose")
        assertTrue(fallback.contains("delay(API_WATCH_FALLBACK_MS)"))
        assertTrue(fallback.contains("observer.stopWatching()"))
        assertTrue(fallback.contains("observer.startWatching()"))
        val cleanup = root.substringAfter("awaitClose {").substringBefore("fun observeProfileRes")
        assertTrue(cleanup.contains("fallback.cancel()"))
        assertTrue(cleanup.contains("observer.stopWatching()"))
    }

    @Test
    fun `all home subscriptions are gated and state updates do not overwrite each other`() {
        val home = source("ui/viewmodel/HomeViewmodel.kt")
        assertTrue(home.contains("MutableStateFlow(false)"))
        assertTrue(Regex("visible\\.collectLatest").findAll(home).count() == 3)
        assertTrue(Regex("if \\(!on\\) return@collectLatest").findAll(home).count() == 3)
        assertFalse(home.contains("_uiState.value ="))
        assertTrue(source("ui/mainscreens/HomeScreen.kt").contains("homeViewModel.setVisible(false)"))
    }

    @Test
    fun `a new config wake cannot be discarded after poll`() {
        val monitor = source("AppMonitor.kt")
        val wait = monitor.substringAfter("private fun awaitNextMonitorCycle()").substringBefore("private fun currentPollIntervalMs")
        assertTrue(wait.contains("monitorWake.poll("))
        assertFalse(wait.contains("monitorWake.clear()"))
        assertTrue(monitor.contains("configObserver?.stopWatching()"))
    }

    @Test
    fun `status writes keep safety live and only cache a successful rename`() {
        val monitor = source("AppMonitor.kt")
        assertTrue(monitor.contains("requiresLiveStatus = charging == 1 || perAppOverridesActive || gracePkg != null"))
        val status = monitor.substringAfter("private fun writeStatus()").substringBefore("private fun getRecentAppPackages")
        assertTrue(status.contains("check(tmpFile.renameTo(file))"))
        assertTrue(status.indexOf("check(tmpFile.renameTo(file))") < status.indexOf("lastStatus = currentStatus"))
        val background = monitor.substringAfter("private fun writeBackgroundApps()").substringBefore("private fun waitForValidFocusedApp")
        assertTrue(background.contains("check(tmpFile.renameTo(file))"))
        assertTrue(background.indexOf("check(tmpFile.renameTo(file))") < background.indexOf("lastBackgroundApps = currentApps"))
        assertFalse(status.contains("fos.fd.sync()"))
        assertFalse(background.contains("fos.fd.sync()"))
    }

    @Test
    fun `island geometry wakes on layout preferences and rotation without caching before attach`() {
        val island = source("service/IslandService.kt")
        assertTrue(island.contains("addOnLayoutChangeListener(layoutListener)"))
        assertTrue(island.contains("removeOnLayoutChangeListener(layoutListener)"))
        assertTrue(island.contains("IslandLive.config.collect"))
        assertTrue(island.contains("override fun onConfigurationChanged"))
        assertTrue(island.contains("!host.isAttachedToWindow"))
    }
}
