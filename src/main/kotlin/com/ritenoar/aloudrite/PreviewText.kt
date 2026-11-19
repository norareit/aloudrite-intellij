package com.ritenoar.aloudrite

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser

// A text node of the preview, numbered in document order, with the span of the file it was made from: from the last
// boundary of an element passed before it to the next one
data class PreviewTextNode(val index: Int, val text: String, val span: SourceRange)

// An element of the preview that carries a range of the file, and the text nodes inside it, from the first to the one
// before `endNode`
data class PreviewElement(val range: SourceRange, val firstNode: Int, val endNode: Int)

// What a page of the preview says of its text after each patch, as the text nodes it holds with the spans of the file they
// were made from, and the elements that carry a range. The version tells one description of a page from the next
class PreviewText(val version: Int, val nodes: List<PreviewTextNode>, val elements: List<PreviewElement>) {

    companion object {

        // The description as the page sends it: {"version": n, "nodes": [["o", start, end], ["t", text], ["c"], ...]}. The
        // page opens an element that carries a range with "o", closes it with "c", and sends each text node with "t". Null
        // where it is not such a description
        fun read(json: String): PreviewText? {
            val message = try {
                JsonParser.parseString(json) as? JsonObject
            } catch (_: JsonParseException) {
                null
            } ?: return null
            val version = message.get("version")?.takeIf(::isNumber)?.asInt ?: return null
            val nodes = message.get("nodes") as? JsonArray ?: return null
            return Builder().takeIf { builder -> nodes.all { builder.add(it) } }?.build(version)
        }

        private fun isNumber(element: JsonElement) = element.isJsonPrimitive && element.asJsonPrimitive.isNumber
        private fun isText(element: JsonElement) = element.isJsonPrimitive && element.asJsonPrimitive.isString
    }

    private class Builder {
        private class Open(val start: Int, val end: Int, val firstNode: Int, val slot: Int)

        private val texts = mutableListOf<String>()
        private val starts = mutableListOf<Int>()
        private val ends = mutableListOf<Int>()
        // In the order the elements were opened, which a slot holds from the opening to the closing
        private val elements = mutableListOf<PreviewElement?>()
        private val open = ArrayDeque<Open>()
        private var boundary = 0

        // The text nodes whose end is the next boundary
        private var waiting = 0

        fun add(node: JsonElement): Boolean {
            val parts = node as? JsonArray ?: return false
            val kind = parts.firstOrNull()?.takeIf(::isText)?.asString ?: return false
            return when {
                kind == "t" && parts.size() == 2 && isText(parts[1]) -> addText(parts[1].asString)
                kind == "o" && parts.size() == 3 && isNumber(parts[1]) && isNumber(parts[2]) ->
                    openElement(parts[1].asInt, parts[2].asInt)
                kind == "c" && parts.size() == 1 -> closeElement()
                else -> false
            }
        }

        private fun addText(text: String): Boolean {
            texts += text
            starts += boundary
            waiting++
            return true
        }

        private fun openElement(start: Int, end: Int): Boolean {
            passBoundary(start)
            open.addLast(Open(start, end, texts.size, elements.size))
            elements += null
            return true
        }

        private fun closeElement(): Boolean {
            val closed = open.removeLastOrNull() ?: return false
            passBoundary(closed.end)
            elements[closed.slot] = PreviewElement(SourceRange(closed.start, closed.end), closed.firstNode, texts.size)
            return true
        }

        private fun passBoundary(offset: Int) {
            repeat(waiting) { ends += offset }
            waiting = 0
            boundary = offset
        }

        // A text node after the last boundary has none to end at, and ends where it starts
        fun build(version: Int): PreviewText? {
            if (open.isNotEmpty()) return null
            val nodes = texts.indices.map { index ->
                PreviewTextNode(index, texts[index], SourceRange(starts[index], ends.getOrElse(index) { starts[index] }))
            }
            return PreviewText(version, nodes, elements.filterNotNull())
        }
    }
}
