package com.ritenoar.aloudrite.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.ZipFile

// The plugin carries the Java archive of piper-phonemize without its copy of the data of eSpeak NG and reads the data of
// the model folder. This finds that both give the same phonemes. eSpeak NG is initialized once in a process, so each copy
// is read in a process of its own
class EspeakDataTest {

    private val modelFolder = INT8_MODEL.folderIn(userModelsFolder())

    @Before
    fun requireTheModelData() {
        assumeTrue(isSpeechSupported(System.getProperty("os.name"), System.getProperty("os.arch")))
        assumeTrue("The model is not in $modelFolder", INT8_MODEL.isInPlace(modelFolder))
    }

    @Test
    fun theDataOfTheModelFolderGivesTheSamePhonemesAsTheDataOfTheJavaArchiveAsPublished() {
        val workFolder = Files.createTempDirectory("espeak-data")
        try {
            val sentencesFile = workFolder.resolve("sentences.txt")
            Files.write(sentencesFile, SENTENCES)
            val publishedData = workFolder.resolve("published")
            unpackDataOfThePublishedArchive(publishedData)

            val withTheModelData = phonemesRead(modelFolder, sentencesFile)
            val withThePublishedData = phonemesRead(publishedData, sentencesFile)

            assertEquals(SENTENCES.size, withTheModelData.size)
            assertTrue("The phonemes are empty: $withTheModelData", withTheModelData.all { it.length > 5 })
            assertEquals(withThePublishedData, withTheModelData)
        } finally {
            Files.walk(workFolder).use { paths -> paths.sorted(Comparator.reverseOrder()).forEach { Files.delete(it) } }
        }
    }

    // The entries of `piper-phonemize/espeak-ng-data/` of the archive go to `espeak-ng-data/` of the folder
    private fun unpackDataOfThePublishedArchive(folder: Path) {
        val archive = checkNotNull(System.getProperty("aloudrite.test.publishedPiperPhonemizeJvm")) {
            "The build gives the test the path of the published archive in aloudrite.test.publishedPiperPhonemizeJvm"
        }
        val prefix = "piper-phonemize/$MODEL_DATA_DIRECTORY_NAME/"
        ZipFile(archive).use { zip ->
            for (entry in zip.entries().asSequence().filter { it.name.startsWith(prefix) }) {
                val target = folder.resolve(MODEL_DATA_DIRECTORY_NAME).resolve(entry.name.removePrefix(prefix))
                if (entry.isDirectory) {
                    Files.createDirectories(target)
                } else {
                    Files.createDirectories(target.parent)
                    zip.getInputStream(entry).use { Files.copy(it, target) }
                }
            }
        }
        assertTrue("The archive holds no data", Files.isDirectory(folder.resolve(MODEL_DATA_DIRECTORY_NAME)))
    }

    private fun phonemesRead(folderWithData: Path, sentencesFile: Path): List<String> {
        val java = Paths.get(System.getProperty("java.home"), "bin", "java").toString()
        // The classpath of the tests is too long for one argument on Linux, so it goes in an argument file
        val argumentFile = Files.createTempFile("phonemes-probe", ".args")
        try {
            Files.writeString(argumentFile, "-cp \"${System.getProperty("java.class.path")}\"")
            val process = ProcessBuilder(
                java, "@$argumentFile", "com.ritenoar.aloudrite.core.PhonemesProbeProcessKt", folderWithData.toString(), sentencesFile.toString(),
            ).redirectError(ProcessBuilder.Redirect.INHERIT).start()
            val lines = process.inputStream.bufferedReader(Charsets.UTF_8).readLines()
            assertEquals("The probe says: $lines", 0, process.waitFor())
            return lines
        } finally {
            Files.deleteIfExists(argumentFile)
        }
    }

    companion object {
        val SENTENCES = listOf(
            "I have 3 cats in 2026.",
            "Version 4.2 is 5% faster & smaller.",
            "Read the README of the API with npm.",
            "It builds for x86_64 and ARM64 with v1.13.8.",
            "AloudRite reads Markdown aloud.",
            "Before the sign, and after the sign.",
            "Before the sign, ❤ and after the sign.",
        )
    }
}
