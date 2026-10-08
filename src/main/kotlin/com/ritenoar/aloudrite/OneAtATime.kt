package com.ritenoar.aloudrite

import java.util.concurrent.atomic.AtomicBoolean

class OneAtATime(private val runInBackground: (() -> Unit) -> Unit) {

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
                }
            }
        } catch (exception: Throwable) {
            running.set(false)
            throw exception
        }
        return true
    }
}
