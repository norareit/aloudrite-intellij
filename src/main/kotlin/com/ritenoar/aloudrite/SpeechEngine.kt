package com.ritenoar.aloudrite

interface SpeechEngine : AutoCloseable {
    val sampleRate: Int

    // `speed` is a factor of the normal speed of the voice: 2 makes the words take half the time. `onSamples` answers
    // true to go on and false to stop. The engine stops after the sentence it is making, which cannot be cut short
    fun speak(text: String, speed: Float, onSamples: (FloatArray) -> Boolean)
}
