package com.ritenoar.aloudrite

// What the model takes in one run. The engine adds the padding token at both ends of a row and cuts a row after 401 tokens
// wherever that falls, also in the middle of a word, so no row of the plugin is longer than this
const val MAXIMUM_TOKENS_PER_ROW = 400

// A row of this many tokens or fewer is joined to the row before it, as the Kokoro front end of sherpa-onnx 1.13.8 did
private const val SHORT_ROW_TOKENS = 10
private const val TINY_ROW_TOKENS = 2
private const val JOINED_ROWS_BELOW_TOKENS = 46

private val SPACE = ' '.code
private val FULL_STOP = '.'.code

// The replacements that 1.13.8 made before it gave the text to eSpeak NG, in this order, and the runs of white space as
// one space
private val REPLACEMENTS = listOf(
    "，" to ",", ":" to ",", "、" to ",", "；" to ";", "：" to ":", "。" to ".", "？" to "?", "！" to "!",
)
private val WHITE_SPACE_RUN = Regex("\\s+")

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

// What sherpa-onnx 1.13.8 did for an English text as the plugin configured it, which the plugin does itself since 2.0
// hands the engine phonemes only: the text goes to eSpeak NG whole, each sentence it returns is a row of tokens, and short
// rows are joined to the row before. `sentencesOf` gives the phonemes of each sentence of a text as code points
class PhonemeRows(
    private val tokenCodePoints: Set<Int>,
    private val sentencesOf: (String) -> List<IntArray>,
) {

    // The rows of tokens for the text of one piece, none longer than MAXIMUM_TOKENS_PER_ROW. A piece that gives no row is
    // not to be handed to the engine
    fun rowsOf(text: String): List<IntArray> {
        val normalizedText = normalized(text)
        if (normalizedText.isBlank()) return emptyList()
        val sentences = sentencesOf(normalizedText)
            .map { sentence -> withSpacesAfterFullStops(sentence.filter { it in tokenCodePoints }) }
            .filter { it.isNotEmpty() }
        return joinedShortRows(sentences.flatMap { cutToTheLimit(it) })
    }
}

// 1.13.8 added the space token after each full stop token of the phonemes, before it counted, cut and joined the rows, so
// that the tokens of a row are those of the rows it counted
private fun withSpacesAfterFullStops(tokens: List<Int>): IntArray =
    tokens.flatMap { if (it == FULL_STOP) listOf(it, SPACE) else listOf(it) }.toIntArray()

private fun normalized(text: String): String =
    REPLACEMENTS.fold(text) { replaced, (from, to) -> replaced.replace(from, to) }.replace(WHITE_SPACE_RUN, " ")

// A sentence over the limit is cut after the last space that keeps a row within it, which falls between two words that
// eSpeak NG reads, and at the limit only where its first 400 tokens hold no such space
private fun cutToTheLimit(sentence: IntArray): List<IntArray> {
    val rows = mutableListOf<IntArray>()
    var start = 0
    while (sentence.size - start > MAXIMUM_TOKENS_PER_ROW) {
        val lastSpace = (start + MAXIMUM_TOKENS_PER_ROW - 1 downTo start + 1).firstOrNull { sentence[it] == SPACE }
        val end = if (lastSpace == null) start + MAXIMUM_TOKENS_PER_ROW else lastSpace + 1
        rows += sentence.copyOfRange(start, end)
        start = end
    }
    rows += sentence.copyOfRange(start, sentence.size)
    return rows
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
