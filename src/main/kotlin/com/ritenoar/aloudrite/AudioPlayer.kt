package com.ritenoar.aloudrite

import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine

class AudioPlayer(sampleRate: Int) : AutoCloseable {

    private val line: SourceDataLine = AudioSystem.getSourceDataLine(
        AudioFormat(sampleRate.toFloat(), 16, 1, true, false)
    )

    init {
        line.open()
        line.start()
    }

    fun play(samples: FloatArray) {
        val bytes = toPcm16LittleEndian(samples)
        line.write(bytes, 0, bytes.size)
    }

    override fun close() {
        line.drain()
        line.close()
    }
}
