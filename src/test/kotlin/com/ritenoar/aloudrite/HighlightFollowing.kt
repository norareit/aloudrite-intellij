package com.ritenoar.aloudrite

import kotlin.math.abs

// The logic of HighlightFollowingMeasurement, unit 0032, on plain values: which word the model says at a moment, which word
// the estimate shows, and how often they are the same. It touches no model, no Python and no audio device

// The samples of one frame of the durations of Kokoro, and the sample rate its audio has
const val FRAME_SAMPLES = 600L
const val DURATIONS_SAMPLE_RATE = 24_000

// The samples between two moments at which the highlight is compared with the voice: 10 ms
const val MOMENT_SAMPLES = 240

// How far the length of the audio by the durations may be from that of the engine, as a share of the latter
private const val LENGTH_TOLERANCE = 0.02

private const val PERCENT = 100.0
private const val SHOWN_TEXT_LENGTH = 50
private val SPACE_TOKEN = ' '.code

// Where the weight of a word in the estimate counts a character, the Unicode categories of punctuation of ReadingSegments
private val PUNCTUATION_TYPES = setOf(
    Character.CONNECTOR_PUNCTUATION, Character.DASH_PUNCTUATION, Character.START_PUNCTUATION, Character.END_PUNCTUATION,
    Character.INITIAL_QUOTE_PUNCTUATION, Character.FINAL_QUOTE_PUNCTUATION, Character.OTHER_PUNCTUATION,
).map { it.toInt() }

// The tokens `firstToken` until `endToken` of the row `row`, which the model says as one word
data class PhonemeWord(val row: Int, val firstToken: Int, val endToken: Int)

private fun isBetweenWords(token: Int) = token == SPACE_TOKEN || Character.getType(token) in PUNCTUATION_TYPES

// The stress and length marks are modifier letters, so they are in no category of punctuation and belong to the word
fun phonemeWordsOf(rows: List<IntArray>): List<PhonemeWord> = rows.flatMapIndexed { rowNumber, row ->
    val words = mutableListOf<PhonemeWord>()
    var start = -1
    for (token in row.indices) {
        val inAWord = !isBetweenWords(row[token])
        if (inAWord && start < 0) start = token
        if (!inAWord && start >= 0) {
            words += PhonemeWord(rowNumber, start, token)
            start = -1
        }
    }
    if (start >= 0) words += PhonemeWord(rowNumber, start, row.size)
    words
}

sealed interface Pairing {
    // The word of the plugin for each phoneme word, in order
    class Paired(val wordsInOrder: List<HeardWord>) : Pairing

    class NotPaired(val reason: String) : Pairing
}

// The words of the plugin that weigh 0 are punctuation only, which eSpeak NG says nothing for, so they are not paired
fun pairedWords(phonemeWords: List<PhonemeWord>, words: List<HeardWord>): Pairing {
    val spoken = words.filter { it.weight > 0 }
    if (spoken.size != phonemeWords.size) return Pairing.NotPaired("${phonemeWords.size} phoneme words for ${spoken.size} words")
    return Pairing.Paired(spoken)
}

// The samples of the piece from `start` until `end` in which the model says one word
data class WordSpan(val start: Long, val end: Long)

// The rows lie end to end, each beginning where the one before ends, and the durations of a row hold the padding token at
// both ends, so the token `n` of the row has the frame `n + 1`
fun spansOf(rows: List<IntArray>, durations: List<IntArray>): List<WordSpan> {
    val rowStarts = durations.runningFold(0L) { start, frames -> start + frames.sum().toLong() * FRAME_SAMPLES }
    return phonemeWordsOf(rows).map { word ->
        val frames = durations[word.row]
        val start = rowStarts[word.row] + (0..word.firstToken).sumOf { frames[it].toLong() } * FRAME_SAMPLES
        val end = rowStarts[word.row] + (0..word.endToken).sumOf { frames[it].toLong() } * FRAME_SAMPLES
        WordSpan(start, end)
    }
}

fun lengthByTheDurations(durations: List<IntArray>): Long = durations.sumOf { it.sum().toLong() } * FRAME_SAMPLES

// Null where the two lengths agree, and else the reason the piece cannot be judged, with both lengths
fun lengthDisagreement(durationSamples: Long, engineSamples: Long, sampleRate: Int = DURATIONS_SAMPLE_RATE): String? {
    val difference = abs(durationSamples - engineSamples)
    if (difference <= engineSamples * LENGTH_TOLERANCE) return null
    return "audio %.2f s by the engine, %.2f s by the durations, %.1f%% apart".format(
        engineSamples.toDouble() / sampleRate, durationSamples.toDouble() / sampleRate, difference.toDouble() / engineSamples * PERCENT)
}

fun momentsOf(samples: Long): List<Long> = (0L until samples step MOMENT_SAMPLES.toLong()).toList()

// The number of the span that holds the moment, or null where it is in a pause, a padding token or the silence of an end
fun wordHeardAt(spans: List<WordSpan>, moment: Long): Int? =
    spans.indexOfFirst { moment >= it.start && moment < it.end }.takeIf { it >= 0 }

class FollowedMoments(val counted: Int, val matching: Int)

// The estimate is called at every moment, also at those that are not counted, because it never moves back within a piece and
// so depends on the moments before. `wordsHeard` is the word of the plugin for each span, and `reading` is told that every
// piece is made in full
fun followPiece(
    segment: Int,
    wordsHeard: List<HeardWord>,
    spans: List<WordSpan>,
    engineSamples: Long,
    reading: ReadingTime,
    estimate: HeardWordEstimate,
): FollowedMoments {
    var counted = 0
    var matching = 0
    for (moment in momentsOf(engineSamples)) {
        reading.heard(segment, moment)
        estimate.heard(segment, moment, reading.shareOfTheSegmentHeard())
        val heard = wordHeardAt(spans, moment) ?: continue
        counted++
        if (estimate.places?.word == wordsHeard[heard].place) matching++
    }
    return FollowedMoments(counted, matching)
}

sealed interface PieceResult {
    val number: Int
    val text: String
}

class FollowedPiece(override val number: Int, override val text: String, val counted: Int, val matching: Int) : PieceResult {
    fun shareText(): String = if (counted == 0) "-" else "%.1f%%".format(matching.toDouble() / counted * PERCENT)
}

class LeftOutPiece(override val number: Int, override val text: String, val reason: String) : PieceResult

class SpeedResult(val speed: String, val pieces: List<PieceResult>)

// The share over all counted pieces, which weighs each by its counted moments, or null where none is counted
fun shareOf(pieces: List<PieceResult>): Double? {
    val followed = pieces.filterIsInstance<FollowedPiece>()
    val counted = followed.sumOf { it.counted }
    return if (counted == 0) null else followed.sumOf { it.matching }.toDouble() / counted
}

fun tableOf(results: List<SpeedResult>): String = buildString {
    for (result in results) {
        val followed = result.pieces.filterIsInstance<FollowedPiece>()
        val share = shareOf(result.pieces)?.let { "%.1f%%".format(it * PERCENT) } ?: "-"
        val seconds = followed.sumOf { it.counted } * MOMENT_SAMPLES.toDouble() / DURATIONS_SAMPLE_RATE
        appendLine("speed ${result.speed}: share $share, ${"%.1f".format(seconds)} s of words counted, ${followed.size} pieces counted, ${result.pieces.size - followed.size} left out")
        for (piece in result.pieces) {
            val outcome = if (piece is FollowedPiece) piece.shareText() else "left out: ${(piece as LeftOutPiece).reason}"
            appendLine("%4d  %s  %s".format(piece.number, outcome, piece.text.replace(Regex("\\s+"), " ").take(SHOWN_TEXT_LENGTH)))
        }
        appendLine()
    }
}
