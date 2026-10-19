package com.ritenoar.aloudrite

import org.junit.Assert.assertEquals
import org.junit.Test

class EngineTextTest {

    @Test
    fun anEmojiAloneGivesOneSpace() {
        assertEquals(" ", withCharactersAboveFfffAsSpaces(GRINNING_FACE))
    }

    @Test
    fun threeEmojiInARowGiveThreeSpaces() {
        assertEquals("   ", withCharactersAboveFfffAsSpaces(GRINNING_FACE.repeat(3)))
    }

    @Test
    fun anEmojiBetweenTwoLettersKeepsThemApart() {
        assertEquals("a b", withCharactersAboveFfffAsSpaces("a${GRINNING_FACE}b"))
    }

    @Test
    fun everyCharacterAboveFfffGivesOneSpace() {
        for (codePoint in listOf(0x20000, 0x10330, 0x1D400)) {
            assertEquals(" ", withCharactersAboveFfffAsSpaces(String(Character.toChars(codePoint))))
        }
    }

    @Test
    fun charactersOfTwoAndThreeBytesStay() {
        val text = "café € → ❤ ✓"

        assertEquals(text, withCharactersAboveFfffAsSpaces(text))
    }

    @Test
    fun aTextWithoutSuchCharactersIsReturnedEqual() {
        val text = "# Title\n\nSome *Markdown* text."

        assertEquals(text, withCharactersAboveFfffAsSpaces(text))
    }

    @Test
    fun aHalfOfASurrogatePairAloneStays() {
        assertEquals("a\uD83Db", withCharactersAboveFfffAsSpaces("a\uD83Db"))
        assertEquals("a\uDE00b", withCharactersAboveFfffAsSpaces("a\uDE00b"))
    }

    private companion object {
        val GRINNING_FACE = String(Character.toChars(0x1F600))
    }
}
