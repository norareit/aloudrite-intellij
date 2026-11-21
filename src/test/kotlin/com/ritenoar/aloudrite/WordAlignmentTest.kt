package com.ritenoar.aloudrite

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WordAlignmentTest {

    private fun heardWord(text: String) = HeardWord(SourceRange(0, text.length), SourceRange(0, text.length), text.length, text)

    private fun interval(start: Double, end: Double, text: String = "w") = AlignedInterval(start, end, text)

    @Test
    fun aWordIsSplitAtWhiteSpaceStrippedOfPunctuationAtBothEndsAndLowercased() {
        assertEquals(listOf("hello"), partsOf(heardWord("\"Hello,\"")))
        assertEquals(listOf("don't"), partsOf(heardWord("Don't")))
        assertEquals(listOf("well-known"), partsOf(heardWord("(Well-Known)!")))
        assertEquals(listOf("code", "example"), partsOf(heardWord("code example")))
        assertEquals(listOf("one", "two"), partsOf(heardWord("One \t\n Two")))
    }

    @Test
    fun aWordOfPunctuationOnlyHasNoParts() {
        assertEquals(emptyList<String>(), partsOf(heardWord("-")))
        assertEquals(emptyList<String>(), partsOf(heardWord("...")))
    }

    @Test
    fun theTextToAlignIsThePartsOfAllWordsJoinedBySingleSpaces() {
        val parts = listOf(listOf("hi"), emptyList(), listOf("code", "example"), listOf("42"))

        assertEquals("hi code example 42", textToAlign(parts))
    }

    @Test
    fun eachWordStartsWhereItsFirstPartDoesInSamples() {
        val parts = listOf(listOf("hi"), listOf("code", "example"), listOf("you"))
        val intervals = listOf(interval(0.0, 0.1), interval(0.5, 0.8), interval(0.8, 1.0), interval(1.25, 1.5))

        val alignment = checkNotNull(wordStartsOf(parts, intervals, 24_000))

        assertEquals(listOf<Long?>(0L, 12_000L, 30_000L), alignment.starts)
    }

    @Test
    fun aWordWithoutPartsHasNoStartAndBlankIntervalsAreSkipped() {
        val parts = listOf(listOf("hi"), emptyList(), listOf("you"))
        val intervals = listOf(interval(0.0, 0.1, ""), interval(0.1, 0.5, "hi"), interval(0.5, 0.6, " "), interval(0.6, 1.0, "you"))

        val alignment = checkNotNull(wordStartsOf(parts, intervals, 1_000))

        assertEquals(listOf<Long?>(100L, null, 600L), alignment.starts)
    }

    @Test
    fun fewerIntervalsThanPartsGiveNoAlignment() {
        assertNull(wordStartsOf(listOf(listOf("a"), listOf("b")), listOf(interval(0.0, 0.1)), 1_000))
    }

    @Test
    fun moreIntervalsThanPartsGiveNoAlignment() {
        assertNull(wordStartsOf(listOf(listOf("a")), listOf(interval(0.0, 0.1), interval(0.1, 0.2)), 1_000))
    }

    @Test
    fun aStartBeforeTheStartBeforeItGivesNoAlignment() {
        val intervals = listOf(interval(0.5, 0.6), interval(0.2, 0.3))

        assertNull(wordStartsOf(listOf(listOf("a"), listOf("b")), intervals, 1_000))
    }

    @Test
    fun aPieceOfNoPartsHasAnAlignmentOfNoStarts() {
        val alignment = checkNotNull(wordStartsOf(listOf(emptyList(), emptyList()), emptyList(), 1_000))

        assertEquals(listOf<Long?>(null, null), alignment.starts)
    }

    @Test
    fun theWordShownIsTheLastWhoseStartPlusTheDelayIsAtOrBeforeThePosition() {
        val starts = listOf<Long?>(100L, 200L, 300L)

        assertEquals(0, wordShownAt(starts, 109, 10))
        assertEquals(1, wordShownAt(starts, 210, 10))
        assertEquals(1, wordShownAt(starts, 309, 10))
        assertEquals(2, wordShownAt(starts, 310, 10))
        assertEquals(2, wordShownAt(starts, 10_000, 10))
    }

    @Test
    fun beforeTheFirstStartPlusTheDelayTheFirstWordIsShown() {
        assertEquals(0, wordShownAt(listOf(100L, 200L), 0, 10))
        assertEquals(0, wordShownAt(listOf(100L, 200L), 105, 10))
    }

    @Test
    fun throughAGapBetweenTwoWordsTheWordBeforeStaysShown() {
        val starts = listOf<Long?>(100L, 5_000L)

        assertEquals(0, wordShownAt(starts, 4_999, 0))
        assertEquals(1, wordShownAt(starts, 5_000, 0))
    }

    @Test
    fun aWordWithoutAStartIsNeverShown() {
        val starts = listOf<Long?>(null, 100L, null, 200L, null)

        assertEquals(1, wordShownAt(starts, 0, 0))
        assertEquals(1, wordShownAt(starts, 150, 0))
        assertEquals(3, wordShownAt(starts, 250, 0))
        assertEquals(3, wordShownAt(starts, 10_000, 0))
    }

    @Test
    fun noWordIsShownWhereNoWordHasAStart() {
        assertNull(wordShownAt(listOf(null, null), 100, 0))
        assertNull(wordShownAt(emptyList(), 100, 0))
    }

    @Test
    fun aDelayOfZeroShowsAWordAtItsStart() {
        assertEquals(1, wordShownAt(listOf<Long?>(0L, 100L), 100, 0))
    }
}
