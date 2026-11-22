package com.ritenoar.aloudrite

import com.ritenoar.aloudrite.core.SourceRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PreviewTextTest {

    private fun read(nodes: String, version: Int = 1): PreviewText =
        PreviewText.read("""{"version": $version, "nodes": $nodes}""")!!

    @Test
    fun `a paragraph with bold text gives three text nodes with the spans around the bold`() {
        // "One **two** three." as paragraph 0..18 with the bold at 4..11
        val text = read("""[["o",0,18],["t","One "],["o",4,11],["t","two"],["c"],["t"," three."],["c"]]""")

        assertEquals(listOf("One ", "two", " three."), text.nodes.map { it.text })
        assertEquals(listOf(SourceRange(0, 4), SourceRange(4, 11), SourceRange(11, 18)), text.nodes.map { it.span })
    }

    @Test
    fun `a ranged element knows its first text node and the one after its last`() {
        val text = read("""[["o",0,18],["t","One "],["o",4,11],["t","two"],["c"],["t"," three."],["c"]]""")

        assertEquals(
            listOf(PreviewElement(SourceRange(0, 18), 0, 3), PreviewElement(SourceRange(4, 11), 1, 2)),
            text.elements,
        )
    }

    @Test
    fun `an element without a range is passed through and its text takes the spans of its ranged parent`() {
        // The description holds no element of its own for it, so its text nodes follow one another
        val text = read("""[["o",10,20],["t","a"],["t","b"],["c"]]""")

        assertEquals(listOf(SourceRange(10, 20), SourceRange(10, 20)), text.nodes.map { it.span })
    }

    @Test
    fun `a text node before any boundary starts at 0 and one after the last ends at its start`() {
        val text = read("""[["t","before"],["o",5,9],["t","in"],["c"],["t","after"]]""")

        assertEquals(SourceRange(0, 5), text.nodes[0].span)
        assertEquals(SourceRange(9, 9), text.nodes[2].span)
    }

    @Test
    fun `an element without text knows an empty run of text nodes`() {
        val text = read("""[["t","a"],["o",3,8],["c"],["t","b"]]""")

        assertEquals(listOf(PreviewElement(SourceRange(3, 8), 1, 1)), text.elements)
    }

    @Test
    fun `text nodes of white space only are counted`() {
        val text = read("""[["o",0,9],["t","a"],["t","\n  "],["o",4,9],["t","b"],["c"],["c"]]""")

        assertEquals(listOf("a", "\n  ", "b"), text.nodes.map { it.text })
        assertEquals(listOf(0, 1, 2), text.nodes.map { it.index })
    }

    @Test
    fun `the version is kept`() {
        assertEquals(7, read("[]", version = 7).version)
    }

    @Test
    fun `a description that is not JSON gives null`() {
        assertNull(PreviewText.read("not json"))
    }

    @Test
    fun `nodes that are not of the three kinds give null`() {
        assertNull(PreviewText.read("""{"version": 1, "nodes": [["x", 1]]}"""))
        assertNull(PreviewText.read("""{"version": 1, "nodes": [["o", "a", 2]]}"""))
        assertNull(PreviewText.read("""{"version": 1, "nodes": [["t", 5]]}"""))
    }

    @Test
    fun `a description without a version or nodes, or with a closing that has no opening, gives null`() {
        assertNull(PreviewText.read("""{"nodes": []}"""))
        assertNull(PreviewText.read("""{"version": 1}"""))
        assertNull(PreviewText.read("""{"version": 1, "nodes": [["c"]]}"""))
        assertNull(PreviewText.read("""{"version": 1, "nodes": [["o",0,1]]}"""))
    }
}
