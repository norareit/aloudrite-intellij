package com.ritenoar.aloudrite

import kotlin.math.abs
import kotlin.math.min

// The logic of HighlightFollowingMeasurement, unit 0032, on plain values: which word the model says at a moment, which word
// the estimate shows, and how often they are the same. It touches no model and no audio device

// The samples of one frame of the durations of Kokoro, and the sample rate its audio has
const val FRAME_SAMPLES = 600L
const val DURATIONS_SAMPLE_RATE = 24_000

// The samples between two moments at which the highlight is compared with the voice: 10 ms
const val MOMENT_SAMPLES = 240

private const val PERCENT = 100.0
private const val SHOWN_TEXT_LENGTH = 50
private const val STRESS_MARKS = "ˈˌ"
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

// The phonemes of a phoneme word or of rows without the space token, the punctuation and the stress marks, which eSpeak NG
// moves where it joins words. The length mark stays
private fun isBare(token: Int) = !isBetweenWords(token) && token.toChar() !in STRESS_MARKS

fun barePhonemesOf(row: IntArray): String = row.filter { isBare(it) }.joinToString("") { Character.toString(it) }

fun barePhonemesOf(row: IntArray, word: PhonemeWord): String = barePhonemesOf(row.copyOfRange(word.firstToken, word.endToken))

fun barePhonemesOfRows(rows: List<IntArray>): String = rows.joinToString("") { barePhonemesOf(it) }

sealed interface WordTexts {
    class Found(val texts: List<String>) : WordTexts

    class NotFound(val reason: String) : WordTexts
}

// The runs of characters that are not white space, which is how ReadingSegments finds the words. Where there are not as many
// as the piece's words, as for words that stand for a whole block, the texts are not known
fun textsOfWords(pieceText: String, wordCount: Int): WordTexts {
    val runs = pieceText.split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (runs.size != wordCount) return WordTexts.NotFound("${runs.size} runs of text for $wordCount words")
    return WordTexts.Found(runs)
}

// A word of the plugin with the text it is made into phonemes from and its bare phonemes
class WordToGroup(val word: HeardWord, val text: String, val barePhonemes: String)

sealed interface Grouping {
    // The words of the plugin of each phoneme word, in order
    class Grouped(val groups: List<List<HeardWord>>) : Grouping

    class NotGrouped(val reason: String) : Grouping
}

// How much an alignment costs: the edit distance, and then the number of steps over more than one word or phoneme word
private data class AlignmentCost(val distance: Int, val joinsAndSplits: Int) : Comparable<AlignmentCost> {
    override fun compareTo(other: AlignmentCost) =
        compareValuesBy(this, other, AlignmentCost::distance, AlignmentCost::joinsAndSplits)

    operator fun plus(other: AlignmentCost) = AlignmentCost(distance + other.distance, joinsAndSplits + other.joinsAndSplits)
}

// One step of an alignment: `phonemeWords` phoneme words with `words` words, of which one is 1
private class AlignmentStep(val phonemeWords: Int, val words: Int, val cost: AlignmentCost)

private fun editDistance(first: String, second: String): Int {
    var previous = IntArray(second.length + 1) { it }
    for (row in 1..first.length) {
        val current = IntArray(second.length + 1)
        current[0] = row
        for (column in 1..second.length) {
            val substitution = previous[column - 1] + if (first[row - 1] == second[column - 1]) 0 else 1
            current[column] = min(substitution, min(previous[column], current[column - 1]) + 1)
        }
        previous = current
    }
    return previous[second.length]
}

// The alignment in order of the phoneme words with the words that costs least. A step is a phoneme word with a run of one
// or more words, or a word with a run of two or more phoneme words. The words of the plugin that weigh 0 are punctuation
// only, which eSpeak NG says nothing for, and so are in no group
fun groupsOf(phonemeWords: List<String>, words: List<WordToGroup>): Grouping {
    val spoken = words.filter { it.word.weight > 0 }
    val best = Array(phonemeWords.size + 1) { arrayOfNulls<Pair<AlignmentCost, AlignmentStep?>>(spoken.size + 1) }
    best[0][0] = AlignmentCost(0, 0) to null
    for (done in 0..phonemeWords.size) {
        for (usedWords in 0..spoken.size) {
            val (costSoFar, _) = best[done][usedWords] ?: continue
            for (step in stepsFrom(phonemeWords, spoken, done, usedWords)) {
                val total = costSoFar + step.cost
                val target = best[done + step.phonemeWords][usedWords + step.words]
                if (target == null || total < target.first) best[done + step.phonemeWords][usedWords + step.words] = total to step
            }
        }
    }
    if (best[phonemeWords.size][spoken.size] == null) {
        return Grouping.NotGrouped("${phonemeWords.size} phoneme words for ${spoken.size} words")
    }
    val steps = mutableListOf<Pair<AlignmentStep, Int>>()
    var done = phonemeWords.size
    var usedWords = spoken.size
    while (done > 0 || usedWords > 0) {
        val step = checkNotNull(best[done][usedWords]!!.second)
        done -= step.phonemeWords
        usedWords -= step.words
        steps += step to usedWords
    }
    steps.reverse()
    val split = steps.firstOrNull { (step, _) -> step.phonemeWords > 1 }
    if (split != null) return Grouping.NotGrouped("eSpeak NG reads ${spoken[split.second].text} as ${split.first.phonemeWords} phoneme words")
    return Grouping.Grouped(steps.map { (step, first) -> spoken.subList(first, first + step.words).map { it.word } })
}

private fun stepsFrom(phonemeWords: List<String>, words: List<WordToGroup>, done: Int, usedWords: Int): List<AlignmentStep> {
    val steps = mutableListOf<AlignmentStep>()
    if (done < phonemeWords.size) {
        for (count in 1..words.size - usedWords) {
            val joined = words.subList(usedWords, usedWords + count).joinToString("") { it.barePhonemes }
            steps += AlignmentStep(1, count, AlignmentCost(editDistance(phonemeWords[done], joined), if (count > 1) 1 else 0))
        }
    }
    if (usedWords < words.size) {
        for (count in 2..phonemeWords.size - done) {
            val joined = phonemeWords.subList(done, done + count).joinToString("")
            steps += AlignmentStep(count, 1, AlignmentCost(editDistance(words[usedWords].barePhonemes, joined), 1))
        }
    }
    return steps
}

// The id of each token code point is the number after the last space of its line, which is also so of the token of the gap
// between words, a space
fun tokenIdsOf(tokenLines: List<String>): Map<Int, Int> =
    tokenLines
        .filter { it.contains(' ') }
        .map { it.substring(0, it.lastIndexOf(' ')) to it.substring(it.lastIndexOf(' ') + 1) }
        .filter { (token, _) -> token.isNotEmpty() && token.codePointCount(0, token.length) == 1 }
        .associate { (token, id) -> token.codePointAt(0) to id.toInt() }

// The ids of a row, with the padding id 0 at both ends as sherpa-onnx gives them to the model
fun idsOfRow(row: IntArray, ids: Map<Int, Int>): LongArray =
    LongArray(row.size + 2) { position -> if (position == 0 || position == row.size + 1) 0L else ids.getValue(row[position - 1]).toLong() }

const val STYLE_ROWS = 510
const val STYLE_VALUES = 256

// The style of a row of `tokens` tokens, without the padding, is row `tokens - 1` of the speaker, as the Kokoro package takes
// it, held within the rows there are
fun styleOf(voices: FloatArray, speaker: Int, tokens: Int): FloatArray {
    require(voices.size % (STYLE_ROWS * STYLE_VALUES) == 0) {
        "voices.bin holds ${voices.size} values, which is not a whole number of speakers of $STYLE_ROWS by $STYLE_VALUES values"
    }
    val start = (speaker * STYLE_ROWS + (tokens - 1).coerceIn(0, STYLE_ROWS - 1)) * STYLE_VALUES
    return voices.copyOfRange(start, start + STYLE_VALUES)
}

sealed interface Placement {
    // The sample of the model's audio where the engine's begins, and how far the two are apart there, as a share of the level
    class Placed(val offset: Int, val difference: Double) : Placement

    class NotPlaced(val reason: String) : Placement
}

// The most the engine's audio may differ from the model's at the best place, as a share of its level
private const val MOST_DIFFERENCE = 0.2
private const val COARSE_STEP = 16
private const val FINE_RADIUS = 32

private fun differenceAt(engine: FloatArray, model: FloatArray, offset: Int, step: Int): Double {
    var sum = 0.0
    var count = 0
    for (index in engine.indices step step) {
        sum += abs(engine[index] - model[offset + index])
        count++
    }
    return sum / count
}

// The offset where the mean absolute difference is least: every 16th sample at every offset, and then every sample near the
// best of those, which the unit's engine and `model.onnx` differ in rounding and threads only
fun placementOf(engine: FloatArray, model: FloatArray, rowNumber: Int): Placement {
    if (engine.isEmpty()) return Placement.NotPlaced("row $rowNumber: the engine made no audio")
    if (engine.size > model.size) return Placement.NotPlaced("row $rowNumber: the engine's audio is longer than the model's")
    val offsets = 0..model.size - engine.size
    val coarse = offsets.minBy { differenceAt(engine, model, it, COARSE_STEP) }
    val near = maxOf(offsets.first, coarse - FINE_RADIUS)..min(offsets.last, coarse + FINE_RADIUS)
    val offset = near.minBy { differenceAt(engine, model, it, 1) }
    val level = engine.indices.sumOf { abs(model[offset + it]).toDouble() } / engine.size
    if (level == 0.0) return Placement.NotPlaced("row $rowNumber: the model's audio is silent where it is placed")
    val difference = differenceAt(engine, model, offset, 1) / level
    if (difference > MOST_DIFFERENCE) {
        return Placement.NotPlaced("row $rowNumber: differs by %.1f%% at the best place".format(difference * PERCENT))
    }
    return Placement.Placed(offset, difference)
}

// The samples of the piece from `start` until `end` in which the model says one word
data class WordSpan(val start: Long, val end: Long)

// A row of a piece: the durations of its tokens with the padding at both ends, where its audio of the engine starts in the
// audio of the piece and how long it is, and where that audio starts within the model's audio of the row
class PlacedRow(val durations: IntArray, val start: Long, val length: Long, val offset: Long)

// A word lies in the model's audio of its row by the durations, the token `n` of the row having the frame `n + 1`, and in the
// piece's audio from the row's start in the engine's audio plus that, less the row's offset. It is held within the engine's
// audio of the row, so that what falls in the silence that the engine cuts off is in no span
fun spansOf(rows: List<IntArray>, placedRows: List<PlacedRow>): List<WordSpan> =
    phonemeWordsOf(rows).map { word ->
        val placed = placedRows[word.row]
        fun inThePiece(tokens: Int): Long {
            val inTheModel = (0..tokens).sumOf { placed.durations[it].toLong() } * FRAME_SAMPLES
            return (placed.start + inTheModel - placed.offset).coerceIn(placed.start, placed.start + placed.length)
        }
        WordSpan(inThePiece(word.firstToken), inThePiece(word.endToken))
    }

fun momentsOf(samples: Long): List<Long> = (0L until samples step MOMENT_SAMPLES.toLong()).toList()

// The number of the span that holds the moment, or null where it is in a pause, a padding token or the silence of an end
fun wordHeardAt(spans: List<WordSpan>, moment: Long): Int? =
    spans.indexOfFirst { moment >= it.start && moment < it.end }.takeIf { it >= 0 }

class FollowedMoments(val counted: Int, val matching: Int, val grouped: Int)

// The estimate is called at every moment, also at those that are not counted, because it never moves back within a piece and
// so depends on the moments before. `groupsHeard` is the words of the plugin of each span, and `reading` is told that every
// piece is made in full
fun followPiece(
    segment: Int,
    groupsHeard: List<List<HeardWord>>,
    spans: List<WordSpan>,
    engineSamples: Long,
    reading: ReadingTime,
    estimate: HeardWordEstimate,
): FollowedMoments {
    var counted = 0
    var matching = 0
    var grouped = 0
    for (moment in momentsOf(engineSamples)) {
        reading.heard(segment, moment)
        estimate.heard(segment, moment, reading.shareOfTheSegmentHeard())
        val heard = groupsHeard[wordHeardAt(spans, moment) ?: continue]
        counted++
        if (heard.size > 1) grouped++
        if (heard.any { it.place == estimate.places?.word }) matching++
    }
    return FollowedMoments(counted, matching, grouped)
}

sealed interface PieceResult {
    val label: String
    val text: String
}

class FollowedPiece(override val label: String, override val text: String, val counted: Int, val matching: Int, val grouped: Int) : PieceResult {
    fun shareText(): String = if (counted == 0) "-" else "%.1f%%".format(matching.toDouble() / counted * PERCENT)
}

class LeftOutPiece(override val label: String, override val text: String, val reason: String) : PieceResult

class SpeedResult(val speed: String, val pieces: List<PieceResult>)

class SetResult(val name: String, val speeds: List<SpeedResult>)

// The share over all counted pieces, which weighs each by its counted moments, or null where none is counted
fun shareOf(pieces: List<PieceResult>): Double? = shareOfCounted(pieces) { it.matching }

// The share of the counted moments that fell in a group of more than one word
fun shareInGroupsOf(pieces: List<PieceResult>): Double? = shareOfCounted(pieces) { it.grouped }

private fun shareOfCounted(pieces: List<PieceResult>, part: (FollowedPiece) -> Int): Double? {
    val followed = pieces.filterIsInstance<FollowedPiece>()
    val counted = followed.sumOf { it.counted }
    return if (counted == 0) null else followed.sumOf(part).toDouble() / counted
}

private fun percentText(share: Double?) = share?.let { "%.1f%%".format(it * PERCENT) } ?: "-"

fun tableOf(sets: List<SetResult>): String = buildString {
    for (set in sets) {
        appendLine(set.name)
        appendLine()
        for (result in set.speeds) {
            val followed = result.pieces.filterIsInstance<FollowedPiece>()
            val seconds = followed.sumOf { it.counted } * MOMENT_SAMPLES.toDouble() / DURATIONS_SAMPLE_RATE
            appendLine(
                "speed ${result.speed}: share ${percentText(shareOf(result.pieces))}, share in groups ${percentText(shareInGroupsOf(result.pieces))}, " +
                    "${"%.1f".format(seconds)} s of words counted, ${followed.size} pieces counted, ${result.pieces.size - followed.size} left out",
            )
            for (piece in result.pieces) {
                val outcome = if (piece is FollowedPiece) piece.shareText() else "left out: ${(piece as LeftOutPiece).reason}"
                appendLine("%4s  %s  %s".format(piece.label, outcome, piece.text.replace(Regex("\\s+"), " ").take(SHOWN_TEXT_LENGTH)))
            }
            appendLine()
        }
    }
}
