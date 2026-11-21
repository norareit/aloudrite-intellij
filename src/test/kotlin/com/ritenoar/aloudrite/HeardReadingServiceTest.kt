package com.ritenoar.aloudrite

import com.intellij.testFramework.LightVirtualFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// The places being heard as the service gives them, on the stand-ins. A text makes 50 samples in two runs of 25, a slice is
// 10 samples, and so a piece plays as the slices of 10, 10, 5, 10, 10 and 5 samples, which put the share heard at 0.2,
// 0.4, 0.5, 0.7, 0.9 and 1.0
class HeardReadingServiceTest {

    private val standIns = SpeechStandIns()
    private val service = standIns.service
    private val target = ReadingTarget(null, LightVirtualFile("notes.md", "text"))
    private val otherTarget = ReadingTarget(null, LightVirtualFile("other.md", "text"))

    // The weights of the words are 5, 4, 5 and 5
    private val onePiece = "alpha beta gamma delta."
    private val twoParagraphs = "One two.\n\nThree four."

    private fun read(markdown: String, readingTarget: ReadingTarget = target, documentStamp: Long? = null) =
        service.read(readingTarget, placedWhatToRead(markdown, documentStamp), INT8_MODEL, System.nanoTime())

    private fun HeardState.wordIn(markdown: String): String? = places?.let { markdown.substring(it.word.start, it.word.end) }

    private fun heardReading(): HeardState = checkNotNull(service.heardReading())

    // The word heard at each slice that plays, as the editor would ask for it
    private fun wordsHeardAtEachSlice(markdown: String): List<String?> {
        val words = mutableListOf<String?>()
        standIns.whilePlaying = { words += heardReading().wordIn(markdown) }
        standIns.runKeptWorks()
        return words
    }

    @Test
    fun thereIsNothingWhileNothingReads() {
        assertNull(service.heardReading())
    }

    @Test
    fun theTargetTheNumberAndTheStampOfTheDocumentAreThoseOfTheReading() {
        read(onePiece, documentStamp = 42L)

        val state = heardReading()

        assertEquals(target, state.target)
        assertEquals(1, state.number)
        assertEquals(42L, state.documentStamp)
        assertEquals(0, state.resumes)
    }

    @Test
    fun thereAreNoPlacesBeforeTheFirstSlice() {
        read(onePiece)

        assertNull(heardReading().places)

        standIns.runNextKeptWork()
        assertNull(heardReading().places)
    }

    @Test
    fun theFirstWordOfTheFirstPieceIsHeardAtTheFirstSlice() {
        read(onePiece)

        assertEquals("alpha", wordsHeardAtEachSlice(onePiece).first())
    }

    @Test
    fun theWordMovesForwardSliceBySliceToTheLastWordOfThePiece() {
        read(onePiece)

        assertEquals(listOf("alpha", "beta", "gamma", "gamma", "delta.", "delta."), wordsHeardAtEachSlice(onePiece))
    }

    @Test
    fun theSentenceIsThatOfTheWordAndStaysWithinIt() {
        val markdown = "One two. Three four."
        read(markdown)
        val sentences = mutableListOf<String>()
        standIns.whilePlaying = {
            val sentence = heardReading().places!!.sentence
            sentences += markdown.substring(sentence.start, sentence.end)
        }

        standIns.runKeptWorks()

        // The two sentences are two pieces of six slices each
        assertEquals(List(6) { "One two." } + List(6) { "Three four." }, sentences)
    }

    @Test
    fun thePlacesStayThroughTheSilenceBetweenTwoParagraphsAndTheNextPieceStartsWithItsFirstWord() {
        read(twoParagraphs)

        val words = wordsHeardAtEachSlice(twoParagraphs)

        // Six slices of the first piece, five of the half second of silence, and six of the second piece
        assertEquals(17, words.size)
        assertEquals(List(5) { "two." }, words.subList(6, 11))
        assertEquals("One", words.first())
        assertEquals("Three", words[11])
        assertEquals("four.", words.last())
    }

    @Test
    fun thePlacesAreThoseOfThePositionOfThePlayerAtTheCallAndNotOfTheLastSlice() {
        read(onePiece)
        val words = mutableListOf<String?>()
        standIns.whilePlaying = { slice ->
            if (slice == 5) {
                words += heardReading().wordIn(onePiece)
                standIns.playerLag = 40
                words += heardReading().wordIn(onePiece)
                standIns.playerLag = 0
            }
        }

        standIns.runKeptWorks()

        assertEquals(listOf("delta.", "alpha"), words)
    }

    @Test
    fun thePlacesStayWhilePausedAlsoWhenThePlayerReportsAnotherPosition() {
        read(onePiece)
        standIns.whilePlaying = { slice -> if (slice == 4) service.pause(target) }
        standIns.runKeptWorks()
        val atThePause = heardReading().places

        standIns.playerLag = 30
        val whilePaused = heardReading().places
        standIns.playerLag = 0

        assertEquals("gamma", onePiece.substring(atThePause!!.word.start, atThePause.word.end))
        assertEquals(atThePause, whilePaused)
    }

    @Test
    fun atAResumeTheCountOfResumesRisesAndTheFirstSliceHeardAgainIsTheFirstWordOfThePiece() {
        read(onePiece)
        standIns.whilePlaying = { slice -> if (slice == 4) service.pause(target) }
        standIns.runKeptWorks()
        val words = mutableListOf<String?>()

        service.resume(target, System.nanoTime())
        standIns.whilePlaying = { words += heardReading().wordIn(onePiece) }
        assertEquals(1, heardReading().resumes)
        standIns.runKeptWorks()

        assertEquals("alpha", words.first())
        assertEquals("delta.", words.last())
    }

    @Test
    fun aResumeDuringTheSilenceGivesTheFirstWordOfTheNextPiece() {
        read(twoParagraphs)
        standIns.whilePlaying = { slice -> if (slice == 8) service.pause(target) }
        standIns.runKeptWorks()

        service.resume(target, System.nanoTime())
        val words = wordsHeardAtEachSlice(twoParagraphs)

        assertEquals("Three", words.first())
    }

    @Test
    fun afterAChangeOfTheSpeedTheOldPlacesStayUntilTheNewMakingPlaysAndThenTheFirstWordOfThePieceIsHeard() {
        read(onePiece)
        val words = mutableListOf<String?>()
        standIns.whilePlaying = { slice ->
            if (slice == 4 && standIns.players.size == 1) service.changeSpeed(ReadingSpeed.FASTEST, afterAWait = false)
            words += heardReading().wordIn(onePiece)
        }

        standIns.runKeptWorks()

        // The first player played four slices, the last of them when the change was made, and the second begins the piece again
        assertEquals(2, standIns.players.size)
        assertEquals(listOf("alpha", "beta", "gamma", "gamma"), words.take(4))
        assertEquals(listOf("alpha", "beta", "gamma", "gamma", "delta.", "delta."), words.drop(4))
    }

    @Test
    fun thereAreNoPlacesOnceTheReadingEndedWasStoppedOrFailed() {
        read(onePiece)
        standIns.runKeptWorks()
        assertNull(service.heardReading())

        read(onePiece)
        service.stop(target)
        assertNull(service.heardReading())

        standIns.engineFailsAt = { true }
        read(onePiece)
        standIns.runKeptWorks()
        assertNull(service.heardReading())
        assertEquals(1, standIns.failures.size)
    }

    @Test
    fun anotherReadingReplacesTheFirstWithANewNumber() {
        read(onePiece)
        val first = heardReading()

        read(onePiece, otherTarget)

        assertEquals(otherTarget, heardReading().target)
        assertNotEquals(first.number, heardReading().number)
        assertEquals(2, heardReading().number)
    }

    @Test
    fun aPositionThatCrossesIntoTheSilenceWithoutLandingInTheLastWordStillFinishesThePiece() {
        val markdown = "alpha beta gamma delta.\n\nNext."
        read(markdown)
        val words = mutableListOf<String?>()
        // The line lags 15 samples behind through the piece, and then catches up in one step into the silence, so that no
        // position lands in the last word
        standIns.whilePlaying = { slice ->
            standIns.playerLag = if (slice < 7) 15 else 0
            if (slice == 6 || slice == 7) words += heardReading().wordIn(markdown)
        }

        standIns.runKeptWorks()

        assertEquals(listOf("gamma", "delta."), words)
    }

    // The words start at 0, 5, 10 and 15 samples of the piece of 50, which is not where the share of letters puts them, so
    // that the word at each slice tells which of the two chose it
    private fun alignWordsAtFiveSamplesApart() {
        standIns.alignerAnswer = { words, _ ->
            words.mapIndexed { number, word -> AlignedInterval(number * 0.05, number * 0.05 + 0.04, word) }
        }
    }

    private val wordsByTheEstimate = listOf("alpha", "beta", "gamma", "gamma", "delta.", "delta.")

    @Test
    fun aPieceWhoseAlignmentIsReadyBeforeItPlaysShowsTheWordsByTheAlignment() {
        alignWordsAtFiveSamplesApart()
        read(onePiece)
        standIns.runNextKeptWork()
        standIns.runKeptAlignmentWorks()

        // The slices are at 10, 20, 25, 35, 45 and 50 samples
        assertEquals(listOf("gamma", "delta.", "delta.", "delta.", "delta.", "delta."), wordsHeardAtEachSlice(onePiece))
    }

    @Test
    fun aPieceWhoseAlignmentIsNotReadyShowsTheWordsByTheEstimate() {
        alignWordsAtFiveSamplesApart()
        read(onePiece)

        assertEquals(wordsByTheEstimate, wordsHeardAtEachSlice(onePiece))
    }

    @Test
    fun anAlignmentThatArrivesDuringThePieceTakesOverFromThePositionAfterIt() {
        alignWordsAtFiveSamplesApart()
        read(onePiece)
        val words = mutableListOf<String?>()
        standIns.whilePlaying = { slice ->
            if (slice == 3) standIns.runKeptAlignmentWorks()
            words += heardReading().wordIn(onePiece)
        }

        standIns.runKeptWorks()

        // The alignment arrives at the third slice, whose position of 25 samples is already within delta
        assertEquals(wordsByTheEstimate.take(2) + List(4) { "delta." }, words)
    }

    @Test
    fun theWordIsShownTheDelayAfterItsStart() {
        alignWordsAtFiveSamplesApart()
        // 120 milliseconds are 12 samples, so beta, which starts at 5, is shown from 17, gamma, which starts at 10, from 22 and delta, which starts at 15, from 27
        standIns.wordDelayMilliseconds = 120
        read(onePiece)
        standIns.runNextKeptWork()
        standIns.runKeptAlignmentWorks()

        assertEquals(listOf("alpha", "beta", "gamma", "delta.", "delta.", "delta."), wordsHeardAtEachSlice(onePiece))
    }

    @Test
    fun aFailedAlignmentIsLoggedAndThePieceShowsTheEstimateAndTheReadingGoesOnToItsEnd() {
        standIns.aligningFails = IllegalStateException("no tier")
        read(onePiece)
        standIns.runNextKeptWork()
        standIns.runKeptAlignmentWorks()

        assertEquals(wordsByTheEstimate, wordsHeardAtEachSlice(onePiece))
        assertEquals(1, standIns.loggedAlignmentFailures.size)
        assertTrue(standIns.failures.isEmpty())
        assertNull(service.heardReading())
        assertEquals(1, standIns.readingTimeReports.size)
    }

    @Test
    fun anAlignmentWithoutAStartForEachWordShowsTheEstimate() {
        standIns.alignerAnswer = { words, _ -> listOf(AlignedInterval(0.0, 0.1, words.first())) }
        read(onePiece)
        standIns.runNextKeptWork()
        standIns.runKeptAlignmentWorks()

        assertEquals(wordsByTheEstimate, wordsHeardAtEachSlice(onePiece))
        assertTrue(standIns.loggedAlignmentFailures.isEmpty())
    }

    @Test
    fun whereTheAlignerModelIsNotInPlaceThePieceShowsTheEstimate() {
        alignWordsAtFiveSamplesApart()
        standIns.alignerModelInPlace = false
        read(onePiece)
        standIns.runNextKeptWork()
        standIns.runKeptAlignmentWorks()

        assertEquals(wordsByTheEstimate, wordsHeardAtEachSlice(onePiece))
        assertTrue(standIns.alignedWords.isEmpty())
    }

    @Test
    fun anAlignmentOfTheMakingBeforeAChangeOfTheSpeedIsNotUsed() {
        alignWordsAtFiveSamplesApart()
        read(onePiece)
        val words = mutableListOf<String?>()
        standIns.whilePlaying = { slice ->
            if (slice == 4 && standIns.players.size == 1) {
                service.changeSpeed(ReadingSpeed.FASTEST, afterAWait = false)
                standIns.runKeptAlignmentWorks()
            }
            words += heardReading().wordIn(onePiece)
        }

        standIns.runKeptWorks()

        // The second making plays its piece again by the estimate, as its own alignment never ran
        assertEquals(2, standIns.players.size)
        assertEquals(wordsByTheEstimate, words.drop(4))
    }

    @Test
    fun theAlignmentsAreKeptOverAPauseAndAResume() {
        alignWordsAtFiveSamplesApart()
        read(onePiece)
        standIns.runNextKeptWork()
        standIns.runKeptAlignmentWorks()
        standIns.whilePlaying = { slice -> if (slice == 4) service.pause(target) }
        standIns.runKeptWorks()
        val words = mutableListOf<String?>()

        service.resume(target, System.nanoTime())
        standIns.whilePlaying = { words += heardReading().wordIn(onePiece) }
        standIns.runKeptWorks()

        // The piece plays again from its first run, whose first slice is at 10 samples, and the estimate would give alpha
        assertEquals("gamma", words.first())
    }

    @Test
    fun theFirstSoundDoesNotWaitForAnAlignment() {
        read(onePiece)

        standIns.runKeptWorks()

        assertEquals(1, standIns.firstSounds.size)
        assertEquals(1, standIns.keptAlignmentWorks.size)
    }
}
