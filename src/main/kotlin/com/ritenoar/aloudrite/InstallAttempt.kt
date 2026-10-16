package com.ritenoar.aloudrite

import com.intellij.openapi.util.io.NioFiles
import java.io.IOException
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

// The directory in which one attempt to install the model writes everything. It lies next to the model folder so that
// the final rename stays on one file system. It is locked for as long as the attempt runs, which is how a later attempt
// tells a directory that a killed IDE left from one that another IDE is still using
class InstallAttempt private constructor(
    val directory: Path,
    private val lockChannel: FileChannel,
    private val lock: FileLock,
) : AutoCloseable {

    val packageFile: Path get() = directory.resolve("$MODEL_PACKAGE_NAME.tar.bz2")
    val unpackedDirectory: Path get() = directory.resolve(UNPACKED_DIRECTORY_NAME)

    // Removes everything of the attempt, whether it succeeded or not
    override fun close() {
        lock.release()
        lockChannel.close()
        runningHere.remove(directory)
        NioFiles.deleteRecursively(directory)
    }

    companion object {
        const val DIRECTORY_PREFIX = ".$MODEL_PACKAGE_NAME.download-"
        private const val LOCK_FILE_NAME = "lock"

        // Closing a second channel on a file can release the locks that this process holds on it, so a directory of
        // an attempt in this process is never opened again, and only the locks of other processes are probed
        private val runningHere: MutableSet<Path> = ConcurrentHashMap.newKeySet()
        private const val UNPACKED_DIRECTORY_NAME = "unpacked"

        // The name under which a directory is made and locked. It does not match the prefix that the leftover removal
        // looks for, so the removal cannot meet a directory that is not locked yet
        private const val STAGING_PREFIX = ".$MODEL_PACKAGE_NAME.staging-"

        // A start takes a few file operations, so a staging directory older than this belongs to a start that was
        // killed. Age is the only mark available, as the directory may not hold its lock file yet
        private val STAGING_GRACE: Duration = Duration.ofMinutes(5)

        // The directory is made and locked under a name that removeLeftovers ignores, and only then renamed to the name
        // that it looks for. A directory that carries that name therefore always has its lock held already, and a
        // concurrent removal of leftovers, in this IDE or another, can never take a starting attempt for a leftover.
        // `beforeVisible` runs after the lock is held and before the rename, for a test to run a removal at that point
        fun start(parent: Path, beforeVisible: () -> Unit = {}): InstallAttempt {
            val staging = Files.createTempDirectory(parent, STAGING_PREFIX)
            val directory = parent.resolve(DIRECTORY_PREFIX + staging.fileName.toString().removePrefix(STAGING_PREFIX))
            // Registered before the directory is visible, so that a removal in this JVM skips it from the first moment
            runningHere.add(staging)
            runningHere.add(directory)
            try {
                val channel = FileChannel.open(
                    staging.resolve(LOCK_FILE_NAME), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
                try {
                    val lock = channel.tryLock() ?: throw IOException("The lock of $staging is held by another process")
                    beforeVisible()
                    Files.move(staging, directory, StandardCopyOption.ATOMIC_MOVE)
                    runningHere.remove(staging)
                    return InstallAttempt(directory, channel, lock)
                } catch (exception: Throwable) {
                    channel.close()
                    throw exception
                }
            } catch (exception: Throwable) {
                runningHere.remove(staging)
                runningHere.remove(directory)
                NioFiles.deleteRecursively(staging)
                NioFiles.deleteRecursively(directory)
                throw exception
            }
        }

        // An attempt directory whose lock is free belongs to an attempt that was killed. One whose lock is held
        // belongs to an attempt that another IDE, or this one, is still running, and is left alone. A staging
        // directory is a start that was killed before its rename once it is older than the grace period
        fun removeLeftovers(parent: Path) {
            Files.newDirectoryStream(parent, "$DIRECTORY_PREFIX*").use { candidates ->
                candidates.filter { Files.isDirectory(it) }.forEach { removeIfNobodyHoldsIt(it) }
            }
            Files.newDirectoryStream(parent, "$STAGING_PREFIX*").use { candidates ->
                candidates.filter { Files.isDirectory(it) && isOlderThanGrace(it) }.forEach { removeIfNobodyHoldsIt(it) }
            }
        }

        private fun isOlderThanGrace(directory: Path): Boolean = try {
            Files.getLastModifiedTime(directory).toInstant().isBefore(Instant.now().minus(STAGING_GRACE))
        } catch (_: NoSuchFileException) {
            false
        }

        private fun removeIfNobodyHoldsIt(directory: Path) {
            if (directory in runningHere) return
            val lockFile = directory.resolve(LOCK_FILE_NAME)
            if (!Files.exists(lockFile)) return NioFiles.deleteRecursively(directory)
            try {
                FileChannel.open(lockFile, StandardOpenOption.WRITE).use { channel ->
                    val lock = channel.tryLock() ?: return
                    lock.release()
                }
            } catch (_: OverlappingFileLockException) {
                return
            } catch (_: NoSuchFileException) {
                return
            }
            NioFiles.deleteRecursively(directory)
        }
    }
}
