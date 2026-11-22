package com.ritenoar.aloudrite.core

import com.github.csukuangfj.piper.phonemize.PiperPhonemize
import java.io.PrintStream
import java.nio.file.Files
import java.nio.file.Paths

// A second process for the test of the data of eSpeak NG, since eSpeak NG is initialized once in a process. Given the
// folder that holds `espeak-ng-data` and a file with one sentence per line, it prints the phonemes that
// piper-phonemize gives for each sentence, one line each
fun main(arguments: Array<String>) {
    val output = PrintStream(System.out, true, Charsets.UTF_8)
    check(PiperPhonemize.initializeWithDataDir(arguments[0]) >= 0) { "eSpeak NG could not be initialized with ${arguments[0]}" }
    for (sentence in Files.readAllLines(Paths.get(arguments[1]))) {
        val phonemes = PiperPhonemize.text(sentence, "en-us").use { it.allPhonemesAsStrings.joinToString("") }
        output.println(phonemes)
    }
}
