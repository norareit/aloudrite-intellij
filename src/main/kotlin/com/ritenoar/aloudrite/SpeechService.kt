package com.ritenoar.aloudrite

import com.intellij.ide.ActivityTracker
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.util.concurrency.AppExecutorUtil
import java.nio.file.Path
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

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

// What a first sound is measured from
enum class FirstSoundAfter { READ_ALOUD, RESUME, SPEED_CHANGE }

// The collaborators are handed in so that a test can run the service without the model, without an audio device and
// without waiting for real time
@Service(Service.Level.APP)
class SpeechService(
    private val runInBackground: (() -> Unit) -> Unit,
    private val createEngine: (modelFolder: Path) -> SpeechEngine,
    private val createPlayer: (sampleRate: Int, onFirstWrite: () -> Unit) -> SamplePlayer,
    private val reportFirstSound: (milliseconds: Long, after: FirstSoundAfter) -> Unit,
    private val reportFailure: (target: ReadingTarget, failure: Throwable) -> Unit,
    private val onChange: () -> Unit,
    private val currentSpeed: () -> ReadingSpeed,
    private val nowMillis: () -> Long,
    private val runLater: (delayMillis: Long, work: () -> Unit) -> Unit,
) : Readings, CurrentReading, Disposable {

    constructor() : this(
        { work -> ApplicationManager.getApplication().executeOnPooledThread(work) },
        ::createKokoroEngine,
        { sampleRate, onFirstWrite -> AudioPlayer.open(sampleRate, onFirstWrite) },
        ::logFirstSound,
        ::logAndShowFailure,
        {
            ActivityTracker.getInstance().inc()
            publishReadingChange()
        },
        { service<ReadingSpeedSetting>().speed },
        { System.nanoTime() / NANOSECONDS_PER_MILLISECOND },
        { delayMillis, work ->
            AppExecutorUtil.getAppScheduledExecutorService().schedule(Runnable { work() }, delayMillis, TimeUnit.MILLISECONDS)
        },
    )

    // The engine is not made for two callers, so the engine work of a making holds this for the whole making. It is
    // not reentrant, so that dispose, called while making the audio on the same thread, sees it held
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
        val reading = Reading(target, parts, modelFolder, currentSpeed(), pressAt(FirstSoundAfter.READ_ALOUD, pressedAtNanos))
        val previous = synchronized(this) { current.also { current = reading } }
        previous?.stop()
        onChange()
        runInBackground { makeTheFirstAudio(reading) }
    }

    override fun stop(target: ReadingTarget) {
        stopWhere { it.target == target }
    }

    override fun pause(target: ReadingTarget) {
        val reading = currentReadingOf(target) ?: return
        if (reading.pause()) onChange()
    }

    // A playing work that has not yet seen the pause plays the resumption by itself, and one that ended at the pause
    // is replaced. Where the speed changed meanwhile, the resume starts the making at the new speed
    override fun resume(target: ReadingTarget, pressedAtNanos: Long) {
        val reading = currentReadingOf(target) ?: return
        val resumed = reading.resume(pressAt(FirstSoundAfter.RESUME, pressedAtNanos))
        if (resumed == Resumed.NOT) return
        onChange()
        when (resumed) {
            Resumed.BY_A_NEW_PLAYING_WORK -> {
                val making = reading.making()
                runInBackground { play(reading, making) }
            }
            Resumed.BY_A_NEW_MAKING -> startTheMakingThatIsDue(reading)
            Resumed.NOT, Resumed.BY_THE_PLAYING_WORK -> Unit
        }
    }

    // A change by a button waits, so that presses in a row make the audio once, at the last speed. A check of the
    // wait is never cancelled: one that comes before the time the reading holds does nothing
    fun changeSpeed(speed: ReadingSpeed, afterAWait: Boolean) {
        val reading = synchronized(this) { current } ?: return
        val changedAtMillis = nowMillis()
        val waitMillis = if (afterAWait) WAIT_AFTER_A_PRESS_MILLISECONDS else 0L
        reading.changeSpeed(speed, changedAtMillis + waitMillis, Press(FirstSoundAfter.SPEED_CHANGE) { nowMillis() - changedAtMillis })
        if (afterAWait) runLater(waitMillis) { startTheMakingThatIsDue(reading) } else startTheMakingThatIsDue(reading)
    }

    private fun pressAt(after: FirstSoundAfter, pressedAtNanos: Long) =
        Press(after) { (System.nanoTime() - pressedAtNanos) / NANOSECONDS_PER_MILLISECOND }

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

    private fun makeTheFirstAudio(reading: Reading) = withTheEngine(reading) { speechEngine ->
        val making = reading.startTheFirstMaking(speechEngine.sampleRate) ?: return@withTheEngine
        runInBackground { play(reading, making) }
        makeTheAudio(reading, making, speechEngine)
    }

    // The three places that may start a making call this: the playing work that saw a change, the check of the wait,
    // and a resume. The reading decides which of them starts it
    private fun startTheMakingThatIsDue(reading: Reading) {
        val making = reading.startTheMakingThatIsDue(nowMillis()) ?: return
        runInBackground { withTheEngine(reading) { speechEngine -> makeTheAudio(reading, making, speechEngine) } }
        runInBackground { play(reading, making) }
    }

    private fun withTheEngine(reading: Reading, work: (SpeechEngine) -> Unit) {
        engineInUse.acquireUninterruptibly()
        try {
            if (reading.isStopped) return
            work(engine ?: createEngine(reading.modelFolder).also { engine = it })
        } catch (exception: Throwable) {
            fail(reading, exception)
        } finally {
            engineInUse.release()
        }
    }

    // A making that a later one replaces, also one still waiting to start, gives the engine no further segment
    private fun makeTheAudio(reading: Reading, making: Making, speechEngine: SpeechEngine) {
        for ((number, segment) in reading.segments.withIndex().drop(making.from.segment)) {
            if (!reading.isCurrent(making)) break
            if (!makeTheAudioOf(segment, number, speechEngine, making)) break
        }
        making.audioAhead.finish()
    }

    // Returns false where the audio ahead was cancelled. A cancel during the words is seen by the caller as the end
    // of the making
    private fun makeTheAudioOf(segment: SpokenPart, number: Int, speechEngine: SpeechEngine, making: Making) =
        when (segment) {
            is Words -> {
                speechEngine.speak(segment.text, making.speed.forTheEngine) { samples ->
                    making.audioAhead.put(AudioRun(number, isSilence = false, samples))
                }
                true
            }
            is Silence -> {
                val milliseconds = making.speed.silenceMilliseconds(segment.milliseconds)
                making.audioAhead.put(
                    AudioRun(number, isSilence = true, FloatArray(making.sampleRate * milliseconds / MILLISECONDS_PER_SECOND)))
            }
        }

    // A pause ends the playing work, unless a resume came before the work stored where to resume. A change of the
    // speed ends it as well, and hands where the next making begins to the reading
    private fun play(reading: Reading, making: Making) {
        var resumption = reading.takeResumption()
        while (true) {
            resumption = playUntilAnInterruption(reading, making, resumption) ?: return
            when (reading.interruptedAt(resumption, making)) {
                Interrupted.PLAYS_ON -> continue
                Interrupted.PARKED -> return
                Interrupted.RESTARTS -> return startTheMakingThatIsDue(reading)
            }
        }
    }

    // Returns where the reading goes on where it was paused or its speed changed, and null where it ended
    private fun playUntilAnInterruption(reading: Reading, making: Making, resumption: Resumption): Resumption? {
        val interruption = reading.interruption()
        if (interruption == Ending.PAUSE || interruption == Ending.CHANGE) return resumption
        val press = reading.lastPress()
        val onFirstWrite = { reportFirstSound(press.millisecondsSince(), press.after) }
        val player = try {
            createPlayer(making.sampleRate, onFirstWrite)
        } catch (exception: Throwable) {
            fail(reading, exception)
            return null
        }
        val runs = RunsToPlay(resumption, making.audioAhead)
        val playedAudio = PlayedAudio()
        val ending = try {
            playUntilTheEndOrAnInterruption(reading, runs, playedAudio, player, making.sampleRate / SLICES_PER_SECOND)
        } catch (exception: Throwable) {
            runCatching { player.discard() }
            fail(reading, exception)
            return null
        }
        return when (ending) {
            Ending.STOP -> null.also { player.discard() }
            Ending.PAUSE, Ending.CHANGE -> interruptedOn(player, runs, playedAudio)
            Ending.END -> playOut(reading, player, runs, playedAudio)
        }
    }

    // The position is taken before the discard, which ends the sound at once
    private fun interruptedOn(player: SamplePlayer, runs: RunsToPlay, playedAudio: PlayedAudio): Resumption {
        val samplesHeard = player.samplesHeard()
        runCatching { player.discard() }
        return runs.resumptionAfter(playedAudio.resumptionAt(samplesHeard))
    }

    // Closing plays out what the line still holds, and a failure of the audio output can come then as well. A pause or
    // a change meanwhile discards the line from its own thread, and what the discarded line throws is no failure
    private fun playOut(reading: Reading, player: SamplePlayer, runs: RunsToPlay, playedAudio: PlayedAudio): Resumption? {
        if (!reading.playsOutOn(player)) {
            if (reading.isStopped) return null.also { player.discard() }
            return interruptedOn(player, runs, playedAudio)
        }
        val failure = runCatching { player.close() }.exceptionOrNull()
        val samplesHeardAtAnInterruption = reading.playedOut()
        if (samplesHeardAtAnInterruption != null) {
            return runs.resumptionAfter(playedAudio.resumptionAt(samplesHeardAtAnInterruption))
        }
        if (failure != null) {
            fail(reading, failure)
            return null
        }
        end(reading)
        return null
    }

    // A stop, a pause and a change are looked at between two slices, so that each ends the sound after at most one
    // slice, and a pause wakes the wait for the next run. A run is kept by playedAudio as soon as it is taken, so that
    // a pause right after the take plays it at the resume
    private fun playUntilTheEndOrAnInterruption(
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

    private enum class Ending { END, PAUSE, CHANGE, STOP }

    private enum class Resumed { NOT, BY_THE_PLAYING_WORK, BY_A_NEW_PLAYING_WORK, BY_A_NEW_MAKING }

    // What the playing work does once it ended at a pause or a change: play on where a resume came before it looked,
    // end where the reading stays paused or was stopped, and start the next making where the speed changed
    private enum class Interrupted { PLAYS_ON, PARKED, RESTARTS }

    // The press or the change that the next first sound is measured from
    private class Press(val after: FirstSoundAfter, val millisecondsSince: () -> Long)

    // The segment a making begins at, and whether its silence runs are dropped until the first run of a piece
    private class Restart(val segment: Int, val skipsSilence: Boolean) {
        companion object {
            val AT_THE_BEGINNING = Restart(0, skipsSilence = false)

            // Null where nothing is to be heard again
            fun of(resumption: Resumption): Restart? =
                resumption.runs.firstOrNull()?.let { Restart(it.segment, resumption.skipsSilence) }
        }
    }

    // The audio made from one segment on, at one speed. A change of the speed replaces it with a new one
    private class Making(val from: Restart, val speed: ReadingSpeed, val audioAhead: AudioAhead, val sampleRate: Int)

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
        private var speed: ReadingSpeed,
        private var lastPress: Press,
    ) {
        // Numbered once per reading, so that the runs of every making of it carry the numbers of the same segments
        val segments: List<SpokenPart> by lazy { parts.flatMap(::segmentsOf) }

        private var stopped = false
        private var paused = false

        // Set by a pause and cleared only by the playing work once it has seen it, so that a resume that comes
        // before then does not take the pause back and the work plays the resumption
        private var pauseUnseen = false

        // Set by a change of the speed and cleared by the playing work once it has seen it, as the pause is
        private var changeUnseen = false
        private var making: Making? = null

        // Where the next making begins, from the moment the playing work saw a change until that making starts
        private var restart: Restart? = null

        // No making that follows a change starts before this, on the clock of the service
        private var notBeforeMillis = Long.MIN_VALUE
        private var playingOut: SamplePlayer? = null
        private var samplesHeardAtAnInterruptionInThePlayOut: Long? = null

        // Whether a playing work runs, which a resume then leaves to play on
        private var playing = false
        private var resumption = Resumption.NONE

        val isStopped: Boolean get() = synchronized(this) { stopped }

        val isPaused: Boolean get() = synchronized(this) { paused }

        fun interruption(): Ending? = synchronized(this) {
            when {
                stopped -> Ending.STOP
                changeUnseen -> Ending.CHANGE
                pauseUnseen -> Ending.PAUSE
                else -> null
            }
        }

        fun lastPress(): Press = synchronized(this) { lastPress }

        fun making(): Making = synchronized(this) { checkNotNull(making) { "The reading has no audio yet" } }

        // False once a change made the making stale, also while the making that replaces it waits to start
        fun isCurrent(audio: Making): Boolean =
            synchronized(this) { !stopped && making === audio && !changeUnseen && restart == null }

        // Returns null where a stop came before the audio had somewhere to go. The caller starts the playing work
        fun startTheFirstMaking(sampleRate: Int): Making? = synchronized(this) {
            if (stopped) return null
            startAMaking(Restart.AT_THE_BEGINNING, sampleRate)
        }

        // Returns the making to start where a change waits for one, the reading plays and its time has come, and
        // null otherwise. The caller starts its engine work and its playing work
        fun startTheMakingThatIsDue(nowMillis: Long): Making? = synchronized(this) {
            val from = restart ?: return null
            if (stopped || paused || nowMillis < notBeforeMillis) return null
            restart = null
            startAMaking(from, making().sampleRate)
        }

        private fun startAMaking(from: Restart, sampleRate: Int): Making {
            val started = Making(from, speed, AudioAhead(sampleRate * SECONDS_AHEAD), sampleRate)
            making = started
            playing = true
            resumption = Resumption(emptyList(), from.skipsSilence)
            return started
        }

        fun takeResumption(): Resumption = synchronized(this) { resumption.also { resumption = Resumption.NONE } }

        // The playing work calls this once it ended at a pause or a change, with where the reading goes on. Where
        // nothing of its making was heard yet, the next making begins where this one did
        fun interruptedAt(at: Resumption, audio: Making): Interrupted = synchronized(this) {
            pauseUnseen = false
            if (changeUnseen) {
                changeUnseen = false
                restart = Restart.of(at) ?: audio.from
                playing = false
                return Interrupted.RESTARTS
            }
            if (!stopped && !paused) return Interrupted.PLAYS_ON
            resumption = at
            playing = false
            Interrupted.PARKED
        }

        // Returns false where the reading was stopped, paused or changed already. One of them that comes later
        // discards the player, which ends the sound that the line still holds
        fun playsOutOn(player: SamplePlayer): Boolean = synchronized(this) {
            val playsOut = !stopped && !pauseUnseen && !changeUnseen
            if (playsOut) playingOut = player
            playsOut
        }

        // Returns the position heard where a pause or a change came during the play-out, and null otherwise
        fun playedOut(): Long? = synchronized(this) {
            playingOut = null
            samplesHeardAtAnInterruptionInThePlayOut.also { samplesHeardAtAnInterruptionInThePlayOut = null }
        }

        // Returns false where there was nothing to pause. The playing work sees the pause before its next slice, or
        // at once where it waits for the next run, except during the play-out, where the line is discarded here, as
        // a stop does. While a making waits to start, no playing work runs to see it
        fun pause(): Boolean {
            val player = synchronized(this) {
                if (stopped || paused) return false
                paused = true
                pauseUnseen = restart == null
                making?.audioAhead?.wake()
                takeThePlayingOut()
            }
            player?.let { runCatching { it.discard() } }
            return true
        }

        fun resume(press: Press): Resumed = synchronized(this) {
            if (stopped || !paused) return Resumed.NOT
            paused = false
            lastPress = press
            if (restart != null) return Resumed.BY_A_NEW_MAKING
            // Before the engine work started the playing, it starts it itself
            if (playing || making == null) return Resumed.BY_THE_PLAYING_WORK
            playing = true
            Resumed.BY_A_NEW_PLAYING_WORK
        }

        // A change before the first making takes effect there, and one while a change waits for its making only
        // replaces the speed and the time of that making. Otherwise the audio made ahead is dropped: where the
        // playing work has ended at a pause, its resumption becomes the restart at once, and where it runs, it sees
        // the change before its next slice, as it sees a pause
        fun changeSpeed(newSpeed: ReadingSpeed, notBefore: Long, press: Press) {
            val player = synchronized(this) {
                if (stopped) return
                speed = newSpeed
                notBeforeMillis = notBefore
                lastPress = press
                val stale = making ?: return
                if (changeUnseen || restart != null) return
                stale.audioAhead.cancel()
                if (paused && !playing) {
                    restart = Restart.of(resumption) ?: stale.from
                    resumption = Resumption.NONE
                    return
                }
                changeUnseen = true
                takeThePlayingOut()
            }
            player?.let { runCatching { it.discard() } }
        }

        private fun takeThePlayingOut(): SamplePlayer? = playingOut?.also {
            samplesHeardAtAnInterruptionInThePlayOut = it.samplesHeard()
            playingOut = null
        }

        // The player is discarded outside the monitor, since stopping a line can take a moment. A failure of it is
        // not reported: the reading is over, and the playing thread reports nothing for a reading that was stopped
        fun stop() {
            val player = synchronized(this) {
                stopped = true
                making?.audioAhead?.cancel()
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
        const val WAIT_AFTER_A_PRESS_MILLISECONDS = 500L

        // A segment is one piece of words, as the plugin cuts the text, or one silence. A resume and a making begin
        // at a segment
        fun segmentsOf(part: SpokenPart): List<SpokenPart> = when (part) {
            is Words -> piecesOf(part.text).filterNot { it.isBlank() }.map(::Words)
            is Silence -> listOf(part)
        }

        fun logFirstSound(milliseconds: Long, after: FirstSoundAfter) {
            val cause = when (after) {
                FirstSoundAfter.READ_ALOUD -> "the button was pressed"
                FirstSoundAfter.RESUME -> "Resume Reading was pressed"
                FirstSoundAfter.SPEED_CHANGE -> "the speed was changed"
            }
            LOG.info("First sound $milliseconds ms after $cause")
        }

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
