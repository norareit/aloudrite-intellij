package com.ritenoar.aloudrite

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

private const val POSITION_ATTRIBUTE = "md-src-pos"
private val SKIPPED_TAGS = setOf("script", "style", "template")

// The HTML that the Markdown library of the platform makes of a text, with the ranges of the file on its elements as the
// preview of the Markdown plugin has them
fun htmlOf(markdown: String): String {
    val flavour = GFMFlavourDescriptor()
    val tree = MarkdownParser(flavour).buildMarkdownTreeFromString(markdown)
    return HtmlGenerator(markdown, tree, flavour, true).generateHtml()
}

// What the script of the page would send of the page of this text, walking the body by the same rule: an element with a range
// opens and closes, any other is passed through, and every text node is sent, white space only ones included
fun describedPage(markdown: String, version: Int = 1): PreviewText = PreviewText.read(descriptionOfPage(markdown, version))!!

// The message itself, as the page sends it
fun descriptionOfPage(markdown: String, version: Int = 1): String {
    val nodes = JsonArray()
    describe(Jsoup.parse(htmlOf(markdown)).body(), nodes)
    return JsonObject().apply {
        addProperty("version", version)
        add("nodes", nodes)
    }.toString()
}

private fun describe(node: Node, into: JsonArray) {
    if (node is TextNode) {
        into.add(JsonArray().apply { add("t"); add(node.wholeText) })
        return
    }
    if (node !is Element || node.normalName() in SKIPPED_TAGS) return
    val position = node.attr(POSITION_ATTRIBUTE).takeIf { it.isNotEmpty() }
    // The body is the page itself and carries none
    if (position == null || node.normalName() == "body") {
        node.childNodes().forEach { describe(it, into) }
        return
    }
    val (start, end) = position.split("..").map(String::toInt)
    into.add(JsonArray().apply { add("o"); add(start); add(end) })
    node.childNodes().forEach { describe(it, into) }
    into.add(JsonArray().apply { add("c") })
}
