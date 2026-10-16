package com.ritenoar.aloudrite

import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.testFramework.ApplicationRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermissions
import java.security.MessageDigest
import kotlin.io.path.relativeTo

// Runs only where ALOUDRITE_MODEL_PACKAGE names a copy of the real package on disk. It installs that copy into a
// temporary folder, and only reads the model folder of the machine, which it compares the result with
class RealModelPackageTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun theRealPackageInstallsToTheSameModelAsTheOneInPlaceByHand() {
        val packageFile = System.getenv("ALOUDRITE_MODEL_PACKAGE")?.let { Paths.get(it) }
        assumeTrue("ALOUDRITE_MODEL_PACKAGE does not name a file", packageFile != null && Files.isRegularFile(packageFile))
        val modelFolder = temporaryFolder.newFolder().toPath().resolve("aloudrite").resolve(MODEL_PACKAGE_NAME)

        PackageServerStandIn().use { server ->
            server.serve("/package", Files.readAllBytes(packageFile!!))
            val modelPackage = KOKORO_MODEL_PACKAGE.copyWithAddress(server.address("/package"))
            val startedAtNanos = System.nanoTime()

            ModelInstaller(modelPackage).install(modelFolder, EmptyProgressIndicator())

            println("Installing the real package took ${(System.nanoTime() - startedAtNanos) / 1_000_000} ms")
        }

        assertTrue(isModelInPlace(modelFolder))
        val handPlaced = userModelFolder(System.getProperty("os.name"), System.getProperty("user.home"), System.getenv("XDG_DATA_HOME"))
        if (isModelInPlace(handPlaced)) compare(handPlaced, modelFolder)
    }

    private fun compare(handPlaced: Path, installed: Path) {
        val expected = describe(handPlaced)
        val actual = describe(installed)
        assertEquals("The entries differ", expected.keys, actual.keys)
        expected.forEach { (path, entry) -> assertEquals("$path differs in kind or content", entry.contentKey, actual.getValue(path).contentKey) }
        val differentPermissions = expected.filter { (path, entry) -> entry.permissions != actual.getValue(path).permissions }
        println("${expected.size} entries compared, ${differentPermissions.size} with other permissions: ${differentPermissions.keys.take(5)}")
    }

    private class Entry(val contentKey: String, val permissions: String)

    private fun describe(folder: Path): Map<String, Entry> = Files.walk(folder).use { paths ->
        paths.filter { it != folder }.toList().associate { path ->
            val isDirectory = Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)
            val key = if (isDirectory) "directory" else "file ${sha256Of(path)}"
            path.relativeTo(folder).toString() to Entry(key, PosixFilePermissions.toString(Files.getPosixFilePermissions(path)))
        }
    }

    private fun sha256Of(file: Path): String {
        val digest = MessageDigest.getInstance("SHA-256")
        Files.newInputStream(file).use { input ->
            val buffer = ByteArray(65_536)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun ModelPackage.copyWithAddress(newAddress: String) = ModelPackage(newAddress, sizeInBytes, unpackedSizeInBytes, sha256)

    companion object {
        @JvmField
        @ClassRule
        val application = ApplicationRule()
    }
}
