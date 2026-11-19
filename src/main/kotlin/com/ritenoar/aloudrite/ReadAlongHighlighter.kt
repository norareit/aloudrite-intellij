package com.ritenoar.aloudrite

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.event.VisibleAreaEvent
import com.intellij.openapi.editor.event.VisibleAreaListener
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.Disposer
import javax.swing.Timer

// The colours of the two highlights come from the colour scheme in use, through these keys, which fall back to keys of the
// scheme, so that they suit a light and a dark theme and leave room for a page that chooses them
val HEARD_SENTENCE_KEY: TextAttributesKey =
    TextAttributesKey.createTextAttributesKey("ALOUDRITE_HEARD_SENTENCE", EditorColors.IDENTIFIER_UNDER_CARET_ATTRIBUTES)

val HEARD_WORD_KEY: TextAttributesKey =
    TextAttributesKey.createTextAttributesKey("ALOUDRITE_HEARD_WORD", EditorColors.SEARCH_RESULT_ATTRIBUTES)

// The selection is drawn over both, and the word over the sentence
internal const val HEARD_SENTENCE_LAYER = HighlighterLayer.SELECTION - 2
internal const val HEARD_WORD_LAYER = HighlighterLayer.SELECTION - 1

private const val REFRESH_MILLISECONDS = 50

// Shows the sentence and the word that are being heard in every text editor of the file that is read. It asks the reading
// for the places while one exists, on the event thread, and removes everything it drew as soon as the reading is gone
@Service(Service.Level.APP)
class ReadAlongHighlighter(
    private val heardReading: () -> HeardReading,
    private val setting: () -> HighlightSetting,
    refreshMilliseconds: Int,
) : Disposable {

    constructor() : this({ service<SpeechService>() }, { service<HighlightSetting>() }, REFRESH_MILLISECONDS)

    internal val timer = Timer(refreshMilliseconds) { refresh() }

    private var shown: ShownReading? = null

    // Told of every start and end of the reading, on whichever thread it happened. The editors are for the event thread
    fun readingChanged() {
        val application = ApplicationManager.getApplication()
        if (application.isDispatchThread) refresh() else application.invokeLater(::refresh, ModalityState.any())
    }

    // Brings the highlights of every editor of the file in line with the reading as it is now
    fun refresh() {
        val state = heardReading().heardReading()
        if (state == null) {
            endTheReading()
            return
        }
        timer.start()
        val reading = readingShownFor(state)
        if (state.resumes != reading.resumes) reading.resumed(state.resumes)
        if (!reading.canBeHighlighted || !setting().highlights) {
            reading.removeHighlights()
            return
        }
        reading.show(state.places)
    }

    private fun readingShownFor(state: HeardState): ShownReading {
        shown?.takeIf { it.number == state.number }?.let { return it }
        endTheReading(keepTheTimer = true)
        return ShownReading(state).also { shown = it }
    }

    private fun endTheReading(keepTheTimer: Boolean = false) {
        shown?.end()
        shown = null
        if (!keepTheTimer) timer.stop()
    }

    override fun dispose() {
        endTheReading()
    }
}

// What is drawn for one reading. The edits are recorded from the moment the reading is first seen, so that the places,
// which are those of the text of the press, can be mapped to the text as it is now
private class ShownReading(state: HeardState) {
    val number = state.number
    var resumes = state.resumes
        private set

    private val document: Document? = FileDocumentManager.getInstance().getCachedDocument(state.target.file)
    private val edits = TextEdits()
    private val whileReading = Disposer.newDisposable("AloudRite read along")
    private val editors = LinkedHashMap<Editor, HighlightsOfAnEditor>()

    // The places are those of the text of the press. A document that already changed since is not that text, as when the
    // press waited for a download, and a reading of the file from disk has no document to compare with
    val canBeHighlighted: Boolean =
        document != null && state.documentStamp != null && state.documentStamp == document.modificationStamp

    init {
        document?.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) = edits.record(event.offset, event.oldLength, event.newLength)
        }, whileReading)
    }

    fun resumed(count: Int) {
        resumes = count
        editors.values.forEach { it.following.startFollowing() }
    }

    fun show(places: HeardPlaces?) {
        val textEditors = editorsOfTheFile()
        val sentence = places?.let { edits.map(it.sentence) }
        val word = places?.let { edits.map(it.word) }
        for (editor in textEditors) {
            val highlights = editors.getOrPut(editor) { HighlightsOfAnEditor(editor, whileReading) }
            highlights.show(sentence, word)
        }
    }

    // Editors that were released are forgotten, and one that is new is found at this update
    private fun editorsOfTheFile(): List<Editor> {
        editors.keys.removeAll { it.isDisposed }
        return document?.let { EditorFactory.getInstance().getEditors(it).toList() }.orEmpty()
    }

    fun removeHighlights() {
        editors.values.forEach(HighlightsOfAnEditor::remove)
    }

    fun end() {
        removeHighlights()
        Disposer.dispose(whileReading)
        editors.clear()
    }
}

// The two highlighters of one editor, and whether the editor follows the reading
private class HighlightsOfAnEditor(private val editor: Editor, whileReading: Disposable) {
    val following: Following

    private var sentence: RangeHighlighter? = null
    private var word: RangeHighlighter? = null

    init {
        val area = editor.scrollingModel.visibleArea
        following = Following(area.x, area.y)
        editor.scrollingModel.addVisibleAreaListener(object : VisibleAreaListener {
            override fun visibleAreaChanged(event: VisibleAreaEvent) =
                following.visibleAreaChanged(event.newRectangle.x, event.newRectangle.y)
        }, whileReading)
        editor.caretModel.addCaretListener(object : CaretListener {
            override fun caretPositionChanged(event: CaretEvent) = following.caretMoved()
        }, whileReading)
    }

    fun show(sentenceRange: SourceRange?, wordRange: SourceRange?) {
        sentence = placed(sentence, sentenceRange, HEARD_SENTENCE_LAYER, HEARD_SENTENCE_KEY)
        word = placed(word, wordRange, HEARD_WORD_LAYER, HEARD_WORD_KEY)
        if (wordRange != null && following.isFollowing) keepInView(wordRange.start)
    }

    fun remove() {
        if (editor.isDisposed) return
        sentence = placed(sentence, null, HEARD_SENTENCE_LAYER, HEARD_SENTENCE_KEY)
        word = placed(word, null, HEARD_WORD_LAYER, HEARD_WORD_KEY)
    }

    // A highlighter is replaced only where its range is not the one it covers now, since the editor moves it with the
    // text that is edited around it. Only a background is set, so that the text keeps its own colours
    private fun placed(current: RangeHighlighter?, range: SourceRange?, layer: Int, key: TextAttributesKey): RangeHighlighter? {
        val markup = editor.markupModel
        if (range != null && current != null && current.isValid && current.startOffset == range.start && current.endOffset == range.end) {
            return current
        }
        current?.let(markup::removeHighlighter)
        if (range == null || range.end > editor.document.textLength) return null
        val attributes = TextAttributes().apply { backgroundColor = editor.colorsScheme.getAttributes(key).backgroundColor }
        return markup.addRangeHighlighter(range.start, range.end, layer, attributes, HighlighterTargetArea.EXACT_RANGE)
    }

    // Scrolls without moving the caret or the selection, and without the animation, which would take the word out of view
    // again before it arrived. An editor that is not shown has no visible area to keep it in
    private fun keepInView(offset: Int) {
        val scrolling = editor.scrollingModel
        if (scrolling.visibleArea.isEmpty || scrolling.visibleArea.contains(editor.offsetToXY(offset))) return
        following.scrollItself {
            scrolling.disableAnimation()
            try {
                scrolling.scrollTo(editor.offsetToLogicalPosition(offset), ScrollType.CENTER)
            } finally {
                scrolling.enableAnimation()
            }
        }
    }
}

class ReadAlongListener : ReadingChangeListener {
    override fun readingChanged() {
        service<ReadAlongHighlighter>().readingChanged()
    }
}
