package com.ritenoar.aloudrite

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

class InstallAttemptTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun aNewAttemptIsALockedDirectoryNextToTheModelFolder() {
        val parent = temporaryFolder.newFolder().toPath()

        InstallAttempt.start(parent).use { attempt ->
            assertEquals(parent, attempt.directory.parent)
            assertTrue(attempt.directory.fileName.toString().startsWith(".kokoro-int8-multi-lang-v1_0.download-"))
            assertTrue(Files.isDirectory(attempt.directory))
            assertTrue("The lock is not held", isLockHeld(attempt.directory.resolve("lock")))
            assertEquals(attempt.directory.resolve("kokoro-int8-multi-lang-v1_0.tar.bz2"), attempt.packageFile)
            assertEquals(attempt.directory.resolve("unpacked"), attempt.unpackedDirectory)
        }
    }

    @Test
    fun closingAnAttemptRemovesItsDirectoryWithEverythingInIt() {
        val parent = temporaryFolder.newFolder().toPath()
        val attempt = InstallAttempt.start(parent)
        Files.write(attempt.packageFile, byteArrayOf(1, 2, 3))
        Files.createDirectories(attempt.unpackedDirectory.resolve("a/b"))

        attempt.close()

        assertTrue(Files.list(parent).use { it.toList() }.isEmpty())
    }

    @Test
    fun aLeftoverWhoseLockIsFreeIsRemoved() {
        val parent = temporaryFolder.newFolder().toPath()
        val leftover = leftoverIn(parent, "one")

        InstallAttempt.removeLeftovers(parent)

        assertFalse(Files.exists(leftover))
    }

    @Test
    fun anAttemptThatIsStillRunningIsLeftAlone() {
        val parent = temporaryFolder.newFolder().toPath()
        InstallAttempt.start(parent).use { running ->
            Files.write(running.packageFile, byteArrayOf(1))

            InstallAttempt.removeLeftovers(parent)

            assertTrue(Files.exists(running.packageFile))
            assertTrue("The lock was released", isLockHeld(running.directory.resolve("lock")))
        }
    }

    @Test
    fun everyOtherEntryOfTheParentIsLeftAlone() {
        val parent = temporaryFolder.newFolder().toPath()
        val modelFolder = parent.resolve(MODEL_PACKAGE_NAME)
        fillWithModelStandIn(modelFolder)
        val otherDirectory = Files.createDirectory(parent.resolve(".other.download-1"))
        val otherFile = Files.createFile(parent.resolve(".kokoro-int8-multi-lang-v1_0.download-file"))
        val leftover = leftoverIn(parent, "gone")

        InstallAttempt.removeLeftovers(parent)

        assertTrue(isModelInPlace(modelFolder))
        assertTrue(Files.isDirectory(otherDirectory))
        assertTrue(Files.isRegularFile(otherFile))
        assertFalse(Files.exists(leftover))
    }

    private fun leftoverIn(parent: Path, name: String): Path {
        val directory = Files.createDirectories(parent.resolve("${InstallAttempt.DIRECTORY_PREFIX}$name/unpacked"))
        Files.write(directory.resolve("half"), byteArrayOf(1, 2))
        Files.createFile(directory.parent.resolve("lock"))
        return directory.parent
    }

    // A lock of this JVM cannot be taken a second time, which is also how the attempt tells a running one
    private fun isLockHeld(lockFile: Path): Boolean = FileChannel.open(lockFile, StandardOpenOption.WRITE).use { channel ->
        try {
            channel.tryLock()?.also { it.release() } == null
        } catch (_: java.nio.channels.OverlappingFileLockException) {
            true
        }
    }
}
