# Plan: the aligner test allows an end rounded past the audio

## Approach

The step of the aligner's times is in the model's configuration. The aligner is the GMM-HMM model `english-mfa-v3.1.0`,
and the `features` object of its `meta.json` holds `"frame_shift": 10`, in milliseconds, with `"frame_length": 25` and
`"sample_frequency": 16000`. The aligner places each phone on whole frames of its features, so every time it gives is a
whole number of 10 ms steps, which agrees with the 0.96 s and 1.39 s of the failure. The step is therefore 10 ms, and
the test holds it as a constant of 0.01 s with a comment that names where it was found (R2).

The bound is a pure function in a new file of the test sources of `core`, beside the other helpers on plain values
there such as `HighlightFollowing.kt`. It takes the end of an interval and the length of the audio, both in seconds,
and says whether the end lies at or before the end of the audio or past it by less than one step:
`endSeconds < audioSeconds + ALIGNER_STEP_SECONDS`. An end of exactly one step past the audio, computed as
`audioSeconds + ALIGNER_STEP_SECONDS`, is then equal to the bound and fails, so the unit test of that case does not
depend on rounding.

`RealAlignerTest` splits its one assertion on the start and the end into two: the start is still at or after 0 as
before, and the end is checked with the new function. Its check that a start is at or before its end, the counts and
texts of the intervals, and the call to `wordStartsOf` are left as they are (R3). Nothing under `src/main` or
`core/src/main` changes (R5).

## Files

* `core/src/test/kotlin/com/ritenoar/aloudrite/core/AlignedEnds.kt`: new, the constant of the step and the function of
  the bound
* `core/src/test/kotlin/com/ritenoar/aloudrite/core/AlignedEndsTest.kt`: new, the unit test of the bound, which needs
  no model
* `core/src/test/kotlin/com/ritenoar/aloudrite/core/RealAlignerTest.kt`: the check on the end uses the bound
* `changes/0039-the-aligner-test-allows-an-end-rounded-past-the-audio/record.md`: the log of the work

## Tasks

1. [x] Write `AlignedEndsTest` with three tests of a function `isEndWithinTheAudio(endSeconds: Double, audioSeconds:
   Double): Boolean`, which does not exist yet: an end of `1.3899997472763062` s in `1.3851666666666667` s of audio
   passes, an end equal to the length of the audio passes, and an end of `audioSeconds + ALIGNER_STEP_SECONDS` fails.
   Run `./gradlew :core:test --tests com.ritenoar.aloudrite.core.AlignedEndsTest` and see it fail to compile, as the
   function and the constant are missing (R4)
2. [x] Write `AlignedEnds.kt` with `const val ALIGNER_STEP_SECONDS = 0.01`, under a comment that says it is the step the
   aligner gives its times in, the `frame_shift` of 10 ms in the `features` of `meta.json` of the model
   `english-mfa-v3.1.0`, and with `isEndWithinTheAudio` as `endSeconds < audioSeconds + ALIGNER_STEP_SECONDS`, under a
   comment that says why an end may lie past the audio by less than one step: the aligner rounds an end to a whole
   step, and the audio is not a whole number of steps long. Run the test of task 1 and see it pass (R1, R2, R4)
3. [x] In `RealAlignerTest.assertPieceAligned`, replace the assertion that a start is at or after 0 and an end at or
   before `seconds` by two: `interval.startSeconds >= 0.0`, with a message that says the interval starts within the
   audio, and `isEndWithinTheAudio(interval.endSeconds, seconds)`, with a message that names the interval, the length
   of the audio, the step and `where`. Leave the other assertions of the method unchanged, and add to the comment at
   the head of the class that an end may lie past the audio by less than one step of the aligner (R1, R3)
4. [x] Check that `git diff master --stat` names no file under `src/main` or `core/src/main` (R5)
5. [x] With the fp32 Kokoro model and the aligner model in their folders, run
   `./gradlew :core:cleanTest :core:test --tests com.ritenoar.aloudrite.core.RealAlignerTest` and see that its tests
   ran and passed, and were not skipped, then run `./gradlew cleanTest test` and see it pass. Record both runs, with
   whether the models were in place, in `record.md` (R1, R3)
6. [x] Commit tasks 1 to 5 as one commit, `bugfix-0039 Allow an end of the aligner past the audio by less than one
   step in the aligner test`, then run `npm run check-changes` and see it pass

## Commit points

* After task 6: the unit test, the bound and the change to `RealAlignerTest`, with the record's log of the runs. The
  plan and the record's entry for it are committed before the work starts, on their own
