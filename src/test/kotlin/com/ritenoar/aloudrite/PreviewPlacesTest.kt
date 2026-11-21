package com.ritenoar.aloudrite

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PreviewPlacesTest {

    private fun placesOf(markdown: String) = PreviewPlaces(describedPage(markdown), markdown)

    // The range of the first appearance of the characters in the text, as unit 0025 gives the range of a word
    private fun rangeOf(markdown: String, characters: String): SourceRange {
        val start = markdown.indexOf(characters)
        check(start >= 0) { "No $characters in $markdown" }
        return SourceRange(start, start + characters.length)
    }

    private fun shown(places: PreviewPlaces, text: PreviewText, pieces: List<TextPiece>): List<String> =
        pieces.map { text.nodes[it.node].text.substring(it.start, it.end) }

    private fun shownOf(markdown: String, characters: String): List<String> {
        val text = describedPage(markdown)
        val places = PreviewPlaces(text, markdown)
        return shown(places, text, places.pieces(rangeOf(markdown, characters)))
    }

    @Test
    fun `a word of plain text gives one piece over its letters`() {
        assertEquals(listOf("brave"), shownOf("Hello brave world", "brave"))
    }

    @Test
    fun `a word in bold, in italic, in inline code or in the text of a link gives its letters without the marks`() {
        assertEquals(listOf("two"), shownOf("One **two** three", "two"))
        assertEquals(listOf("two"), shownOf("One *two* three", "two"))
        assertEquals(listOf("two"), shownOf("One `two` three", "two"))
        assertEquals(listOf("two"), shownOf("One [two](http://a.b) three", "two"))
    }

    @Test
    fun `a range that takes in the marks around a word still gives only its letters`() {
        assertEquals(listOf("two"), shownOf("One **two** three", "**two**"))
    }

    @Test
    fun `a word with formatting inside it gives a piece in each of the two text nodes`() {
        assertEquals(listOf("foo", "bar"), shownOf("A foo**bar** baz", "foo**bar"))
    }

    @Test
    fun `an escaped character is in the word it is read in`() {
        assertEquals(listOf("*"), shownOf("a \\* b", "\\*"))
    }

    @Test
    fun `an entity is in the word it is read in`() {
        assertEquals(listOf("©"), shownOf("x &copy; y", "&copy;"))
    }

    @Test
    fun `a whitespace entity does not misalign the words that follow it`() {
        for (entity in listOf("&#32;", "&#x20;")) {
            val markdown = "Hello${entity}world again."

            assertEquals(entity, listOf("world"), shownOf(markdown, "world"))
            assertEquals(entity, listOf("again."), shownOf(markdown, "again."))
        }
    }

    @Test
    fun `a named or numeric entity that is no white space keeps the positions of the words after it`() {
        for (entity in listOf("&amp;", "&nbsp;", "&#169;", "&copy;")) {
            val markdown = "Fish${entity} chips and peas."

            assertEquals(entity, listOf("chips"), shownOf(markdown, "chips"))
            assertEquals(entity, listOf("peas."), shownOf(markdown, "peas."))
        }
    }

    @Test
    fun `the letters of an entity are not taken for the characters that follow it`() {
        val markdown = "x&amp;a b"
        val text = describedPage(markdown)
        val after = markdown.indexOf(";a") + 1

        assertEquals(listOf("a"), shown(PreviewPlaces(text, markdown), text, PreviewPlaces(text, markdown).pieces(SourceRange(after, after + 1))))
    }

    @Test
    fun `an entity spelled in inline code is literal text and keeps the positions of all its characters`() {
        val markdown = "Use `&amp; word` now."

        assertEquals(listOf("&amp;"), shownOf(markdown, "&amp;"))
        assertEquals(listOf("amp"), shownOf(markdown, "amp"))
        assertEquals(listOf("word"), shownOf(markdown, "word"))
        assertEquals(listOf("&#32;x"), shownOf("Use `&#32;x` now.", "&#32;x"))
    }

    @Test
    fun `a sentence over bold text gives a piece in each text node`() {
        assertEquals(listOf("One ", "two", " three."), shownOf("One **two** three.", "One **two** three."))
    }

    @Test
    fun `a sentence over two paragraphs gives a piece in each`() {
        val markdown = "First one.\n\nSecond one."

        assertEquals(listOf("First one.", "Second one."), shownOf(markdown, markdown))
    }

    @Test
    fun `a range that no text node overlaps gives no pieces`() {
        val markdown = "Hello world"
        val places = placesOf(markdown)

        assertEquals(emptyList<TextPiece>(), places.pieces(SourceRange(100, 105)))
    }

    @Test
    fun `a range of white space only gives no pieces`() {
        val markdown = "Hello   world"

        assertEquals(emptyList<String>(), shownOf(markdown, "   "))
    }

    @Test
    fun `the range of a fenced code block gives the text of the block`() {
        val markdown = "Before.\n\n```kotlin\nval a = 1\n```\n\nAfter."

        assertEquals(listOf("val a = 1\n"), shownOf(markdown, "```kotlin\nval a = 1\n```"))
    }

    @Test
    fun `the range of a table gives the text of its cells`() {
        val markdown = "Before.\n\n| a | b |\n|---|---|\n| 1 | 2 |\n\nAfter."

        assertEquals(listOf("a", "b", "1", "2"), shownOf(markdown, "| a | b |\n|---|---|\n| 1 | 2 |"))
    }

    @Test
    fun `the address of a link gives no pieces and its word is all of the text of the link`() {
        val markdown = "See [the docs](http://a.b/c) now."
        val text = describedPage(markdown)
        val places = PreviewPlaces(text, markdown)
        val address = rangeOf(markdown, "(http://a.b/c)")

        assertEquals(emptyList<TextPiece>(), places.pieces(address))
        val word = places.wordPlace(address)
        assertEquals(listOf("the docs"), shown(places, text, word.pieces))
        assertNull(word.outlinedElement)
    }

    @Test
    fun `an image gives no pieces and its word names the element of the image by its range`() {
        val markdown = "Before.\n\n![alt](boat.png)\n\nAfter."
        val places = placesOf(markdown)
        val image = rangeOf(markdown, "![alt](boat.png)")

        assertEquals(emptyList<TextPiece>(), places.pieces(image))
        val word = places.wordPlace(image)
        assertEquals(emptyList<TextPiece>(), word.pieces)
        assertEquals(image, word.outlinedElement)
    }

    @Test
    fun `a word in the text is its pieces and names no element`() {
        val markdown = "Hello brave world"
        val text = describedPage(markdown)
        val places = PreviewPlaces(text, markdown)

        val word = places.wordPlace(rangeOf(markdown, "brave"))

        assertEquals(listOf("brave"), shown(places, text, word.pieces))
        assertNull(word.outlinedElement)
    }

    @Test
    fun `a word in no element is not placed`() {
        val places = placesOf("Hello world")

        val word = places.wordPlace(SourceRange(100, 105))

        assertEquals(emptyList<TextPiece>(), word.pieces)
        assertNull(word.outlinedElement)
    }

    @Test
    fun `the element of a word is the smallest that holds it, and of equal ones the last`() {
        val text = PreviewText.read(
            """{"version": 1, "nodes": [["o",0,10],["t","a"],["o",0,10],["t","b"],["c"],["o",4,8],["c"],["c"]]}""",
        )!!
        val places = PreviewPlaces(text, "0123456789")

        // Within 0..10 twice and within 4..8 once, which is the smallest and holds no text
        assertEquals(SourceRange(4, 8), places.wordPlace(SourceRange(5, 6)).outlinedElement)
        // Within the two equal ones only, the second of which holds the text "b"
        val word = places.wordPlace(SourceRange(1, 2))
        assertEquals(listOf(TextPiece(1, 0, 1)), word.pieces)
        assertNull(word.outlinedElement)
    }

    @Test
    fun `the offsets of a text node are worked out once for a description`() {
        val markdown = "One **two** three."
        val places = placesOf(markdown)

        places.pieces(rangeOf(markdown, "One"))
        places.pieces(rangeOf(markdown, "One **two**"))
        places.pieces(rangeOf(markdown, "three"))

        assertEquals(3, places.alignmentsMade)
    }

    @Test
    fun `the places that the reading gives to the words around an emoji give the pieces that show them`() {
        val markdown = "Before 😀 after."
        val text = describedPage(markdown)
        val places = PreviewPlaces(text, markdown)

        val words = ReadingSegments(placedPartsOf(markdown).parts).segments.single().words

        assertEquals(listOf("Before", "😀", "after."), words.map { word -> shown(places, text, places.pieces(word.place)).joinToString("") })
    }

    @Test
    fun `the pieces are in the units of the text of the page`() {
        val markdown = "A 😀 big word"
        val text = describedPage(markdown)
        val places = PreviewPlaces(text, markdown)

        val piece = places.pieces(rangeOf(markdown, "big")).single()

        assertTrue(text.nodes[piece.node].text.substring(piece.start, piece.end) == "big")
        assertEquals(5, piece.start)
    }
}
