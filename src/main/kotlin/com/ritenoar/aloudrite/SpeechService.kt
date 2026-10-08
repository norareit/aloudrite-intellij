package com.ritenoar.aloudrite

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.ui.Messages
import java.nio.file.Path

@Service(Service.Level.APP)
class SpeechService : Disposable {

    private val oneAtATime = OneAtATime { work -> ApplicationManager.getApplication().executeOnPooledThread(work) }

    @Volatile
    private var engine: KokoroSpeechEngine? = null

    fun speak(text: String, modelFolder: Path, pressedAtNanos: Long) {
        oneAtATime.tryRun { speakInBackground(text, modelFolder, pressedAtNanos) }
    }

    private fun speakInBackground(text: String, modelFolder: Path, pressedAtNanos: Long) {
        try {
            val speechEngine = engine ?: KokoroSpeechEngine(modelFolder).also { engine = it }
            AudioPlayer(speechEngine.sampleRate).use { player ->
                var isFirstPiece = true
                speechEngine.speak(text) { samples ->
                    if (isFirstPiece) {
                        isFirstPiece = false
                        logTimeToFirstSound(pressedAtNanos)
                    }
                    player.play(samples)
                }
            }
        } catch (exception: Throwable) {
            LOG.warn("AloudRite could not speak", exception)
            showFailure(exception)
        }
    }

    private fun logTimeToFirstSound(pressedAtNanos: Long) {
        val milliseconds = (System.nanoTime() - pressedAtNanos) / NANOSECONDS_PER_MILLISECOND
        LOG.info("First sound $milliseconds ms after the button was pressed")
    }

    private fun showFailure(exception: Throwable) {
        ApplicationManager.getApplication().invokeLater {
            Messages.showErrorDialog("AloudRite could not speak: ${exception.message}", "AloudRite")
        }
    }

    override fun dispose() {
        // Releasing an engine that another thread is using is a fault in native code. The service is disposed only
        // when the IDE closes or the plugin is unloaded, so a sentence that is still being made is left alone
        if (oneAtATime.isRunning) return
        engine?.close()
        engine = null
    }

    private companion object {
        val LOG = Logger.getInstance(SpeechService::class.java)
        const val NANOSECONDS_PER_MILLISECOND = 1_000_000L
    }
}
