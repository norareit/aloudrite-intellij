package com.ritenoar.aloudrite

private const val PROCESSORS_PER_THREAD = 2
private const val MINIMUM_THREADS = 1
private const val MAXIMUM_THREADS = 4

// Half the processors stands for the physical cores, which Java cannot read, and the cap keeps the plugin a guest in
// the IDE. See the spec of unit 0011 for the rest
fun engineThreadCount(availableProcessors: Int): Int =
    (availableProcessors / PROCESSORS_PER_THREAD).coerceIn(MINIMUM_THREADS, MAXIMUM_THREADS)
