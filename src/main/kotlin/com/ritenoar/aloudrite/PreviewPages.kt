package com.ritenoar.aloudrite

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

// One page of a preview of a file, as the IDE sees it: the last description the page sent of its text, and a way to send it
// a message. It knows nothing of the Markdown plugin or of the browser, and is used from the event thread
class PreviewPage(val file: VirtualFile, private val deliver: (String) -> Unit) {

    var text: PreviewText? = null
        private set

    private var places: PreviewPlaces? = null
    private var lastSent: String? = null

    // A description that cannot be read leaves the last one. A new one is a new text, so what was sent is to be sent again
    fun described(json: String) {
        val described = PreviewText.read(json) ?: return
        text = described
        places = null
        lastSent = null
    }

    // The places in the text of the page, with the offsets worked out the first time they are needed for a description.
    // Null until the page has described its text
    fun places(source: () -> CharSequence): PreviewPlaces? {
        val described = text ?: return null
        return places ?: PreviewPlaces(described, source()).also { places = it }
    }

    // The page is sent what it was not sent last, so that a reading that stays where it is costs the page nothing
    fun send(message: String) {
        if (message == lastSent) return
        lastSent = message
        deliver(message)
    }
}

// The pages of the previews of every file in the IDE, and the one line to the log for a preview that shows no highlights
@Service(Service.Level.APP)
class PreviewPages(private val log: (String) -> Unit) {

    constructor() : this({ Logger.getInstance(PreviewPages::class.java).info(it) })

    private val pages = CopyOnWriteArrayList<PreviewPage>()
    private val logged = AtomicBoolean(false)

    fun register(page: PreviewPage, whileRegistered: Disposable) {
        pages += page
        Disposer.register(whileRegistered) { pages -= page }
    }

    fun pagesOf(file: VirtualFile): List<PreviewPage> = pages.filter { it.file == file }

    // Once whatever the reason, since the log is for a developer who asks why no highlight shows, and the first reason is
    // enough to start from
    fun noHighlightsInThePreview(reason: String) {
        if (logged.compareAndSet(false, true)) log("AloudRite: the preview shows no highlights, as $reason")
    }
}
