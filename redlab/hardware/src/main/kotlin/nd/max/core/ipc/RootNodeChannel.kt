package nd.max.core.ipc

/** Compatibility seam: no bound root-node service exists in RedLab yet, so this is always null. */
object RootNodeChannel {
    interface Service {
        fun exists(path: String): Boolean
        fun readText(path: String): String?
        fun readTexts(paths: MutableList<String>): List<String?>
        fun writeText(path: String, value: String): Boolean
        fun listNames(path: String, directoriesOnly: Boolean): List<String>
    }

    val service: Service? = null
}
