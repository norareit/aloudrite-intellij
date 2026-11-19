package com.ritenoar.aloudrite

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertEquals
import org.junit.Test

class ReadingSegmentsTest {

    private fun segmentsOf(markdown: String, place: PlaceInTheText = PlaceInTheText.NoText) =
        ReadingSegments(whatToRead(markdown, place).placedParts)

    // The characters of the file that each word covers, for the words of every segment
    private fun wordsIn(markdown: String, segments: ReadingSegments): List<String> =
        segments.segments.flatMap { segment -> segment.words.map { markdown.substring(it.place.start, it.place.end) } }

    private fun sentencesIn(markdown: String, segments: ReadingSegments): List<String> =
        segments.segments.flatMap { segment -> segment.words.map { markdown.substring(it.sentence.start, it.sentence.end) } }
            .distinct()

    private fun placeOf(markdown: String, text: String, from: Int = 0): SourceRange {
        val start = markdown.indexOf(text, from)
        check(start >= 0) { "\"$text\" is not in the Markdown" }
        return SourceRange(start, start + text.length)
    }

    @Test
    fun theSegmentsAreTheOnesTheServiceCutBefore() {
        val markdown = Files.readString(Paths.get("samples/markdown-features.md"))
        val parts = spokenPartsOf(markdown)

        val before = parts.flatMap { part ->
            when (part) {
                is Words -> piecesOf(part.text).filterNot { it.isBlank() }.map(::Words)
                is Silence -> listOf(part)
            }
        }

        assertEquals(before, segmentsOf(markdown).spoken)
    }

    @Test
    fun aWordOfBoldOrItalicTextCoversItsLettersAndNotTheMarksAroundIt() {
        val markdown = "Say **bold** and *it* now."

        assertEquals(listOf("Say", "bold", "and", "it", "now."), wordsIn(markdown, segmentsOf(markdown)))
    }

    @Test
    fun aWordOfALinkWithTextCoversItsLetters() {
        val markdown = "Go to [the link](http://x.example) now."

        assertEquals(listOf("Go", "to", "the", "link", "now."), wordsIn(markdown, segmentsOf(markdown)))
    }

    @Test
    fun aSelectionThatCutsAWordGivesTheWholeWord() {
        val markdown = "One wonderful day."
        val selection = PlaceInTheText.Selection(markdown.indexOf("nder"), markdown.indexOf("nder") + 2)

        assertEquals(listOf("wonderful"), wordsIn(markdown, segmentsOf(markdown, selection)))
    }

    @Test
    fun anEscapedCharacterAndAnEntityHaveAPlace() {
        val markdown = "a \\* b &amp; c"

        val words = segmentsOf(markdown).segments.single().words

        assertEquals(listOf("a", "*", "b", "&", "c"), words.map { markdown.substring(it.place.start, it.place.end) })
    }

    @Test
    fun anEmojiSplitsTwoWordsAndIsInNeitherOfThem() {
        val emoji = String(Character.toChars(0x1F600))
        val markdown = "one${emoji}two"

        assertEquals(listOf("one", "two"), wordsIn(markdown, segmentsOf(markdown)))
    }

    @Test
    fun codeExampleIsOneWordOverTheWholeCodeBlockAndItsSentenceIsThatBlock() {
        val markdown = "Before.\n\n```kotlin\nval x = 1\n```\n\nAfter."
        val block = placeOf(markdown, "```kotlin\nval x = 1\n```")

        val code = segmentsOf(markdown).segments.single { (it.spoken as? Words)?.text == "code example" }

        assertEquals(listOf(HeardWord(block, block, 11)), code.words)
    }

    @Test
    fun seeTableIsOneWordOverTheWholeTable() {
        val markdown = "Before.\n\n| a | b |\n|---|---|\n| 1 | 2 |\n\nAfter."
        val table = placeOf(markdown, "| a | b |\n|---|---|\n| 1 | 2 |")

        val words = segmentsOf(markdown).segments.single { (it.spoken as? Words)?.text == "see table" }.words

        assertEquals(listOf(table), words.map { it.place })
        assertEquals(listOf(table), words.map { it.sentence })
    }

    @Test
    fun linkIsOneWordOverTheAddressAndItsSentenceIsThatOfTheParagraphAroundIt() {
        val markdown = "See <http://x.example/page> for more."

        val segments = segmentsOf(markdown)

        assertEquals(listOf("See", "<http://x.example/page>", "for", "more."), wordsIn(markdown, segments))
        assertEquals(listOf(markdown), sentencesIn(markdown, segments))
    }

    @Test
    fun aSentenceOfTwoPiecesCutAtACommaIsOneSentenceCoveringBoth() {
        val clause = "a clause that is long enough to be cut off from the rest of its sentence at the comma"
        val markdown = "$clause, and then it goes on to the end."

        val segments = segmentsOf(markdown)

        assertEquals(2, segments.segments.size)
        assertEquals(listOf(markdown), sentencesIn(markdown, segments))
    }

    @Test
    fun twoSentencesOfOneParagraphAreTwo() {
        val markdown = "First one. Second one."

        assertEquals(listOf("First one.", "Second one."), sentencesIn(markdown, segmentsOf(markdown)))
    }

    @Test
    fun aSentenceWithInlineCodeInItIsOneSentenceAcrossTheSilences() {
        val markdown = "Call `run` now."

        val segments = segmentsOf(markdown)

        assertEquals(listOf("Call", "run", "now."), wordsIn(markdown, segments))
        assertEquals(listOf(markdown), sentencesIn(markdown, segments))
    }

    @Test
    fun theNumberOfAnItemBelongsToTheFirstSentenceOfTheItem() {
        val markdown = "1. First one. Second one.\n2. Next."

        val sentences = sentencesIn(markdown, segmentsOf(markdown))

        assertEquals(listOf("1. First one.", "Second one.", "2. Next."), sentences)
    }

    @Test
    fun theNumberOfAnItemBelongsToTheFirstSentenceAlsoInAReadingFromTheCaretAndASelection() {
        val markdown = "1. First one. Second one.\n2. Next."
        val fromTheCaret = PlaceInTheText.Caret(markdown.indexOf("First"))
        val selected = PlaceInTheText.Selection(markdown.indexOf("First"), markdown.indexOf("Second"))

        assertEquals(listOf("1. First one.", "Second one.", "2. Next."), sentencesIn(markdown, segmentsOf(markdown, fromTheCaret)))
        assertEquals(listOf("1. First one."), sentencesIn(markdown, segmentsOf(markdown, selected)))
    }

    @Test
    fun theFullStopAddedToAListItemBelongsToItsLastWord() {
        val markdown = "- one two"

        val words = segmentsOf(markdown).segments.single().words

        assertEquals(listOf("one", "two"), words.map { markdown.substring(it.place.start, it.place.end) })
        assertEquals(listOf(3, 3), words.map { it.weight })
    }

    @Test
    fun theWeightsLeaveOutWhiteSpaceAndPunctuationAndCountDigitsAndSymbols() {
        val markdown = "Hi, \"you\" - 42 ✓ (ok)!"

        val words = segmentsOf(markdown).segments.single().words

        assertEquals(listOf("Hi,", "\"you\"", "-", "42", "✓", "(ok)!"), words.map { markdown.substring(it.place.start, it.place.end) })
        assertEquals(listOf(2, 3, 0, 2, 1, 2), words.map { it.weight })
    }

    @Test
    fun aSilenceHasNoWords() {
        val markdown = "One.\n\nTwo."

        val segments = segmentsOf(markdown).segments

        assertEquals(listOf(1, 0, 1), segments.map { it.words.size })
    }
}
