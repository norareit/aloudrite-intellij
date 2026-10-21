package com.ritenoar.aloudrite

import kotlin.math.roundToInt

private const val SLOWEST_HUNDREDTHS = 75
private const val NORMAL_HUNDREDTHS = 100
private const val FASTEST_HUNDREDTHS = 200
private const val HUNDREDTHS_PER_UNIT = 100
private const val HUNDREDTHS_PER_TENTH = 10

// Below this the steps are a twentieth, where listeners are most likely to settle and a tenth is a large step, and
// above it a tenth
private const val FINE_STEPS_UP_TO_HUNDREDTHS = 130
private const val FINE_STEP_HUNDREDTHS = 5
private const val COARSE_STEP_HUNDREDTHS = 10

// One or more digits, and at the most two decimals after a point
private val TYPED_SPEED = Regex("""(\d+)(?:\.(\d{1,2}))?""")

// Held as a whole number of hundredths, so that no comparison of floating point numbers decides a limit or a step
@ConsistentCopyVisibility
data class ReadingSpeed private constructor(val hundredths: Int) {

    val forTheEngine: Float get() = hundredths.toFloat() / HUNDREDTHS_PER_UNIT

    // With two decimals where the second is not 0, and with one otherwise, as the field shows it
    val number: String
        get() {
            val decimals = hundredths % HUNDREDTHS_PER_UNIT
            val fraction = if (decimals % HUNDREDTHS_PER_TENTH == 0) "${decimals / HUNDREDTHS_PER_TENTH}" else "%02d".format(decimals)
            return "${hundredths / HUNDREDTHS_PER_UNIT}.$fraction"
        }

    val display: String get() = "$number×"

    val canGoSlower: Boolean get() = hundredths > SLOWEST_HUNDREDTHS

    val canGoFaster: Boolean get() = hundredths < FASTEST_HUNDREDTHS

    // The nearest step below, also from a speed between two steps
    fun slower(): ReadingSpeed = STEPS.lastOrNull { it.hundredths < hundredths } ?: this

    fun faster(): ReadingSpeed = STEPS.firstOrNull { it.hundredths > hundredths } ?: this

    fun silenceMilliseconds(millisecondsAtNormalSpeed: Int): Int =
        (millisecondsAtNormalSpeed * HUNDREDTHS_PER_UNIT.toDouble() / hundredths).roundToInt()

    companion object {
        val SLOWEST = ReadingSpeed(SLOWEST_HUNDREDTHS)
        val NORMAL = ReadingSpeed(NORMAL_HUNDREDTHS)
        val FASTEST = ReadingSpeed(FASTEST_HUNDREDTHS)
        val STEPS: List<ReadingSpeed> =
            ((SLOWEST_HUNDREDTHS..FINE_STEPS_UP_TO_HUNDREDTHS step FINE_STEP_HUNDREDTHS) +
                (FINE_STEPS_UP_TO_HUNDREDTHS + COARSE_STEP_HUNDREDTHS..FASTEST_HUNDREDTHS step COARSE_STEP_HUNDREDTHS))
                .map(::ReadingSpeed)

        // Null where the hundredths are outside the range
        fun ofHundredths(hundredths: Int): ReadingSpeed? =
            hundredths.takeIf { it in SLOWEST_HUNDREDTHS..FASTEST_HUNDREDTHS }?.let(::ReadingSpeed)

        // The steps, with a speed between two of them in its place
        fun stepsWith(speed: ReadingSpeed): List<ReadingSpeed> = (STEPS + speed).distinct().sortedBy { it.hundredths }
    }
}

// What was typed in the field, or null where it is not a speed of the range with two decimals at the most
fun parsedSpeed(text: String): ReadingSpeed? {
    val match = TYPED_SPEED.matchEntire(text.trim()) ?: return null
    val units = match.groupValues[1].toIntOrNull() ?: return null
    if (units > FASTEST_HUNDREDTHS / HUNDREDTHS_PER_UNIT) return null
    val decimals = match.groupValues[2].padEnd(2, '0').toInt()
    return ReadingSpeed.ofHundredths(units * HUNDREDTHS_PER_UNIT + decimals)
}
