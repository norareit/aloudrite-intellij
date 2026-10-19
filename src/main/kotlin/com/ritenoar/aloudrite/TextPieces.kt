package com.ritenoar.aloudrite

// One call of the engine with more than about 1,500 characters ends the process, see the plan of unit 0006. A piece
// of this size stays far below that, also for characters of four bytes, and keeps a stop waiting for little
const val MAXIMUM_PIECE_BYTES = 400

private const val SENTENCE_ENDS = ".!?"
private const val SENTENCE_CLOSERS = "\"')]*"
private const val CLAUSE_ENDS = ",:;"

// The engine makes a whole piece before any of it plays, so a long sentence after a short one left a silence of
// seconds at the start of a reading. Ending a piece at a clause as well lets the engine finish its first part sooner.
// The minimum keeps a list of short words from becoming a piece per word
private const val MINIMUM_CLAUSE_BYTES = 60

// Cuts the text where the engine may pause anyway: after a sentence, after a long enough clause and after a block. The
// pieces joined give the text exactly, and a piece may be white space only
fun piecesOf(text: String): List<String> =
    piecesAtSentencesAndBlocks(text).flatMap { piecesWithinTheLimit(it) }

private fun piecesAtSentencesAndBlocks(text: String): List<String> {
    val pieces = mutableListOf<String>()
    var pieceStart = 0
    var index = 0
    while (index < text.length) {
        if (!text[index].isWhitespace()) {
            index++
            continue
        }
        val whiteSpaceStart = index
        while (index < text.length && text[index].isWhitespace()) index++
        val whiteSpace = text.substring(whiteSpaceStart, index)
        if (endsASentence(text, whiteSpaceStart) ||
            endsALongEnoughClause(text, pieceStart, whiteSpaceStart) ||
            holdsABlankLine(whiteSpace)
        ) {
            pieces += text.substring(pieceStart, index)
            pieceStart = index
        }
    }
    if (pieceStart < text.length) pieces += text.substring(pieceStart)
    return pieces
}

// Looks past the marks that close a sentence, such as a quote or the asterisks of bold
fun endsWithASentenceEnd(text: String): Boolean = endsASentence(text, text.length)

private fun endsASentence(text: String, whiteSpaceStart: Int): Boolean {
    val index = lastBeforeTheClosers(text, whiteSpaceStart)
    return index >= 0 && text[index] in SENTENCE_ENDS
}

private fun endsALongEnoughClause(text: String, pieceStart: Int, whiteSpaceStart: Int): Boolean {
    val index = lastBeforeTheClosers(text, whiteSpaceStart)
    if (index < 0 || text[index] !in CLAUSE_ENDS) return false
    return utf8Length(text, pieceStart, whiteSpaceStart) >= MINIMUM_CLAUSE_BYTES
}

private fun lastBeforeTheClosers(text: String, whiteSpaceStart: Int): Int {
    var index = whiteSpaceStart - 1
    while (index >= 0 && text[index] in SENTENCE_CLOSERS) index--
    return index
}

// A carriage return counts as part of a line break, so that a file read from disk with Windows line ends is cut alike
private fun holdsABlankLine(whiteSpace: String): Boolean {
    var lineBreaks = 0
    for (character in whiteSpace) {
        when (character) {
            '\n' -> if (++lineBreaks == 2) return true
            ' ', '\t', '\r' -> Unit
            else -> lineBreaks = 0
        }
    }
    return false
}

private fun piecesWithinTheLimit(piece: String): List<String> {
    val pieces = mutableListOf<String>()
    var start = 0
    while (start < piece.length) {
        val limit = endWithinTheLimit(piece, start)
        if (limit == piece.length) {
            pieces += piece.substring(start)
            break
        }
        val lastWhiteSpace = (limit - 1 downTo start).firstOrNull { piece[it].isWhitespace() }
        val cut = if (lastWhiteSpace != null) lastWhiteSpace + 1 else limit
        pieces += piece.substring(start, cut)
        start = cut
    }
    return pieces
}

// Steps by code point, so that the end never falls inside a surrogate pair
private fun endWithinTheLimit(text: String, start: Int): Int {
    var bytes = 0
    var index = start
    while (index < text.length) {
        val codePoint = text.codePointAt(index)
        bytes += utf8Length(codePoint)
        if (bytes > MAXIMUM_PIECE_BYTES) break
        index += Character.charCount(codePoint)
    }
    return index
}

private fun utf8Length(text: String, start: Int, end: Int): Int {
    var bytes = 0
    var index = start
    while (index < end) {
        val codePoint = text.codePointAt(index)
        bytes += utf8Length(codePoint)
        index += Character.charCount(codePoint)
    }
    return bytes
}

private fun utf8Length(codePoint: Int): Int = when {
    codePoint < 0x80 -> 1
    codePoint < 0x800 -> 2
    codePoint < 0x10000 -> 3
    else -> 4
}
