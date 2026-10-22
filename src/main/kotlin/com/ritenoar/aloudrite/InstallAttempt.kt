package com.ritenoar.aloudrite

import com.intellij.openapi.util.io.NioFiles
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.concurrent.atomic.AtomicBoolean

// The one attempt to install a model, in this IDE or any other. It holds a lock on a single lock file in the folder
// that holds the model folders, taken before anything else is touched, so whoever holds it is the only attempt and
// every leftover found while holding it belongs to a dead attempt. The lock file is never deleted, not even after a
// success: deleting a file that is held reopens a race on POSIX, in which another process locks the old inode while a
// third one creates and locks a new file
class InstallAttempt private constructor(
    private val parent: Path,
    private val lockFile: Path,
    private val lockChannel: FileChannel,
    private val lock: FileLock,
) : AutoCloseable {

    private var workDirectory: Path? = null
    private val closed = AtomicBoolean(false)

    val directory: Path get() = checkNotNull(workDirectory) { "The directory of the attempt was not prepared" }
    val packageFile: Path get() = directory.resolve("package.tar.bz2")
    val unpackedDirectory: Path get() = directory.resolve(UNPACKED_DIRECTORY_NAME)

    // Removes what attempts that were killed left behind, which is safe only because the lock is held. Only
    // directories with the prefix go, and nothing else in the parent
    fun removeLeftovers() {
        Files.newDirectoryStream(parent, "$DIRECTORY_PREFIX*").use { candidates ->
            candidates.filter { Files.isDirectory(it) }.forEach { NioFiles.deleteRecursively(it) }
        }
    }

    // Removes the leftovers, then makes the directory in which this attempt writes everything. It lies next to the
    // model folder so that the final rename stays on one file system
    fun prepareDirectory() {
        removeLeftovers()
        workDirectory = Files.createTempDirectory(parent, DIRECTORY_PREFIX)
    }

    // Removes everything of the attempt, whether it succeeded or not, and releases the lock. The lock file stays
    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        try {
            workDirectory?.let { NioFiles.deleteRecursively(it) }
        } finally {
            lock.release()
            lockChannel.close()
            markFree(lockFile)
        }
    }

    companion object {
        // One lock and one prefix for every model, which is what lets one download run at a time across the models and
        // the IDE windows. They keep the names from when the int8 model was the only model, so that an IDE that still
        // runs an earlier version of the plugin takes the same lock and its leftovers are found
        const val DIRECTORY_PREFIX = ".kokoro-int8-multi-lang-v1_0.download-"
        const val LOCK_FILE_NAME = ".kokoro-int8-multi-lang-v1_0.install.lock"
        private const val UNPACKED_DIRECTORY_NAME = "unpacked"

        private const val HELD_LOCK_PROPERTY_PREFIX = "com.ritenoar.aloudrite.heldInstallLock:"

        // Closing a second channel on a file can release the locks that this process holds on it, so a lock file that
        // this process holds is never opened again: the answer comes from the system properties, which every class
        // loader of the JVM shares, such as that of a reloaded plugin, which a set of this class alone would not be.
        // One property per held lock file, with plain strings only, since Properties.store fails for the whole IDE on
        // any other value. putIfAbsent on the properties is atomic, so two claims never both succeed
        private fun holdKey(lockFile: Path) = HELD_LOCK_PROPERTY_PREFIX + lockFile

        private fun markHeld(lockFile: Path): Boolean = System.getProperties().putIfAbsent(holdKey(lockFile), "held") == null

        private fun markFree(lockFile: Path) {
            System.getProperties().remove(holdKey(lockFile))
        }

        // Creates the folder that holds the model folder where it is missing, and takes the lock. Returns null, and
        // changes nothing else, when another IDE or this one holds it
        fun claim(parent: Path): InstallAttempt? {
            Files.createDirectories(parent)
            val realParent = parent.toRealPath()
            val lockFile = realParent.resolve(LOCK_FILE_NAME)
            if (!markHeld(lockFile)) return null
            try {
                return lockOrNull(realParent, lockFile) ?: run { markFree(lockFile); null }
            } catch (exception: Throwable) {
                markFree(lockFile)
                throw exception
            }
        }

        private fun lockOrNull(parent: Path, lockFile: Path): InstallAttempt? {
            val channel = FileChannel.open(lockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE)
            try {
                val lock = try {
                    channel.tryLock()
                } catch (_: OverlappingFileLockException) {
                    // Not reached by a copy of this class, which finds the lock in the properties. Another holder in this JVM
                    // is not known to them, and closing this channel then may release its lock
                    null
                }
                if (lock == null) {
                    channel.close()
                    return null
                }
                return InstallAttempt(parent, lockFile, channel, lock)
            } catch (exception: Throwable) {
                channel.close()
                throw exception
            }
        }
    }
}
