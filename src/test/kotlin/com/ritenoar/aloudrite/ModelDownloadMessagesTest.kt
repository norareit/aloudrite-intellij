package com.ritenoar.aloudrite

import com.ritenoar.aloudrite.core.ALIGNER_MODEL
import com.ritenoar.aloudrite.core.FP32_MODEL
import com.ritenoar.aloudrite.core.INT8_MODEL
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Paths

class ModelDownloadMessagesTest {

    @Test
    fun theQuestionStatesTheSizesTheAddressAndTheFolder() {
        val folder = Paths.get("/data/aloudrite/kokoro-int8-multi-lang-v1_0")

        assertEquals(
            "AloudRite needs its speech model (Kokoro 1.0, int8) to speak, and the model is not on this machine yet. Download it now?\n\n" +
                "The download is 132 MB, from " +
                "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-int8-multi-lang-v1_0.tar.bz2. " +
                "Once unpacked, the model takes 190 MB in /data/aloudrite/kokoro-int8-multi-lang-v1_0.",
            downloadQuestion(INT8_MODEL, folder))
    }

    @Test
    fun theQuestionsOfThePageNameTheModelItsSizesAndItsFolder() {
        val folder = Paths.get("/data/aloudrite/kokoro-multi-lang-v1_0")
        val facts = "The download is 350 MB, from " +
            "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-multi-lang-v1_0.tar.bz2. " +
            "Once unpacked, the model takes 401 MB in /data/aloudrite/kokoro-multi-lang-v1_0."

        assertEquals("Download Kokoro 1.0, fp32?\n\n$facts", downloadQuestionFromThePage(FP32_MODEL, folder))
        assertEquals(
            "Kokoro 1.0, fp32 is not on this machine yet. Download it, and use it once it is in place?\n\n$facts",
            chooseQuestion(FP32_MODEL, folder))
    }

    @Test
    fun theQuestionOfARemovalNamesTheModelItsFolderAndTheMegabytesItFrees() {
        assertEquals(
            "Remove Kokoro 1.0, fp32?\n\nThis deletes /data/aloudrite/kokoro-multi-lang-v1_0 and frees 401 MB. " +
                "It can be downloaded again from the settings page.",
            removeQuestion(FP32_MODEL, Paths.get("/data/aloudrite/kokoro-multi-lang-v1_0")))
    }

    @Test
    fun theQuestionsBeforeADownloadStateTheSizeOfTheAlignerModelWhereItIsNotOnTheMachine() {
        val folder = Paths.get("/data/aloudrite/kokoro-multi-lang-v1_0")
        val facts = "The download is 397 MB: 350 MB from " +
            "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-multi-lang-v1_0.tar.bz2, and 48 MB from " +
            "https://github.com/k2-fsa/sherpa-onnx/releases/download/fa-models/english-mfa-v3.1.0.tar.bz2 " +
            "for Word timing (english-mfa-v3.1.0), which finds the word being read. " +
            "Once unpacked, the model takes 401 MB in /data/aloudrite/kokoro-multi-lang-v1_0, and " +
            "Word timing (english-mfa-v3.1.0) takes 52 MB in /data/aloudrite/english-mfa-v3.1.0."

        assertEquals(
            "AloudRite needs its speech model (Kokoro 1.0, fp32) to speak, and the model is not on this machine yet. " +
                "Download it now?\n\n$facts",
            downloadQuestion(FP32_MODEL, folder, ALIGNER_MODEL))
        assertEquals("Download Kokoro 1.0, fp32?\n\n$facts", downloadQuestionFromThePage(FP32_MODEL, folder, ALIGNER_MODEL))
        assertEquals(
            "Kokoro 1.0, fp32 is not on this machine yet. Download it, and use it once it is in place?\n\n$facts",
            chooseQuestion(FP32_MODEL, folder, ALIGNER_MODEL))
    }

    @Test
    fun theQuestionOfTheAlignerModelAloneStatesItsSizesAndItsFolder() {
        assertEquals(
            "Download Word timing (english-mfa-v3.1.0)?\n\nIt finds the word being read, so that the highlight of the word " +
                "follows the voice. Until it is in place, the highlight estimates the word. The download is 48 MB, from " +
                "https://github.com/k2-fsa/sherpa-onnx/releases/download/fa-models/english-mfa-v3.1.0.tar.bz2. " +
                "Once unpacked, it takes 52 MB in /data/aloudrite/english-mfa-v3.1.0.",
            downloadAlignerQuestion(ALIGNER_MODEL, Paths.get("/data/aloudrite/english-mfa-v3.1.0")))
    }

    @Test
    fun theQuestionOfTheRemovalOfTheLastSpeechModelCountsTheAlignerModelToo() {
        assertEquals(
            "Remove Kokoro 1.0, fp32?\n\nThis deletes /data/aloudrite/kokoro-multi-lang-v1_0 and frees 401 MB. " +
                "As no other speech model is in place, it also deletes Word timing (english-mfa-v3.1.0) in " +
                "/data/aloudrite/english-mfa-v3.1.0 and frees 52 MB more. It can be downloaded again from the settings page.",
            removeQuestion(FP32_MODEL, Paths.get("/data/aloudrite/kokoro-multi-lang-v1_0"), ALIGNER_MODEL))
    }

    @Test
    fun theRefusalsOfARemovalNameTheModelAndTheFolder() {
        assertTrue(removeRefusedMessage(FP32_MODEL).startsWith("Kokoro 1.0, fp32 is in use"))
        assertTrue(removeRefusedNotThePluginsMessage(Paths.get("/data/x")).contains("/data/x"))
    }

    @Test
    fun sizesAreRoundedToTheNearestMegabyte() {
        assertEquals(132L, megabytes(132_303_094L))
        assertEquals(190L, megabytes(189_882_497L))
        assertEquals(0L, megabytes(499_999L))
        assertEquals(1L, megabytes(500_000L))
    }

    @Test
    fun everyKindOfFailureHasItsText() {
        val folder = Paths.get("/data/aloudrite/model")
        val expected = mapOf(
            InstallFailure.NoConnection("github.com") to
                "There is no connection to github.com. Check the network and the proxy settings of the IDE.",
            InstallFailure.ServerAnswer(404) to "The download address answered with HTTP 404.",
            InstallFailure.RedirectRefused("http://example.com/x") to
                "The download address sent AloudRite on to http://example.com/x, which is not an HTTPS address, " +
                "so AloudRite did not follow it.",
            InstallFailure.TooManyRedirects to "The download address redirected AloudRite more than 10 times.",
            InstallFailure.BrokeOff(66_000_000L, 132_303_094L) to "The download broke off after 66 of 132 MB.",
            InstallFailure.OutOfDiskSpace(folder, INT8_MODEL.modelPackage) to
                "There is not enough free disk space in /data/aloudrite/model. The download and the unpacking need " +
                "about 322 MB for a moment, and the model takes 190 MB after.",
            InstallFailure.CannotUnpack("Unexpected end of file") to
                "The package could not be unpacked: Unexpected end of file.",
            InstallFailure.ChecksumMismatch("https://example.com/p.tar.bz2") to
                "The package at https://example.com/p.tar.bz2 is not the one this version of AloudRite was built " +
                "for: its SHA-256 checksum does not match. The publisher has replaced the package at that address " +
                "before. The README of AloudRite says how to put a model in place by hand.",
            InstallFailure.FolderInTheWay(folder) to
                "The folder /data/aloudrite/model exists and does not hold a complete model. Remove it, then try again.",
            InstallFailure.RunningElsewhere to
                "A download of the AloudRite speech model is already running in another IDE window.",
            InstallFailure.Unexpected("Permission denied") to "Permission denied",
        )

        expected.forEach { (failure, text) -> assertEquals(text, failureText(failure)) }
    }
}
