package com.ritenoar.aloudrite

import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.testFramework.ApplicationRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import java.nio.file.Path

class PackageDownloadTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val standIn = ModelPackageStandIn.create()

    @Test
    fun anAbsoluteHttpsLocationIsTakenAsItIs() {
        assertEquals(
            "https://objects.example.com/a/b.tar.bz2",
            redirectTarget("https://example.com/x", "https://objects.example.com/a/b.tar.bz2"))
    }

    @Test
    fun aRelativeLocationIsResolvedAgainstTheAddressThatSentIt() {
        assertEquals("https://example.com/other/b.tar.bz2", redirectTarget("https://example.com/x/y", "/other/b.tar.bz2"))
    }

    @Test
    fun aLocationThatIsNotHttpsIsRefused() {
        listOf("http://example.com/b", "ftp://example.com/b", null).forEach { location ->
            val exception = failureOf { redirectTarget("https://example.com/x", location) }

            assertTrue("$location: ${exception.failure}", exception.failure is InstallFailure.RedirectRefused)
        }
    }

    @Test
    fun aWriteFailsForLackOfDiskSpaceOnTheMessageOrOnTheFreeBytes() {
        assertTrue(isOutOfDiskSpace("No space left on device", 500_000_000L))
        assertTrue(isOutOfDiskSpace("Other message", 9_999_999L))
        assertFalse(isOutOfDiskSpace("Other message", 10_000_000L))
        assertFalse(isOutOfDiskSpace(null, 10_000_000L))
    }

    @Test
    fun thePackageArrivesWholeAndItsChecksumIsReturned() {
        PackageServerStandIn().use { server ->
            server.serve("/package", standIn.bytes)
            val target = targetFile()

            val sha256 = downloadPackage(standIn.modelPackage(server.address("/package")), target, EmptyProgressIndicator())

            assertEquals(standIn.sha256, sha256)
            assertTrue(standIn.bytes.contentEquals(Files.readAllBytes(target)))
        }
    }

    @Test
    fun aRedirectToAnHttpAddressFailsAndTheAddressIsNeverRequested() {
        PackageServerStandIn().use { server ->
            server.redirect("/package", server.address("/elsewhere"))
            server.serve("/elsewhere", standIn.bytes)

            val exception = failureOf {
                downloadPackage(standIn.modelPackage(server.address("/package")), targetFile(), EmptyProgressIndicator())
            }

            assertEquals(InstallFailure.RedirectRefused(server.address("/elsewhere")), exception.failure)
            assertEquals(0, server.requestCount("/elsewhere"))
        }
    }

    @Test
    fun anAnswerThatIsNotTheFileFailsWithItsStatus() {
        PackageServerStandIn().use { server ->
            server.answerWith("/package", 404)

            val exception = failureOf {
                downloadPackage(standIn.modelPackage(server.address("/package")), targetFile(), EmptyProgressIndicator())
            }

            assertEquals(InstallFailure.ServerAnswer(404), exception.failure)
        }
    }

    @Test
    fun aRedirectIsFollowedToThePackage() {
        PackageServerStandIn().use { server ->
            server.redirect("/package", "/elsewhere")
            server.serve("/elsewhere", standIn.bytes)

            val sha256 = downloadPackage(
                standIn.modelPackage(server.address("/package")), targetFile(), EmptyProgressIndicator(), acceptAnyRedirect)

            assertEquals(standIn.sha256, sha256)
        }
    }

    @Test
    fun elevenRedirectsInARowFail() {
        PackageServerStandIn().use { server ->
            server.redirect("/package", "/package")

            val exception = failureOf {
                downloadPackage(
                    standIn.modelPackage(server.address("/package")), targetFile(), EmptyProgressIndicator(), acceptAnyRedirect)
            }

            assertEquals(InstallFailure.TooManyRedirects, exception.failure)
            assertEquals(11, server.requestCount("/package"))
        }
    }

    @Test
    fun tenRedirectsInARowAreFollowed() {
        PackageServerStandIn().use { server ->
            (0 until 10).forEach { step -> server.redirect("/step$step", "/step${step + 1}") }
            server.serve("/step10", standIn.bytes)

            val sha256 = downloadPackage(
                standIn.modelPackage(server.address("/step0")), targetFile(), EmptyProgressIndicator(), acceptAnyRedirect)

            assertEquals(standIn.sha256, sha256)
        }
    }

    @Test
    fun aBodyCutOffHalfwayFailsWithTheBytesThatArrived() {
        PackageServerStandIn().use { server ->
            val half = standIn.bytes.size / 2
            server.serveCutOff("/package", standIn.bytes, half)

            val exception = failureOf {
                downloadPackage(standIn.modelPackage(server.address("/package")), targetFile(), EmptyProgressIndicator())
            }

            val failure = exception.failure as InstallFailure.BrokeOff
            assertEquals(standIn.bytes.size.toLong(), failure.totalBytes)
            assertTrue("${failure.receivedBytes} of $half", failure.receivedBytes in 1..half.toLong())
        }
    }

    @Test
    fun aBodyLongerThanThePackageFailsAsAChecksumThatDoesNotMatch() {
        PackageServerStandIn().use { server ->
            server.serve("/package", standIn.bytes)
            val shorterPackage = ModelPackage(server.address("/package"), standIn.bytes.size - 10L, 1L, standIn.sha256)

            val exception = failureOf { downloadPackage(shorterPackage, targetFile(), EmptyProgressIndicator()) }

            assertEquals(InstallFailure.ChecksumMismatch(shorterPackage.address), exception.failure)
        }
    }

    @Test
    fun anAddressNobodyListensOnFailsAsNoConnection() {
        val address = PackageServerStandIn().use { it.address("/package") }

        val exception = failureOf { downloadPackage(standIn.modelPackage(address), targetFile(), EmptyProgressIndicator()) }

        assertEquals(InstallFailure.NoConnection("127.0.0.1"), exception.failure)
    }

    @Test
    fun anIndicatorCancelledHalfwayStopsTheDownload() {
        PackageServerStandIn().use { server ->
            server.serve("/package", standIn.bytes)
            val cancelledAtHalf = object : EmptyProgressIndicator() {
                override fun setFraction(fraction: Double) {
                    super.setFraction(fraction)
                    if (fraction >= 0.5) cancel()
                }
            }

            try {
                downloadPackage(standIn.modelPackage(server.address("/package")), targetFile(), cancelledAtHalf)
                fail("The download was not cancelled")
            } catch (_: ProcessCanceledException) {
            }
        }
    }

    private val acceptAnyRedirect = { current: String, location: String? ->
        java.net.URI(current).resolve(location!!).toString()
    }

    private fun targetFile(): Path = temporaryFolder.newFolder().toPath().resolve("package.tar.bz2")

    private fun failureOf(work: () -> Unit): ModelInstallException {
        try {
            work()
        } catch (exception: ModelInstallException) {
            return exception
        }
        throw AssertionError("No ModelInstallException was thrown")
    }

    companion object {
        @JvmField
        @ClassRule
        val application = ApplicationRule()
    }
}
