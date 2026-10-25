---
kind: feature
status: in-progress
timestamp: 2026-10-25T11:52:42Z
---

# Reading time in the tool window

## Log

- 2026-10-20T13:39:39Z [human] in a session of Claude Code, suggested showing an estimated and a remaining reading time
  in the tool window of unit 0015, as an idea for later. It was kept in the agent's memory on the Linux host only
- 2026-10-22T12:10:47Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `e5f7a03`, at the human's request, wrote `spec.md` of a new unit from that idea, so that it is
  kept in the project rather than in the memory of one machine, and deleted the memory. Read `process.md`, the
  standards and `roles/specifier.md` of the installed kit, 8.0.0. Made it a feature and numbered it 0022, after 0021.
  Decided by me and open to the human: an estimate from the text made better by the audio, the times in the tool
  window only, and a check on Linux only. Left beyond the request: a bar of progress. The status is `draft`
- 2026-10-25T11:52:42Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `11d9e91`, at the human's request, took in item 1 of "Beyond the request", the bar of progress,
  as requirement 5, and added the measurement of the accuracy of the estimate against the length of the audio, with
  both models, three speeds and two files, as requirements 8 and 9, since the human wants to know the accuracy before
  the merge without having to measure it. Read `process.md`, the standards and `roles/specifier.md` of the installed
  kit, 8.2.0. Decided by me and open to the human: the bar shows the share of the text read rather than of the time,
  and no bound is set on the error. The status is `draft`
- 2026-10-25T11:57:29Z [human] approved the spec. The status is `approved`
- 2026-10-25T12:02:05Z [Claude Code, anthropic/claude-opus-5-5] as the planner, in a unit run, on
  `feature-0022-reading-time-in-the-tool-window` at `a8bd35f`, wrote `plan.md`. Read `process.md`, the standards and
  `roles/planner.md` of the installed kit, 8.2.0. The plan puts the arithmetic of the times and the bar in one pure
  class that the service feeds and the measurement drives, decides how it counts, including a default rate of
  0.075 seconds per character before the first piece is made, and adds a line in the IDE's log at the end of a
  reading so that the developer can take the first estimate and the time the reading took for requirement 10. Only
  the int8 model is in place on the machine of the run, so the implementer puts the fp32 model in place by hand
  before the measurement. The status is `approved`
- 2026-10-25T13:10:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, began the work on
  `feature-0022-reading-time-in-the-tool-window`. Moved `segmentsOf` and `SECONDS_AHEAD` out of the service, wrote
  `ReadingTime` and `PlayedAudio.positionAt`, fed them from the service with `reportReadingTime` for the log line,
  added the row and the bar to `ReadingPanel`, and wrote `ReadingTimeMeasurement`, each with its tests first. The
  existing expectations of `ReadingPanelTest` gained the line of the times. The status is `in-progress`
- 2026-10-25T13:11:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, could not look at the tool window
  in the sandbox IDE: the sandbox has no display and no `xwd`, `xwininfo` or Xvfb, so no capture of the row and the bar
  was made while playing, paused or ended. The look is left to the developer, whose reading for requirement 10 is also
  the first look at the row
- 2026-10-25T13:12:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, ran `ReadingTimeMeasurement` with
  `ALOUDRITE_MEASURE_READING_TIME=1` after putting the fp32 model in place by hand from the release of sherpa-onnx. It
  ran and was not skipped (1 test, 0 skipped, 35 minutes) on Linux amd64 with 6 processors, and wrote this table
  (requirement 9). The estimate after the first piece is 5 to 15 percent off in both samples: too long for the short
  file, whose first sentences are slow, and too short for the long file. From a quarter on it is within 10 percent for
  the short file and within 0.4 percent for the long one. The whole audio is the time of the reading only while the
  engine stays ahead of the playing, which was not measured here:
  ```text
  processors 6, os Linux, arch amd64
  estimate of the whole reading after the first piece and when 1/4, 2/4 and 3/4 of the audio have played
  error is the estimate minus the length of the audio, in seconds and in percent of that length
  file                     model        speed     audio s  at           estimate s   error s  error %
  markdown-features.md     int8         1.0×        328.8  first piece       378.5      49.7     15.1
  markdown-features.md     int8         1.0×        328.8  1/4               346.0      17.2      5.2
  markdown-features.md     int8         1.0×        328.8  2/4               355.6      26.8      8.2
  markdown-features.md     int8         1.0×        328.8  3/4               335.3       6.5      2.0
  markdown-features.md     int8         1.5×        214.7  first piece       247.7      33.0     15.4
  markdown-features.md     int8         1.5×        214.7  1/4               228.8      14.1      6.6
  markdown-features.md     int8         1.5×        214.7  2/4               225.5      10.8      5.0
  markdown-features.md     int8         1.5×        214.7  3/4               217.2       2.5      1.1
  markdown-features.md     int8         2.0×        187.3  first piece       202.9      15.7      8.4
  markdown-features.md     int8         2.0×        187.3  1/4               204.7      17.4      9.3
  markdown-features.md     int8         2.0×        187.3  2/4               197.3      10.0      5.3
  markdown-features.md     int8         2.0×        187.3  3/4               189.0       1.7      0.9
  markdown-features.md     fp32         1.0×        329.7  first piece       379.0      49.3     14.9
  markdown-features.md     fp32         1.0×        329.7  1/4               347.4      17.7      5.4
  markdown-features.md     fp32         1.0×        329.7  2/4               356.8      27.0      8.2
  markdown-features.md     fp32         1.0×        329.7  3/4               336.3       6.5      2.0
  markdown-features.md     fp32         1.5×        214.2  first piece       246.8      32.7     15.3
  markdown-features.md     fp32         1.5×        214.2  1/4               228.3      14.1      6.6
  markdown-features.md     fp32         1.5×        214.2  2/4               225.3      11.1      5.2
  markdown-features.md     fp32         1.5×        214.2  3/4               216.7       2.5      1.2
  markdown-features.md     fp32         2.0×        186.2  first piece       202.7      16.5      8.9
  markdown-features.md     fp32         2.0×        186.2  1/4               204.4      18.2      9.8
  markdown-features.md     fp32         2.0×        186.2  2/4               196.7      10.5      5.7
  markdown-features.md     fp32         2.0×        186.2  3/4               187.8       1.6      0.9
  ten-thousand-words.md    int8         1.0×       2693.2  first piece      2473.2    -220.0     -8.2
  ten-thousand-words.md    int8         1.0×       2693.2  1/4              2691.6      -1.6     -0.1
  ten-thousand-words.md    int8         1.0×       2693.2  2/4              2691.4      -1.8     -0.1
  ten-thousand-words.md    int8         1.0×       2693.2  3/4              2690.8      -2.4     -0.1
  ten-thousand-words.md    int8         1.5×       1775.0  first piece      1634.8    -140.3     -7.9
  ten-thousand-words.md    int8         1.5×       1775.0  1/4              1768.7      -6.3     -0.4
  ten-thousand-words.md    int8         1.5×       1775.0  2/4              1773.9      -1.2     -0.1
  ten-thousand-words.md    int8         1.5×       1775.0  3/4              1774.9      -0.1     -0.0
  ten-thousand-words.md    int8         2.0×       1565.9  first piece      1437.5    -128.4     -8.2
  ten-thousand-words.md    int8         2.0×       1565.9  1/4              1566.4       0.5      0.0
  ten-thousand-words.md    int8         2.0×       1565.9  2/4              1566.5       0.6      0.0
  ten-thousand-words.md    int8         2.0×       1565.9  3/4              1567.1       1.2      0.1
  ten-thousand-words.md    fp32         1.0×       2696.5  first piece      2552.0    -144.4     -5.4
  ten-thousand-words.md    fp32         1.0×       2696.5  1/4              2694.1      -2.3     -0.1
  ten-thousand-words.md    fp32         1.0×       2696.5  2/4              2694.4      -2.1     -0.1
  ten-thousand-words.md    fp32         1.0×       2696.5  3/4              2693.9      -2.5     -0.1
  ten-thousand-words.md    fp32         1.5×       1767.4  first piece      1635.4    -131.9     -7.5
  ten-thousand-words.md    fp32         1.5×       1767.4  1/4              1764.0      -3.3     -0.2
  ten-thousand-words.md    fp32         1.5×       1767.4  2/4              1766.5      -0.8     -0.0
  ten-thousand-words.md    fp32         1.5×       1767.4  3/4              1766.8      -0.6     -0.0
  ten-thousand-words.md    fp32         2.0×       1539.7  first piece      1437.4    -102.3     -6.6
  ten-thousand-words.md    fp32         2.0×       1539.7  1/4              1543.2       3.5      0.2
  ten-thousand-words.md    fp32         2.0×       1539.7  2/4              1540.8       1.1      0.1
  ten-thousand-words.md    fp32         2.0×       1539.7  3/4              1540.4       0.7      0.0
  ```
- 2026-10-25T13:30:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, described the times, the bar, the
  log line and the measurement in `README.md` (requirement 11), and ran `./gradlew cleanTest test buildPlugin` on the
  tree before this commit: it built, and 628 tests ran with 0 failures and 4 skipped, which are the three measurements
  that need their environment variable and `RealModelPackageTest`. The tests with the real engine in
  `KokoroSpeechEngineTest` were not among the skipped. Requirement 10 is open: it needs the developer to read
  `samples/markdown-features.md` to the end on Linux x64 and take the line `Read markdown-features.md to the end` from
  the IDE's log. The status stays `in-progress`
- 2026-10-25T14:23:26Z [OpenCode, openai/gpt-6.1-sol] as the reviewer, made a full review of
  `changes/0022-reading-time-in-the-tool-window` at `79caf960abf4c998956d447034d5544937462971`, against merge base
  `a8bd35f749073b7ee3839d098a12abbf5eb8b3df` with `origin/master`. Returned S1; the complete findings are in
  `.reviews/changes/0022-reading-time-in-the-tool-window/review-20261025T142251.md`. Ran
  `./gradlew cleanTest test buildPlugin`: 628 tests, zero failures, four opt-in skips, with all five real-engine tests
  run. Ran `npm run check-changes`, which passed. Could not verify the IDE appearance or the developer's playback
  result for R10, and did not independently reproduce the opt-in reading-time measurement's table or runtime
- 2026-10-25T14:40:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, took up the review of
  `79caf96`, whose one finding is S1, the missing playback check of requirement 10. S1 is not mine to apply: it needs
  the developer to read `samples/markdown-features.md` to the end on Linux x64 and give the line `Read
  markdown-features.md to the end` from the IDE's log. Changed no code. When the line arrives, I record the first
  estimate, the time the reading took and the difference, tick the open task of `plan.md`, run the suites again and
  set `implemented`. The status stays `in-progress`
- 2026-10-25T19:11:49Z [human] read `samples/markdown-features.md` to the end in the sandbox IDE on Linux x64, at 1.0×,
  without a pause or a change of the speed, and found the row and the bar right while playing and after the end
- 2026-10-25T19:16:00Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, took the line of that reading from
  `.intellijPlatform/sandbox/aloudrite-intellij/IU-2026.2.3/log/idea.log`, where the developer had not found it:
  `Read markdown-features.md to the end in 5:31, first estimated at 6:20`. The first estimate was 49 seconds above
  the time the reading took, 14.8 percent of it, close to the 15.1 percent of the first piece that the measurement
  gives for this file at 1.0× with int8. This makes requirement 10 true. The developer saw the whole rise and fall
  during the reading, which the measurement shows too for this file, whose first half is short pieces of features and
  whose second half is prose. Recorded S1 as accepted and resolved by this reading, which the human approved, and
  ticked the task of requirement 10 in `plan.md`
- 2026-10-25T19:16:30Z [human] decided that both times are marked as estimates, as `~5:48 left of ~6:10`, with a
  tooltip that explains the estimate, and that no unit for a steadier estimate is opened for now
- 2026-10-25T19:17:00Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, added to `plan.md` the tasks that
  mark the times as estimates, which requirement 1 allows as it calls the whole an estimate, and that name the folder
  of the sandbox IDE's log in `README.md`, since the developer could not find the line there. The change comes after
  the review of `79caf96` and goes to the human for review. The status stays `in-progress`
- 2026-10-25T19:24:00Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, corrected my entry of 19:16:00: the
  line of the developer's reading is in `.intellijPlatform/sandbox/aloudrite-intellij/IU-2026.2.3/log_runIde/idea.log`,
  not in `log/idea.log`, as `grep -l` on the three folders of logs of the sandbox shows
- 2026-10-25T19:40:00Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, marked both times as estimates in
  `ReadingPanel`, as `~9:28 left of ~12:40`, with the tooltip `Estimated from the text. It gets more exact as the audio
  is made.` on the label of the times. The changed expectations of `ReadingPanelTest` failed first, 17 of its 46
  tests, and pass after the change. Changed the example in `README.md`, mentioned the tooltip there, and named the
  folder of the sandbox IDE's log. Captured only the window of the sandbox IDE with `xwd -id` while the developer read
  `samples/markdown-features.md` from the caret at 1.3× with fp32: the row showed `~4:04 left of ~4:11` with the bar
  beside it, on one line and not cut off. The tooltip was not looked at in the IDE, which would have moved the
  developer's pointer; `ReadingPanelTest` checks its text. Ran `./gradlew cleanTest test buildPlugin` on the tree of
  this commit: it built, and 628 tests ran with 0 failures and 4 skipped, `EngineSilenceMeasurement`,
  `EngineThreadsMeasurement`, `ReadingTimeMeasurement` and `RealModelPackageTest`, which need their opt-in input. The 5
  tests with the real engine in `KokoroSpeechEngineTest` ran. Every task of `plan.md` is ticked. The change after the
  review of `79caf96` is ready for the human's review. The status stays `in-progress`

## Amendments
