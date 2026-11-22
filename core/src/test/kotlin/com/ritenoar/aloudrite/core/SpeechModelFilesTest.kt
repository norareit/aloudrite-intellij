package com.ritenoar.aloudrite.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechModelFilesTest {

    private val commit = "a".repeat(40)
    private val sha256 = "b".repeat(64)
    private val modelLine = "model\tfirst\towner/first\t$commit"

    private fun fileLine(path: String, size: String = "5", sum: String = sha256) = "file\t$size\t$sum\t$path"

    private fun parse(vararg lines: String) = parseSpeechModelFiles(lines.joinToString("\n"), "list.tsv")

    private fun rejection(vararg lines: String): String =
        assertThrows(IllegalArgumentException::class.java) { parse(*lines) }.message!!

    @Test
    fun twoModelsAreReadIntoTheirRepositoriesCommitsAndFilesInOrder() {
        val second = "c".repeat(40)

        val models = parse(
            "# a comment", modelLine, fileLine("a.txt", "5"), fileLine("dir/b.txt", "7"), "",
            "model\tsecond\towner/second\t$second", fileLine("c.txt", "9"))

        assertEquals(listOf("first", "second"), models.keys.toList())
        assertEquals(
            RepositoryFiles("owner/first", commit, listOf(RepositoryFile("a.txt", 5, sha256), RepositoryFile("dir/b.txt", 7, sha256))),
            models.getValue("first"))
        assertEquals(RepositoryFiles("owner/second", second, listOf(RepositoryFile("c.txt", 9, sha256))), models.getValue("second"))
    }

    @Test
    fun aPathWithASpaceIsReadWhole() {
        val models = parse(modelLine, fileLine("espeak-ng-data/voices/!v/Mr serious"))

        assertEquals("espeak-ng-data/voices/!v/Mr serious", models.getValue("first").files.single().path)
    }

    @Test
    fun aLineOfAnotherKindIsRejectedWithItsNumber() {
        assertEquals("list.tsv line 3: the line is neither a model nor a file line", rejection(modelLine, fileLine("a"), "other\tx"))
    }

    @Test
    fun aFileLineBeforeAnyModelLineIsRejected() {
        assertEquals("list.tsv line 1: a file line comes before any model line", rejection(fileLine("a")))
    }

    @Test
    fun aSizeThatIsNotAPositiveNumberIsRejected() {
        listOf("0", "-1", "ten", "").forEach {
            assertEquals("list.tsv line 2: the size is not a positive number", rejection(modelLine, fileLine("a", size = it)))
        }
    }

    @Test
    fun aSha256ThatIsNotSixtyFourLowercaseHexadecimalDigitsIsRejected() {
        listOf("b".repeat(63), "B".repeat(64), "g".repeat(64), "").forEach {
            assertEquals(
                "list.tsv line 2: the SHA-256 is not 64 lowercase hexadecimal digits", rejection(modelLine, fileLine("a", sum = it)))
        }
    }

    @Test
    fun aCommitThatIsNotFortyLowercaseHexadecimalDigitsIsRejected() {
        listOf("a".repeat(39), "A".repeat(40), "main").forEach {
            assertEquals(
                "list.tsv line 1: the commit is not 40 lowercase hexadecimal digits", rejection("model\tfirst\towner/first\t$it"))
        }
    }

    @Test
    fun aPathThatCouldLeaveTheFolderOfTheModelIsRejected() {
        mapOf(
            "/etc/passwd" to "the path is absolute",
            "a\\b" to "the path holds a backslash",
            "a//b" to "the path has an empty segment",
            "a/" to "the path has an empty segment",
            "a/./b" to "the path holds . or ..",
            "../b" to "the path holds . or ..",
        ).forEach { (path, reason) -> assertEquals("list.tsv line 2: $reason", rejection(modelLine, fileLine(path))) }
    }

    @Test
    fun aPathThatRepeatsIsRejected() {
        assertEquals("list.tsv line 3: the path a is listed twice", rejection(modelLine, fileLine("a"), fileLine("a")))
    }

    @Test
    fun aLineWithTheWrongNumberOfFieldsIsRejected() {
        assertEquals(
            "list.tsv line 1: a model line holds the package name, the repository and the commit", rejection("model\tfirst"))
        assertEquals("list.tsv line 2: a file line holds the size, the SHA-256 and the path", rejection(modelLine, "file\t5"))
    }

    @Test
    fun aModelListedTwiceIsRejected() {
        assertEquals("list.tsv line 2: the model first is listed twice", rejection(modelLine, modelLine))
    }

    @Test
    fun theResourceFileOfThePluginHoldsTheTwoKokoroModelsAtTheirCommits() {
        val models = loadSpeechModelFiles()

        assertEquals(listOf("kokoro-int8-multi-lang-v1_0", "kokoro-multi-lang-v1_0"), models.keys.toList())
        val int8 = models.getValue("kokoro-int8-multi-lang-v1_0")
        val fp32 = models.getValue("kokoro-multi-lang-v1_0")
        assertEquals("csukuangfj/kokoro-int8-multi-lang-v1_0", int8.repository)
        assertEquals("57adb37fc0e8d4bd430cfc1b8f8c2294945b9af2", int8.commit)
        assertEquals("csukuangfj/kokoro-multi-lang-v1_0", fp32.repository)
        assertEquals("f7b96bb6bef5c5da4d3aa4f4e0498fbbf62dc78b", fp32.commit)
        assertEquals(377, int8.files.size)
        assertEquals(377, fp32.files.size)
        assertEquals(189_881_734L, int8.sizeInBytes)
        assertEquals(401_239_297L, fp32.sizeInBytes)
    }

    @Test
    fun theResourceFileNamesTheModelFilesByTheirSizeAndSha256() {
        val models = loadSpeechModelFiles()

        assertEquals(
            RepositoryFile("model.int8.onnx", 114_202_993L, "75635ea3b85a32d00094dcdd61a258aba87c160f7c264c304a7b3ae9d734195e"),
            models.getValue("kokoro-int8-multi-lang-v1_0").files.single { it.path == "model.int8.onnx" })
        assertEquals(
            RepositoryFile("model.onnx", 325_560_556L, "b40f62b166ac8164b0627ef48a0b358eda0985e272fb03ef5252e7206305da11"),
            models.getValue("kokoro-multi-lang-v1_0").files.single { it.path == "model.onnx" })
    }

    @Test
    fun theResourceFileHoldsNoGitAttributesAndEveryFileThatAModelInPlaceNeeds() {
        SPEECH_MODELS.forEach { model ->
            val paths = (model.modelPackage as RepositoryFiles).files.map { it.path }
            assertFalse(paths.contains(".gitattributes"))
            (SHARED_MODEL_FILE_NAMES + model.modelFileName).forEach { assertTrue("${model.packageName} lacks $it", it in paths) }
            assertTrue(paths.any { it.startsWith("$MODEL_DATA_DIRECTORY_NAME/") })
            assertEquals("Mr serious", paths.single { it.endsWith("/Mr serious") }.substringAfterLast('/'))
        }
    }

    @Test
    fun theFilesOfTheResourceFileAreSortedByPathInByteOrder() {
        loadSpeechModelFiles().values.forEach { files ->
            val paths = files.files.map { it.path.toByteArray() }
            assertTrue(paths.zipWithNext().all { (before, after) -> java.util.Arrays.compareUnsigned(before, after) < 0 })
        }
    }
}
