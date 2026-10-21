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

## Amendments
