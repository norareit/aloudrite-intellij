package com.ritenoar.aloudrite

// The word of a reading that is heard now and the sentence it is part of, as ranges of the file
data class HeardPlaces(val word: SourceRange, val sentence: SourceRange)

// Estimates which word is being heard. The engine gives the times of whole pieces only, so the word is chosen from the
// share of the piece that has been heard. It knows nothing of threads, players or editors: the caller tells it each
// position that is heard and reads the places
class HeardWordEstimate(private val segments: List<ReadingSegment>) {

    private var segmentHeard = -1
    private var samplesHeard = 0L
    private var wordHeard = 0

    // Null until the first position in a piece of words is heard
    var places: HeardPlaces? = null
        private set

    // The share is the one of the bar of progress. A position in a silence changes nothing, so the places stay as at the
    // end of the piece before it
    fun heard(segment: Int, samplesIntoIt: Long, share: Double) {
        val words = segments[segment].words
        if (words.isEmpty()) return
        val wordOfTheShare = wordAtShare(words, share)
        // Audio of the segment that arrives late lengthens it, and must not move the word back. A position with fewer
        // samples, or in another segment, is a piece played again and starts afresh
        val continues = segment == segmentHeard && samplesIntoIt >= samplesHeard
        wordHeard = if (continues) maxOf(wordOfTheShare, wordHeard) else wordOfTheShare
        segmentHeard = segment
        samplesHeard = samplesIntoIt
        places = words[wordHeard].let { HeardPlaces(it.place, it.sentence) }
    }
}

// The first word whose running sum of weights, through itself, is greater than the share of their sum. Where there is
// none, which is at a share of 1, it is the last word whose weight is above 0, and where all weigh 0 it is the first
internal fun wordAtShare(words: List<HeardWord>, share: Double): Int {
    val total = words.sumOf { it.weight }
    if (total == 0) return 0
    var runningSum = 0
    for ((number, word) in words.withIndex()) {
        runningSum += word.weight
        if (runningSum > share * total) return number
    }
    return words.indexOfLast { it.weight > 0 }
}
