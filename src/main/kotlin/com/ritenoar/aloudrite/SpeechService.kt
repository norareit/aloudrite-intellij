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
    // Also while the reading is paused
    fun isReading(target: ReadingTarget): Boolean

    fun isPaused(target: ReadingTarget): Boolean

    // Stops the reading of any other target. Returns at once, the reading is done in the background
    fun read(target: ReadingTarget, parts: List<SpokenPart>, modelFolder: Path, pressedAtNanos: Long)

    fun stop(target: ReadingTarget)

    fun pause(target: ReadingTarget)

    fun resume(target: ReadingTarget, pressedAtNanos: Long)

    // Gives what reads the parts once the model is in place. It reads only while this is the reading that waits, which
    // a later call of this replaces and closing the file or its project forgets
    fun readOnceTheModelIsInPlace(
        target: ReadingTarget, parts: List<SpokenPart>, modelFolder: Path, pressedAtNanos: Long,
    ): () -> Unit
}

// What the tool window needs of the reading, and nothing more
interface CurrentReading {
    // Also while the reading is paused
    fun currentTarget(): ReadingTarget?

    fun isPaused(target: ReadingTarget): Boolean

    fun stop(target: ReadingTarget)

    fun pause(target: ReadingTarget)

    fun resume(target: ReadingTarget, pressedAtNanos: Long)
}

// The collaborators are handed in so that a test can run the service without the model and without an audio device
@Service(Service.Level.APP)
class SpeechService(
    private val runInBackground: (() -> Unit) -> Unit,
    private val createEngine: (modelFolder: Path) -> SpeechEngine,
    private val createPlayer: (sampleRate: Int, onFirstWrite: () -> Unit) -> SamplePlayer,
    private val reportFirstSound: (milliseconds: Long, afterResume: Boolean) -> Unit,
    private val reportFailure: (target: ReadingTarget, failure: Throwable) -> Unit,
    private val onChange: () -> Unit,
) : Readings, CurrentReading, Disposable {

    constructor() : this(
        { work -> ApplicationManager.getApplication().executeOnPooledThread(work) },
        ::createKokoroEngine,
        { sampleRate, onFirstWrite -> AudioPlayer.open(sampleRate, onFirstWrite) },
        { milliseconds, afterResume ->
            val press = if (afterResume) "Resume Reading" else "the button"
            LOG.info("First sound $milliseconds ms after $press was pressed")
        },
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

    override fun isPaused(target: ReadingTarget): Boolean = currentReadingOf(target)?.isPaused == true

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

    override fun pause(target: ReadingTarget) {
        val reading = currentReadingOf(target) ?: return
        if (reading.pause()) onChange()
    }

    // A playing work that has not yet seen the pause plays the resumption by itself, and one that ended at the pause
    // is replaced
    override fun resume(target: ReadingTarget, pressedAtNanos: Long) {
        val reading = currentReadingOf(target) ?: return
        val resumed = reading.resume(pressedAtNanos)
        if (resumed == Resumed.NOT) return
        onChange()
        if (resumed == Resumed.BY_A_NEW_PLAYING_WORK) runInBackground { play(reading) }
    }

    private fun currentReadingOf(target: ReadingTarget): Reading? =
        synchronized(this) { current?.takeIf { it.target == target } }

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
            reading.playFrom(Playback(audioAhead, speechEngine.sampleRate))
            runInBackground { play(reading) }
            for ((number, segment) in reading.parts.flatMap(::segmentsOf).withIndex()) {
                if (reading.isStopped) break
                if (!makeTheAudioOf(segment, number, speechEngine, audioAhead)) break
            }
            audioAhead.finish()
        } catch (exception: Throwable) {
            fail(reading, exception)
        } finally {
            engineInUse.release()
        }
    }

    // A segment is one piece of words, as the plugin cuts the text, or one silence. A resume begins at a segment
    private fun segmentsOf(part: SpokenPart): List<SpokenPart> = when (part) {
        is Words -> piecesOf(part.text).filterNot { it.isBlank() }.map(::Words)
        is Silence -> listOf(part)
    }

    // Returns false where the audio ahead was cancelled. A cancel during the words is seen by the caller as the stop
    private fun makeTheAudioOf(segment: SpokenPart, number: Int, speechEngine: SpeechEngine, audioAhead: AudioAhead) =
        when (segment) {
            is Words -> {
                speechEngine.speak(segment.text) { samples -> audioAhead.put(AudioRun(number, isSilence = false, samples)) }
                true
            }
            is Silence -> audioAhead.put(
                AudioRun(number, isSilence = true, FloatArray(speechEngine.sampleRate * segment.milliseconds / MILLISECONDS_PER_SECOND)))
        }

    // A pause ends the playing work, unless a resume came before the work stored where to resume
    private fun play(reading: Reading) {
        var resumption = reading.takeResumption()
        while (true) {
            resumption = playUntilAPause(reading, resumption) ?: return
            if (!reading.parkAt(resumption)) return
        }
    }

    // Returns where a resume begins where the reading was paused, and null where it ended
    private fun playUntilAPause(reading: Reading, resumption: Resumption): Resumption? {
        if (reading.interruption() == Ending.PAUSE) return resumption
        val playback = reading.playback()
        val press = reading.lastPress()
        val onFirstWrite = {
            reportFirstSound((System.nanoTime() - press.atNanos) / NANOSECONDS_PER_MILLISECOND, press.isResume)
        }
        val player = try {
            createPlayer(playback.sampleRate, onFirstWrite)
        } catch (exception: Throwable) {
            fail(reading, exception)
            return null
        }
        val runs = RunsToPlay(resumption, playback.audioAhead)
        val playedAudio = PlayedAudio()
        val ending = try {
            playUntilTheEndOrAPauseOrAStop(reading, runs, playedAudio, player, playback.sampleRate / SLICES_PER_SECOND)
        } catch (exception: Throwable) {
            runCatching { player.discard() }
            fail(reading, exception)
            return null
        }
        return when (ending) {
            Ending.STOP -> null.also { player.discard() }
            Ending.PAUSE -> pausedOn(player, runs, playedAudio)
            Ending.END -> playOut(reading, player, runs, playedAudio)
        }
    }

    // The position is taken before the discard, which ends the sound at once
    private fun pausedOn(player: SamplePlayer, runs: RunsToPlay, playedAudio: PlayedAudio): Resumption {
        val samplesHeard = player.samplesHeard()
        runCatching { player.discard() }
        return runs.resumptionAfter(playedAudio.resumptionAt(samplesHeard))
    }

    // Closing plays out what the line still holds, and a failure of the audio output can come then as well. A pause
    // meanwhile discards the line from the pressing thread, and what the discarded line throws is no failure
    private fun playOut(reading: Reading, player: SamplePlayer, runs: RunsToPlay, playedAudio: PlayedAudio): Resumption? {
        if (!reading.playsOutOn(player)) {
            if (reading.isStopped) return null.also { player.discard() }
            return pausedOn(player, runs, playedAudio)
        }
        val failure = runCatching { player.close() }.exceptionOrNull()
        val samplesHeardAtAPause = reading.playedOut()
        if (samplesHeardAtAPause != null) return runs.resumptionAfter(playedAudio.resumptionAt(samplesHeardAtAPause))
        if (failure != null) {
            fail(reading, failure)
            return null
        }
        end(reading)
        return null
    }

    // A stop and a pause are looked at between two slices, so that either ends the sound after at most one slice, and
    // a pause wakes the wait for the next run. A run is kept by playedAudio as soon as it is taken, so that a pause
    // right after the take plays it at the resume
    private fun playUntilTheEndOrAPauseOrAStop(
        reading: Reading, runs: RunsToPlay, playedAudio: PlayedAudio, player: SamplePlayer, sliceLength: Int,
    ): Ending {
        while (true) {
            val taken = runs.next()
            if (taken is Taken.Run) playedAudio.taken(taken.run, player.samplesHeard())
            reading.interruption()?.let { return it }
            val samples = when (taken) {
                is Taken.Run -> taken.run.samples
                Taken.End -> return Ending.END
                Taken.Woken -> continue
            }
            for (start in samples.indices step sliceLength) {
                reading.interruption()?.let { return it }
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

    private enum class Ending { END, PAUSE, STOP }

    private enum class Resumed { NOT, BY_THE_PLAYING_WORK, BY_A_NEW_PLAYING_WORK }

    private class Playback(val audioAhead: AudioAhead, val sampleRate: Int)

    // The press that the next first sound is measured from
    private class Press(val atNanos: Long, val isResume: Boolean)

    // The runs a player plays: those of the resumption first, then those the engine makes. Where the resumption says
    // so, silence runs are dropped until the first run of a piece
    private class RunsToPlay(resumption: Resumption, private val audioAhead: AudioAhead) {
        private val again = ArrayDeque(resumption.runs)
        private var skipsSilence = resumption.skipsSilence

        fun next(): Taken {
            while (true) {
                val taken = again.removeFirstOrNull()?.let(Taken::Run) ?: audioAhead.take()
                if (taken !is Taken.Run) return taken
                if (skipsSilence && taken.run.isSilence) continue
                skipsSilence = false
                return taken
            }
        }

        // The runs of the resumption that were not taken yet follow every run that was. A silence still being
        // skipped is skipped on after the resume
        fun resumptionAfter(heard: Resumption) =
            Resumption(heard.runs + again, skipsSilence = heard.skipsSilence || skipsSilence)
    }

    private class Reading(
        val target: ReadingTarget,
        val parts: List<SpokenPart>,
        val modelFolder: Path,
        pressedAtNanos: Long,
    ) {
        private var stopped = false
        private var paused = false

        // Set by a pause and cleared only by the playing work once it has seen it, so that a resume that comes
        // before then does not take the pause back and the work plays the resumption
        private var pauseUnseen = false
        private var playback: Playback? = null
        private var playingOut: SamplePlayer? = null
        private var samplesHeardAtAPauseInThePlayOut: Long? = null

        // Whether a playing work runs, which a resume then leaves to play on
        private var playing = false
        private var resumption = Resumption.NONE
        private var lastPress = Press(pressedAtNanos, isResume = false)

        val isStopped: Boolean get() = synchronized(this) { stopped }

        val isPaused: Boolean get() = synchronized(this) { paused }

        fun interruption(): Ending? = synchronized(this) {
            when {
                stopped -> Ending.STOP
                pauseUnseen -> Ending.PAUSE
                else -> null
            }
        }

        fun lastPress(): Press = synchronized(this) { lastPress }

        fun playback(): Playback = synchronized(this) { checkNotNull(playback) { "The reading has no audio yet" } }

        // A stop that came before the audio had somewhere to go cancels it here. The caller starts the playing work
        fun playFrom(audio: Playback) {
            synchronized(this) {
                playback = audio
                playing = true
                if (stopped) audio.audioAhead.cancel()
            }
        }

        fun takeResumption(): Resumption = synchronized(this) { resumption.also { resumption = Resumption.NONE } }

        // The playing work calls this once it ended at a pause. Returns true where a resume came meanwhile, and the
        // work then plays the resumption itself
        fun parkAt(at: Resumption): Boolean = synchronized(this) {
            pauseUnseen = false
            if (!stopped && !paused) return true
            resumption = at
            playing = false
            false
        }

        // Returns false where the reading was stopped or paused already. A stop or a pause that comes later discards
        // the player, which ends the sound that the line still holds
        fun playsOutOn(player: SamplePlayer): Boolean = synchronized(this) {
            val playsOut = !stopped && !pauseUnseen
            if (playsOut) playingOut = player
            playsOut
        }

        // Returns the position heard where a pause came during the play-out, and null otherwise
        fun playedOut(): Long? = synchronized(this) {
            playingOut = null
            samplesHeardAtAPauseInThePlayOut.also { samplesHeardAtAPauseInThePlayOut = null }
        }

        // Returns false where there was nothing to pause. The playing work sees the pause before its next slice, or
        // at once where it waits for the next run, except during the play-out, where the line is discarded here, as
        // a stop does
        fun pause(): Boolean {
            val player = synchronized(this) {
                if (stopped || paused) return false
                paused = true
                pauseUnseen = true
                playback?.audioAhead?.wake()
                playingOut?.also {
                    samplesHeardAtAPauseInThePlayOut = it.samplesHeard()
                    playingOut = null
                }
            }
            player?.let { runCatching { it.discard() } }
            return true
        }

        fun resume(pressedAtNanos: Long): Resumed = synchronized(this) {
            if (stopped || !paused) return Resumed.NOT
            paused = false
            lastPress = Press(pressedAtNanos, isResume = true)
            // Before the engine work started the playing, it starts it itself
            if (playing || playback == null) return Resumed.BY_THE_PLAYING_WORK
            playing = true
            Resumed.BY_A_NEW_PLAYING_WORK
        }

        // The player is discarded outside the monitor, since stopping a line can take a moment. A failure of it is
        // not reported: the reading is over, and the playing thread reports nothing for a reading that was stopped
        fun stop() {
            val player = synchronized(this) {
                stopped = true
                playback?.audioAhead?.cancel()
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
