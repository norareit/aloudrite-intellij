# The speech core as a module of its own

## Approach

The plugin becomes a Gradle build of two modules: the root project, which stays the plugin with the package
`com.ritenoar.aloudrite`, and a new module `core`, in the folder `core/`, with the package `com.ritenoar.aloudrite.core`
and no IntelliJ Platform on any of its classpaths. The plugin depends on `core`, and its archive carries `core` as the
jar `aloudrite-core-0.0.1.jar` in `lib/`.

What was found for this plan, on 2026-11-22, on the branch at `433363f`, in the sandbox:

* The tree has moved on since the spec was written at `27d1d17`. Unit 0028 added the word alignment
  (`WordAlignment.kt`, `PieceAligning.kt`, `ForcedWordAligner.kt`, `AlignerModel.kt`, `HeardWordChoice.kt`) and the
  common type of the two downloaded packages (`InstallablePackage.kt`), and unit 0035 removed `EngineText.kt`.
  `SpeechService` is now 879 lines, and its constructor also takes `pieceAligning` and `wordDelayMillis`. The alignment
  finds which word is heard, so it falls under "the estimate of the word heard" of requirement 2, and the reading uses
  it, so it moves with the reading. That does not conflict with the spec
* Of the 69 main source files, 32 reference neither a class of the platform nor one of the plugin's files that does,
  once two things are rewritten: `PieceAligning` has a constructor without arguments that uses `AppExecutorUtil` and
  `Logger`, and `SpeechService` holds the reading together with its IDE parts. Both are rewritten before any move
* `SpeechModel` and `AlignerModel` implement `InstallablePackage`, which holds `ModelPackage`. Both are plain values
  (an address, sizes, a checksum, a folder name) and go to `core` as the description of the models. Everything that
  downloads and installs them stays in the plugin, as the spec decides: `ModelDownloadService`, `ModelDownloadOffer`,
  `ModelDownloadMessages`, `ModelInstaller`, `InstallAttempt`, `InstallFailure`, `PackageDownload`,
  `PackageUnpacking`, `OneAtATime` and `SpeechModelManagement`
* `gradle.properties` sets `kotlin.stdlib.default.dependency=false` for the whole build, since the IDE provides the
  Kotlin standard library. `core` therefore declares the standard library itself, as `compileOnly` for its main code
  and its test fixtures and as `testImplementation` for its tests, so that its tests run with it and the plugin
  archive still carries none
* The tests of what moves read `samples/...` and write `build/...` by paths relative to the working directory, which
  Gradle sets to the project's folder. The test task of `core` gets the root folder as its working directory, so that
  these paths, and the paths the README gives for the measurements, stay as they are
* Gradle resolves the module dependencies of `core` again in the plugin, with the plugin's repositories, so both
  modules need the Ivy repositories of sherpa-onnx and piper-phonemize. They are declared once, in an `allprojects`
  block of the root build script, with the Java 25 target and JUnit, which both modules share. The plugin adds the
  repositories of the IntelliJ Platform in its own block
* `./gradlew test` from the root runs the task `test` of both projects. `./gradlew test --tests <class>` fails in the
  project where the filter matches nothing, so the commands of the measurements become `:core:`-qualified
* The model, both speech models and the aligner model are in place in the sandbox under `~/.local/share/aloudrite`

**The reading in `core` (R3).** `SpeechService` is split in two:

* `ReadingAloud<Target : Any>` in `ReadingAloud.kt` is the whole of the reading as `SpeechService` holds it now: the
  class `Reading`, the makings, the playing work, the engine kept, pause, resume, stop, `changeSpeed`, `modelChanged`
  and `readOnceTheModelIsInPlace`. Its constructor takes the same 14 parts as that of `SpeechService` now, in the same
  order and with the same names, with `Target` in place of `ReadingTarget`. It implements `AutoCloseable`, whose
  `close` is what `dispose` does now. `fileClosed(target)` becomes `forget(target)`, and the private `forgetWhere` is
  made public, so that the plugin can forget the readings of a project without `core` knowing a project. The narrow
  interfaces go with it and take the type of the target: `Readings<Target>`, `CurrentReading<Target>`,
  `HeardReading<Target>`, `HeardState<Target>`, with `ReadingModel` and `FirstSoundAfter` as they are.
  `NANOSECONDS_PER_MILLISECOND` and `MILLISECONDS_PER_SECOND` become public top-level constants of the file, since the
  plugin's part uses them too
* `SpeechService` in the plugin stays the `@Service(Service.Level.APP)` and `Disposable`, with
  `val reading: ReadingAloud<ReadingTarget>` as its primary constructor parameter and the constructor without arguments
  that builds it from the parts of the IDE as now. It keeps `projectClosed(project)`, which calls
  `reading.forgetWhere { it.project == project }`, `dispose`, which calls `reading.close()`, and in its companion what
  names the IDE: `LOG`, `logReadingTime`, `logFirstSound`, `createKokoroEngine` and `logAndShowFailure`
* The plugin's callers ask the service for `reading`: `service<SpeechService>().reading` in `ReadAloudAction`,
  `StopReadingAction`, `ReadAlongHighlighter`, `SpeechModelManagement`, `ReadingToolWindowFactory`,
  `ReadingSpeedSetting` and `SpeechModelSetting`, and `service<SpeechService>().reading.forget(...)` in
  `ReadingTabListener`. The types of their parameters become `Readings<ReadingTarget>`, `CurrentReading<ReadingTarget>`,
  `HeardReading<ReadingTarget>` and `HeardState<ReadingTarget>`

**`PieceAligning` without the platform.** Its constructor without arguments, the companion's `LOG`, `EXECUTOR` and
`runOnTheAlignmentThread` go to a new function `pieceAligningOfTheIde()` of the plugin, in `IdePieceAligning.kt`, which
builds the same `PieceAligning` from the same four parts, keeps the logger of `PieceAligning::class.java` and the
executor named "AloudRite alignment". The category of that logger then follows the package of `PieceAligning`, which
nothing reads. `FirstAudioMeasurement`, which calls `PieceAligning()` now, builds it from the real aligner, a
single-thread executor of `java.util.concurrent` and a failure it rethrows, since it runs without the IDE.

**The tests (R4, R6).** The tests of what moves go to `core/src/test/`, and what the tests of both modules use goes to
the test fixtures of `core`, in `core/src/testFixtures/`, with the Gradle plugin `java-test-fixtures`, which the plugin
takes as `testImplementation(testFixtures(project(":core")))`:

* The test fixtures: `SpeechStandIns.kt` and `KokoroModelStandIn.kt`. `SpeechStandIns` becomes
  `SpeechStandIns<Target : Any>`, builds a `ReadingAloud<Target>` as `reading` in place of the `SpeechService` as
  `service`, and `ReadingTimeReport` and `failures` take `Target`. The plugin's tests use
  `SpeechStandIns<ReadingTarget>()`
* The tests of the reading that use a `ReadingTarget` of a `LightVirtualFile` now, `SpeechServiceTest`,
  `HeardReadingServiceTest`, `ReadingTimeServiceTest`, `PartOfTheFileTest`, `SpokenMarkdownTest` and
  `FirstAudioMeasurement`, read a target of their own, a `String` such as `"notes.md"`, and lose the `ApplicationRule`
  that only the virtual files and the default project needed. The test of `projectClosed`,
  `closingTheProjectStopsItsReadingAndNoOther`, needs a project and stays in the plugin, in a new `SpeechServiceTest`
  that builds `SpeechService(standIns.reading)`. The old `SpeechServiceTest` becomes `ReadingAloudTest`,
  `HeardReadingServiceTest` becomes `ReadingAloudHeardTest` and `ReadingTimeServiceTest` becomes
  `ReadingAloudTimeTest`, as their subject is now `ReadingAloud`
* The test of requirement 4, `ReadingAloudWithoutThePlatformTest` in `core`, reads a Markdown text of three paragraphs
  through `whatToRead` and a `ReadingAloud<String>` of `SpeechStandIns`, pauses it after its first piece is heard,
  resumes it, runs it to its end, and checks that every piece was heard once and in order, that the reading has
  ended, and that `Class.forName("com.intellij.openapi.application.ApplicationManager")` throws
  `ClassNotFoundException`

**The files and where they go.** Main code moved to `core/src/main/kotlin/com/ritenoar/aloudrite/core/`:
`AlignerModel`, `AudioAhead`, `AudioPlayer`, `EngineThreads`, `EspeakPhonemes`, `ForcedWordAligner`, `FrontMatter`,
`HeardWordChoice`, `HeardWordEstimate`, `InstallablePackage`, `KokoroModel`, `KokoroSpeechEngine`, `ModelPackage`,
`PartOfTheFile`, `PcmAudio`, `PhonemeRows`, `PieceAligning`, `PlacedPart`, `PlayedAudio`, `ReadingAloud`,
`ReadingSegments`, `ReadingSpeed`, `ReadingTime`, `SamplePlayer`, `Sentences`, `SpeechEngine`, `SpeechModel`,
`SpeechPlatform`, `SpokenMarkdown`, `SpokenPart`, `TextPieces`, `Voice` and `WordAlignment`.

Tests moved to `core/src/test/kotlin/com/ritenoar/aloudrite/core/`: `AlignerModelTest`, `AudioAheadTest`,
`AudioPlayerTest`, `EngineLengthsMeasurement`, `EngineSilenceMeasurement`, `EngineThreadsMeasurement`,
`EngineThreadsTest`, `EspeakDataTest`, `EspeakPhonemesTest`, `FirstAudioMeasurement`, `FrontMatterTest`,
`HeardWordChoiceTest`, `HeardWordEstimateTest`, `HighlightFollowing`, `HighlightFollowingMeasurement`,
`HighlightFollowingTest`, `KokoroModelTest`, `KokoroSpeechEngineTest`, `KokoroTtsConfigTest`, `KokoroVoicesTest`,
`ModelPackageTest`, `PartOfTheFileTest`, `PcmAudioTest`, `PhonemeRowsTest`, `PhonemesProbeProcess`,
`PieceAligningTest`, `PlacedPartsTest`, `PlayedAudioTest`, `ReadingAloudTest`, `ReadingAloudHeardTest`,
`ReadingAloudTimeTest`, `ReadingSegmentsTest`, `ReadingSpeedTest`, `ReadingTimeMeasurement`, `ReadingTimeTest`,
`RealAlignerTest`, `SpeechModelTest`, `SpeechPlatformTest`, `SpokenMarkdownTest`, `TextPiecesTest`, `TrialSentences`,
`VoiceTest` and `WordAlignmentTest`.

Everything else stays in the plugin, among it `Following`, `PreviewText`, `PreviewPlaces`, `TextEdits` and the
download, as the spec decides, and `ReadingTarget`, `ReadingChange` and the settings.

**The commits (R8).** A commit that relocates code to `core` changes nothing in a moved file but its `package` line,
its imports and the qualified names in strings that the new package forces, as the main class of the probe process
in `EspeakDataTest`; the plugin's files gain the imports of what moved, and the build declarations move with the code
that needs them. Everything that changes how code is written is done before, in the plugin's module, in commits that
relocate nothing to `core`: the rewrite of `PieceAligning`, the split of `SpeechService` and the tests without the
platform. The split of `SpeechService` keeps the lines it carries to the new `SpeechService.kt` unchanged, so that the
reviewer reads it as the rewrite it is. The documents follow in a commit of their own.

## Files

- `settings.gradle.kts`: includes `core`
- `build.gradle.kts`: an `allprojects` block with the group, the version, the shared repositories, the Java 25 target
  and JUnit; the dependency on `core` and on its test fixtures; the dependencies of the engine, the phonemes and the
  Markdown parser, the stripping of piper-phonemize, the published archive of piper-phonemize for the test of the data,
  and ONNX Runtime go to `core`
- `core/build.gradle.kts`: new, the module `core`
- `src/main/kotlin/com/ritenoar/aloudrite/SpeechService.kt`: becomes `ReadingAloud.kt` and moves to `core`; a new
  `SpeechService.kt` holds the IntelliJ service
- `src/main/kotlin/com/ritenoar/aloudrite/IdePieceAligning.kt`: new, the `PieceAligning` of the IDE
- `src/main/kotlin/com/ritenoar/aloudrite/PieceAligning.kt`: loses its constructor without arguments and its
  companion, then moves to `core`
- `src/main/kotlin/com/ritenoar/aloudrite/{ReadAloudAction,StopReadingAction,ReadAlongHighlighter,
  SpeechModelManagement,ReadingToolWindowFactory,ReadingSpeedSetting,SpeechModelSetting,ReadingListeners,ReadingPanel,
  ReadingTitleActions}.kt`: take `reading` of the service and the generic interfaces, and import from `core`
- The other main files of the plugin that use a moved class: gain its import
- The 32 other main files and the 43 tests named above: move to `core`
- `src/test/kotlin/com/ritenoar/aloudrite/{SpeechStandIns,KokoroModelStandIn}.kt`: move to
  `core/src/testFixtures/kotlin/com/ritenoar/aloudrite/core/`
- `src/test/kotlin/com/ritenoar/aloudrite/SpeechServiceTest.kt`: new, the test of `projectClosed`
- The plugin's tests that use `SpeechStandIns` or a moved class: `SpeechStandIns<ReadingTarget>`, `reading` in place
  of `service`, and the imports from `core`
- `core/src/test/kotlin/com/ritenoar/aloudrite/core/ReadingAloudWithoutThePlatformTest.kt`: new (R4)
- `README.md`: the two modules, and the commands of the measurements
- The comments of the six measurements that give their command: `:core:` and the new package

## Tasks

### The baseline

- [x] Run `./gradlew test` with the models in place and keep, outside the repository, the list of every test case of
  `build/test-results/test/*.xml` as class and method, with its outcome, and the count of tests, failures and skipped.
  Run `./gradlew buildPlugin` and keep the listing of `lib/` of `build/distributions/aloudrite-intellij-0.0.1.zip`
  with the sizes, and the size of the archive. Record the counts and the size in `record.md` (R6)

### The module `core`, empty

- [x] Add `include("core")` to `settings.gradle.kts` (R1)
- [x] In `build.gradle.kts`, add an `allprojects` block that sets `group = "com.ritenoar"` and `version = "0.0.1"`,
  declares `mavenCentral()` and the two `exclusiveContent` Ivy repositories, with their comments, and, in
  `pluginManager.withPlugin("org.jetbrains.kotlin.jvm")`, sets `sourceCompatibility` and `targetCompatibility` of the
  Java extension to 25 and `jvmTarget` of the Kotlin compiler options to `JVM_25`, with the comment on the JDK of the
  IDE, and adds `testImplementation("junit:junit:4.13.2")`. Remove those declarations from the root's own blocks, which
  keep `intellijPlatform { defaultRepositories() }` in `repositories`, and remove the root's `group` and `version`
  (R1)
- [x] Add `implementation(project(":core"))` and `testImplementation(testFixtures(project(":core")))` to the root's
  dependencies (R1)
- [x] Write `core/build.gradle.kts`: the plugins `org.jetbrains.kotlin.jvm`, without a version, since the root's
  `plugins` block puts it on the classpath, and `java-test-fixtures`; `base { archivesName = "aloudrite-core" }`; the
  dependencies `compileOnly(kotlin("stdlib"))`, `testFixturesCompileOnly(kotlin("stdlib"))` and
  `testImplementation(kotlin("stdlib"))`, with a comment that the IDE provides it to the plugin and the archive carries
  none; and `tasks.test { workingDir = rootProject.projectDir }`, with a comment that the tests read `samples/` and
  write `build/` of the repository (R1, R6)
- [x] Run `./gradlew test` and `./gradlew buildPlugin`. The results equal the baseline, and `lib/` holds
  `aloudrite-core-0.0.1.jar` beside what it held, and no `kotlin-stdlib` (R1, R6)
- [x] Commit: the module `core`, empty

### `PieceAligning` without the platform

- [x] Write `IdePieceAligning.kt` with `fun pieceAligningOfTheIde(): PieceAligning`, from the constructor without
  arguments and the companion of `PieceAligning`, as the approach says, and remove those from `PieceAligning.kt` along
  with its imports of the platform. `SpeechService` calls `pieceAligningOfTheIde()` in place of `PieceAligning()`
  (R1, R2)
- [x] `FirstAudioMeasurement` builds its `PieceAligning` from `ALIGNER_MODEL` in `userModelsFolder()`,
  `ForcedWordAligner`, `Executors.newSingleThreadExecutor()`, shut down at the end of the measurement, and a
  `logFailure` that rethrows (R1, R6)
- [x] Run `./gradlew test`; the results equal the baseline (R6)
- [x] Commit: `PieceAligning` takes all its parts from its caller

### The reading as a class that does not know the IDE

- [x] `git mv` `SpeechService.kt` to `ReadingAloud.kt`, and in it turn `SpeechService` into `ReadingAloud<Target : Any>`
  as the approach says: `Target` for `ReadingTarget`, the interfaces and `HeardState` generic, `AutoCloseable` with
  `close` for `Disposable` with `dispose`, `forget` for `fileClosed`, `forgetWhere` public, `projectClosed`, the
  annotation, the constructor without arguments and the companion functions that name the IDE removed, and the two
  constants public at the top level (R3)
- [x] Write the new `SpeechService.kt` as the approach says, with the constructor without arguments, the companion
  functions and their comments carried over unchanged (R3)
- [x] Change the plugin's callers to `service<SpeechService>().reading` and to the generic types, as the approach says
  (R3)
- [x] `SpeechStandIns` builds `reading` as a `ReadingAloud<ReadingTarget>` in place of `service`. `git mv`
  `SpeechServiceTest.kt` to `ReadingAloudTest.kt`, `HeardReadingServiceTest.kt` to `ReadingAloudHeardTest.kt` and
  `ReadingTimeServiceTest.kt` to `ReadingAloudTimeTest.kt`, rename their classes, and take `closingTheProjectStopsIts
  ReadingAndNoOther` out of `ReadingAloudTest` into a new `SpeechServiceTest` that builds
  `SpeechService(standIns.reading)`, with the `ApplicationRule` it needs. Change the tests from `standIns.service` to
  `standIns.reading`, and `fileClosed` to `forget`; `projectClosed` in `ReadingPanelTest` and `ModelDownloadFlowTest`
  is called on `SpeechService(standIns.reading)` (R3)
- [x] Run `./gradlew test`; every test of the baseline passes under its class, the three renamed ones under their new
  names and the moved test under `SpeechServiceTest` (R3, R6)
- [x] Commit: the reading as `ReadingAloud`, and `SpeechService` as the service that hands it the parts of the IDE

### The tests of the reading without the platform

- [x] Make `SpeechStandIns` generic, `SpeechStandIns<Target : Any>`, with `ReadingTimeReport<Target>` and `failures`
  of `Target`, and change the plugin's tests to `SpeechStandIns<ReadingTarget>()` (R4, R6)
- [x] In `ReadingAloudTest`, `ReadingAloudHeardTest`, `ReadingAloudTimeTest`, `PartOfTheFileTest`,
  `SpokenMarkdownTest` and `FirstAudioMeasurement`, read targets that are strings in place of a `ReadingTarget` of a
  `LightVirtualFile`, and remove the `ApplicationRule` and the imports of the platform from them (R4, R6)
- [x] Check with `grep -l "com.intellij\|org.intellij"` that none of the files to be moved to `core`, main, test or test
  fixture, imports the platform (R1)
- [x] Run `./gradlew test`; the results equal those of the commit before (R6)
- [x] Commit: the tests of the reading read targets of their own

### The move to `core`

- [x] `git mv` the main files, the tests and the test fixtures named in the approach to their folders under `core/`,
  set their `package` line to `com.ritenoar.aloudrite.core`, add the imports they need of one another where a test
  fixture or a test now lives in another package than before, and add to the plugin's main and test files the
  explicit imports of what moved, in the order of the imports around them (R1, R2, R5)
- [x] In `EspeakDataTest`, set the main class of the probe process to
  `com.ritenoar.aloudrite.core.PhonemesProbeProcessKt` (R6)
- [x] Move from `build.gradle.kts` to `core/build.gradle.kts`, unchanged with their comments: `sherpaOnnxVersion`,
  `piperPhonemizeVersion`, `commonmarkVersion`, `publishedPiperPhonemizeJvm`, `stripPiperPhonemizeJvm`, the
  dependencies of sherpa-onnx, piper-phonemize and commonmark as `implementation`, ONNX Runtime as
  `testImplementation`, and the `jvmArgumentProviders` of the test task with its comment. The root keeps the platform,
  the archive's size check, `prepareSandbox` and `runIde` (R1, R2)
- [x] Run `./gradlew test`, with both models and the aligner model in place. Every test case of the baseline passes,
  under its class in `com.ritenoar.aloudrite.core` where its class moved and under its new name where it was renamed,
  `EspeakPhonemesTest`, `EspeakDataTest`, `KokoroSpeechEngineTest`, `KokoroVoicesTest` and `RealAlignerTest` ran and
  none of them was skipped, and the counts of tests and skipped tests across `build/test-results/test/` and
  `core/build/test-results/test/` equal those of the baseline, the skipped ones being those of the baseline (R6)
- [x] Commit: the speech core moved to the module `core`

### The test of requirement 4

- [ ] Write `ReadingAloudWithoutThePlatformTest` as the approach says, run `./gradlew :core:test --tests
  com.ritenoar.aloudrite.core.ReadingAloudWithoutThePlatformTest` until it passes (R4)
- [ ] Commit: a test that reads a Markdown text through `ReadingAloud` without the platform

### The documents

- [ ] In `README.md`, add under "Building" a part on the two modules: that `core`, in `core/`, holds the reading of
  Markdown aloud from the Markdown to the audio played, with what requirement 2 lists, in the package
  `com.ritenoar.aloudrite.core`, that it has no dependency on the IntelliJ Platform and expects the Kotlin standard
  library from its user, which the IDE is for the plugin, that the plugin adds the actions, the tool window, the
  settings, the highlights in the editor and the preview, and the download of the models, and that
  `./gradlew :core:build` builds `core` alone into `core/build/libs/aloudrite-core-0.0.1.jar` and
  `./gradlew :core:test` tests it alone. Change "Testing" to say that `./gradlew test` runs the tests of both modules,
  those of `core` without the platform and those of the plugin in the test environment of the platform (R7)
- [ ] In `README.md` and in the comments of the six measurements, change each command to
  `./gradlew :core:cleanTest :core:test --tests com.ritenoar.aloudrite.core.<Measurement>`, with the variable before
  it as it stands (R7)
- [ ] Run `git grep -n "com.ritenoar.aloudrite\.[A-Z]"` outside `changes/` and check that every class it names is in
  that package (R5, R7)
- [ ] Commit: the README describes the two modules

## Verification

- [ ] `./gradlew :core:dependencies` for `compileClasspath`, `runtimeClasspath`, `testCompileClasspath`,
  `testRuntimeClasspath`, `testFixturesCompileClasspath` and `testFixturesRuntimeClasspath` names no artifact of the
  groups `com.jetbrains.intellij.*` or `com.jetbrains`, nor any with `intellij` or `idea` in its name; record in
  `record.md` the artifacts they name (R1)
- [ ] `./gradlew buildPlugin` passes its size check; the listing of `lib/` of the archive equals that of the baseline
  with `aloudrite-core-0.0.1.jar` added and the plugin's own jar smaller; record both sizes (R1, R6)
- [ ] `./gradlew test` with the models in place passes, with the counts recorded as in the baseline (R6)
- [ ] `git grep -l "^package com.ritenoar.aloudrite.core" -- core` lists every Kotlin file under `core/`, and no file
  under `src/` has that package (R5)
- [ ] `git log --stat` of the unit's commits shows the move commit as renames only, apart from the build scripts and
  the lines of package, imports and the probe's class name; record the similarity that `git show -M --stat` gives
  for the moved files (R8)
- [ ] `npm run check-changes` passes

## Commit Points

- [x] The plan and its record entry
- [x] The module `core`, empty
- [x] `PieceAligning` takes all its parts from its caller
- [x] The reading as `ReadingAloud`, and `SpeechService` as the service that hands it the parts of the IDE
- [x] The tests of the reading read targets of their own
- [x] The speech core moved to the module `core`
- [ ] A test that reads a Markdown text through `ReadingAloud` without the platform
- [ ] The README describes the two modules
- [ ] The record of the work
