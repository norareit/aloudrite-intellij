package com.ritenoar.aloudrite

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayedAudioTest {

    private val playedAudio = PlayedAudio()

    @Test
    fun aPositionInsideAPieceResumesAtTheFirstRunOfThatPieceAndKeepsEveryLaterRun() {
        val first = piece(0, 10)
        val second = piece(1, 10)
        val silence = silence(2, 5)
        val third = piece(3, 10)
        takeAll(first, second, silence, third)

        val resumption = playedAudio.resumptionAt(samplesHeard = 14)

        assertEquals(listOf(second, silence, third), resumption.runs)
        assertFalse(resumption.skipsSilence)
    }

    @Test
    fun aPositionInsideALaterRunOfAPieceOfSeveralRunsResumesAtItsFirstRun() {
        val first = piece(0, 10)
        val secondStart = piece(1, 10)
        val secondEnd = piece(1, 10)
        takeAll(first, secondStart, secondEnd)

        val resumption = playedAudio.resumptionAt(samplesHeard = 25)

        assertEquals(listOf(secondStart, secondEnd), resumption.runs)
    }

    @Test
    fun aPositionAtTheStartOfAPieceResumesAtThatPiece() {
        val first = piece(0, 10)
        val second = piece(1, 10)
        takeAll(first, second)

        assertEquals(listOf(second), playedAudio.resumptionAt(samplesHeard = 10).runs)
    }

    @Test
    fun aPositionPastWhatWasWrittenResumesAtTheLastSegmentWritten() {
        val first = piece(0, 10)
        val secondStart = piece(1, 10)
        val secondEnd = piece(1, 10)
        takeAll(first, secondStart, secondEnd)

        val resumption = playedAudio.resumptionAt(samplesHeard = 100)

        assertEquals(listOf(secondStart, secondEnd), resumption.runs)
    }

    @Test
    fun aPositionInsideASilenceKeepsTheRunsFromTheSilenceOnAndSaysThatSilenceIsToBeSkipped() {
        val before = piece(0, 10)
        val silence = silence(1, 20)
        val after = piece(2, 10)
        takeAll(before, silence, after)

        val resumption = playedAudio.resumptionAt(samplesHeard = 15)

        assertEquals(listOf(silence, after), resumption.runs)
        assertTrue(resumption.skipsSilence)
    }

    @Test
    fun aSegmentThePlayerHasMadeAudibleInFullIsForgottenAndOneMadeAudibleInPartIsKept() {
        val first = piece(0, 10)
        val secondStart = piece(1, 10)
        val secondEnd = piece(1, 10)
        val third = piece(2, 10)

        playedAudio.taken(first, samplesHeard = 0)
        playedAudio.taken(secondStart, samplesHeard = 0)
        assertEquals(listOf(first, secondStart), playedAudio.runsKept())

        playedAudio.taken(secondEnd, samplesHeard = 10)
        assertEquals(listOf(secondStart, secondEnd), playedAudio.runsKept())

        playedAudio.taken(third, samplesHeard = 25)
        assertEquals(listOf(secondStart, secondEnd, third), playedAudio.runsKept())
    }

    @Test
    fun theLastSegmentIsKeptAlsoWhenItWasHeardInFull() {
        val only = piece(0, 10)
        val more = piece(0, 10)

        playedAudio.taken(only, samplesHeard = 0)
        playedAudio.taken(more, samplesHeard = 10)

        assertEquals(listOf(only, more), playedAudio.runsKept())
    }

    @Test
    fun nothingWrittenGivesAnEmptyResumptionThatSkipsNothing() {
        val resumption = playedAudio.resumptionAt(samplesHeard = 0)

        assertTrue(resumption.runs.isEmpty())
        assertFalse(resumption.skipsSilence)
    }

    @Test
    fun afterAResumptionTheOffsetsStartAgainAtZero() {
        takeAll(piece(0, 10), piece(1, 10))
        playedAudio.resumptionAt(samplesHeard = 15)

        val again = piece(1, 10)
        val next = piece(2, 10)
        takeAll(again, next)

        assertEquals(listOf(again, next), playedAudio.resumptionAt(samplesHeard = 5).runs)
        assertTrue(playedAudio.runsKept().isEmpty())
    }

    private fun takeAll(vararg runs: AudioRun) {
        for (run in runs) playedAudio.taken(run, samplesHeard = 0)
    }

    private fun piece(segment: Int, length: Int) = AudioRun(segment, isSilence = false, samples = FloatArray(length) { 0.5f })

    private fun silence(segment: Int, length: Int) = AudioRun(segment, isSilence = true, samples = FloatArray(length))
}
