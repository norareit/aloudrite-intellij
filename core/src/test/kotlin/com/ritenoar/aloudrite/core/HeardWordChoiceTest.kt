package com.ritenoar.aloudrite.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HeardWordChoiceTest {

    private val markdown = "Zero one, two three.\n\nNext piece here."
    private val segments = ReadingSegments(whatToRead(markdown, PlaceInTheText.NoText).placedParts).segments
    private val firstPiece = 0
    private val silence = 1
    private val secondPiece = 2

    // Zero, one, two, three: the alignment of the first piece, at a sample rate of 100
    private val firstPieceAlignment = PieceAlignment(listOf(0L, 50L, 100L, 150L))

    private fun choiceOf(delayMilliseconds: Int = 0) = HeardWordChoice(segments, delayMilliseconds, sampleRate = 100)

    private fun HeardWordChoice.wordHeard(): String? = places?.let { markdown.substring(it.word.start, it.word.end) }

    @Test
    fun thereAreNoPlacesBeforeAnyPosition() {
        assertNull(choiceOf().places)
    }

    @Test
    fun aPieceWithAnAlignmentShowsTheWordByItInsteadOfTheShare() {
        val choice = choiceOf()
        choice.aligned(firstPiece, firstPieceAlignment)

        // The share of 0.0 gives the first word by the estimate, the position of 100 samples gives the third by the alignment
        choice.heard(firstPiece, 100, 0.0)

        assertEquals("two", choice.wordHeard())
    }

    @Test
    fun aPieceWithoutAnAlignmentShowsTheWordByTheEstimate() {
        val choice = choiceOf()
        choice.aligned(secondPiece, PieceAlignment(listOf(0L, 10L, 20L)))

        choice.heard(firstPiece, 100, 0.5)

        // The weights are 4, 3, 3 and 5, so the share of 0.5 is in the third word
        assertEquals("two", choice.wordHeard())
    }

    @Test
    fun theAlignmentIsUsedFromThePositionAfterItArrivedWhileThePieceWasHeard() {
        val choice = choiceOf()
        choice.heard(firstPiece, 100, 0.0)
        assertEquals("Zero", choice.wordHeard())

        choice.aligned(firstPiece, firstPieceAlignment)
        choice.heard(firstPiece, 160, 0.0)

        assertEquals("three.", choice.wordHeard())
    }

    @Test
    fun aSilenceKeepsTheLastWordOfThePieceBeforeIt() {
        val choice = choiceOf()
        choice.aligned(firstPiece, firstPieceAlignment)
        choice.heard(firstPiece, 100, 0.0)

        choice.heard(silence, 10, 0.5)

        assertEquals("three.", choice.wordHeard())
    }

    @Test
    fun theNextPieceStartsWithItsFirstWordWhetherOrNotItHasAnAlignment() {
        val choice = choiceOf()
        choice.aligned(secondPiece, PieceAlignment(listOf(30L, 60L, 90L)))
        choice.heard(firstPiece, 100, 1.0)

        choice.heard(secondPiece, 0, 0.0)

        assertEquals("Next", choice.wordHeard())
    }

    @Test
    fun aWordIsShownTheDelayAfterItsStart() {
        val choice = choiceOf(delayMilliseconds = 500)
        choice.aligned(firstPiece, firstPieceAlignment)

        // The delay is 50 samples, so the second word, which starts at 50, is shown from 100
        choice.heard(firstPiece, 99, 0.0)
        assertEquals("Zero", choice.wordHeard())
        choice.heard(firstPiece, 100, 0.0)
        assertEquals("one,", choice.wordHeard())
    }

    @Test
    fun forgettingTheAlignmentsGivesTheEstimateAgain() {
        val choice = choiceOf()
        choice.aligned(firstPiece, firstPieceAlignment)
        choice.forget()

        choice.heard(firstPiece, 150, 0.0)

        assertEquals("Zero", choice.wordHeard())
    }

    @Test
    fun theDelayInSamplesFollowsTheMillisecondsAndTheSampleRate() {
        assertEquals(1_680L, delayInSamples(70, 24_000))
        assertEquals(0L, delayInSamples(0, 24_000))
    }

    @Test
    fun theDelayOfThePropertyIsUsedWhereItIsAWholeNumberFromZero() {
        assertEquals(90, wordDelayMilliseconds("90"))
        assertEquals(0, wordDelayMilliseconds("0"))
    }

    @Test
    fun theConstantIsTheDelayWhereThePropertyIsMissingOrNotAWholeNumberFromZero() {
        assertEquals(WORD_DELAY_MILLISECONDS, wordDelayMilliseconds(null))
        assertEquals(WORD_DELAY_MILLISECONDS, wordDelayMilliseconds(""))
        assertEquals(WORD_DELAY_MILLISECONDS, wordDelayMilliseconds("ninety"))
        assertEquals(WORD_DELAY_MILLISECONDS, wordDelayMilliseconds("-5"))
        assertEquals(WORD_DELAY_MILLISECONDS, wordDelayMilliseconds("7.5"))
    }
}
