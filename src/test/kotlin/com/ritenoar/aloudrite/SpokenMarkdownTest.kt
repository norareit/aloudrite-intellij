package com.ritenoar.aloudrite

import com.intellij.testFramework.LightVirtualFile
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Paths

// Each case presses the Markdown through the stand-in engine and asserts what was heard. A case stays below the 3,000
// samples that the audio ahead holds at the stand-in's sample rate, since its kept works run one after the other
class SpokenMarkdownTest {

    @Test
    fun headingsOfEveryLevelGiveTheirTextOnly() {
        val markdown = "# One\n## Two\n### Three\n#### Four\n##### Five\n###### Six\n\n" +
            "Setext one\n==========\n\nSetext two\n----------\n"

        assertHeard(
            listOf("One", PARAGRAPH, "Two", PARAGRAPH, "Three", PARAGRAPH, "Four", PARAGRAPH, "Five", PARAGRAPH, "Six",
                PARAGRAPH, "Setext one", PARAGRAPH, "Setext two"),
            markdown)
    }

    @Test
    fun boldAndItalicGiveTheWordsOnly() {
        assertHeard(listOf("bold italic both bold italic"), "**bold** *italic* ***both*** __bold__ _italic_")
    }

    @Test
    fun struckOutTextGivesNothing() {
        assertHeard(listOf("the  new value"), "the ~~old~~ new value")
    }

    @Test
    fun aParagraphOfNothingButStruckOutTextGivesNothingAndLeavesOnePause() {
        assertHeard(listOf("Before.", PARAGRAPH, "After."), "Before.\n\n~~All struck out.~~\n\nAfter.")
    }

    @Test
    fun aSingleTildeIsLeftAsItIs() {
        assertHeard(listOf("a ~b~ c"), "a ~b~ c")
    }

    @Test
    fun theItemsOfABulletedListGiveNoMarkerAndEndAsASentence() {
        for (marker in listOf("-", "*", "+")) {
            assertHeard(
                listOf("One.", LIST_ITEM, "Two!", LIST_ITEM, "Three?", LIST_ITEM, "Four."),
                "$marker One\n$marker Two!\n$marker Three?\n$marker Four.")
        }
    }

    @Test
    fun theItemsOfANestedListEndAsASentenceOnce() {
        assertHeard(
            listOf("Parent.", LIST_ITEM, "Child one.", LIST_ITEM, "Child two!", LIST_ITEM, "Next?"),
            "- Parent\n  - Child one\n  - Child two!\n- Next?")
    }

    @Test
    fun anItemThatEndsBeforeAClosingMarkIsNotEndedAgain() {
        assertHeard(
            listOf("He said \"yes.\"", LIST_ITEM, "(see this).", LIST_ITEM, "(Done.)", LIST_ITEM, "*Done.*"),
            "- He said \"yes.\"\n- (see this)\n- (Done.)\n- \\*Done.\\*")
    }

    @Test
    fun aNumberedListKeepsItsNumbersAndEndsItsItemsAsASentence() {
        assertHeard(listOf("1. First.", LIST_ITEM, "2. Second."), "1. First\n2. Second.")
        assertHeard(listOf("3) Third.", LIST_ITEM, "4) Fourth!"), "3) Third\n4) Fourth!")
    }

    @Test
    fun aListHasThePauseBetweenBlocksBeforeAndAfterIt() {
        assertHeard(
            listOf("Intro.", PARAGRAPH, "One.", LIST_ITEM, "Two.", PARAGRAPH, "After."), "Intro.\n\n- One\n- Two\n\nAfter.")
    }

    @Test
    fun theParagraphsOfAnItemHaveThePauseBetweenItemsBetweenThem() {
        assertHeard(listOf("First.", LIST_ITEM, "Second.", LIST_ITEM, "Next."), "- First\n\n  Second\n- Next")
    }

    @Test
    fun theItemsOfATaskListGiveNeitherBoxNorState() {
        assertHeard(
            listOf("Open.", LIST_ITEM, "Done.", LIST_ITEM, "Also done."), "- [ ] Open\n- [x] Done\n- [X] Also done.")
    }

    @Test
    fun aLinkGivesItsTextOnly() {
        assertHeard(
            listOf("Read the guide and this."),
            "Read [the guide](https://example.com/guide) and [this](https://example.com \"Title\").")
    }

    @Test
    fun aReferenceLinkGivesItsTextAndItsDefinitionNothing() {
        assertHeard(
            listOf("See text, label and label."),
            "See [text][label], [label][] and [label].\n\n[label]: https://example.com/page")
        assertHeard(
            listOf("Before label.", PARAGRAPH, "After."),
            "Before [label].\n\n[label]: https://example.com/page\n\nAfter.")
    }

    @Test
    fun anAddressGivesTheWordLink() {
        assertHeard(listOf("Go to link now."), "Go to https://example.com/page now.")
        assertHeard(listOf("Go to link now."), "Go to www.example.com now.")
        assertHeard(listOf("Go to link now."), "Go to <https://example.com/page> now.")
        assertHeard(listOf("Write to link now."), "Write to <someone@example.com> now.")
        assertHeard(listOf("Write to link now."), "Write to someone@example.com now.")
    }

    @Test
    fun aLinkWhoseTextIsAnAddressGivesThatAddress() {
        assertHeard(listOf("https://example.com"), "[https://example.com](https://example.com)")
    }

    @Test
    fun anImageGivesItsAlternativeText() {
        assertHeard(listOf("A red apple"), "![A red apple](apple.png)")
    }

    @Test
    fun anImageWithoutAlternativeTextGivesNothingAndLeavesNoPause() {
        assertHeard(listOf("Before.", PARAGRAPH, "After."), "Before.\n\n![](apple.png)\n\nAfter.")
    }

    @Test
    fun inlineCodeHasAPauseBeforeAndAfterIt() {
        assertHeard(listOf("Call ", INLINE_CODE, "x", INLINE_CODE, " now."), "Call `x` now.")
    }

    @Test
    fun inlineCodeAtTheStartOfAParagraphGivesOnlyThePauseBetweenBlocks() {
        assertHeard(listOf("Before.", PARAGRAPH, "x", INLINE_CODE, " starts."), "Before.\n\n`x` starts.")
    }

    @Test
    fun aCodeBlockGivesCodeExampleAndTwoSeconds() {
        val codeBlocks = listOf("```\ncode\n```", "~~~\ncode\n~~~", "```kotlin\nval x = 1\n```", "    code")

        for (codeBlock in codeBlocks) {
            assertHeard(listOf("Before.", PARAGRAPH, "code example", CODE_BLOCK, "After."), "Before.\n\n$codeBlock\n\nAfter.")
        }
    }

    @Test
    fun aCodeBlockAsTheLastBlockGivesNoSilenceAfterIt() {
        assertHeard(listOf("Before.", PARAGRAPH, "code example"), "Before.\n\n```\ncode\n```")
    }

    @Test
    fun aCodeBlockInAListItemGivesCodeExampleAndTwoSeconds() {
        assertHeard(
            listOf("Item.", LIST_ITEM, "code example.", CODE_BLOCK, "Next."),
            "- Item\n\n  ```\n  code\n  ```\n- Next")
    }

    @Test
    fun aTableGivesSeeTable() {
        assertHeard(
            listOf("Before.", PARAGRAPH, "see table", PARAGRAPH, "After."),
            "Before.\n\n| A | B |\n|---|---|\n| 1 | 2 |\n\nAfter.")
    }

    @Test
    fun aHorizontalRuleGivesOnePause() {
        for (rule in listOf("---", "***", "___")) {
            assertHeard(listOf("Before.", PARAGRAPH, "After."), "Before.\n\n$rule\n\nAfter.")
        }
    }

    @Test
    fun anEscapedCharacterGivesTheCharacter() {
        assertHeard(listOf("* and # and \\ and &"), "\\* and \\# and \\\\ and &amp;")
    }

    @Test
    fun frontMatterIsNotHeardAndLeavesNoSilence() {
        assertHeard(listOf("Text."), "---\ntitle: Notes\ntags: [a, b]\n---\nText.")
        assertHeard(listOf("Text."), "---\ntitle: Notes\n...\n\nText.")
    }

    @Test
    fun aFileOfNothingButFrontMatterHearsNothing() {
        assertHeard(emptyList(), "---\ntitle: Notes\n---\n")
    }

    @Test
    fun threeDashesOnTheFirstLineWithoutAnEndAreHeardAsCommonMarkParsesThem() {
        assertHeard(listOf("Text.", PARAGRAPH, "More."), "---\nText.\n\nMore.")
        assertHeard(listOf("Title", PARAGRAPH, "Text."), "---\nTitle\n===\n\nText.")
    }

    @Test
    fun aCommentOnLinesOfItsOwnLeavesOnlyThePauseBetweenTheParagraphs() {
        assertHeard(listOf("Before.", PARAGRAPH, "After."), "Before.\n\n<!-- a note\nfor the editor -->\n\nAfter.")
    }

    @Test
    fun aCommentWithinASentenceGivesNothing() {
        assertHeard(listOf("Before  after."), "Before <!-- a note --> after.")
    }

    @Test
    fun aCommentThatIsNotClosedAtTheStartOfALineLeavesOutTheRestOfTheFile() {
        assertHeard(listOf("Before."), "Before.\n\n<!-- not closed\n\nAfter.")
    }

    @Test
    fun aCommentThatIsNotClosedWithinASentenceIsText() {
        assertHeard(listOf("Before <!-- not closed after."), "Before <!-- not closed after.")
    }

    @Test
    fun aCommentInCodeIsCode() {
        assertHeard(listOf("Use ", INLINE_CODE, "<!-- x -->", INLINE_CODE, " here."), "Use `<!-- x -->` here.")
        assertHeard(listOf("code example"), "```\n<!-- x -->\n```")
    }

    @Test
    fun theHtmlAroundACommentInAnHtmlBlockIsLeftAsItIs() {
        assertHeard(listOf("<div>Kept</div>\n"), "<div>Kept</div>\n<!-- gone -->")
    }

    @Test
    fun aCommentThatAnHtmlBlockLeavesOpenLeavesOutWhatFollowsIt() {
        assertHeard(listOf("<div>\n"), "<div>\n<!-- not closed\n\nStill in the comment.")
        assertHeard(
            listOf("<div>\n"),
            "<div>\n<!-- not closed\n\n1. One\n2. Two\n\n---\n\n```\ncode\n```\n\n| A |\n|---|\n| 1 |")
    }

    @Test
    fun aCommentThatAnHtmlBlockLeavesOpenEndsAtTheCloseOfALaterOne() {
        assertHeard(
            listOf("<div>\n", PARAGRAPH, " shown.", PARAGRAPH, "After."),
            "<div>\n<!-- not closed\n\nHidden <!-- closed --> shown.\n\nAfter.")
        assertHeard(
            listOf("<div>\n", PARAGRAPH, "<p>Shown</p>", PARAGRAPH, "After."),
            "<div>\n<!-- not closed\n\nHidden.\n\n<div>\n--><p>Shown</p>\n\nAfter.")
    }

    @Test
    fun aFileOfNothingButCommentsHearsNothing() {
        assertHeard(emptyList(), "<!-- one -->\n\n<!-- two -->")
    }

    @Test
    fun twoBlocksHaveHalfASecondBetweenThem() {
        assertHeard(listOf("One.", PARAGRAPH, "Two."), "One.\n\nTwo.")
        assertHeard(listOf("Title", PARAGRAPH, "Text."), "# Title\n\nText.")
        assertHeard(listOf("Text.", PARAGRAPH, "Item."), "Text.\n\n- Item")
        assertHeard(listOf("Item.", PARAGRAPH, "Quote."), "- Item\n\n> Quote.")
    }

    @Test
    fun lineBreaksWithinAParagraphStay() {
        assertHeard(listOf("One\ntwo\nthree."), "One\ntwo  \nthree.")
    }

    @Test
    fun aQuoteGivesItsTextWithThePauseBetweenItsParagraphs() {
        assertHeard(listOf("Quoted text."), "> Quoted text.")
        assertHeard(listOf("One.", PARAGRAPH, "Two."), "> One.\n>\n> Two.")
    }

    @Test
    fun emojiAtTheStartOfALineAreSpacesAndMakeNoCodeBlock() {
        val emoji = GRINNING_FACE.repeat(4)

        assertHeard(listOf("     not code."), "$emoji not code.")
        assertHeard(listOf("Before   after."), "Before $GRINNING_FACE after.")
    }

    private fun assertHeard(expected: List<String>, markdown: String) {
        val standIns = SpeechStandIns()
        val target = ReadingTarget(null, LightVirtualFile("notes.md", markdown))

        standIns.service.read(target, spokenPartsOf(markdown), Paths.get("/model"), System.nanoTime())
        standIns.runKeptWorks()

        assertEquals(markdown, expected, standIns.heard())
    }

    private companion object {
        const val PARAGRAPH = "<silence 500 ms>"
        const val INLINE_CODE = "<silence 250 ms>"
        const val LIST_ITEM = "<silence 250 ms>"
        const val CODE_BLOCK = "<silence 2000 ms>"
        val GRINNING_FACE = String(Character.toChars(0x1F600))
    }
}
