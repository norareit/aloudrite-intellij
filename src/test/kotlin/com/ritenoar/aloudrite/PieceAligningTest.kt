package com.ritenoar.aloudrite

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PieceAligningTest {

    private class StandInAligner(var intervals: List<AlignedInterval>, var failure: Throwable? = null) : WordAligner {
        var closed = false
        val wordsGiven = mutableListOf<List<String>>()

        override fun align(samples: FloatArray, sampleRate: Int, words: List<String>): List<AlignedInterval> {
            wordsGiven += words
            failure?.let { throw it }
            return intervals
        }

        override fun close() {
            closed = true
        }
    }

    private val parts = listOf(listOf("one"), listOf("two"))
    private val intervals = listOf(AlignedInterval(0.0, 0.5, "one"), AlignedInterval(0.5, 1.0, "two"))

    private var modelInPlace = true
    private val aligners = mutableListOf<StandInAligner>()
    private val failures = mutableListOf<Throwable>()
    private val works = mutableListOf<() -> Unit>()
    private var creationFailure: Throwable? = null

    private val aligning = PieceAligning(
        isModelInPlace = { modelInPlace },
        createAligner = {
            creationFailure?.let { throw it }
            StandInAligner(intervals).also { aligners += it }
        },
        runOnItsThread = { work -> works += work },
        logFailure = { failures += it },
    )

    // Runs the work of the piece and gives what it called back with, and null where it did not call back at all
    private fun alignAPiece(partsOfWords: List<List<String>> = parts): Pair<Boolean, PieceAlignment?> {
        var called = false
        var given: PieceAlignment? = null
        aligning.align(FloatArray(100), 100, partsOfWords) { called = true; given = it }
        works.toList().also { works.clear() }.forEach { it() }
        return called to given
    }

    @Test
    fun theStartsOfTheWordsAreGivenWhereTheModelIsInPlace() {
        val (called, alignment) = alignAPiece()

        assertTrue(called)
        assertEquals(listOf<Long?>(0L, 50L), alignment!!.starts)
    }

    @Test
    fun nothingIsGivenAndNoAlignerIsMadeWhereTheModelIsNotInPlace() {
        modelInPlace = false

        val (called, alignment) = alignAPiece()

        assertTrue(called)
        assertNull(alignment)
        assertTrue(aligners.isEmpty())
    }

    @Test
    fun theWorkIsNotRunWhereTheCallerDoesNotRunIt() {
        var called = false

        aligning.align(FloatArray(100), 100, parts) { called = true }

        assertTrue(!called)
        assertEquals(1, works.size)
    }

    @Test
    fun onePieceOfPunctuationOnlyGivesNothingWithoutAligning() {
        val (called, alignment) = alignAPiece(listOf(emptyList()))

        assertTrue(called)
        assertNull(alignment)
        assertTrue(aligners.isEmpty())
    }

    @Test
    fun oneAlignerServesSeveralPieces() {
        alignAPiece()
        alignAPiece()

        assertEquals(1, aligners.size)
        assertEquals(2, aligners.single().wordsGiven.size)
        assertEquals(listOf("one", "two"), aligners.single().wordsGiven.first())
    }

    @Test
    fun theAlignerIsReleasedWhenTheModelIsNoLongerInPlaceAndMadeAgainWhenItIsBack() {
        alignAPiece()
        modelInPlace = false
        assertNull(alignAPiece().second)
        assertTrue(aligners.single().closed)

        modelInPlace = true
        assertNotNull(alignAPiece().second)
        assertEquals(2, aligners.size)
    }

    @Test
    fun aFailureOfAligningIsLoggedAndGivesNothing() {
        alignAPiece()
        val failure = IllegalStateException("no tier")
        aligners.single().failure = failure

        val (called, alignment) = alignAPiece()

        assertTrue(called)
        assertNull(alignment)
        assertEquals(listOf<Throwable>(failure), failures)
    }

    @Test
    fun aFailureOfMakingTheAlignerIsLoggedAndGivesNothingAndTheNextPieceTriesAgain() {
        val failure = UnsatisfiedLinkError("no library")
        creationFailure = failure

        assertNull(alignAPiece().second)
        assertEquals(listOf<Throwable>(failure), failures)

        creationFailure = null
        assertNotNull(alignAPiece().second)
    }

    @Test
    fun intervalsThatDoNotFitTheWordsGiveNothing() {
        alignAPiece()
        aligners.single().intervals = intervals.take(1)

        assertNull(alignAPiece().second)
        assertTrue(failures.isEmpty())
    }

    @Test
    fun closingReleasesTheAlignerAndLaterPiecesGetNothing() {
        alignAPiece()

        aligning.close()

        assertTrue(aligners.single().closed)
        assertNull(alignAPiece().second)
        assertEquals(1, aligners.size)
    }
}
