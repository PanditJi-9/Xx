/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.core.daemon

import nd.max.MaxManagerPaths
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * وضع الروم (حزمة المطوّرين): الثنائيّ في `/system/bin` والـAPK في `/product/priv-app` ولا مجلّد موديول.
 * العطب المقيس: «service binary missing at /data/adb/modules/…» ⇒ الشارة `IDLE` بعد منح الجذر.
 * والاختبارات خالصة (`exists` مُمرَّر) — حكم الجهاز نفسه **يحتاج جهازًا**.
 */
class RomModeTest {

    private val romBin = "/system/bin/sys.maxmanager-service"
    private val romApk = "/product/priv-app/MaxManager/MaxManager.apk"

    @Test
    fun `module path is kept when no ROM daemon exists`() {
        assertEquals(MaxManagerPaths.SERVICE_BIN, MaxManagerPaths.serviceBin { false })
        assertFalse(MaxManagerPaths.isRomInstall { false })
    }

    @Test
    fun `ROM daemon wins when it exists`() {
        assertEquals(romBin, MaxManagerPaths.serviceBin { it == romBin })
        assertTrue(MaxManagerPaths.isRomInstall { it == romBin })
    }

    @Test
    fun `companion APK falls back to the priv-app copy`() {
        assertEquals(romApk, MaxManagerPaths.companionApk { it == romApk })
        assertEquals(MaxManagerPaths.MODULE_APK, MaxManagerPaths.companionApk { false })
        assertEquals(MaxManagerPaths.MODULE_APK, MaxManagerPaths.companionApk { it == MaxManagerPaths.MODULE_APK })
    }

    @Test
    fun `ROM restart does not use the module-only rerun`() {
        assertEquals("${MaxManagerPaths.SERVICE_BIN} --rerun", DaemonCommands.restart(MaxManagerPaths.SERVICE_BIN))
        val rom = DaemonCommands.restart(romBin)
        assertTrue(rom.contains("$romBin --run"))
        assertFalse(rom.contains("--rerun"))
        // `pkill -f` would match the shell running this very command line.
        assertFalse(rom.contains("pkill"))
    }

    @Test
    fun `ROM blockReason ignores module markers and module prop`() {
        val io = object : DaemonIo {
            override fun shell(command: String) = ShellOutcome(0, "", "")
            override fun readText(path: String): String? = null // no module.prop in a ROM
            override fun exists(path: String) = path == romBin
            override fun sleep(ms: Long) = Unit
            override fun uptimeMs() = 0L
        }
        assertNull(DaemonSupervisor(io = io, serviceBin = romBin).blockReason())
    }

    @Test
    fun `ROM blockReason still reports a missing binary`() {
        val io = object : DaemonIo {
            override fun shell(command: String) = ShellOutcome(0, "", "")
            override fun readText(path: String): String? = null
            override fun exists(path: String) = false
            override fun sleep(ms: Long) = Unit
            override fun uptimeMs() = 0L
        }
        assertEquals(BlockReason.BinaryMissing(romBin), DaemonSupervisor(io = io, serviceBin = romBin).blockReason())
    }

    @Test
    fun `RomRuntime recognises module versus ROM binaries`() {
        assertFalse(RomRuntime.isRom(MaxManagerPaths.SERVICE_BIN))
        assertTrue(RomRuntime.isRom(romBin))
        assertTrue(RomRuntime.PREPARE.contains(MaxManagerPaths.APPLIST_JSON))
        // never overwrites an existing game list
        assertTrue(RomRuntime.PREPARE.contains("[ -s '${MaxManagerPaths.APPLIST_JSON}' ]"))
    }
}
