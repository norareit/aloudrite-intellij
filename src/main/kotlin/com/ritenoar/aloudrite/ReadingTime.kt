package com.ritenoar.aloudrite

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

// What the tool window shows of a reading: the seconds of the whole, the seconds left, both at the speed in use, and
// the share of the text that has been read, from 0 to 1
class ReadingTimes(val wholeSeconds: Double, val leftSeconds: Double, val shareRead: Double)

// Whole seconds, rounded up, as minutes and seconds with the minutes not capped at 59
fun minutesAndSeconds(seconds: Double): String {
    val wholeSeconds = ceil(seconds - ROUNDING_TOLERANCE).toLong().coerceAtLeast(0)
    return "${wholeSeconds / SECONDS_PER_MINUTE}:${(wholeSeconds % SECONDS_PER_MINUTE).toString().padStart(2, '0')}"
}

// Seconds of audio at normal speed for each character of text before any audio is made, near the speech of the
// default voice
internal const val DEFAULT_SECONDS_PER_CHARACTER = 0.075

private const val ROUNDING_TOLERANCE = 1e-6
private const val SECONDS_PER_MINUTE = 60

// The arithmetic of the times and of the bar of a reading, which knows nothing of threads, players or Swing. The caller
// tells it what the engine made and what was heard, and serializes the calls. The sample rate may be set once the
// engine is known, since no audio is told of before then
class ReadingTime(private val segments: List<SpokenPart>, var sampleRate: Int, initialSpeed: ReadingSpeed) {

    private val textLengths: IntArray = IntArray(segments.size) { number ->
        (segments[number] as? Words)?.text?.codePoints()?.filter { !Character.isWhitespace(it) }?.count()?.toInt() ?: 0
    }

    // The text before each segment, so that the share read is found without summing
    private val textBefore: IntArray = IntArray(segments.size + 1).also { sums ->
        for (number in segments.indices) sums[number + 1] = sums[number] + textLengths[number]
    }

    private val samplesMade = LongArray(segments.size)
    private val madeInFull = BooleanArray(segments.size)

    // Seconds at normal speed that each segment of words made in full showed, kept across changes of the speed
    private val secondsAtNormalSpeed = DoubleArray(segments.size)
    private val hasEntry = BooleanArray(segments.size)
    private var entrySeconds = 0.0
    private var entryText = 0L

    private var speed = initialSpeed
    private var segmentHeard = 0
    private var secondsHeardIntoIt = 0.0
    private var shareShown = 0.0
    private var frozen: ReadingTimes? = null

    // The whole as it stood when the first segment of words was made in full, null until then
    var firstEstimateSeconds: Double? = null
        private set

    fun audioMade(segment: Int, samples: Int) {
        if (segments[segment] is Words) samplesMade[segment] += samples
    }

    fun segmentMade(segment: Int) {
        if (segments[segment] !is Words) return
        madeInFull[segment] = true
        recordTheEntry(segment)
        if (firstEstimateSeconds == null) firstEstimateSeconds = computeTimes().wholeSeconds
    }

    fun heard(segment: Int, samplesIntoIt: Long) {
        segmentHeard = segment
        secondsHeardIntoIt = samplesIntoIt.toDouble() / sampleRate
    }

    // The audio ahead is dropped, since the service cancels it, and the seconds heard are kept by their share of the
    // segment
    fun speedChanged(newSpeed: ReadingSpeed) {
        secondsHeardIntoIt *= factorOf(speed) / factorOf(newSpeed)
        speed = newSpeed
        samplesMade.fill(0)
        madeInFull.fill(false)
        if (frozen != null) frozen = computeTimes()
    }

    fun pause() {
        if (frozen == null) frozen = computeTimes()
    }

    fun resume() {
        frozen = null
    }

    fun times(): ReadingTimes = frozen ?: computeTimes()

    private fun recordTheEntry(segment: Int) {
        if (hasEntry[segment]) {
            entrySeconds -= secondsAtNormalSpeed[segment]
            entryText -= textLengths[segment]
        }
        secondsAtNormalSpeed[segment] = samplesMade[segment].toDouble() / sampleRate * factorOf(speed)
        hasEntry[segment] = true
        entrySeconds += secondsAtNormalSpeed[segment]
        entryText += textLengths[segment]
    }

    private fun secondsPerCharacterAtNormalSpeed() =
        if (entryText > 0) entrySeconds / entryText else DEFAULT_SECONDS_PER_CHARACTER

    private fun computeTimes(): ReadingTimes {
        val rate = secondsPerCharacterAtNormalSpeed()
        val seconds = DoubleArray(segments.size) { secondsOf(it, rate) }
        val left = max(seconds.getOrElse(segmentHeard) { 0.0 } - secondsHeardIntoIt, 0.0) +
            (segmentHeard + 1 until segments.size).sumOf { seconds[it] }
        return ReadingTimes(seconds.sum(), left, shareRead(seconds))
    }

    private fun secondsOf(segment: Int, rate: Double): Double {
        val part = segments[segment]
        if (part is Silence) return speed.silenceMilliseconds(part.milliseconds) / MILLISECONDS_PER_SECOND
        val madeSeconds = samplesMade[segment].toDouble() / sampleRate
        if (madeInFull[segment]) return madeSeconds
        val estimate = if (hasEntry[segment]) {
            secondsAtNormalSpeed[segment] / factorOf(speed)
        } else {
            textLengths[segment] * rate / factorOf(speed)
        }
        return max(madeSeconds, estimate)
    }

    // Kept as a high-water mark, so that a piece played again or a longer estimate never moves the bar back
    private fun shareRead(seconds: DoubleArray): Double {
        val total = textBefore.last()
        if (total == 0) return 0.0
        val segmentSeconds = seconds.getOrElse(segmentHeard) { 0.0 }
        val shareOfTheSegment = if (segmentSeconds > 0) min(secondsHeardIntoIt / segmentSeconds, 1.0) else 0.0
        val textRead = textBefore[segmentHeard] + textLengths.getOrElse(segmentHeard) { 0 } * shareOfTheSegment
        shareShown = max(shareShown, textRead / total)
        return shareShown
    }

    private fun factorOf(readingSpeed: ReadingSpeed) = readingSpeed.forTheEngine.toDouble()

    private companion object {
        const val MILLISECONDS_PER_SECOND = 1000.0
    }
}
