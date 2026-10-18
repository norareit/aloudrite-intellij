package com.ritenoar.aloudrite

interface SamplePlayer : AutoCloseable {
    fun play(samples: FloatArray)

    // Ends the sound at once, without playing what was written and not heard yet. The player is closed after it
    fun discard()
}
