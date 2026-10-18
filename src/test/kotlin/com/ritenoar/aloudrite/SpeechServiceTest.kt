package com.ritenoar.aloudrite

import com.intellij.openapi.project.ProjectManager
import com.intellij.testFramework.ApplicationRule
import com.intellij.testFramework.LightVirtualFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.ClassRule
import org.junit.Test
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

class SpeechServiceTest {

    private val modelFolder: Path = Paths.get("/model")
    private val standIns = SpeechStandIns()
    private val service = standIns.service
    private val target = ReadingTarget(null, LightVirtualFile("notes.md", "text"))
    private val otherTarget = ReadingTarget(null, LightVirtualFile("other.md", "text"))

    private fun read(text: String = "One sentence.", readingTarget: ReadingTarget = target) =
        service.read(readingTarget, text, modelFolder, System.nanoTime())

    @Test
    fun readReturnsAtOnceAndOnlyHandsWorkOver() {
        read()

        assertEquals(1, standIns.keptWorks.size)
        assertTrue(standIns.engines.isEmpty())
        assertTrue(standIns.players.isEmpty())
        assertTrue(service.isReading(target))
        assertFalse(service.isReading(otherTarget))
    }

    @Test
    fun theEngineIsGivenThePiecesWithoutWhiteSpaceOnlyAndAllTheirSamplesArePlayedInSlices() {
        val text = "# Title\n\nFirst sentence. Second sentence!\n\n  \n\nLast."

        read(text)
        standIns.runKeptWorks()

        val expectedTexts = piecesOf(text).filter { it.isNotBlank() }
        assertEquals(listOf("# Title\n\n", "First sentence. ", "Second sentence!\n\n  \n\n", "Last."), expectedTexts)
        assertEquals(expectedTexts, standIns.textsMade)
        val expectedSamples = (1..expectedTexts.size).flatMap { textNumber ->
            List(SAMPLES_PER_TEXT) { textNumber.toFloat() }
        }
        assertEquals(expectedSamples, standIns.samplesPlayed())
        assertTrue(standIns.players.single().played.all { it.size <= STAND_IN_SAMPLE_RATE / 10 })
        assertTrue(standIns.failures.isEmpty())
    }

    @Test
    fun theAudioOfAPieceCanBePlayedBeforeTheNextPieceIsMade() {
        var samplesPlayedWhileTheSecondPieceIsMade = -1
        var playing: Thread? = null
        standIns.whileMaking = { text ->
            if (text == "Two.") {
                val playingWork = standIns.keptWorks.removeAt(0)
                playing = Thread { playingWork() }.also { it.start() }
                waitUntil { standIns.samplesPlayed().size >= SAMPLES_PER_TEXT }
                samplesPlayedWhileTheSecondPieceIsMade = standIns.samplesPlayed().size
            }
        }
        read("One. Two.")

        standIns.runNextKeptWork()
        playing!!.join(TimeUnit.SECONDS.toMillis(5))

        assertEquals(SAMPLES_PER_TEXT, samplesPlayedWhileTheSecondPieceIsMade)
        assertEquals(2 * SAMPLES_PER_TEXT, standIns.samplesPlayed().size)
        assertTrue(standIns.players.single().closed)
    }

    @Test
    fun theEndOfTheTextClosesThePlayerAndEndsTheReading() {
        read()
        val changesAtTheStart = standIns.changes.get()

        standIns.runKeptWorks()

        val player = standIns.players.single()
        assertTrue(player.closed)
        assertFalse(player.discarded)
        assertFalse(service.isReading(target))
        assertEquals(changesAtTheStart + 1, standIns.changes.get())
    }

    @Test
    fun stopEndsTheReadingAtOnceAndTheEngineIsAnsweredStopAtItsNextPiece() {
        read("One. Two. Three.")
        val changesAtTheStart = standIns.changes.get()
        standIns.whileMaking = { text ->
            if (text == "Two. ") {
                service.stop(target)
                assertFalse(service.isReading(target))
                assertEquals(changesAtTheStart + 1, standIns.changes.get())
            }
        }

        standIns.runKeptWorks()

        assertEquals(listOf("One. ", "Two. "), standIns.textsMade)
        assertEquals(listOf(true, true, false), standIns.engines.single().answers)
        val player = standIns.players.single()
        assertTrue(player.played.isEmpty())
        assertTrue(player.discarded)
        assertFalse(player.closed)
        assertEquals(changesAtTheStart + 1, standIns.changes.get())
        assertTrue(standIns.failures.isEmpty())
    }

    @Test
    fun aStopWhilePlayingPlaysNoFurtherSliceAndDiscardsThePlayer() {
        read("One.")
        standIns.whilePlaying = { slicesPlayed -> if (slicesPlayed == 2) service.stop(target) }

        standIns.runKeptWorks()

        val player = standIns.players.single()
        assertEquals(2, player.played.size)
        assertTrue(player.discarded)
        assertFalse(player.closed)
        assertFalse(service.isReading(target))
    }

    @Test
    fun readOfAnotherTargetStopsTheFirstAndReadsTheSecondOnceTheFirstLetTheEngineGo() {
        var secondEngineWork: Thread? = null
        var textsWhileTheFirstHeldTheEngine = emptyList<String>()
        standIns.whileMaking = { text ->
            if (text == "First. ") {
                read("Second.", otherTarget)
                assertFalse(service.isReading(target))
                assertTrue(service.isReading(otherTarget))
                // The kept works are now the playing of the first and the engine work of the second
                val engineWorkOfTheSecond = standIns.keptWorks.removeAt(1)
                secondEngineWork = Thread { engineWorkOfTheSecond() }.also { it.start() }
                Thread.sleep(200)
                textsWhileTheFirstHeldTheEngine = standIns.textsMade.toList()
            }
        }
        read("First. More.")

        standIns.runNextKeptWork()
        secondEngineWork!!.join(TimeUnit.SECONDS.toMillis(5))
        standIns.runKeptWorks()

        assertEquals(listOf("First. "), textsWhileTheFirstHeldTheEngine)
        assertEquals(listOf("First. ", "Second."), standIns.textsMade)
        assertTrue(standIns.players.first().discarded)
        assertTrue(standIns.players.last().closed)
        assertEquals(List(SAMPLES_PER_TEXT) { 2f }, standIns.players.last().played.flatMap { it.toList() })
        assertFalse(service.isReading(otherTarget))
    }

    @Test
    fun theEngineIsGivenExactlyTheTextHandedToRead() {
        val text = "Some *Markdown* with `code` and [a link](https://example.com)."

        read(text)
        standIns.runKeptWorks()

        assertEquals(text, standIns.textsMade.joinToString(""))
    }

    @Test
    fun aFailureOfTheEngineAtTheFirstPieceEndsTheReading() {
        standIns.engineFailsAt = { it == "One. " }

        assertAFailureEndsTheReading(expectAPlayer = true)
    }

    @Test
    fun aFailureOfTheEngineAtALaterPieceEndsTheReading() {
        standIns.engineFailsAt = { it == "Two." }

        assertAFailureEndsTheReading(expectAPlayer = true)
    }

    @Test
    fun aFailureOfMakingTheEngineEndsTheReading() {
        standIns.makingTheEngineFails = IllegalStateException("no engine")

        assertAFailureEndsTheReading(expectAPlayer = false)
    }

    @Test
    fun aFailureOfThePlayerEndsTheReading() {
        standIns.playerFails = IllegalStateException("no audio device")

        assertAFailureEndsTheReading(expectAPlayer = true)
    }

    @Test
    fun aFailureOfThePlayerAtTheEndOfTheTextEndsTheReading() {
        standIns.closingThePlayerFails = IllegalStateException("drain failed")
        read("One.")
        val changesAtTheStart = standIns.changes.get()

        standIns.runKeptWorks()

        assertSame(target, standIns.failures.single().first)
        assertFalse(service.isReading(target))
        assertEquals(changesAtTheStart + 1, standIns.changes.get())
        standIns.closingThePlayerFails = null
        read("Again.")
        standIns.runKeptWorks()
        assertTrue(standIns.players.last().closed)
        assertEquals(1, standIns.failures.size)
    }

    @Test
    fun aStopWhileThePlayerPlaysOutTheEndOfTheTextDiscardsIt() {
        read("One.")
        var discardedDuringTheClose = false
        standIns.whileClosing = {
            service.stop(target)
            discardedDuringTheClose = standIns.players.single().discarded
        }

        standIns.runKeptWorks()

        assertTrue(discardedDuringTheClose)
        assertFalse(service.isReading(target))
        assertTrue(standIns.failures.isEmpty())
    }

    private fun assertAFailureEndsTheReading(expectAPlayer: Boolean) {
        read("One. Two.")
        val changesAtTheStart = standIns.changes.get()

        standIns.runKeptWorks()

        assertSame(target, standIns.failures.single().first)
        assertFalse(service.isReading(target))
        if (expectAPlayer) assertTrue(standIns.players.single().discarded) else assertTrue(standIns.players.isEmpty())
        assertEquals(changesAtTheStart + 1, standIns.changes.get())

        standIns.engineFailsAt = { false }
        standIns.makingTheEngineFails = null
        standIns.playerFails = null
        standIns.closingThePlayerFails = null
        standIns.textsMade.clear()
        read("Again.")
        standIns.runKeptWorks()
        assertEquals(listOf("Again."), standIns.textsMade)
        assertTrue(standIns.players.last().closed)
        assertEquals(1, standIns.failures.size)
    }

    @Test
    fun closingTheFileStopsItsReadingAndNoOther() {
        read()

        service.fileClosed(otherTarget)
        assertTrue(service.isReading(target))

        service.fileClosed(target)
        assertFalse(service.isReading(target))
        standIns.runKeptWorks()
        assertTrue(standIns.players.isEmpty() || standIns.players.single().played.isEmpty())
    }

    @Test
    fun closingTheProjectStopsItsReadingAndNoOther() {
        val project = ProjectManager.getInstance().defaultProject
        val projectTarget = ReadingTarget(project, target.file)
        service.read(projectTarget, "One sentence.", modelFolder, System.nanoTime())

        service.projectClosed(null)
        assertTrue(service.isReading(projectTarget))

        service.projectClosed(project)
        assertFalse(service.isReading(projectTarget))

        read()
        service.projectClosed(project)
        assertTrue(service.isReading(target))
        service.projectClosed(null)
        assertFalse(service.isReading(target))
    }

    @Test
    fun theReadingOnceTheModelIsInPlaceReadsTheTextOfThePressOnce() {
        val readTheText = service.readOnceTheModelIsInPlace(target, "The text of the press.", modelFolder, System.nanoTime())
        assertFalse(service.isReading(target))

        readTheText()
        assertTrue(service.isReading(target))
        standIns.runKeptWorks()
        readTheText()
        standIns.runKeptWorks()

        assertEquals(listOf("The text of the press."), standIns.textsMade)
    }

    @Test
    fun theReadingOnceTheModelIsInPlaceReadsNothingAfterItsFileWasClosed() {
        val readTheText = service.readOnceTheModelIsInPlace(target, "Text.", modelFolder, System.nanoTime())

        service.fileClosed(target)
        readTheText()
        readTheText()

        assertNothingIsRead()
    }

    @Test
    fun theReadingOnceTheModelIsInPlaceReadsNothingAfterItsProjectWasClosed() {
        val readTheText = service.readOnceTheModelIsInPlace(target, "Text.", modelFolder, System.nanoTime())

        service.projectClosed(null)
        readTheText()
        readTheText()

        assertNothingIsRead()
    }

    @Test
    fun aSecondReadingOnceTheModelIsInPlaceReplacesTheFirst() {
        val readTheFirst = service.readOnceTheModelIsInPlace(target, "First.", modelFolder, System.nanoTime())
        val readTheSecond = service.readOnceTheModelIsInPlace(otherTarget, "Second.", modelFolder, System.nanoTime())

        readTheFirst()
        assertNothingIsRead()
        readTheSecond()
        standIns.runKeptWorks()

        assertEquals(listOf("Second."), standIns.textsMade)
    }

    private fun assertNothingIsRead() {
        assertFalse(service.isReading(target))
        assertTrue(standIns.keptWorks.isEmpty())
        assertTrue(standIns.textsMade.isEmpty())
    }

    @Test
    fun theFirstSoundIsReportedOncePerReading() {
        read("One. Two.")
        standIns.runKeptWorks()
        read("Three.")
        standIns.runKeptWorks()

        assertEquals(2, standIns.firstSounds.size)
        assertTrue(standIns.firstSounds.all { it >= 0 })
    }

    @Test
    fun theEngineIsMadeAtTheFirstReadingAndKept() {
        read()
        standIns.runKeptWorks()
        read()
        standIns.runKeptWorks()

        assertEquals(1, standIns.engines.size)
        assertEquals(listOf(modelFolder), standIns.modelFolders)
        assertEquals(2, standIns.players.size)
    }

    @Test
    fun disposeStopsTheReadingForgetsTheWaitingOneAndClosesTheEngine() {
        read()
        standIns.runKeptWorks()
        read()
        val readTheText = service.readOnceTheModelIsInPlace(otherTarget, "Text.", modelFolder, System.nanoTime())

        service.dispose()
        readTheText()

        assertFalse(service.isReading(target))
        assertFalse(service.isReading(otherTarget))
        assertTrue(standIns.engines.single().closed)
    }

    @Test
    fun disposeLeavesTheEngineAloneWhileAnEngineWorkHoldsIt() {
        read()
        standIns.runKeptWorks()
        standIns.whileMaking = { service.dispose() }
        read()

        standIns.runKeptWorks()

        assertFalse(standIns.engines.single().closed)
        assertFalse(service.isReading(target))
    }

    @Test
    fun disposeWithoutAnEngineDoesNothing() {
        service.dispose()

        assertTrue(standIns.engines.isEmpty())
    }

    private fun waitUntil(condition: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (!condition()) {
            check(System.nanoTime() < deadline) { "The condition did not come true within 5 seconds" }
            Thread.sleep(10)
        }
    }

    companion object {
        private const val SAMPLES_PER_TEXT = STAND_IN_PIECES_PER_TEXT * STAND_IN_SAMPLES_PER_PIECE

        // The default project, which closing a project is tried with, needs the application
        @JvmField
        @ClassRule
        val application = ApplicationRule()
    }
}
