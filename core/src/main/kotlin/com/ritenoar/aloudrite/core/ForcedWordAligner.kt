package com.ritenoar.aloudrite.core

import com.k2fsa.sherpa.onnx.ForcedAligner
import com.k2fsa.sherpa.onnx.ForcedAlignerConfig
import java.nio.file.Path

private const val WORDS_TIER_NAME = "words"

// The WordAligner of sherpa-onnx: the forced aligner with the model of a folder. This is the one file that names its classes
class ForcedWordAligner(modelFolder: Path) : WordAligner {

    private val aligner: ForcedAligner

    init {
        val config = ForcedAlignerConfig()
        config.modelDir = modelFolder.toString()
        config.lexicon = modelFolder.resolve(ALIGNER_LEXICON_FILE_NAME).toString()
        aligner = ForcedAligner(config)
    }

    override fun align(samples: FloatArray, sampleRate: Int, words: List<String>): List<AlignedInterval> {
        val stream = aligner.createStream()
        try {
            stream.acceptWaveform(samples, sampleRate)
            val tiers = aligner.compute(stream, words.joinToString(" ")).tiers
            val wordsTier = tiers.firstOrNull { it.name == WORDS_TIER_NAME }
                ?: error("The aligner gave no tier named $WORDS_TIER_NAME, only ${tiers.map { it.name }}")
            return wordsTier.intervals.map { AlignedInterval(it.start.toDouble(), it.end.toDouble(), it.text) }
        } finally {
            stream.release()
        }
    }

    override fun close() = aligner.release()
}
