package com.ritenoar.aloudrite

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhonemeRowsTest {

    // The tokens are the letters a to z, the digits, some punctuation and the space
    private val tokenCodePoints = ('a'..'z').map { it.code }.toSet() + "0123456789 ;,.!?".map { it.code }

    private val askedTexts = mutableListOf<String>()

    // The stand-in for eSpeak NG gives the sentences it was set to, whatever the text
    private fun phonemeRows(vararg sentences: String) = PhonemeRows(tokenCodePoints) { text ->
        askedTexts += text
        sentences.map { sentence -> sentence.codePoints().toArray() }
    }

    private fun rows(vararg sentences: String): List<String> =
        phonemeRows(*sentences).rowsOf("text").map { String(it, 0, it.size) }

    private fun sizes(vararg sentences: String): List<Int> = phonemeRows(*sentences).rowsOf("text").map { it.size }

    // A sentence of the given number of tokens, which ends in a full stop
    private fun sentenceOf(tokens: Int) = "a".repeat(tokens - 1) + "."

    @Test
    fun theTextIsGivenToESpeakWholeAndEachSentenceItReturnsIsARow() {
        val phonemeRows = phonemeRows("first sentence.", "second sentence.")

        val rows = phonemeRows.rowsOf("One sentence. Another one.")

        assertEquals(listOf("One sentence. Another one."), askedTexts)
        assertEquals(listOf("first sentence.", "second sentence."), rows.map { String(it, 0, it.size) })
    }

    @Test
    fun eachReplacementOfPunctuationIsMadeBeforeESpeakIsAsked() {
        val expected = mapOf(
            "a:b" to "a,b",
            "a，b" to "a,b",
            "a、b" to "a,b",
            "a；b" to "a;b",
            "a。b" to "a.b",
            "a？b" to "a?b",
            "a！b" to "a!b",
        )
        for ((text, replaced) in expected) {
            askedTexts.clear()
            phonemeRows("a").rowsOf(text)
            assertEquals("for $text", listOf(replaced), askedTexts)
        }
    }

    @Test
    fun theFullWidthColonBecomesAColonAndIsNotReadAsACommaAfterThat() {
        phonemeRows("a").rowsOf("a：b")

        assertEquals(listOf("a:b"), askedTexts)
    }

    @Test
    fun aRunOfWhiteSpaceIsOneSpaceWhenESpeakIsAsked() {
        phonemeRows("a").rowsOf("one \n\t two\n\nthree")

        assertEquals(listOf("one two three"), askedTexts)
    }

    @Test
    fun aCodePointThatIsNotATokenIsLeftOut() {
        assertEquals(listOf("ab c"), rows("aˈbɑ c"))
    }

    @Test
    fun aSentenceOfNothingButCodePointsThatAreNotTokensGivesNoRow() {
        assertEquals(emptyList<String>(), rows("ˈɑ"))
    }

    @Test
    fun theTokensAreTheCodePointsOfTheLinesOfTheTokensFileAndTheSpaceIsOne() {
        val tokens = tokenCodePointsOf(listOf("\$ 0", "; 1", "  16", "a 43", "ˈ 156", "no 7"))

        assertEquals(setOf('$'.code, ';'.code, ' '.code, 'a'.code, 0x2C8), tokens)
    }

    @Test
    fun aPieceOfWhiteSpaceOnlyGivesNoRowAndESpeakIsNotAsked() {
        assertEquals(0, phonemeRows("a").rowsOf(" \n\t ").size)
        assertEquals(0, phonemeRows("a").rowsOf("").size)
        assertEquals(emptyList<String>(), askedTexts)
    }

    @Test
    fun aRowOfMoreThanTenTokensStandsAloneAndOneOfTenOrFewerIsJoinedToTheRowBefore() {
        assertEquals(listOf(21), sizes(sentenceOf(11), sentenceOf(10)))
        assertEquals(listOf(11, 11), sizes(sentenceOf(11), sentenceOf(11)))
    }

    @Test
    fun theFirstRowStandsAloneWhateverItsSize() {
        assertEquals(listOf(20), sizes(sentenceOf(10), sentenceOf(10)))
        assertEquals(listOf(3), sizes(sentenceOf(3)))
    }

    @Test
    fun aShortRowIsJoinedWhereTheTwoTogetherHaveFewerThan46TokensAndNotAt46() {
        assertEquals(listOf(45), sizes(sentenceOf(36), sentenceOf(9)))
        assertEquals(listOf(36, 10), sizes(sentenceOf(36), sentenceOf(10)))
    }

    @Test
    fun aRowOfTwoTokensOrFewerIsJoinedWhateverTheSumAndOneOfThreeIsNot() {
        assertEquals(listOf(52), sizes(sentenceOf(50), sentenceOf(2)))
        assertEquals(listOf(50, 3), sizes(sentenceOf(50), sentenceOf(3)))
    }

    @Test
    fun aRowOfExactlyTheLimitIsOneRow() {
        assertEquals(listOf(400), sizes(sentenceOf(400)))
    }

    @Test
    fun aShortRowIsJoinedToARowBeforeUpToTheLimitAndNotAboveIt() {
        assertEquals(listOf(400), sizes(sentenceOf(398), sentenceOf(2)))
        assertEquals(listOf(399, 2), sizes(sentenceOf(399), sentenceOf(2)))
    }

    @Test
    fun aSentenceOverTheLimitIsCutAfterTheLastSpaceThatKeepsARowWithinIt() {
        val sentence = "a".repeat(199) + " " + "a".repeat(200) + "."

        assertEquals(listOf(200, 201), sizes(sentence))
    }

    @Test
    fun aSentenceOverTheLimitWithoutASpaceInItsFirst400TokensIsCutAtTheLimit() {
        assertEquals(listOf(400, 1), sizes("a".repeat(401)))
    }

    @Test
    fun aSentenceOfSeveralTimesTheLimitIsCutIntoRowsWithinIt() {
        val sentence = List(300) { "word" }.joinToString(" ")

        val rows = phonemeRows(sentence).rowsOf("text")

        assertTrue(rows.size > 3 && rows.all { it.size <= MAXIMUM_TOKENS_PER_ROW })
    }

    @Test
    fun noRowOfALongTextHoldsMoreThanTheLimitAndTheRowsHoldEveryWordInOrder() {
        val words = (1..300).map { "w${it % 9}" + "a".repeat(it % 7) }
        val sentences = words.chunked(40).map { it.joinToString(" ") + "." }

        val rows = phonemeRows(*sentences.toTypedArray()).rowsOf("text")

        assertTrue(rows.all { it.size <= MAXIMUM_TOKENS_PER_ROW })
        val heard = rows.joinToString(" ") { String(it, 0, it.size).trim() }.split(Regex("[ .]+")).filter { it.isNotEmpty() }
        assertEquals(words, heard)
    }

    @Test
    fun aCharacterAboveFfffReachesESpeakAsASpaceThatSplitsTheWordsOnBothSides() {
        phonemeRows("a").rowsOf(withCharactersAboveFfffAsSpaces("one😀two"))

        assertEquals(listOf("one two"), askedTexts)
    }
}
