package com.ritenoar.aloudrite

import java.util.Locale

// What the model takes in one run. The engine adds the padding token at both ends of a row and cuts a row after 401 tokens
// wherever that falls, also in the middle of a word, so no row of the front end is longer than this
const val MAXIMUM_TOKENS_PER_ROW = 400

// A row of this many tokens or fewer is joined to the row before it, as the front end of sherpa-onnx 1.13.8 did
private const val SHORT_ROW_TOKENS = 10
private const val TINY_ROW_TOKENS = 2
private const val JOINED_ROWS_BELOW_TOKENS = 46

private val SPACE = ' '.code

// The punctuation that is a token of its own, and the part of it that ends a row
private val PUNCTUATION_TOKENS = setOf(";", ":", ",", ".", "!", "?", "—", "…", "\"", "(", ")", "“", "”")
private val ROW_ENDS = setOf(".", "!", "?", ";")

// The letters with a diacritic that 1.13.8 kept inside a word, as `IsGermanUmlaut`, `IsSpanishDiacritic` and
// `IsFrenchDiacritic` of sherpa-onnx accept them
private val JOINING_RANGES = listOf(
    0x00C0..0x00C2, 0x00C4..0x00C4, 0x00C7..0x00CB, 0x00CD..0x00CF, 0x00D1..0x00D1, 0x00D3..0x00D4, 0x00D6..0x00D6,
    0x00D9..0x00DC, 0x00DF..0x00DF, 0x00E0..0x00E2, 0x00E4..0x00E4, 0x00E7..0x00EB, 0x00ED..0x00EF, 0x00F1..0x00F1,
    0x00F3..0x00F4, 0x00F6..0x00F6, 0x00F9..0x00FC,
)

// The replacements that 1.13.8 made before anything else, in this order, and the runs of white space as one space
private val REPLACEMENTS = listOf(
    "，" to ",", ":" to ",", "、" to ",", "；" to ";", "：" to ":", "。" to ".", "？" to "?", "！" to "!",
)
private val WHITE_SPACE_RUN = Regex("[ \\t\\n\\r\\u000B\\u000C]+")

// The code points that are tokens of the model, from the lines of its tokens.txt. A line is the token, a space and its
// number, and the token of the gap between words is itself a space, so the number is what follows the last space. A
// token of more than one code point cannot be given as one and is left out
fun tokenCodePointsOf(tokenLines: List<String>): Set<Int> =
    tokenLines
        .filter { it.contains(' ') }
        .map { it.substring(0, it.lastIndexOf(' ')) }
        .filter { it.isNotEmpty() && it.codePointCount(0, it.length) == 1 }
        .map { it.codePointAt(0) }
        .toSet()

// The phonemes of each word of the lexicon, from the lines of its lexicon-us-en.txt: a word and its tokens, separated by
// white space. The word is lowercased, of two lines with the same word the first is kept, and a line with no tokens or
// with one that is not a token of the model is left out
fun lexiconOf(lexiconLines: List<String>, tokenCodePoints: Set<Int>): Map<String, IntArray> {
    val lexicon = HashMap<String, IntArray>()
    for (line in lexiconLines) {
        val fields = line.trim().split(Regex("\\s+"))
        if (fields.size < 2) continue
        val phonemeCodePoints = fields.drop(1).map { token ->
            if (token.codePointCount(0, token.length) == 1) token.codePointAt(0) else return@map null
        }
        if (phonemeCodePoints.any { it == null || it !in tokenCodePoints }) continue
        lexicon.putIfAbsent(fields[0].lowercase(Locale.ROOT), phonemeCodePoints.filterNotNull().toIntArray())
    }
    return lexicon
}

// What the Kokoro front end inside sherpa-onnx 1.13.8 did for an English text with a lexicon and no language set, which
// the plugin does itself since 2.0 hands the engine phonemes only: a word of the lexicon takes its phonemes, any other
// word takes those of `phonemesOf`, which gives them as text, and the rows of the result are the sentences, joined
// where they are short. `phonemesOf` is given a word in lowercase
class KokoroFrontEnd(
    private val lexicon: Map<String, IntArray>,
    private val tokenCodePoints: Set<Int>,
    private val phonemesOf: (String) -> String,
) {

    // The rows of tokens for the text of one piece, none longer than MAXIMUM_TOKENS_PER_ROW. A piece that gives no row
    // is not to be handed to the engine
    fun rowsOf(text: String): List<IntArray> = joinedShortRows(sentenceRowsOf(wordsOf(normalized(text))))

    private fun sentenceRowsOf(words: List<String>): List<IntArray> {
        val rows = RowCollector()
        for (word in words) {
            val lowercaseWord = word.lowercase(Locale.ROOT)
            when {
                lowercaseWord in PUNCTUATION_TOKENS -> addPunctuation(rows, lowercaseWord)
                else -> rows.addWord(tokensOfWord(lowercaseWord))
            }
        }
        return rows.finish()
    }

    private fun addPunctuation(rows: RowCollector, punctuation: String) {
        val codePoint = punctuation.codePointAt(0)
        if (codePoint !in tokenCodePoints) return
        rows.addWord(intArrayOf(codePoint), endsTheRow = punctuation in ROW_ENDS)
    }

    // The word and the space after it
    private fun tokensOfWord(word: String): IntArray {
        val phonemes = lexicon[word] ?: phonemesOf(word).codePoints().filter { it in tokenCodePoints }.toArray()
        return phonemes + SPACE
    }
}

private fun normalized(text: String): String =
    REPLACEMENTS.fold(text) { replaced, (from, to) -> replaced.replace(from, to) }.replace(WHITE_SPACE_RUN, " ")

private fun isAsciiWhiteSpace(codePoint: Int) = codePoint == ' '.code || codePoint in 0x09..0x0D

// Not the apostrophe, which stays inside a word
private fun isAsciiPunctuation(codePoint: Int) =
    codePoint < 0x80 && codePoint != '\''.code && (codePoint in 0x21..0x2F || codePoint in 0x3A..0x40 || codePoint in 0x5B..0x60 || codePoint in 0x7B..0x7E)

private fun joinsAWord(codePoint: Int): Boolean =
    codePoint < 0x80 && !isAsciiWhiteSpace(codePoint) && !isAsciiPunctuation(codePoint) || JOINING_RANGES.any { codePoint in it }

// A run of ASCII characters that are neither white space nor punctuation is a word, as are the letters with a diacritic
// inside it. Any other character is a word of its own, and white space is dropped
private fun wordsOf(text: String): List<String> {
    val words = mutableListOf<String>()
    val run = StringBuilder()
    fun endRun() {
        if (run.isNotEmpty()) words += run.toString()
        run.setLength(0)
    }
    text.codePoints().forEach { codePoint ->
        when {
            isAsciiWhiteSpace(codePoint) -> endRun()
            joinsAWord(codePoint) -> run.appendCodePoint(codePoint)
            else -> {
                endRun()
                words += String(Character.toChars(codePoint))
            }
        }
    }
    endRun()
    return words
}

// Collects the tokens of the words into rows, the end of a sentence ending a row and no row holding more than the limit
private class RowCollector {
    private val rows = mutableListOf<IntArray>()
    private var current = ArrayList<Int>()

    fun addWord(tokens: IntArray, endsTheRow: Boolean = false) {
        if (tokens.size > MAXIMUM_TOKENS_PER_ROW) {
            addWordLongerThanARow(tokens)
        } else {
            if (current.size + tokens.size > MAXIMUM_TOKENS_PER_ROW) endRow()
            tokens.forEach { current.add(it) }
        }
        if (endsTheRow) endRow()
    }

    fun finish(): List<IntArray> {
        endRow()
        return rows
    }

    // The word ends the row before it, and its tokens are cut after the last space that keeps a row within the limit,
    // which falls between two words that eSpeak NG reads, and at the limit only where they hold no such space
    private fun addWordLongerThanARow(tokens: IntArray) {
        endRow()
        var start = 0
        while (tokens.size - start > MAXIMUM_TOKENS_PER_ROW) {
            val lastSpace = (start + MAXIMUM_TOKENS_PER_ROW - 1 downTo start).firstOrNull { tokens[it] == SPACE }
            val end = if (lastSpace == null || lastSpace == start) start + MAXIMUM_TOKENS_PER_ROW else lastSpace + 1
            for (index in start until end) current.add(tokens[index])
            endRow()
            start = end
        }
        for (index in start until tokens.size) current.add(tokens[index])
    }

    private fun endRow() {
        if (current.isNotEmpty()) rows += current.toIntArray()
        current = ArrayList()
    }
}

// A row of more than 10 tokens stands alone. One of 10 or fewer goes on the row before it where the two together have fewer
// than 46 tokens or it has 2 or fewer, and where they are not more than a row holds. The first row always stands alone
private fun joinedShortRows(rows: List<IntArray>): List<IntArray> {
    val joined = mutableListOf<IntArray>()
    for (row in rows) {
        val before = joined.lastOrNull()
        val goesOnTheRowBefore = before != null && row.size <= SHORT_ROW_TOKENS &&
            (before.size + row.size < JOINED_ROWS_BELOW_TOKENS || row.size <= TINY_ROW_TOKENS) &&
            before.size + row.size <= MAXIMUM_TOKENS_PER_ROW
        if (goesOnTheRowBefore) joined[joined.lastIndex] = before + row else joined += row
    }
    return joined
}
