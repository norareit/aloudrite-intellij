package com.ritenoar.aloudrite

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KokoroFrontEndTest {

    // The tokens are the letters a to z, the digits, the punctuation of the front end and the space
    private val tokenCodePoints = ('a'..'z').map { it.code }.toSet() + "0123456789 ;:,.!?—…\"()“”".map { it.code }

    private val askedWords = mutableListOf<String>()

    private fun frontEnd(
        lexiconLines: List<String> = listOf("hello h h", "world w w"),
        phonemes: (String) -> String = { "zz" },
    ) = KokoroFrontEnd(lexiconOf(lexiconLines, tokenCodePoints), tokenCodePoints) { word ->
        askedWords += word
        phonemes(word)
    }

    private fun rows(text: String, frontEnd: KokoroFrontEnd = frontEnd()): List<String> =
        frontEnd.rowsOf(text).map { row -> String(row, 0, row.size) }

    // A word "t10" has 10 tokens with the space that follows it, in a front end with an empty lexicon
    private fun frontEndOfSizedWords() = frontEnd(emptyList()) { word -> "a".repeat(word.drop(1).toInt() - 1) }

    private fun sizes(text: String): List<Int> = frontEndOfSizedWords().rowsOf(text).map { it.size }

    @Test
    fun aWordOfTheLexiconGivesItsTokensAndASpaceAndESpeakIsNotAsked() {
        assertEquals(listOf("hh "), rows("hello"))
        assertEquals(emptyList<String>(), askedWords)
    }

    @Test
    fun aWordNotInTheLexiconGivesThePhonemesOfTheStandInAndASpace() {
        assertEquals(listOf("kk "), rows("cats", frontEnd { "kk" }))
        assertEquals(listOf("cats"), askedWords)
    }

    @Test
    fun aCapitalizedWordIsLookedUpAndHandedOverInLowercase() {
        assertEquals(listOf("hh "), rows("Hello"))
        assertEquals(listOf("kk "), rows("CATS", frontEnd { "kk" }))
        assertEquals(listOf("cats"), askedWords)
    }

    @Test
    fun aCodePointThatIsNotATokenIsLeftOutOfThePhonemes() {
        assertEquals(listOf("ab "), rows("cats", frontEnd { "aˈbɑ" }))
    }

    @Test
    fun aWordThatGivesNoPhonemesStillAddsASpace() {
        assertEquals(listOf("hh  ww "), rows("hello _ world", frontEnd { "" }))
    }

    @Test
    fun eachPunctuationMarkOfTheRuleAddsItsToken() {
        val marks = listOf(";", ":", ",", ".", "!", "?", "—", "…", "\"", "(", ")", "“", "”")
        for (mark in marks) {
            val expected = if (mark == ":") "," else mark
            assertEquals("for $mark", listOf("hh $expected"), rows("hello$mark"))
        }
    }

    @Test
    fun theFullWidthMarksAreReadAsTheirOrdinaryOnes() {
        assertEquals(listOf("hh ,ww ."), rows("hello，world。"))
        assertEquals(listOf("hh ,ww ;"), rows("hello、world；"))
        assertEquals(listOf("hh ?"), rows("hello？"))
        assertEquals(listOf("hh !"), rows("hello！"))
    }

    @Test
    fun aColonIsReadAsACommaAndTheFullWidthColonAsAColon() {
        assertEquals(rows("hello,"), rows("hello:"))
        assertEquals(listOf("hh :"), rows("hello："))
    }

    @Test
    fun aFullStopExclamationQuestionAndSemicolonEndARowAndACommaDoesNot() {
        // 12 tokens are more than a row can take to be joined to the one before
        val twelve = "t11"

        assertEquals(listOf(12, 12), sizes("$twelve. $twelve."))
        assertEquals(listOf(12, 12), sizes("$twelve! $twelve?"))
        assertEquals(listOf(12, 12), sizes("$twelve; $twelve;"))
        assertEquals(listOf(24), sizes("$twelve, $twelve,"))
    }

    @Test
    fun aNumberIsHandedToESpeakWholeAndADecimalAsThreeWords() {
        rows("2026")
        assertEquals(listOf("2026"), askedWords)
        askedWords.clear()
        rows("4.2")
        assertEquals(listOf("4", "2"), askedWords)
    }

    @Test
    fun aWordWithAnUnderscoreOrDotsIsSplitAtThemAndEachMarkIsAWordOfItsOwn() {
        rows("x86_64")
        assertEquals(listOf("x86", "_", "64"), askedWords)
        askedWords.clear()
        rows("v1.13.8")
        assertEquals(listOf("v1", "13", "8"), askedWords)
    }

    @Test
    fun anApostropheAndALetterWithADiacriticStayInsideTheWord() {
        rows("don't")
        rows("öffnen")
        assertEquals(listOf("don't", "öffnen"), askedWords)
    }

    @Test
    fun aSpaceInPlaceOfACharacterAboveFfffSplitsTheWordsOnBothSidesAndAddsNothing() {
        val text = withCharactersAboveFfffAsSpaces("hello😀world")

        assertEquals(rows("hello world"), rows(text))
        assertEquals(listOf("hh ww "), rows(text))
    }

    @Test
    fun aPieceOfWhiteSpaceOnlyGivesNoRow() {
        assertEquals(0, frontEnd().rowsOf(" \n\t ").size)
        assertEquals(0, frontEnd().rowsOf("").size)
    }

    @Test
    fun aRowOfMoreThanTenTokensStandsAloneAndOneOfTenOrFewerIsJoinedToTheRowBefore() {
        // The full stop is a token as well: "t9." has 10 tokens and "t10." has 11
        assertEquals(listOf(22), sizes("t11. t9."))
        assertEquals(listOf(12, 11), sizes("t11. t10."))
    }

    @Test
    fun theFirstRowStandsAloneWhateverItsSize() {
        assertEquals(listOf(20), sizes("t9. t9."))
        assertEquals(listOf(3), sizes("t2."))
    }

    @Test
    fun aShortRowIsJoinedWhereTheTwoTogetherHaveFewerThan46TokensAndNotAt46() {
        assertEquals(listOf(45), sizes("t35. t8."))
        assertEquals(listOf(36, 10), sizes("t35. t9."))
    }

    @Test
    fun aRowOfTwoTokensOrFewerIsJoinedWhateverTheSumAndOneOfThreeIsNot() {
        assertEquals(listOf(53), sizes("t50. t1."))
        assertEquals(listOf(51, 3), sizes("t50. t2."))
    }

    @Test
    fun aRowOfExactlyTheLimitIsOneRowAndAWordThatBringsItOverStartsANewOne() {
        assertEquals(listOf(400), sizes("t400"))
        assertEquals(listOf(399, 2), sizes("t399 t2"))
    }

    @Test
    fun aShortRowIsNotJoinedToARowBeforeWhereTheTwoWouldHoldMoreThanTheLimit() {
        assertEquals(listOf(399, 2), sizes("t398. t1."))
        assertEquals(listOf(400), sizes("t397. t1."))
    }

    @Test
    fun aWordWhosePhonemesAloneAreOverTheLimitIsCutAtTheLastSpaceThatKeepsARowWithinIt() {
        val phonemes = "a".repeat(199) + " " + "a".repeat(200)

        val rows = frontEnd(emptyList()) { phonemes }.rowsOf("long").map { it.size }

        assertEquals(listOf(200, 201), rows)
    }

    @Test
    fun aWordWithoutASpaceInItsFirst400TokensIsCutAtTheLimit() {
        val phonemes = "a".repeat(400)

        val rows = frontEnd(emptyList()) { phonemes }.rowsOf("long").map { it.size }

        assertEquals(listOf(400, 1), rows)
    }

    @Test
    fun noRowOfALongTextHoldsMoreThanTheLimitAndTheRowsHoldEveryWordInOrder() {
        val words = (1..300).map { "t${it % 9 + 3}" }
        val rows = frontEndOfSizedWords().rowsOf(words.joinToString(" "))

        assertTrue(rows.all { it.size <= MAXIMUM_TOKENS_PER_ROW })
        assertEquals(words.sumOf { it.drop(1).toInt() }, rows.sumOf { it.size })
        val phonemesInOrder = rows.flatMap { it.toList() }.map { it.toChar() }.joinToString("")
        assertEquals(words.joinToString("") { "a".repeat(it.drop(1).toInt() - 1) + " " }, phonemesInOrder)
    }

    @Test
    fun theTokensAreTheCodePointsOfTheLinesOfTheTokensFileAndTheSpaceIsOne() {
        val tokens = tokenCodePointsOf(listOf("\$ 0", "; 1", "  16", "a 43", "ˈ 156", "no 7"))

        assertEquals(setOf('$'.code, ';'.code, ' '.code, 'a'.code, 0x2C8), tokens)
    }

    @Test
    fun theLexiconTakesTheFirstOfTwoLinesWithTheSameWordAndLowercasesIt() {
        val lexicon = lexiconOf(listOf("Cat k a t", "cat x x x", "dog d o g"), tokenCodePoints)

        assertEquals(setOf("cat", "dog"), lexicon.keys)
        assertEquals("kat", lexicon.getValue("cat").let { String(it, 0, it.size) })
    }

    @Test
    fun theLexiconLeavesOutALineWithoutTokensAndOneWithATokenThatIsNotOne() {
        val lexicon = lexiconOf(listOf("lonely", "odd ˈ a", "tab\tt\ta", "two ab"), tokenCodePoints)

        assertEquals(setOf("tab"), lexicon.keys)
    }
}
