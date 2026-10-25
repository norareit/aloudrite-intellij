package com.ritenoar.aloudrite

// How many seconds of audio the engine may make ahead of the playing
internal const val SECONDS_AHEAD = 30

// A segment is one piece of words, as the plugin cuts the text, or one silence. A resume and a making begin at a segment
internal fun segmentsOf(part: SpokenPart): List<SpokenPart> = when (part) {
    is Words -> piecesOf(part.text).filterNot { it.isBlank() }.map(::Words)
    is Silence -> listOf(part)
}
