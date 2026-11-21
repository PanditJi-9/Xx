package nd.max.core.platform

/**
 * Compatibility seam. MaxManager writes every verified write to a rotating log; here the call is
 * kept (so ported code is unchanged) and forwarded to an optional sink the app installs.
 */
object EventLog {
    @Volatile
    var sink: ((path: String, wrote: String, readBack: String?, verdict: String) -> Unit)? = null

    fun writeCheck(path: String, wrote: String, readBack: String?, verdict: String) {
        sink?.invoke(path, wrote, readBack, verdict)
    }
}
