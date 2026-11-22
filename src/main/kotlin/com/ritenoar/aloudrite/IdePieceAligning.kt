package com.ritenoar.aloudrite

import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.concurrency.AppExecutorUtil

private val LOG = Logger.getInstance(PieceAligning::class.java)

// One thread, which the aligner is made and used on
private val ALIGNMENT_EXECUTOR by lazy { AppExecutorUtil.createBoundedApplicationPoolExecutor("AloudRite alignment", 1) }

fun pieceAligningOfTheIde(): PieceAligning = PieceAligning(
    { ALIGNER_MODEL.isInPlace(ALIGNER_MODEL.folderIn(userModelsFolder())) },
    { ForcedWordAligner(ALIGNER_MODEL.folderIn(userModelsFolder())) },
    { work -> ALIGNMENT_EXECUTOR.execute(work) },
    { failure -> LOG.warn("AloudRite could not align the words of a piece with its audio", failure) },
)
