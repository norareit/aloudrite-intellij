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
- 2026-10-21T21:14:34Z [human] made step 5 of `developer-checks.md` on Linux x64, on `46b2734` with the field six columns wide, which they
  found right. At 0.75, 0.85, 0.95, 1.05, 1.15, 1.25 and a typed 1.37, every speed was understandable and sounded
  fine. At 0.75 they heard a stutter once. At one moment, at about 1.2, they heard what seemed an unrelated word or
  sound after a pause, which they could not make happen again. They did not report a silence in the middle of a
  sentence. The entry was written by Claude Code, anthropic/claude-opus-5-5, on the human's word
- 2026-10-21T21:14:34Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, looked in the log of the sandbox IDE for the
  unrelated sound. It holds the first sounds of the session, among them twenty after a change of the speed, from 693 to
  1428 ms, and four after a resume, from 10 to 14 ms, and no warning or error of the plugin. It does not show where the
  sound came from. Not reproduced, and no cause is known. The status stays `in-progress`
- 2026-10-21T21:18:12Z [human] decided not to look further into the unrelated sound at about 1.2, since they cannot say what came
  before it. It stays recorded as not reproduced. The human made steps 7, 8 and 9 of `developer-checks.md` on Linux
  x64, on `e655b03`: a single press on the plus button, three presses in a row and a typed "2" each behaved as the steps
  describe. The entry was written by Claude Code, anthropic/claude-opus-5-5, on the human's word
- 2026-10-21T21:18:12Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, read the log of the sandbox IDE for those steps. It
  holds one reading started at 23:16:44 local time and four first sounds after a change of the speed after it, of
  1816, 2297, 1580 and 2635 ms, each measured from the last change and, after a press on a button, with the wait of
  half a second in it. There are four lines for three steps and one reading for what the steps start as three, so the
  times cannot be matched to the steps with certainty: from a change to the sound at the new speed took between 1.6
  and 2.7 seconds in these steps. Earlier in the same session, while the human tried the speeds, twenty such times lay
  between 693 and 1428 ms. The status stays `in-progress`
- 2026-10-21T21:26:04Z [human] made steps 10 to 13 of `developer-checks.md` on Linux x64. Several changes in quick succession, a
  change while paused, and the limits and the field each behaved as the steps describe. The settings page worked, but
  its drop-down filled the whole height of the page. With `4dcae8e` the human found the page right. They did not say
  whether a typed 1.37 showed among the steps on the page. The entry was written by Claude Code,
  anthropic/claude-opus-5-5, on the human's word
- 2026-10-21T21:26:04Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, laid out the settings page in `4dcae8e` with the Kotlin
  UI DSL of the IntelliJ Platform in place of a `HorizontalLayout`, which gave the drop-down the height of the page.
  I did not look at the page in the sandbox IDE before handing it over, against the rule added to `AGENTS.md` in this
  unit. A test now lays the page out at 600 by 500 pixels and checks that the drop-down keeps its own height. It
  failed with the old layout, at 500 pixels against 26, and passes with the new one. On that tree `./gradlew test`
  ran 403 tests: none failed, and two were skipped, `RealModelPackageTest` and `EngineThreadsMeasurement`, as on
  `master`. The status stays `in-progress`
- 2026-10-21T21:30:14Z [human] confirmed the rest of step 13: with the speed set to 1.37, the settings page showed it among the
  steps, and once the speed was set to 1.3 and the page opened again, 1.37 was no longer among them, as requirement 4
  says. They chose to keep requirement 16 as it is written, and to make step 14 on Linux x64 and on macOS on Apple
  Silicon later, after the review. The entry was written by Claude Code, anthropic/claude-opus-5-5, on the human's word
- 2026-10-21T21:35:43Z [Claude Code, anthropic/claude-opus-5-5] started the reviewer on this unit without being asked, and stopped
  it at the human's word before it had written or committed anything. At the human's request, and outside the scope of
  this unit, added to `AGENTS.md` in `6199720` that the human runs the review of a unit in another harness and that an
  agent does not start the reviewer. The status stays `in-progress`
- 2026-10-21T21:41:51Z [OpenCode, openai/gpt-6.1-sol] as the reviewer, made a full review of unit 0016 at
  `8cb77b8c3673310fbc9d17613c6c0b93dce00270`, against merge base `4e5be2e13dcac1a104a31312ed4a8a3bff4059de` with
  `master`. Returned S1, S2, S3, P1, P2 and C1, saved in `.reviews/0016-reading-speed/2026-10-21-8cb77b8-full.md`.
  Independently ran `./gradlew cleanTest test buildPlugin`: 403 tests, none failed, two skipped, with all five real-engine
  cases run and the archive built; the two later commits changed documentation only. `npm run check-changes` and
  `git diff --check master...HEAD` passed on the reviewed commit. Reproduced S1, S2 and C1 against the compiled classes.
  Could not independently verify the rendered controls, listening results, device-level stop timing or the developer's
  log files; R16 remains deferred by the human. The optional engine-threads measurement was not independently repeated.
- 2026-10-21T21:45:21Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, in the main session on the Linux host, at the
  human's request, took up the findings of the review at `8cb77b8` and added a section for them to `plan.md`, with
  commit point 4c, `158aeaa`. Each regression test was written first and seen to fail before its correction. The outcome
  of each finding:
  - S1, accepted and applied. A test opened the page, set the speed elsewhere to 1.37 and then 2.0, and found the
    page still at 1.0, the same as the reviewer's reproduction. The page now follows `READING_SPEED_CHANGED` from
    `createComponent` to `disposeUIResources` and resets itself at a change, in any modality, since the settings are
    a modal dialog. The test now finds the page at each new speed, 1.37 among its choices, the page not modified, and
    an apply without an edit writing nothing. A second test checks that a closed page no longer follows. A change
    elsewhere drops an edit on the page that was not applied yet, which keeps the page showing the one speed of the
    IDE, as requirement 4 says
  - S2, accepted and applied. A test with two presses 300 ms apart before the first making had started found the
    piece made at the speed of the first press before the wait had passed. The first making now waits for the time
    the reading holds: where that time has not come, the engine work leaves a restart from the first segment, which
    the check of the wait starts, with the sample rate kept by the reading. The test now finds nothing made before
    500 ms after the last press and every piece made once, at its speed. A second test checks that a pause during
    that wait stays paused and that its resume starts the making
  - S3, accepted, and needs the human: steps 7, 8 and 9 of `developer-checks.md` are to be made again on Linux x64,
    each from a reading of its own, so that each logged time belongs to one step. The task of the check of R15 in
    `plan.md` now says so
  - P1, not disputed. The service and the settings page were written before their tests, as the record says above,
    and that order cannot be undone. The regression tests of this review were written first and seen to fail. The
    disposition is the human's
  - P2, accepted and applied: the open tasks of the developer's checks now ask for the steps and the speeds of the
    revised requirements 15 and 16
  - C1, accepted and applied. A test under the locale `ar-EG` found "0.٧٥" as the number of 0.75, the same as the
    reviewer's reproduction. The second decimal is now made without the locale, and the test finds "0.75" and
    "1.37", and 0.75 read back by `parsedSpeed`
  On that tree, with the model in its folder, `./gradlew test` ran 408 tests: none failed, and two were skipped,
  `RealModelPackageTest` and `EngineThreadsMeasurement`, as on `master`. All five cases of the real engine ran. The
  status stays `in-progress`
- 2026-10-21T21:47:08Z [human] decided on finding P1 of the review: the order in which the service and the settings page were
  written before their tests is accepted as it happened. The entry was written by Claude Code,
  anthropic/claude-opus-5-5, on the human's word. The status stays `in-progress`
- 2026-10-21T21:52:45Z [OpenCode, openai/gpt-6.1-sol] as the reviewer, made a delta review of unit 0016 at
  `c4ad54f6f2dade7a107686c9dad8ba77fde9893e`, from the previous review's commit
  `8cb77b8c3673310fbc9d17613c6c0b93dce00270`. Verified the prior corrections and returned the new S4; S3 remains
  open. Saved the complete review in `.reviews/0016-reading-speed/2026-10-21-c4ad54f-delta.md`. Independently ran
  `./gradlew cleanTest test buildPlugin`: 408 tests, none failed, two skipped, with all five real-engine cases run
  and the archive built. `npm run check-changes`, `git diff --check 8cb77b8..HEAD` and
  `git diff --check master...HEAD` passed. Reproduced S4 with a latch-controlled probe against the compiled service.
  Could not independently verify the rendered controls, listening results, device-level stop timing or developer
  log files; the replacement S3 timings and the deferred R16 checks remain pending.
- 2026-10-21T21:58:11Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, in the main session on the Linux host, took up
  finding S4 of the delta review at `c4ad54f`, and added a section for it to `plan.md`, with commit point 4d,
  `7f386f9`. S4 is accepted and applied. It came with my correction of S2. A test, written first, holds the first engine
  work on another thread just after it read the clock at 499 ms, runs the check of the wait at 500 ms on a third
  thread, and then lets the engine work go on. It failed as the reviewer's reproduction did: nothing was made and the
  reading stayed on. The reading now reads the clock under its own monitor, where it decides whether the first making
  starts and whether a waiting making is due, so that a check that comes after the decision finds the restart and one
  that came before it leaves a time that has come. The test now finds both pieces made at 1.05 and played, and it
  passed in ten runs in a row. On that tree, with the model in its folder, `./gradlew test` ran 409 tests: none
  failed, and two were skipped, `RealModelPackageTest` and `EngineThreadsMeasurement`, as on `master`. All five
  cases of the real engine ran. S3 stays open until the human makes steps 7, 8 and 9 again, each from a reading of
  its own. The status stays `in-progress`
- 2026-10-21T22:09:43Z [OpenCode, openai/gpt-6.1-sol] as the reviewer, made a delta review of unit 0016 at
  `c361a065a73f5d25d2ac48289a54777d83dbc4c0`, from the previous review's commit
  `c4ad54f6f2dade7a107686c9dad8ba77fde9893e`. Verified the S4 correction and its concurrent regression, with no new
  findings; S3 remains open. Saved the complete review in `.reviews/0016-reading-speed/2026-10-21-c361a06-delta.md`.
  Independently ran `./gradlew cleanTest test buildPlugin`: 409 tests, none failed, two skipped, with all five
  real-engine cases run and the archive built. `npm run check-changes`, `git diff --check c4ad54f..HEAD` and
  `git diff --check master...HEAD` passed. Could not independently verify rendered controls, listening results,
  device-level stop timing or developer log files; the replacement S3 timings and deferred R16 checks remain pending.
- 2026-10-21T22:32:12Z [human] made steps 7, 8 and 9 of `developer-checks.md` again on Linux x64, each from a reading of its own
  that held only that step's change, as finding S3 of the review asked: a single press on the plus button, three
  presses in a row on it, and "2" typed in the field. The entry was written by Claude Code, anthropic/claude-opus-5-5,
  on the human's word
- 2026-10-21T22:32:12Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, read the log of the sandbox IDE for those three
  readings. Each began with its own line of the first sound after "Read Aloud", at 00:30:17, 00:30:57 and 00:31:46
  local time, and held exactly one line of the first sound after a change of the speed: 1759 ms after the single
  press, 2006 ms after the last of the presses in a row, and 2174 ms after the typed speed. The first two include the
  wait of half a second after a press, and the third has none, so the engine took about 1.3, 1.5 and 2.2 seconds to
  make the piece again. Why the typed change took the longest was not looked into: the length of the piece that was
  playing differs between the readings, and the engine work of the dropped making finishes the sentence it is making
  before the next making may use the engine, but neither was measured. The spec sets no limit on this time. Finding
  S3 is corrected, and the check of R15 on Linux x64 is complete. The status stays `in-progress`
- 2026-10-21T22:37:01Z [human] made step 14 of `developer-checks.md` on Linux x64 at 2.0, on `bf43a46`: `samples/markdown-features.md`
  read to its end sounded fine, and they did not report a silence in the middle of a sentence. Around inline code the
  reading stumbled somewhat, which they put down to how the model pronounces code, already odd at 1.0 and more so at
  2.0. The entry was written by Claude Code, anthropic/claude-opus-5-5, on the human's word
- 2026-10-21T22:44:51Z [human] made step 14 of `developer-checks.md` on Linux x64 at 1.5, on `bc0484b`: `samples/markdown-features.md`
  sounded fine, close to a speed they would use, and they did not report a silence in the middle of a sentence.
  Inline code still sounded odd but could be followed. The entry was written by Claude Code,
  anthropic/claude-opus-5-5, on the human's word
- 2026-10-21T22:47:04Z [human] made step 14 of `developer-checks.md` on Linux x64 at 0.75, on `3f27ef2`. They found what makes 0.75 and
  the speeds below it sound odd: many sentences seem to begin with a short "a" sound, so that the items of a numbered
  list sound as "a one", "a two" and so on. They do not find it a problem, and 0.75 stays the lowest speed. The entry
  was written by Claude Code, anthropic/claude-opus-5-5, on the human's word
- 2026-10-21T22:52:49Z [human] added to step 14 on Linux x64 at 0.75: they stopped the reading after the first of the two chapters of
  prose, as listening to the whole file took too long, so the second chapter was not heard at 0.75. In the prose the
  short "a" sound did not seem to occur; they think it comes at special characters, at the start of a paragraph or
  after a silence, and it is no problem to them. They did not report a silence in the middle of a sentence or say
  how the silences sounded at 0.75. The entry was written by Claude Code, anthropic/claude-opus-5-5, on the human's
  word
- 2026-10-21T23:04:24Z [human] made step 14 of `developer-checks.md` on macOS on Apple Silicon at 0.75 and at 1.5. At 0.75 the
  reading sounded about the same as on Linux x64. At 1.5 the heading "Bulleted list" of `samples/markdown-features.md`
  seemed not to be read, the same in two readings, and further on more and more parts seemed to be left out. They
  suspect the Mac cannot keep up at that speed. The entry was written by Claude Code, anthropic/claude-opus-5-5, on
  the human's word
- 2026-10-21T23:04:24Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, took the report as a problem of the unit that is
  not reproduced and has no known cause. At 1.5 on Linux x64 the human heard the whole file. Requirement 13 says a
  reading has no silence in the middle of a sentence at every speed on both machines, and requirement 12 that what is
  read is the same at every speed, and a part that is not heard would make one of them false on macOS. The status
  stays `in-progress`
- 2026-10-21T23:10:06Z [human] added that on macOS at 1.5 there were odd silences where the parts not heard should have been, and
  that the log of the sandbox IDE on the Mac holds only the first sounds and that the engine was made with 6 threads
  of 16 processors, with no warning or error of the plugin. The entry was written by Claude Code,
  anthropic/claude-opus-5-5, on the human's word
- 2026-10-21T23:10:06Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, added a section to `plan.md` for the parts not
  heard on macOS, and in `8695f68` the opt-in `EngineSilenceMeasurement`, which makes every piece of
  `samples/markdown-features.md` at 1.0, 1.5 and 2.0 with the real engine and counts per piece the samples that are
  not a number and those that are silent. The plugin plays samples that are not a number as silence, which would
  sound as the human describes, and an engine that falls behind would leave a gap and then read the words rather
  than leave them out. That is a guess the measurement is to settle. Run on Linux x64, all 136 pieces had sound at
  each of the three speeds, none of them mostly silence or not a number. The status stays `in-progress`
- 2026-10-21T23:23:27Z [human] ran `EngineSilenceMeasurement` on macOS on Apple Silicon, on `0ba571f`, with 6 engine threads of 16
  processors, and handed over the summary of `build/engine-silence.txt`. The entry was written by Claude Code,
  anthropic/claude-opus-5-5, on the human's word
- 2026-10-21T23:23:27Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, read that summary. Of the 136 pieces of
  `samples/markdown-features.md`, the engine on macOS made 1 at 1.0, 30 at 1.5 and 56 at 2.0 as samples of which
  every one is not a number, where on Linux x64 none was. Such a piece has a plausible length, from 13800 to 27600
  samples, but no sound, and the plugin plays it as silence. They are all short pieces: headings such as "Bulleted
  list", the numbers of a numbered list, items such as "Food.", the parts between inline code, and short sentences
  such as "He did not mind.". At 1.0 the one piece is "Call ", before inline code, so a piece is lost on macOS at the
  speed of every unit before this one as well, more rarely. The cause lies in the native engine on that platform, and
  why it gives no number for those pieces is not known. Requirements 12 and 13 are false on macOS at 1.5 and 2.0 as
  the unit stands. The status stays `in-progress`
- 2026-10-21T23:37:48Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, at the human's request, looked for issues of
  sherpa-onnx like the pieces made as not a number on macOS. Issue 3791 of k2-fsa/sherpa-onnx, a fault of onnxruntime
  1.27.0 on Apple chips with SME, does not apply: the library for macOS of sherpa-onnx 1.13.8 holds onnxruntime 1.28.2.
  Issue 3754 is close to what was found here: with the int8 model `kokoro-int8-multi-lang-v1_0`, which the plugin
  uses, on an ARM phone, the engine at times makes a whole piece as garbage, every sample at about +1.0 rather than not
  a number, most often for short pieces, depending on what was made before, never on x86_64, and never with the fp32
  model `kokoro-multi-lang-v1_0`. The maintainer marked it invalid for not following the issue template, and it is
  open with no fix. Whether the cause here is the same is not known; the second run of the measurement on macOS, with
  one thread and a second try of each failing piece, is to say more. The status stays `in-progress`
- 2026-10-21T23:39:52Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, at the human's request, keeps
  `EngineSilenceMeasurement` in the project for later use, and added to `plan.md` a task for it and to `README.md` in
  `fc7379f` how it is run, beside `EngineThreadsMeasurement`. It runs only with `ALOUDRITE_MEASURE_ENGINE_SILENCE` set, so
  `./gradlew test` reports it as skipped. The status stays `in-progress`
- 2026-10-21T23:48:55Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, at the human's request, downloaded the fp32
  model `kokoro-multi-lang-v1_0` on the Linux host, 349906910 bytes, checked it against the SHA-256 that GitHub gives
  for the release file, `c5f7e2d2caf082bc1d20fb70334a61d99d20b484500aad32e7cf84c128ea3298`, and unpacked it next to the
  int8 model, in `~/.local/share/aloudrite/kokoro-multi-lang-v1_0`, where it takes 384 MB against 183 MB. The plugin
  does not use it. In `b5e4056` the measurement of silence takes another model from two environment variables, with the
  model file of `KokoroSpeechEngine` an argument whose default is the plugin's int8 file. A run of it against fp32 on
  Linux x64 was stopped on the human's word, since Linux does not show the fault. The human considers a choice
  between the models on the settings page, with a download from there, which would be a unit of its own. On that tree
  `./gradlew test` ran 410 tests: none failed, and three were skipped, `RealModelPackageTest`,
  `EngineThreadsMeasurement` and `EngineSilenceMeasurement`, which need settings of their own. The status stays
  `in-progress`
- 2026-10-22T10:45:47Z [human] ran the extended `EngineSilenceMeasurement` on macOS on Apple Silicon with the int8 model and handed
  over its summary. The entry was written by Claude Code, anthropic/claude-opus-5-5, on the human's word
- 2026-10-22T10:45:47Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, read that summary. Of 136 pieces, those made as
  mostly silence or not a number, and of those the ones made so again on a second try with the same engine: with 6
  threads, 1 and 0 at 1.0, 25 and 11 at 1.5, 50 and 33 at 2.0; with 1 thread, 129 and 117 at 1.0, 118 and 102 at 1.5,
  112 and 97 at 2.0. One thread makes it far worse, nearly every piece at every speed, so it is no race between
  threads. A second try gives sound for part of the pieces, so it does not depend on the text alone. The plugin uses
  half the processors with at most 6 threads, so a Mac with fewer processors than this one, 16, would run the engine
  with fewer threads, and might lose most of a reading at 1.0, before this unit as well. That was not tried on such a
  Mac. The run with the fp32 model on macOS is still to come. The status stays `in-progress`
- 2026-10-22T11:11:23Z [human] ran `EngineSilenceMeasurement` on macOS on Apple Silicon against the fp32 model
  `kokoro-multi-lang-v1_0`, downloaded and checked against its SHA-256 and unpacked next to the int8 model, and handed
  over its summary. A first try had failed because the unpacking was cut off, and was made again. The entry was
  written by Claude Code, anthropic/claude-opus-5-5, on the human's word
- 2026-10-22T11:11:23Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, read that summary: with the fp32 model, none of the
  136 pieces was made as silence or not a number, with 6 threads or with 1, at 1.0, 1.5 or 2.0. The pieces lost on
  macOS come from the int8 model on that platform, and the fp32 model does not lose them. How fast the fp32 model makes
  its audio on macOS, and so whether it stays ahead of the playing, was not measured. The status stays `in-progress`

## Amendments
