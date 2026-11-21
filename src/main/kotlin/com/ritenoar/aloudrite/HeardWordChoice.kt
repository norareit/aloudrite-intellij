package com.ritenoar.aloudrite

// How long after the start that the aligner gives for a word the word is shown, so that it is shown with the voice. The
// aligner places the start of a word earlier than it is heard. Set by ear
const val WORD_DELAY_MILLISECONDS = 70

// Lets the developer set the delay by ear in the sandbox IDE without a build per value. An installed plugin never has it
const val WORD_DELAY_PROPERTY = "aloudrite.word.delay.milliseconds"

fun wordDelayMilliseconds(propertyValue: String?): Int =
    propertyValue?.toIntOrNull()?.takeIf { it >= 0 } ?: WORD_DELAY_MILLISECONDS

fun delayInSamples(delayMilliseconds: Int, sampleRate: Int): Long = delayMilliseconds.toLong() * sampleRate / 1_000

// Chooses which word is heard: by the alignment of its piece where it has one, and by the estimate otherwise. The estimate is
// told every position, so that it is current whenever a piece falls back to it. It knows nothing of threads, players or
// editors, as the estimate does not
class HeardWordChoice(
    private val segments: List<ReadingSegment>,
    delayMilliseconds: Int,
    sampleRate: Int,
) {

    private val estimate = HeardWordEstimate(segments)
    private val delaySamples = delayInSamples(delayMilliseconds, sampleRate)
    private val alignments = mutableMapOf<Int, PieceAlignment>()

    // Null until the first position in a piece of words is heard
    var places: HeardPlaces? = null
        private set

    // Kept for the audio that the piece was aligned on, so that a position of it is looked up in these starts
    fun aligned(segment: Int, alignment: PieceAlignment) {
        alignments[segment] = alignment
    }

    // A new making makes new audio for the same segments, which the alignments do not fit
    fun forget() = alignments.clear()

    fun heard(segment: Int, samplesIntoIt: Long, share: Double) {
        estimate.heard(segment, samplesIntoIt, share)
        places = placesByTheAlignment(segment, samplesIntoIt) ?: estimate.places
    }

    // Null in a silence and in a piece without an alignment, which show what the estimate does
    private fun placesByTheAlignment(segment: Int, samplesIntoIt: Long): HeardPlaces? {
        val alignment = alignments[segment] ?: return null
        val words = segments[segment].words
        val word = wordShownAt(alignment.starts, samplesIntoIt, delaySamples) ?: return null
        return words[word].let { HeardPlaces(it.place, it.sentence) }
    }
}
