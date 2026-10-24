package com.ritenoar.aloudrite

// Where in the text of a file the person is, as the editor shows it
sealed interface PlaceInTheText {
    // What is selected, from start up to and not including end
    data class Selection(val start: Int, val end: Int) : PlaceInTheText {
        init {
            require(start < end) { "A selection of $start up to $end is empty" }
        }
    }

    data class Caret(val offset: Int) : PlaceInTheText

    // A preview without the text of the file, which shows no caret
    data object NoText : PlaceInTheText
}

// What a press reads of the file
enum class PartOfTheFile { WHOLE_FILE, SELECTION, FROM_THE_CARET }

class WhatToRead(val parts: List<SpokenPart>, val partOfTheFile: PartOfTheFile)

// The part of the file that a press reads. The file is parsed as a whole and the part is cut out of what the whole
// file would read, so that a part means what it means in the file: a code block, a table, a reference link and a comment
// that is open before the part are read as they are in a full reading
fun whatToRead(markdown: String, place: PlaceInTheText): WhatToRead {
    val placed = placedPartsOf(markdown)
    return when (place) {
        PlaceInTheText.NoText -> wholeFile(placed)
        is PlaceInTheText.Selection ->
            WhatToRead(spokenPartsFrom(selected(placed, place)), PartOfTheFile.SELECTION)
        is PlaceInTheText.Caret -> fromTheCaret(placed, place)
    }
}

private fun wholeFile(placed: PlacedMarkdown) = WhatToRead(spokenPartsFrom(placed.parts), PartOfTheFile.WHOLE_FILE)

// Where in the parts: the part, and the character of its words
private class Position(val part: Int, val character: Int)

// The selection

// Everything from the first part that the selection touches to the last, with a word that it cuts read whole. A part
// that stands for a whole block is touched by a selection that overlaps the block at all
private fun selected(placed: PlacedMarkdown, selection: PlaceInTheText.Selection): List<PlacedPart> {
    val parts = placed.parts
    val touched = parts.map { if (it is PlacedWords) charactersTouched(it, selection) else null }
    val first = touched.indexOfFirst { it != null }
    if (first < 0) return emptyList()
    val last = touched.indexOfLast { it != null }
    val chosen = parts.subList(first, last + 1).toMutableList()
    val firstWords = chosen.first() as PlacedWords
    val lastWords = chosen.last() as PlacedWords
    val start = wordStart(firstWords, touched[first]!!.first)
    val end = wordEnd(lastWords, touched[last]!!.last)
    if (first == last) {
        chosen[0] = firstWords.sliceUnlessAWholeBlock(start, end)
    } else {
        chosen[0] = firstWords.sliceUnlessAWholeBlock(start, firstWords.text.length)
        chosen[chosen.lastIndex] = lastWords.sliceUnlessAWholeBlock(0, end)
    }
    return withItsNumber(chosen, placed.numberedItems)
}

private fun charactersTouched(words: PlacedWords, selection: PlaceInTheText.Selection): IntRange? {
    words.standsForBlock?.let {
        return if (selection.start < it.end && selection.end > it.start) words.text.indices else null
    }
    val touched = words.offsets.indices.filter { words.offsets[it] < selection.end && words.endOf(it) > selection.start }
    return if (touched.isEmpty()) null else touched.first()..touched.last()
}

// The words keep their characters of a whole block, which are not words of their own
private fun PlacedWords.sliceUnlessAWholeBlock(start: Int, end: Int): PlacedWords =
    if (standsForBlock != null) this else slice(start, end)

// Back to the start of the word that the character is in, and a character of white space is no word
private fun wordStart(words: PlacedWords, character: Int): Int {
    var start = character
    while (start > 0 && !words.text[start].isWhitespace() && !words.text[start - 1].isWhitespace()) start--
    return start
}

// On to the end of the word that the character is in, counted as up to and not including
private fun wordEnd(words: PlacedWords, character: Int): Int {
    var end = character + 1
    while (end < words.text.length && !words.text[end - 1].isWhitespace() && !words.text[end].isWhitespace()) end++
    return end
}

// The caret

private fun fromTheCaret(placed: PlacedMarkdown, caret: PlaceInTheText.Caret): WhatToRead {
    val parts = placed.parts
    val hit = firstReadAtOrAfter(parts, caret.offset) ?: return wholeFile(placed)
    val start = startOfTheSentence(parts, hit)
    if (nothingIsReadBefore(parts, start)) return wholeFile(placed)
    val startWords = parts[start.part] as PlacedWords
    val first = startWords.sliceUnlessAWholeBlock(start.character, startWords.text.length)
    val tail = listOf<PlacedPart>(first) + parts.drop(start.part + 1)
    return WhatToRead(spokenPartsFrom(withItsNumber(tail, placed.numberedItems)), PartOfTheFile.FROM_THE_CARET)
}

// The first words after the caret that are read: a whole block that holds the caret or begins after it, and else the
// first character that is not white space and reaches the caret or lies after it. The caret in text that is not read
// is so given to the next words that are
private fun firstReadAtOrAfter(parts: List<PlacedPart>, caret: Int): Position? {
    parts.forEachIndexed { index, part ->
        if (part !is PlacedWords) return@forEachIndexed
        val wholeBlock = part.standsForBlock
        if (wholeBlock != null) {
            if (caret < wholeBlock.end) return Position(index, 0)
            return@forEachIndexed
        }
        val character = part.text.indices.firstOrNull { !part.text[it].isWhitespace() && part.endOf(it) > caret }
        if (character != null) return Position(index, character)
    }
    return null
}

// The sentences are those of the words of the block joined, as the engine is given them, and the number of an item is
// part of its first sentence
private fun startOfTheSentence(parts: List<PlacedPart>, hit: Position): Position {
    val words = parts[hit.part] as PlacedWords
    if (words.standsForBlock != null) return Position(hit.part, 0)
    val blockParts = partsOfTheBlock(parts, hit.part)
    val before = blockParts.takeWhile { it != hit.part }.sumOf { (parts[it] as PlacedWords).text.length }
    val joined = blockParts.joinToString("") { (parts[it] as PlacedWords).text }
    val sentenceStart = sentencesOf(joined)
        .runningFold(0) { start, sentence -> start + sentence.length }
        .last { it <= before + hit.character }
    return positionIn(parts, blockParts, sentenceStart)
}

// The words of the block of the words at that place, which inline code, for one, separates with silences
private fun partsOfTheBlock(parts: List<PlacedPart>, place: Int): List<Int> {
    val block = (parts[place] as PlacedWords).block
    fun isOfTheBlock(index: Int) = (parts.getOrNull(index) as? PlacedWords)?.let { it.block == block } == true
    fun isTheSilenceInIt(index: Int, step: Int) =
        parts.getOrNull(index) is PlacedSilence && isOfTheBlock(index + step)
    var from = place
    while (isOfTheBlock(from - 1) || (isTheSilenceInIt(from - 1, -1))) from--
    var to = place
    while (isOfTheBlock(to + 1) || (isTheSilenceInIt(to + 1, 1))) to++
    return (from..to).filter { parts[it] is PlacedWords }
}

private fun positionIn(parts: List<PlacedPart>, blockParts: List<Int>, joinedIndex: Int): Position {
    var passed = 0
    for (index in blockParts) {
        val words = parts[index] as PlacedWords
        if (joinedIndex < passed + words.text.length) {
            val character = joinedIndex - passed
            val number = words.numbers.firstOrNull { character > it.start && character <= it.end }
            return Position(index, number?.start ?: character)
        }
        passed += words.text.length
    }
    return Position(blockParts.last(), (parts[blockParts.last()] as PlacedWords).text.length)
}

private fun nothingIsReadBefore(parts: List<PlacedPart>, start: Position): Boolean {
    val before = (parts[start.part] as PlacedWords).text.substring(0, start.character)
    return before.isBlank() && parts.take(start.part).none { it is PlacedWords && it.text.isNotBlank() }
}

// The number of an item

// Where the part begins in an item of a list after its number, the number of the innermost such item is put before it
private fun withItsNumber(chosen: List<PlacedPart>, items: List<NumberedItem>): List<PlacedPart> {
    val first = chosen.firstOrNull() as? PlacedWords ?: return chosen
    val offset = first.offsets.firstOrNull() ?: return chosen
    val item = items.filter { offset >= it.range.start && offset < it.range.end }
        .minByOrNull { it.range.end - it.range.start } ?: return chosen
    if (first.numbers.any { it.item == item }) return chosen
    val number = PlacedWords(
        item.number, List(item.number.length) { item.range.start }, first.block,
        numbers = listOf(NumberMark(0, item.number.length, item)))
    return listOf<PlacedPart>(number + first) + chosen.drop(1)
}
