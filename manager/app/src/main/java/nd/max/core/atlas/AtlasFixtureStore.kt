/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.atlas

import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Keeps captured device fixtures as files the user can hand over (`P13`'s device half).
 *
 * A fixture is the only artifact in this system that carries what a **real** device answered, so this
 * store is deliberately the dullest thing in the package: an app-generated name, a bounded size checked
 * *before* the file exists, a temporary name renamed into place, and an age-based cleanup that never
 * takes the file a share may still be reading. There is **no upload path, no client and no recipient**
 * anywhere in this class; a fixture leaves the device only when the user picks a destination in the
 * system chooser, exactly like the support report.
 *
 * Why this is not [nd.max.core.diagnostics.AtlasReportExporter] with a different name: they are
 * different artifacts with different rules, and folding them together would force one to adopt the
 * other's. A report is a *reconstructible summary* of a finished pass and is kept for a day; a fixture
 * is *captured evidence* the user asked for deliberately, bounded by
 * [AtlasFixture.MAX_BYTES] rather than by the report schema's ceiling, and kept long enough that a
 * capture made to report a bug is still there when the reply comes. Both keep the same write
 * discipline, and that discipline is the part worth sharing — the lifetime is not.
 *
 * The directory is the app's cache: a fixture is derivable from another run on the same device, and
 * private storage that backups do not carry is the right home for something that describes one machine.
 */
class AtlasFixtureStore(
    private val directory: File,
    private val clockMs: () -> Long = System::currentTimeMillis,
) {

    /** Names are generated here, never taken from content, so a fixture cannot name its own file. */
    private fun nameFor(stamp: Long): String = "$PREFIX$stamp$SUFFIX"

    /**
     * Writes one encoded fixture and returns the file, or `null` when it could not be written.
     *
     * The ceiling is checked first: an artifact larger than the format allows is refused rather than
     * landing as a file a maintainer would read as complete. A refused write is a `null`, and the caller
     * reports it — never a silent success.
     */
    fun write(text: String): File? {
        if (text.toByteArray(StandardCharsets.UTF_8).size > AtlasFixture.MAX_BYTES) return null
        return try {
            if (!directory.exists() && !directory.mkdirs()) return null
            val temporary = File(directory, "$PREFIX$TEMP_SUFFIX")
            temporary.writeText(text, StandardCharsets.UTF_8)
            val target = File(directory, nameFor(clockMs()))
            try {
                Files.move(
                    temporary.toPath(),
                    target.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE,
                )
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
            target
        } catch (_: IOException) {
            null
        } catch (_: SecurityException) {
            null
        }
    }

    /**
     * Reads one stored fixture back, bounded by the format's own ceiling.
     *
     * An oversize file is refused rather than read: the decoder would refuse it anyway, and reading a
     * hundred megabytes to learn that is the kind of cost a bound exists to prevent.
     */
    fun read(file: File): String? {
        val length = file.length()
        if (length <= 0L || length > AtlasFixture.MAX_BYTES) return null
        return try {
            String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)
        } catch (_: IOException) {
            null
        } catch (_: SecurityException) {
            null
        }
    }

    /** Fixtures currently on disk, newest first. */
    fun fixtures(): List<File> = directory.listFiles()
        .orEmpty()
        .filter { it.isFile && it.name.startsWith(PREFIX) && it.name.endsWith(SUFFIX) }
        .sortedByDescending { it.name }

    /**
     * Removes fixtures older than [maxAgeMs], except [protected] — the one a share may still be reading.
     *
     * A fixture that cannot be aged (a clock that moved backwards) is kept: the file is the only record
     * of a device the maintainer may still need, and deleting evidence because time is unmeasurable is
     * the wrong direction.
     */
    fun cleanup(maxAgeMs: Long = DEFAULT_MAX_AGE_MS, protected: File? = null): Int {
        if (maxAgeMs <= 0L) return 0
        val now = clockMs()
        return fixtures().count { fixture ->
            if (fixture == protected) return@count false
            val age = now - fixture.lastModified()
            age > maxAgeMs && fixture.delete()
        }
    }

    /** The temporary file must never survive a write, so a reader can never see a half artifact. */
    fun temporaryFiles(): List<File> = directory.listFiles().orEmpty().filter { it.name.endsWith(TEMP_SUFFIX) }

    companion object {
        /** The directory name under a cache root. Declared once so a test cannot guess it. */
        const val DIRECTORY_NAME: String = "atlas-fixtures"

        /**
         * Seven days: long enough that a capture made to report a problem survives the conversation
         * about it, short enough that repeated captures do not accumulate.
         */
        const val DEFAULT_MAX_AGE_MS: Long = 7L * 24L * 60L * 60L * 1000L

        private const val PREFIX = "atlas-fixture-"
        private const val SUFFIX = ".json"
        private const val TEMP_SUFFIX = ".tmp"

        /** The directory under a cache root. Cache, not files: a fixture describes one machine. */
        fun directoryFor(cacheRoot: File): File = File(cacheRoot, DIRECTORY_NAME)
    }
}
