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
fun spokenPartsOf(markdown: String): List<SpokenPart> {
    val source = withoutFrontMatter(markdown)
    val parts = SpokenMarkdown(source).partsOfBlocks(PARSER.parse(source))
    // Only after parsing, so that a space in place of an emoji cannot change the structure, as four of them at the
    // start of a line would by making an indented code block
    return tidied(parts.map { if (it is Words) Words(withCharactersAboveFfffAsSpaces(it.text)) else it })
}

private class SpokenMarkdown(private val source: String) {

    // Whether a comment is open. An HTML block such as <div> ends at a blank line, and a comment it leaves open hides
    // what follows in a rendered file, up to the --> of a later piece of HTML
    private var inComment = false

    // The blocks of a document or a quote
    fun partsOfBlocks(container: Node): List<SpokenPart> =
        joined(childrenOf(container).map(::partsOfBlock), PAUSE_BETWEEN_BLOCKS_MILLISECONDS)

    private fun partsOfBlock(block: Node): List<SpokenPart> = when (block) {
        is Paragraph, is Heading -> partsOfInlines(block)
        is FencedCodeBlock, is IndentedCodeBlock ->
            unlessInAComment(listOf(Words(CODE_BLOCK_WORDS), Silence(PAUSE_AFTER_A_CODE_BLOCK_MILLISECONDS)))
        is TableBlock -> unlessInAComment(listOf(Words(TABLE_WORDS)))
        is ThematicBreak -> unlessInAComment(listOf(Silence(PAUSE_BETWEEN_BLOCKS_MILLISECONDS)))
        is HtmlBlock -> listOf(Words(withoutComments(block.literal)))
        is LinkReferenceDefinition -> emptyList()
        is ListBlock -> partsOfList(block)
        // A quote, and any other block that holds blocks
        else -> partsOfBlocks(block)
    }

    // The items of a list, and the blocks of an item, follow each other with the end of a sentence and the shorter
    // pause between list items between them
    private fun partsOfList(list: ListBlock): List<SpokenPart> = joined(
        childrenOf(list).mapIndexed { place, item ->
            endedAsASentence(partsOfItem(item, if (inComment) "" else numberOf(list, place)))
        },
        PAUSE_BETWEEN_LIST_ITEMS_MILLISECONDS,
    )

    private fun partsOfItem(item: Node, number: String): List<SpokenPart> {
        val blocks = childrenOf(item).filterNot { it is TaskListItemMarker }.map { block ->
            val blockParts = partsOfBlock(block)
            if (block is Paragraph) endedAsASentence(blockParts) else blockParts
        }
        val parts = PartsInARow()
        parts.add(listOf(Words(number)))
        parts.add(joined(blocks, PAUSE_BETWEEN_LIST_ITEMS_MILLISECONDS))
        return parts.parts
    }

    // The number is read, as it was in unit 0006. It is counted from the first, as a rendered list shows it
    private fun numberOf(list: ListBlock, place: Int): String {
        if (list !is OrderedList) return ""
        return "${(list.markerStartNumber ?: 1) + place}${list.markerDelimiter ?: "."} "
    }

    private fun partsOfInlines(parent: Node): List<SpokenPart> = PartsInARow().also { addInlines(parent, it) }.parts

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
            is Text -> parts.add(listOf(Words(inline.literal)))
            is SoftLineBreak, is HardLineBreak -> parts.add(listOf(Words("\n")))
            is Code -> parts.add(listOf(
                Silence(PAUSE_AROUND_INLINE_CODE_MILLISECONDS),
                Words(inline.literal),
                Silence(PAUSE_AROUND_INLINE_CODE_MILLISECONDS),
            ))
            is Strikethrough, is TaskListItemMarker -> Unit
            is HtmlInline -> parts.add(listOf(Words(withoutComments(inline.literal))))
            is Link -> if (hasText(inline)) addInlines(inline, parts) else parts.add(listOf(Words(ADDRESS_WORDS)))
            // Emphasis, strong emphasis, and an image, whose children are its alternative text
            else -> addInlines(inline, parts)
        }
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

    private fun unlessInAComment(parts: List<SpokenPart>): List<SpokenPart> = if (inComment) emptyList() else parts

    // A link written with text begins with [, as it is or as a reference. Any other is an address in angle brackets,
    // or a bare one that the autolink extension recognised
    private fun hasText(link: Link): Boolean {
        val start = link.sourceSpans.firstOrNull()?.inputIndex ?: return false
        return source.getOrNull(start) == '['
    }
}

// Parts in the order they come, with the text of words that follow each other joined into one
private class PartsInARow {
    val parts = mutableListOf<SpokenPart>()

    fun add(more: List<SpokenPart>) {
        for (part in more) {
            val last = parts.lastOrNull()
            if (part is Words && last is Words) parts[parts.lastIndex] = Words(last.text + part.text) else parts += part
        }
    }
}

// The blocks that give something, with a pause between each two of them
private fun joined(blocks: List<List<SpokenPart>>, pauseMilliseconds: Int): List<SpokenPart> {
    val parts = mutableListOf<SpokenPart>()
    for (block in blocks.filterNot(::givesNothing)) {
        if (parts.isNotEmpty()) parts += Silence(pauseMilliseconds)
        parts += block
    }
    return parts
}

private fun childrenOf(parent: Node): List<Node> = generateSequence(parent.firstChild) { it.next }.toList()

private fun givesNothing(parts: List<SpokenPart>): Boolean = parts.all { it is Words && it.text.isBlank() }

// A full stop is added to the last words unless they end a sentence already, so that every item of a list ends alike
// and none gets a second pause. Words of white space only are no words, and get none
private fun endedAsASentence(parts: List<SpokenPart>): List<SpokenPart> {
    val lastWords = parts.indexOfLast { it is Words && it.text.isNotBlank() }
    if (lastWords < 0) return parts
    val text = (parts[lastWords] as Words).text.trimEnd()
    if (endsWithASentenceEnd(text)) return parts
    return parts.toMutableList().also { it[lastWords] = Words(text + LIST_ITEM_END) }
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
