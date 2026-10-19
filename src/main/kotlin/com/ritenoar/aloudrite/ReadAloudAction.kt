package com.ritenoar.aloudrite

import com.intellij.icons.AllIcons
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import java.io.IOException
import java.nio.file.Path

class ReadAloudAction(
    private val operatingSystemName: String,
    private val architectureName: String,
    private val modelFolder: Path,
    private val readings: () -> Readings,
    private val downloads: () -> ModelDownloads,
) : AnAction() {

    constructor() : this(
        System.getProperty("os.name"),
        System.getProperty("os.arch"),
        userModelFolder(System.getProperty("os.name"), System.getProperty("user.home"), System.getenv("XDG_DATA_HOME")),
        { service<SpeechService>() },
        { service<ModelDownloadService>() },
    )

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE)
        event.presentation.isEnabledAndVisible = file?.fileType?.name == MARKDOWN_FILE_TYPE_NAME
        if (file == null || !event.presentation.isVisible) return
        // The presentation of a toolbar button is kept between updates, so both states are set every time
        if (readings().isReading(ReadingTarget(event.project, file))) {
            event.presentation.text = STOP_READING_TEXT
            event.presentation.icon = AllIcons.Actions.Suspend
        } else {
            event.presentation.text = templatePresentation.text
            event.presentation.icon = templatePresentation.icon
        }
    }

    override fun actionPerformed(event: AnActionEvent) {
        val pressedAtNanos = System.nanoTime()
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val target = ReadingTarget(event.project, file)
        // First, so that the stop button always works
        if (readings().isReading(target)) return readings().stop(target)
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

    // The document holds the changes that are not saved. It is copied, so that a later change does not reach the
    // reading
    private fun textAtThePress(file: VirtualFile): String =
        runReadAction { FileDocumentManager.getInstance().getDocument(file)?.text } ?: VfsUtilCore.loadText(file)

    private fun notifyNothingToRead(file: VirtualFile) {
        Notification(NOTIFICATION_GROUP_ID, "There is nothing to read in ${file.name}.", NotificationType.INFORMATION).notify(null)
    }

    private fun readOrOfferTheDownload(target: ReadingTarget, parts: List<SpokenPart>, pressedAtNanos: Long) {
        if (isModelInPlace(modelFolder)) {
            return readings().read(target, parts, modelFolder, pressedAtNanos)
        }
        // The progress in the status bar already says what is happening, so a press meanwhile says nothing
        if (downloads().isRunning) return
        offerTheDownload(target, parts, pressedAtNanos)
    }

    // The lock of the installation is taken before the question, so that nothing is asked while another IDE window
    // downloads, and it is held until the download ends or the user declines
    private fun offerTheDownload(target: ReadingTarget, parts: List<SpokenPart>, pressedAtNanos: Long) {
        val project = target.project
        val attempt = try {
            downloads().claim(modelFolder)
        } catch (exception: IOException) {
            // The retry asks the question again, since nothing has been agreed to yet
            return downloads().report(unexpectedFailure(exception)) { readOrOfferTheDownload(target, parts, pressedAtNanos) }
        } ?: return Messages.showInfoMessage(project, DOWNLOAD_RUNNING_ELSEWHERE_MESSAGE, MESSAGE_TITLE)
        var handedOver = false
        try {
            // Another IDE window may have finished between the first look and the lock
            if (isModelInPlace(modelFolder)) {
                attempt.close()
                return readings().read(target, parts, modelFolder, pressedAtNanos)
            }
            if (!userWantsTheDownload(project)) return
            val readOnceTheModelIsInPlace = readings().readOnceTheModelIsInPlace(target, parts, modelFolder, pressedAtNanos)
            handedOver = true
            downloads().start(modelFolder, attempt, readOnceTheModelIsInPlace)
        } finally {
            if (!handedOver) attempt.close()
        }
    }

    private fun userWantsTheDownload(project: Project?): Boolean = Messages.showYesNoDialog(
        project, downloadQuestion(KOKORO_MODEL_PACKAGE, modelFolder), MESSAGE_TITLE, "Download", "Not Now",
        Messages.getQuestionIcon()) == Messages.YES

    private companion object {
        // Compared by name so that the plugin needs no class of the Markdown plugin, which may be absent
        const val MARKDOWN_FILE_TYPE_NAME = "Markdown"

        const val MESSAGE_TITLE = "AloudRite"

        const val STOP_READING_TEXT = "Stop Reading"

        const val SPEECH_NOT_SUPPORTED_MESSAGE =
            "Speech is not supported on this platform. AloudRite speaks on Linux x64 and on macOS on Apple Silicon."
    }
}
