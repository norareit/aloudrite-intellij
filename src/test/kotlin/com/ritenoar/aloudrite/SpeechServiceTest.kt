package com.ritenoar.aloudrite

import com.intellij.openapi.project.ProjectManager
import com.intellij.testFramework.ApplicationRule
import com.intellij.testFramework.LightVirtualFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.ClassRule
import org.junit.Test
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class SpeechServiceTest {

    private val modelFolder: Path = Paths.get("/model")
    private val standIns = SpeechStandIns()
    private val service = standIns.service
    private val target = ReadingTarget(null, LightVirtualFile("notes.md", "text"))
    private val otherTarget = ReadingTarget(null, LightVirtualFile("other.md", "text"))

    private fun read(text: String = "One sentence.", readingTarget: ReadingTarget = target) =
        readParts(listOf(Words(text)), readingTarget)

    private fun readParts(parts: List<SpokenPart>, readingTarget: ReadingTarget = target) =
        service.read(readingTarget, parts, modelFolder, System.nanoTime())

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
    fun aSilenceIsHeardBetweenTheWordsAroundIt() {
        readParts(listOf(Words("One."), Silence(500), Words("Two.")))
        standIns.runKeptWorks()

        assertEquals(listOf("One.", "<silence 500 ms>", "Two."), standIns.heard())
        assertEquals(listOf("One.", "Two."), standIns.textsMade)
        assertTrue(standIns.players.single().closed)
    }

    @Test
    fun aSilenceIsPlayedInSlicesOfATenthOfASecond() {
        readParts(listOf(Silence(2000)))
        standIns.runKeptWorks()

        val player = standIns.players.single()
        assertEquals(20, player.played.size)
        assertTrue(player.played.all { it.size == STAND_IN_SAMPLE_RATE / 10 })
    }

    @Test
    fun aStopDuringASilencePlaysNoFurtherSliceAndDiscardsThePlayer() {
        readParts(listOf(Silence(2000), Words("After.")))
        standIns.whilePlaying = { slicesPlayed -> if (slicesPlayed == 3) service.stop(target) }

        standIns.runKeptWorks()

        val player = standIns.players.single()
        assertEquals(3, player.played.size)
        assertTrue(player.discarded)
        assertFalse(player.closed)
        assertFalse(service.isReading(target))
        assertTrue(standIns.failures.isEmpty())
    }

    @Test
    fun aSilenceAsTheOnlyPartIsPlayedAndEndsTheReading() {
        readParts(listOf(Silence(500)))
        standIns.runKeptWorks()

        assertEquals(listOf("<silence 500 ms>"), standIns.heard())
        assertTrue(standIns.textsMade.isEmpty())
        assertTrue(standIns.players.single().closed)
        assertFalse(service.isReading(target))
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
        service.read(projectTarget, listOf(Words("One sentence.")), modelFolder, System.nanoTime())

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
        val readTheText = service.readOnceTheModelIsInPlace(target, listOf(Words("The text of the press.")), modelFolder, System.nanoTime())
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
        val readTheText = service.readOnceTheModelIsInPlace(target, listOf(Words("Text.")), modelFolder, System.nanoTime())

        service.fileClosed(target)
        readTheText()
        readTheText()

        assertNothingIsRead()
    }

    @Test
    fun theReadingOnceTheModelIsInPlaceReadsNothingAfterItsProjectWasClosed() {
        val readTheText = service.readOnceTheModelIsInPlace(target, listOf(Words("Text.")), modelFolder, System.nanoTime())

        service.projectClosed(null)
        readTheText()
        readTheText()

        assertNothingIsRead()
    }

    @Test
    fun aSecondReadingOnceTheModelIsInPlaceReplacesTheFirst() {
        val readTheFirst = service.readOnceTheModelIsInPlace(target, listOf(Words("First.")), modelFolder, System.nanoTime())
        val readTheSecond = service.readOnceTheModelIsInPlace(otherTarget, listOf(Words("Second.")), modelFolder, System.nanoTime())

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
        assertTrue(standIns.firstSounds.all { it.milliseconds >= 0 && it.after == FirstSoundAfter.READ_ALOUD })
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
        val readTheText = service.readOnceTheModelIsInPlace(otherTarget, listOf(Words("Text.")), modelFolder, System.nanoTime())

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

    @Test
    fun theCurrentTargetIsWhatIsReadAndNothingBeforeAReading() {
        assertNull(service.currentTarget())

        read()
        assertEquals(target, service.currentTarget())

        read(readingTarget = otherTarget)
        assertEquals(otherTarget, service.currentTarget())
    }

    @Test
    fun theCurrentTargetIsNothingAfterAStop() {
        read()

        service.stop(target)

        assertNull(service.currentTarget())
    }

    @Test
    fun theCurrentTargetIsNothingAfterTheEndOfTheFile() {
        read()

        standIns.runKeptWorks()

        assertNull(service.currentTarget())
    }

    @Test
    fun theCurrentTargetIsNothingAfterAFailureOfTheEngine() {
        standIns.engineFailsAt = { true }
        read()

        standIns.runKeptWorks()

        assertNull(service.currentTarget())
        assertEquals(1, standIns.failures.size)
    }

    // Pauses the reading once, at the given slice of the player that plays then
    private fun pauseOnceAtSlice(slice: Int) {
        var pausedOnce = false
        standIns.whilePlaying = { slicesPlayed ->
            if (slicesPlayed == slice && !pausedOnce) {
                pausedOnce = true
                service.pause(target)
            }
        }
    }

    private fun resume(pressedAtNanos: Long = System.nanoTime()) = service.resume(target, pressedAtNanos)

    private fun samplesOf(player: SpeechStandIns.StandInPlayer): List<Float> = player.played.flatMap { it.toList() }

    private fun samplesOfTexts(vararg textNumbers: Int): List<Float> =
        textNumbers.flatMap { textNumber -> List(SAMPLES_PER_TEXT) { textNumber.toFloat() } }

    // A reading of "One. Two." that is paused in its first piece, and whose playing work has ended at the pause
    private fun pausedReading() {
        read("One. Two.")
        pauseOnceAtSlice(2)
        standIns.runKeptWorks()
        assertTrue(service.isPaused(target))
    }

    @Test
    fun aPauseWhilePlayingPlaysNoFurtherSliceDiscardsThePlayerAndKeepsTheReadingCurrentAndPaused() {
        read("One. Two.")
        pauseOnceAtSlice(2)
        val changesAtTheStart = standIns.changes.get()

        standIns.runKeptWorks()

        val player = standIns.players.single()
        assertEquals(2, player.played.size)
        assertTrue(player.discarded)
        assertFalse(player.closed)
        assertTrue(service.isReading(target))
        assertTrue(service.isPaused(target))
        assertEquals(target, service.currentTarget())
        assertTrue(standIns.keptWorks.isEmpty())
        assertEquals(changesAtTheStart + 1, standIns.changes.get())
        assertTrue(standIns.failures.isEmpty())
    }

    @Test
    fun aResumeAfterAPauseInTheSecondOfThreePiecesPlaysTheSecondFromItsFirstSampleThenTheThird() {
        read("One. Two. Three.")
        pauseOnceAtSlice(7)
        standIns.runKeptWorks()
        val changesAtThePause = standIns.changes.get()

        resume()
        assertFalse(service.isPaused(target))
        assertEquals(changesAtThePause + 1, standIns.changes.get())
        standIns.runKeptWorks()

        assertEquals(2, standIns.players.size)
        assertEquals(samplesOfTexts(2, 3), samplesOf(standIns.players.last()))
        assertTrue(standIns.players.last().closed)
        assertFalse(service.isReading(target))
        assertEquals(listOf("One. ", "Two. ", "Three."), standIns.textsMade)
        assertTrue(standIns.failures.isEmpty())
    }

    @Test
    fun withThePlayerLaggingAPauseJustAfterTheThirdPieceBeganToBeWrittenResumesAtTheSecond() {
        standIns.playerLag = STAND_IN_SAMPLES_PER_PIECE
        read("One. Two. Three.")
        // A piece is two runs of 25 samples, which play as slices of 10, 10 and 5, so the third piece begins at slice 13
        pauseOnceAtSlice(13)
        standIns.runKeptWorks()

        resume()
        standIns.runKeptWorks()

        assertEquals(samplesOfTexts(2, 3), samplesOf(standIns.players.last()))
    }

    @Test
    fun aPauseInTheSilenceAfterCodeExampleResumesAtThePieceAfterIt() {
        readParts(listOf(Words("code example"), Silence(2000), Words("After.")))
        pauseOnceAtSlice(8)
        standIns.runKeptWorks()

        resume()
        standIns.runKeptWorks()

        // "code example" plays as six slices, so two slices of the silence were played
        assertEquals(listOf("code example", "<silence 200 ms>", "After."), standIns.heard())
        assertEquals(listOf("code example", "After."), standIns.textsMade)
        assertTrue(standIns.players.last().closed)
        assertFalse(service.isReading(target))
    }

    @Test
    fun aPauseInASilenceAtTheEndOfThePartsResumesIntoTheEndAndEndsTheReading() {
        readParts(listOf(Words("One."), Silence(2000)))
        pauseOnceAtSlice(8)
        standIns.runKeptWorks()

        resume()
        standIns.runKeptWorks()

        assertTrue(standIns.players.last().played.isEmpty())
        assertTrue(standIns.players.last().closed)
        assertFalse(service.isReading(target))
        assertTrue(standIns.failures.isEmpty())
    }

    @Test
    fun aPauseBeforeThePlayingWorkHasRunResumesAtTheFirstPiece() {
        read("One. Two.")
        service.pause(target)
        standIns.runKeptWorks()

        assertTrue(standIns.players.isEmpty())
        assertTrue(service.isPaused(target))
        resume()
        standIns.runKeptWorks()

        assertEquals(samplesOfTexts(1, 2), samplesOf(standIns.players.single()))
        assertFalse(service.isReading(target))
    }

    @Test
    fun aPauseWhileTheEngineMakesTheFirstPieceResumesAtTheFirstPiece() {
        standIns.whileMaking = { text -> if (text == "One. ") service.pause(target) }
        read("One. Two.")
        standIns.runKeptWorks()

        resume()
        standIns.runKeptWorks()

        assertEquals(samplesOfTexts(1, 2), standIns.samplesPlayed())
        assertFalse(service.isReading(target))
    }

    @Test
    fun aResumeBeforeTheEngineWorkHasRunLeavesTheReadingToPlayOnItsOwn() {
        read("One. Two.")
        service.pause(target)
        resume()

        standIns.runKeptWorks()

        assertEquals(samplesOfTexts(1, 2), samplesOf(standIns.players.single()))
        assertFalse(service.isReading(target))
    }

    @Test
    fun aPauseAtTheLastSliceWhileTheEngineHasMadeTheEndOfTheTextDoesNotEndTheReading() {
        read("One. Two.")
        pauseOnceAtSlice(12)
        standIns.runKeptWorks()

        assertTrue(service.isReading(target))
        assertTrue(service.isPaused(target))
        assertFalse(standIns.players.single().closed)
        resume()
        standIns.runKeptWorks()

        assertEquals(samplesOfTexts(2), samplesOf(standIns.players.last()))
        assertTrue(standIns.players.last().closed)
        assertFalse(service.isReading(target))
    }

    @Test
    fun aPauseDuringThePlayOutAtTheEndDiscardsThePlayerAndTheResumePlaysTheLastPieceAgain() {
        read("One. Two.")
        var pausedOnce = false
        standIns.whileClosing = {
            if (!pausedOnce) {
                pausedOnce = true
                service.pause(target)
            }
        }
        standIns.runKeptWorks()

        assertTrue(standIns.players.single().discarded)
        assertTrue(service.isPaused(target))
        resume()
        standIns.runKeptWorks()

        assertEquals(samplesOfTexts(2), samplesOf(standIns.players.last()))
        assertTrue(standIns.players.last().closed)
        assertFalse(standIns.players.last().discarded)
        assertFalse(service.isReading(target))
        assertTrue(standIns.failures.isEmpty())
    }

    @Test
    fun threePausesAndResumesInOneReadingReachTheEndOfTheText() {
        read("One. Two. Three. Four.")
        var pauses = 0
        standIns.whilePlaying = { slicesPlayed ->
            if (slicesPlayed == 7 && pauses < 3) {
                pauses++
                service.pause(target)
            }
        }

        standIns.runKeptWorks()
        while (service.isPaused(target)) {
            resume()
            standIns.runKeptWorks()
        }

        assertEquals(3, pauses)
        assertEquals(4, standIns.players.size)
        assertEquals(samplesOfTexts(4), samplesOf(standIns.players.last()))
        assertTrue(standIns.players.last().closed)
        assertTrue(standIns.players.dropLast(1).all { it.discarded })
        assertEquals(listOf("One. ", "Two. ", "Three. ", "Four."), standIns.textsMade)
        assertFalse(service.isReading(target))
    }

    @Test
    fun aStopEndsAPausedReading() {
        assertEndsAPausedReading { service.stop(target) }
    }

    @Test
    fun closingTheFileEndsAPausedReading() {
        assertEndsAPausedReading { service.fileClosed(target) }
    }

    @Test
    fun closingTheProjectEndsAPausedReading() {
        assertEndsAPausedReading { service.projectClosed(null) }
    }

    @Test
    fun aReadOfAnotherTargetEndsAPausedReadingAndReadsTheOther() {
        assertEndsAPausedReading { read("Other.", otherTarget) }

        assertEquals(listOf("One. ", "Two.", "Other."), standIns.textsMade)
        assertEquals(List(SAMPLES_PER_TEXT) { 3f }, samplesOf(standIns.players.last()))
        assertFalse(service.isReading(otherTarget))
    }

    private fun assertEndsAPausedReading(end: () -> Unit) {
        pausedReading()
        val changesAtThePause = standIns.changes.get()

        end()

        assertFalse(service.isReading(target))
        assertFalse(service.isPaused(target))
        assertEquals(changesAtThePause + 1, standIns.changes.get())
        resume()
        assertEquals(changesAtThePause + 1, standIns.changes.get())
        val playersBefore = standIns.players.size
        standIns.runKeptWorks()
        // No player after the end plays the paused text, whose pieces made the samples 1 and 2
        assertTrue(standIns.players.drop(playersBefore).none { player -> samplesOf(player).any { it == 1f || it == 2f } })
        assertTrue(standIns.failures.isEmpty())
    }

    @Test
    fun aFailureOfTheEngineWhilePausedEndsTheReadingAndReportsIt() {
        standIns.whileMaking = { text -> if (text == "Two.") service.pause(target) }
        standIns.engineFailsAt = { it == "Two." }
        read("One. Two.")

        standIns.runKeptWorks()

        assertSame(target, standIns.failures.single().first)
        assertFalse(service.isReading(target))
        assertFalse(service.isPaused(target))
    }

    @Test
    fun aFailureOfOpeningThePlayerAtAResumeEndsTheReadingAndReportsIt() {
        pausedReading()
        standIns.openingThePlayerFails = IllegalStateException("no audio device")

        resume()
        standIns.runKeptWorks()

        assertSame(target, standIns.failures.single().first)
        assertFalse(service.isReading(target))
    }

    @Test
    fun aPauseDuringThePlayOutDoesNotReportTheFailureOfTheDiscardedPlayer() {
        read("One.")
        standIns.whileClosing = {
            service.pause(target)
            standIns.closingThePlayerFails = IllegalStateException("The line was discarded")
        }

        standIns.runKeptWorks()

        assertTrue(standIns.failures.isEmpty())
        assertTrue(service.isPaused(target))
    }

    @Test
    fun aStopDuringThePlayOutDoesNotReportTheFailureOfTheDiscardedPlayer() {
        read("One.")
        standIns.whileClosing = {
            service.stop(target)
            standIns.closingThePlayerFails = IllegalStateException("The line was discarded")
        }

        standIns.runKeptWorks()

        assertTrue(standIns.failures.isEmpty())
        assertFalse(service.isReading(target))
    }

    @Test
    fun aPauseOrResumeOfAnotherTargetAPauseOfAPausedReadingAndAResumeOfOneThatPlaysDoNothing() {
        read("One. Two.")
        val changesAtTheStart = standIns.changes.get()

        resume()
        service.pause(otherTarget)
        assertFalse(service.isPaused(target))
        assertEquals(changesAtTheStart, standIns.changes.get())

        service.pause(target)
        service.pause(target)
        service.resume(otherTarget, System.nanoTime())
        assertTrue(service.isPaused(target))
        assertFalse(service.isPaused(otherTarget))
        assertEquals(changesAtTheStart + 1, standIns.changes.get())
    }

    @Test
    fun nothingIsPausedWhileNothingIsRead() {
        service.pause(target)
        resume()

        assertFalse(service.isPaused(target))
        assertFalse(service.isReading(target))
        assertTrue(standIns.keptWorks.isEmpty())
    }

    @Test
    fun theFirstSoundAfterAResumeIsReportedAsSuchAndMeasuredFromThePressOfResume() {
        read("One. Two.")
        pauseOnceAtSlice(2)
        standIns.runKeptWorks()

        resume(pressedAtNanos = System.nanoTime() - TimeUnit.SECONDS.toNanos(5))
        standIns.runKeptWorks()

        assertEquals(2, standIns.firstSounds.size)
        assertEquals(FirstSoundAfter.READ_ALOUD, standIns.firstSounds.first().after)
        assertTrue(standIns.firstSounds.first().milliseconds < 5_000)
        assertEquals(FirstSoundAfter.RESUME, standIns.firstSounds.last().after)
        assertTrue(standIns.firstSounds.last().milliseconds >= 5_000)
    }

    @Test
    fun disposeEndsAPausedReading() {
        pausedReading()

        service.dispose()

        assertFalse(service.isReading(target))
        assertFalse(service.isPaused(target))
    }

    // A reading of "One. Two." whose engine work runs in a thread of its own and is held at "Two." until the test lets
    // it go, and whose playing work runs in another and has played the first piece, with the player lagging inside
    // it, and waits for the second
    private fun playingThatWaitsForTheSecondPiece(): HeldReading {
        val engineAtTheSecondPiece = CountDownLatch(1)
        val letTheEngineGo = CountDownLatch(1)
        standIns.whileMaking = { text ->
            if (text == "Two.") {
                engineAtTheSecondPiece.countDown()
                letTheEngineGo.await(60, TimeUnit.SECONDS)
            }
        }
        standIns.playerLag = 20
        read("One. Two.")
        val engineWork = inAThread(standIns.keptWorks.removeAt(0))
        assertTrue(engineAtTheSecondPiece.await(5, TimeUnit.SECONDS))
        val playingWork = inAThread(standIns.keptWorks.removeAt(0))
        waitUntil { standIns.players.singleOrNull()?.let(::samplesOf)?.size == SAMPLES_PER_TEXT }
        waitUntil { playingWork.state == Thread.State.WAITING }
        return HeldReading(engineWork, playingWork, letTheEngineGo)
    }

    private class HeldReading(val engineWork: Thread, val playingWork: Thread, val letTheEngineGo: CountDownLatch)

    private fun inAThread(work: () -> Unit): Thread = Thread(work).also {
        it.isDaemon = true
        it.start()
    }

    @Test
    fun aPauseAndAResumeWhileThePlayingWaitsForTheEnginePlayTheFirstPieceAgainBeforeTheEngineGoesOn() {
        val held = playingThatWaitsForTheSecondPiece()

        service.pause(target)
        resume()
        // The playing work either plays the resumption itself or, where it had ended at the pause first, the resume
        // handed a new one over
        waitUntil { standIns.players.size == 2 || standIns.keptWorks.isNotEmpty() }
        val resumedWork = if (standIns.keptWorks.isEmpty()) null else inAThread(standIns.keptWorks.removeAt(0))
        waitUntil { standIns.players.size == 2 && samplesOf(standIns.players[1]).size == SAMPLES_PER_TEXT }

        assertTrue(held.engineWork.isAlive)
        assertTrue(standIns.players.first().discarded)
        assertEquals(samplesOfTexts(1), samplesOf(standIns.players[1]))
        assertEquals(FirstSoundAfter.RESUME, standIns.firstSounds.last().after)
        held.letTheEngineGo.countDown()
        for (thread in listOfNotNull(held.engineWork, held.playingWork, resumedWork)) thread.join(5_000)

        assertEquals(2, standIns.players.size)
        assertEquals(samplesOfTexts(1, 2), samplesOf(standIns.players.last()))
        assertTrue(standIns.players.last().closed)
        assertFalse(service.isReading(target))
        assertTrue(standIns.failures.isEmpty())
    }

    @Test
    fun aPauseWhileThePlayingWaitsForTheEngineDiscardsThePlayerAndLeavesTheReadingPausedBeforeTheEngineGoesOn() {
        val held = playingThatWaitsForTheSecondPiece()

        service.pause(target)
        held.playingWork.join(5_000)

        assertFalse(held.playingWork.isAlive)
        assertTrue(held.engineWork.isAlive)
        assertTrue(standIns.players.single().discarded)
        assertTrue(service.isPaused(target))
        assertTrue(standIns.keptWorks.isEmpty())
        held.letTheEngineGo.countDown()
        held.engineWork.join(5_000)
        assertTrue(service.isPaused(target))

        resume()
        standIns.runKeptWorks()

        assertEquals(samplesOfTexts(1, 2), samplesOf(standIns.players.last()))
        assertFalse(service.isReading(target))
        assertTrue(standIns.failures.isEmpty())
    }

    private fun speed(typed: String): ReadingSpeed = checkNotNull(parsedSpeed(typed))

    private fun speedsOf(vararg typed: String): List<Float> = typed.map { speed(it).forTheEngine }

    // Changes the speed once, at the given slice of the first player
    private fun changeOnceAtSlice(slice: Int, newSpeed: ReadingSpeed, afterAWait: Boolean = false) {
        var changedOnce = false
        standIns.whilePlaying = { slicesPlayed ->
            if (slicesPlayed == slice && !changedOnce) {
                changedOnce = true
                service.changeSpeed(newSpeed, afterAWait)
            }
        }
    }

    // "One. Two. Three." with the speed changed by a button in the second piece, at the clock's zero, and the
    // playing work ended at the change, so that the next making waits
    private fun waitingAfterAPress(newSpeed: ReadingSpeed = speed("1.1")) {
        read("One. Two. Three.")
        changeOnceAtSlice(8, newSpeed, afterAWait = true)
        standIns.runKeptWorks()
        assertEquals(3, standIns.textsMade.size)
    }

    @Test
    fun aReadingHandsTheEngineTheSpeedOfTheSettingAtItsStartWithEveryPiece() {
        standIns.speed = speed("1.5")
        read("One. Two. Three.")
        standIns.runKeptWorks()
        assertEquals(speedsOf("1.5", "1.5", "1.5"), standIns.speedsMade)

        standIns.speed = speed("0.8")
        read("Four. Five.")
        standIns.runKeptWorks()
        assertEquals(speedsOf("1.5", "1.5", "1.5", "0.8", "0.8"), standIns.speedsMade)
    }

    @Test
    fun theSilencesLastTheirLengthAtNormalSpeedDividedByTheSpeed() {
        val parts = listOf(Words("One."), Silence(500), Words("Two."), Silence(2000), Words("Three."))

        standIns.speed = ReadingSpeed.FASTEST
        readParts(parts)
        standIns.runKeptWorks()
        assertEquals(listOf("One.", "<silence 250 ms>", "Two.", "<silence 1000 ms>", "Three."), standIns.heard())

        standIns.players.clear()
        standIns.speed = ReadingSpeed.SLOWEST
        readParts(parts)
        standIns.runKeptWorks()
        // 667 and 2667 ms, which the stand-ins make as 66 and 266 samples at their 100 samples a second
        assertEquals(listOf("One.", "<silence 660 ms>", "Two.", "<silence 2660 ms>", "Three."), standIns.heard())
    }

    @Test
    fun aChangeWithNoReadingDoesNothing() {
        service.changeSpeed(ReadingSpeed.FASTEST, afterAWait = false)
        service.changeSpeed(ReadingSpeed.SLOWEST, afterAWait = true)
        standIns.passTime(1_000)

        assertTrue(standIns.keptWorks.isEmpty())
        assertTrue(standIns.engines.isEmpty())
    }

    @Test
    fun aChangeByTheFieldWhileTheSecondPieceIsHeardPlaysItAgainAndTheRestAtTheNewSpeed() {
        read("One. Two. Three.")
        changeOnceAtSlice(8, ReadingSpeed.FASTEST)

        standIns.runKeptWorks()

        val first = standIns.players.first()
        assertEquals(8, first.played.size)
        assertTrue(first.discarded)
        assertEquals(listOf("One. ", "Two. ", "Three.", "Two. ", "Three."), standIns.textsMade)
        assertEquals(speedsOf("1.0", "1.0", "1.0", "2.0", "2.0"), standIns.speedsMade)
        assertEquals(listOf("One. Two. Two. Three."), standIns.heard())
        assertEquals(samplesOfTexts(4, 5), samplesOf(standIns.players.last()))
        assertTrue(standIns.players.last().closed)
        assertFalse(service.isReading(target))
        assertTrue(standIns.failures.isEmpty())
    }

    @Test
    fun aTypedSpeedFarFromTheOneBeforeIsOneChange() {
        read("One. Two. Three.")
        changeOnceAtSlice(8, ReadingSpeed.FASTEST)

        standIns.runKeptWorks()

        assertEquals(2, standIns.players.size)
        assertEquals(2, standIns.speedsMade.count { it == ReadingSpeed.FASTEST.forTheEngine })
    }

    @Test
    fun withThePlayerLaggingAChangeJustAfterTheThirdPieceBeganToBeWrittenGoesOnAtTheSecond() {
        standIns.playerLag = STAND_IN_SAMPLES_PER_PIECE
        read("One. Two. Three.")
        changeOnceAtSlice(13, ReadingSpeed.FASTEST)

        standIns.runKeptWorks()

        assertEquals(samplesOfTexts(4, 5), samplesOf(standIns.players.last()))
        assertEquals(listOf("Two. ", "Three."), standIns.textsMade.drop(3))
    }

    @Test
    fun aChangeInTheSilenceAfterCodeExampleGoesOnAtThePieceAfterIt() {
        readParts(listOf(Words("code example"), Silence(2000), Words("After.")))
        changeOnceAtSlice(8, ReadingSpeed.FASTEST)

        standIns.runKeptWorks()

        assertEquals(listOf("code example", "<silence 200 ms>", "After."), standIns.heard())
        assertEquals(listOf("code example", "After.", "After."), standIns.textsMade)
        assertEquals(ReadingSpeed.FASTEST.forTheEngine, standIns.speedsMade.last())
        assertFalse(service.isReading(target))
    }

    @Test
    fun aChangeDuringThePlayOutAtTheEndMakesTheLastPieceAgainAtTheNewSpeed() {
        read("One. Two.")
        var changedOnce = false
        standIns.whileClosing = {
            if (!changedOnce) {
                changedOnce = true
                service.changeSpeed(ReadingSpeed.FASTEST, afterAWait = false)
            }
        }

        standIns.runKeptWorks()

        assertTrue(standIns.players.first().discarded)
        assertEquals(listOf("One. ", "Two.", "Two."), standIns.textsMade)
        assertEquals(speedsOf("1.0", "1.0", "2.0"), standIns.speedsMade)
        assertEquals(samplesOfTexts(3), samplesOf(standIns.players.last()))
        assertTrue(standIns.players.last().closed)
        assertFalse(service.isReading(target))
        assertTrue(standIns.failures.isEmpty())
    }

    @Test
    fun aChangeBeforeTheEngineWorkHasRunMakesEveryPieceAtTheNewSpeed() {
        read("One. Two.")

        service.changeSpeed(ReadingSpeed.FASTEST, afterAWait = false)
        standIns.runKeptWorks()

        assertEquals(speedsOf("2.0", "2.0"), standIns.speedsMade)
        assertEquals(samplesOfTexts(1, 2), standIns.samplesPlayed())
    }

    @Test
    fun aChangeBeforeThePlayingWorkHasRunPlaysNothingAtTheOldSpeed() {
        read("One. Two.")
        standIns.runNextKeptWork()

        service.changeSpeed(ReadingSpeed.FASTEST, afterAWait = false)
        standIns.runKeptWorks()

        assertEquals(1, standIns.players.size)
        assertEquals(samplesOfTexts(3, 4), standIns.samplesPlayed())
        assertEquals(speedsOf("1.0", "1.0", "2.0", "2.0"), standIns.speedsMade)
        assertFalse(service.isReading(target))
    }

    @Test
    fun aChangeByAButtonStopsTheSoundAtOnceAndMakesThePieceOnlyOnceHalfASecondHasPassed() {
        waitingAfterAPress()

        assertEquals(8, standIns.players.single().played.size)
        assertTrue(standIns.players.single().discarded)
        standIns.passTime(499)
        assertTrue(standIns.keptWorks.isEmpty())
        standIns.passTime(1)
        standIns.runKeptWorks()

        assertEquals(speedsOf("1.0", "1.0", "1.0", "1.1", "1.1"), standIns.speedsMade)
        assertEquals(samplesOfTexts(4, 5), samplesOf(standIns.players.last()))
        assertFalse(service.isReading(target))
    }

    @Test
    fun pressesBeforeTheFirstMakingStartedMakeNothingBeforeTheWaitAndNothingAtASpeedInBetween() {
        read("One. Two. Three.")

        service.changeSpeed(speed("1.05"), afterAWait = true)
        standIns.runKeptWorks()
        assertTrue(standIns.textsMade.isEmpty())
        assertTrue(standIns.players.isEmpty())
        standIns.passTime(300)
        service.changeSpeed(speed("1.1"), afterAWait = true)
        standIns.passTime(499)
        assertTrue(standIns.keptWorks.isEmpty())
        standIns.passTime(1)
        standIns.runKeptWorks()

        assertEquals(speedsOf("1.1", "1.1", "1.1"), standIns.speedsMade)
        assertEquals(samplesOfTexts(1, 2, 3), standIns.samplesPlayed())
        assertFalse(service.isReading(target))
    }

    // The engine work reads the clock just before the end of the wait and is held there, the check of the wait runs at
    // its end, and only then does the engine work go on with the time it read
    @Test
    fun theCheckOfTheWaitThatComesWhileTheFirstEngineWorkDecidesStillStartsTheMaking() {
        read("One. Two.")
        service.changeSpeed(speed("1.05"), afterAWait = true)
        val check = standIns.takeLaterWorks().single()
        standIns.passTime(499)
        val engineWork = standIns.keptWorks.removeAt(0)
        val clockRead = CountDownLatch(1)
        val letTheEngineWorkGo = CountDownLatch(1)
        lateinit var engineThread: Thread
        standIns.afterReadingTheClock = {
            if (Thread.currentThread() === engineThread && clockRead.count > 0) {
                clockRead.countDown()
                letTheEngineWorkGo.await(5, TimeUnit.SECONDS)
            }
        }
        engineThread = Thread(engineWork).also { it.isDaemon = true }
        engineThread.start()
        assertTrue(clockRead.await(5, TimeUnit.SECONDS))

        standIns.passTime(1)
        val checkThread = inAThread(check)
        waitUntil { checkThread.state == Thread.State.TERMINATED || checkThread.state == Thread.State.BLOCKED }
        letTheEngineWorkGo.countDown()
        engineThread.join(5_000)
        checkThread.join(5_000)
        standIns.runKeptWorks()

        assertEquals(speedsOf("1.05", "1.05"), standIns.speedsMade)
        assertEquals(samplesOfTexts(1, 2), standIns.samplesPlayed())
        assertFalse(service.isReading(target))
    }

    @Test
    fun aPauseBeforeTheWaitedFirstMakingStartedStaysPausedAndItsResumeStartsIt() {
        read("One. Two.")
        service.changeSpeed(speed("1.05"), afterAWait = true)
        standIns.runKeptWorks()

        service.pause(target)
        standIns.passTime(1_000)
        assertTrue(standIns.keptWorks.isEmpty())
        resume()
        standIns.runKeptWorks()

        assertEquals(speedsOf("1.05", "1.05"), standIns.speedsMade)
        assertEquals(samplesOfTexts(1, 2), standIns.samplesPlayed())
        assertFalse(service.isReading(target))
    }

    @Test
    fun pressesInARowWithinTheWaitMakeThePieceOnceAtTheSpeedOfTheLast() {
        waitingAfterAPress(speed("1.1"))

        standIns.passTime(300)
        service.changeSpeed(speed("1.2"), afterAWait = true)
        standIns.passTime(300)
        service.changeSpeed(speed("1.3"), afterAWait = true)
        standIns.passTime(499)
        assertTrue(standIns.keptWorks.isEmpty())
        standIns.passTime(1)
        standIns.runKeptWorks()

        assertEquals(1_100, standIns.clockMillis)
        assertEquals(speedsOf("1.0", "1.0", "1.0", "1.3", "1.3"), standIns.speedsMade)
        assertFalse(service.isReading(target))
    }

    @Test
    fun twoPressesMoreThanTheWaitApartMakeThePieceOnceAtEachSpeed() {
        waitingAfterAPress(speed("1.1"))

        standIns.passTime(500)
        standIns.runNextKeptWork()
        standIns.passTime(200)
        service.changeSpeed(speed("1.2"), afterAWait = true)
        standIns.runKeptWorks()
        standIns.passTime(500)
        standIns.runKeptWorks()

        assertEquals(speedsOf("1.0", "1.0", "1.0", "1.1", "1.1", "1.2", "1.2"), standIns.speedsMade)
        assertEquals(samplesOfTexts(6, 7), samplesOf(standIns.players.last()))
        assertFalse(service.isReading(target))
    }

    @Test
    fun aChangeByTheFieldDuringTheWaitOfAPressMakesThePieceAtOnceAtTheSpeedOfTheField() {
        waitingAfterAPress(speed("1.1"))

        standIns.passTime(200)
        service.changeSpeed(speed("1.5"), afterAWait = false)
        standIns.runKeptWorks()
        standIns.passTime(1_000)

        assertTrue(standIns.keptWorks.isEmpty())
        assertEquals(speedsOf("1.0", "1.0", "1.0", "1.5", "1.5"), standIns.speedsMade)
        assertFalse(service.isReading(target))
    }

    @Test
    fun severalChangesBeforeTheWorksOfTheOneBeforeHaveRunMakeThePieceOnceAtTheLastSpeed() {
        read("One. Two. Three.")
        standIns.runNextKeptWork()
        changeOnceAtSlice(8, speed("1.2"))
        standIns.runNextKeptWork()
        assertEquals(2, standIns.keptWorks.size)

        service.changeSpeed(speed("1.5"), afterAWait = false)
        service.changeSpeed(ReadingSpeed.FASTEST, afterAWait = false)
        standIns.runKeptWorks()

        assertEquals(speedsOf("1.0", "1.0", "1.0", "2.0", "2.0"), standIns.speedsMade)
        assertEquals(samplesOfTexts(4, 5), samplesOf(standIns.players.last()))
        assertFalse(service.isReading(target))
    }

    @Test
    fun aChangeWhilePausedPlaysNothingUntilTheResumeWhichPlaysThePieceOfThePauseAtTheNewSpeed() {
        read("One. Two. Three.")
        pauseOnceAtSlice(8)
        standIns.runKeptWorks()

        service.changeSpeed(ReadingSpeed.FASTEST, afterAWait = false)
        assertTrue(standIns.keptWorks.isEmpty())
        assertTrue(service.isPaused(target))
        resume()
        standIns.runKeptWorks()

        assertEquals(2, standIns.players.size)
        assertEquals(samplesOfTexts(4, 5), samplesOf(standIns.players.last()))
        assertEquals(speedsOf("1.0", "1.0", "1.0", "2.0", "2.0"), standIns.speedsMade)
        assertFalse(service.isReading(target))
    }

    @Test
    fun aChangeWhilePausedInASilenceGoesOnAtThePieceAfterItAtTheResume() {
        readParts(listOf(Words("code example"), Silence(2000), Words("After.")))
        pauseOnceAtSlice(8)
        standIns.runKeptWorks()

        service.changeSpeed(ReadingSpeed.FASTEST, afterAWait = false)
        resume()
        standIns.runKeptWorks()

        assertEquals(listOf("code example", "<silence 200 ms>", "After."), standIns.heard())
        assertEquals(ReadingSpeed.FASTEST.forTheEngine, standIns.speedsMade.last())
    }

    @Test
    fun aPauseAfterAChangeAndBeforeItsMakingStartedStaysPausedAndItsResumeStartsThatMaking() {
        waitingAfterAPress(ReadingSpeed.FASTEST)

        service.pause(target)
        standIns.passTime(1_000)
        assertTrue(standIns.keptWorks.isEmpty())
        assertTrue(service.isPaused(target))
        resume()
        standIns.runKeptWorks()

        assertEquals(samplesOfTexts(4, 5), samplesOf(standIns.players.last()))
        assertEquals(speedsOf("1.0", "1.0", "1.0", "2.0", "2.0"), standIns.speedsMade)
        assertFalse(service.isReading(target))
    }

    @Test
    fun aPauseAndAResumeAfterAChangeGoOnWithTheAudioMadeAtTheNewSpeed() {
        read("One. Two. Three.")
        var changed = false
        var paused = false
        standIns.whilePlaying = { slicesPlayed ->
            if (standIns.players.size == 1 && slicesPlayed == 8 && !changed) {
                changed = true
                service.changeSpeed(ReadingSpeed.FASTEST, afterAWait = false)
            }
            if (standIns.players.size == 2 && slicesPlayed == 3 && !paused) {
                paused = true
                service.pause(target)
            }
        }
        standIns.runKeptWorks()
        assertTrue(service.isPaused(target))

        resume()
        standIns.runKeptWorks()

        assertEquals(speedsOf("1.0", "1.0", "1.0", "2.0", "2.0"), standIns.speedsMade)
        assertEquals(samplesOfTexts(4, 5), samplesOf(standIns.players.last()))
        assertFalse(service.isReading(target))
    }

    @Test
    fun aStopDuringTheWaitEndsTheReadingAndTheCheckStartsNothing() {
        assertEndsTheReadingDuringTheWait { service.stop(target) }
    }

    @Test
    fun closingTheFileDuringTheWaitEndsTheReadingAndTheCheckStartsNothing() {
        assertEndsTheReadingDuringTheWait { service.fileClosed(target) }
    }

    @Test
    fun aReadOfAnotherTargetDuringTheWaitEndsTheReadingAndTheCheckStartsNothing() {
        assertEndsTheReadingDuringTheWait { read("Other.", otherTarget) }

        assertEquals("Other.", standIns.textsMade.last())
    }

    private fun assertEndsTheReadingDuringTheWait(end: () -> Unit) {
        waitingAfterAPress(speed("1.1"))

        end()
        standIns.passTime(1_000)
        standIns.runKeptWorks()

        assertFalse(service.isReading(target))
        assertFalse(speed("1.1").forTheEngine in standIns.speedsMade)
        assertTrue(standIns.failures.isEmpty())
    }

    @Test
    fun theFirstSoundAfterAChangeIsReportedAsSuchAndMeasuredFromTheLastChange() {
        waitingAfterAPress(speed("1.1"))

        standIns.passTime(300)
        service.changeSpeed(speed("1.2"), afterAWait = true)
        standIns.passTime(500)
        standIns.runKeptWorks()

        assertEquals(FirstSound(500, FirstSoundAfter.SPEED_CHANGE), standIns.firstSounds.last())
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
