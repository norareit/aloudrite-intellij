package com.ritenoar.aloudrite

// The words of the block of the words at that place, which inline code, for one, separates with silences
internal fun partsOfTheBlock(parts: List<PlacedPart>, place: Int): List<Int> {
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

// Where each sentence of the words of a block starts, in the text of those words joined, as the engine is given them.
// The number of an item is part of its first sentence, so that a sentence that starts after the start of a number and
// no later than its end starts at the start of the number
internal fun sentenceStartsOf(parts: List<PlacedPart>, blockParts: List<Int>): List<Int> {
    val words = blockParts.map { parts[it] as PlacedWords }
    val joined = words.joinToString("") { it.text }
    val numberStarts = mutableListOf<IntRange>()
    var passed = 0
    for (part in words) {
        part.numbers.forEach { numberStarts += (passed + it.start)..(passed + it.end) }
        passed += part.text.length
    }
    return sentencesOf(joined)
        .runningFold(0) { start, sentence -> start + sentence.length }
        .map { start -> numberStarts.firstOrNull { start > it.first && start <= it.last }?.first ?: start }
}
