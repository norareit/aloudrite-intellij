package com.ritenoar.aloudrite

import kotlin.math.roundToInt

private const val SLOWEST_TENTHS = 5
private const val NORMAL_TENTHS = 10
private const val FASTEST_TENTHS = 20
private const val TENTHS_PER_UNIT = 10

// One or more digits, and at the most one decimal after a point
private val TYPED_SPEED = Regex("""(\d+)(?:\.(\d))?""")

// Held as a whole number of tenths, so that no comparison of floating point numbers decides a limit or a step
@ConsistentCopyVisibility
data class ReadingSpeed private constructor(val tenths: Int) {

    val forTheEngine: Float get() = tenths.toFloat() / TENTHS_PER_UNIT

    // With one decimal, as the field shows it
    val number: String get() = "${tenths / TENTHS_PER_UNIT}.${tenths % TENTHS_PER_UNIT}"

    val display: String get() = "$number×"

    val canGoSlower: Boolean get() = tenths > SLOWEST_TENTHS

    val canGoFaster: Boolean get() = tenths < FASTEST_TENTHS

    fun slower(): ReadingSpeed = if (canGoSlower) ReadingSpeed(tenths - 1) else this

    fun faster(): ReadingSpeed = if (canGoFaster) ReadingSpeed(tenths + 1) else this

    fun silenceMilliseconds(millisecondsAtNormalSpeed: Int): Int =
        (millisecondsAtNormalSpeed * TENTHS_PER_UNIT.toDouble() / tenths).roundToInt()

    companion object {
        val SLOWEST = ReadingSpeed(SLOWEST_TENTHS)
        val NORMAL = ReadingSpeed(NORMAL_TENTHS)
        val FASTEST = ReadingSpeed(FASTEST_TENTHS)
        val ALL: List<ReadingSpeed> = (SLOWEST_TENTHS..FASTEST_TENTHS).map(::ReadingSpeed)

        // Null where the tenths are outside the range
        fun ofTenths(tenths: Int): ReadingSpeed? = tenths.takeIf { it in SLOWEST_TENTHS..FASTEST_TENTHS }?.let(::ReadingSpeed)
    }
}

// What was typed in the field, or null where it is not a speed of the range with one decimal at the most
fun parsedSpeed(text: String): ReadingSpeed? {
    val match = TYPED_SPEED.matchEntire(text.trim()) ?: return null
    val units = match.groupValues[1].toIntOrNull() ?: return null
    val decimal = match.groupValues[2].ifEmpty { "0" }.toInt()
    if (units > FASTEST_TENTHS) return null
    return ReadingSpeed.ofTenths(units * TENTHS_PER_UNIT + decimal)
}
