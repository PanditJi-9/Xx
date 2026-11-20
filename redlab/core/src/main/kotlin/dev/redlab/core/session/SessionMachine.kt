package dev.redlab.core.session

/**
 * The five states of `docs/GAME_SESSION_SPEC.md` §2.
 *
 * `ARMED` exists because the original separates *preparing* from *showing*: `GameAssistService` is
 * bound and ready, while `ACTION_SHOW_PANEL` / `ACTION_HIDE_PANEL` decide whether the panel is on
 * screen. Collapsing the two builds an overlay the user never asked for.
 */
enum class SessionState { IDLE, ARMED, ACTIVE, PAUSED, ENDED }

/** Contract version carried on the wire; a consumer that does not know it must disable, not guess. */
const val SESSION_CONTRACT_VERSION = 1

data class SessionSnapshot(
    val state: SessionState,
    val packageName: String? = null,
    val sinceMillis: Long = 0,
    val contractVersion: Int = SESSION_CONTRACT_VERSION,
)

/**
 * The session state machine, owned by `gamespace` alone (`docs/GAME_SESSION_SPEC.md` §1).
 *
 * Pure Kotlin on purpose: the transitions are the part another module's behaviour depends on, so
 * they are tested on the JVM. Detecting the foreground package is the caller's job — this class
 * never reads Android APIs and never guesses that something is a game.
 *
 * Illegal transitions are ignored rather than thrown: a stale broadcast arriving late must not
 * crash the owner. What the caller *does* learn is the resulting snapshot, always.
 */
class SessionMachine(
    private val clock: () -> Long = System::currentTimeMillis,
    /** Grace period before a backgrounded game ends. Design choice, see the spec. */
    private val graceMillis: Long = 60_000,
) {
    private var snapshot = SessionSnapshot(SessionState.IDLE, sinceMillis = clock())
    private var backgroundedAt: Long? = null

    fun current(): SessionSnapshot = snapshot

    private fun move(state: SessionState, pkg: String?, at: Long = clock()): SessionSnapshot {
        if (snapshot.state == state && snapshot.packageName == pkg) return snapshot
        snapshot = SessionSnapshot(state, pkg, at)
        return snapshot
    }

    /**
     * The foreground application changed. A known game arms the session; anything else either
     * pauses it (temporary loss) or ends it (left for good — decided by [tick]).
     */
    fun onForeground(packageName: String?, isGame: Boolean, at: Long = clock()): SessionSnapshot =
        when {
            isGame && packageName != null -> {
                backgroundedAt = null
                when (snapshot.state) {
                    SessionState.ACTIVE, SessionState.PAUSED, SessionState.ARMED ->
                        move(SessionState.ARMED, packageName, at)
                    SessionState.IDLE, SessionState.ENDED -> move(SessionState.ARMED, packageName, at)
                }
            }
            // Not a game: only an armed/active session has anything to lose.
            snapshot.state == SessionState.ARMED || snapshot.state == SessionState.ACTIVE -> {
                if (backgroundedAt == null) backgroundedAt = at
                move(SessionState.PAUSED, snapshot.packageName, at)
            }
            else -> snapshot
        }

    /** The overlay was shown by its owner; only an armed session may become active. */
    fun onOverlayShown(at: Long = clock()): SessionSnapshot =
        if (snapshot.state == SessionState.ARMED) move(SessionState.ACTIVE, snapshot.packageName, at) else snapshot

    /** The overlay was hidden; the session goes back to armed and keeps the game. */
    fun onOverlayHidden(at: Long = clock()): SessionSnapshot =
        if (snapshot.state == SessionState.ACTIVE) move(SessionState.ARMED, snapshot.packageName, at) else snapshot

    /** The game came back within the grace period, or after it has already ended. */
    fun onResume(packageName: String?, isGame: Boolean, at: Long = clock()): SessionSnapshot =
        onForeground(packageName, isGame, at)

    /** End the session explicitly (user action, game exit, or grace expiry). */
    fun end(reason: String? = null, at: Long = clock()): SessionSnapshot {
        if (snapshot.state == SessionState.ENDED) return snapshot
        backgroundedAt = null
        lastEndReason = reason
        return move(SessionState.ENDED, null, at)
    }

    /**
     * Called on a timer. Ends a paused session once the grace period passes; does nothing while
     * the game is still in front.
     */
    fun tick(at: Long = clock()): SessionSnapshot {
        val since = backgroundedAt ?: return snapshot
        if (snapshot.state == SessionState.PAUSED && at - since >= graceMillis) {
            lastEndReason = "grace-expired"
            return move(SessionState.ENDED, null, at)
        }
        return snapshot
    }

    /** Why the last session ended; null when it has not ended. */
    var lastEndReason: String? = null
        private set
}
