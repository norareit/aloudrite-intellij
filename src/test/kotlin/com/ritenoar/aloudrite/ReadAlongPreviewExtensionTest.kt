package com.ritenoar.aloudrite

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.ApplicationRule
import com.intellij.testFramework.LightVirtualFile
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.runInEdtAndWait
import com.ritenoar.aloudrite.preview.ReadAlongPreviewExtension
import com.ritenoar.aloudrite.preview.ReadAlongPreviewProvider
import org.intellij.plugins.markdown.extensions.MarkdownBrowserPreviewExtension
import org.intellij.plugins.markdown.ui.preview.BrowserPipe
import org.intellij.plugins.markdown.ui.preview.MarkdownHtmlPanel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.ClassRule
import org.junit.Test
import java.lang.reflect.Proxy
import java.nio.file.Files
import java.nio.file.Path
import kotlin.concurrent.thread

// The Markdown side, on a panel and a pipe of the test's own. The panel is a proxy that answers the two questions the
// extension asks of it, since the panel of the Markdown plugin holds a browser
class ReadAlongPreviewExtensionTest {

    private class PipeOfATest : BrowserPipe {
        val sent = mutableListOf<Pair<String, String>>()
        val subscriptions = mutableMapOf<String, MutableList<BrowserPipe.Handler>>()
        override fun send(tag: String, data: String) {
            sent += tag to data
        }

        override fun subscribe(tag: String, handler: BrowserPipe.Handler) {
            subscriptions.getOrPut(tag) { mutableListOf() } += handler
        }

        override fun removeSubscription(tag: String, handler: BrowserPipe.Handler) {
            subscriptions[tag]?.remove(handler)
        }

        override fun dispose() = Unit

        fun post(tag: String, data: String) = subscriptions[tag].orEmpty().toList().forEach { it.processMessageReceived(data) }
        fun subscribed(tag: String) = subscriptions[tag].orEmpty().size
    }

    private fun panelOf(file: VirtualFile?, pipe: BrowserPipe?): MarkdownHtmlPanel =
        Proxy.newProxyInstance(javaClass.classLoader, arrayOf(MarkdownHtmlPanel::class.java)) { _, method, _ ->
            when (method.name) {
                "getBrowserPipe" -> pipe
                "getVirtualFile" -> file
                else -> null
            }
        } as MarkdownHtmlPanel

    private val logged = mutableListOf<String>()
    private val pages = PreviewPages { logged += it }
    private val file = LightVirtualFile("a.md")
    private val provided = mutableListOf<MarkdownBrowserPreviewExtension>()

    private fun extensionOf(panel: MarkdownHtmlPanel): MarkdownBrowserPreviewExtension =
        ReadAlongPreviewExtension(panel, pages).also { provided += it }

    @After
    fun disposeTheExtensions() = provided.forEach(Disposer::dispose)

    private fun dispatchEvents() = runInEdtAndWait { PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue() }

    @Test
    fun theExtensionOfAPanelWithAPipeGivesTheScriptAndItsResourceProviderServesTheBytesOfTheFileAsJavaScript() {
        val extension = extensionOf(panelOf(file, PipeOfATest()))

        assertEquals(listOf("aloudrite-read-along.js"), extension.scripts)
        val provider = extension.resourceProvider
        assertTrue(provider.canProvide("aloudrite-read-along.js"))
        assertFalse(provider.canProvide("other.js"))
        val resource = provider.loadResource("aloudrite-read-along.js")!!
        assertEquals("text/javascript", resource.type)
        assertEquals(
            String(javaClass.getResourceAsStream("/com/ritenoar/aloudrite/preview/aloudrite-read-along.js")!!.readBytes()),
            String(resource.content),
        )
        assertNull(provider.loadResource("other.js"))
    }

    @Test
    fun theExtensionSubscribesToThePageAndTheNoRangesMessagesAndADescriptionPostedFromAnotherThreadReachesThePageOnTheEventThread() {
        val pipe = PipeOfATest()
        extensionOf(panelOf(file, pipe))
        assertEquals(1, pipe.subscribed("aloudrite.page"))
        assertEquals(1, pipe.subscribed("aloudrite.noRanges"))

        thread { pipe.post("aloudrite.page", """{"version": 3, "nodes": [["t","hello"]]}""") }.join()
        dispatchEvents()

        assertEquals(3, pages.pagesOf(file).single().text!!.version)
    }

    @Test
    fun aMessageSentToThePageGoesOutThroughThePipeAsPlaces() {
        val pipe = PipeOfATest()
        extensionOf(panelOf(file, pipe))

        pages.pagesOf(file).single().send("""{"clear":true}""")

        assertEquals(listOf("aloudrite.places" to """{"clear":true}"""), pipe.sent)
    }

    @Test
    fun theNoRangesMessageWritesTheLineToTheLog() {
        val pipe = PipeOfATest()
        extensionOf(panelOf(file, pipe))

        pipe.post("aloudrite.noRanges", "")
        dispatchEvents()

        assertEquals(1, logged.size)
        assertTrue(logged.single(), logged.single().startsWith("AloudRite: the preview shows no highlights, as "))
    }

    @Test
    fun disposingTheExtensionRemovesThePageAndBothSubscriptions() {
        val pipe = PipeOfATest()
        val extension = extensionOf(panelOf(file, pipe))

        Disposer.dispose(extension)

        assertEquals(emptyList<PreviewPage>(), pages.pagesOf(file))
        assertEquals(0, pipe.subscribed("aloudrite.page"))
        assertEquals(0, pipe.subscribed("aloudrite.noRanges"))
    }

    @Test
    fun aPanelWithoutAPipeGivesAnExtensionWithNoScriptRegistersNoPageThrowsNothingAndWritesTheLine() {
        val extension = extensionOf(panelOf(file, null))

        assertEquals(emptyList<String>(), extension.scripts)
        assertEquals(emptyList<PreviewPage>(), pages.pagesOf(file))
        assertEquals(1, logged.size)
        Disposer.dispose(extension)
    }

    @Test
    fun aPanelWithoutAFileRegistersNoPage() {
        val extension = extensionOf(panelOf(null, PipeOfATest()))

        assertEquals(emptyList<String>(), extension.scripts)
        assertEquals(emptyList<PreviewPage>(), pages.pagesOf(file))
    }

    @Test
    fun theProviderMakesTheExtensionOfAPanelThroughThePagesOfTheApplication() {
        val extension = ReadAlongPreviewProvider().createBrowserExtension(panelOf(file, PipeOfATest()))
        provided += extension

        assertEquals(listOf("aloudrite-read-along.js"), extension.scripts)
    }

    @Test
    fun theDescriptorOfThePluginDependsOnTheMarkdownPluginOptionallyAndTheProviderIsAmongItsExtensions() {
        val plugin = PluginManagerCore.getPlugin(PluginId.getId("com.ritenoar.aloudrite"))!!

        val markdown = plugin.dependencies.single { it.pluginId.idString == "org.intellij.plugins.markdown" }

        assertTrue(markdown.isOptional)
        val providers = ApplicationManager.getApplication().extensionArea
            .getExtensionPoint<Any>("org.intellij.markdown.browserPreviewExtensionProvider").extensionList
        assertTrue(providers.any { it is ReadAlongPreviewProvider })
    }

    @Test
    fun noSourceFileOutsideThePreviewPackageNamesTheMarkdownPluginAndThePluginDescriptorDoesNotNameThePreviewPackage() {
        val sources = Path.of("src/main/kotlin")
        val outside = Files.walk(sources).use { paths ->
            paths.filter { it.toString().endsWith(".kt") && !it.toString().contains("/com/ritenoar/aloudrite/preview/") }.toList()
        }

        assertTrue(outside.isNotEmpty())
        assertEquals(emptyList<Path>(), outside.filter { Files.readString(it).contains("org.intellij.plugins.markdown") })
        assertFalse(Files.readString(Path.of("src/main/resources/META-INF/plugin.xml")).contains("aloudrite.preview"))
    }

    companion object {
        @JvmField
        @ClassRule
        val application = ApplicationRule()
    }
}
