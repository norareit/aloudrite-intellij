package com.ritenoar.aloudrite

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.ApplicationRule
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.runInEdtAndWait
import com.ritenoar.aloudrite.core.INT8_MODEL
import com.ritenoar.aloudrite.core.SpeechStandIns
import com.ritenoar.aloudrite.core.fillWithModelStandIn
import com.ritenoar.aloudrite.core.placedWhatToRead
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

// The highlighter with pages of a preview in place of the browser: pages that record what they are sent, on the stand-ins of
// the engine and the player. A text makes two runs of 25 samples and a slice is 10 samples, as in ReadAlongHighlighterTest
class ReadAlongPreviewTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @get:Rule
    val editors = EditorsOfATest()

    private val standIns = SpeechStandIns<ReadingTarget>()
    private var setting = HighlightSetting().also { it.choices = HighlightChoices(word = true) }
    private val logged = mutableListOf<String>()
    private val pages = PreviewPages { logged += it }
    private var extensionPointIsThere = true
    private val whileTheTest = Disposer.newDisposable("ReadAlongPreviewTest")

    private val highlighter = ReadAlongHighlighter(
        { standIns.reading }, { setting }, { pages }, { extensionPointIsThere }, refreshMilliseconds = 3_600_000,
    )

    private val idleDownloads = ModelDownloadService(
        runInBackground = { },
        install = { _, _, _, _ -> },
        reportFailure = { _, _ -> },
        onRunningChanged = { },
    )

    private val text = "One two three. Four five."

    // A page of the preview of a file that records the messages it is sent
    private inner class RecordingPage(file: VirtualFile) {
        val messages = mutableListOf<String>()
        val page = PreviewPage(file) { messages += it }.also { pages.register(it, whileTheTest) }

        fun describe(markdown: String, version: Int = 1) = page.described(descriptionOfPage(markdown, version))

        val last: JsonObject get() = JsonParser.parseString(messages.last()).asJsonObject

        // The characters that the last message draws, of the text nodes of the page
        fun drawn(key: String): String =
            last[key].asJsonArray.joinToString("") { piece ->
                val (node, start, end) = piece.asJsonArray.map { it.asInt }
                page.text!!.nodes[node].text.substring(start, end)
            }
    }

    @After
    fun tidy() {
        runInEdtAndWait { highlighter.dispose() }
        Disposer.dispose(whileTheTest)
    }

    private fun press(file: VirtualFile) {
        val action = readAloudActionOn(
            standIns.reading, idleDownloads, temporaryFolder.newFolder().toPath().also { fillWithModelStandIn(INT8_MODEL.folderIn(it)) })
        pressTheButtonOn(action, file)
    }

    private fun refresh() = runInEdtAndWait { highlighter.refresh() }

    private fun refreshThroughTheEvent() {
        highlighter.readingChanged()
        runInEdtAndWait { PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue() }
    }

    private fun readAndCheckAtEachSlice(file: VirtualFile, check: (slice: Int) -> Unit) {
        press(file)
        standIns.whilePlaying = { slice ->
            refresh()
            check(slice)
        }
        standIns.runKeptWorks()
    }

    private fun documentOf(file: VirtualFile) = runReadAction { FileDocumentManager.getInstance().getDocument(file)!! }

    private fun heardIn(editor: Editor, layer: Int): String? = runInEdtAndGetHighlights(editor)
        .singleOrNull { it.layer == layer }?.let { editor.document.text.substring(it.startOffset, it.endOffset) }

    private fun runInEdtAndGetHighlights(editor: Editor) = com.intellij.testFramework.runInEdtAndGet { editor.markupModel.allHighlighters.toList() }

    @Test
    fun twoPagesOfTheFileShowTheSameCharactersAsTheEditorAtEachSliceAndAPageOfAnotherFileIsSentNothing() {
        val file = markdownFile(text)
        val editor = editors.textEditorOf(file).editor
        val first = RecordingPage(file).also { it.describe(text) }
        val second = RecordingPage(file).also { it.describe(text) }
        val other = RecordingPage(markdownFile(text)).also { it.describe(text) }
        val checked = mutableListOf<Boolean>()

        readAndCheckAtEachSlice(file) {
            for (page in listOf(first, second)) {
                checked += page.drawn("sentence") == heardIn(editor, HEARD_SENTENCE_LAYER)
                checked += page.drawn("word") == heardIn(editor, HEARD_WORD_LAYER)
            }
        }

        assertEquals(48, checked.size)
        assertTrue(checked.all { it })
        assertEquals(emptyList<String>(), other.messages.filter { it != PreviewMessage.clear() })
    }

    @Test
    fun theMessageHoldsTheVersionOfTheDescriptionTheNumberOfTheReadingAndTheColoursOfTheKeys() {
        val file = markdownFile(text)
        editors.textEditorOf(file)
        val page = RecordingPage(file).also { it.describe(text, version = 5) }

        readAndCheckAtEachSlice(file) { slice -> if (slice == 1) assertEquals(5, page.last["version"].asInt) }

        val places = page.messages.map { JsonParser.parseString(it).asJsonObject }.first { it.has("reading") }
        assertEquals(1, places["reading"].asInt)
        assertEquals(0, places["resumes"].asInt)
        assertTrue(places["sentenceColour"].asString.startsWith("rgba("))
        assertTrue(places["wordColour"].asString.startsWith("rgba("))
    }

    @Test
    fun aPageWithNoDescriptionIsSentNoPlacesAndOnceItReceivesOneItIsSentThemAtTheNextRefresh() {
        val file = markdownFile(text)
        editors.textEditorOf(file)
        val page = RecordingPage(file)
        val before = mutableListOf<Int>()

        readAndCheckAtEachSlice(file) { slice ->
            if (slice == 2) {
                before += page.messages.count { it != PreviewMessage.clear() }
                page.describe(text)
                before += page.messages.count { it != PreviewMessage.clear() }
                refresh()
                before += page.messages.count { it != PreviewMessage.clear() }
            }
        }

        assertEquals(listOf(0, 0, 1), before)
    }

    @Test
    fun aPageThatDescribedItselfAgainIsSentThePlacesAgain() {
        val file = markdownFile(text)
        editors.textEditorOf(file)
        val page = RecordingPage(file).also { it.describe(text) }
        val counts = mutableListOf<Int>()

        readAndCheckAtEachSlice(file) { slice ->
            if (slice == 1) {
                counts += page.messages.size
                page.describe(text, version = 2)
                refresh()
                counts += page.messages.size
                assertEquals(2, page.last["version"].asInt)
            }
        }

        assertEquals(listOf(1, 2), counts)
    }

    @Test
    fun codeExampleIsDrawnOnTheTextOfTheCodeBlockAndALinkOnTheTextOfTheLink() {
        val code = "Before.\n\n```kotlin\nval x = 1\n```\n\nAfter."
        val codeFile = markdownFile(code)
        editors.textEditorOf(codeFile)
        val codePage = RecordingPage(codeFile).also { it.describe(code) }
        val words = mutableSetOf<String>()

        readAndCheckAtEachSlice(codeFile) { if (codePage.last.has("word")) words += codePage.drawn("word") }

        assertTrue(words.toString(), "val x = 1\n" in words)
    }

    @Test
    fun linkIsDrawnOnTheTextOfTheLink() {
        val linked = "Go to https://example.com/page now."
        val file = markdownFile(linked)
        editors.textEditorOf(file)
        val page = RecordingPage(file).also { it.describe(linked) }
        val words = mutableSetOf<String>()

        readAndCheckAtEachSlice(file) { if (page.last.has("word")) words += page.drawn("word") }

        assertTrue(words.toString(), "https://example.com/page" in words)
    }

    @Test
    fun theMessageIsNotSentAgainWhileThePlacesStayAndSoNotWhilePaused() {
        val file = markdownFile(text)
        editors.textEditorOf(file)
        val page = RecordingPage(file).also { it.describe(text) }
        val counts = mutableListOf<Int>()

        press(file)
        standIns.whilePlaying = { slice ->
            refresh()
            if (slice == 3) standIns.reading.pause(ReadingTarget(null, file))
            if (slice == 3) counts += page.messages.size
        }
        standIns.runKeptWorks()
        refresh()
        refresh()
        counts += page.messages.size

        assertEquals(counts[0], counts[1])
    }

    @Test
    fun aResumeSendsTheNewCountOfResumesAndTheNextReadingItsNewNumber() {
        val file = markdownFile(text)
        editors.textEditorOf(file)
        val page = RecordingPage(file).also { it.describe(text) }
        val target = ReadingTarget(null, file)

        press(file)
        standIns.whilePlaying = { slice ->
            refresh()
            if (slice == 3) standIns.reading.pause(target)
        }
        standIns.runKeptWorks()
        standIns.reading.resume(target, System.nanoTime())
        standIns.whilePlaying = { refresh() }
        standIns.runKeptWorks()
        val resumed = page.messages.map { JsonParser.parseString(it).asJsonObject }.filter { it.has("resumes") }

        assertEquals(setOf(0, 1), resumed.map { it["resumes"].asInt }.toSet())

        refreshThroughTheEvent()
        press(file)
        standIns.whilePlaying = { refresh() }
        standIns.runKeptWorks()
        val readings = page.messages.map { JsonParser.parseString(it).asJsonObject }.filter { it.has("reading") }.map { it["reading"].asInt }

        assertEquals(setOf(1, 2), readings.toSet())
    }

    @Test
    fun theEndOfTheReadingSendsTheClearingMessageToEveryPageOfTheFileOnce() {
        val file = markdownFile(text)
        editors.textEditorOf(file)
        val first = RecordingPage(file).also { it.describe(text) }
        val second = RecordingPage(file).also { it.describe(text) }
        readAndCheckAtEachSlice(file) { }

        refreshThroughTheEvent()
        refreshThroughTheEvent()

        for (page in listOf(first, second)) {
            assertEquals(PreviewMessage.clear(), page.messages.last())
            assertEquals(1, page.messages.size - page.messages.indexOfLast { it != PreviewMessage.clear() } - 1)
        }
    }

    @Test
    fun aStopSendsTheClearingMessage() {
        val file = markdownFile(text)
        editors.textEditorOf(file)
        val page = RecordingPage(file).also { it.describe(text) }
        press(file)
        standIns.whilePlaying = { slice ->
            refresh()
            if (slice == 2) standIns.reading.stop(ReadingTarget(null, file))
        }
        standIns.runKeptWorks()

        refreshThroughTheEvent()

        assertEquals(PreviewMessage.clear(), page.messages.last())
    }

    @Test
    fun aFailureOfTheEngineSendsTheClearingMessage() {
        val file = markdownFile(text)
        editors.textEditorOf(file)
        val page = RecordingPage(file).also { it.describe(text) }
        standIns.engineFailsAt = { true }
        press(file)
        standIns.runKeptWorks()

        refreshThroughTheEvent()

        assertEquals(1, standIns.failures.size)
        assertTrue(page.messages.all { it == PreviewMessage.clear() })
    }

    @Test
    fun anotherReadingSendsTheClearingMessageToThePagesOfTheFirst() {
        val file = markdownFile(text)
        val other = markdownFile("Another file.")
        editors.textEditorOf(file)
        val page = RecordingPage(file).also { it.describe(text) }
        press(file)
        standIns.whilePlaying = { slice ->
            refresh()
            if (slice == 2) {
                press(other)
                refresh()
            }
        }
        standIns.runNextKeptWork()
        standIns.runNextKeptWork()

        refresh()

        assertEquals(PreviewMessage.clear(), page.messages.last())
    }

    private fun sentAtEachSliceWith(change: (HighlightChoices) -> HighlightChoices): List<String> {
        val file = markdownFile(text)
        editors.textEditorOf(file)
        val page = RecordingPage(file).also { it.describe(text) }
        val kinds = mutableListOf<String>()
        val all = setting.choices

        readAndCheckAtEachSlice(file) { slice ->
            if (slice == 2) setting.choices = change(all)
            if (slice == 4) setting.choices = all
            kinds += if (page.last.has("clear")) "clear" else "places"
        }

        assertEquals(12, standIns.players.single().played.size)
        return kinds
    }

    @Test
    fun withTheDefaultsEachMessageHoldsTheSentenceAndTheWord() {
        setting = HighlightSetting()
        val file = markdownFile(text)
        editors.textEditorOf(file)
        val page = RecordingPage(file).also { it.describe(text) }
        val shown = mutableListOf<Pair<String, String>>()

        readAndCheckAtEachSlice(file) { shown += page.drawn("sentence") to page.drawn("word") }

        assertTrue(shown.isNotEmpty())
        assertTrue(shown.all { (sentence, word) -> sentence.isNotEmpty() && word.isNotEmpty() })
    }

    @Test
    fun withTheSentenceClearedAndTheWordChosenEachMessageHoldsTheWordAndNoSentence() {
        setting.choices = HighlightChoices(sentence = false, word = true)
        val file = markdownFile(text)
        editors.textEditorOf(file)
        val page = RecordingPage(file).also { it.describe(text) }
        val shown = mutableListOf<Pair<Int, String>>()

        readAndCheckAtEachSlice(file) { shown += page.last["sentence"].asJsonArray.size() to page.drawn("word") }

        assertTrue(shown.isNotEmpty())
        assertTrue(shown.all { (sentencePieces, word) -> sentencePieces == 0 && word.isNotEmpty() })
    }

    @Test
    fun withBothHighlightsClearedEveryPageIsSentTheClearingMessageAndNoPlaces() {
        setting.choices = HighlightChoices(sentence = false, word = false)
        val file = markdownFile(text)
        editors.textEditorOf(file)
        val page = RecordingPage(file).also { it.describe(text) }

        readAndCheckAtEachSlice(file) { }

        assertTrue(page.messages.isNotEmpty())
        assertTrue(page.messages.all { it == PreviewMessage.clear() })
    }

    @Test
    fun withThePreviewClearedEveryPageIsSentTheClearingMessageAndTheEditorStillShowsItsHighlights() {
        setting.choices = HighlightChoices(inPreview = false, word = true)
        val file = markdownFile(text)
        val editor = editors.textEditorOf(file).editor
        val page = RecordingPage(file).also { it.describe(text) }
        val inTheEditor = mutableListOf<Int>()

        readAndCheckAtEachSlice(file) { inTheEditor += runInEdtAndGetHighlights(editor).size }

        assertTrue(page.messages.isNotEmpty())
        assertTrue(page.messages.all { it == PreviewMessage.clear() })
        assertTrue(inTheEditor.all { it == 2 })
    }

    @Test
    fun withTheTextEditorClearedThePagesAreStillSentThePlaces() {
        setting.choices = HighlightChoices(inTextEditor = false, word = true)
        val file = markdownFile(text)
        editors.textEditorOf(file)
        val page = RecordingPage(file).also { it.describe(text) }
        val drawn = mutableListOf<String>()

        readAndCheckAtEachSlice(file) { drawn += page.drawn("sentence") }

        assertTrue(drawn.isNotEmpty())
        assertTrue(drawn.all { it.isNotEmpty() })
    }

    @Test
    fun thePreviewClearedDuringTheReadingClearsThePagesAndCheckedAgainTheyAreSentThePlaces() {
        val kinds = sentAtEachSliceWith { it.copy(inPreview = false) }

        assertEquals(listOf("places", "places", "clear", "clear", "places"), kinds.take(5))
    }

    @Test
    fun theSentenceClearedDuringTheReadingEmptiesItsPlacesAndCheckedAgainTheyAreSentAgain() {
        val file = markdownFile(text)
        editors.textEditorOf(file)
        val page = RecordingPage(file).also { it.describe(text) }
        val sentencePieces = mutableListOf<Int>()
        val all = setting.choices

        readAndCheckAtEachSlice(file) { slice ->
            if (slice == 2) setting.choices = all.copy(sentence = false)
            if (slice == 4) setting.choices = all
            sentencePieces += page.last["sentence"].asJsonArray.size()
        }

        assertEquals(listOf(true, true, false, false, true), sentencePieces.take(5).map { it > 0 })
        assertEquals(12, standIns.players.single().played.size)
    }

    @Test
    fun theWordClearedDuringTheReadingEmptiesItsPlacesAndCheckedAgainTheyAreSentAgain() {
        val file = markdownFile(text)
        editors.textEditorOf(file)
        val page = RecordingPage(file).also { it.describe(text) }
        val wordPieces = mutableListOf<Int>()
        val all = setting.choices

        readAndCheckAtEachSlice(file) { slice ->
            if (slice == 2) setting.choices = all.copy(word = false)
            if (slice == 4) setting.choices = all
            wordPieces += page.last["word"].asJsonArray.size()
        }

        assertEquals(listOf(true, true, false, false, true), wordPieces.take(5).map { it > 0 })
        assertEquals(12, standIns.players.single().played.size)
    }

    @Test
    fun aReadingWhoseDocumentChangedSinceThePressSendsTheClearingMessageAndNoPlaces() {
        val file = markdownFile(text)
        val document = documentOf(file)
        editors.textEditorOf(file)
        val page = RecordingPage(file).also { it.describe(text) }
        val stampOfThePress = document.modificationStamp
        WriteCommandAction.runWriteCommandAction(null) { document.insertString(0, "Changed during a download. ") }
        standIns.reading.read(ReadingTarget(null, file), placedWhatToRead(text, stampOfThePress), INT8_MODEL, System.nanoTime())
        standIns.whilePlaying = { refresh() }

        standIns.runKeptWorks()

        assertTrue(page.messages.isNotEmpty())
        assertTrue(page.messages.all { it == PreviewMessage.clear() })
    }

    @Test
    fun withTheExtensionPointMissingTheEditorsShowTheirHighlightsAsBeforeAndTheLogHasTheOneLine() {
        extensionPointIsThere = false
        val file = markdownFile(text)
        val editor = editors.textEditorOf(file).editor
        val seen = mutableListOf<String?>()

        readAndCheckAtEachSlice(file) { slice -> if (slice == 1) seen += heardIn(editor, HEARD_WORD_LAYER) }

        assertEquals(listOf<String?>("One"), seen)
        assertEquals(1, logged.size)
        assertTrue(logged.single(), logged.single().startsWith("AloudRite: the preview shows no highlights, as "))
    }

    @Test
    fun withTheExtensionPointThereTheLogHasNoLine() {
        val file = markdownFile(text)
        editors.textEditorOf(file)

        readAndCheckAtEachSlice(file) { }

        assertEquals(emptyList<String>(), logged)
    }

    companion object {
        @JvmField
        @ClassRule
        val application = ApplicationRule()
    }
}
