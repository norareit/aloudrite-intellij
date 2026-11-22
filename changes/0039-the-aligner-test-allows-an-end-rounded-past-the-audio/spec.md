# The aligner test allows an end rounded past the audio

## Context

`RealAlignerTest` runs the real speech engine and the real forced aligner of sherpa-onnx on the sentences of the trial
of unit 0028 and on a sentence with an emoji, at 1.0× and at 1.5×. Among its checks, every interval with text that the
aligner gives must end no later than the end of the audio of its piece. Unit 0028 kept that check on purpose: in its
second review the human declined to bound the ends in `wordStartsOf`, which uses only the starts and lets an end lie a
little past the audio, because the native code may round an end there, and accepted that `RealAlignerTest` still holds
the ends to the audio on the trial.

While unit 0036 was being closed, the test failed now and then on the Linux x64 host, on `master` before that unit as
well as on its branch, and so the check fails on audio that the aligner placed right.

### Observed behavior

`./gradlew cleanTest test` fails from time to time in `RealAlignerTest` on "Two candles and a box of matches." at 1.5×:

```
AlignedInterval(startSeconds=0.9599999785423279, endSeconds=1.3899997472763062, text=matches) is within the
1.3851666666666667 s of audio for "Two candles and a box of matches." at 1.5×
```

The last word ends about 5 ms after the last sample. On the host it failed in 2 of 8 full runs on the branch of unit
0036 at `15bcfaf` and in 1 of 13 full runs on `master` at `433363f`, and passed every time it was run alone, 3 of 3 on
each tree. The sandbox runs and the reviewer's runs of unit 0036 passed.

### Expected behavior

The test passes whenever the aligner places every word of a piece in order, with each start in the audio, and an end
that lies past the audio only by the rounding of the aligner's times.

### Steps to reproduce

1. Put the fp32 Kokoro model and the aligner model in their folders, as `README.md` says under "The speech model"
2. Run `./gradlew cleanTest test` repeatedly on the Linux x64 host
3. In some runs, `RealAlignerTest` fails with the message above. The length of the audio in the message differs by a
   few samples from one failing run to the next: 1.385292 s, 1.385208 s and 1.385167 s were seen

### Root cause

Reproduced: the length of the audio the engine makes for the sentence varies by a few samples from run to run, and in
some runs the aligner ends the last word after the last sample. Both times in the failure, 0.96 s and 1.39 s, are whole
hundredths, so the aligner appears to give its times in steps of 10 ms and to round the end of the last word up to a
step. With about 1.385 s of audio, a last end of 1.39 s lies past the audio while 1.38 s does not, and which of the two
the aligner gives depends on the few samples by which the audio varies. The step of 10 ms and the rounding up are
inferred from these values and not yet confirmed in sherpa-onnx or in the model of the aligner. Why the length of the
audio varies, and why the failures came more often in the full suite than alone, is not known; the engine runs on
several threads, which may make its output differ slightly between runs.

### Fix

The test allows an end past the audio by less than one step of the aligner's times, and still fails an end any
further past it. The bound is a function of the tests on plain values, which a unit test checks with the values of the
failure.

## Requirements

1. In `RealAlignerTest`, an interval with text passes the check on its end when the end lies at or before the end of
   the audio, or past it by less than one step of the aligner's times. An end one step or more past the audio still
   fails the test, as an end before the start does
2. The step is the one that sherpa-onnx or the model of the aligner gives its times in, as found in its source or its
   configuration, and is written in the test with where it was found. If it is not 10 ms, the step found is used
3. The checks on the starts, on the number and texts of the intervals and on `wordStartsOf` stay as they are
4. A unit test, which needs no model, checks the bound of requirement 1 on plain values: the end of 1.3899997472763062 s
   in 1.3851666666666667 s of audio passes, an end at the end of the audio passes, and an end one step past the end of
   the audio fails
5. What the plugin does does not change. No file under `src/main` or `core/src/main` changes

## Decisions

- The test is fixed, not the aligner's results. Bounding or clamping the ends in `ForcedWordAligner` would change the
  code of the plugin for no gain, as only the starts are used, and would hide from the test what the aligner gives
- The bound on the ends is loosened by one step, not dropped. Unit 0028 kept the check on the ends in this test as the
  one place that holds the aligner's ends to the audio, and an end far past the audio would still show a misplaced word.
  Dropping it, as `wordStartsOf` does, was rejected for that reason
- The tolerance is one step of the aligner's times rather than a margin picked by hand, because the step is what the
  rounding can add, and a wider margin would let through ends that rounding does not explain
- Making the engine give the same audio in every run, for example with one thread, was rejected: it would not remove
  the rounding, which can push an end past the audio for any length that is not a whole number of steps, and it would
  slow the test
- The bound is a function of the tests on plain values, so that it is unit tested without the models, as the standards
  ask of new logic

## Non-goals

- Finding why the length of the audio varies between runs, or why the failure came more often in the full suite
- Any change to `wordStartsOf`, to the highlights or to the other checks of `RealAlignerTest`
- Any other test with the real engine or aligner, and the measurements

## Beyond the request

1. Bound the ends in `wordStartsOf` by the same step, so that the plugin rejects an alignment whose end lies far past
   the audio. It costs a change to the code of the plugin and to `WordAlignmentTest`, and reverses part of a decision
   the human made in unit 0028. Without it, nothing changes for the user, as only the starts are used
2. Run the full suite many times on the host as part of the verification, as was done for unit 0036, to show the
   failure is gone. It costs about 25 minutes of the developer's machine for 15 runs. Without it, the unit test of
   requirement 4 shows that the values of the failure pass, and a failure that comes back shows in a later run
