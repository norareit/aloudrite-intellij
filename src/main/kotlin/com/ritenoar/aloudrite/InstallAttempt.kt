package com.ritenoar.aloudrite

import com.intellij.openapi.util.io.NioFiles
import java.io.IOException
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
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

        fun start(parent: Path): InstallAttempt {
            val directory = Files.createTempDirectory(parent, DIRECTORY_PREFIX)
            try {
                val channel = FileChannel.open(
                    directory.resolve(LOCK_FILE_NAME), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
                val lock = channel.tryLock() ?: run {
                    channel.close()
                    throw IOException("The lock of $directory is held by another process")
                }
                runningHere.add(directory)
                return InstallAttempt(directory, channel, lock)
            } catch (exception: Throwable) {
                NioFiles.deleteRecursively(directory)
                throw exception
            }
        }

        // An attempt directory whose lock is free belongs to an attempt that was killed. One whose lock is held
        // belongs to an attempt that another IDE, or this one, is still running, and is left alone
        fun removeLeftovers(parent: Path) {
            Files.newDirectoryStream(parent, "$DIRECTORY_PREFIX*").use { candidates ->
                candidates.filter { Files.isDirectory(it) }.forEach { removeIfNobodyHoldsIt(it) }
            }
        }

        private fun removeIfNobodyHoldsIt(directory: Path) {
            if (directory in runningHere) return
            val lockFile = directory.resolve(LOCK_FILE_NAME)
            if (!Files.exists(lockFile)) return NioFiles.deleteRecursively(directory)
            FileChannel.open(lockFile, StandardOpenOption.WRITE).use { channel ->
                val lock = channel.tryLock() ?: return
                lock.release()
            }
            NioFiles.deleteRecursively(directory)
        }
    }
}
