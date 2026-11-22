package com.ritenoar.aloudrite

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.ritenoar.aloudrite.core.SourceRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.Color

class PreviewMessageTest {

    private fun parsed(json: String): JsonObject = JsonParser.parseString(json).asJsonObject

    @Test
    fun `the places message holds the version, the number and resumes of the reading, both colours, the pieces and no element`() {
        val message = parsed(
            PreviewMessage.places(
                version = 4,
                reading = 7,
                resumes = 2,
                sentenceColour = "rgba(1, 2, 3, 0.5)",
                wordColour = "rgba(4, 5, 6, 1)",
                sentence = listOf(TextPiece(0, 0, 4), TextPiece(1, 0, 3)),
                word = WordPlace(listOf(TextPiece(1, 1, 2)), null),
            ),
        )

        assertEquals(4, message["version"].asInt)
        assertEquals(7, message["reading"].asInt)
        assertEquals(2, message["resumes"].asInt)
        assertEquals("rgba(1, 2, 3, 0.5)", message["sentenceColour"].asString)
        assertEquals("rgba(4, 5, 6, 1)", message["wordColour"].asString)
        assertEquals("[[0,0,4],[1,0,3]]", message["sentence"].toString())
        assertEquals("[[1,1,2]]", message["word"].toString())
        assertTrue(message["wordElement"].isJsonNull)
    }

    @Test
    fun `the element to outline is given by its range`() {
        val message = parsed(
            PreviewMessage.places(1, 1, 0, null, null, emptyList(), WordPlace(emptyList(), SourceRange(110, 126))),
        )

        assertEquals("[110,126]", message["wordElement"].toString())
    }

    @Test
    fun `a missing colour is null`() {
        val message = parsed(PreviewMessage.places(1, 1, 0, null, null, emptyList(), WordPlace(emptyList(), null)))

        assertTrue(message["sentenceColour"].isJsonNull)
        assertTrue(message["wordColour"].isJsonNull)
    }

    @Test
    fun `a colour with an alpha is written as rgba with the alpha as a share of 1`() {
        assertEquals("rgba(10, 20, 30, 1)", PreviewMessage.cssColour(Color(10, 20, 30, 255)))
        assertEquals("rgba(10, 20, 30, 0.2)", PreviewMessage.cssColour(Color(10, 20, 30, 51)))
        assertEquals("rgba(10, 20, 30, 0)", PreviewMessage.cssColour(Color(10, 20, 30, 0)))
    }

    @Test
    fun `no colour is no css colour`() {
        assertNull(PreviewMessage.cssColour(null))
    }

    @Test
    fun `the clearing message is the clearing alone`() {
        assertEquals("""{"clear":true}""", PreviewMessage.clear())
    }
}
