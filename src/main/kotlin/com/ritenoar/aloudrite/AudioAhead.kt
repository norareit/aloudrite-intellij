package com.ritenoar.aloudrite

import java.util.ArrayDeque
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

// Samples the engine made, or a silence the plugin added, with the number of the segment they belong to. A segment is
// one piece of the text, whose samples may come in several runs, or one silence, which is put as one run
class AudioRun(val segment: Int, val isSilence: Boolean, val samples: FloatArray)

// The audio that the engine made and that is not played yet, between the thread of the engine and the thread that
// plays. The limit keeps the engine from running far ahead, which would only cost memory
class AudioAhead(private val limitInSamples: Int) {

    private val lock = ReentrantLock()
    private val changed = lock.newCondition()
    private val runs = ArrayDeque<AudioRun>()
    private var samplesWaiting = 0L
    private var finished = false
    private var cancelled = false

    // Waits while the limit is reached. Returns false, and drops the run, once the reading was cancelled
    fun put(run: AudioRun): Boolean = lock.withLock {
        while (samplesWaiting >= limitInSamples && !cancelled) changed.await()
        if (cancelled) return false
        runs.addLast(run)
        samplesWaiting += run.samples.size
        changed.signalAll()
        return true
    }

    // Waits while nothing is waiting. Returns null at the end of the audio and once the reading was cancelled
    fun take(): AudioRun? = lock.withLock {
        while (runs.isEmpty() && !finished && !cancelled) changed.await()
        if (cancelled) return null
        val run = runs.pollFirst() ?: return null
        samplesWaiting -= run.samples.size
        changed.signalAll()
        return run
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
