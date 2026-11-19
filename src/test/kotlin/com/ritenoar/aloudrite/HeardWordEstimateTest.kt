package com.ritenoar.aloudrite

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private val EMOJI = String(Character.toChars(0x1F600))

class HeardWordEstimateTest {

    private val markdown = "Zero one, two three.\n\nNext piece here."
    private val segments = ReadingSegments(whatToRead(markdown, PlaceInTheText.NoText).placedParts)
    private val firstPiece = 0
    private val silence = 1
    private val secondPiece = 2

    private fun estimateOf(text: String = markdown) =
        HeardWordEstimate(ReadingSegments(whatToRead(text, PlaceInTheText.NoText).placedParts).segments)

    private fun HeardWordEstimate.wordHeard(text: String = markdown): String? =
        places?.let { text.substring(it.word.start, it.word.end) }

    private fun HeardWordEstimate.sentenceHeard(text: String = markdown): String? =
        places?.let { text.substring(it.sentence.start, it.sentence.end) }

    @Test
    fun thereAreNoPlacesBeforeAnyPosition() {
        assertNull(estimateOf().places)
    }

    @Test
    fun theFirstWordIsHeardAtAShareOfZeroAndTheLastAtAShareOfOne() {
        val estimate = estimateOf()

        estimate.heard(firstPiece, 0, 0.0)
        assertEquals("Zero", estimate.wordHeard())

        estimate.heard(firstPiece, 100, 1.0)
        assertEquals("three.", estimate.wordHeard())
    }

    @Test
    fun aShareBetweenTwoRunningSumsGivesTheWordAfterTheLowerOne() {
        val estimate = estimateOf()

        // The weights of Zero, one, two and three. are 4, 3, 3 and 5, so the sums are 4, 7, 10 and 15, and 0.5 of 15 is 7.5
        estimate.heard(firstPiece, 10, 0.5)

        assertEquals("two", estimate.wordHeard())
    }

    @Test
    fun aCommaOrADashDoesNotShiftTheWord() {
        val text = "aa - bb, cc"
        val estimate = estimateOf(text)

        // The weights are 2, 0, 2 and 2, so a share of 0.5 is 3 of 6, which is in the third word, not the dash
        estimate.heard(0, 10, 0.5)

        assertEquals("bb,", estimate.wordHeard(text))
    }

    @Test
    fun aSegmentOfWeightZeroGivesItsFirstWord() {
        val text = "\u2014 \u2013"
        val estimate = estimateOf(text)

        estimate.heard(0, 10, 0.7)

        assertEquals("\u2014", estimate.wordHeard(text))
        assertEquals(0, estimate.places!!.word.start)
    }

    @Test
    fun aLaterPositionWithALowerShareInTheSameSegmentKeepsTheWord() {
        val estimate = estimateOf()
        estimate.heard(firstPiece, 10, 0.8)
        val word = estimate.wordHeard()

        // More audio of the segment arrived and lengthened it
        estimate.heard(firstPiece, 20, 0.4)

        assertEquals(word, estimate.wordHeard())
    }

    @Test
    fun aPositionWithFewerSamplesInTheSameSegmentStartsAfreshAtTheWordOfItsShare() {
        val estimate = estimateOf()
        estimate.heard(firstPiece, 30, 0.9)

        estimate.heard(firstPiece, 0, 0.0)

        assertEquals("Zero", estimate.wordHeard())
    }

    @Test
    fun aPositionInAnEarlierSegmentStartsAfresh() {
        val estimate = estimateOf()
        estimate.heard(secondPiece, 30, 0.9)

        estimate.heard(firstPiece, 10, 0.1)

        assertEquals("Zero", estimate.wordHeard())
    }

    @Test
    fun aPositionInASilenceKeepsTheWordAndTheSentenceOfThePieceBeforeIt() {
        val estimate = estimateOf()
        estimate.heard(firstPiece, 30, 1.0)
        val before = estimate.places

        estimate.heard(silence, 10, 0.5)

        assertEquals(before, estimate.places)
    }

    @Test
    fun theFirstPositionAfterTheSilenceGivesTheFirstWordOfTheNextPiece() {
        val estimate = estimateOf()
        estimate.heard(firstPiece, 30, 1.0)
        estimate.heard(silence, 10, 0.5)

        estimate.heard(secondPiece, 0, 0.0)

        assertEquals("Next", estimate.wordHeard())
    }

    @Test
    fun thePlacesAreTheWordsAndTheSentences() {
        val estimate = estimateOf()

        estimate.heard(secondPiece, 5, 0.5)

        val places = estimate.places!!
        assertEquals("piece", markdown.substring(places.word.start, places.word.end))
        assertEquals("Next piece here.", markdown.substring(places.sentence.start, places.sentence.end))
    }

    @Test
    fun theWordAtAShareFollowsTheRunningSumsOfTheWeights() {
        val words = listOf(1, 0, 2, 1).map { HeardWord(SourceRange(0, 1), SourceRange(0, 1), it) }

        assertEquals(listOf(0, 0, 2, 2, 3, 3), listOf(0.0, 0.2, 0.3, 0.7, 0.8, 1.0).map { wordAtShare(words, it) })
    }

    @Test
    fun enteringASilenceFinishesTheWordOfThePieceBeforeItWithoutAShareOfOne() {
        val text = "encyclopedia a.\n\nNext."
        val estimate = estimateOf(text)
        estimate.heard(0, 30, 0.9)
        assertEquals("encyclopedia", estimate.wordHeard(text))

        estimate.heard(1, 5, 0.1)

        assertEquals("a.", estimate.wordHeard(text))
    }

    @Test
    fun aSilenceReachedPastAShortPieceFinishesThatPieceRatherThanTheOneHeardLast() {
        val text = "One two.\n\nThree four.\n\nFive."
        val estimate = estimateOf(text)
        estimate.heard(0, 10, 0.1)

        estimate.heard(3, 5, 0.1)

        assertEquals("four.", estimate.wordHeard(text))
        assertEquals("Three four.", estimate.sentenceHeard(text))
    }

    @Test
    fun theSecondOfARunOfSilencesFinishesThePieceBeforeTheRun() {
        val emoji = String(Character.toChars(0x1F600))
        val text = "encyclopedia a. `$emoji` Next."
        val segments = ReadingSegments(whatToRead(text, PlaceInTheText.NoText).placedParts).segments
        assertEquals(listOf(true, false, false, true), segments.map { it.words.isNotEmpty() })
        val estimate = estimateOf(text)
        estimate.heard(0, 30, 0.9)

        estimate.heard(2, 5, 0.1)

        assertEquals("a.", estimate.wordHeard(text))
    }

    @Test
    fun aPieceHeardAgainAfterTheSilenceAfterItStartsAtItsShareAgain() {
        val estimate = estimateOf()
        estimate.heard(firstPiece, 100, 0.9)
        estimate.heard(silence, 5, 0.1)

        estimate.heard(firstPiece, 0, 0.0)

        assertEquals("Zero", estimate.wordHeard())
    }

    @Test
    fun aSilenceBeforeAnyPieceLeavesNoPlaces() {
        val text = "`$EMOJI` Hello."
        val estimate = estimateOf(text)
        val segments = ReadingSegments(whatToRead(text, PlaceInTheText.NoText).placedParts).segments
        assertEquals(false, segments.first().words.isNotEmpty())

        estimate.heard(0, 5, 0.1)

        assertNull(estimate.places)
    }
}
