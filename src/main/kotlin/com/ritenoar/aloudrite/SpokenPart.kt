package com.ritenoar.aloudrite

// What a reading is made of: words, which the engine makes into audio, and silences, which the plugin adds itself
sealed interface SpokenPart

data class Words(val text: String) : SpokenPart

data class Silence(val milliseconds: Int) : SpokenPart
