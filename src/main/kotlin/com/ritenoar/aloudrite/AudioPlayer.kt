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

    // What the line had played when it was closed. A question that races the end of the reading is then answered from
    // here, and never from a line that is closed
    @Volatile
    private var positionWhenClosed: Long? = null

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

    // The line has one channel, so a frame is a sample
    override fun samplesHeard(): Long = positionWhenClosed ?: line.longFramePosition

    override fun close() {
        try {
            line.drain()
        } finally {
            closeTheLine()
        }
    }

    override fun discard() {
        keepThePosition()
        try {
            line.stop()
            line.flush()
        } finally {
            closeTheLine()
        }
    }

    private fun closeTheLine() {
        keepThePosition()
        line.close()
    }

    // Kept once, by whichever of a close and a discard comes first, since the other may come after the line is closed
    @Synchronized
    private fun keepThePosition() {
        if (positionWhenClosed == null) positionWhenClosed = line.longFramePosition
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
