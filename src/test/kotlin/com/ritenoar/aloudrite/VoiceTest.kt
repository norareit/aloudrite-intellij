package com.ritenoar.aloudrite

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class VoiceTest {

    @Test
    fun theListHoldsTheThirteenVoicesInOrderWithTheirNamesAndSpeakerNumbers() {
        val expected = listOf(
            Triple("af_heart", "Heart (American English, female)", 3),
            Triple("af_bella", "Bella (American English, female)", 2),
            Triple("af_kore", "Kore (American English, female)", 5),
            Triple("af_sarah", "Sarah (American English, female)", 9),
            Triple("af_aoede", "Aoede (American English, female)", 1),
            Triple("af_nicole", "Nicole (American English, female, whispers)", 6),
            Triple("am_puck", "Puck (American English, male)", 18),
            Triple("am_fenrir", "Fenrir (American English, male)", 14),
            Triple("am_michael", "Michael (American English, male)", 16),
            Triple("bf_emma", "Emma (British English, female)", 21),
            Triple("bf_isabella", "Isabella (British English, female)", 22),
            Triple("bm_fable", "Fable (British English, male)", 25),
            Triple("bm_george", "George (British English, male)", 26),
        )

        assertEquals(expected, VOICES.map { Triple(it.kokoroName, it.displayName, it.speakerNumber) })
    }

    @Test
    fun theDefaultVoiceIsHeart() {
        assertEquals("af_heart", DEFAULT_VOICE.kokoroName)
        assertSame(VOICES.first(), DEFAULT_VOICE)
    }

    @Test
    fun anAmericanVoiceHasTheLanguageEnUsAndABritishOneTheLanguageEn() {
        for (voice in VOICES) {
            val language = if (voice.kokoroName.startsWith("af_") || voice.kokoroName.startsWith("am_")) "en-us" else "en"
            assertEquals(voice.kokoroName, language, voice.accent.espeakLanguage)
        }
        assertEquals(setOf("af_", "am_", "bf_", "bm_"), VOICES.map { it.kokoroName.take(3) }.toSet())
    }

    @Test
    fun voiceNamedGivesTheVoiceOfEachNameAndNullForAnyOtherName() {
        VOICES.forEach { assertSame(it, voiceNamed(it.kokoroName)) }
        assertNull(voiceNamed("af_alloy"))
        assertNull(voiceNamed(""))
    }
}
