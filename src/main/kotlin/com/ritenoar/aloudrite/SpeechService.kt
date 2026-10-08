package com.ritenoar.aloudrite

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.ui.Messages
import java.nio.file.Path

// The collaborators are handed in so that a test can run the service without the model and without an audio device
@Service(Service.Level.APP)
class SpeechService(
    runInBackground: (() -> Unit) -> Unit,
    private val createEngine: (modelFolder: Path) -> SpeechEngine,
    private val createPlayer: (sampleRate: Int, onFirstWrite: () -> Unit) -> SamplePlayer,
    private val reportFirstSound: (milliseconds: Long) -> Unit,
    private val reportFailure: (Throwable) -> Unit,
) : Disposable {

    constructor() : this(
        { work -> ApplicationManager.getApplication().executeOnPooledThread(work) },
        { modelFolder -> KokoroSpeechEngine(modelFolder) },
        { sampleRate, onFirstWrite -> AudioPlayer.open(sampleRate, onFirstWrite) },
        { milliseconds -> LOG.info("First sound $milliseconds ms after the button was pressed") },
        ::logAndShowFailure,
    )

    private val oneAtATime = OneAtATime(runInBackground)

    @Volatile
    private var engine: SpeechEngine? = null

    fun speak(text: String, modelFolder: Path, pressedAtNanos: Long) {
        oneAtATime.tryRun { speakInBackground(text, modelFolder, pressedAtNanos) }
    }

    private fun speakInBackground(text: String, modelFolder: Path, pressedAtNanos: Long) {
        try {
            val speechEngine = engine ?: createEngine(modelFolder).also { engine = it }
            val onFirstWrite = { reportFirstSound((System.nanoTime() - pressedAtNanos) / NANOSECONDS_PER_MILLISECOND) }
            createPlayer(speechEngine.sampleRate, onFirstWrite).use { player ->
                speechEngine.speak(text) { samples -> player.play(samples) }
            }
        } catch (exception: Throwable) {
            reportFailure(exception)
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

        // A warning is not an error report of the IDE, and an error would be one
        fun logAndShowFailure(exception: Throwable) {
            LOG.warn("AloudRite could not speak", exception)
            ApplicationManager.getApplication().invokeLater {
                Messages.showErrorDialog("AloudRite could not speak: ${exception.message}", "AloudRite")
            }
        }
    }
}
