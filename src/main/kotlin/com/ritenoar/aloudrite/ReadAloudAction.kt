package com.ritenoar.aloudrite

import com.intellij.icons.AllIcons
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import java.io.IOException
import java.nio.file.Path

// The first button on the tab, which acts on the file of its tab, and the action "Read Aloud, Pause or Resume" of the
// keymap, which acts on the one reading of the IDE and otherwise on the file of the selected editor
class ReadAloudAction(
    private val operatingSystemName: String,
    private val architectureName: String,
    private val modelsFolder: Path,
    private val modelInUse: () -> SpeechModel,
    private val readings: () -> Readings,
    private val downloads: () -> ModelDownloads,
    private val selectedFile: (Project) -> VirtualFile?,
) : AnAction() {

    constructor() : this(
        System.getProperty("os.name"),
        System.getProperty("os.arch"),
        userModelsFolder(),
        { service<SpeechModelSetting>().modelInUse },
        { service<SpeechService>() },
        { service<ModelDownloadService>() },
        { project -> FileEditorManager.getInstance(project).selectedEditor?.file },
    )

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    // Whether the selected file is Markdown is left to the press, which runs on the EDT, so that the update on the
    // background thread does not ask the editors of the project
    override fun update(event: AnActionEvent) {
        if (!isOnTheTab(event)) {
            event.presentation.isEnabledAndVisible = event.project != null
            event.presentation.text = templatePresentation.text
            event.presentation.icon = templatePresentation.icon
            return
        }
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE)
        event.presentation.isEnabledAndVisible = isMarkdown(file)
        if (file == null || !event.presentation.isVisible) return
        // The presentation of a toolbar button is kept between updates, so every state is set every time
        val target = ReadingTarget(event.project, file)
        when {
            !readings().isReading(target) -> {
                event.presentation.text = READ_ALOUD_TEXT
                event.presentation.icon = AloudRiteIcons.ReadAloud
            }
            readings().isPaused(target) -> {
                event.presentation.text = RESUME_READING_TEXT
                event.presentation.icon = AloudRiteIcons.ResumeReading
            }
            else -> {
                event.presentation.text = PAUSE_READING_TEXT
                event.presentation.icon = AllIcons.Actions.Pause
            }
        }
        // While another file is read, a press here reads this file and the shortcut pauses the other, so the tooltip
        // names no shortcut
        val actsAsTheShortcut = readings().currentTarget().let { it == null || it == target }
        event.presentation.putClientProperty(
            ActionButton.CUSTOM_HELP_TOOLTIP, shortcutTooltip(event.presentation.text, if (actsAsTheShortcut) ID else null))
    }

    override fun actionPerformed(event: AnActionEvent) {
        val pressedAtNanos = System.nanoTime()
        val target = (if (isOnTheTab(event)) targetOfTheTab(event) else targetOfTheKey(event)) ?: return
        val file = target.file
        // First, so that pausing and resuming always work
        if (readings().isReading(target)) {
            if (readings().isPaused(target)) return readings().resume(target, pressedAtNanos)
            return readings().pause(target)
        }
        if (!isSpeechSupported(operatingSystemName, architectureName)) {
            return Messages.showInfoMessage(event.project, SPEECH_NOT_SUPPORTED_MESSAGE, MESSAGE_TITLE)
        }
        val parts = try {
            spokenPartsOf(textAtThePress(file))
        } catch (exception: IOException) {
            return Messages.showErrorDialog(
                event.project, "AloudRite could not read ${file.name} aloud: ${exception.message}", MESSAGE_TITLE)
        }
        // Nothing is asked and nothing downloaded for a file that would read nothing: also one of nothing but
        // characters above U+FFFF, which have become spaces, and one of nothing but front matter, comments or struck
        // out text. One of nothing but a code block reads its words
        if (parts.none { it is Words && it.text.isNotBlank() }) return notifyNothingToRead(file)
        readOrOfferTheDownload(target, parts, pressedAtNanos)
    }

    // The reading there is, also a paused one and one of another project, and only without one the selected file
    private fun targetOfTheKey(event: AnActionEvent): ReadingTarget? {
        readings().currentTarget()?.let { return it }
        val project = event.project ?: return null
        val file = selectedFile(project)?.takeIf(::isMarkdown) ?: return null
        return ReadingTarget(project, file)
    }

    // The document holds the changes that are not saved. It is copied, so that a later change does not reach the
    // reading
    private fun textAtThePress(file: VirtualFile): String =
        runReadAction { FileDocumentManager.getInstance().getDocument(file)?.text } ?: VfsUtilCore.loadText(file)

    private fun notifyNothingToRead(file: VirtualFile) {
        Notification(NOTIFICATION_GROUP_ID, "There is nothing to read in ${file.name}.", NotificationType.INFORMATION).notify(null)
    }

    private fun readOrOfferTheDownload(target: ReadingTarget, parts: List<SpokenPart>, pressedAtNanos: Long) {
        val model = modelInUse()
        if (model.isInPlace(model.folderIn(modelsFolder))) {
            return readings().read(target, parts, model, pressedAtNanos)
        }
        // The progress in the status bar already says what is happening, so a press meanwhile says nothing
        if (downloads().isRunning) return
        val project = target.project
        ModelDownloadOffer(downloads(), modelsFolder, { question -> userWantsTheDownload(project, question) }) { message ->
            Messages.showInfoMessage(project, message, MESSAGE_TITLE)
        }.offer(
            model,
            downloadQuestion(model, model.folderIn(modelsFolder)),
            onAlreadyInPlace = { readings().read(target, parts, model, pressedAtNanos) },
            onAccepted = { readings().readOnceTheModelIsInPlace(target, parts, model, pressedAtNanos) },
            onFailureRetry = { readOrOfferTheDownload(target, parts, pressedAtNanos) },
        )
    }

    private fun userWantsTheDownload(project: Project?, question: String): Boolean = Messages.showYesNoDialog(
        project, question, MESSAGE_TITLE, "Download", "Not Now", Messages.getQuestionIcon()) == Messages.YES

    companion object {
        const val ID = "com.ritenoar.aloudrite.ReadAloud"

        private const val MESSAGE_TITLE = "AloudRite"

        // The text of the button on the tab, where the name of the action in the keymap would be too long
        private const val READ_ALOUD_TEXT = "Read Aloud"

        private const val PAUSE_READING_TEXT = "Pause Reading"

        private const val RESUME_READING_TEXT = "Resume Reading"

        private const val SPEECH_NOT_SUPPORTED_MESSAGE =
            "Speech is not supported on this platform. AloudRite speaks on Linux x64 and on macOS on Apple Silicon."
    }
}

// Compared by name so that the plugin needs no class of the Markdown plugin, which may be absent
fun isMarkdown(file: VirtualFile?): Boolean = file?.fileType?.name == "Markdown"
