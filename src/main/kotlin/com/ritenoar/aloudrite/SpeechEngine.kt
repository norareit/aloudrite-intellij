package com.ritenoar.aloudrite

interface SpeechEngine : AutoCloseable {
    val sampleRate: Int

    fun speak(text: String, onSamples: (FloatArray) -> Unit)
}
