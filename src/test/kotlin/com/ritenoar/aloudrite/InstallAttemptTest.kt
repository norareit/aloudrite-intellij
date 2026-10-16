package com.ritenoar.aloudrite

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import java.nio.file.Path

class InstallAttemptTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val lockFileName = ".kokoro-int8-multi-lang-v1_0.install.lock"

    @Test
    fun claimingCreatesTheParentAndTheLockFileAndTouchesNothingElse() {
        val parent = temporaryFolder.newFolder().toPath().resolve("data/aloudrite")

        InstallAttempt.claim(parent)!!.use {
            assertEquals(listOf(lockFileName), namesIn(parent))
        }
    }

    @Test
    fun anAttemptThatClaimedHasNoDirectoryUntilItPreparesOne() {
        val parent = temporaryFolder.newFolder().toPath()

        InstallAttempt.claim(parent)!!.use { attempt ->
            assertEquals(listOf(lockFileName), namesIn(parent))

            attempt.prepareDirectory()

            assertEquals(parent.toRealPath(), attempt.directory.parent)
            assertTrue(attempt.directory.fileName.toString().startsWith(".kokoro-int8-multi-lang-v1_0.download-"))
            assertTrue(Files.isDirectory(attempt.directory))
            assertEquals(attempt.directory.resolve("kokoro-int8-multi-lang-v1_0.tar.bz2"), attempt.packageFile)
            assertEquals(attempt.directory.resolve("unpacked"), attempt.unpackedDirectory)
        }
    }

    @Test
    fun leftoversAreRemovedWhilstTheLockIsHeldAndNothingElseIsTouched() {
        val parent = temporaryFolder.newFolder().toPath()
        val modelFolder = parent.resolve(MODEL_PACKAGE_NAME)
        fillWithModelStandIn(modelFolder)
        val leftover = leftoverIn(parent, "one")
        val leftoverWithoutLockFile = Files.createDirectories(parent.resolve("${InstallAttempt.DIRECTORY_PREFIX}two/unpacked"))
        val oldStaging = Files.createDirectory(parent.resolve(".kokoro-int8-multi-lang-v1_0.staging-three"))
        val otherDirectory = Files.createDirectory(parent.resolve(".other.download-1"))
        val otherFile = Files.createFile(parent.resolve("${InstallAttempt.DIRECTORY_PREFIX}file"))

        InstallAttempt.claim(parent)!!.use { attempt ->
            attempt.prepareDirectory()

            assertFalse(Files.exists(leftover))
            assertFalse(Files.exists(leftoverWithoutLockFile))
            assertTrue(Files.isDirectory(attempt.directory))
        }

        assertTrue(isModelInPlace(modelFolder))
        assertTrue(Files.isDirectory(oldStaging))
        assertTrue(Files.isDirectory(otherDirectory))
        assertTrue(Files.isRegularFile(otherFile))
    }

    @Test
    fun aSecondAttemptInTheSameProcessStartsNothingAndChangesNothing() {
        val parent = temporaryFolder.newFolder().toPath()
        InstallAttempt.claim(parent)!!.use { first ->
            first.prepareDirectory()
            Files.write(first.packageFile, byteArrayOf(1, 2, 3))
            val before = namesIn(parent)

            assertNull(InstallAttempt.claim(parent))

            assertEquals(before, namesIn(parent))
            assertTrue(Files.isRegularFile(first.packageFile))
            assertNull("The lock was released by the failed claim", InstallAttempt.claim(parent))
        }
    }

    @Test
    fun closingRemovesTheDirectoryReleasesTheLockAndKeepsTheLockFile() {
        val parent = temporaryFolder.newFolder().toPath()
        val attempt = InstallAttempt.claim(parent)!!
        attempt.prepareDirectory()
        Files.write(attempt.packageFile, byteArrayOf(1, 2, 3))
        Files.createDirectories(attempt.unpackedDirectory.resolve("a/b"))

        attempt.close()
        attempt.close()

        assertEquals(listOf(lockFileName), namesIn(parent))
        InstallAttempt.claim(parent)!!.close()
    }

    @Test
    fun theLockFileSurvivesAFailureInTheMiddleOfAnAttemptAndTheLockIsFree() {
        val parent = temporaryFolder.newFolder().toPath()

        try {
            InstallAttempt.claim(parent)!!.use { attempt ->
                attempt.prepareDirectory()
                throw IllegalStateException("The attempt failed")
            }
        } catch (_: IllegalStateException) {
        }

        assertEquals(listOf(lockFileName), namesIn(parent))
        assertNotNull(InstallAttempt.claim(parent)?.also { it.close() })
    }

    @Test
    fun anAttemptWhileAnotherProcessHoldsTheLockStartsNothingAndTheLockIsFreeOnceThatProcessIsGone() {
        val parent = temporaryFolder.newFolder().toPath()
        Files.createDirectories(parent)
        val leftover = leftoverIn(parent, "other-ide")
        val lockFile = parent.toRealPath().resolve(lockFileName)

        LockProbe.start("hold", lockFile).use { otherIde ->
            assertEquals("locked", otherIde.firstLine)

            assertNull(InstallAttempt.claim(parent))
            assertTrue("A leftover was removed without the lock", Files.exists(leftover))

            otherIde.kill()
        }

        InstallAttempt.claim(parent)!!.use { attempt ->
            attempt.prepareDirectory()
            assertFalse(Files.exists(leftover))
        }
    }

    @Test
    fun theLockOfAnAttemptKeepsAnotherProcessOut() {
        val parent = temporaryFolder.newFolder().toPath()

        InstallAttempt.claim(parent)!!.use {
            LockProbe.start("try", parent.toRealPath().resolve(lockFileName)).use { otherIde ->
                assertEquals("held", otherIde.firstLine)
            }
        }
        LockProbe.start("try", parent.toRealPath().resolve(lockFileName)).use { otherIde ->
            assertEquals("free", otherIde.firstLine)
        }
    }

    @Test
    fun aRejectedClaimThroughAnotherClassLoaderLeavesTheLockOfTheOwnerInForce() {
        val parent = temporaryFolder.newFolder().toPath()
        val lockFile = parent.toRealPath().resolve(lockFileName)

        InstallAttempt.claim(parent)!!.use { owner ->
            assertNull("Another class loader got the lock", claimThroughAnotherClassLoader(parent))

            LockProbe.start("try", lockFile).use { otherIde ->
                assertEquals("The rejected claim released the lock of the owner", "held", otherIde.firstLine)
            }
            assertNotNull(owner)
        }
    }

    // Loads a copy of InstallAttempt of its own, as a second class loader of the plugin would. The rest comes from here
    private fun claimThroughAnotherClassLoader(parent: Path): AutoCloseable? {
        val ownCopy = object : ClassLoader(InstallAttempt::class.java.classLoader) {
            override fun loadClass(name: String, resolve: Boolean): Class<*> {
                if (!name.startsWith(InstallAttempt::class.java.name)) return super.loadClass(name, resolve)
                synchronized(getClassLoadingLock(name)) {
                    findLoadedClass(name)?.let { return it }
                    val bytes = parent().getResourceAsStream(name.replace('.', '/') + ".class")!!.use { it.readBytes() }
                    return defineClass(name, bytes, 0, bytes.size)
                }
            }

            private fun parent() = InstallAttempt::class.java.classLoader
        }
        val attemptClass = ownCopy.loadClass(InstallAttempt::class.java.name)
        assertTrue(attemptClass !== InstallAttempt::class.java)
        val companion = attemptClass.getField("Companion").get(null)
        return companion.javaClass.getMethod("claim", Path::class.java).invoke(companion, parent) as AutoCloseable?
    }

    private fun leftoverIn(parent: Path, name: String): Path {
        val directory = Files.createDirectories(parent.resolve("${InstallAttempt.DIRECTORY_PREFIX}$name/unpacked"))
        Files.write(directory.resolve("half"), byteArrayOf(1, 2))
        return directory.parent
    }

    private fun namesIn(directory: Path): List<String> =
        Files.list(directory).use { entries -> entries.map { it.fileName.toString() }.toList().sorted() }
}
