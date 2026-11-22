package com.ritenoar.aloudrite

import com.intellij.ide.plugins.IdeaPluginDescriptorImpl
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.ApplicationRule
import com.intellij.testFramework.LightVirtualFile
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.runInEdtAndWait
import com.ritenoar.aloudrite.core.INT8_MODEL
import com.ritenoar.aloudrite.core.SourceRange
import com.ritenoar.aloudrite.core.SpeechStandIns
import com.ritenoar.aloudrite.core.fillWithModelStandIn
import com.ritenoar.aloudrite.core.placedWhatToRead
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.awt.Font

// The highlights in editors, on the stand-ins of the engine and the player. A text makes two runs of 25 samples, a slice is
// 10 samples, and so a piece plays as six slices, which put the share heard at 0.2, 0.4, 0.5, 0.7, 0.9 and 1.0
class ReadAlongHighlighterTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @get:Rule
    val editors = EditorsOfATest()

    private val standIns = SpeechStandIns<ReadingTarget>()
    private var setting = HighlightSetting().also { it.choices = HighlightChoices(word = true) }

    // The timer is left to the tests, which refresh at the moments they choose
    private val highlighter = ReadAlongHighlighter(
        { standIns.reading }, { setting }, { PreviewPages { } }, { true }, refreshMilliseconds = 3_600_000,
    )

    private val idleDownloads = ModelDownloadService(
        runInBackground = { },
        install = { _, _, _, _ -> },
        reportFailure = { _, _ -> },
        onRunningChanged = { },
    )

    private val text = "One two three. Four five."

    @After
    fun closeTheHighlighter() {
        runInEdtAndWait { highlighter.dispose() }
    }

    private fun documentOf(file: VirtualFile): Document = runReadAction { FileDocumentManager.getInstance().getDocument(file)!! }

    private fun press(file: VirtualFile) {
        val action = readAloudActionOn(
            standIns.reading, idleDownloads, temporaryFolder.newFolder().toPath().also { fillWithModelStandIn(INT8_MODEL.folderIn(it)) })
        pressTheButtonOn(action, file)
    }

    private fun refresh() = runInEdtAndWait { highlighter.refresh() }

    // Asks for the refresh through the event, as the end of a reading does, and lets the events of the EDT run
    private fun refreshThroughTheEvent() {
        highlighter.readingChanged()
        runInEdtAndWait { PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue() }
    }

    // Reads the file and plays it, calling the check after the slice, with the highlights refreshed
    private fun readAndCheckAtEachSlice(file: VirtualFile, check: (slice: Int) -> Unit) {
        press(file)
        standIns.whilePlaying = { slice ->
            refresh()
            check(slice)
        }
        standIns.runKeptWorks()
    }

    private fun highlightersOf(editor: Editor): List<RangeHighlighter> =
        runInEdtAndGetAll(editor).filter { it.layer == HEARD_SENTENCE_LAYER || it.layer == HEARD_WORD_LAYER }

    private fun runInEdtAndGetAll(editor: Editor): List<RangeHighlighter> {
        var all = emptyList<RangeHighlighter>()
        runInEdtAndWait { all = editor.markupModel.allHighlighters.toList() }
        return all
    }

    private fun textOf(editor: Editor, layer: Int): String? =
        highlightersOf(editor).singleOrNull { it.layer == layer }?.let { editor.document.text.substring(it.startOffset, it.endOffset) }

    private fun sentenceOf(editor: Editor) = textOf(editor, HEARD_SENTENCE_LAYER)

    private fun wordOf(editor: Editor) = textOf(editor, HEARD_WORD_LAYER)

    @Test
    fun twoEditorsOfTheFileAndTheTextEditorOfASplitEachShowTheSentenceAndTheWord() {
        val file = markdownFile(text)
        val first = editors.textEditorOf(file).editor
        val second = editors.textEditorOf(file).editor
        val split = editors.editorWithPreviewOf(file).textEditor.editor
        val shown = mutableListOf<List<String?>>()

        readAndCheckAtEachSlice(file) { slice ->
            if (slice == 1) shown += listOf(first, second, split).flatMap { listOf(sentenceOf(it), wordOf(it)) }
        }

        assertEquals(listOf(List(3) { listOf("One two three.", "One") }.flatten()), shown)
    }

    @Test
    fun theWordMovesOnWhileTheSentenceStaysWithinASentence() {
        val file = markdownFile(text)
        val editor = editors.textEditorOf(file).editor
        val words = mutableListOf<String?>()
        val sentences = mutableListOf<String?>()

        readAndCheckAtEachSlice(file) { slice ->
            if (slice <= 6) {
                words += wordOf(editor)
                sentences += sentenceOf(editor)
            }
        }

        assertEquals(listOf("One", "two", "two", "three.", "three.", "three."), words)
        assertEquals(List(6) { "One two three." }, sentences)
    }

    @Test
    fun codeExampleIsHighlightedOnTheWholeBlockInBothHighlighters() {
        val block = "```kotlin\nval x = 1\n```"
        val file = markdownFile("Before.\n\n$block\n\nAfter.")
        val editor = editors.textEditorOf(file).editor
        val shown = mutableListOf<Pair<String?, String?>>()

        // Before. is six slices and its silence of one second, which "code example" follows, is ten
        readAndCheckAtEachSlice(file) { slice -> if (slice == 6 + 10 + 1) shown += sentenceOf(editor) to wordOf(editor) }

        assertEquals(listOf<Pair<String?, String?>>(block to block), shown)
    }

    @Test
    fun theHighlightersAreBelowTheSelectionWithTheWordAboveTheSentenceAndSetOnlyABackground() {
        val file = markdownFile(text)
        val editor = editors.textEditorOf(file).editor
        val layers = mutableListOf<Pair<Int, TextAttributesSummary>>()

        readAndCheckAtEachSlice(file) { slice ->
            if (slice == 1) {
                layers += highlightersOf(editor).map {
                    it.layer to TextAttributesSummary(it.getTextAttributes(null)!!.foregroundColor, it.getTextAttributes(null)!!.fontType)
                }
            }
        }

        assertEquals(setOf(HEARD_SENTENCE_LAYER, HEARD_WORD_LAYER), layers.map { it.first }.toSet())
        assertTrue(HEARD_SENTENCE_LAYER < HEARD_WORD_LAYER)
        assertTrue(HEARD_WORD_LAYER < com.intellij.openapi.editor.markup.HighlighterLayer.SELECTION)
        layers.forEach { assertNull(it.second.foreground); assertEquals(Font.PLAIN, it.second.fontType) }
    }

    private data class TextAttributesSummary(val foreground: java.awt.Color?, val fontType: Int)

    @Test
    fun theColoursComeFromKeysOfThePluginWithAFallbackToKeysOfTheColourScheme() {
        assertEquals("ALOUDRITE_HEARD_SENTENCE", HEARD_SENTENCE_KEY.externalName)
        assertEquals("ALOUDRITE_HEARD_WORD", HEARD_WORD_KEY.externalName)
        assertNotNull(HEARD_SENTENCE_KEY.fallbackAttributeKey)
        assertNotNull(HEARD_WORD_KEY.fallbackAttributeKey)
    }

    @Test
    fun theHighlightsHaveABackgroundOfTheColourScheme() {
        val file = markdownFile(text)
        val editor = editors.textEditorOf(file).editor
        val backgrounds = mutableListOf<java.awt.Color?>()

        readAndCheckAtEachSlice(file) { slice ->
            if (slice == 1) backgrounds += highlightersOf(editor).map { it.getTextAttributes(null)!!.backgroundColor }
        }

        assertEquals(2, backgrounds.size)
        assertTrue(backgrounds.all { it != null })
    }

    @Test
    fun nothingIsShownBeforeTheFirstSlice() {
        val file = markdownFile(text)
        val editor = editors.textEditorOf(file).editor
        press(file)

        refresh()
        standIns.runNextKeptWork()
        refresh()

        assertTrue(highlightersOf(editor).isEmpty())
    }

    @Test
    fun anEditorMadeDuringTheReadingShowsTheHighlightsAfterTheNextRefreshAndNotBefore() {
        val file = markdownFile(text)
        editors.textEditorOf(file)
        val shown = mutableListOf<List<String?>>()

        readAndCheckAtEachSlice(file) { slice ->
            if (slice == 2) {
                val late = editors.textEditorOf(file).editor
                shown += listOf(sentenceOf(late), wordOf(late))
                refresh()
                shown += listOf(sentenceOf(late), wordOf(late))
            }
        }

        assertEquals(listOf(listOf(null, null), listOf("One two three.", "two")), shown)
    }

    @Test
    fun textInsertedBeforeTheSentenceDuringTheReadingLeavesTheHighlightsOnTheSameCharacters() {
        val file = markdownFile(text)
        val document = documentOf(file)
        val editor = editors.textEditorOf(file).editor
        val shown = mutableListOf<Triple<String?, String?, Int>>()

        readAndCheckAtEachSlice(file) { slice ->
            if (slice == 1) WriteCommandAction.runWriteCommandAction(null) { document.insertString(0, "Inserted. ") }
            if (slice == 2) shown += Triple(sentenceOf(editor), wordOf(editor), highlightersOf(editor).minOf { it.startOffset })
        }

        assertEquals(listOf(Triple<String?, String?, Int>("One two three.", "two", "Inserted. ".length)), shown)
    }

    @Test
    fun aWordWhoseCharactersWereAllDeletedIsNotHighlightedAndTheReadingGoesOn() {
        val file = markdownFile(text)
        val document = documentOf(file)
        val editor = editors.textEditorOf(file).editor
        val shown = mutableListOf<Pair<String?, String?>>()

        readAndCheckAtEachSlice(file) { slice ->
            if (slice == 1) WriteCommandAction.runWriteCommandAction(null) { document.deleteString(text.indexOf("two"), text.indexOf("two") + 4) }
            if (slice == 2) shown += sentenceOf(editor) to wordOf(editor)
        }

        assertEquals(listOf<Pair<String?, String?>>("One three." to null), shown)
        assertFalse(standIns.reading.isReading(ReadingTarget(null, file)))
        assertTrue(standIns.failures.isEmpty())
        assertEquals(12, standIns.players.single().played.size)
    }

    @Test
    fun noHighlighterIsLeftOnceTheReadingEnded() {
        val file = markdownFile(text)
        val editor = editors.textEditorOf(file).editor
        readAndCheckAtEachSlice(file) { }

        refreshThroughTheEvent()

        assertTrue(highlightersOf(editor).isEmpty())
    }

    @Test
    fun noHighlighterIsLeftOnceTheReadingWasStopped() {
        val file = markdownFile(text)
        val editor = editors.textEditorOf(file).editor
        press(file)
        standIns.whilePlaying = { slice ->
            refresh()
            if (slice == 2) {
                assertEquals(2, highlightersOf(editor).size)
                standIns.reading.stop(ReadingTarget(null, file))
            }
        }
        standIns.runKeptWorks()

        refreshThroughTheEvent()

        assertTrue(highlightersOf(editor).isEmpty())
    }

    @Test
    fun noHighlighterIsLeftOnceAReadingOfAnotherFileReplacedTheReading() {
        val file = markdownFile(text)
        val other = LightVirtualFile("other.md", markdownFileType(), "Another file.")
        val editor = editors.textEditorOf(file).editor
        val otherEditor = editors.textEditorOf(other).editor
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

        assertTrue(highlightersOf(editor).isEmpty())
        assertTrue(highlightersOf(otherEditor).isEmpty())
    }

    @Test
    fun noHighlighterIsLeftOnceTheEngineFailed() {
        val file = markdownFile(text)
        val editor = editors.textEditorOf(file).editor
        readAndCheckAtEachSlice(file) { }
        standIns.engineFailsAt = { true }
        press(file)
        standIns.runKeptWorks()

        refreshThroughTheEvent()

        assertEquals(1, standIns.failures.size)
        assertTrue(highlightersOf(editor).isEmpty())
    }

    @Test
    fun nothingTheHighlightsDoChangesTheTextOfTheDocumentOrItsStamp() {
        val file = markdownFile(text)
        val document = documentOf(file)
        val editor = editors.textEditorOf(file).editor
        val stamp = document.modificationStamp

        readAndCheckAtEachSlice(file) { assertEquals(stamp, document.modificationStamp) }
        refreshThroughTheEvent()

        assertEquals(text, document.text)
        assertEquals(stamp, document.modificationStamp)
        assertTrue(highlightersOf(editor).isEmpty())
    }

    @Test
    fun aReadingWhoseDocumentChangedSinceThePressShowsNoHighlights() {
        val file = markdownFile(text)
        val document = documentOf(file)
        val editor = editors.textEditorOf(file).editor
        val stampOfThePress = document.modificationStamp
        WriteCommandAction.runWriteCommandAction(null) { document.insertString(0, "Changed during a download. ") }
        standIns.reading.read(ReadingTarget(null, file), placedWhatToRead(text, stampOfThePress), INT8_MODEL, System.nanoTime())
        val shown = mutableListOf<Int>()
        standIns.whilePlaying = {
            refresh()
            shown += highlightersOf(editor).size
        }

        standIns.runKeptWorks()

        assertEquals(List(12) { 0 }, shown)
    }

    // The number of highlighters in the editor after each slice, with the choices changed by `change` at slice 2 and
    // changed back at slice 4, which the refresh of the slice after sees
    private fun highlightersAtEachSliceWith(change: (HighlightChoices) -> HighlightChoices): List<Int> {
        val file = markdownFile(text)
        val editor = editors.textEditorOf(file).editor
        val shown = mutableListOf<Int>()
        val all = setting.choices

        readAndCheckAtEachSlice(file) { slice ->
            if (slice == 2) setting.choices = change(all)
            if (slice == 4) setting.choices = all
            shown += highlightersOf(editor).size
        }

        assertEquals(12, standIns.players.single().played.size)
        return shown
    }

    @Test
    fun withTheDefaultsEachSliceShowsTheSentenceAndTheWordInEveryEditorOfTheFile() {
        setting = HighlightSetting()
        val file = markdownFile(text)
        val first = editors.textEditorOf(file).editor
        val second = editors.textEditorOf(file).editor
        val shown = mutableListOf<List<String?>>()

        readAndCheckAtEachSlice(file) { slice ->
            if (slice == 1) shown += listOf(first, second).flatMap { listOf(sentenceOf(it), wordOf(it)) }
        }

        assertEquals(listOf(listOf("One two three.", "One", "One two three.", "One")), shown)
    }

    @Test
    fun withTheSentenceClearedAndTheWordChosenEachSliceShowsTheWordAndNoSentence() {
        setting.choices = HighlightChoices(sentence = false, word = true)
        val file = markdownFile(text)
        val editor = editors.textEditorOf(file).editor
        val shown = mutableListOf<List<String?>>()

        readAndCheckAtEachSlice(file) { slice -> if (slice == 1) shown += listOf(sentenceOf(editor), wordOf(editor)) }

        assertEquals(listOf(listOf(null, "One")), shown)
    }

    @Test
    fun withBothHighlightsClearedNoEditorHasAHighlighterAndTheReadingPlaysAllItsSlices() {
        setting.choices = HighlightChoices(sentence = false, word = false)
        val file = markdownFile(text)
        val editor = editors.textEditorOf(file).editor
        val shown = mutableListOf<Int>()

        readAndCheckAtEachSlice(file) { shown += highlightersOf(editor).size }

        assertEquals(List(12) { 0 }, shown)
        assertEquals(12, standIns.players.single().played.size)
    }

    @Test
    fun withTheTextEditorClearedNoEditorHasAHighlighter() {
        setting.choices = HighlightChoices(inTextEditor = false, word = true)
        val file = markdownFile(text)
        val editor = editors.textEditorOf(file).editor
        val shown = mutableListOf<Int>()

        readAndCheckAtEachSlice(file) { shown += highlightersOf(editor).size }

        assertEquals(List(12) { 0 }, shown)
    }

    @Test
    fun theTextEditorClearedDuringTheReadingRemovesItsHighlightsAndCheckedAgainTheyComeBack() {
        val shown = highlightersAtEachSliceWith { it.copy(inTextEditor = false) }

        // The change is seen at the refresh of the next slice
        assertEquals(listOf(2, 2, 0, 0, 2, 2, 2, 2, 2, 2, 2, 2), shown)
    }

    @Test
    fun thePreviewClearedDuringTheReadingLeavesTheEditorAsItIs() {
        assertEquals(List(12) { 2 }, highlightersAtEachSliceWith { it.copy(inPreview = false) })
    }

    @Test
    fun theSentenceClearedDuringTheReadingRemovesOnlyItAndCheckedAgainItComesBack() {
        assertEquals(listOf(2, 2, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2), highlightersAtEachSliceWith { it.copy(sentence = false) })
    }

    @Test
    fun theWordClearedDuringTheReadingRemovesOnlyItAndCheckedAgainItComesBack() {
        assertEquals(listOf(2, 2, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2), highlightersAtEachSliceWith { it.copy(word = false) })
    }

    @Test
    fun theRangeToFollowIsTheWordThenTheSentenceThenNothing() {
        val sentence = SourceRange(0, 14)
        val word = SourceRange(0, 3)

        assertEquals(word, rangeToFollow(sentence, word))
        assertEquals(sentence, rangeToFollow(sentence, null))
        assertNull(rangeToFollow(null, null))
    }

    @Test
    fun theTimerRunsEveryTenthOfASecondAtMostAndOnlyWhileAReadingExists() {
        val file = markdownFile(text)
        val running = mutableListOf<Boolean>()

        assertTrue(service<ReadAlongHighlighter>().timer.delay <= 100)
        refresh()
        running += highlighter.timer.isRunning
        press(file)
        refresh()
        running += highlighter.timer.isRunning
        standIns.runKeptWorks()
        refresh()
        running += highlighter.timer.isRunning

        assertEquals(listOf(false, true, false), running)
    }

    @Test
    fun theHighlighterListensToTheChangesOfTheReadingThroughTheListenerOfThePlugin() {
        val plugin = PluginManagerCore.getPlugin(PluginId.getId("com.ritenoar.aloudrite")) as IdeaPluginDescriptorImpl

        val listeners = plugin.appContainerDescriptor.listeners.orEmpty()

        assertTrue(listeners.any {
            it.listenerClassName == ReadAlongListener::class.java.name && it.topicClassName == ReadingChangeListener::class.java.name
        })
    }

    companion object {
        @JvmField
        @ClassRule
        val application = ApplicationRule()
    }
}
