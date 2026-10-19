package com.ritenoar.aloudrite

import org.junit.Assert.assertEquals
import org.junit.Test

class FrontMatterTest {

    @Test
    fun frontMatterEndedByThreeDashesIsTakenOffWithBothItsLines() {
        assertEquals("# Title\n\nText.\n", withoutFrontMatter("---\ntitle: Notes\ntags: [a, b]\n---\n# Title\n\nText.\n"))
    }

    @Test
    fun frontMatterEndedByThreeDotsIsTakenOffWithBothItsLines() {
        assertEquals("\nText.", withoutFrontMatter("---\ntitle: Notes\n...\n\nText."))
    }

    @Test
    fun windowsLineEndsAreHandledAlike() {
        assertEquals("Text.\r\n", withoutFrontMatter("---\r\ntitle: Notes\r\n---\r\nText.\r\n"))
    }

    @Test
    fun threeDashesOnTheFirstLineWithoutAnEndLeaveTheTextAsItIs() {
        val text = "---\ntitle: Notes\n\nText."

        assertEquals(text, withoutFrontMatter(text))
    }

    @Test
    fun threeDashesThatAreNotOnTheFirstLineLeaveTheTextAsItIs() {
        val text = "\n---\ntitle: Notes\n---\nText."

        assertEquals(text, withoutFrontMatter(text))
        assertEquals("Intro\n---\nText.", withoutFrontMatter("Intro\n---\nText."))
    }

    @Test
    fun aFileOfNothingButFrontMatterGivesTheEmptyText() {
        assertEquals("", withoutFrontMatter("---\ntitle: Notes\n---"))
        assertEquals("", withoutFrontMatter("---\ntitle: Notes\n---\n"))
        assertEquals("", withoutFrontMatter("---\n---\n"))
    }

    @Test
    fun linesWithTrailingSpacesCount() {
        assertEquals("Text.", withoutFrontMatter("---  \ntitle: Notes\n--- \t\nText."))
    }

    @Test
    fun theFrontMatterEndsAtTheFirstClosingLineAndALaterBlockStays() {
        assertEquals("Text.\n---\nmore: yes\n---\nEnd.", withoutFrontMatter("---\na: 1\n---\nText.\n---\nmore: yes\n---\nEnd."))
    }

    @Test
    fun aLineThatOnlyBeginsWithThreeDashesIsNoClosingLine() {
        val text = "---\ntitle: Notes\n----\nText."

        assertEquals(text, withoutFrontMatter(text))
    }
}
