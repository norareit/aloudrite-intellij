package com.ritenoar.aloudrite.preview

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.service
import com.ritenoar.aloudrite.PreviewPage
import com.ritenoar.aloudrite.PreviewPages
import org.intellij.plugins.markdown.extensions.MarkdownBrowserPreviewExtension
import org.intellij.plugins.markdown.ui.preview.BrowserPipe
import org.intellij.plugins.markdown.ui.preview.MarkdownHtmlPanel
import org.intellij.plugins.markdown.ui.preview.ResourceProvider

internal const val SCRIPT_NAME = "aloudrite-read-along.js"
private const val SCRIPT_RESOURCE = "/com/ritenoar/aloudrite/preview/$SCRIPT_NAME"

// The tags of the messages between the IDE and the script of the page
internal const val PAGE_TAG = "aloudrite.page"
internal const val NO_RANGES_TAG = "aloudrite.noRanges"
internal const val PLACES_TAG = "aloudrite.places"

// Gives each preview panel of the Markdown plugin the script that draws the highlights. The panel asks each time it loads
// its extensions, and the provider is registered in the descriptor, since the panel does not watch the extension point
class ReadAlongPreviewProvider : MarkdownBrowserPreviewExtension.Provider {
    override fun createBrowserExtension(panel: MarkdownHtmlPanel): MarkdownBrowserPreviewExtension =
        ReadAlongPreviewExtension(panel, service<PreviewPages>())
}

// The preview of one panel. Where the panel gives no pipe or no file there is nothing to send the places through or to,
// so it gives no script, and the preview goes on as it is
internal class ReadAlongPreviewExtension(panel: MarkdownHtmlPanel, private val pages: PreviewPages) : MarkdownBrowserPreviewExtension {

    private val pipe: BrowserPipe? = panel.browserPipe
    private val pageHandler = HandOverToTheEventThread { page?.described(it) }
    private val noRangesHandler = HandOverToTheEventThread {
        pages.noHighlightsInThePreview("its page holds no ranges of the file")
    }

    private val page: PreviewPage?

    init {
        val file = panel.virtualFile
        if (pipe == null) {
            pages.noHighlightsInThePreview("a panel of the preview gives no pipe to the page")
        }
        page = if (pipe != null && file != null) PreviewPage(file) { pipe.send(PLACES_TAG, it) } else null
        if (pipe != null && page != null) {
            pages.register(page, this)
            pipe.subscribe(PAGE_TAG, pageHandler)
            pipe.subscribe(NO_RANGES_TAG, noRangesHandler)
        }
    }

    override val scripts: List<String> = if (page != null) listOf(SCRIPT_NAME) else emptyList()

    override val resourceProvider: ResourceProvider = object : ResourceProvider {
        override fun canProvide(resourceName: String): Boolean = resourceName == SCRIPT_NAME

        override fun loadResource(resourceName: String): ResourceProvider.Resource? {
            if (!canProvide(resourceName)) return null
            val bytes = ReadAlongPreviewExtension::class.java.getResourceAsStream(SCRIPT_RESOURCE)?.use { it.readBytes() } ?: return null
            return ResourceProvider.Resource(bytes, "text/javascript")
        }
    }

    override fun dispose() {
        if (page == null) return
        pipe?.removeSubscription(PAGE_TAG, pageHandler)
        pipe?.removeSubscription(NO_RANGES_TAG, noRangesHandler)
    }

    // The pipe calls its handlers on a thread of the browser, and the pages are for the event thread
    private class HandOverToTheEventThread(private val handle: (String) -> Unit) : BrowserPipe.Handler {
        override fun processMessageReceived(data: String): Boolean {
            ApplicationManager.getApplication().invokeLater({ handle(data) }, ModalityState.any())
            return true
        }
    }
}
