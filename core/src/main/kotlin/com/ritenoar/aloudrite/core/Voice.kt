package com.ritenoar.aloudrite.core

// The language that eSpeak NG is given with each piece decides the pronunciation. `en-gb` gives no phonemes at all, and
// `en` is British English in eSpeak NG
enum class Accent(val espeakLanguage: String) {
    AMERICAN("en-us"),
    BRITISH("en"),
}

// The speaker numbers are those of the `id2speaker` metadata of both models, which hold the same voices in the same
// order. The Java binding of sherpa-onnx has no lookup of a voice by name
class Voice(val kokoroName: String, val displayName: String, val speakerNumber: Int, val accent: Accent)

// In the order of the list on the settings page
val VOICES: List<Voice> = listOf(
    Voice("af_heart", "Heart (American English, female)", 3, Accent.AMERICAN),
    Voice("af_bella", "Bella (American English, female)", 2, Accent.AMERICAN),
    Voice("af_kore", "Kore (American English, female)", 5, Accent.AMERICAN),
    Voice("af_sarah", "Sarah (American English, female)", 9, Accent.AMERICAN),
    Voice("af_aoede", "Aoede (American English, female)", 1, Accent.AMERICAN),
    Voice("af_nicole", "Nicole (American English, female, whispers)", 6, Accent.AMERICAN),
    Voice("am_puck", "Puck (American English, male)", 18, Accent.AMERICAN),
    Voice("am_fenrir", "Fenrir (American English, male)", 14, Accent.AMERICAN),
    Voice("am_michael", "Michael (American English, male)", 16, Accent.AMERICAN),
    Voice("bf_emma", "Emma (British English, female)", 21, Accent.BRITISH),
    Voice("bf_isabella", "Isabella (British English, female)", 22, Accent.BRITISH),
    Voice("bm_fable", "Fable (British English, male)", 25, Accent.BRITISH),
    Voice("bm_george", "George (British English, male)", 26, Accent.BRITISH),
)

val DEFAULT_VOICE: Voice = VOICES.first()

// The voice of a stored name, or null where the name is that of no voice of the list
fun voiceNamed(kokoroName: String): Voice? = VOICES.firstOrNull { it.kokoroName == kokoroName }
