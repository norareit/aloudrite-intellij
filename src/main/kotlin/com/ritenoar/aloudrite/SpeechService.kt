package com.ritenoar.aloudrite

import com.intellij.ide.ActivityTracker
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import java.nio.file.Path
import java.util.concurrent.Semaphore

// What the action needs of the reading, and nothing more
interface Readings {
    fun isReading(target: ReadingTarget): Boolean

    // Stops the reading of any other target. Returns at once, the reading is done in the background
    fun read(target: ReadingTarget, text: String, modelFolder: Path, pressedAtNanos: Long)

    fun stop(target: ReadingTarget)

    // Gives what reads the text once the model is in place. It reads only while this is the reading that waits, which
    // a later call of this replaces and closing the file or its project forgets
    fun readOnceTheModelIsInPlace(target: ReadingTarget, text: String, modelFolder: Path, pressedAtNanos: Long): () -> Unit
}

// The collaborators are handed in so that a test can run the service without the model and without an audio device
@Service(Service.Level.APP)
class SpeechService(
    private val runInBackground: (() -> Unit) -> Unit,
    private val createEngine: (modelFolder: Path) -> SpeechEngine,
    private val createPlayer: (sampleRate: Int, onFirstWrite: () -> Unit) -> SamplePlayer,
    private val reportFirstSound: (milliseconds: Long) -> Unit,
    private val reportFailure: (target: ReadingTarget, failure: Throwable) -> Unit,
    private val onChange: () -> Unit,
) : Readings, Disposable {

    constructor() : this(
        { work -> ApplicationManager.getApplication().executeOnPooledThread(work) },
        { modelFolder -> KokoroSpeechEngine(modelFolder) },
        { sampleRate, onFirstWrite -> AudioPlayer.open(sampleRate, onFirstWrite) },
        { milliseconds -> LOG.info("First sound $milliseconds ms after the button was pressed") },
        ::logAndShowFailure,
        { ActivityTracker.getInstance().inc() },
    )

    // The engine is not made for two callers, so the engine work of a reading holds this for the whole reading. It
    // is not reentrant, so that dispose, called while making the audio on the same thread, sees it held
    private val engineInUse = Semaphore(1)

    @Volatile
    private var engine: SpeechEngine? = null

    // Both are changed only under the monitor of the service
    private var current: Reading? = null
    private var waiting: WaitingReading? = null

    override fun isReading(target: ReadingTarget): Boolean = synchronized(this) { current?.target == target }

    override fun read(target: ReadingTarget, text: String, modelFolder: Path, pressedAtNanos: Long) {
        val reading = Reading(target, text, modelFolder, pressedAtNanos)
        val previous = synchronized(this) { current.also { current = reading } }
        previous?.stop()
        onChange()
        runInBackground { makeTheAudio(reading) }
    }

    override fun stop(target: ReadingTarget) {
        stopWhere { it.target == target }
    }

    override fun readOnceTheModelIsInPlace(
        target: ReadingTarget, text: String, modelFolder: Path, pressedAtNanos: Long,
    ): () -> Unit {
        val waitingReading = WaitingReading(target)
        synchronized(this) { waiting = waitingReading }
        return {
            val stillWaiting = synchronized(this) {
                (waiting === waitingReading).also { if (it) waiting = null }
            }
            if (stillWaiting) read(target, text, modelFolder, pressedAtNanos)
        }
    }

    fun fileClosed(target: ReadingTarget) {
        forgetWhere { it == target }
    }

    fun projectClosed(project: Project?) {
        forgetWhere { it.project == project }
    }

    private fun forgetWhere(matches: (ReadingTarget) -> Boolean) {
        synchronized(this) { if (waiting?.target?.let(matches) == true) waiting = null }
        stopWhere { matches(it.target) }
    }

    private fun stopWhere(matches: (Reading) -> Boolean) {
        val stopped = synchronized(this) {
            current?.takeIf(matches)?.also { current = null }
        } ?: return
        stopped.stop()
        onChange()
    }

    // Ends a reading that ended by itself, at its end or by a failure. Returns false where it was stopped before
    private fun end(reading: Reading): Boolean {
        val wasCurrent = synchronized(this) {
            (current === reading).also { if (it) current = null }
        }
        if (wasCurrent) onChange()
        return wasCurrent
    }

    private fun fail(reading: Reading, failure: Throwable) {
        reading.stop()
        if (end(reading)) reportFailure(reading.target, failure)
    }

    private fun makeTheAudio(reading: Reading) {
        engineInUse.acquireUninterruptibly()
        try {
            if (reading.isStopped) return
            val speechEngine = engine ?: createEngine(reading.modelFolder).also { engine = it }
            val audioAhead = AudioAhead(speechEngine.sampleRate * SECONDS_AHEAD)
            reading.playFrom(audioAhead)
            runInBackground { play(reading, audioAhead, speechEngine.sampleRate) }
            for (piece in piecesOf(reading.text)) {
                if (reading.isStopped) break
                if (piece.isBlank()) continue
                speechEngine.speak(piece) { samples -> audioAhead.put(samples) }
            }
            audioAhead.finish()
        } catch (exception: Throwable) {
            fail(reading, exception)
        } finally {
            engineInUse.release()
        }
    }

    private fun play(reading: Reading, audioAhead: AudioAhead, sampleRate: Int) {
        val onFirstWrite = {
            reportFirstSound((System.nanoTime() - reading.pressedAtNanos) / NANOSECONDS_PER_MILLISECOND)
        }
        val player = try {
            createPlayer(sampleRate, onFirstWrite)
        } catch (exception: Throwable) {
            return fail(reading, exception)
        }
        try {
            playUntilTheEndOrAStop(reading, audioAhead, player, sampleRate / SLICES_PER_SECOND)
        } catch (exception: Throwable) {
            runCatching { player.discard() }
            return fail(reading, exception)
        }
        if (reading.isStopped) return player.discard()
        player.close()
        end(reading)
    }

    // The stop is looked at between two slices, so that a stop ends the sound after at most one slice
    private fun playUntilTheEndOrAStop(reading: Reading, audioAhead: AudioAhead, player: SamplePlayer, sliceLength: Int) {
        while (true) {
            val samples = audioAhead.take() ?: return
            for (start in samples.indices step sliceLength) {
                if (reading.isStopped) return
                player.play(samples.copyOfRange(start, minOf(start + sliceLength, samples.size)))
            }
        }
    }

    override fun dispose() {
        val stopped = synchronized(this) {
            waiting = null
            current.also { current = null }
        }
        stopped?.stop()
        // Releasing an engine that another thread is using is a fault in native code. The service is disposed only
        // when the IDE closes or the plugin is unloaded, so an engine that is making audio is left to the process
        if (!engineInUse.tryAcquire()) return
        try {
            engine?.close()
            engine = null
        } finally {
            engineInUse.release()
        }
    }

    private class Reading(
        val target: ReadingTarget,
        val text: String,
        val modelFolder: Path,
        val pressedAtNanos: Long,
    ) {
        private var stopped = false
        private var audioAhead: AudioAhead? = null

        val isStopped: Boolean get() = synchronized(this) { stopped }

        // A stop that came before the audio had somewhere to go cancels it here
        fun playFrom(ahead: AudioAhead) {
            synchronized(this) {
                audioAhead = ahead
                if (stopped) ahead.cancel()
            }
        }

        fun stop() {
            synchronized(this) {
                stopped = true
                audioAhead?.cancel()
            }
        }
    }

    // Compared by identity, so that a callback of an earlier wait never reads in place of a later one
    private class WaitingReading(val target: ReadingTarget)

    private companion object {
        val LOG = Logger.getInstance(SpeechService::class.java)
        const val NANOSECONDS_PER_MILLISECOND = 1_000_000L
        const val SECONDS_AHEAD = 30
        const val SLICES_PER_SECOND = 10

        // A warning is not an error report of the IDE, and an error would be one
        fun logAndShowFailure(target: ReadingTarget, failure: Throwable) {
            LOG.warn("AloudRite could not read ${target.file.name} aloud", failure)
            ApplicationManager.getApplication().invokeLater {
                val project = target.project?.takeUnless { it.isDisposed }
                Messages.showErrorDialog(
                    project, "AloudRite could not read ${target.file.name} aloud: ${failure.message}", "AloudRite")
            }
        }
    }
}
