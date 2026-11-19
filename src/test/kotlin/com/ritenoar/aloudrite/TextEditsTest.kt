package com.ritenoar.aloudrite

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TextEditsTest {

    private val range = SourceRange(10, 20)

    private fun editsOf(vararg edits: Triple<Int, Int, Int>) = TextEdits().also { text ->
        edits.forEach { (offset, oldLength, newLength) -> text.record(offset, oldLength, newLength) }
    }

    @Test
    fun noEditLeavesARangeAsItIs() {
        assertEquals(range, TextEdits().map(range))
    }

    @Test
    fun anInsertionBeforeARangeShiftsIt() {
        assertEquals(SourceRange(13, 23), editsOf(Triple(2, 0, 3)).map(range))
    }

    @Test
    fun anInsertionAfterARangeLeavesIt() {
        assertEquals(range, editsOf(Triple(25, 0, 3)).map(range))
    }

    @Test
    fun anInsertionInsideARangeWidensIt() {
        assertEquals(SourceRange(10, 23), editsOf(Triple(15, 0, 3)).map(range))
    }

    @Test
    fun anInsertionAtTheStartOfARangeShiftsItAndOneAtItsEndLeavesIt() {
        assertEquals(SourceRange(13, 23), editsOf(Triple(10, 0, 3)).map(range))
        assertEquals(range, editsOf(Triple(20, 0, 3)).map(range))
    }

    @Test
    fun aDeletionOfPartOfARangeShrinksIt() {
        assertEquals(SourceRange(10, 17), editsOf(Triple(12, 3, 0)).map(range))
        assertEquals(SourceRange(10, 14), editsOf(Triple(14, 10, 0)).map(range))
        assertEquals(SourceRange(5, 10), editsOf(Triple(5, 10, 0)).map(range))
    }

    @Test
    fun aDeletionOrAReplacementOfAllOfARangeGivesNothing() {
        assertNull(editsOf(Triple(10, 10, 0)).map(range))
        assertNull(editsOf(Triple(8, 15, 0)).map(range))
        assertNull(editsOf(Triple(10, 10, 4)).map(range))
    }

    @Test
    fun editsInARowAreAppliedInTheirOrder() {
        val edits = editsOf(Triple(0, 0, 5), Triple(18, 0, 2), Triple(0, 5, 0))

        // Five characters in front, two inside, and the five in front taken out again
        assertEquals(SourceRange(10, 22), edits.map(range))
    }

    @Test
    fun aRangeThatCameOutEmptyStaysGone() {
        val edits = editsOf(Triple(10, 10, 0), Triple(10, 0, 5))

        assertNull(edits.map(range))
    }
}
