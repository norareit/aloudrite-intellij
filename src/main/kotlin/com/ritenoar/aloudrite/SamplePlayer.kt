package com.ritenoar.aloudrite

interface SamplePlayer : AutoCloseable {
    fun play(samples: FloatArray)

    // The samples of what was played that have been made audible, which is less than what was played by what the
    // output still holds
    fun samplesHeard(): Long

    // Ends the sound at once, without playing what was written and not heard yet. The player is closed after it
    fun discard()
}
