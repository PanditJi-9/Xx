package dev.redlab.core.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionMachineTest {

    private var now = 1_000L
    private val machine = SessionMachine(clock = { now }, graceMillis = 60_000)

    private fun foreground(pkg: String?, isGame: Boolean) = machine.onForeground(pkg, isGame, now)

    @Test
    fun `a game in front arms the session from idle`() {
        val snap = foreground("cn.nubia.gamelauncher.game", isGame = true)
        assertEquals(SessionState.ARMED, snap.state)
        assertEquals("cn.nubia.gamelauncher.game", snap.packageName)
        assertEquals(SESSION_CONTRACT_VERSION, snap.contractVersion)
    }

    @Test
    fun `a non-game from idle keeps the session idle`() {
        assertEquals(SessionState.IDLE, foreground("com.android.settings", isGame = false).state)
    }

    @Test
    fun `showing the overlay activates an armed session only`() {
        assertEquals(SessionState.IDLE, machine.onOverlayShown(now).state)

        foreground("game.a", isGame = true)
        assertEquals(SessionState.ACTIVE, machine.onOverlayShown(now).state)
        assertEquals(SessionState.ARMED, machine.onOverlayHidden(now).state)
    }

    @Test
    fun `losing the foreground pauses and keeps the game name`() {
        foreground("game.a", isGame = true)
        machine.onOverlayShown(now)

        now += 1_000
        val paused = foreground("com.android.dialer", isGame = false)
        assertEquals(SessionState.PAUSED, paused.state)
        assertEquals("game.a", paused.packageName)
    }

    @Test
    fun `coming back within the grace period does not end the session`() {
        foreground("game.a", isGame = true)
        machine.onOverlayShown(now)
        now += 10_000
        foreground("com.android.dialer", isGame = false)

        now += 30_000
        assertEquals(SessionState.ARMED, machine.onResume("game.a", isGame = true).state)
        machine.onOverlayShown(now)
        assertEquals(SessionState.ACTIVE, machine.current().state)
    }

    @Test
    fun `grace expiry ends the session with a reason`() {
        foreground("game.a", isGame = true)
        now += 1_000
        foreground("com.android.dialer", isGame = false)

        now += 59_000
        assertEquals(SessionState.PAUSED, machine.tick(now).state)

        now += 2_000
        assertEquals(SessionState.ENDED, machine.tick(now).state)
        assertEquals("grace-expired", machine.lastEndReason)
        assertNull(machine.current().packageName)
    }

    @Test
    fun `a tick while the game is in front never ends anything`() {
        foreground("game.a", isGame = true)
        now += 600_000
        assertEquals(SessionState.ARMED, machine.tick(now).state)
    }

    @Test
    fun `ending is explicit and idempotent`() {
        foreground("game.a", isGame = true)
        machine.end("user")
        assertEquals(SessionState.ENDED, machine.end("again", now).state)
        assertEquals("user", machine.lastEndReason)
    }
}
