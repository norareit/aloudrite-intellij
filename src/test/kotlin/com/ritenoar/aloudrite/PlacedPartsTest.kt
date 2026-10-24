package com.ritenoar.aloudrite

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlacedPartsTest {

    // The source character that each character of the words was read from
    private fun sourceOf(markdown: String, words: PlacedWords): String =
        words.offsets.map { markdown[it] }.joinToString("")

    private fun wordsOf(markdown: String): List<PlacedWords> = placedPartsOf(markdown).parts.filterIsInstance<PlacedWords>()

    @Test
    fun theOffsetsAreThoseOfTheFileAlsoAfterFrontMatter() {
        val markdown = "---\ntitle: x\n---\nHello world."

        val words = wordsOf(markdown).single()

        assertEquals("Hello world.", words.text)
        assertEquals(markdown.indexOf("Hello"), words.offsets.first())
        assertEquals("Hello world.", sourceOf(markdown, words))
    }

    @Test
    fun anEscapedCharacterAndAnEntityTakeTheOffsetOfTheirSource() {
        val markdown = "a \\* b &amp; c"

        val words = wordsOf(markdown).single()

        assertEquals("a * b & c", words.text)
        assertEquals("a * b & c", sourceOf(markdown, words))
    }

    @Test
    fun boldItalicAndALinkWithTextTakeTheOffsetsOfTheirWords() {
        val markdown = "Say **bold** and *it* and [the link](http://x.example) now."

        val words = wordsOf(markdown).single()

        assertEquals("Say bold and it and the link now.", words.text)
        assertEquals("Say bold and it and the link now.", sourceOf(markdown, words))
    }

    @Test
    fun inlineCodeTakesTheOffsetsOfItsLiteral() {
        val markdown = "Call `run now` today."

        val words = wordsOf(markdown)

        assertEquals(listOf("Call ", "run now", " today."), words.map { it.text })
        assertEquals("run now", sourceOf(markdown, words[1]))
    }

    @Test
    fun anHtmlBlockKeepsTheOffsetsOfWhatIsLeftOfItsComment() {
        val markdown = "<div>a <!-- hidden --> b</div>"

        val words = wordsOf(markdown).single()

        assertEquals("<div>a  b</div>", words.text)
        assertEquals("<div>a  b</div>", sourceOf(markdown, words))
    }

    @Test
    fun theFullStopOfAnItemTakesTheOffsetOfItsLastCharacterAndTheNumberThatOfItsMarker() {
        val markdown = "1. First\n2. Second"

        val words = wordsOf(markdown)

        assertEquals(listOf("1. First.", "2. Second."), words.map { it.text })
        assertEquals(markdown.indexOf("t\n"), words[0].offsets.last())
        assertEquals(markdown.indexOf("t\n"), words[0].offsets[words[0].text.length - 2])
        assertEquals(List(3) { 0 }, words[0].offsets.take(3))
        assertEquals(List(3) { markdown.indexOf("2.") }, words[1].offsets.take(3))
    }

    @Test
    fun theNumberOfAnItemIsMarkedWithTheRangeOfItsItem() {
        val markdown = "1. First\n2. Second"

        val placed = placedPartsOf(markdown)
        val second = placed.parts.filterIsInstance<PlacedWords>()[1]

        val mark = second.numbers.single()
        assertEquals(0, mark.start)
        assertEquals(3, mark.end)
        assertEquals("2. ", mark.item.number)
        assertEquals(markdown.indexOf("2."), mark.item.range.start)
        assertEquals(markdown.length, mark.item.range.end)
        assertTrue(mark.item in placed.numberedItems)
    }

    @Test
    fun aBulletedItemHasNoNumber() {
        val placed = placedPartsOf("- One\n- Two")

        assertTrue(placed.numberedItems.isEmpty())
        assertTrue(placed.parts.filterIsInstance<PlacedWords>().all { it.numbers.isEmpty() })
    }

    @Test
    fun aLineBreakTakesTheOffsetOfTheBreak() {
        val markdown = "one\ntwo"

        val words = wordsOf(markdown).single()

        assertEquals("one\ntwo", words.text)
        assertEquals(markdown.indexOf('\n'), words.offsets[3])
    }

    @Test
    fun aCodeBlockAndATableStandForTheRangeOfTheirBlock() {
        val markdown = "Before.\n\n```\nx = 1\n```\n\n| a | b |\n|---|---|\n| 1 | 2 |\n"

        val words = wordsOf(markdown)

        assertEquals(listOf("Before.", "code example", "see table"), words.map { it.text })
        assertEquals(markdown.indexOf("```"), words[1].standsForBlock!!.start)
        assertTrue(markdown.indexOf("x = 1") in words[1].standsForBlock!!.start until words[1].standsForBlock!!.end)
        assertEquals(markdown.indexOf("| a"), words[2].standsForBlock!!.start)
        assertEquals(null, words[0].standsForBlock)
    }

    @Test
    fun theCodeExampleIsFollowedByItsSilence() {
        val parts = placedPartsOf("```\nx\n```\n\nAfter.").parts

        // The silences next to each other are made one only when the parts are made into spoken ones
        assertEquals(
            listOf("code example", 2_000, 500, "After."),
            parts.map { if (it is PlacedWords) it.text else (it as PlacedSilence).milliseconds })
    }

    @Test
    fun everyPartOfOneParagraphHasTheBlockOfItAndTwoParagraphsHaveTwo() {
        val oneParagraph = wordsOf("Call `run` now.")
        val twoParagraphs = wordsOf("One.\n\nTwo.")

        assertEquals(1, oneParagraph.map { it.block }.toSet().size)
        assertEquals(2, twoParagraphs.map { it.block }.toSet().size)
        assertNotEquals(twoParagraphs[0].block, twoParagraphs[1].block)
    }

    @Test
    fun theNumberOfAnItemBelongsToTheBlockOfItsFirstParagraph() {
        val words = wordsOf("1. First\n\n   More.\n2. Second")

        assertEquals(listOf("1. First.", "More.", "2. Second."), words.map { it.text })
        assertEquals(3, words.map { it.block }.toSet().size)
    }
}
