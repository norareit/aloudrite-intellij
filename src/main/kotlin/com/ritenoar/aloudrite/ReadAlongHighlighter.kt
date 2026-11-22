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
import com.intellij.openapi.editor.colors.EditorColorsManager
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
import com.intellij.openapi.vfs.VirtualFile
import com.ritenoar.aloudrite.core.HeardPlaces
import com.ritenoar.aloudrite.core.HeardReading
import com.ritenoar.aloudrite.core.HeardState
import com.ritenoar.aloudrite.core.SourceRange
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

// The extension point of the Markdown plugin that the preview takes its scripts through, named here as a string so that no
// class of that plugin is loaded where it is missing
private const val PREVIEW_EXTENSION_POINT = "org.intellij.markdown.browserPreviewExtensionProvider"

// Shows the sentence and the word that are being heard in every text editor of the file that is read, and sends the same
// places to every page of a preview of it. It asks the reading for the places while one exists, on the event thread, and
// removes everything it drew as soon as the reading is gone
@Service(Service.Level.APP)
class ReadAlongHighlighter(
    private val heardReading: () -> HeardReading<ReadingTarget>,
    private val setting: () -> HighlightSetting,
    private val previews: () -> PreviewPages,
    private val hasPreviewExtensionPoint: () -> Boolean,
    refreshMilliseconds: Int,
) : Disposable {

    constructor() : this(
        { service<SpeechService>().reading },
        { service<HighlightSetting>() },
        { service<PreviewPages>() },
        { ApplicationManager.getApplication().extensionArea.hasExtensionPoint(PREVIEW_EXTENSION_POINT) },
        REFRESH_MILLISECONDS,
    )

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
        val choices = setting().choices
        if (!reading.canBeHighlighted) {
            reading.removeHighlights()
            return
        }
        reading.show(state.places, choices)
    }

    private fun readingShownFor(state: HeardState<ReadingTarget>): ShownReading {
        shown?.takeIf { it.number == state.number }?.let { return it }
        endTheReading(keepTheTimer = true)
        if (!hasPreviewExtensionPoint()) {
            previews().noHighlightsInThePreview("the Markdown plugin has no extension point for the page of the preview")
        }
        return ShownReading(state, previews).also { shown = it }
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
private class ShownReading(state: HeardState<ReadingTarget>, private val previews: () -> PreviewPages) {
    val number = state.number
    private val file: VirtualFile = state.target.file
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

    // The choices say where each highlight is drawn: a view that is not chosen is cleared and the other is left as it is
    fun show(places: HeardPlaces?, choices: HighlightChoices) {
        val sentence = places?.takeIf { choices.sentence }?.let { edits.map(it.sentence) }
        val word = places?.takeIf { choices.word }?.let { edits.map(it.word) }
        showInTheEditors(sentence, word, choices.inTextEditor)
        if (choices.inPreview) showInThePreviews(sentence, word) else clearThePreviews()
    }

    private fun showInTheEditors(sentence: SourceRange?, word: SourceRange?, chosen: Boolean) {
        val textEditors = editorsOfTheFile()
        if (!chosen) {
            editors.values.forEach(HighlightsOfAnEditor::remove)
            return
        }
        for (editor in textEditors) {
            val highlights = editors.getOrPut(editor) { HighlightsOfAnEditor(editor, whileReading) }
            highlights.show(sentence, word)
        }
    }

    // The pages are sent what the editors were. A page that has not described its text yet is sent nothing, and is sent the
    // places at the first refresh after it did
    private fun showInThePreviews(sentence: SourceRange?, word: SourceRange?) {
        val text = document?.charsSequence ?: return
        for (page in previews().pagesOf(file)) {
            val pagePlaces = page.places { text } ?: continue
            val version = page.text?.version ?: continue
            page.send(
                if (sentence == null && word == null) PreviewMessage.clear()
                else PreviewMessage.places(
                    version = version,
                    reading = number,
                    resumes = resumes,
                    sentenceColour = colourOf(HEARD_SENTENCE_KEY),
                    wordColour = colourOf(HEARD_WORD_KEY),
                    sentence = sentence?.let(pagePlaces::pieces).orEmpty(),
                    word = word?.let(pagePlaces::wordPlace) ?: WordPlace(emptyList(), null),
                ),
            )
        }
    }

    // The colour scheme that the preview takes its own colours from, which is the global one
    private fun colourOf(key: TextAttributesKey): String? =
        PreviewMessage.cssColour(EditorColorsManager.getInstance().globalScheme.getAttributes(key).backgroundColor)

    // Editors that were released are forgotten, and one that is new is found at this update
    private fun editorsOfTheFile(): List<Editor> {
        editors.keys.removeAll { it.isDisposed }
        return document?.let { EditorFactory.getInstance().getEditors(it).toList() }.orEmpty()
    }

    fun removeHighlights() {
        editors.values.forEach(HighlightsOfAnEditor::remove)
        clearThePreviews()
    }

    private fun clearThePreviews() {
        previews().pagesOf(file).forEach { it.send(PreviewMessage.clear()) }
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
        val toFollow = rangeToFollow(sentenceRange, wordRange)
        if (toFollow != null && following.isFollowing) keepInView(toFollow)
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

    // Scrolls without moving the caret or the selection, and without the animation, which would take the range out of view
    // again before it arrived. An editor that is not shown has no visible area to keep it in. A range that is in view at
    // both ends is left where it is
    private fun keepInView(range: SourceRange) {
        val scrolling = editor.scrollingModel
        val offset = range.start
        val visible = scrolling.visibleArea
        if (visible.isEmpty || (visible.contains(editor.offsetToXY(range.start)) && visible.contains(editor.offsetToXY(range.end)))) return
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

// What a view keeps in view: the word where it is drawn, else the sentence, and nothing where neither is
internal fun rangeToFollow(sentence: SourceRange?, word: SourceRange?): SourceRange? = word ?: sentence

class ReadAlongListener : ReadingChangeListener {
    override fun readingChanged() {
        service<ReadAlongHighlighter>().readingChanged()
    }
}
