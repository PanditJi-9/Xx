/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.core.daemon

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** قناة التطبيق ↔ الخادم: النصّ على السلك وتصنيف الردّ. السوكيت الحقيقيّ **يحتاج جهازًا**. */
class DaemonIpcTest {

    @Test
    fun `profile request is a closed whitelist`() {
        assertEquals("PROFILE 1", DaemonIpcProtocol.profileRequest("1", fromAi = false))
        assertEquals("PROFILE 3 AI", DaemonIpcProtocol.profileRequest("3", fromAi = true))
        assertNull(DaemonIpcProtocol.profileRequest("0", fromAi = false))
        assertNull(DaemonIpcProtocol.profileRequest("4", fromAi = false))
        assertNull(DaemonIpcProtocol.profileRequest("1; reboot", fromAi = false))
        assertNull(DaemonIpcProtocol.profileRequest("", fromAi = true))
    }

    @Test
    fun `replies are classified`() {
        assertEquals(DaemonIpc.Result.Ok(""), DaemonIpcProtocol.parseReply("OK\n"))
        assertEquals(DaemonIpc.Result.Ok("1"), DaemonIpcProtocol.parseReply("OK 1\n"))
        assertEquals(DaemonIpc.Result.Refused("denied"), DaemonIpcProtocol.parseReply("ERR denied\n"))
        assertEquals(DaemonIpc.Result.Refused("exit=1"), DaemonIpcProtocol.parseReply("ERR exit=1\n"))
        assertEquals(DaemonIpc.Result.Unreachable, DaemonIpcProtocol.parseReply("garbage"))
    }

    @Test
    fun `no transport means unreachable not refused`() {
        val ipc = DaemonIpc(IpcTransport { null })
        assertEquals(DaemonIpc.Result.Unreachable, ipc.profile("2", fromAi = false))
        assertFalse(ipc.ping())
    }

    @Test
    fun `an invalid profile never reaches the transport`() {
        var sent = 0
        val ipc = DaemonIpc(IpcTransport { sent++; "OK\n" })
        assertEquals(DaemonIpc.Result.Refused("invalid-profile"), ipc.profile("7", fromAi = false))
        assertEquals(0, sent)
    }

    @Test
    fun `ping and profile use the wire format the daemon parses`() {
        val seen = mutableListOf<String>()
        val ipc = DaemonIpc(IpcTransport { request -> seen += request; if (request == "PING") "OK 1\n" else "OK\n" })
        assertTrue(ipc.ping())
        assertEquals(DaemonIpc.Result.Ok(""), ipc.profile("2", fromAi = true))
        assertEquals(listOf("PING", "PROFILE 2 AI"), seen)
    }

    @Test
    fun `read follows the node allowlist and returns the trimmed body`() {
        val seen = mutableListOf<String>()
        val ipc = DaemonIpc(IpcTransport { request -> seen += request; "OK 4\n87\n\n" })
        assertEquals("87", ipc.read("/sys/class/power_supply/battery/capacity"))
        assertNull(ipc.read("/etc/passwd"))
        assertNull(ipc.read("/sys/class/power_supply/../../../etc/passwd"))
        assertEquals(listOf("READ /sys/class/power_supply/battery/capacity"), seen)
    }

    @Test
    fun `an error reply or an empty body is not a read`() {
        assertNull(DaemonIpc(IpcTransport { "ERR notfound\n" }).read("/proc/meminfo"))
        assertNull(DaemonIpc(IpcTransport { "OK 0\n" }).read("/proc/meminfo"))
    }

    @Test
    fun `exists answers from the daemon in both directions`() {
        assertEquals(true, DaemonIpc(IpcTransport { "OK 1\n" }).exists("/proc/meminfo"))
        assertEquals(false, DaemonIpc(IpcTransport { "OK 0\n" }).exists("/proc/meminfo"))
        assertNull(DaemonIpc(IpcTransport { "ERR invalid\n" }).exists("/proc/meminfo"))
        assertNull(DaemonIpc(IpcTransport { "OK 1\n" }).exists("/data/adb/x"))
    }

    @Test
    fun `setProperty only for config keys and only on a confirmed OK`() {
        val seen = mutableListOf<String>()
        val ok = DaemonIpc(IpcTransport { request -> seen += request; "OK\n" })
        assertTrue(ok.setProperty("persist.sys.maxmanagerconf.sic_boost", "1"))
        assertFalse(ok.setProperty("persist.sys.maxmanager.state", "1"))
        assertFalse(ok.setProperty("ro.debuggable", "1"))
        assertFalse(ok.setProperty("persist.sys.maxmanagerconf.a", "x y"))
        assertEquals(listOf("SETPROP persist.sys.maxmanagerconf.sic_boost 1"), seen)
        assertFalse(DaemonIpc(IpcTransport { "ERR invalid\n" }).setProperty("persist.sys.maxmanagerconf.a", "1"))
        assertFalse(DaemonIpc(IpcTransport { null }).setProperty("persist.sys.maxmanagerconf.a", "1"))
    }

    @Test
    fun `an unreachable daemon is not retried for a few seconds then is`() {
        var now = 0L
        var calls = 0
        val ipc = DaemonIpc(IpcTransport { calls++; null }, nowNanos = { now })
        assertNull(ipc.read("/proc/meminfo"))
        assertNull(ipc.read("/proc/stat"))
        assertEquals("only the first failure paid for a connection", 1, calls)
        now += 6_000_000_000L
        assertNull(ipc.read("/proc/stat"))
        assertEquals("after the backoff the daemon is tried again", 2, calls)
    }

    @Test
    fun `a success clears the backoff`() {
        var now = 0L
        var reachable = false
        val ipc = DaemonIpc(IpcTransport { if (reachable) "OK 1\n" else null }, nowNanos = { now })
        assertFalse(ipc.ping())
        reachable = true
        now += 6_000_000_000L
        assertTrue(ipc.ping())
        assertTrue(ipc.ping())
    }

    @Test
    fun `readable mirrors the daemon allowlist`() {
        assertTrue(DaemonIpcProtocol.readable("/proc/meminfo"))
        assertTrue(DaemonIpcProtocol.readable("/sys/class/thermal/thermal_zone0/temp"))
        assertFalse(DaemonIpcProtocol.readable("/sys/class/thermal/"))
        assertFalse(DaemonIpcProtocol.readable("/sys/kernel/debug/x"))
        assertFalse(DaemonIpcProtocol.readable("/data/adb/modules/MaxManager/module.prop"))
        assertFalse(DaemonIpcProtocol.readable("/proc/self/environ"))
    }

    @Test
    fun `write sends one WRITE line and succeeds only on OK`() {
        val seen = mutableListOf<String>()
        val ok = DaemonIpc(IpcTransport { request -> seen += request; "OK\n" })
        assertTrue(ok.write("/proc/sys/vm/swappiness", "60"))
        assertTrue(ok.write("/sys/devices/virtual/thermal/thermal_message/cpu_limits", "480000 1400000"))
        assertEquals(
            listOf(
                "WRITE /proc/sys/vm/swappiness 60",
                "WRITE /sys/devices/virtual/thermal/thermal_message/cpu_limits 480000 1400000",
            ),
            seen,
        )
        assertFalse(DaemonIpc(IpcTransport { "ERR invalid\n" }).write("/proc/sys/vm/swappiness", "60"))
        assertFalse(DaemonIpc(IpcTransport { "ERR write=13\n" }).write("/proc/sys/vm/swappiness", "60"))
        assertFalse(DaemonIpc(IpcTransport { null }).write("/proc/sys/vm/swappiness", "60"))
    }

    @Test
    fun `a malformed write never reaches the transport`() {
        var sent = 0
        val ipc = DaemonIpc(IpcTransport { sent++; "OK\n" })
        assertFalse(ipc.write("/proc/sys/vm/swappiness", ""))
        assertFalse(ipc.write("/proc/sys/vm/swappiness", "1;reboot"))
        assertFalse(ipc.write("/proc/sys/vm/swappiness", " 1"))
        assertFalse(ipc.write("/proc/sys/../etc/passwd", "1"))
        assertFalse(ipc.write("relative/path", "1"))
        assertFalse(ipc.write("/proc/sys/vm/swappiness", "x".repeat(65)))
        assertEquals(0, sent)
    }

    @Test
    fun `canWrite is a three-way answer`() {
        assertEquals(true, DaemonIpc(IpcTransport { "OK 1\n" }).canWrite("/proc/sys/vm/swappiness"))
        assertEquals(false, DaemonIpc(IpcTransport { "OK 0\n" }).canWrite("/proc/sys/vm/swappiness"))
        assertNull(DaemonIpc(IpcTransport { "ERR invalid\n" }).canWrite("/proc/sys/vm/swappiness"))
        assertNull(DaemonIpc(IpcTransport { null }).canWrite("/proc/sys/vm/swappiness"))
    }
}
