package com.ritenoar.aloudrite

// A range of the Markdown of a whole file, from its start up to and not including its end
data class SourceRange(val start: Int, val end: Int)

// An item of an ordered list, with the number as it is read, such as "3. "
data class NumberedItem(val range: SourceRange, val number: String)

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
    )

    // The characters from start up to and not including end, whose number marks are those that lie within them
    fun slice(start: Int, end: Int) = PlacedWords(
        text.substring(start, end),
        offsets.subList(start, end),
        block,
        standsForBlock,
        numbers.filter { it.start >= start && it.end <= end }.map { it.copy(start = it.start - start, end = it.end - start) },
        ends?.subList(start, end),
    )
}

data class PlacedSilence(val milliseconds: Int) : PlacedPart

// The parts of a file as they are read, and the items of its ordered lists
class PlacedMarkdown(val parts: List<PlacedPart>, val numberedItems: List<NumberedItem>)
