package com.ritenoar.aloudrite

// Where a paused reading goes on: the runs to play again before anything new, and whether silence runs are dropped
// until the first run of a piece, also those still to come
class Resumption(val runs: List<AudioRun>, val skipsSilence: Boolean) {
    companion object {
        val NONE = Resumption(emptyList(), skipsSilence = false)
    }
}

// The segment being heard and the samples heard into it, counted from the first run of the segment that is kept
data class HeardPosition(val segment: Int, val samplesIntoIt: Long)

// The runs taken to be played on one player, with the offset in that player's samples at which each begins, so that
// a pause can say from the position the player has made audible where a resume begins
class PlayedAudio {

    private class TakenRun(val start: Long, val run: AudioRun)

    private val runs = ArrayDeque<TakenRun>()
    private var samplesTaken = 0L

    fun taken(run: AudioRun, samplesHeard: Long) {
        runs.addLast(TakenRun(samplesTaken, run))
        samplesTaken += run.samples.size
        forgetWhatWasHeard(samplesHeard)
    }

    // The segment being heard is the one that holds the position, and where the position is at or past the end of
    // what was taken, which is when the line ran dry, it is the last one. Forgets everything, so that the offsets of
    // the next player start again at zero
    fun resumptionAt(samplesHeard: Long): Resumption {
        val beingHeard = runs.indexOfLast { it.start <= samplesHeard }.coerceAtLeast(0)
        val segment = runs.getOrNull(beingHeard)?.run?.segment
        val from = runs.indexOfFirst { it.run.segment == segment }
        val resumption = if (from < 0) {
            Resumption.NONE
        } else {
            Resumption(runs.drop(from).map { it.run }, skipsSilence = runs[from].run.isSilence)
        }
        runs.clear()
        samplesTaken = 0
        return resumption
    }

    // Null where nothing was taken. Unlike resumptionAt, it forgets nothing. A position at or past the end of what was
    // taken is the end of the last segment
    fun positionAt(samplesHeard: Long): HeardPosition? {
        val beingHeard = runs.indexOfLast { it.start <= samplesHeard }.coerceAtLeast(0)
        val segment = runs.getOrNull(beingHeard)?.run?.segment ?: return null
        val segmentStart = runs.first { it.run.segment == segment }.start
        val segmentEnd = runs.last { it.run.segment == segment }.let { it.start + it.run.samples.size }
        return HeardPosition(segment, minOf(samplesHeard, segmentEnd) - segmentStart)
    }

    internal fun runsKept(): List<AudioRun> = runs.map { it.run }

    // The last segment is kept also when it was heard in full, since more of it may follow
    private fun forgetWhatWasHeard(samplesHeard: Long) {
        while (true) {
            val firstSegment = runs.first().run.segment
            val next = runs.firstOrNull { it.run.segment != firstSegment } ?: return
            if (next.start > samplesHeard) return
            while (runs.first().run.segment == firstSegment) runs.removeFirst()
        }
    }
}
