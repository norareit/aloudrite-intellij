package com.ritenoar.aloudrite.core

import com.ritenoar.aloudrite.sources.SourceAnswer
import com.ritenoar.aloudrite.sources.largestFile
import com.ritenoar.aloudrite.sources.modelSources
import com.ritenoar.aloudrite.sources.problemsOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelSourcesCheckTest {

    private val commit = "a".repeat(40)
    private val small = RepositoryFile("small.txt", 10, "1".repeat(64))
    private val large = RepositoryFile("dir/large file.onnx", 5_000, "2".repeat(64))
    private val files = RepositoryFiles("owner/model", commit, listOf(small, large))

    private val revisionOk = SourceAnswer(200, emptyMap(), "{\"_id\":\"x\",\"sha\":\"$commit\",\"siblings\":[]}")
    private val headOk = SourceAnswer(
        302, mapOf("x-repo-commit" to commit, "x-linked-size" to "5000", "x-linked-etag" to "\"${large.sha256}\""))

    private fun problems(revision: SourceAnswer = revisionOk, head: SourceAnswer = headOk) = problemsOf("model", files, revision, head)

    @Test
    fun answersThatMatchGiveNoProblem() {
        assertEquals(emptyList<String>(), problems())
    }

    @Test
    fun theLargestFileOfTheListIsTheOneAskedFor() {
        assertEquals(large, largestFile(files))
    }

    @Test
    fun aMissingCommitIsAProblemThatNamesTheModelTheAddressAndTheCommit() {
        val problem = problems(revision = SourceAnswer(404, emptyMap())).single()

        assertEquals(
            "model, https://huggingface.co/api/models/owner/model/revision/$commit: the commit $commit is missing from owner/model",
            problem)
    }

    @Test
    fun aRevisionThatAnswersWithAnotherCommitIsAProblem() {
        val other = "b".repeat(40)

        val problem = problems(revision = SourceAnswer(200, emptyMap(), "{\"sha\":\"$other\"}")).single()

        assertTrue(problem, problem.contains("the commit $commit is missing, the revision answered with $other"))
    }

    @Test
    fun aSizeAndAnEtagThatDifferEachGiveAProblemWithBothValues() {
        val head = SourceAnswer(302, mapOf("x-repo-commit" to commit, "x-linked-size" to "4999", "x-linked-etag" to "\"${"3".repeat(64)}\""))

        val found = problems(head = head)

        assertEquals(2, found.size)
        assertTrue(found[0], found[0].contains("x-linked-size is 4999, the list has 5000"))
        assertTrue(found[1], found[1].contains("x-linked-etag is ${"3".repeat(64)}, the list has ${large.sha256}"))
        assertTrue(found[0], found[0].startsWith("model, https://huggingface.co/owner/model/resolve/$commit/dir/large%20file.onnx: "))
    }

    @Test
    fun aCommitHeaderThatDiffersAndAMissingHeaderEachGiveAProblem() {
        val head = SourceAnswer(302, mapOf("x-repo-commit" to "c".repeat(40), "x-linked-size" to "5000"))

        val found = problems(head = head)

        assertEquals(2, found.size)
        assertTrue(found[0], found[0].contains("x-repo-commit is ${"c".repeat(40)}, the list has $commit"))
        assertTrue(found[1], found[1].contains("x-linked-etag is missing"))
    }

    @Test
    fun aStatusOf4xxOr5xxOfTheHeadIsAProblem() {
        listOf(404, 500).forEach { assertTrue(problems(head = SourceAnswer(it, headOk.headers)).single().contains("HTTP $it")) }
    }

    private val alignerCommit = "39914ed8514a568e2d24a5e785608e1abb429133"
    private val alignerRevisionOk = SourceAnswer(200, emptyMap(), "{\"sha\":\"$alignerCommit\"}")
    private val alignerHeadOk = SourceAnswer(
        302,
        mapOf(
            "x-repo-commit" to alignerCommit,
            "x-linked-size" to "47567475",
            "x-linked-etag" to "\"86235e50fc9a8d9351c5622756c2cacd5164e3211fcbdf84bedbd00c98e916da\""))

    @Test
    fun answersThatMatchTheAlignerModelGiveNoProblem() {
        assertEquals(
            emptyList<String>(),
            problemsOf("english-mfa-v3.1.0", ALIGNER_MODEL_SOURCE, alignerRevisionOk, alignerHeadOk))
    }

    @Test
    fun aMissingCommitOfTheAlignerModelIsAProblemThatNamesIt() {
        val problem = problemsOf("english-mfa-v3.1.0", ALIGNER_MODEL_SOURCE, SourceAnswer(404, emptyMap()), alignerHeadOk).single()

        assertEquals(
            "english-mfa-v3.1.0, https://huggingface.co/api/models/csukuangfj/fa-models/revision/$alignerCommit: " +
                "the commit $alignerCommit is missing from csukuangfj/fa-models",
            problem)
    }

    @Test
    fun theLargestFileOfTheAlignerSourceIsTheArchive() {
        assertEquals(
            "https://huggingface.co/csukuangfj/fa-models/resolve/$alignerCommit/english-mfa-v3.1.0.tar.bz2",
            ALIGNER_MODEL_SOURCE.fileAddress(largestFile(ALIGNER_MODEL_SOURCE)))
    }

    @Test
    fun theModelsToCheckAreTheSpeechModelsThenTheAlignerModel() {
        assertEquals(loadSpeechModelFiles() + (ALIGNER_MODEL.packageName to ALIGNER_MODEL_SOURCE), modelSources())
    }
}
