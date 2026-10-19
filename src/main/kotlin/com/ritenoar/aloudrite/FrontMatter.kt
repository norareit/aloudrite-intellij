package com.ritenoar.aloudrite

private const val OPENING_LINE = "---"
private val CLOSING_LINES = setOf("---", "...")

// Front matter is a block between a line of --- as the first line and the next line of --- or ..., both lines
// included. Whatever stands between them is left out, not only lines of YAML keys. Without such an end, the text is
// left as it is, so that a file that begins with a horizontal rule keeps it
fun withoutFrontMatter(markdown: String): String {
    val firstLineEnd = lineEnd(markdown, 0)
    if (markdown.substring(0, firstLineEnd).trimEnd() != OPENING_LINE) return markdown
    var lineStart = nextLineStart(markdown, firstLineEnd) ?: return markdown
    while (true) {
        val end = lineEnd(markdown, lineStart)
        val following = nextLineStart(markdown, end)
        if (markdown.substring(lineStart, end).trimEnd() in CLOSING_LINES) return markdown.substring(following ?: end)
        lineStart = following ?: return markdown
    }
}

private fun lineEnd(text: String, lineStart: Int): Int = text.indexOf('\n', lineStart).let { if (it < 0) text.length else it }

private fun nextLineStart(text: String, lineEnd: Int): Int? = if (lineEnd < text.length) lineEnd + 1 else null
