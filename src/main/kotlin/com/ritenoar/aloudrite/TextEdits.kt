package com.ritenoar.aloudrite

// The edits made to a text from a moment on, which map a range of the text of that moment to the text now, as a range
// marker that is not greedy moves with the edits around it. The characters of the range that were there at the start are
// followed one by one, so that text inserted among them is no reason to keep the range once all of them are gone
class TextEdits {

    private class Edit(val offset: Int, val oldLength: Int, val newLength: Int)

    private val edits = mutableListOf<Edit>()

    fun record(offset: Int, oldLength: Int, newLength: Int) {
        edits += Edit(offset, oldLength, newLength)
    }

    // The range from the first to the last of the original characters that are left, with what was inserted between them,
    // and null where none is left
    fun map(range: SourceRange): SourceRange? {
        var surviving = listOf(range)
        for (edit in edits) {
            surviving = surviving.flatMap { piecesAfter(edit, it) }
            if (surviving.isEmpty()) return null
        }
        return SourceRange(surviving.minOf { it.start }, surviving.maxOf { it.end })
    }

    // What is left of a run of original characters after an edit: what lies before the replaced text stays, and what lies
    // after it shifts by the change in length. Text inserted inside the run splits it
    private fun piecesAfter(edit: Edit, piece: SourceRange): List<SourceRange> {
        val oldEnd = edit.offset + edit.oldLength
        val before = SourceRange(piece.start, minOf(piece.end, edit.offset))
        val shift = edit.newLength - edit.oldLength
        val after = SourceRange(maxOf(piece.start, oldEnd) + shift, piece.end + shift)
        return listOf(before, after).filter { it.start < it.end }
    }
}
