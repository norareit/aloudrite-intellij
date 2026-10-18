package com.ritenoar.aloudrite

import java.util.ArrayDeque
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

// The audio that the engine made and that is not played yet, between the thread of the engine and the thread that
// plays. The limit keeps the engine from running far ahead, which would only cost memory
class AudioAhead(private val limitInSamples: Int) {

    private val lock = ReentrantLock()
    private val changed = lock.newCondition()
    private val pieces = ArrayDeque<FloatArray>()
    private var samplesWaiting = 0L
    private var finished = false
    private var cancelled = false

    // Waits while the limit is reached. Returns false, and drops the samples, once the reading was cancelled
    fun put(samples: FloatArray): Boolean = lock.withLock {
        while (samplesWaiting >= limitInSamples && !cancelled) changed.await()
        if (cancelled) return false
        pieces.addLast(samples)
        samplesWaiting += samples.size
        changed.signalAll()
        return true
    }

    // Waits while nothing is waiting. Returns null at the end of the audio and once the reading was cancelled
    fun take(): FloatArray? = lock.withLock {
        while (pieces.isEmpty() && !finished && !cancelled) changed.await()
        if (cancelled) return null
        val samples = pieces.pollFirst() ?: return null
        samplesWaiting -= samples.size
        changed.signalAll()
        return samples
    }

    // No more audio follows what was put
    fun finish() = lock.withLock {
        finished = true
        changed.signalAll()
    }

    fun cancel() = lock.withLock {
        cancelled = true
        pieces.clear()
        samplesWaiting = 0
        changed.signalAll()
    }
}
