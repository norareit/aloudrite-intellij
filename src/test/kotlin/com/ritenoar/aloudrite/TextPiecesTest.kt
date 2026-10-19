package com.ritenoar.aloudrite

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextPiecesTest {

    @Test
    fun oneSentenceIsOnePiece() {
        assertEquals(listOf("This is one sentence."), piecesOfChecked("This is one sentence."))
    }

    @Test
    fun eachSentenceEndEndsAPieceWithTheWhiteSpaceAfterIt() {
        assertEquals(listOf("One. ", "Two!  ", "Three?\n", "Four"), piecesOfChecked("One. Two!  Three?\nFour"))
    }

    @Test
    fun aSentenceEndsAfterTheMarksThatCloseIt() {
        assertEquals(listOf("He said \"yes.\" ", "Then"), piecesOfChecked("He said \"yes.\" Then"))
        assertEquals(listOf("(See this.) ", "Then"), piecesOfChecked("(See this.) Then"))
        assertEquals(listOf("It is **done.** ", "Then"), piecesOfChecked("It is **done.** Then"))
    }

    @Test
    fun aFullStopThatIsNotFollowedByWhiteSpaceEndsNothing() {
        assertEquals(listOf("3.14 is a number"), piecesOfChecked("3.14 is a number"))
        assertEquals(listOf("file.md is a file"), piecesOfChecked("file.md is a file"))
    }

    @Test
    fun aClauseMarkAfterAtLeast60BytesEndsAPiece() {
        val start = "This part of the sentence is long enough to be made on its own"

        for (mark in listOf(",", ":", ";")) {
            assertEquals(listOf("$start$mark ", "and then it goes on."), piecesOfChecked("$start$mark and then it goes on."))
        }
    }

    @Test
    fun aClauseMarkBefore60BytesEndsNothing() {
        assertEquals(listOf("Short, and short again."), piecesOfChecked("Short, and short again."))
    }

    @Test
    fun aListOfShortWordsIsCutOnlyOnceEachPartHas60Bytes() {
        val text = "It holds headings, bold and italic words, lists, links, code, quotes, a table and a few emoji, and more."

        assertEquals(
            listOf("It holds headings, bold and italic words, lists, links, code, ", "quotes, a table and a few emoji, and more."),
            piecesOfChecked(text))
    }

    @Test
    fun aCommaNotFollowedByWhiteSpaceEndsNothing() {
        val start = "This sentence is long enough to be cut at the comma that follows here, "
        val rest = "but 1,000 and 2,000,000 are numbers that stay whole, as they should."

        assertEquals(listOf(start, rest), piecesOfChecked(start + rest))
    }

    @Test
    fun aBlankLineEndsAPiece() {
        assertEquals(listOf("# Title\n\n", "Text."), piecesOfChecked("# Title\n\nText."))
    }

    @Test
    fun aSingleLineBreakEndsNothing() {
        val list = "- first item\n- second item\n- third item"

        assertEquals(listOf(list), piecesOfChecked(list))
    }

    @Test
    fun aBlankLineOfSpacesEndsAPiece() {
        assertEquals(listOf("- item\n  \t\n", "- next"), piecesOfChecked("- item\n  \t\n- next"))
    }

    @Test
    fun tenThousandWordsOfProseGiveOnlyShortPieces() {
        val sentence = "The quick brown fox jumps over the lazy dog, and then it runs away into the woods. "
        val text = sentence.repeat(10_000 / sentence.split(' ').count { it.isNotEmpty() } + 1)

        val pieces = piecesOfChecked(text)

        assertTrue(pieces.size > 100)
    }

    @Test
    fun aLongSentenceWithoutPunctuationIsCutAtWhiteSpace() {
        val text = (1..200).joinToString(" ") { "word$it" }

        val pieces = piecesOfChecked(text)

        assertTrue(pieces.size > 1)
        pieces.dropLast(1).forEach { assertTrue("The piece \"$it\" does not end in white space", it.last() == ' ') }
    }

    @Test
    fun aLongRunWithoutWhiteSpaceIsCutAtTheLimit() {
        val pieces = piecesOfChecked("x".repeat(2_000))

        assertEquals(listOf(400, 400, 400, 400, 400), pieces.map { it.length })
    }

    @Test
    fun aLongRunOfEmojiIsCutBetweenTwoSurrogatePairs() {
        val emoji = "\uD83D\uDE00"

        val pieces = piecesOfChecked(emoji.repeat(500))

        pieces.forEach { piece ->
            assertTrue(piece.first().isHighSurrogate())
            assertTrue(piece.last().isLowSurrogate())
        }
        assertEquals(100, pieces.first().length / 2)
    }

    @Test
    fun theEmptyTextGivesNoPiece() {
        assertEquals(emptyList<String>(), piecesOfChecked(""))
    }

    @Test
    fun aTextOfWhiteSpaceOnlyGivesOnePiece() {
        assertEquals(listOf(" \n\n\t "), piecesOfChecked(" \n\n\t "))
    }

    private fun piecesOfChecked(text: String): List<String> {
        val pieces = piecesOf(text)

        assertEquals(text, pieces.joinToString(""))
        pieces.forEach { piece ->
            val bytes = piece.toByteArray(Charsets.UTF_8).size
            assertTrue("A piece is $bytes bytes long", bytes <= MAXIMUM_PIECE_BYTES)
        }
        return pieces
    }
}
