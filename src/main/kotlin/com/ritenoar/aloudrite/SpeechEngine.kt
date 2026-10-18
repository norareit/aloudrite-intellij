package com.ritenoar.aloudrite

interface SpeechEngine : AutoCloseable {
    val sampleRate: Int

    // `onSamples` answers true to go on and false to stop. The engine stops after the sentence it is making, which
    // cannot be cut short
    fun speak(text: String, onSamples: (FloatArray) -> Boolean)
}
