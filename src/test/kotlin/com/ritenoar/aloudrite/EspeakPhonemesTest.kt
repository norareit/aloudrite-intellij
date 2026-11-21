package com.ritenoar.aloudrite

import org.junit.Assert.assertEquals
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

    // Tells whether the release of piper-phonemize in use hands a character above U+FFFF intact to eSpeak NG, and fails on
    // neither outcome. A release with the fix gives "grinning face" for the emoji, and one without leaves it out
    @Test
    fun anEmojiIsReportedAsReadOrLeftOut() {
        EspeakPhonemes.useDataOf(modelData)
        val sentence = "Before the sign, ${String(Character.toChars(0x1F600))} and after the sign."

        val phonemes = phonemesOf(sentence, "en-us")

        val read = phonemes.contains("ɡɹˈɪnɪŋ fˈeɪs")
        println("piper-phonemize reads U+1F600 as \"grinning face\": $read. The phonemes of \"$sentence\" are: $phonemes")
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
