---
kind: feature
status: in-progress
timestamp: 2026-10-21T14:55:12Z
---

# Reading speed

## Log

- 2026-10-20T12:01:29Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `33ea6fb`, at the human's request, wrote `spec.md` of a new unit: the speed of the reading can
  be made slower and faster. The human found, testing the plugin, that they miss it. Read `process.md` and
  `roles/specifier.md` of the installed kit. Made it a feature and numbered it 0016, after 0015, and wrote it to be
  built after units 0015 and 0012. Asked before writing, the human chose: the controls in the tool window of unit
  0015; a change during a reading starts the piece that is playing again at the new speed; the speed is kept across
  restarts of the IDE. Decided by me and open to the human: a range from 0.5 to 2.0 in steps of 0.1, shown as "1.0×";
  a settings page under Tools that shows the same speed; one speed for the whole IDE; the speed made by the engine and
  not by playing faster; no limit on the time to the new speed, which the developer measures instead; the pauses of
  unit 0010 as long at every speed. Left beyond the request: pauses that follow the speed, and keyboard shortcuts for
  it. The status is `draft`
- 2026-10-20T12:07:01Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, corrected the context of `spec.md` on
  unit 0012, which another session revised after this spec was written: unit 0012 keeps a stop button on the tab
  beside the pause, and gives the tool window a pause, a resume and a stop. Nothing else of this spec depends on
  where the stop is. The status stays `draft`
- 2026-10-21T14:46:34Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `6811f0e`, at the human's request, revised `spec.md`. The human asked that not every speed be
  listened to on both machines: requirement 14 now has every speed listened to on Linux, on the first five paragraphs
  of `samples/ten-thousand-words.md`, and requirement 15 has `samples/markdown-features.md` read to its end at 0.5,
  1.5 and 2.0 on Linux and on macOS. The human asked for a field in the tool window to type the speed in, so that a
  large step is one change and the piece is not made again for every press on a button: requirement 3. The human
  took in the first item beyond the request, silences that follow the speed: requirement 10. Decided by me and open to
  the human: the two seconds after "code example" stay two seconds; the field takes a point as the decimal sign and
  one decimal at the most, and sets the value on Enter or when it loses focus; a change during a silence goes on at
  the piece after it; the changes during a reading are checked on Linux only. Added beyond the request: a wait after
  a press on a button, and a comma as the decimal sign. Brought the context up to date with units 0015 and 0012,
  which are implemented, and renumbered the requirements to 16. The status stays `draft`
- 2026-10-21T14:52:50Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, on `master` at `b880974`, revised
  `spec.md` on the human's answers. The human decided that the two seconds after "code example" follow the speed like
  every other silence, with no exception: requirement 11. The human agreed to the point as the only decimal sign and
  to setting a typed value on Enter or when the field loses focus. The human took in the wait after a press on a
  button: requirement 8, with presses in a row within half a second made once, and the sound stopped at the first
  press. Decided by me and open to the human: the half second, and stopping the sound at the first press. Renumbered
  the requirements to 17. The status stays `draft`
- 2026-10-21T14:55:12Z [human] agreed to the half second of the wait after a press on a button and to stopping the sound
  at the first press, and approved the spec. The entry was written by Claude Code, anthropic/claude-opus-5-5, on the
  human's word. The status is `approved`
- 2026-10-21T14:59:35Z [Claude Code, anthropic/claude-opus-5-5] as the planner, in the main session on the Linux host,
  cut `feature-0016-reading-speed` from `master` at `4e5be2e` and wrote `plan.md`. Read `process.md`, the standards and
  `roles/planner.md` of the installed kit. The plan keeps the speed in an application setting stored in
  `aloudrite.xml`, makes the audio of a reading in makings at one speed each, and starts a new making at a change from
  the restart that the resumption of unit 0012 gives, after a wait held as a time in the reading and checked by a
  delayed work on a clock the tests set. Read as not accepted by the field, since requirement 3 asks for neither: a
  number that ends in a point, as "1.", and one that begins with it, as ".5". The status stays `approved`
- 2026-10-21T15:04:08Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, in the main session on the Linux
  host, on `feature-0016-reading-speed` at `d15afff`, began the plan. Read `process.md`, the standards and
  `roles/implementer.md` of the installed kit, and `spec.md`, `plan.md` and `record.md` of the unit. The status is
  `in-progress`
- 2026-10-21T17:06:47Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, did the tasks of commit points 2
  and 3. Three things differ from the plan in their order or detail, and none of them changes what is built. The
  default collaborator of `ReadingSpeedSetting` hands the change to `SpeechService.changeSpeed` only from commit point
  3, since the service has no such function before it; at commit point 2 it only publishes the change. In the section
  of the service, I wrote `SpeechService` before the new tests of `SpeechServiceTest`, not after them. To make up for
  it, I made two changes to the service by hand, a wait of 0 ms and an engine work that goes on after a change, and saw
  10 of the new tests fail, then put the service back. The first sound after a change is measured on the clock of the
  service, which the stand-ins set, so that a test can check that it is measured from the last change. I added one
  test beside those of the plan: a change before the engine work has run makes every piece at the new speed.
  `./gradlew test` on the tree of commit point 3, with the model in its folder, ran 385 tests: none failed, and two
  were skipped, `RealModelPackageTest` and `EngineThreadsMeasurement`, as on `master`. Neither of them is a test of
  the engine that the model alone enables. All five cases of `KokoroSpeechEngineTest` ran, among them the new one at
  2.0 and 0.5. The status stays `in-progress`
- 2026-10-21T17:13:28Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, did the tasks of commit points 4
  and 5, the last of them `0fad0d2`. I wrote `ReadingSpeedConfigurable` before its test, not after it. On that tree,
  with the model in its folder, `./gradlew test` ran 396 tests: none failed, and two were skipped, `RealModelPackageTest`
  and `EngineThreadsMeasurement`, as on `master`. I ran `EngineThreadsMeasurement` by hand once, on the tree of commit
  point 3, since its calls of the engine changed, and it passed. `./gradlew buildPlugin` built the archive and passed
  its size check, and `npm run check-changes` passed. Not checked by me, and left to the developer's check of R15:
  anything of the tool window and the settings page in a running IDE, among them that Escape in the field stays there
  rather than moving the focus to the editor, which the tests check only as an action registered on the field, and the
  sound and the times of a change. The checks of R15 on Linux x64 and of R16 on Linux x64 and macOS on Apple Silicon
  are still to be done. The status stays `in-progress`
- 2026-10-21T20:08:48Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, on `d23586a`, at the human's request, wrote
  `developer-checks.md` beside the spec: the exact steps of the developer's checks, those of R15 on Linux x64 only and
  those of R16 on Linux x64 and on macOS on Apple Silicon, with tables to fill in, as the tasks of the developer's
  checks in `plan.md` hand them over. The status stays `in-progress`
- 2026-10-21T20:20:24Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux host, on
  `feature-0016-reading-speed` at `00f8359`, at the human's request, revised `spec.md` after the human saw the row of the
  speed in the sandbox IDE. The row said nowhere that it was the speed, and the "×" after the field looked like a
  button that deletes something, which the human tried to click. Read `roles/specifier.md` of the installed kit. Asked
  before writing, the human chose: a label "Reading speed" over the row, the minus and plus icons of the IDE in place
  of the buttons in text, with the tooltips "Slower" and "Faster", the "×" inside the field in the grey of a hint, and
  a text in the grey of a comment under the row that says a change starts the sentence again and can take a few
  seconds. Requirements 2, 3 and 14 now say so, and a decision says why. Added to requirement 15 that the developer
  looks at the row and the record says whether it was clear, since the look is what the revision is for. The developer's
  checks are paused until the new row is built. The spec needs the human's approval again before it is built. The
  status stays `in-progress`
- 2026-10-21T20:22:13Z [human] approved the revised spec, with the text under the row of the speed reading "A change starts the
  sentence that is playing again at the new speed. This can take a few seconds." The entry was written by Claude
  Code, anthropic/claude-opus-5-5, on the human's word. The status stays `in-progress`
- 2026-10-21T20:22:13Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, added to `plan.md` a section for the revised row
  of the speed, with its tests first and a commit point of its own, 4a, before the developer's checks, which the
  revised requirements 2, 3, 14 and 15 need and the plan did not hold. Added to the steps of the developer's checks
  the look of the row. The status stays `in-progress`
- 2026-10-21T20:24:41Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, did the tasks of commit point 4a, `67f790e`, and
  rewrapped three lines of `README.md` from commit point 5 that ran past 120 columns. On that tree, with the model in
  its folder, `./gradlew test` ran 398 tests: none failed, and two were skipped, `RealModelPackageTest` and
  `EngineThreadsMeasurement`, as on `master`. How the row looks in a running IDE is not checked by me, and is the new
  first step of the developer's check of R15. The status stays `in-progress`
- 2026-10-21T20:38:32Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, made two changes after the human looked at the
  revised row. The field of the speed was too narrow to show its number, since the "×" inside it takes room that the
  three columns of the field did not count, and it is now five columns wide. I captured the window of the sandbox
  IDE and saw "1.9" shown whole beside the "×". The human found that opening the tool window gave the focus to the
  field, which drew the eye to it. The panel now takes the focus itself, as the preferred focusable component of its
  content. That focus is not in the spec, and is decided by me on the human's word. No test checks how wide the field
  looks, since the tests run without the look and feel of the IDE and a test of its width passed with the field too
  narrow. At the human's request, and outside the scope of this unit, added to `AGENTS.md` in `2ebac0e` that a change to
  the user interface is looked at in a capture of the sandbox IDE's own window before it is handed to the developer.
  On that tree `./gradlew test` ran 399 tests: none failed, and two were skipped, `RealModelPackageTest` and
  `EngineThreadsMeasurement`, as on `master`. The status stays `in-progress`
- 2026-10-21T20:49:45Z [human] listened in the sandbox IDE on Linux x64, on `344f7ac`, to readings at speeds from 0.5 to 2.0 in steps of
  0.1, without keeping to the five paragraphs of requirement 15. Every speed above 1.0 sounded crisp and clear. The
  speed they would choose for themselves lies between 1.2 and 1.3, 1.5 was already too fast for them, and at 2.0 the
  words came so fast that they could barely follow them. At 0.5 the voice sounded bad and cracked now and then, and it
  went on cracking at every speed up to 0.8. They found the row of the speed clear, with the field no longer taking
  the focus when the tool window opens. The entry was written by Claude Code, anthropic/claude-opus-5-5, on the
  human's word, which the human asked to have recorded in other words than their own
- 2026-10-21T20:49:45Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux host, on
  `feature-0016-reading-speed` at `344f7ac`, revised `spec.md` on the human's findings, as requirement 15 provides where a
  speed is not understandable. Asked before writing, the human chose: 0.75 as the lowest speed, as a round number,
  knowing that it is among the speeds that cracked and that it can be moved up to 0.85; 2.0 kept as the highest; steps
  of 0.05 from 0.75 to 1.3 and of 0.1 from 1.3 to 2.0; and a field that takes any speed in the range with two decimals
  at the most. Decided by me and open to the human: a speed is shown with two decimals only where the second is not 0;
  a button from a typed speed between two steps goes to the next step; the settings page lists the steps and shows a
  typed speed between them in its place; the point stays the only decimal sign; and requirement 15 counts the human's
  findings above for the speeds they heard, and asks for the steps that are new and one typed speed between two steps.
  Requirements 1 to 4, 11, 15 and 16 now say so, and the decision on the range says why. The spec needs the human's
  approval again before it is built. The status stays `in-progress`
- 2026-10-21T20:51:42Z [human] approved the revised spec, with the point as the only decimal sign. The entry was written by Claude
  Code, anthropic/claude-opus-5-5, on the human's word. The status stays `in-progress`
- 2026-10-21T20:51:42Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, added to `plan.md` a section for the range set from
  what was heard, with its tests first and a commit point of its own, 4b, which the revised requirements 1 to 4, 11, 15
  and 16 need and the plan did not hold. The status stays `in-progress`
- 2026-10-21T20:57:03Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, did the tasks of commit point 4b, `e87ff99`. A test I
  wrote first had "Slower" from 1.37 go to 1.35, which is no step, since the steps above 1.3 are a tenth, and I
  corrected the test and the plan to 1.3, as requirement 2 says. A capture of the sandbox IDE showed the field cutting
  off the first digit of "0.75", which the human saw as well, so the field is now six columns wide, and a second
  capture showed "0.75" and the "×" whole. I added to `README.md` that the slowed voice may crack below 1.0, from what
  the human heard. On that tree, with the model in its folder, `./gradlew test` ran 402 tests: none failed, and two
  were skipped, `RealModelPackageTest` and `EngineThreadsMeasurement`, as on `master`. The case of the real engine at
  0.75 ran and passed. The status stays `in-progress`

## Amendments
