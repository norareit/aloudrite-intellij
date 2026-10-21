package com.ritenoar.aloudrite

import java.util.ArrayDeque
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

// Samples the engine made, or a silence the plugin added, with the number of the segment they belong to. A segment is
// one piece of the text, whose samples may come in several runs, or one silence, which is put as one run
class AudioRun(val segment: Int, val isSilence: Boolean, val samples: FloatArray)

// What a take gives: a run, the end of the audio, which a cancel is as well, or that it was woken before a run came
sealed interface Taken {
    class Run(val run: AudioRun) : Taken

    object End : Taken

    object Woken : Taken
}

// The audio that the engine made and that is not played yet, between the thread of the engine and the thread that
// plays. The limit keeps the engine from running far ahead, which would only cost memory
class AudioAhead(private val limitInSamples: Int) {

    private val lock = ReentrantLock()
    private val changed = lock.newCondition()
    private val runs = ArrayDeque<AudioRun>()
    private var samplesWaiting = 0L
    private var finished = false
    private var cancelled = false
    private var woken = false

    // Waits while the limit is reached. Returns false, and drops the run, once the reading was cancelled
    fun put(run: AudioRun): Boolean = lock.withLock {
        while (samplesWaiting >= limitInSamples && !cancelled) changed.await()
        if (cancelled) return false
        runs.addLast(run)
        samplesWaiting += run.samples.size
        changed.signalAll()
        return true
    }

    // Waits while nothing is waiting. A wake is answered by this take, or by the next where none waits, unless a run
    // or the end is there to give
    fun take(): Taken = lock.withLock {
        while (runs.isEmpty() && !finished && !cancelled && !woken) changed.await()
        val wasWoken = woken
        woken = false
        if (cancelled) return Taken.End
        val run = runs.pollFirst() ?: return if (wasWoken && !finished) Taken.Woken else Taken.End
        samplesWaiting -= run.samples.size
        changed.signalAll()
        return Taken.Run(run)
    }

    // Lets the taker look at what changed without waiting for the next run
    fun wake() = lock.withLock {
        woken = true
        changed.signalAll()
    }

    // No more audio follows what was put
    fun finish() = lock.withLock {
        finished = true
        changed.signalAll()
    }

    fun cancel() = lock.withLock {
        cancelled = true
        runs.clear()
        samplesWaiting = 0
        changed.signalAll()
    }
}
