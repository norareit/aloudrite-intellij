package com.ritenoar.aloudrite.core

// The step the aligner gives its times in: the frame_shift of 10 ms in the features of meta.json of the model
// english-mfa-v3.1.0, so every time it gives is a whole number of steps
const val ALIGNER_STEP_SECONDS = 0.01

// The aligner rounds an end to a whole step and the audio is not a whole number of steps long, so an end may lie past the
// audio by less than one step. An end one step or more past it is a misplaced word
fun isEndWithinTheAudio(endSeconds: Double, audioSeconds: Double): Boolean = endSeconds < audioSeconds + ALIGNER_STEP_SECONDS
