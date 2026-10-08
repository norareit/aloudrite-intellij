package com.ritenoar.aloudrite

interface SamplePlayer : AutoCloseable {
    fun play(samples: FloatArray)
}
