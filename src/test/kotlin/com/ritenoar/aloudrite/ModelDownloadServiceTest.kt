package com.ritenoar.aloudrite

import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.testFramework.ApplicationRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.ClassRule
import org.junit.Test
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths

class ModelDownloadServiceTest {

    private val modelFolder = Paths.get("/data/aloudrite/model")
    private val keptWorks = mutableListOf<() -> Unit>()
    private val reported = mutableListOf<Pair<InstallFailure, () -> Unit>>()
    private val installed = mutableListOf<Path>()
    private var installBehaviour: () -> Unit = {}
    private var modelInPlaceCount = 0

    private val service = ModelDownloadService(
        runInBackground = { work -> keptWorks += work },
        install = { folder, _: ProgressIndicator ->
            installed.add(folder)
            installBehaviour()
        },
        reportFailure = { failure, retry -> reported += failure to retry },
    )

    @Test
    fun startHandsOneWorkOverAndASecondStartMeanwhileHandsNothing() {
        assertTrue(service.start(modelFolder) { modelInPlaceCount++ })
        assertTrue(service.isRunning)

        assertFalse(service.start(modelFolder) { modelInPlaceCount++ })

        assertEquals(1, keptWorks.size)
        runKeptWork()
        assertFalse(service.isRunning)
    }

    @Test
    fun aDownloadThatSucceedsCallsBackOnceAndReportsNothing() {
        service.start(modelFolder) { modelInPlaceCount++ }

        runKeptWork()

        assertEquals(listOf(modelFolder), installed)
        assertEquals(1, modelInPlaceCount)
        assertTrue(reported.isEmpty())
    }

    @Test
    fun aFailureIsReportedOnceWithARetryThatDownloadsAgain() {
        installBehaviour = { throw ModelInstallException(InstallFailure.ServerAnswer(500)) }
        service.start(modelFolder) { modelInPlaceCount++ }

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
        service.start(modelFolder) { modelInPlaceCount++ }

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
        service.start(modelFolder) { modelInPlaceCount++ }

        runKeptWork()

        assertEquals(listOf<InstallFailure>(InstallFailure.Unexpected("Permission denied")), reported.map { it.first })
        assertEquals(0, modelInPlaceCount)
        assertFalse(service.isRunning)
    }

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
