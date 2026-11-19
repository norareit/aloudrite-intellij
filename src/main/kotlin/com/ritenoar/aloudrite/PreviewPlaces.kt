package com.ritenoar.aloudrite

// A run of the characters of a text node of the preview, in the UTF-16 units of the text of the page
data class TextPiece(val node: Int, val start: Int, val end: Int)

// Where a word is drawn in the preview: on the pieces of text that show it, or, where the preview shows it in no text, on
// the element named by its range
data class WordPlace(val pieces: List<TextPiece>, val outlinedElement: SourceRange?)

// Finds the places of the file in the text of a page of the preview. The page carries the ranges of the file on its elements
// and not on words, so the characters of a text node are matched, in order, with the characters of the span of the file it
// was made from. The marks that the preview does not show, such as the asterisks of bold text, the backslash of an escape
// and the address of a link, are passed over in that match. It knows nothing of pages or threads
class PreviewPlaces(private val text: PreviewText, private val source: CharSequence) {

    private val offsetsOfNodes = arrayOfNulls<IntArray>(text.nodes.size)

    // How many text nodes were aligned with the file, since the alignment is worked out the first time a node is needed
    internal var alignmentsMade = 0
        private set

    // The runs of characters, one for each text node that holds them, that the characters of the range of the file were
    // read from. A run of white space only is not text that anything is read from
    fun pieces(range: SourceRange): List<TextPiece> =
        text.nodes.filter { overlaps(it.span, range) }.flatMap { piecesOf(it, range) }

    // The word as the preview shows it. Where no text shows it, the word is all of the text of the smallest element that
    // holds it, such as the link whose address is read, and where that holds no text, the element itself is outlined
    fun wordPlace(range: SourceRange): WordPlace {
        pieces(range).takeIf { it.isNotEmpty() }?.let { return WordPlace(it, null) }
        val element = smallestElementHolding(range) ?: return WordPlace(emptyList(), null)
        val textOfElement = (element.firstNode until element.endNode).map(text.nodes::get)
            .filterNot { it.text.isBlank() }
            .map { TextPiece(it.index, 0, it.text.length) }
        return if (textOfElement.isEmpty()) WordPlace(emptyList(), element.range) else WordPlace(textOfElement, null)
    }

    // Of equal size the last one wins, which is the one opened last and so the innermost
    private fun smallestElementHolding(range: SourceRange): PreviewElement? =
        text.elements.withIndex()
            .filter { it.value.range.start <= range.start && range.end <= it.value.range.end }
            .minWithOrNull(compareBy<IndexedValue<PreviewElement>> { it.value.range.end - it.value.range.start }.thenByDescending { it.index })
            ?.value

    // An empty span is a point that the text of its node sits at
    private fun overlaps(span: SourceRange, range: SourceRange): Boolean =
        if (span.start == span.end) span.start in range.start until range.end
        else span.start < range.end && range.start < span.end

    private fun piecesOf(node: PreviewTextNode, range: SourceRange): List<TextPiece> {
        val offsets = offsetsOf(node)
        val pieces = mutableListOf<TextPiece>()
        var runStart = -1
        for (position in 0..node.text.length) {
            val inside = position < node.text.length && offsets[position] in range.start until range.end
            if (inside && runStart < 0) runStart = position
            if (!inside && runStart >= 0) {
                if (node.text.substring(runStart, position).isNotBlank()) pieces += TextPiece(node.index, runStart, position)
                runStart = -1
            }
        }
        return pieces
    }

    private fun offsetsOf(node: PreviewTextNode): IntArray =
        offsetsOfNodes[node.index] ?: align(node).also {
            offsetsOfNodes[node.index] = it
            alignmentsMade++
        }

    // The offset of each character of the node in the file. An entity of the file, such as "&copy;" or "&#32;", is one
    // character of the node, and its offset is where it starts. A character with no match gets the place reached, which
    // does not move
    private fun align(node: PreviewTextNode): IntArray {
        val limit = minOf(node.span.end, source.length)
        var place = node.span.start
        return IntArray(node.text.length) { index ->
            val match = matchOf(node.text[index], place, limit)
            if (match == null) {
                place
            } else {
                place = match.next
                match.offset
            }
        }
    }

    private class Match(val offset: Int, val next: Int)

    // Marks that the preview does not show are passed over. An entity met on the way is the character itself, since the
    // characters of a node are in the order of the file and an entity always shows as one of them
    private fun matchOf(character: Char, from: Int, limit: Int): Match? {
        for (position in from until limit) {
            val entityEnd = entityEndAt(position, limit)
            if (entityEnd != null) return Match(position, entityEnd)
            if (sameCharacter(character, source[position])) return Match(position, position + 1)
        }
        return null
    }

    private fun entityEndAt(position: Int, limit: Int): Int? {
        if (source[position] != '&') return null
        val end = (position + 1 until minOf(limit, position + ENTITY_LENGTH_LIMIT)).firstOrNull { source[it] == ';' } ?: return null
        return (end + 1).takeIf { ENTITY.matches(source.subSequence(position, end + 1)) }
    }

    private fun sameCharacter(shown: Char, written: Char) = shown == written || (shown.isWhitespace() && written.isWhitespace())
}

private const val ENTITY_LENGTH_LIMIT = 34
private val ENTITY = Regex("&(#[0-9]+|#[xX][0-9a-fA-F]+|[A-Za-z][A-Za-z0-9]*);")
