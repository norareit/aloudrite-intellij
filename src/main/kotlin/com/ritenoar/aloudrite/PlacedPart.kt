package com.ritenoar.aloudrite

// A range of the Markdown of a whole file, from its start up to and not including its end
data class SourceRange(val start: Int, val end: Int)

// An item of an ordered list, with the number as it is read, such as "3, ", and the marker as the file writes it, such as
// "10.", whose characters the words of the number stand for. The marker is as long as the number the file holds, which may
// be another than the one that is read
data class NumberedItem(
    val range: SourceRange,
    val number: String,
    val marker: SourceRange = SourceRange(range.start, range.start + 1),
)

// The characters of the words that are the number of an item, from start up to and not including end
data class NumberMark(val start: Int, val end: Int, val item: NumberedItem)

// What a reading is made of, as SpokenPart is, with where in the Markdown of the whole file, front matter included, each
// thing came from, so that a part of the file can be chosen before the words are made into what the engine is given
sealed interface PlacedPart

// offsets holds, for each character of the text, the offset of the character of the file it was read from, and a
// character that the plugin added takes that of the one next to it. The block is that of the paragraph, heading, code
// block or other block that the words are of, and the words of the number of an item are of the block of the item's first
// block. standsForBlock is set where the words stand for a whole block that is not read word by word, such as the
// words "code example"
data class PlacedWords(
    val text: String,
    val offsets: List<Int>,
    val block: Int,
    val standsForBlock: SourceRange? = null,
    val numbers: List<NumberMark> = emptyList(),
    val ends: List<Int>? = null,
    // The characters that the engine is given as a space for one that it cannot be given, which still count as a character
    // that is no white space where the sentences are cut, as they did before they were made a space
    val substituted: Set<Int> = emptySet(),
) : PlacedPart {
    // Up to and not including which offset the character of the file reaches. It is the next one for a character read
    // from one of the file, and further for words that stand for a longer source, such as "link" for an address
    fun endOf(character: Int): Int = ends?.get(character) ?: (offsets[character] + 1)

    operator fun plus(more: PlacedWords) = PlacedWords(
        text + more.text,
        offsets + more.offsets,
        block,
        standsForBlock ?: more.standsForBlock,
        numbers + more.numbers.map { it.copy(start = it.start + text.length, end = it.end + text.length) },
        if (ends == null && more.ends == null) null else List(text.length) { endOf(it) } + List(more.text.length) { more.endOf(it) },
        substituted + more.substituted.map { it + text.length },
    )

    // The text as the sentences are cut from it, where a character that was made a space is not white space
    fun textForTheSentences(): String =
        if (substituted.isEmpty()) text else String(CharArray(text.length) { if (it in substituted) SUBSTITUTE else text[it] })

    // The characters from start up to and not including end, whose number marks are those that lie within them
    fun slice(start: Int, end: Int) = PlacedWords(
        text.substring(start, end),
        offsets.subList(start, end),
        block,
        standsForBlock,
        numbers.filter { it.start >= start && it.end <= end }.map { it.copy(start = it.start - start, end = it.end - start) },
        ends?.subList(start, end),
        substituted.filter { it in start until end }.map { it - start }.toSet(),
    )

    // The words as the engine is given them: each character above U+FFFF is a space, as withCharactersAboveFfffAsSpaces
    // makes it, which takes the offset of the first half of the pair and reaches up to the end of the second half, so
    // that every character of what is read still has its place
    fun forTheEngine(): PlacedWords {
        if (text.codePoints().noneMatch { it > 0xFFFF }) return this
        val engineText = StringBuilder(text.length)
        val engineOffsets = mutableListOf<Int>()
        val engineEnds = mutableListOf<Int>()
        val engineSubstituted = mutableSetOf<Int>()
        // For each index of the text, how many characters come before it in the text for the engine
        val engineIndexes = IntArray(text.length + 1)
        var index = 0
        while (index < text.length) {
            val codePoint = text.codePointAt(index)
            val width = Character.charCount(codePoint)
            engineIndexes[index] = engineText.length
            if (codePoint > 0xFFFF) {
                engineSubstituted += engineText.length
                engineText.append(' ')
                engineOffsets += offsets[index]
                engineEnds += endOf(index + width - 1)
            } else {
                engineText.appendCodePoint(codePoint)
                engineOffsets += offsets[index]
                engineEnds += endOf(index)
            }
            index += width
            engineIndexes[index] = engineText.length
        }
        return PlacedWords(
            engineText.toString(),
            engineOffsets,
            block,
            standsForBlock,
            numbers.map { it.copy(start = engineIndexes[it.start], end = engineIndexes[it.end]) },
            engineEnds,
            engineSubstituted,
        )
    }

    private companion object {
        // Neither white space nor a mark that ends a sentence or closes one
        const val SUBSTITUTE = 'x'
    }
}

data class PlacedSilence(val milliseconds: Int) : PlacedPart

// The parts of a file as they are read, and the items of its ordered lists
class PlacedMarkdown(val parts: List<PlacedPart>, val numberedItems: List<NumberedItem>)
