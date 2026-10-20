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
    fun read(target: ReadingTarget, parts: List<SpokenPart>, modelFolder: Path, pressedAtNanos: Long)

    fun stop(target: ReadingTarget)

    // Gives what reads the parts once the model is in place. It reads only while this is the reading that waits, which
    // a later call of this replaces and closing the file or its project forgets
    fun readOnceTheModelIsInPlace(
        target: ReadingTarget, parts: List<SpokenPart>, modelFolder: Path, pressedAtNanos: Long,
    ): () -> Unit
}

// What the tool window needs of the reading, and nothing more
interface CurrentReading {
    fun currentTarget(): ReadingTarget?

    fun stop(target: ReadingTarget)
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
) : Readings, CurrentReading, Disposable {

    constructor() : this(
        { work -> ApplicationManager.getApplication().executeOnPooledThread(work) },
        ::createKokoroEngine,
        { sampleRate, onFirstWrite -> AudioPlayer.open(sampleRate, onFirstWrite) },
        { milliseconds -> LOG.info("First sound $milliseconds ms after the button was pressed") },
        ::logAndShowFailure,
        {
            ActivityTracker.getInstance().inc()
            publishReadingChange()
        },
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

    override fun currentTarget(): ReadingTarget? = synchronized(this) { current?.target }

    override fun read(target: ReadingTarget, parts: List<SpokenPart>, modelFolder: Path, pressedAtNanos: Long) {
        val reading = Reading(target, parts, modelFolder, pressedAtNanos)
        val previous = synchronized(this) { current.also { current = reading } }
        previous?.stop()
        onChange()
        runInBackground { makeTheAudio(reading) }
    }

    override fun stop(target: ReadingTarget) {
        stopWhere { it.target == target }
    }

    override fun readOnceTheModelIsInPlace(
        target: ReadingTarget, parts: List<SpokenPart>, modelFolder: Path, pressedAtNanos: Long,
    ): () -> Unit {
        val waitingReading = WaitingReading(target)
        synchronized(this) { waiting = waitingReading }
        return {
            val stillWaiting = synchronized(this) {
                (waiting === waitingReading).also { if (it) waiting = null }
            }
            if (stillWaiting) read(target, parts, modelFolder, pressedAtNanos)
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
            for (part in reading.parts) {
                if (reading.isStopped) break
                if (!makeTheAudioOf(part, reading, speechEngine, audioAhead)) break
            }
            audioAhead.finish()
        } catch (exception: Throwable) {
            fail(reading, exception)
        } finally {
            engineInUse.release()
        }
    }

    // Returns false where the audio ahead was cancelled
    private fun makeTheAudioOf(part: SpokenPart, reading: Reading, speechEngine: SpeechEngine, audioAhead: AudioAhead) =
        when (part) {
            is Words -> {
                for (piece in piecesOf(part.text)) {
                    if (reading.isStopped) break
                    if (piece.isBlank()) continue
                    speechEngine.speak(piece) { samples -> audioAhead.put(samples) }
                }
                true
            }
            is Silence -> audioAhead.put(FloatArray(speechEngine.sampleRate * part.milliseconds / MILLISECONDS_PER_SECOND))
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
        if (!reading.playsOutOn(player)) return player.discard()
        // Closing plays out what the line still holds, and a failure of the audio output can come then as well
        try {
            player.close()
        } catch (exception: Throwable) {
            return fail(reading, exception)
        }
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
        val parts: List<SpokenPart>,
        val modelFolder: Path,
        val pressedAtNanos: Long,
    ) {
        private var stopped = false
        private var audioAhead: AudioAhead? = null
        private var playingOut: SamplePlayer? = null

        val isStopped: Boolean get() = synchronized(this) { stopped }

        // A stop that came before the audio had somewhere to go cancels it here
        fun playFrom(ahead: AudioAhead) {
            synchronized(this) {
                audioAhead = ahead
                if (stopped) ahead.cancel()
            }
        }

        // Returns false where the reading was stopped already. A stop that comes later discards the player, which
        // ends the sound that the line still holds
        fun playsOutOn(player: SamplePlayer): Boolean = synchronized(this) {
            if (!stopped) playingOut = player
            !stopped
        }

        // The player is discarded outside the monitor, since stopping a line can take a moment. A failure of it is
        // not reported: the reading is over, and the playing thread reports nothing for a reading that was stopped
        fun stop() {
            val player = synchronized(this) {
                stopped = true
                audioAhead?.cancel()
                playingOut
            }
            player?.let { runCatching { it.discard() } }
        }
    }

    // Compared by identity, so that a callback of an earlier wait never reads in place of a later one
    private class WaitingReading(val target: ReadingTarget)

    private companion object {
        val LOG = Logger.getInstance(SpeechService::class.java)
        const val NANOSECONDS_PER_MILLISECOND = 1_000_000L
        const val MILLISECONDS_PER_SECOND = 1_000
        const val SECONDS_AHEAD = 30
        const val SLICES_PER_SECOND = 10

        // The processors are asked at each creation, so that a limit on the IDE's process, such as that of a
        // container, is followed
        fun createKokoroEngine(modelFolder: Path): SpeechEngine {
            val processors = Runtime.getRuntime().availableProcessors()
            val threads = engineThreadCount(processors)
            val engine = KokoroSpeechEngine(modelFolder, threads)
            LOG.info("Created the speech engine with $threads threads, of $processors processors available")
            return engine
        }

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
