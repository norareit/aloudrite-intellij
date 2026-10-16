package com.ritenoar.aloudrite

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.Messages
import java.io.IOException
import java.nio.file.Path

class ReadAloudAction(
    private val operatingSystemName: String,
    private val architectureName: String,
    private val modelFolder: Path,
    private val speak: (text: String, modelFolder: Path, pressedAtNanos: Long) -> Unit,
    private val downloads: () -> ModelDownloads,
) : AnAction() {

    constructor() : this(
        System.getProperty("os.name"),
        System.getProperty("os.arch"),
        userModelFolder(System.getProperty("os.name"), System.getProperty("user.home"), System.getenv("XDG_DATA_HOME")),
        { text, folder, pressedAtNanos -> service<SpeechService>().speak(text, folder, pressedAtNanos) },
        { service<ModelDownloadService>() },
    )

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE)
        event.presentation.isEnabledAndVisible = file?.fileType?.name == MARKDOWN_FILE_TYPE_NAME
    }

    override fun actionPerformed(event: AnActionEvent) {
        val pressedAtNanos = System.nanoTime()
        if (!isSpeechSupported(operatingSystemName, architectureName)) {
            return showMessage(event, SPEECH_NOT_SUPPORTED_MESSAGE)
        }
        if (isModelInPlace(modelFolder)) {
            return speak(TEST_SENTENCE, modelFolder, pressedAtNanos)
        }
        // The progress in the status bar already says what is happening, so a press meanwhile says nothing
        if (downloads().isRunning) return
        offerTheDownload(event, pressedAtNanos)
    }

    // The lock of the installation is taken before the question, so that nothing is asked while another IDE window
    // downloads, and it is held until the download ends or the user declines
    private fun offerTheDownload(event: AnActionEvent, pressedAtNanos: Long) {
        val attempt = try {
            downloads().claim(modelFolder)
        } catch (exception: IOException) {
            return showMessage(event, failureText(InstallFailure.Unexpected(exception.message ?: exception.toString())))
        } ?: return showMessage(event, DOWNLOAD_RUNNING_ELSEWHERE_MESSAGE)
        var handedOver = false
        try {
            // Another IDE window may have finished between the first look and the lock
            if (isModelInPlace(modelFolder)) {
                attempt.close()
                return speak(TEST_SENTENCE, modelFolder, pressedAtNanos)
            }
            if (!userWantsTheDownload(event)) return
            handedOver = true
            downloads().start(modelFolder, attempt) { speak(TEST_SENTENCE, modelFolder, pressedAtNanos) }
        } finally {
            if (!handedOver) attempt.close()
        }
    }

    private fun userWantsTheDownload(event: AnActionEvent): Boolean = Messages.showYesNoDialog(
        event.project, downloadQuestion(KOKORO_MODEL_PACKAGE, modelFolder), MESSAGE_TITLE, "Download", "Not Now",
        Messages.getQuestionIcon()) == Messages.YES

    private fun showMessage(event: AnActionEvent, message: String) {
        Messages.showInfoMessage(event.project, message, MESSAGE_TITLE)
    }

    private companion object {
        // Compared by name so that the plugin needs no class of the Markdown plugin, which may be absent
        const val MARKDOWN_FILE_TYPE_NAME = "Markdown"

        const val MESSAGE_TITLE = "AloudRite"

        const val SPEECH_NOT_SUPPORTED_MESSAGE =
            "Speech is not supported on this platform. AloudRite speaks on Linux x64 and on macOS on Apple Silicon."

        // A placeholder for the text of the file, which a later unit reads aloud in its place
        const val TEST_SENTENCE = "AloudRite can speak. This sentence was made on your own machine."
    }
}
