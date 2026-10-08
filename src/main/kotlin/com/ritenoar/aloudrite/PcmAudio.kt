package com.ritenoar.aloudrite

import kotlin.math.roundToInt

private const val MAXIMUM_SAMPLE_VALUE = 32_767

fun toPcm16LittleEndian(samples: FloatArray): ByteArray {
    val bytes = ByteArray(samples.size * 2)
    samples.forEachIndexed { index, sample ->
        val value = (sample.coerceIn(-1f, 1f) * MAXIMUM_SAMPLE_VALUE).roundToInt()
        bytes[index * 2] = value.toByte()
        bytes[index * 2 + 1] = (value shr 8).toByte()
    }
    return bytes
}
