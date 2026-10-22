package com.ritenoar.aloudrite

import java.util.concurrent.atomic.AtomicBoolean

// Tells onChange when the work starts and when it has ended, after isRunning says so
class OneAtATime(
    private val runInBackground: (() -> Unit) -> Unit,
    private val onChange: () -> Unit,
) {

    private val running = AtomicBoolean(false)

    val isRunning: Boolean get() = running.get()

    fun tryRun(work: () -> Unit): Boolean {
        if (!running.compareAndSet(false, true)) return false
        try {
            runInBackground {
                try {
                    work()
                } finally {
                    running.set(false)
                    onChange()
                }
            }
        } catch (exception: Throwable) {
            running.set(false)
            throw exception
        }
        onChange()
        return true
    }
}
