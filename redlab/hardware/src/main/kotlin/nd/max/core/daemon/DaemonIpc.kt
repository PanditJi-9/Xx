package nd.max.core.daemon

/**
 * Compatibility seam. MaxManager talks to a companion daemon; RedLab has no daemon yet, so every
 * call answers `null` ("this channel knows nothing"), which is exactly what the original expects
 * from an absent channel. The engine then falls through to its root shell path.
 */
object DaemonIpc {
    val default: DaemonIpc get() = this

    fun exists(path: String): Boolean? = null
    fun canWrite(path: String): Boolean? = null
    fun read(path: String): String? = null
    fun write(path: String, value: String): Boolean? = null
}
