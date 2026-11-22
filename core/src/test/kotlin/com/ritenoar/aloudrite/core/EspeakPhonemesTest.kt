package com.ritenoar.aloudrite.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

// Runs the real binding of piper-phonemize, only where the model, whose data it reads, is in its folder
class EspeakPhonemesTest {

    private val modelFolder = INT8_MODEL.folderIn(userModelsFolder())
    private val modelData = modelFolder.resolve(MODEL_DATA_DIRECTORY_NAME)

    @Before
    fun requireTheModelData() {
        assumeTrue(isSpeechSupported(System.getProperty("os.name"), System.getProperty("os.arch")))
        assumeTrue("The model is not in $modelFolder", INT8_MODEL.isInPlace(modelFolder))
    }

    @Test
    fun aWordIsGivenItsPhonemesWithTheDataOfTheModel() {
        EspeakPhonemes.useDataOf(modelData)

        assertEquals("ˌeɪpˌiːˈaɪ", phonemesOf("api", "en-us"))
    }

    // The data of a model that is removed after another one was chosen must not leave eSpeak NG without data
    // British English is the language `en` of eSpeak NG, and `en-gb` gives no phonemes
    @Test
    fun theLanguageEnGivesTheBritishPhonemesAndEnUsTheAmericanOnes() {
        EspeakPhonemes.useDataOf(modelData)

        assertEquals("ʃˈɛdjuːl", phonemesOf("schedule", "en"))
        assertEquals("təmˈɑːtəʊ", phonemesOf("tomato", "en"))
        assertEquals("skˈɛdʒuːl", phonemesOf("schedule", "en-us"))
        assertEquals("təmˈeɪɾoʊ", phonemesOf("tomato", "en-us"))
    }

    // The number of the first item and its words must reach eSpeak NG as one sentence, as unit 0037 wants it read
    @Test
    fun theFirstItemOfTheNumberedListOfTheSampleIsOneSentenceInAnAmericanAndABritishVoice() {
        EspeakPhonemes.useDataOf(modelData)
        val sample = Files.readString(Path.of("samples/markdown-features.md"))
        val piece = spokenPartsOf(sample).filterIsInstance<Words>().flatMap { piecesOf(it.text) }
            .first { it.startsWith("1, Climb") }

        for (accent in listOf(Accent.AMERICAN, Accent.BRITISH)) {
            assertEquals(accent.name, 1, EspeakPhonemes.sentencesOf(piece, accent.espeakLanguage).size)
        }
    }

    @Test
    fun theDataStaysReadableAfterAnotherCopyItWasPointedAtIsDeleted() {
        val copyFolder = Files.createTempDirectory("espeak-copy")
        try {
            copyTree(modelData, copyFolder.resolve(MODEL_DATA_DIRECTORY_NAME))
            EspeakPhonemes.useDataOf(copyFolder.resolve(MODEL_DATA_DIRECTORY_NAME))
            assertEquals("tˈuː θˈaʊzənd twˈɛnti sˈɪks", phonemesOf("2026", "en-us"))
            EspeakPhonemes.useDataOf(modelData)
        } finally {
            deleteTree(copyFolder)
        }

        assertEquals("tˈuː θˈaʊzənd twˈɛnti sˈɪks", phonemesOf("2026", "en-us"))
    }

    // Requirement 5 of unit 0035: a release that hands a character above U+FFFF intact to eSpeak NG reads U+1F600 by its name
    @Test
    fun anEmojiIsReadAsGrinningFace() {
        EspeakPhonemes.useDataOf(modelData)
        val sentence = "Before the sign, ${String(Character.toChars(0x1F600))} and after the sign."

        val phonemes = phonemesOf(sentence, "en-us")

        assertTrue("The phonemes of \"$sentence\" are $phonemes", phonemes.contains("ɡɹˈɪnɪŋ fˈeɪs"))
    }

    // Requirement 9 of unit 0035: what the plugin hands the engine for these sentences did not change with the move of
    // piper-phonemize from 1.5.9 to 1.6.1. The constants were found under 1.5.9 on the commit named in the record
    @Test
    fun thePhonemesHandedToTheEngineAreThoseOf1_5_9() {
        EspeakPhonemes.useDataOf(modelData)
        val tokenLines = Files.readAllLines(modelFolder.resolve(TOKENS_FILE_NAME))
        val phonemeRows = PhonemeRows(tokenCodePointsOf(tokenLines), EspeakPhonemes::sentencesOf)

        val found = EspeakDataTest.SENTENCES.associateWith { sentence ->
            phonemeRows.rowsOf(sentence, DEFAULT_VOICE.accent.espeakLanguage).joinToString(" | ") { String(it, 0, it.size) }
        }

        assertEquals(PHONEMES_UNDER_1_5_9.toString(), found.toString())
    }

    private companion object {
        // Found under piper-phonemize 1.5.9 on a80af4a, with the int8 model, on Linux x64
        val PHONEMES_UNDER_1_5_9: Map<String, String> = mapOf(
            "I have 3 cats in 2026." to "aɪ hæv θɹˈiː kˈæts ɪn tˈuː θˈaʊzənd twˈɛnti sˈɪks. ",
            "Version 4.2 is 5% faster & smaller." to "vˈɜːʒən fˈoːɹ pɔɪnt tˈuː ɪz fˈaɪv pɚsˈɛnt fˈæstɚ ænd smˈɔːlɚ. ",
            "Read the README of the API with npm." to "ɹˈiːd ðə ɹˈiːdmiː ʌvðɪ ˌeɪpˌiːˈaɪ wɪð ˌɛnpˌiːˈɛm. ",
            "It builds for x86_64 and ARM64 with v1.13.8." to "ɪt bˈɪldz fɔːɹ ˈɛks ˈeɪɾi sˈɪks sˈɪksti fˈoːɹ ænd ˌeɪˌɑːɹɹˈɛm sˈɪksti fˈoːɹ wɪð vˈiː wˈʌn pɔɪnt wˈʌn θɹˈiːpɔɪnt ˈeɪt. ",
            "AloudRite reads Markdown aloud." to "ɐlˈaʊd ɹˈaɪt ɹˈiːdz mˈɑːɹkdaʊn ɐlˈaʊd. ",
            "Before the sign, and after the sign." to "bᵻfˌoːɹ ðə sˈaɪn, ænd ˈæftɚ ðə sˈaɪn. ",
            "Before the sign, ❤ and after the sign." to "bᵻfˌoːɹ ðə sˈaɪn, ɹˈɛd hˈɑːɹt ænd ˈæftɚ ðə sˈaɪn. ",
        )
    }

    private fun phonemesOf(text: String, language: String): String =
        EspeakPhonemes.sentencesOf(text, language).joinToString("") { String(it, 0, it.size) }

    private fun copyTree(from: Path, to: Path) {
        Files.walk(from).use { paths ->
            paths.forEach { path ->
                val target = to.resolve(from.relativize(path).toString())
                if (Files.isDirectory(path)) Files.createDirectories(target) else Files.copy(path, target)
            }
        }
    }

    private fun deleteTree(folder: Path) {
        Files.walk(folder).use { paths -> paths.sorted(Comparator.reverseOrder()).forEach { Files.delete(it) } }
    }
}
