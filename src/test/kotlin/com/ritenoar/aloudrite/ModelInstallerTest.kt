package com.ritenoar.aloudrite

import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.util.ProgressIndicatorBase
import com.intellij.testFramework.ApplicationRule
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import java.nio.file.Path

class ModelInstallerTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val standIn = ModelPackageStandIn.create()
    private val server = PackageServerStandIn()
    private lateinit var dataFolder: Path
    private lateinit var parent: Path
    private lateinit var modelFolder: Path

    @Before
    fun prepareFolders() {
        dataFolder = Files.createDirectory(temporaryFolder.newFolder().toPath().resolve("data"))
        parent = dataFolder.resolve("aloudrite")
        modelFolder = parent.resolve(MODEL_PACKAGE_NAME)
    }

    private val LOCK_FILE_NAME = InstallAttempt.LOCK_FILE_NAME

    @After
    fun stopServer() = server.close()

    @Test
    fun theModelIsPutInPlaceAsWholeAndNothingElseIsLeft() {
        server.serve("/package", standIn.bytes)

        install(standIn.modelPackage(server.address("/package")), IndicatorThatChecksTheModelFolder())

        assertTrue(isModelInPlace(modelFolder))
        assertEquals(standIn.expectedTree, ModelPackageStandIn.readTree(modelFolder))
        assertEquals(listOf(LOCK_FILE_NAME, MODEL_PACKAGE_NAME), namesIn(parent))
    }

    @Test
    fun aModelAlreadyInPlaceIsLeftAsItIsAndNothingIsRequested() {
        server.serve("/package", standIn.bytes)
        fillWithModelStandIn(modelFolder)
        val before = ModelPackageStandIn.readTree(modelFolder)

        install(standIn.modelPackage(server.address("/package")))

        assertEquals(0, server.requestCount("/package"))
        assertEquals(before, ModelPackageStandIn.readTree(modelFolder))
        assertEquals(listOf(LOCK_FILE_NAME, MODEL_PACKAGE_NAME), namesIn(parent))
    }

    @Test
    fun aFolderInTheWayIsNeverTouchedAndNothingIsRequested() {
        server.serve("/package", standIn.bytes)
        Files.createDirectories(modelFolder)
        Files.write(modelFolder.resolve(MODEL_FILE_NAME), byteArrayOf(1, 2, 3))

        val failure = installAndExpectFailure(standIn.modelPackage(server.address("/package")))

        assertEquals(InstallFailure.FolderInTheWay(modelFolder), failure)
        assertEquals(0, server.requestCount("/package"))
        assertEquals(listOf(MODEL_FILE_NAME), namesIn(modelFolder))
        assertFalse(isModelInPlace(modelFolder))
    }

    @Test
    fun anEmptyFolderInTheWayIsReplaced() {
        server.serve("/package", standIn.bytes)
        Files.createDirectories(modelFolder)

        install(standIn.modelPackage(server.address("/package")))

        assertEquals(standIn.expectedTree, ModelPackageStandIn.readTree(modelFolder))
    }

    @Test
    fun cancellingDuringTheDownloadLeavesNothing() {
        server.serve("/package", standIn.bytes)
        val indicator = IndicatorThatChecksTheModelFolder { fraction, text ->
            if (text == "Downloading the speech model" && fraction >= 0.5) cancel()
        }

        assertCancelled { install(standIn.modelPackage(server.address("/package")), indicator) }

        assertNothingLeft()
    }

    @Test
    fun cancellingDuringTheUnpackingLeavesNothing() {
        server.serve("/package", standIn.bytes)
        val indicator = IndicatorThatChecksTheModelFolder { fraction, text ->
            if (text == "Unpacking the speech model" && fraction > 0.0) cancel()
        }

        assertCancelled { install(standIn.modelPackage(server.address("/package")), indicator) }

        assertNothingLeft()
    }

    @Test
    fun aBodyCutOffLeavesNothing() {
        server.serveCutOff("/package", standIn.bytes, standIn.bytes.size / 2)

        val failure = installAndExpectFailure(standIn.modelPackage(server.address("/package")))

        assertTrue(failure.toString(), failure is InstallFailure.BrokeOff)
        assertNothingLeft()
    }

    @Test
    fun aChecksumThatDoesNotMatchLeavesNothingAndUnpacksNothing() {
        server.serve("/package", standIn.bytes)
        val modelPackage = ModelPackage(server.address("/package"), standIn.bytes.size.toLong(), 1L, "0".repeat(64))

        val failure = installAndExpectFailure(modelPackage)

        assertEquals(InstallFailure.ChecksumMismatch(modelPackage.address), failure)
        assertNothingLeft()
    }

    @Test
    fun aPackageThatIsCutOffButMatchesItsChecksumCannotBeUnpacked() {
        val cutOff = standIn.cutOffAtHalf()
        server.serve("/package", cutOff.bytes)

        val failure = installAndExpectFailure(cutOff.modelPackage(server.address("/package")))

        assertTrue(failure.toString(), failure is InstallFailure.CannotUnpack)
        assertNothingLeft()
    }

    @Test
    fun aPackageWithoutALexiconCannotBeUnpackedToAModel() {
        val withoutLexicon = standIn.without(LEXICON_FILE_NAME)
        server.serve("/package", withoutLexicon.bytes)

        val failure = installAndExpectFailure(withoutLexicon.modelPackage(server.address("/package")))

        assertTrue(failure.toString(), failure is InstallFailure.CannotUnpack)
        assertNothingLeft()
    }

    @Test
    fun aFailureKeepsAParentThatExistedBefore() {
        server.answerWith("/package", 404)
        Files.createDirectories(parent)

        val failure = installAndExpectFailure(standIn.modelPackage(server.address("/package")))

        assertEquals(InstallFailure.ServerAnswer(404), failure)
        assertTrue(Files.isDirectory(parent))
        assertEquals(listOf(LOCK_FILE_NAME), namesIn(parent))
    }

    @Test
    fun aLeftoverOfAKilledAttemptIsRemovedByTheNextOne() {
        server.serve("/package", standIn.bytes)
        val leftover = Files.createDirectories(parent.resolve("${InstallAttempt.DIRECTORY_PREFIX}old/unpacked"))
        Files.write(leftover.parent.resolve(MODEL_PACKAGE_NAME + ".tar.bz2"), byteArrayOf(1, 2, 3))
        Files.createFile(leftover.parent.resolve("lock"))

        install(standIn.modelPackage(server.address("/package")))

        assertEquals(listOf(LOCK_FILE_NAME, MODEL_PACKAGE_NAME), namesIn(parent))
    }

    @Test
    fun aModelThatAppearsMeanwhileCountsAsSuccessAndIsTheOneInPlace() {
        server.serve("/package", standIn.bytes)
        val indicator = IndicatorThatChecksTheModelFolder(checkFolder = false) { fraction, text ->
            if (text == "Unpacking the speech model" && fraction > 0.0 && !Files.exists(modelFolder)) {
                fillWithModelStandIn(modelFolder)
            }
        }

        install(standIn.modelPackage(server.address("/package")), indicator)

        assertTrue(isModelInPlace(modelFolder))
        assertEquals(0L, Files.size(modelFolder.resolve(MODEL_FILE_NAME)))
        assertEquals(listOf(LOCK_FILE_NAME, MODEL_PACKAGE_NAME), namesIn(parent))
    }

    private fun install(modelPackage: ModelPackage, indicator: ProgressIndicator = ProgressIndicatorBase()) =
        ModelInstaller(modelPackage).installClaiming(modelFolder, indicator)

    private fun installAndExpectFailure(modelPackage: ModelPackage): InstallFailure {
        try {
            install(modelPackage)
        } catch (exception: ModelInstallException) {
            return exception.failure
        }
        throw AssertionError("The installation did not fail")
    }

    private fun assertCancelled(work: () -> Unit) {
        try {
            work()
            fail("The installation was not cancelled")
        } catch (_: ProcessCanceledException) {
        }
    }

    // Only the lock file stays, the one exception to leaving nothing
    private fun assertNothingLeft() {
        assertFalse(isModelInPlace(modelFolder))
        assertFalse(Files.exists(modelFolder))
        assertEquals(listOf(LOCK_FILE_NAME), namesIn(parent))
    }

    private fun namesIn(directory: Path): List<String> =
        Files.list(directory).use { entries -> entries.map { it.fileName.toString() }.toList().sorted() }

    // Asserts at every fraction that no model folder exists yet, which is what makes the rename the only moment
    private inner class IndicatorThatChecksTheModelFolder(
        private val checkFolder: Boolean = true,
        private val onFraction: ProgressIndicatorBase.(fraction: Double, text: String) -> Unit = { _, _ -> },
    ) : ProgressIndicatorBase() {
        override fun setFraction(fraction: Double) {
            super.setFraction(fraction)
            if (checkFolder) assertFalse("The model folder exists at $fraction", Files.exists(modelFolder))
            onFraction(fraction, text)
        }
    }

    companion object {
        @JvmField
        @ClassRule
        val application = ApplicationRule()
    }
}
