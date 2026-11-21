package nd.max.core.jni

/**
 * Compatibility seam for MaxManager's Rust probe. No native library is shipped here, so every
 * call returns `null` and the engine uses the next transport — the same contract as a build
 * without the `.so`, which MaxManager already handles.
 */
object ProbeBridge {
    fun existing(paths: List<String>): List<Boolean>? = null
    fun readMany(paths: List<String>): List<String?>? = null
    fun listNames(path: String, dirsOnly: Boolean = false): List<String>? = null
}
