package com.ritenoar.aloudrite

import com.intellij.testFramework.ApplicationRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import java.nio.file.Path

class SpeechModelManagementTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val modelsFolder: Path by lazy { temporaryFolder.newFolder().toPath().resolve("aloudrite") }
    private val int8 = INT8_MODEL
    private val fp32 = FP32_MODEL

    private var modelInUse = int8
    private var modelOfTheReading: SpeechModel? = null
    private val chosen = mutableListOf<SpeechModel>()
    private val questions = mutableListOf<String>()
    private var answer = true
    private val told = mutableListOf<String>()
    private val downloadWorks = mutableListOf<() -> Unit>()
    private val removalWorks = mutableListOf<() -> Unit>()
    private var modelsChanged = 0
    private var install: (SpeechModel, Path) -> Unit = { model, folder -> fillWithModelStandIn(folder, model) }

    private val downloads by lazy {
        ModelDownloadService(
            runInBackground = { work -> downloadWorks += work },
            install = { model, folder, _, _ -> install(model, folder) },
            reportFailure = { _, _ -> },
            onRunningChanged = { modelsChanged++ },
        )
    }

    private val management by lazy {
        SpeechModelManagement(
            modelInUse = { modelInUse },
            chooseModel = { model ->
                modelInUse = model
                chosen += model
            },
            downloads = downloads,
            readingModel = object : ReadingModel {
                override fun modelOfTheReading(): SpeechModel? = modelOfTheReading
            },
            modelsFolder = modelsFolder,
            ask = { question, _, _ ->
                questions += question
                answer
            },
            tell = { told += it },
            runInBackground = { work -> removalWorks += work },
            onModelsChanged = { modelsChanged++ },
        )
    }

    private fun folderOf(model: SpeechModel) = model.folderIn(modelsFolder)

    private fun putInPlace(model: SpeechModel) = fillWithModelStandIn(folderOf(model), model)

    private fun runDownload() {
        val work = downloadWorks.removeAt(0)
        work()
    }

    @Test
    fun aDownloadFromThePageAsksWithTheModelItsSizesAndItsFolderAndOnAYesStartsItAndLeavesTheModelInUse() {
        management.download(fp32)

        assertEquals(listOf(downloadQuestionFromThePage(fp32, folderOf(fp32))), questions)
        assertTrue(questions.single().startsWith("Download Kokoro 1.0, fp32?"))
        assertTrue(questions.single().contains("350 MB"))
        assertTrue(questions.single().contains("401 MB"))
        assertTrue(questions.single().contains(folderOf(fp32).toAbsolutePath().toString()))
        assertTrue(management.isDownloadRunning)

        runDownload()

        assertTrue(management.isInPlace(fp32))
        assertSame(int8, modelInUse)
        assertTrue(chosen.isEmpty())
        assertFalse(management.isDownloadRunning)
    }

    @Test
    fun theModelBeingDownloadedFromThePageIsKnownWhileItRunsOnly() {
        assertNull(management.downloadingModel)

        management.download(fp32)
        assertSame(fp32, management.downloadingModel)

        runDownload()
        assertNull(management.downloadingModel)
    }

    @Test
    fun aDeclinedDownloadStartsNothing() {
        answer = false

        management.download(fp32)

        assertEquals(1, questions.size)
        assertTrue(downloadWorks.isEmpty())
        assertFalse(management.isDownloadRunning)
    }

    @Test
    fun aDownloadIsRefusedWithoutAQuestionWhileOneRunsInThisIdeAndNothingStarts() {
        management.download(fp32)
        questions.clear()

        management.download(int8)

        assertTrue(questions.isEmpty())
        assertEquals(listOf(DOWNLOAD_RUNNING_HERE_MESSAGE), told)
        assertEquals(1, downloadWorks.size)
    }

    @Test
    fun aDownloadIsRefusedWithTheMessageOfAnotherWindowWhileAnotherProcessHoldsTheLock() {
        Files.createDirectories(modelsFolder)
        val lockFile = modelsFolder.toRealPath().resolve(InstallAttempt.LOCK_FILE_NAME)

        LockProbe.start("hold", lockFile).use { otherIde ->
            assertEquals("locked", otherIde.firstLine)
            management.download(fp32)
        }

        assertEquals(listOf(DOWNLOAD_RUNNING_ELSEWHERE_MESSAGE), told)
        assertTrue(questions.isEmpty())
        assertTrue(downloadWorks.isEmpty())
    }

    @Test
    fun aChoiceOfAModelInPlaceMakesItTheModelInUseWithNoQuestion() {
        putInPlace(fp32)

        assertTrue(management.choose(fp32))

        assertSame(fp32, modelInUse)
        assertTrue(questions.isEmpty())
    }

    @Test
    fun aDeclinedChoiceOfAModelNotInPlaceLeavesTheModelInUseAndStartsNothing() {
        answer = false

        assertFalse(management.choose(fp32))

        assertEquals(listOf(chooseQuestion(fp32, folderOf(fp32))), questions)
        assertSame(int8, modelInUse)
        assertTrue(downloadWorks.isEmpty())
        assertNull(management.pendingChoice)
    }

    @Test
    fun aChoiceOfAModelNotInPlaceKeepsTheModelInUseUntilTheDownloadEndsAndThenMakesItTheModelInUse() {
        assertTrue(management.choose(fp32))

        assertSame(int8, modelInUse)
        assertSame(fp32, management.pendingChoice)

        runDownload()

        assertSame(fp32, modelInUse)
        assertNull(management.pendingChoice)
    }

    @Test
    fun aFailedDownloadOfAChosenModelLeavesTheModelInUseAndNoPendingChoice() {
        install = { _, _ -> throw ModelInstallException(InstallFailure.ServerAnswer(500)) }
        management.choose(fp32)

        runDownload()

        assertSame(int8, modelInUse)
        assertTrue(chosen.isEmpty())
        assertNull(management.pendingChoice)
        assertFalse(management.isInPlace(fp32))
    }

    @Test
    fun aLaterChoiceOfAModelInPlaceMadeWhileTheDownloadRunsStaysTheModelInUseAfterTheDownloadEnds() {
        management.choose(fp32)
        assertSame(fp32, management.pendingChoice)
        putInPlace(int8)

        management.choose(int8)
        runDownload()

        assertSame(int8, modelInUse)
        assertEquals(listOf(int8), chosen)
        assertNull(management.pendingChoice)
    }

    @Test
    fun aRemovalOfAModelInPlaceAndNotInUseAsksWithTheMegabytesItFreesAndOnAYesRemovesTheFolderAndKeepsTheLockFile() {
        putInPlace(fp32)

        management.remove(fp32)

        assertEquals(listOf(removeQuestion(fp32, folderOf(fp32))), questions)
        assertTrue(questions.single().contains("401 MB"))
        assertFalse(Files.exists(folderOf(fp32)))
        assertFalse(management.isInPlace(fp32))
        removalWorks.forEach { it() }
        assertEquals(listOf(InstallAttempt.LOCK_FILE_NAME), Files.list(modelsFolder).use { it.map { path -> path.fileName.toString() }.toList() })
        assertTrue(modelsChanged > 0)
    }

    @Test
    fun theLockIsFreeAgainOnceTheRemovalHasFinished() {
        putInPlace(fp32)
        management.remove(fp32)
        removalWorks.forEach { it() }

        InstallAttempt.claim(modelsFolder)!!.close()
    }

    @Test
    fun aDeclinedRemovalKeepsTheFolder() {
        putInPlace(fp32)
        answer = false

        management.remove(fp32)

        assertTrue(management.isInPlace(fp32))
        assertTrue(removalWorks.isEmpty())
    }

    @Test
    fun theModelInUseAndTheModelOfTheCurrentReadingCannotBeRemovedAndNothingIsAsked() {
        putInPlace(int8)
        putInPlace(fp32)
        modelOfTheReading = fp32

        assertFalse(management.canRemove(int8))
        assertFalse(management.canRemove(fp32))
        management.remove(int8)
        management.remove(fp32)

        assertTrue(questions.isEmpty())
        assertEquals(listOf(removeRefusedMessage(int8), removeRefusedMessage(fp32)), told)
        assertTrue(management.isInPlace(int8))
        assertTrue(management.isInPlace(fp32))
    }

    @Test
    fun aModelInPlaceThatIsNotInUseCanBeRemovedAndOneNotInPlaceCannot() {
        putInPlace(fp32)

        assertTrue(management.canRemove(fp32))
        assertFalse(management.canRemove(int8))
    }

    @Test
    fun aRemovalWhileADownloadRunsIsRefusedAndTheFolderStays() {
        putInPlace(fp32)
        management.download(int8)
        questions.clear()

        management.remove(fp32)

        assertEquals(listOf(DOWNLOAD_RUNNING_HERE_MESSAGE), told)
        assertTrue(management.isInPlace(fp32))
        assertTrue(removalWorks.isEmpty())
    }

    @Test
    fun aFolderThatDoesNotHoldACompleteModelIsNotRemoved() {
        Files.createDirectories(folderOf(fp32))
        Files.createFile(folderOf(fp32).resolve("mine.txt"))

        management.remove(fp32)

        assertEquals(listOf(removeRefusedNotThePluginsMessage(folderOf(fp32))), told)
        assertTrue(Files.exists(folderOf(fp32).resolve("mine.txt")))
        assertTrue(removalWorks.isEmpty())
        InstallAttempt.claim(modelsFolder)!!.close()
    }

    @Test
    fun theModelsAreToldAsChangedWhenADownloadStartsAndEndsEitherWayAndAfterARemoval() {
        management.download(fp32)
        assertEquals(1, modelsChanged)
        runDownload()
        assertEquals(2, modelsChanged)

        install = { _, _ -> throw ModelInstallException(InstallFailure.ServerAnswer(500)) }
        management.download(int8)
        runDownload()
        assertEquals(4, modelsChanged)

        management.remove(fp32)
        assertEquals(5, modelsChanged)
    }

    companion object {
        @JvmField
        @ClassRule
        val application = ApplicationRule()
    }
}
