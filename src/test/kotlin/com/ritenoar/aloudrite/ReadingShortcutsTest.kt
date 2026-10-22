package com.ritenoar.aloudrite

import com.intellij.icons.AllIcons
import com.intellij.mock.MockProject
import com.intellij.notification.Notification
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.keymap.ex.KeymapManagerEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.ApplicationRule
import com.intellij.testFramework.LightVirtualFile
import com.intellij.testFramework.runInEdtAndGet
import com.intellij.testFramework.runInEdtAndWait
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Path
import javax.swing.KeyStroke

class ReadingShortcutsTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val standIns = SpeechStandIns()
    private val disposable = Disposer.newDisposable()
    private val notifications = mutableListOf<Notification>()
    private val ownProject: Project = ProjectManager.getInstance().defaultProject
    private val otherProject: Project = MockProject(null, disposable)
    private val idleDownloads = ModelDownloadService(
        runInBackground = { },
        install = { _, _, _, _ -> },
        reportFailure = { _, _ -> },
        onRunningChanged = { },
    )

    private val file = markdownFile("One. Two.")
    // One piece, so that what the engine makes after a change of the speed is that piece again and not the next
    private val oneSentenceFile = markdownFile("One sentence.")
    private val otherMarkdownFile = LightVirtualFile("other.md", markdownFileType(), "Other.")
    private val plainTextFile = LightVirtualFile("notes.txt", PlainTextFileType.INSTANCE, "Plain.")

    // The file of the selected editor, which the IDE takes from the editors of the project
    private var selectedFile: VirtualFile? = null
    private val action by lazy { readingAction(folderWithTheModelStandIn()) }
    private val stop by lazy { stopReadingActionOn(standIns.service) }

    // As the setting of the IDE does, but with the stand-in service of the reading
    private val setting = ReadingSpeedSetting { speed, afterAWait -> standIns.service.changeSpeed(speed, afterAWait) }
    private val slower = ReadSlowerAction { setting }
    private val faster = ReadFasterAction { setting }

    @Before
    fun catchTheNotifications() {
        ApplicationManager.getApplication().messageBus.connect(disposable).subscribe(Notifications.TOPIC, object : Notifications {
            override fun notify(notification: Notification) {
                notifications += notification
            }
        })
    }

    @After
    fun cleanUp() {
        Disposer.dispose(disposable)
    }

    @Test
    fun theFourActionsAreNamedForWhatTheirKeysDoAndEveryActionOfThePluginHasANameOfItsOwn() {
        val actionManager = ActionManager.getInstance()
        val texts = actionManager.getActionIdList("com.ritenoar.aloudrite.").mapNotNull { actionManager.getAction(it).templateText }

        assertEquals("Read Aloud, Pause or Resume", registeredReadAloudAction().templateText)
        assertEquals("Stop Reading", registeredAction(STOP_READING_ACTION_ID).templateText)
        assertEquals("Read Slower", registeredAction(READ_SLOWER_ACTION_ID).templateText)
        assertEquals("Read Faster", registeredAction(READ_FASTER_ACTION_ID).templateText)
        assertTrue(registeredAction(READ_SLOWER_ACTION_ID) is ReadSlowerAction)
        assertTrue(registeredAction(READ_FASTER_ACTION_ID) is ReadFasterAction)
        assertEquals(texts.distinct(), texts)
    }

    @Test
    fun outsideTheTabTheActionsShowTheirNames() {
        assertEquals("Read Aloud, Pause or Resume" to AloudRiteIcons.ReadAloud, keyPresentationFor(action, ownProject))
        assertEquals("Stop Reading" to AllIcons.Actions.Suspend, keyPresentationFor(stop, ownProject))

        pressTheButtonOn(action, file, ownProject)

        assertEquals("Read Aloud, Pause or Resume" to AloudRiteIcons.ReadAloud, keyPresentationFor(action, ownProject))
        assertEquals("Stop Reading" to AllIcons.Actions.Suspend, keyPresentationFor(stop, ownProject))
    }

    @Test
    fun withNothingReadTheKeyReadsTheMarkdownFileOfTheSelectedEditorAndNotTheFileSelectedInTheProjectView() {
        selectedFile = file

        val messages = pressTheKeyOf(action, ownProject, otherMarkdownFile)

        assertTrue(messages.isEmpty())
        assertTrue(standIns.service.isReading(ReadingTarget(ownProject, file)))
        standIns.runKeptWorks()
        assertEquals(listOf("One. ", "Two."), standIns.textsMade)
    }

    @Test
    fun withNoModelInItsFolderTheKeyAsksAboutTheDownloadAsTheTabButtonDoes() {
        val modelsFolder = temporaryFolder.newFolder().toPath()
        val withoutTheModel = readingAction(modelsFolder)
        selectedFile = file

        val messagesOfTheKey = pressTheKeyOf(withoutTheModel, ownProject) { Messages.NO }
        val messagesOfTheButton = pressTheButtonOn(withoutTheModel, file, ownProject) { Messages.NO }

        assertEquals(listOf(downloadQuestion(INT8_MODEL, INT8_MODEL.folderIn(modelsFolder))), messagesOfTheKey)
        assertEquals(messagesOfTheButton, messagesOfTheKey)
        assertFalse(standIns.service.isReading(ReadingTarget(ownProject, file)))
    }

    @Test
    fun withNothingReadTheKeyDoesNothingForASelectedFileThatIsNotMarkdownOrWithNoSelectedEditor() {
        for (selected in listOf(plainTextFile, null)) {
            selectedFile = selected

            val messages = pressTheKeyOf(action, ownProject, otherMarkdownFile)

            assertTrue(messages.isEmpty())
            assertNull(standIns.service.currentTarget())
        }
        assertTrue(notifications.isEmpty())
        assertTrue(standIns.keptWorks.isEmpty())
    }

    @Test
    fun whileAFileIsReadTheKeyPausesAndResumesItWhereverTheFocusIs() {
        val target = ReadingTarget(ownProject, file)
        pressTheButtonOn(action, file, ownProject)

        for ((project, selected) in placesOfTheFocus()) {
            selectedFile = selected

            pressTheKeyOf(action, project, selected)
            assertTrue("Paused from $selected in ${project.name}", standIns.service.isPaused(target))

            pressTheKeyOf(action, project, selected)
            assertTrue("Resumed from $selected in ${project.name}", standIns.service.isReading(target))
            assertFalse("Resumed from $selected in ${project.name}", standIns.service.isPaused(target))
        }
        assertEquals(target, standIns.service.currentTarget())
    }

    @Test
    fun theKeyOfStopReadingStopsAReadingAndAPausedReadingWhereverTheFocusIs() {
        val target = ReadingTarget(ownProject, file)

        for ((project, selected) in placesOfTheFocus()) {
            selectedFile = selected
            for (paused in listOf(false, true)) {
                pressTheButtonOn(action, file, ownProject)
                if (paused) pressTheButtonOn(action, file, ownProject)
                assertTrue(isEnabledByKey(stop, project, selected))

                pressTheKeyOf(stop, project, selected)

                assertFalse("Stopped from $selected in ${project.name}", standIns.service.isReading(target))
            }
        }
    }

    @Test
    fun withNothingReadTheKeyOfStopReadingIsDisabledAndDoesNothing() {
        selectedFile = file

        assertFalse(isEnabledByKey(stop, ownProject, file))
        val messages = pressTheKeyOf(stop, ownProject, file)

        assertTrue(messages.isEmpty())
        assertNull(standIns.service.currentTarget())
    }

    @Test
    fun onTheTabTheButtonsDoWhatTheyDidBefore() {
        pressTheButtonOn(action, file, ownProject)

        assertEquals("Read Aloud" to AloudRiteIcons.ReadAloud, presentationFor(action, otherMarkdownFile, ownProject))
        assertEquals("Stop Reading" to AllIcons.Actions.Suspend, visiblePresentationFor(stop, file, ownProject))
        assertNull(visiblePresentationFor(stop, otherMarkdownFile, ownProject))

        pressTheButtonOn(action, otherMarkdownFile, ownProject)

        assertTrue(standIns.service.isReading(ReadingTarget(ownProject, otherMarkdownFile)))
        assertEquals("Read Aloud" to AloudRiteIcons.ReadAloud, presentationFor(action, file, ownProject))
        assertNull(visiblePresentationFor(stop, file, ownProject))
    }

    @Test
    fun afterEachKeyTheButtonsOnTheTabShowTheStateOfTheReading() {
        pressTheButtonOn(action, file, ownProject)
        selectedFile = otherMarkdownFile

        pressTheKeyOf(action, otherProject)
        assertEquals("Resume Reading" to AloudRiteIcons.ResumeReading, presentationFor(action, file, ownProject))
        assertEquals("Stop Reading" to AllIcons.Actions.Suspend, visiblePresentationFor(stop, file, ownProject))

        pressTheKeyOf(action, otherProject)
        assertEquals("Pause Reading" to AllIcons.Actions.Pause, presentationFor(action, file, ownProject))
        assertEquals("Stop Reading" to AllIcons.Actions.Suspend, visiblePresentationFor(stop, file, ownProject))

        pressTheKeyOf(stop, otherProject)
        assertEquals("Read Aloud" to AloudRiteIcons.ReadAloud, presentationFor(action, file, ownProject))
        assertNull(visiblePresentationFor(stop, file, ownProject))
    }

    @Test
    fun withNothingReadTheSpeedKeysChangeTheSpeedOneStepFromAnyProject() {
        for (project in listOf(ownProject, otherProject)) {
            setting.set(ReadingSpeed.NORMAL, afterAWait = false)

            pressTheKeyOf(faster, project, file)
            assertEquals(speed("1.05"), setting.speed)
            pressTheKeyOf(slower, project, file)
            pressTheKeyOf(slower, project, file)
            assertEquals(speed("0.95"), setting.speed)
        }
        assertTrue(standIns.textsMade.isEmpty())
    }

    @Test
    fun duringAReadingASpeedKeyMakesThePieceAgainAtTheNewSpeedAfterTheWaitFromAnyProject() {
        for (project in listOf(ownProject, otherProject)) {
            standIns.speedsMade.clear()
            setting.set(ReadingSpeed.NORMAL, afterAWait = false)
            pressTheButtonOn(action, oneSentenceFile, ownProject)
            standIns.runNextKeptWork()

            pressTheKeyOf(faster, project)
            standIns.runKeptWorks()
            assertEquals(speedsOf("1.0"), standIns.speedsMade)
            standIns.passTime(500)
            standIns.runKeptWorks()

            assertEquals(speedsOf("1.0", "1.05"), standIns.speedsMade.take(2))
            pressTheKeyOf(stop, ownProject)
        }
    }

    @Test
    fun atTheSlowestSpeedReadSlowerAndAtTheFastestReadFasterIsDisabledAndDoesNothing() {
        setting.set(ReadingSpeed.SLOWEST, afterAWait = false)
        assertFalse(isEnabledByKey(slower, ownProject))
        assertTrue(isEnabledByKey(faster, ownProject))
        pressTheKeyOf(slower, ownProject)
        assertEquals(ReadingSpeed.SLOWEST, setting.speed)

        setting.set(ReadingSpeed.FASTEST, afterAWait = false)
        assertFalse(isEnabledByKey(faster, ownProject))
        assertTrue(isEnabledByKey(slower, ownProject))
        pressTheKeyOf(faster, ownProject)
        assertEquals(ReadingSpeed.FASTEST, setting.speed)
    }

    @Test
    fun aPressOnFasterAndAKeyOfReadFaster300MillisecondsLaterMakeThePieceOnceAtTheSpeedOfTheSecond() {
        val panel = panel()
        pressTheButtonOn(action, oneSentenceFile, ownProject)
        standIns.runNextKeptWork()

        runInEdtAndWait { panel.fasterButton.doClick() }
        standIns.passTime(300)
        pressTheKeyOf(faster, otherProject)
        standIns.runKeptWorks()
        standIns.passTime(499)
        standIns.runKeptWorks()
        assertEquals(speedsOf("1.0"), standIns.speedsMade)
        standIns.passTime(1)
        standIns.runKeptWorks()

        assertEquals(speedsOf("1.0", "1.1"), standIns.speedsMade.take(2))
        assertFalse(speed("1.05").forTheEngine in standIns.speedsMade)
    }

    @Test
    fun aKeyOfReadFasterAndAPressOnFaster300MillisecondsLaterMakeThePieceOnceAtTheSpeedOfTheSecond() {
        val panel = panel()
        pressTheButtonOn(action, oneSentenceFile, ownProject)
        standIns.runNextKeptWork()

        pressTheKeyOf(faster, otherProject)
        standIns.passTime(300)
        runInEdtAndWait { panel.fasterButton.doClick() }
        standIns.runKeptWorks()
        standIns.passTime(499)
        standIns.runKeptWorks()
        assertEquals(speedsOf("1.0"), standIns.speedsMade)
        standIns.passTime(1)
        standIns.runKeptWorks()

        assertEquals(speedsOf("1.0", "1.1"), standIns.speedsMade.take(2))
        assertFalse(speed("1.05").forTheEngine in standIns.speedsMade)
    }

    @Test
    fun inTheDefaultKeymapsOfLinuxAndMacOsEachActionHasItsShortcutAloneAndNoOther() {
        // Every keymap and shortcut where something else is found, so that one run names all of them
        val found = DEFAULT_KEYMAPS.flatMap { keymapName ->
            val keymap = checkNotNull(KeymapManagerEx.getInstanceEx().getKeymap(keymapName)) { "No keymap $keymapName" }
            SHORTCUTS.mapNotNull { (actionId, keystroke) ->
                val shortcut = KeyboardShortcut(KeyStroke.getKeyStroke(keystroke), null)
                val shortcutsOfTheAction = keymap.getShortcuts(actionId).toList()
                val actionsOfTheShortcut = keymap.getActionIdList(shortcut)
                if (shortcutsOfTheAction == listOf(shortcut) && actionsOfTheShortcut == listOf(actionId)) null
                else "$keymapName: $actionId has $shortcutsOfTheAction, and $keystroke belongs to $actionsOfTheShortcut"
            }
        }

        assertEquals(emptyList<String>(), found)
    }

    @Test
    fun theTooltipsOfTheTabButtonsNameTheShortcutsOfTheirActions() {
        assertEquals("Read Aloud" to READ_ALOUD_KEY, tooltipOfReadAloud(file))

        pressTheButtonOn(action, file, ownProject)
        assertEquals("Pause Reading" to READ_ALOUD_KEY, tooltipOfReadAloud(file))
        assertEquals("Stop Reading" to STOP_KEY, titleAndShortcutOf(tooltipFor(stop, file, ownProject)!!))

        pressTheButtonOn(action, file, ownProject)
        assertEquals("Resume Reading" to READ_ALOUD_KEY, tooltipOfReadAloud(file))
        assertEquals("Stop Reading" to STOP_KEY, titleAndShortcutOf(tooltipFor(stop, file, ownProject)!!))
    }

    @Test
    fun onTheTabOfAnotherMarkdownFileReadAloudIsTheTitleAloneWhileAFileIsReadOrPaused() {
        pressTheButtonOn(action, file, ownProject)
        assertEquals("Read Aloud" to null, tooltipOfReadAloud(otherMarkdownFile))

        pressTheButtonOn(action, file, ownProject)
        assertEquals("Read Aloud" to null, tooltipOfReadAloud(otherMarkdownFile))
    }

    @Test
    fun theTooltipNamesAShortcutTheUserGaveTheFirstOfSeveralAndNoneWhereTheShortcutsAreRemoved() {
        val ofTheUser = KeyboardShortcut(KeyStroke.getKeyStroke(USER_KEYSTROKE), null)

        withAKeymapOfTheUser({ keymap ->
            keymap.removeAllActionShortcuts(READ_ALOUD_ACTION_ID)
            keymap.addShortcut(READ_ALOUD_ACTION_ID, ofTheUser)
        }) {
            assertEquals("Read Aloud" to shortcutText(USER_KEYSTROKE), tooltipOfReadAloud(file))
        }
        withAKeymapOfTheUser({ keymap -> keymap.addShortcut(READ_ALOUD_ACTION_ID, ofTheUser) }) {
            assertEquals("Read Aloud" to READ_ALOUD_KEY, tooltipOfReadAloud(file))
        }
        withAKeymapOfTheUser({ keymap -> keymap.removeAllActionShortcuts(READ_ALOUD_ACTION_ID) }) {
            assertEquals("Read Aloud" to null, tooltipOfReadAloud(file))
        }
    }

    // Each project and selected editor the focus can be in while a file of the own project is read: that file, another
    // Markdown file, one that is not Markdown and none, in the own project and in another
    private fun placesOfTheFocus(): List<Pair<Project, VirtualFile?>> =
        listOf(ownProject, otherProject).flatMap { project ->
            listOf(file, otherMarkdownFile, plainTextFile, null).map { selected -> project to selected }
        }

    private fun readingAction(modelsFolder: Path): ReadAloudAction =
        readAloudActionOn(standIns.service, idleDownloads, modelsFolder, selectedFile = { selectedFile })

    private fun tooltipOfReadAloud(onTheTabOf: VirtualFile): Pair<String, String?> =
        titleAndShortcutOf(tooltipFor(action, onTheTabOf, ownProject)!!)

    private fun panel(): ReadingPanel = runInEdtAndGet { ReadingPanel(ownProject, { standIns.service }, { setting }, { INT8_MODEL }) }

    private fun speed(typed: String): ReadingSpeed = checkNotNull(parsedSpeed(typed))

    private fun speedsOf(vararg typed: String): List<Float> = typed.map { speed(it).forTheEngine }

    private fun folderWithTheModelStandIn(): Path =
        temporaryFolder.newFolder().toPath().also { fillWithModelStandIn(INT8_MODEL.folderIn(it)) }

    companion object {
        // "Mac OS X 10.5+" is the keymap the IDE names "macOS"
        private val DEFAULT_KEYMAPS = listOf("Default for XWin", "Default for GNOME", "Default for KDE", "Mac OS X 10.5+")

        private val SHORTCUTS = listOf(
            READ_ALOUD_ACTION_ID to "ctrl alt shift SPACE",
            STOP_READING_ACTION_ID to "ctrl alt shift O",
            READ_SLOWER_ACTION_ID to "ctrl alt shift COMMA",
            READ_FASTER_ACTION_ID to "ctrl alt shift PERIOD",
        )

        // Read when a test asks, once the application is there
        private val READ_ALOUD_KEY get() = shortcutText("ctrl alt shift SPACE")
        private val STOP_KEY get() = shortcutText("ctrl alt shift O")
        private const val USER_KEYSTROKE = "ctrl alt shift F11"

        @JvmField
        @ClassRule
        val application = ApplicationRule()
    }
}
