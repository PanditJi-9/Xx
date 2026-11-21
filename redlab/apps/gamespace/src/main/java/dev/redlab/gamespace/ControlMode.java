package dev.redlab.gamespace;

/**
 * How this build reaches privileged controls on the phone it is installed on.
 *
 * <p>One value is chosen at runtime and every control screen asks this, never the build type:
 * the same APK must work on a rooted phone, a phone running Shizuku, and a stock phone.
 * {@link #NONE} is not a failure — it means the UI reads state and changes nothing.
 */
public enum ControlMode {
    /** A shell that answers `id` with uid 0. */
    ROOT,
    /** Shizuku's binder service is alive and can run commands with shell identity. */
    SHIZUKU,
    /** No privileged transport: read-only. */
    NONE
}
