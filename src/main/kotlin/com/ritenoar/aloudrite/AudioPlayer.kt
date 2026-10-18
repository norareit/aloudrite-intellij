package com.ritenoar.aloudrite

import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine

// Takes a line that is open and not started. `onFirstWrite` runs after the first piece is converted and just before
// it is written to the line, which is as near to the sound as the plugin can see
class AudioPlayer(
    private val line: SourceDataLine,
    private val onFirstWrite: () -> Unit,
) : SamplePlayer {

    private var hasWritten = false

    init {
        line.start()
    }

    override fun play(samples: FloatArray) {
        val bytes = toPcm16LittleEndian(samples)
        if (!hasWritten) {
            hasWritten = true
            onFirstWrite()
        }
        line.write(bytes, 0, bytes.size)
    }

    override fun close() {
        try {
            line.drain()
        } finally {
            line.close()
        }
    }

    override fun discard() {
        try {
            line.stop()
            line.flush()
        } finally {
            line.close()
        }
    }

    companion object {
        // The default output of the machine, which is what `AudioSystem.getSourceDataLine` gives
        fun open(sampleRate: Int, onFirstWrite: () -> Unit): AudioPlayer {
            val line = AudioSystem.getSourceDataLine(AudioFormat(sampleRate.toFloat(), 16, 1, true, false))
            line.open()
            return try {
                AudioPlayer(line, onFirstWrite)
            } catch (exception: Throwable) {
                line.close()
                throw exception
            }
        }
    }
}
