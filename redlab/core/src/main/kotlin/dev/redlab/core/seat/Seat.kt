package dev.redlab.core.seat

import dev.redlab.core.capability.NodeIo

/** The three ways this ecosystem is allowed to touch a device, weakest first. */
enum class Tier { LOCAL, ADB, ROOT }

/** Result of one shell invocation. `exitCode = -1` means "never ran", not "failed with -1". */
data class ShellResult(val exitCode: Int, val stdout: String, val stderr: String) {
    val ok: Boolean get() = exitCode == 0
}

/** Everything the seats need from a process runner; Android supplies it, tests fake it. */
fun interface Shell {
    fun run(command: List<String>): ShellResult
}

/**
 * [NodeIo] over a shell, for the ADB and ROOT seats. The `LOCAL` seat never uses this: without a
 * shell there is no path outside our own app directory that a plain app may write.
 *
 * Values are validated before they reach a command line. Concatenating an unvalidated value into
 * `sh -c` would turn a hardware control into command injection — a capability the original app
 * never had, and one we will not invent.
 */
class ShellNodeIo(private val shell: Shell) : NodeIo {

    override fun read(path: String): String? {
        if (!isSafePath(path)) return null
        val result = shell.run(listOf("cat", path))
        if (!result.ok) return null
        return result.stdout.trim().ifEmpty { null }
    }

    override fun write(path: String, value: String): Boolean {
        if (!isSafePath(path) || !isSafeValue(value)) return false
        val result = shell.run(listOf("sh", "-c", "echo '$value' > '$path'"))
        return result.ok
    }

    private fun isSafePath(path: String): Boolean =
        path.startsWith("/") && path.none { it == '\'' || it == '\n' || it == '\r' }

    private fun isSafeValue(value: String): Boolean = SAFE_VALUE.matches(value)

    private companion object {
        /** Sysfs values are enums, integers, or short identifiers. Nothing else is accepted. */
        val SAFE_VALUE = Regex("[A-Za-z0-9._:+/-]{1,64}")
    }
}
