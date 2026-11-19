package com.ritenoar.aloudrite

// The edits made to a text from a moment on, which map a range of the text of that moment to the text now, as a range
// marker that is not greedy moves with the edits around it
class TextEdits {

    private class Edit(val offset: Int, val oldLength: Int, val newLength: Int)

    private val edits = mutableListOf<Edit>()

    fun record(offset: Int, oldLength: Int, newLength: Int) {
        edits += Edit(offset, oldLength, newLength)
    }

    // Null where every character of the range was deleted or replaced
    fun map(range: SourceRange): SourceRange? {
        var start = range.start
        var end = range.end
        for (edit in edits) {
            start = startAfter(edit, start)
            end = endAfter(edit, end)
            if (start >= end) return null
        }
        return SourceRange(start, end)
    }

    private fun startAfter(edit: Edit, start: Int): Int {
        val oldEnd = edit.offset + edit.oldLength
        return when {
            start < edit.offset -> start
            start >= oldEnd -> start + edit.newLength - edit.oldLength
            else -> edit.offset + edit.newLength
        }
    }

    private fun endAfter(edit: Edit, end: Int): Int {
        val oldEnd = edit.offset + edit.oldLength
        return when {
            end <= edit.offset -> end
            end >= oldEnd -> end + edit.newLength - edit.oldLength
            else -> edit.offset
        }
    }
}
