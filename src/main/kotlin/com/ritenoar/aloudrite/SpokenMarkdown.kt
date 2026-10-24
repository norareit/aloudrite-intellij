package com.ritenoar.aloudrite

import org.commonmark.ext.autolink.AutolinkExtension
import org.commonmark.ext.gfm.strikethrough.Strikethrough
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.gfm.tables.TableBlock
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.ext.task.list.items.TaskListItemMarker
import org.commonmark.ext.task.list.items.TaskListItemsExtension
import org.commonmark.node.BulletList
import org.commonmark.node.Code
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Heading
import org.commonmark.node.HtmlBlock
import org.commonmark.node.HtmlInline
import org.commonmark.node.IndentedCodeBlock
import org.commonmark.node.Link
import org.commonmark.node.LinkReferenceDefinition
import org.commonmark.node.ListBlock
import org.commonmark.node.Node
import org.commonmark.node.OrderedList
import org.commonmark.node.Paragraph
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.Text
import org.commonmark.node.ThematicBreak
import org.commonmark.parser.IncludeSourceSpans
import org.commonmark.parser.Parser

// The lengths are guesses until they are heard, see the decision on the lengths of the pauses in the spec of unit 0010
private const val PAUSE_BETWEEN_BLOCKS_MILLISECONDS = 500
private const val PAUSE_AROUND_INLINE_CODE_MILLISECONDS = 250
private const val PAUSE_BETWEEN_LIST_ITEMS_MILLISECONDS = 250
private const val PAUSE_AFTER_A_CODE_BLOCK_MILLISECONDS = 2_000

private const val CODE_BLOCK_WORDS = "code example"
private const val TABLE_WORDS = "see table"
private const val ADDRESS_WORDS = "link"

private const val COMMENT_END = "-->"
private const val LIST_ITEM_END = "."

// A comment runs to the next -->, or to the end of the HTML where there is none, and is then left open. <!--> and
// <!---> are whole comments in CommonMark
private val COMMENT = Regex("""<!--(?:-?>|.*?-->|(.*\z))""", RegexOption.DOT_MATCHES_ALL)

// The source spans of the inlines tell a link with text from an address that the parser recognised on its own
private val PARSER: Parser = Parser.builder()
    .extensions(listOf(
        StrikethroughExtension.builder().requireTwoTildes(true).build(),
        TablesExtension.create(),
        TaskListItemsExtension.create(),
        AutolinkExtension.create(),
    ))
    .includeSourceSpans(IncludeSourceSpans.BLOCKS_AND_INLINES)
    .build()

// What a listener should hear of the Markdown: its text without the markers, words in place of what cannot be read
// aloud well, and silences where its structure asks for a pause
fun spokenPartsOf(markdown: String): List<SpokenPart> = spokenPartsFrom(placedPartsOf(markdown).parts)

// The parts that are read, with the place in the file of each character. The text is parsed as a whole, so that what a
// part of it means is what it means in the file
fun placedPartsOf(markdown: String): PlacedMarkdown {
    val source = withoutFrontMatter(markdown)
    val walk = SpokenMarkdown(source, markdown.length - source.length)
    return PlacedMarkdown(walk.partsOfBlocks(PARSER.parse(source)), walk.numberedItems)
}

// Only after parsing, so that a space in place of an emoji cannot change the structure, as four of them at the start of
// a line would by making an indented code block
fun spokenPartsFrom(placed: List<PlacedPart>): List<SpokenPart> = tidied(placed.map {
    when (it) {
        is PlacedWords -> Words(withCharactersAboveFfffAsSpaces(it.text))
        is PlacedSilence -> Silence(it.milliseconds)
    }
})

// The source is the Markdown without its front matter, which is shifted by the length of the front matter that was
// taken off, so that the offsets are those of the whole file
class SpokenMarkdown(private val source: String, private val shift: Int) {

    val numberedItems = mutableListOf<NumberedItem>()

    // Whether a comment is open. An HTML block such as <div> ends at a blank line, and a comment it leaves open hides
    // what follows in a rendered file, up to the --> of a later piece of HTML
    private var inComment = false

    // The number that the next block that gives words is given, and the block that is walked
    private var blocksNumbered = 0
    private var currentBlock = 0
    private var currentBlockStart = shift

    // The blocks of a document or a quote
    fun partsOfBlocks(container: Node): List<PlacedPart> =
        joined(childrenOf(container).map(::partsOfBlock), PAUSE_BETWEEN_BLOCKS_MILLISECONDS)

    private fun partsOfBlock(block: Node): List<PlacedPart> = when (block) {
        is Paragraph, is Heading -> {
            startBlock(block)
            partsOfInlines(block)
        }
        is FencedCodeBlock, is IndentedCodeBlock -> unlessInAComment(listOf(
            wordsOfAWholeBlock(CODE_BLOCK_WORDS, block), PlacedSilence(PAUSE_AFTER_A_CODE_BLOCK_MILLISECONDS)))
        is TableBlock -> unlessInAComment(listOf(wordsOfAWholeBlock(TABLE_WORDS, block)))
        is ThematicBreak -> unlessInAComment(listOf(PlacedSilence(PAUSE_BETWEEN_BLOCKS_MILLISECONDS)))
        is HtmlBlock -> {
            startBlock(block)
            listOf(wordsOf(withoutComments(block.literal), block))
        }
        is LinkReferenceDefinition -> emptyList()
        is ListBlock -> partsOfList(block)
        // A quote, and any other block that holds blocks
        else -> partsOfBlocks(block)
    }

    // The items of a list, and the blocks of an item, follow each other with the end of a sentence and the shorter
    // pause between list items between them
    private fun partsOfList(list: ListBlock): List<PlacedPart> = joined(
        childrenOf(list).mapIndexed { place, item ->
            endedAsASentence(partsOfItem(item, if (inComment) "" else numberOf(list, place)))
        },
        PAUSE_BETWEEN_LIST_ITEMS_MILLISECONDS,
    )

    private fun partsOfItem(item: Node, number: String): List<PlacedPart> {
        // The number is of the block that the first block of the item is given
        val numberBlock = blocksNumbered
        val blocks = childrenOf(item).filterNot { it is TaskListItemMarker }.map { block ->
            val blockParts = partsOfBlock(block)
            if (block is Paragraph) endedAsASentence(blockParts) else blockParts
        }
        val parts = PartsInARow()
        if (number.isNotEmpty()) parts.add(listOf(numberWords(number, item, numberBlock)))
        parts.add(joined(blocks, PAUSE_BETWEEN_LIST_ITEMS_MILLISECONDS))
        return parts.parts
    }

    // The number is read, as it was in unit 0006. It is counted from the first, as a rendered list shows it
    private fun numberOf(list: ListBlock, place: Int): String {
        if (list !is OrderedList) return ""
        return "${(list.markerStartNumber ?: 1) + place}${list.markerDelimiter ?: "."} "
    }

    private fun numberWords(number: String, item: Node, block: Int): PlacedWords {
        val range = rangeOf(item) ?: SourceRange(currentBlockStart, currentBlockStart)
        val numbered = NumberedItem(range, number)
        numberedItems += numbered
        return PlacedWords(
            number, List(number.length) { range.start }, block, numbers = listOf(NumberMark(0, number.length, numbered)))
    }

    private fun startBlock(block: Node) {
        currentBlock = blocksNumbered++
        currentBlockStart = rangeOf(block)?.start ?: currentBlockStart
    }

    private fun partsOfInlines(parent: Node): List<PlacedPart> = PartsInARow().also { addInlines(parent, it) }.parts

    private fun addInlines(parent: Node, parts: PartsInARow) {
        childrenOf(parent).forEach { addInline(it, parts) }
    }

    private fun addInline(inline: Node, parts: PartsInARow) {
        // Only HTML can close the comment, and emphasis or a link can hold it
        if (inComment && inline !is HtmlInline) {
            addInlines(inline, parts)
            return
        }
        when (inline) {
            is Text -> parts.add(listOf(wordsOf(inline.literal, inline)))
            is SoftLineBreak, is HardLineBreak -> parts.add(listOf(lineBreakAt(inline, parts)))
            is Code -> parts.add(listOf(
                PlacedSilence(PAUSE_AROUND_INLINE_CODE_MILLISECONDS),
                wordsOf(inline.literal, inline),
                PlacedSilence(PAUSE_AROUND_INLINE_CODE_MILLISECONDS),
            ))
            is Strikethrough, is TaskListItemMarker -> Unit
            is HtmlInline -> parts.add(listOf(wordsOf(withoutComments(inline.literal), inline)))
            is Link -> if (hasText(inline)) addInlines(inline, parts) else parts.add(listOf(wordsSpreadOver(ADDRESS_WORDS, inline)))
            // Emphasis, strong emphasis, and an image, whose children are its alternative text
            else -> addInlines(inline, parts)
        }
    }

    // The words of a literal that can be shorter than its source, because of escapes, entities or backticks: each
    // character is matched to the next equal one of the source, searching forwards, and one that has none, such as the
    // character of an entity, takes the place the search is at
    private fun wordsOf(text: String, node: Node): PlacedWords {
        val range = rangeOf(node) ?: SourceRange(currentBlockStart, currentBlockStart)
        var cursor = range.start
        val offsets = text.map { character ->
            var found = cursor
            while (found < range.end && source[found] != character) found++
            if (found < range.end) cursor = found + 1
            minOf(if (found < range.end) found else cursor, maxOf(range.end - 1, range.start)) + shift
        }
        return PlacedWords(text, offsets, currentBlock)
    }

    // Words for an address, whose characters are spread over it so that a selection of any part of it reaches them
    private fun wordsSpreadOver(text: String, node: Node): PlacedWords {
        val range = rangeOf(node) ?: SourceRange(currentBlockStart, currentBlockStart)
        val length = range.end - range.start
        return PlacedWords(text, List(text.length) { range.start + it * length / text.length + shift }, currentBlock)
    }

    private fun wordsOfAWholeBlock(text: String, block: Node): PlacedWords {
        startBlock(block)
        val range = rangeOf(block) ?: SourceRange(currentBlockStart, currentBlockStart)
        val shifted = SourceRange(range.start + shift, range.end + shift)
        return PlacedWords(text, List(text.length) { shifted.start }, currentBlock, standsForBlock = shifted)
    }

    private fun lineBreakAt(lineBreak: Node, parts: PartsInARow): PlacedWords {
        val offset = rangeOf(lineBreak)?.start?.plus(shift) ?: parts.lastOffset()?.plus(1) ?: (currentBlockStart + shift)
        return PlacedWords("\n", listOf(offset), currentBlock)
    }

    // In the source without the front matter, from the first character of the node up to and not including the
    // last. The shift is added where the offsets are made
    private fun rangeOf(node: Node): SourceRange? {
        val spans = node.sourceSpans
        if (spans.isEmpty()) return null
        return SourceRange(spans.first().inputIndex, spans.last().inputIndex + spans.last().length)
    }

    // The HTML without its comments, and without what comes before the --> that closes a comment left open
    private fun withoutComments(html: String): String {
        var rest = html
        if (inComment) {
            val end = rest.indexOf(COMMENT_END)
            if (end < 0) return ""
            rest = rest.substring(end + COMMENT_END.length)
        }
        inComment = COMMENT.findAll(rest).lastOrNull()?.groups?.get(1) != null
        return COMMENT.replace(rest, "")
    }

    private fun unlessInAComment(parts: List<PlacedPart>): List<PlacedPart> = if (inComment) emptyList() else parts

    // A link written with text begins with [, as it is or as a reference. Any other is an address in angle brackets,
    // or a bare one that the autolink extension recognised
    private fun hasText(link: Link): Boolean {
        val start = link.sourceSpans.firstOrNull()?.inputIndex ?: return false
        return source.getOrNull(start) == '['
    }
}

// Parts in the order they come, with the words that follow each other joined into one
private class PartsInARow {
    val parts = mutableListOf<PlacedPart>()

    fun add(more: List<PlacedPart>) {
        for (part in more) {
            val last = parts.lastOrNull()
            if (part is PlacedWords && last is PlacedWords) parts[parts.lastIndex] = last + part else parts += part
        }
    }

    // The soft line breaks of the parser have no source span, and follow the character before them
    fun lastOffset(): Int? = (parts.lastOrNull() as? PlacedWords)?.offsets?.lastOrNull()
}

// The blocks that give something, with a pause between each two of them
private fun joined(blocks: List<List<PlacedPart>>, pauseMilliseconds: Int): List<PlacedPart> {
    val parts = mutableListOf<PlacedPart>()
    for (block in blocks.filterNot(::givesNothing)) {
        if (parts.isNotEmpty()) parts += PlacedSilence(pauseMilliseconds)
        parts += block
    }
    return parts
}

private fun childrenOf(parent: Node): List<Node> = generateSequence(parent.firstChild) { it.next }.toList()

private fun givesNothing(parts: List<PlacedPart>): Boolean = parts.all { it is PlacedWords && it.text.isBlank() }

// A full stop is added to the last words unless they end a sentence already, so that every item of a list ends alike
// and none gets a second pause. Words of white space only are no words, and get none. The full stop takes the offset of
// the last character before it
private fun endedAsASentence(parts: List<PlacedPart>): List<PlacedPart> {
    val lastWords = parts.indexOfLast { it is PlacedWords && it.text.isNotBlank() }
    if (lastWords < 0) return parts
    val words = parts[lastWords] as PlacedWords
    val text = words.text.trimEnd()
    if (endsWithASentenceEnd(text)) return parts
    val ended = words.slice(0, text.length) + PlacedWords(LIST_ITEM_END, listOf(words.offsets[text.length - 1]), words.block)
    return parts.toMutableList().also { it[lastWords] = ended }
}

// Silences next to each other become the longest of them, and nothing is silent before the first words or after the
// last. A silence next to words of white space only stays, since an emoji or a space stood there
private fun tidied(parts: List<SpokenPart>): List<SpokenPart> {
    val result = mutableListOf<SpokenPart>()
    for (part in parts) {
        if (part is Words && part.text.isEmpty()) continue
        val last = result.lastOrNull()
        if (part is Silence && last is Silence) {
            result[result.lastIndex] = Silence(maxOf(last.milliseconds, part.milliseconds))
        } else {
            result += part
        }
    }
    return result.dropWhile { it is Silence }.dropLastWhile { it is Silence }
}
