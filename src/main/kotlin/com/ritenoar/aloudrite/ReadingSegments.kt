package com.ritenoar.aloudrite

// How many seconds of audio the engine may make ahead of the playing
internal const val SECONDS_AHEAD = 30

// A word that can be heard: the range of the file it was read from, the range of the sentence it is part of, and its
// weight in the estimate of which word is heard
data class HeardWord(val place: SourceRange, val sentence: SourceRange, val weight: Int)

// A segment is one piece of words, as the plugin cuts the text, or one silence. A resume and a making begin at a segment
class ReadingSegment(val spoken: SpokenPart, val words: List<HeardWord>)

// The one cut of a reading into segments, which gives what the engine and the player work on and the places of the words
// in the file that the highlights need, so that the numbering of the segments is the same for both
class ReadingSegments(placedParts: List<PlacedPart>) {

    val segments: List<ReadingSegment> = segmentsOf(placedParts)

    val spoken: List<SpokenPart> = segments.map { it.spoken }
}

private class WordInAPiece(val start: Int, val place: SourceRange, val weight: Int)

// Where a sentence of the words of a block lies, as the first part of the block and the start of the sentence in the text
// of the block joined
private data class SentenceKey(val block: Int, val start: Int)

// A segment before the places of the sentences are known, which need every word of them
private sealed interface PendingSegment {
    class OfSilence(val silence: PlacedSilence) : PendingSegment
    class OfWords(val words: PlacedWords, val found: List<WordInAPiece>, val sentences: List<SentenceKey>) : PendingSegment
}

private fun segmentsOf(parts: List<PlacedPart>): List<ReadingSegment> {
    val pending = parts.flatMapIndexed { index, part ->
        when (part) {
            is PlacedSilence -> listOf(PendingSegment.OfSilence(part))
            is PlacedWords -> pendingSegmentsOf(parts, index, part)
        }
    }
    val sentencePlaces = HashMap<SentenceKey, SourceRange>()
    for (segment in pending.filterIsInstance<PendingSegment.OfWords>()) {
        segment.found.zip(segment.sentences).forEach { (word, key) ->
            sentencePlaces.merge(key, word.place) { first, second ->
                SourceRange(minOf(first.start, second.start), maxOf(first.end, second.end))
            }
        }
    }
    return pending.map { segment ->
        when (segment) {
            is PendingSegment.OfSilence -> ReadingSegment(spokenPartOf(segment.silence), emptyList())
            is PendingSegment.OfWords -> ReadingSegment(
                spokenPartOf(segment.words),
                segment.found.zip(segment.sentences) { word, key -> HeardWord(word.place, sentencePlaces.getValue(key), word.weight) },
            )
        }
    }
}

// Words that stand for a whole block are one sentence of their own, and the others are cut as the words of their block
// joined are
private fun pendingSegmentsOf(parts: List<PlacedPart>, index: Int, words: PlacedWords): List<PendingSegment> {
    val blockParts = if (words.standsForBlock == null) partsOfTheBlock(parts, index) else listOf(index)
    val sentenceStarts = if (words.standsForBlock == null) sentenceStartsOf(parts, blockParts) else listOf(0)
    val before = blockParts.takeWhile { it != index }.sumOf { (parts[it] as PlacedWords).text.length }
    return pieceRangesOf(words.text).map { range ->
        val piece = words.slice(range.start, range.end)
        val found = wordsOf(piece)
        val sentences = found.map { word ->
            SentenceKey(blockParts.first(), sentenceStarts.last { it <= before + range.start + word.start })
        }
        PendingSegment.OfWords(piece, found, sentences)
    }
}

// The words of a piece of words: runs of characters that are not white space, and, where the words stand for a whole
// block, one run over every character that is read from the block, white space included
private fun wordsOf(piece: PlacedWords): List<WordInAPiece> {
    val words = mutableListOf<WordInAPiece>()
    var start = -1
    var inTheBlock = false
    fun end(at: Int) {
        if (start < 0) return
        words += WordInAPiece(start, placeOf(piece, start, at), weightOf(piece.text.substring(start, at)))
        start = -1
    }
    for (character in piece.text.indices) {
        val ofTheBlock = isOfTheBlock(piece, character)
        val blank = piece.text[character].isWhitespace() && !ofTheBlock
        if (blank || (start >= 0 && ofTheBlock != inTheBlock)) end(character)
        if (!blank && start < 0) {
            start = character
            inTheBlock = ofTheBlock
        }
    }
    end(piece.text.length)
    return words
}

private fun isOfTheBlock(words: PlacedWords, character: Int): Boolean {
    val block = words.standsForBlock ?: return false
    return words.offsets[character] >= block.start && words.offsets[character] < block.end
}

// The place of anything over several characters runs from the smallest start of their places to the largest end
private fun placeOf(words: PlacedWords, start: Int, end: Int): SourceRange {
    val places = (start until end).map { character ->
        if (isOfTheBlock(words, character)) words.standsForBlock!! else SourceRange(words.offsets[character], words.endOf(character))
    }
    return SourceRange(places.minOf { it.start }, places.maxOf { it.end })
}

// The characters that are neither white space nor punctuation
private fun weightOf(word: String): Int = word.codePoints().filter { !Character.isWhitespace(it) && !isPunctuation(it) }.count().toInt()

private val PUNCTUATION = setOf(
    Character.CONNECTOR_PUNCTUATION, Character.DASH_PUNCTUATION, Character.START_PUNCTUATION, Character.END_PUNCTUATION,
    Character.INITIAL_QUOTE_PUNCTUATION, Character.FINAL_QUOTE_PUNCTUATION, Character.OTHER_PUNCTUATION,
).map { it.toInt() }.toSet()

private fun isPunctuation(codePoint: Int) = Character.getType(codePoint) in PUNCTUATION
