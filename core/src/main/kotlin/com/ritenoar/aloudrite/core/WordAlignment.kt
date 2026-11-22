package com.ritenoar.aloudrite.core

import java.util.Locale

// The logic of finding the word heard from an alignment of the audio of a piece with its text, on plain values. It touches
// no model: the aligner of sherpa-onnx is behind WordAligner

// A stretch of the audio of a piece that the aligner gives for a text, in seconds from its start
data class AlignedInterval(val startSeconds: Double, val endSeconds: Double, val text: String)

// Aligns the audio of a piece with its words and gives the intervals of the words, in the order of the words
interface WordAligner : AutoCloseable {
    fun align(samples: FloatArray, sampleRate: Int, words: List<String>): List<AlignedInterval>
}

// The start, in samples of the piece's audio, of each word of the piece, null for a word that has none
class PieceAlignment(val starts: List<Long?>)

// The words that a word of the plugin is given to the aligner as: its text split at white space, each part stripped of
// punctuation at both ends and lowercased. A word of punctuation only has none, which is also a word of weight 0 in the
// estimate, so that the alignment never shows it
fun partsOf(word: HeardWord): List<String> =
    word.text.split(Regex("\\s+"))
        .map { part -> part.trim { isPunctuation(it.code) }.lowercase(Locale.ROOT) }
        .filter { it.isNotEmpty() }
        .map { part -> if (part.codePoints().anyMatch { it > MAXIMUM_BASIC_CODE_POINT }) WORD_FOR_AN_EMOJI else part }

// The aligner gives no interval for a character above U+FFFF, so that a piece that held one would have no alignment at all.
// Given a word its lexicon lacks, it places the speech of the name there, which keeps the words around it where they are
// heard. A test checks that the lexicon of the model holds no line for it
const val WORD_FOR_AN_EMOJI = "emoji"

// The intervals that have text are taken in order, one for each part given. Where their number is not that of the parts,
// their texts are not the parts, or a start is not a time in the audio of the piece or is before the start before it, or an end is not a finite time after its start, the
// aligner did not place the words and there is no alignment. The texts are compared in lower case, as the parts are given
fun wordStartsOf(
    partsOfWords: List<List<String>>,
    intervals: List<AlignedInterval>,
    sampleRate: Int,
    audioSamples: Int,
): PieceAlignment? {
    val spoken = intervals.filter { it.text.isNotBlank() }
    val parts = partsOfWords.flatten()
    if (spoken.size != parts.size) return null
    if (spoken.map { it.text.lowercase(Locale.ROOT) } != parts) return null
    val audioSeconds = audioSamples.toDouble() / sampleRate
    // A start must have a sample of the piece to be heard in, and an end is only a finite time that is not before the start,
    // since only the starts are used and the native code may round an end a little past the audio
    if (spoken.any { !(it.startSeconds >= 0.0 && it.startSeconds < audioSeconds && it.endSeconds.isFinite() && it.endSeconds >= it.startSeconds) }) return null
    if (spoken.zipWithNext().any { (before, after) -> after.startSeconds < before.startSeconds }) return null
    var next = 0
    val starts = partsOfWords.map { wordParts ->
        val first = next
        next += wordParts.size
        if (wordParts.isEmpty()) null else (spoken[first].startSeconds * sampleRate).toLong()
    }
    return PieceAlignment(starts)
}

// The word shown at a position of a piece: the last word whose start plus the delay is at or before the position, and the
// first word that has a start where there is none yet, so that a piece shows its first word from its start. Words without a
// start are never shown, and null is for a piece in which no word has one
fun wordShownAt(starts: List<Long?>, position: Long, delaySamples: Long): Int? {
    val reached = starts.indices.lastOrNull { number -> starts[number]?.let { it + delaySamples <= position } == true }
    return reached ?: starts.indexOfFirst { it != null }.takeIf { it >= 0 }
}
