---
kind: feature
status: implemented
timestamp: 2026-11-21T11:56:00Z
---

# The speech core as a module of its own

## Log

- 2026-11-21T11:56:00Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `27d1d17`, at the human's request, wrote `spec.md`. Read `process.md`, the standards and
  `roles/specifier.md` of the installed kit, 8.4.0. Made it a feature and numbered it 0036, after 0035. Went through
  the imports of every source file of the plugin and the constructor of `SpeechService`, as the context of the spec
  says. Decided with the human: a module with no dependency on the IntelliJ Platform, now, and no port to TypeScript.
  Decided by me and open to the human: a module of the build rather than a library, the thing read as a type of the
  caller's choosing, the download of the model and the highlights kept in the plugin, and the package
  `com.ritenoar.aloudrite.core`. Left beyond the request: publishing `core` as a library, and moving the download of
  the model to `core`. The status is `draft`
- 2026-11-21T22:35:01Z [human] approved the spec. The status is `approved`
- 2026-11-22T01:04:19Z [Claude Code, anthropic/claude-opus-5-5] as the planner, in a unit run of the sandbox, on the
  branch at `433363f`, wrote `plan.md`. Read `process.md`, the standards and `roles/planner.md` of the installed kit.
  The tree has moved on since the spec was written at `27d1d17`: unit 0028 added the word alignment and the common type
  of the downloaded packages, and `SpeechService` now also takes the aligning of the pieces and the delay of the word.
  The alignment finds which word is heard and the reading uses it, so it moves to `core` under requirement 2, which the
  plan says, and nothing in the spec conflicts with the tree. Decided in the plan: the class of requirement 3 is
  `ReadingAloud<Target>`, with the interfaces of the reading made generic and the service of the plugin holding it as
  `reading`; the shared stand-ins as test fixtures of `core`; the Kotlin standard library left to the IDE as now; the
  commits that relocate code to `core` change only the package, the imports and the references the package forces. The
  status stays `approved`
- 2026-11-22T01:20:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, made `PieceAligning` take all its
  parts from its caller: the constructor without arguments and the companion went to `pieceAligningOfTheIde()` in
  `IdePieceAligning.kt`, and `FirstAudioMeasurement` builds its own. `./gradlew test` reports the cases of the baseline
  with the same outcomes
- 2026-11-22T01:45:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, split `SpeechService` into
  `ReadingAloud<Target : Any>` and the IntelliJ service that holds it as `reading`, as the plan says. One deviation in
  the tests: besides `closingTheProjectStopsItsReadingAndNoOther`, two more tests of `ReadingAloudTest` called
  `projectClosed`, one on a waiting reading and one on a paused one. As `ReadingAloud` has no project, they kept their
  subject as the forgetting of a target by a condition, with `forgetWhere { true }`, and were renamed to say so. The
  nested classes `Reading` and `WaitingReading` became generic in `Target`, since a nested class cannot use the type
  parameter of its outer class. `./gradlew test` reports 1061 cases, 1053 passed and 8 skipped as in the baseline, with
  the cases of the three renamed classes under their new names and the project test under the new `SpeechServiceTest`
- 2026-11-22T02:05:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, made `SpeechStandIns` generic in
  the target and changed `ReadingAloudTest`, `ReadingAloudHeardTest`, `ReadingAloudTimeTest`, `PartOfTheFileTest`,
  `SpokenMarkdownTest` and `FirstAudioMeasurement` to read targets that are strings, without the `ApplicationRule`.
  `grep -l "com.intellij\|org.intellij"` over the main files, the tests and the test fixtures that are to move names
  none. `./gradlew test` reports 1061 cases, 1053 passed and 8 skipped, as the commit before
- 2026-11-22T02:40:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, moved the 33 main files, the 43
  tests and the 2 test fixtures of the plan to `core`, with the build declarations of the engine, the phonemes, the
  Markdown parser and ONNX Runtime. `./gradlew test` reports 1061 cases, 1053 passed and 8 skipped, the same counts as the
  baseline, split between `build/test-results/test` and `core/build/test-results/test`, with every case of the baseline
  present and passed under its class, bar the three of the deviation above under their new names.
  `EspeakPhonemesTest` (6 cases), `EspeakDataTest` (1), `KokoroSpeechEngineTest` (6), `KokoroVoicesTest` (2) and
  `RealAlignerTest` (2) ran and passed, with none skipped
- 2026-11-22T03:10:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, wrote
  `ReadingAloudWithoutThePlatformTest` in `core`. It reads a Markdown text through `whatToRead` and a
  `ReadingAloud<String>`, pauses it in its first piece, resumes it and reads it to its end, checks that every piece was
  made once and in order, that the reading ended and that no failure was reported, and that
  `Class.forName("com.intellij.openapi.application.ApplicationManager")` throws `ClassNotFoundException`. It passes
- 2026-11-22T03:20:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, wrote the README: the two
  modules under "Building", the tests of both modules under "Testing", and the commands of the six measurements, in the
  README and in their comments, as `./gradlew :core:cleanTest :core:test --tests com.ritenoar.aloudrite.core.<Name>`.
  Every `com.ritenoar.aloudrite.<Name>` that `git grep` finds outside `changes/` in a file that is not Kotlin is the id
  of an action or of a configurable or the class of the plugin, none of a class that moved. The six measurements were
  not run, as they are skipped without their properties, so their commands were not tried
- 2026-11-22T03:30:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, verified the final tree at the
  commit before this entry. `./gradlew test buildPlugin` passes, with 1062 cases, 1054 passed and 8 skipped, the
  baseline's 1061 and the new test, the skipped being those of the baseline. `./gradlew :core:dependencies` for
  `compileClasspath`, `runtimeClasspath`, `testCompileClasspath`, `testRuntimeClasspath`,
  `testFixturesCompileClasspath` and `testFixturesRuntimeClasspath` names these artifacts: the jars of sherpa-onnx and of
  piper-phonemize with their native libraries, commonmark with its four extensions and `autolink`, ONNX Runtime 1.22.0,
  JUnit 4.13.2 with `hamcrest-core`, and `kotlin-stdlib` with `org.jetbrains:annotations:13.0`. None is of the groups
  `com.jetbrains.intellij.*` or `com.jetbrains`, and none has `intellij` or `idea` in its name. The archive is 21949243
  bytes, against 21944816 of the baseline, and its `lib/` equals that of the baseline with
  `aloudrite-core-0.0.1.jar` of 225870 bytes added and the plugin's own jar at 292629 bytes, down from 512333. Every
  Kotlin file under `core/` has the package `com.ritenoar.aloudrite.core` and none under `src/` has. In the move
  commit, `git show -M --summary` lists 78 renames, which are all 78 files moved, with a similarity from 79% to 98% at
  the median, and no other file moves. `npm run check-changes` passes
- 2026-11-22T01:22:21Z [OpenCode, openai/gpt-6.1-sol] as the reviewer, completed a full review of unit 0036 at
  `d50eb1ab6e55718f6cb31fb33856c11c548effa3`, against merge base `433363f` with `origin/master`. Returned the
  nonblocking plan-fidelity finding P1, saved in `.reviews/0036-the-speech-core-as-a-module-of-its-own/review-round-1.md`.
  Independently ran `./gradlew test buildPlugin --rerun-tasks`: 1062 cases, 1054 passed and 8 opt-in skips, with the real
  engine and aligner tests passing without skips. Verified the six core classpaths, the separate core jar, the archive
  size, `:core:build` and the change check. The retained baseline test-case inventory was unavailable for an independent
  case-by-case comparison; listening and macOS execution were not performed. No spec-conformance or code-quality
  findings. The status stays `in-progress`
- 2026-11-22T01:40:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, accepted the finding P1 of
  `review-round-1.md`, as the definition of the orchestrator for round 2 lets the step accept findings it agrees with,
  and not as a decision of the human. `ReadingAloudWithoutThePlatformTest` now also asserts the audio the player
  received: the values of the samples played, with the runs of equal values collapsed for the replay of the first piece
  at the resume, are each piece once and in order. The plan needed no change, as it already says that. `./gradlew test`
  reports 1062 cases, 1054 passed and 8 skipped on the tree of this change. The status stays `in-progress`
- 2026-11-22T01:26:16Z [OpenCode, openai/gpt-6.1-sol] as the reviewer, completed a delta review of unit 0036 at
  `1fe1d01`, compared with the previously reviewed commit `d50eb1ab6e55718f6cb31fb33856c11c548effa3`. Verified the
  correction of P1 and returned no new findings on any of the three axes. Saved the review in
  `.reviews/0036-the-speech-core-as-a-module-of-its-own/review-round-2.md`. Independently ran
  `./gradlew test --rerun-tasks`: 1062 cases, 1054 passed and 8 opt-in skips, with the real engine and aligner tests
  passing without skips. The change check and diff whitespace check passed. Packaging and classpath checks were not
  repeated for this test-only correction; listening and macOS execution were not performed. The status stays
  `in-progress`
- 2026-11-22T02:08:39Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, in the main session on the Linux
  host, recorded the developer's checks and set the status to `implemented`. The developer ran the plugin with
  `./gradlew runIde` on Linux x64 and found that reading, pausing, resuming, stopping and changing the speed work, that
  the words are highlighted as they are heard, that the tool window shows what it showed before, that reading a
  selection and reading from the caret work, that closing the file being read stops the reading, and that switching to
  the int8 model and to other voices works. Not checked: reading from the preview of a Markdown file, closing the
  project being read, removing the model of the current reading, and the download of the model. The human decided that
  no check on macOS on Apple Silicon is needed, as the jars with the native libraries are unchanged and only the code
  that loads them moved, and that the timestamps of the implementer's entries above stay as they are, although they do
  not match the times of their commits. On the host, `./gradlew cleanTest test` on `15bcfaf` failed in 2 of 8 runs, each
  time in `RealAlignerTest` on "Two candles and a box of matches." at 1.5×, where the aligner ends "matches" at 1.390 s
  in about 1.385 s of audio, the length of which varies by a few samples from run to run. Run alone, the test passed 3
  of 3 times. On `master` at `433363f`, the same test failed in the same way in 1 of 13 runs of the full suite and
  passed 3 of 3 times alone, so the failure is older than this unit and not caused by it, and a bug unit of its own
  takes it up. The last run on `15bcfaf` passed, with 1062 cases, 1054 passed and 8 skipped, the skipped being those of
  the baseline. No blocking finding is left: P1 does not block and was applied, and round 2 returned no findings. The
  status is `implemented`

## Amendments
