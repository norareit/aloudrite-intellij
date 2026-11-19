package com.ritenoar.aloudrite

import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import java.awt.Color
import java.util.Locale

// The messages that the IDE sends to a page of the preview, as the JSON that the script of the page reads
object PreviewMessage {

    // What to draw: the pieces of the text of the page that the sentence and the word stand for, or the element of the
    // word where no text shows it. The version is that of the description the pieces were found in, so that the page can
    // tell pieces of a text it no longer has
    fun places(
        version: Int,
        reading: Int,
        resumes: Int,
        sentenceColour: String?,
        wordColour: String?,
        sentence: List<TextPiece>,
        word: WordPlace,
    ): String = JsonObject().apply {
        addProperty("version", version)
        addProperty("reading", reading)
        addProperty("resumes", resumes)
        addProperty("sentenceColour", sentenceColour)
        addProperty("wordColour", wordColour)
        add("sentence", piecesAsJson(sentence))
        add("word", piecesAsJson(word.pieces))
        add("wordElement", word.outlinedElement?.let { JsonArray().apply { add(it.start); add(it.end) } } ?: JsonNull.INSTANCE)
    }.toString()

    // Applies to a description of any version
    fun clear(): String = JsonObject().apply { addProperty("clear", true) }.toString()

    // As the page takes a colour, with the alpha as a share of 1
    fun cssColour(colour: Color?): String? {
        colour ?: return null
        val alpha = String.format(Locale.ROOT, "%.3f", colour.alpha / 255.0).trimEnd('0').trimEnd('.')
        return "rgba(${colour.red}, ${colour.green}, ${colour.blue}, $alpha)"
    }

    private fun piecesAsJson(pieces: List<TextPiece>): JsonArray = JsonArray().apply {
        pieces.forEach { piece -> add(JsonArray().apply { add(piece.node); add(piece.start); add(piece.end) }) }
    }
}
