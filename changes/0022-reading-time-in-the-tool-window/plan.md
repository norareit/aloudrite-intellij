# Reading time in the tool window

## Approach

The arithmetic of the times and of the bar lives in one pure class, `ReadingTime`, which knows nothing of threads,
players or Swing. It is built from the segments of a reading, the list `SpeechService.Reading.segments` already
numbers, and is told three kinds of event: audio of a segment made at a speed, a segment made in full, and the
position heard. From these it gives `ReadingTimes`: the seconds of the whole reading, the seconds left, and the share
of the text read. The service feeds it from the making work and the playing work, under the monitor of the `Reading`,
and the tool window asks for the times every half second while a reading is shown. The measurement of requirement 8
drives the same class with the real engine, so it measures the arithmetic the tool window shows and not a copy of it.

How `ReadingTime` counts, decided here so that the implementer does not re-derive it:

* **The length of the text** of a segment of words is the number of its code points that are not white space. A
  silence has none. The share of the text read is counted in these
* **The rate** is seconds of audio at 1.0× per code point. Before any segment of words is made in full it is
  `DEFAULT_SECONDS_PER_CHARACTER = 0.075`, about 13 characters a second, which is near the speech of `af_heart` at
  1.0×. Afterwards it is the sum, over every segment of words made in full during the reading, of its seconds times the
  speed it was made at, divided by the sum of their lengths of text. A segment made again after a change of the speed
  replaces its own entry, so that it counts once. Kept across changes of the speed, since it is stored at 1.0×
* **The seconds of one segment at the speed in use** are, in this order of preference:
    * for a silence, `speed.silenceMilliseconds(milliseconds) / 1000.0`, which is its exact length
    * for words whose audio is made in full at the speed in use, the length of that audio (requirement 4)
    * for words of which part of the audio is made at the speed in use, the larger of that part and the estimate
    * for words heard before at another speed, their entry in the rate divided by the speed in use
    * otherwise the estimate: length of text times the rate, divided by the speed in use
* **A change of the speed** drops the made audio of every segment, since `SpeechService.Reading.changeSpeed` cancels
  the audio ahead, and takes the new speed at once, so that within the next refresh of the panel both times are for
  the new speed (requirement 3). A making that starts afterwards records its audio anew
* **The position heard** is a segment and the seconds heard into it. Before anything is heard it is segment 0 at 0
* **The whole** is the sum of the seconds of every segment. **The time left** is the seconds of the segment heard
  minus the seconds heard into it, never below 0, plus the seconds of every later segment
* **The share read** is the length of text of every segment before the one heard, plus that of the segment heard
  times the share of it heard, which is the seconds heard into it divided by its seconds, at most 1, all divided by
  the length of text of the whole reading. It is kept as a high-water mark: a share lower than the one shown before,
  as after a resume or a change of the speed that plays the piece again, leaves the one shown before (requirement 5)
* **A pause** freezes the times: while paused, `times()` returns what it returned at the pause, so that audio the
  engine goes on making meanwhile moves neither the times nor the bar (requirement 2). A change of the speed while
  paused takes the frozen times again at the new speed, as requirement 3 asks also then. A resume unfreezes them
* **The first estimate** of the whole is the whole as it stands when the first segment of words is made in full. It is
  kept for the log line of requirement 10 and is what the measurement reports "after the first piece"

A resume and a change of the speed start the piece that was playing again from its beginning, as unit 0012 and
unit 0016 made them. The time left then rises by what of that piece is heard again, which is the truth about what is
still to be heard, while the bar stays where it was.

In the service, the making work records each run it puts, and the end of each segment of words it made, through
methods of `Reading` that ignore a making that is no longer current, as `isCurrent` already tells. The playing work
records the position after every slice and every run it takes, from `PlayedAudio`, which gains a function that turns
the samples heard into a segment and the samples heard into it. The samples heard come from `player.samplesHeard()`,
as the resumption is made today. The last slice of a reading is followed by the play-out of what the line still holds,
during which no slice is played and the position is not updated; the reading ends within that time, at which point
the times go, as requirement 6 asks.

The tool window shows a row under the name of the file, the part and the project, and above the buttons: a label of
the times, as `9:28 left of 12:40`, and a `JProgressBar` of the IDE beside it, from 0 to 1000. Both times are
rounded up to the whole second and written as minutes and seconds, with the minutes not capped at 59, as `64:05`.
A `javax.swing.Timer` of 500 milliseconds, started in `followTheReading` and stopped when its disposable is disposed,
refreshes the row while a reading is shown, and `showTheCurrentReading` refreshes it too. While nothing is read the
row is hidden.

For requirement 10, the service logs one line when a reading reaches its end by itself, with the file, the first
estimate of the whole and the time from the first sound to the end, so that the developer reads both from the IDE's
log after listening. The line goes through a collaborator handed in, as `reportFirstSound` does, so the stand-ins
record it.

## Files

- `src/main/kotlin/com/ritenoar/aloudrite/ReadingSegments.kt`: new, holds `segmentsOf`, moved out of the companion of
  `SpeechService`, and `SECONDS_AHEAD`, moved likewise, both internal, so that the measurement uses them
- `src/main/kotlin/com/ritenoar/aloudrite/ReadingTime.kt`: new, `ReadingTime`, `ReadingTimes` and
  `minutesAndSeconds(seconds: Double): String`
- `src/main/kotlin/com/ritenoar/aloudrite/PlayedAudio.kt`: `positionAt(samplesHeard: Long)`, giving the segment heard
  and the samples heard into it
- `src/main/kotlin/com/ritenoar/aloudrite/SpeechService.kt`: `CurrentReading.timesOf(target): ReadingTimes?`, a
  `ReadingTime` in each `Reading`, the records from the making and the playing work, and the collaborator
  `reportReadingTime` with its default that logs
- `src/main/kotlin/com/ritenoar/aloudrite/ReadingPanel.kt`: the row of the times and the bar, and its timer
- `src/test/kotlin/com/ritenoar/aloudrite/ReadingTimeTest.kt`: new, unit tests of `ReadingTime` and
  `minutesAndSeconds`
- `src/test/kotlin/com/ritenoar/aloudrite/PlayedAudioTest.kt`: tests of `positionAt`
- `src/test/kotlin/com/ritenoar/aloudrite/SpeechStandIns.kt`: records what `reportReadingTime` is given
- `src/test/kotlin/com/ritenoar/aloudrite/ReadingTimeServiceTest.kt`: new, the times of a reading through
  `SpeechService` with the stand-ins of unit 0006
- `src/test/kotlin/com/ritenoar/aloudrite/ReadingPanelTest.kt`: the row in the tool window
- `src/test/kotlin/com/ritenoar/aloudrite/ReadingTimeMeasurement.kt`: new, the measurement of requirement 8
- `README.md`: the times, the bar, the log line and the measurement
- `changes/0022-reading-time-in-the-tool-window/record.md`: the table of requirement 9

## Tasks

- [x] Move `segmentsOf` and `SECONDS_AHEAD` from the companion of `SpeechService` into the new
  `ReadingSegments.kt` as internal top-level declarations, with no change of behavior, and run `./gradlew test`. This
  is a refactoring on its own, so that the measurement can cut a file into segments as a reading does (R8)
- [x] Commit: `feature-0022 Move the segments of a reading out of the service`
- [x] Write `ReadingTimeTest` first, with segments of words and silences given by hand and a sample rate of 100, so
  that it fails to compile until the class exists. It covers: the whole and the time left before anything is made,
  from the default rate; a silence counted at its exact length at 1.0× and at 2.0×; a segment made in full counted at
  the length of its audio and no longer at the estimate; a segment made in part counted at the larger of the two; the
  rate learnt from segments made in full, stored at 1.0× and used for the rest; the time left falling as the position
  moves through a segment and into the next; a change of the speed giving both times at the new speed at once, with the
  made audio dropped and the rate kept; a segment remade after a change replacing its entry in the rate; the share
  read at 0 at the start, growing within a segment with the seconds heard, and never falling when the position goes
  back to the start of the segment; the times and the share unchanged while paused although more audio is made,
  taken again at the new speed on a change while paused, and following again after the resume; the first estimate
  kept from the moment the first segment of words is made in full; a reading of silences only giving a share of 0.
  `minutesAndSeconds` gives `0:00`, `0:01` for 0.2 seconds, `1:00`, `12:40` and `64:05` (R1, R2, R3, R4, R5, R7)
- [x] Write `ReadingTime.kt` as the approach says, until `ReadingTimeTest` passes (R1, R2, R3, R4, R5)
- [x] Add tests of `positionAt` to `PlayedAudioTest`: the segment and the samples into it for a position inside a run,
  inside the second run of a segment of two runs, after a segment that was forgotten as heard, and at or past the
  end of what was taken, which gives the last segment at its end. Then add `positionAt` to `PlayedAudio` (R2, R5)
- [x] Commit: `feature-0022 Count the time of a reading and the share of its text read`
- [x] Add `reportReadingTime: (target: ReadingTarget, firstEstimateSeconds: Double?, tookMillis: Long) -> Unit` to
  the constructor of `SpeechService`, with a default that logs `Read <file> to the end in <m:ss>, first estimated at
  <m:ss>` at info level, and record its calls in `SpeechStandIns` (R10)
- [x] Write `ReadingTimeServiceTest` with `SpeechStandIns`, where a text makes 0.5 seconds of audio, and use
  `whilePlaying` to look at `timesOf` during the playing. It covers: `timesOf` null before a reading, and for another
  target; at the start of a reading, before the making work runs, a whole from the estimate and a share of 0; once the
  making work ran, a whole equal to the length of the audio made, silences included, and a time left equal to it; the
  time left falling and the share growing slice by slice while the audio plays; both standing still during a pause in
  the middle of a reading, also when the making work runs on during the pause; after `changeSpeed` from 1.0 to 2.0
  during the playing, times at 2.0 before the new making starts, and the share not lower than before the change;
  `timesOf` null once the reading ended, was stopped, or failed; `reportReadingTime` called once at the end of a
  reading with its first estimate and the time from the first sound on the clock of the stand-ins, and not called for
  a reading that was stopped (R1, R2, R3, R4, R5, R6, R7, R10)
- [x] Give each `Reading` a `ReadingTime` of its segments and add `timesOf` to `CurrentReading` and `SpeechService`.
  In `makeTheAudioOf`, after each `put` that returns true, record the run, and after `speak` of words returns and the
  making is still current, record the segment as made in full. In `playUntilTheEndOrAnInterruption`, after each run
  taken and each slice played, record `playedAudio.positionAt(player.samplesHeard())`. Tell the `ReadingTime` of a
  pause, a resume and a change of the speed in the methods of `Reading` that make them. Record the clock of the
  service at the first sound of the reading, and call `reportReadingTime` where `playOut` ends the reading by
  itself. All of it under the monitor of `Reading`, and only for a making that is current. Run until
  `ReadingTimeServiceTest` and `SpeechServiceTest` pass (R1, R2, R3, R4, R5, R6, R10)
- [x] Commit: `feature-0022 Keep the time of a reading in the service`
- [x] Add tests to `ReadingPanelTest`: while a file is read, the row under the name of the file shows
  `<left> left of <whole>` and a bar, between the part and project lines and the buttons; the bar is at 0 at the start
  and moves forward after a tick of the timer once audio was heard; while paused, two ticks show the same text and
  value; after a change of the speed, the next tick shows the times at the new speed; while nothing is read, and after
  the reading ended, the row is hidden and nothing in the panel shows a time; the delay of the timer is at most 1000
  milliseconds, and the timer stops when the disposable of `followTheReading` is disposed. Update `shownBy` and the
  existing expectations for the new row (R1, R2, R3, R5, R6, R7)
- [x] Add the row to `ReadingPanel`: a `JBLabel` of the times and a `JProgressBar` from 0 to 1000 in a
  `HorizontalLayout` row, added after `projectLabel` and before `controlRow`, with a `javax.swing.Timer` of 500
  milliseconds that calls the refresh while a target is shown, and the refresh in `showTheCurrentReading`. Run until
  `ReadingPanelTest` passes (R1, R2, R3, R5, R6)
- [x] Start the sandbox IDE with `./gradlew runIde`, read `samples/markdown-features.md`, and capture only the window
  whose title ends in `[aloudrite-intellij]` with `xwd -id <window id>`, as `AGENTS.md` says, while the reading plays,
  while it is paused, and once it ended. Look at the row and the bar in each, fix what looks wrong, and note in
  `record.md` what the captures showed. Where the host of the run has no display, log that the capture could not be
  made, so that the developer's check of requirement 10 is also the first look at the row (R1, R5, R6)
- [x] Commit: `feature-0022 Show the time and the progress of a reading in the tool window`
- [x] Write `ReadingTimeMeasurement`, run only with `ALOUDRITE_MEASURE_READING_TIME` set, on a platform with speech,
  and with both models of `SPEECH_MODELS` in place in `userModelsFolder()`, and reported as skipped otherwise, as
  `EngineSilenceMeasurement` is. For each of `samples/markdown-features.md` and `samples/ten-thousand-words.md`, each
  model and each of 1.0, 1.5 and 2.0, with a new `KokoroSpeechEngine` of the plugin's thread count, it cuts the file
  with `spokenPartsOf` and `segmentsOf` and makes every segment in order, words with the engine and silences with
  `silenceMilliseconds`, keeping the length of each. The length of the whole audio is their sum. It then drives a
  `ReadingTime` of the segments as a reading would: after the first segment of words, with that segment made in full
  and nothing heard; and at a quarter, a half and three quarters of the length of the whole audio, with the position
  at that point of the audio and every segment made in full that begins before that point plus `SECONDS_AHEAD`, which
  is how far `AudioAhead` lets the engine run ahead. At each of the four points it writes the whole the times give,
  its error in seconds, and its error in percent of the length of the whole audio, and it writes the table, with the
  processors, the operating system and the architecture, to `build/reading-time.txt` (R8)
- [x] Put the fp32 model in place by hand where it is not, as `README.md` says under "The speech model", run
  `ALOUDRITE_MEASURE_READING_TIME=1 ./gradlew cleanTest test --tests com.ritenoar.aloudrite.ReadingTimeMeasurement`
  on Linux x64, and confirm it ran and was not skipped (R8)
- [x] Commit: `feature-0022 Measure the accuracy of the estimate of a reading`
- [x] Copy the table of `build/reading-time.txt` into `record.md` as a log entry, with the machine it was measured on
  (R9)
- [x] Describe in `README.md`, in the paragraph on the tool window, the row of the times and the bar: what the two
  times are, that the whole is an estimate from the text that gets more exact as the audio is made, that the time left
  stands still while paused and follows a change of the speed, that a resume or a change of the speed adds back what of
  the piece is heard again, and that the bar shows the share of the text read and never moves back. Add to "Testing"
  how `ReadingTimeMeasurement` is run, what it writes, that it needs both models, and that it takes a long time, with
  the time the run of the previous task took. Mention the log line at the end of a reading (R11)
- [x] Commit: `feature-0022 Describe the reading time in the README and record the measurement`
- [ ] Added after the developer's reading of requirement 10, at their request: change the expectations of the times in
  `ReadingPanelTest` to `~0:01 left of ~0:01` and the like, and add a test that the label of the times has the tooltip
  `Estimated from the text. It gets more exact as the audio is made.`, and see them fail (R1)
- [ ] Mark both times in `ReadingPanel` as estimates, as `~9:28 left of ~12:40`, and give the label of the times that
  tooltip, until `ReadingPanelTest` passes (R1)
- [ ] Look at the row in the sandbox IDE while a reading plays, captured as `AGENTS.md` says, and note in `record.md`
  what the capture showed (R1)
- [ ] Change the example of the row in `README.md` to `~9:28 left of ~12:40`, mention the tooltip, and say that the
  IDE's log of the sandbox IDE is `.intellijPlatform/sandbox/<project>/<IDE>/log/idea.log` (R11)
- [ ] Commit: `feature-0022 Mark the times of a reading as estimates`

## Verification

- [x] `./gradlew test` passes, with the tests with the real engine run, not skipped, since the unit changes what the
  service does with the engine's audio
- [x] `./gradlew buildPlugin` builds the archive
- [x] `npm run check-changes` passes on the committed branch
- [x] Every requirement has a task: R1 to R6 the tests and code of `ReadingTime`, the service and the panel, R7 the
  three test classes with the stand-ins, R8 the measurement and its run, R9 the table in the record, R10 the log line
  and the developer's reading, R11 the README
- [x] Requirement 10 is left to the developer: reading `samples/markdown-features.md` to the end on Linux x64 without a
  pause or a change of the speed, and taking the line `Read markdown-features.md to the end` from the IDE's log into
  `record.md`

## Commit Points

- [x] `feature-0022 Move the segments of a reading out of the service`
- [x] `feature-0022 Count the time of a reading and the share of its text read`
- [x] `feature-0022 Keep the time of a reading in the service`
- [x] `feature-0022 Show the time and the progress of a reading in the tool window`
- [x] `feature-0022 Measure the accuracy of the estimate of a reading`
- [x] `feature-0022 Describe the reading time in the README and record the measurement`
- [ ] `feature-0022 Mark the times of a reading as estimates`
