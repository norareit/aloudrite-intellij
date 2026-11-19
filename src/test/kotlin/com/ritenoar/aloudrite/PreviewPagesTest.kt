package com.ritenoar.aloudrite

import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.LightVirtualFile
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PreviewPagesTest {

    private val logged = mutableListOf<String>()
    private val pages = PreviewPages { logged += it }
    private val whileRegistered = Disposer.newDisposable("PreviewPagesTest")

    private val file = LightVirtualFile("a.md")
    private val otherFile = LightVirtualFile("b.md")

    @After
    fun dispose() = Disposer.dispose(whileRegistered)

    private fun description(version: Int) = """{"version": $version, "nodes": [["o",0,5],["t","hello"],["c"]]}"""

    @Test
    fun `a page registered for a file is among the pages of that file and not of another`() {
        val page = PreviewPage(file) { }
        pages.register(page, whileRegistered)

        assertEquals(listOf(page), pages.pagesOf(file))
        assertEquals(emptyList<PreviewPage>(), pages.pagesOf(otherFile))
    }

    @Test
    fun `a page is gone once the disposable it was registered with is disposed`() {
        val page = PreviewPage(file) { }
        val registered = Disposer.newDisposable("registered")
        pages.register(page, registered)

        Disposer.dispose(registered)

        assertEquals(emptyList<PreviewPage>(), pages.pagesOf(file))
    }

    @Test
    fun `a page that receives a description keeps it, and one that cannot be read leaves the last`() {
        val page = PreviewPage(file) { }
        assertNull(page.text)

        page.described(description(3))
        assertEquals(3, page.text!!.version)

        page.described("not a description")
        assertEquals(3, page.text!!.version)
    }

    @Test
    fun `a page sends a message only when it differs from the last one sent, and again after a new description`() {
        val sent = mutableListOf<String>()
        val page = PreviewPage(file) { sent += it }
        page.described(description(1))

        page.send("one")
        page.send("one")
        page.send("two")
        page.send("two")
        assertEquals(listOf("one", "two"), sent)

        page.described(description(2))
        page.send("two")
        assertEquals(listOf("one", "two", "two"), sent)
    }

    @Test
    fun `the places of a page are worked out once for a description and again for the next`() {
        val page = PreviewPage(file) { }
        assertNull(page.places { "hello" })

        page.described(description(1))
        val first = page.places { "hello" }!!
        assertTrue(first === page.places { "hello" })

        page.described(description(2))
        assertTrue(first !== page.places { "hello" })
    }

    @Test
    fun `the line is written once whatever the reasons and however often they come`() {
        pages.noHighlightsInThePreview("the Markdown plugin is missing")
        pages.noHighlightsInThePreview("a panel gives no pipe")
        pages.noHighlightsInThePreview("the Markdown plugin is missing")

        assertEquals(listOf("AloudRite: the preview shows no highlights, as the Markdown plugin is missing"), logged)
    }

    @Test
    fun `a second instance writes its own line`() {
        pages.noHighlightsInThePreview("a reason")
        val others = mutableListOf<String>()
        PreviewPages { others += it }.noHighlightsInThePreview("a reason")

        assertEquals(1, others.size)
    }
}
