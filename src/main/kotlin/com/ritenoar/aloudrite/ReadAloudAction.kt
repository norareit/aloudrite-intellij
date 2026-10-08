package com.ritenoar.aloudrite

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.Messages
import java.nio.file.Path

class ReadAloudAction(
    private val operatingSystemName: String,
    private val architectureName: String,
    private val modelFolder: Path,
    private val speak: (text: String, modelFolder: Path, pressedAtNanos: Long) -> Unit,
) : AnAction() {

    constructor() : this(
        System.getProperty("os.name"),
        System.getProperty("os.arch"),
        userModelFolder(System.getProperty("os.name"), System.getProperty("user.home"), System.getenv("XDG_DATA_HOME")),
        { text, folder, pressedAtNanos -> service<SpeechService>().speak(text, folder, pressedAtNanos) },
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
        if (!isModelInPlace(modelFolder)) {
            return showMessage(event, modelMissingMessage(modelFolder))
        }
        speak(TEST_SENTENCE, modelFolder, pressedAtNanos)
    }

    private fun showMessage(event: AnActionEvent, message: String) {
        Messages.showInfoMessage(event.project, message, MESSAGE_TITLE)
    }

    private fun modelMissingMessage(folder: Path) =
        "The speech model is missing. AloudRite looks for it in ${folder.toAbsolutePath()}. " +
            "The README of AloudRite says which model package to get and how to put it there."

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
