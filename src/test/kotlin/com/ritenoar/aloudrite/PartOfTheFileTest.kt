package com.ritenoar.aloudrite

import org.junit.Assert.assertEquals
import org.junit.Test

// What is chosen is asserted as what is heard: the words, with a "|" for each silence
class PartOfTheFileTest {

    private fun heard(markdown: String, place: PlaceInTheText): String =
        whatToRead(markdown, place).parts.joinToString("") { if (it is Words) it.text else "|" }

    private fun selecting(markdown: String, from: String, to: String): PlaceInTheText.Selection {
        val start = markdown.indexOf(from)
        return PlaceInTheText.Selection(start, markdown.indexOf(to, start) + to.length)
    }

    private fun caretIn(markdown: String, marker: String, after: Int = 0) = PlaceInTheText.Caret(markdown.indexOf(marker) + after)

    private fun partOf(markdown: String, place: PlaceInTheText) = whatToRead(markdown, place).partOfTheFile

    // The selection

    @Test
    fun aSelectionWithinAParagraphReadsWhatItCovers() {
        val markdown = "# Title\n\nOne two three four. Five six."

        assertEquals("two three", heard(markdown, selecting(markdown, "two", "three")))
        assertEquals(PartOfTheFile.SELECTION, partOf(markdown, selecting(markdown, "two", "three")))
    }

    @Test
    fun aSelectionThatCutsWordsReadsThemWhole() {
        val markdown = "Alpha bravo charlie delta."

        assertEquals("bravo charlie", heard(markdown, selecting(markdown, "ravo", "harli")))
    }

    @Test
    fun aSelectionAcrossTwoParagraphsKeepsTheSilenceBetweenThem() {
        val markdown = "Before. One two.\n\nThree four. After."

        assertEquals("One two.|Three four.", heard(markdown, selecting(markdown, "One", "four.")))
    }

    @Test
    fun aSelectionThatTouchesACodeBlockInPartReadsItWhole() {
        val markdown = "Before.\n\n```\nx = 1\ny = 2\n```\n\nAfter."

        assertEquals("code example", heard(markdown, selecting(markdown, "y = 2", "y = 2")))
        assertEquals("code example|After.", heard(markdown, selecting(markdown, "y = 2", "After")))
    }

    @Test
    fun aSelectionThatTouchesATableInPartReadsSeeTable() {
        val markdown = "Before.\n\n| a | b |\n|---|---|\n| 1 | 2 |\n| 3 | 4 |\n\nAfter."

        assertEquals("see table", heard(markdown, selecting(markdown, "| 3", "4")))
    }

    @Test
    fun aSelectionThatBeginsInAnItemAfterItsNumberReadsTheNumberFirst() {
        val markdown = "1. First\n2. Second\n3. Third thing\n4. Fourth"

        assertEquals("3. Third thing.", heard(markdown, selecting(markdown, "Third", "thing")))
    }

    @Test
    fun aSelectionThatIncludesTheMarkerOfAnItemReadsItsNumberOnce() {
        val markdown = "1. First\n2. Second\n3. Third thing"

        assertEquals("2. Second.|3. Third thing.", heard(markdown, selecting(markdown, "2.", "thing")))
    }

    @Test
    fun aSelectionOfAnInnerItemReadsTheNumberOfTheInnermostItem() {
        val markdown = "1. Outer\n   1. Inner one\n   2. Inner two"

        assertEquals("2. Inner two.", heard(markdown, selecting(markdown, "Inner two", "two")))
    }

    @Test
    fun aSelectionOfFrontMatterOnlyReadsNothing() {
        val markdown = "---\ntitle: Hello\n---\nBody."

        assertEquals("", heard(markdown, selecting(markdown, "title", "Hello")))
        assertEquals(PartOfTheFile.SELECTION, partOf(markdown, selecting(markdown, "title", "Hello")))
    }

    @Test
    fun aSelectionOfWhiteSpaceOnlyReadsNothing() {
        val markdown = "One.   \n\n   Two."

        assertEquals("", heard(markdown, PlaceInTheText.Selection(5, 7)).trim())
        assertEquals(PartOfTheFile.SELECTION, partOf(markdown, PlaceInTheText.Selection(5, 7)))
    }

    @Test
    fun aReferenceLinkWhoseDefinitionIsOutsideThePartIsReadByItsText() {
        val markdown = "See [the guide][g] now.\n\nOther.\n\n[g]: http://example.com"

        assertEquals("the guide now.", heard(markdown, selecting(markdown, "the guide", "now.")))
    }

    @Test
    fun aCommentOpenedBeforeThePartHidesWhatItCovers() {
        val markdown = "One.\n\n<!-- hidden\n\n<p>still hidden</p> -->\n\nShown."

        assertEquals("Shown.", heard(markdown, selecting(markdown, "still", "Shown.")).trim())
    }

    @Test
    fun noPartIsHeardWithASilenceFirstAndCharactersAboveFfffAreSpaces() {
        val markdown = "One.\n\nTwo 😀 three."

        assertEquals("Two   three.", heard(markdown, selecting(markdown, "Two", "three")))
    }

    // The caret

    private val sentences = "# Title\n\nFirst one. Second two.\n\nNext."

    @Test
    fun aCaretInTheMiddleOfASentenceReadsFromItsBeginningToTheEnd() {
        assertEquals("Second two.|Next.", heard(sentences, caretIn(sentences, "cond", 2)))
        assertEquals(PartOfTheFile.FROM_THE_CARET, partOf(sentences, caretIn(sentences, "cond", 2)))
    }

    @Test
    fun aCaretAtTheFirstCharacterOfASentenceReadsFromIt() {
        assertEquals("Second two.|Next.", heard(sentences, caretIn(sentences, "Second")))
    }

    @Test
    fun aCaretBetweenTwoSentencesReadsFromTheSecond() {
        assertEquals("Second two.|Next.", heard(sentences, caretIn(sentences, " Second")))
    }

    @Test
    fun aCaretInTheMarkerOfAHeadingReadsFromTheHeading() {
        val markdown = "Intro.\n\n## Heading here\n\nBody."

        assertEquals("Heading here|Body.", heard(markdown, caretIn(markdown, "##", 1)))
        assertEquals(PartOfTheFile.FROM_THE_CARET, partOf(markdown, caretIn(markdown, "##", 1)))
    }

    @Test
    fun aCaretInTheAddressOfALinkReadsFromTheSentenceThatHoldsIt() {
        val markdown = "Intro.\n\nSee <http://example.com/page> for more. Then this."

        assertEquals("See link for more. Then this.", heard(markdown, caretIn(markdown, "example", 3)))
    }

    @Test
    fun aCaretInFrontMatterBeforeTheFirstSentenceReadsTheWholeFile() {
        val markdown = "---\ntitle: Hello\n---\nFirst. Second."

        assertEquals("First. Second.", heard(markdown, caretIn(markdown, "title", 2)))
        assertEquals(PartOfTheFile.WHOLE_FILE, partOf(markdown, caretIn(markdown, "title", 2)))
    }

    @Test
    fun aCaretInsideAFencedCodeBlockReadsFromItsWords() {
        val markdown = "Before.\n\n```\nx = 1\ny = 2\n```\n\nAfter."

        assertEquals("code example|After.", heard(markdown, caretIn(markdown, "y = 2")))
        assertEquals(PartOfTheFile.FROM_THE_CARET, partOf(markdown, caretIn(markdown, "y = 2")))
    }

    @Test
    fun aCaretInsideATableReadsFromSeeTable() {
        val markdown = "Before.\n\n| a | b |\n|---|---|\n| 1 | 2 |\n\nAfter."

        assertEquals("see table|After.", heard(markdown, caretIn(markdown, "| 1", 2)))
    }

    @Test
    fun aCaretAfterTheLastWordReadsTheWholeFile() {
        assertEquals(PartOfTheFile.WHOLE_FILE, partOf(sentences, PlaceInTheText.Caret(sentences.length)))
        assertEquals("Title|First one. Second two.|Next.", heard(sentences, PlaceInTheText.Caret(sentences.length)))
    }

    @Test
    fun aCaretAtTheStartOfTheFileReadsTheWholeFile() {
        assertEquals(PartOfTheFile.WHOLE_FILE, partOf(sentences, PlaceInTheText.Caret(0)))
        assertEquals("Title|First one. Second two.|Next.", heard(sentences, PlaceInTheText.Caret(0)))
    }

    @Test
    fun aCaretAfterACommaThatThePiecesCutAtStartsAtTheSentenceNotTheClause() {
        val markdown = "Intro.\n\nThis is a long first clause of the sentence, and this is the second clause that follows it. Then more."

        assertEquals(
            "This is a long first clause of the sentence, and this is the second clause that follows it. Then more.",
            heard(markdown, caretIn(markdown, "second clause", 2)))
    }

    @Test
    fun aCaretInTheSecondSentenceOfANumberedItemReadsItsNumberFirst() {
        val markdown = "1. One. Uno.\n2. Two. Dos.\n3. Three."

        assertEquals("2. Dos.|3. Three.", heard(markdown, caretIn(markdown, "Dos", 1)))
    }

    @Test
    fun aCaretInTheFirstSentenceOfTheFirstItemReadsTheWholeFile() {
        val markdown = "1. One thing. Uno.\n2. Two."

        assertEquals(PartOfTheFile.WHOLE_FILE, partOf(markdown, caretIn(markdown, "thing", 2)))
    }

    @Test
    fun aCaretInAReferenceLinkReadsItByItsText() {
        val markdown = "Intro.\n\nSee [the guide][g] now.\n\n[g]: http://example.com"

        assertEquals("See the guide now.", heard(markdown, caretIn(markdown, "guide", 1)))
    }

    @Test
    fun noPlaceReadsTheWholeFile() {
        assertEquals("Title|First one. Second two.|Next.", heard(sentences, PlaceInTheText.NoText))
        assertEquals(PartOfTheFile.WHOLE_FILE, partOf(sentences, PlaceInTheText.NoText))
    }
}
