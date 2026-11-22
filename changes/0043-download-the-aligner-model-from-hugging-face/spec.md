# Download the aligner model from Hugging Face

## Context

Unit 0041 moves the download of the speech models from the GitHub release `tts-models` of sherpa-onnx, whose assets
the maintainer replaced under the same names, to their repositories on Hugging Face at fixed commits, and adds the
Gradle task `checkModelSources` and a weekly workflow that check that those commits are still served. It left the
aligner model `english-mfa-v3.1.0` of unit 0028 on the GitHub release `fa-models`, because it was then thought not to be
on Hugging Face.

It is. The workflow `upload-fa-models.yaml` of sherpa-onnx packs the aligner models and pushes the archives to the
repository `csukuangfj/fa-models` on Hugging Face, in the same run that uploads them to the release `fa-models`. On
2026-11-22 the specifier found there, at the latest commit `39914ed8514a568e2d24a5e785608e1abb429133` of
2026-11-13, the archive `english-mfa-v3.1.0.tar.bz2` of 47,567,475 bytes with the SHA-256
`86235e50fc9a8d9351c5622756c2cacd5164e3211fcbdf84bedbd00c98e916da`: the archive the plugin pins today, byte for byte.

The release `fa-models` is in use and changes: all its assets were uploaded again on 2026-11-13, and its `checksum.txt`
on 2026-11-22. An asset replaced there would fail the download of the aligner model, as it did for the speech models.

The human asked for the aligner model to be downloaded from Hugging Face as well. This unit is built after unit 0041 is
merged, as it extends what 0041 adds.

## Requirements

1. The aligner model is downloaded from
   `https://huggingface.co/csukuangfj/fa-models/resolve/39914ed8514a568e2d24a5e785608e1abb429133/english-mfa-v3.1.0.tar.bz2`,
   as one archive, checked against the size and the SHA-256 the plugin pins today, and unpacked as today. Nothing else
   about its download changes: when it is downloaded, the question before it, its failure and "Try Again".
2. `checkModelSources` checks the aligner model too: that the commit exists in `csukuangfj/fa-models`, and that the
   headers of the archive at that commit give its size and its SHA-256. A mismatch fails the task naming the aligner
   model, the address and what did not match, as for a speech model. The weekly workflow runs it with no change of its
   own.
3. Automated tests cover the address of requirement 1 and the check of requirement 2 with stand-in answers, a match
   and a missing commit.
4. `README.md` and `THIRD-PARTY.md` name the repository and the commit as where the aligner model comes from, in place
   of the release `fa-models`, keeping the attribution its license CC BY 4.0 requires. The instructions of `README.md`
   for putting it in place by hand use the new address and the same SHA-256.
5. `./gradlew checkModelSources` succeeds against Hugging Face, and the aligner model is downloaded through the plugin
   from a machine on which it is not in place, after which the word is highlighted in a reading. Both are recorded in
   `record.md`.

## Decisions

- **The archive, not the files in it.** Hugging Face holds the aligner model as the same `.tar.bz2` the release holds,
  so the download of an archive that the plugin has stays as it is and only its address changes. A file at a commit
  does not change, which is what the move is for.
- **The same weekly check as the speech models.** The aligner model now depends on a commit on Hugging Face in the
  same way, and can be lost in the same way, so `checkModelSources` covers it rather than a check of its own.

## Non-goals

- The other aligner models of `csukuangfj/fa-models`, which the plugin does not use
- A newer version of the aligner model

## Beyond the request

None.
