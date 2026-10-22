package com.ritenoar.aloudrite

import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.testFramework.ApplicationRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

class ModelDownloadServiceTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val modelFolder: Path by lazy { temporaryFolder.newFolder().toPath().resolve("aloudrite").resolve(INT8_MODEL.packageName) }
    private val modelsFolder: Path get() = modelFolder.parent
    private val keptWorks = mutableListOf<() -> Unit>()
    private val reported = mutableListOf<Pair<InstallFailure, () -> Unit>>()
    private val installed = mutableListOf<Path>()
    private var installBehaviour: () -> Unit = {}
    private var modelInPlaceCount = 0

    private val service = ModelDownloadService(
        runInBackground = { work -> keptWorks += work },
        install = { _: SpeechModel, folder, _: InstallAttempt, _: ProgressIndicator ->
            installed.add(folder)
            installBehaviour()
        },
        reportFailure = { failure, retry -> reported += failure to retry },
        onRunningChanged = { },
    )

    @Test
    fun startHandsOneWorkOverAndASecondStartMeanwhileHandsNothing() {
        assertTrue(startDownload { modelInPlaceCount++ })
        assertTrue(service.isRunning)

        val otherFolder = temporaryFolder.newFolder().toPath().resolve("other")
        val otherAttempt = service.claim(otherFolder)!!
        assertFalse(service.start(INT8_MODEL, otherFolder, otherAttempt) { modelInPlaceCount++ })

        service.claim(otherFolder)!!.close()
        assertEquals(1, keptWorks.size)
        runKeptWork()
        assertFalse(service.isRunning)
    }

    @Test
    fun theModelOfTheDownloadThatRunsIsKnownWhoeverStartedItAndNullOtherwise() {
        assertNull(service.downloadingModel)

        startDownload { modelInPlaceCount++ }
        assertSame(INT8_MODEL, service.downloadingModel)

        runKeptWork()
        assertNull(service.downloadingModel)
    }

    @Test
    fun aDownloadThatSucceedsCallsBackOnceAndReportsNothing() {
        startDownload { modelInPlaceCount++ }

        runKeptWork()

        assertEquals(listOf(modelFolder), installed)
        assertEquals(1, modelInPlaceCount)
        assertTrue(reported.isEmpty())
    }

    @Test
    fun aFailureIsReportedOnceWithARetryThatDownloadsAgain() {
        installBehaviour = { throw ModelInstallException(InstallFailure.ServerAnswer(500)) }
        startDownload { modelInPlaceCount++ }

        runKeptWork()

        assertEquals(listOf(InstallFailure.ServerAnswer(500)), reported.map { it.first })
        assertEquals(0, modelInPlaceCount)
        assertFalse(service.isRunning)

        installBehaviour = {}
        reported.single().second()
        assertEquals(2, keptWorks.size)
        runKeptWork()
        assertEquals(1, modelInPlaceCount)
        assertEquals(1, reported.size)
    }

    @Test
    fun aCancellationIsRethrownAndReportsNothing() {
        installBehaviour = { throw ProcessCanceledException() }
        startDownload { modelInPlaceCount++ }

        try {
            runKeptWork()
            fail("The cancellation was swallowed")
        } catch (_: ProcessCanceledException) {
        }

        assertTrue(reported.isEmpty())
        assertEquals(0, modelInPlaceCount)
        assertFalse(service.isRunning)
    }

    @Test
    fun anIoFailureIsReportedAsUnexpected() {
        installBehaviour = { throw IOException("Permission denied") }
        startDownload { modelInPlaceCount++ }

        runKeptWork()

        assertEquals(listOf<InstallFailure>(InstallFailure.Unexpected("Permission denied")), reported.map { it.first })
        assertEquals(0, modelInPlaceCount)
        assertFalse(service.isRunning)
    }

    @Test
    fun theLockIsHeldFromTheClaimUntilTheWorkEndsAndThenReleased() {
        val lockFile = modelFolder.parent.resolve(InstallAttempt.LOCK_FILE_NAME)
        startDownload { modelInPlaceCount++ }

        assertEquals(null, service.claim(modelsFolder))
        runKeptWork()

        service.claim(modelsFolder)!!.close()
        assertTrue(Files.exists(lockFile))
    }

    @Test
    fun theLockIsReleasedAfterAFailureAndAfterACancellation() {
        installBehaviour = { throw ModelInstallException(InstallFailure.ServerAnswer(500)) }
        startDownload { modelInPlaceCount++ }
        runKeptWork()
        service.claim(modelsFolder)!!.close()

        installBehaviour = { throw ProcessCanceledException() }
        startDownload { modelInPlaceCount++ }
        try {
            runKeptWork()
        } catch (_: ProcessCanceledException) {
        }

        service.claim(modelsFolder)!!.close()
    }

    @Test
    fun aRetryWhileTheLockIsHeldElsewhereReportsThatAndOffersToTryAgain() {
        installBehaviour = { throw ModelInstallException(InstallFailure.ServerAnswer(500)) }
        startDownload { modelInPlaceCount++ }
        runKeptWork()
        val heldElsewhere = service.claim(modelsFolder)!!

        reported.single().second()

        assertEquals(listOf(InstallFailure.ServerAnswer(500), InstallFailure.RunningElsewhere), reported.map { it.first })
        assertEquals(1, keptWorks.size)
        heldElsewhere.close()
    }

    private fun startDownload(onModelInPlace: () -> Unit): Boolean =
        service.start(INT8_MODEL, modelsFolder, checkNotNull(service.claim(modelsFolder)), onModelInPlace)

    private fun runKeptWork() {
        val work = keptWorks.last()
        ProgressManager.getInstance().runProcess(Runnable { work() }, EmptyProgressIndicator())
    }

    companion object {
        @JvmField
        @ClassRule
        val application = ApplicationRule()
    }
}
