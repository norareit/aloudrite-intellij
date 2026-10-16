package com.ritenoar.aloudrite

import com.intellij.openapi.util.io.NioFiles
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

// The one attempt to install the model, in this IDE or any other. It holds a lock on a single lock file in the folder
// that holds the model folder, taken before anything else is touched, so whoever holds it is the only attempt and
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
    val packageFile: Path get() = directory.resolve("$MODEL_PACKAGE_NAME.tar.bz2")
    val unpackedDirectory: Path get() = directory.resolve(UNPACKED_DIRECTORY_NAME)

    // Removes what attempts that were killed left behind, which is safe only because the lock is held, and then makes
    // the directory in which this attempt writes everything. It lies next to the model folder so that the final
    // rename stays on one file system
    fun prepareDirectory() {
        removeLeftovers(parent)
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
            heldHere.remove(lockFile)
        }
    }

    companion object {
        const val DIRECTORY_PREFIX = ".$MODEL_PACKAGE_NAME.download-"
        const val LOCK_FILE_NAME = ".$MODEL_PACKAGE_NAME.install.lock"
        private const val UNPACKED_DIRECTORY_NAME = "unpacked"

        // Closing a second channel on a file can release the locks that this process holds on it, so a lock file that
        // this process holds is never opened again: the answer comes from this set
        private val heldHere: MutableSet<Path> = ConcurrentHashMap.newKeySet()

        // Creates the folder that holds the model folder where it is missing, and takes the lock. Returns null, and
        // changes nothing else, when another IDE or this one holds it
        fun claim(parent: Path): InstallAttempt? {
            Files.createDirectories(parent)
            val realParent = parent.toRealPath()
            val lockFile = realParent.resolve(LOCK_FILE_NAME)
            if (!heldHere.add(lockFile)) return null
            try {
                return lockOrNull(realParent, lockFile) ?: run { heldHere.remove(lockFile); null }
            } catch (exception: Throwable) {
                heldHere.remove(lockFile)
                throw exception
            }
        }

        private fun lockOrNull(parent: Path, lockFile: Path): InstallAttempt? {
            val channel = FileChannel.open(lockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE)
            try {
                val lock = try {
                    channel.tryLock()
                } catch (_: OverlappingFileLockException) {
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

        // Only called while the lock is held. Only directories with the prefix go, and nothing else in the parent
        private fun removeLeftovers(parent: Path) {
            Files.newDirectoryStream(parent, "$DIRECTORY_PREFIX*").use { candidates ->
                candidates.filter { Files.isDirectory(it) }.forEach { NioFiles.deleteRecursively(it) }
            }
        }
    }
}
