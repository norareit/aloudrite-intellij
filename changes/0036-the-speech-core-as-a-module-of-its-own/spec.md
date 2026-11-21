# The speech core as a module of its own

## Context

The human may want to read Markdown aloud in another application, which may not be written in Kotlin, and asked how
well the plugin is built for taking that part out. The plugin is one Gradle module with one package,
`com.ritenoar.aloudrite`. On `master` at `27d1d17` its 61 source files fall in two groups:

* About half import no class of the IntelliJ Platform. They turn Markdown into the words and silences that are read,
  with the range of the file each comes from (`SpokenMarkdown`, `PlacedPart`, `FrontMatter`, `SpokenPart`), choose
  what a press reads from the caret and the selection given as offsets (`PartOfTheFile`), cut the text into pieces
  (`TextPieces`, `Sentences`), make the phonemes (`EspeakPhonemes`, `PhonemeRows`, `EngineText`), drive the engine
  (`SpeechEngine`, `KokoroSpeechEngine`, `KokoroModel`, `SpeechModel`, `Voice`, `ReadingSpeed`, `EngineThreads`,
  `SpeechPlatform`), make audio ahead and play it (`AudioAhead`, `PlayedAudio`, `AudioPlayer`, `SamplePlayer`,
  `PcmAudio`), and say which word is heard and how long the reading takes (`ReadingSegments`, `HeardWordEstimate`,
  `ReadingTime`)
* The rest is the plugin in the IDE: actions, the tool window, settings, highlights in the editor and the preview, and
  the download of the model, which uses `HttpRequests`, `Decompressor` and `ProgressIndicator` of the platform

The reading itself, making audio ahead, playing it, pause, resume, stop and the change of the speed, is in
`SpeechService`, 840 lines. It takes the parts of the IDE as functions from its constructor already: the thread pool,
the scheduler, the settings, the reports of failures and the notice of a change. It still is an IntelliJ service: it
implements `Disposable`, its constructor without arguments reaches the platform, and it knows what is read as a
`ReadingTarget`, which holds a `Project` and a `VirtualFile`. So nothing in the build keeps a class of the platform
out of the part that does not need it, and the loop that every other application needs is bound to the IDE by its
type.

The human decided to have the part that reads Markdown aloud made a module of its own with no dependency on the
IntelliJ Platform, now, before the language of a second application is known.

## Requirements

1. The build has a module `core` beside the plugin. No artifact of the IntelliJ Platform is on the compile or the
   runtime classpath of `core`, nor of its tests, and the plugin depends on `core`. The plugin archive holds `core` as
   a jar of its own.
2. `core` holds the turning of Markdown into words and silences with their ranges in the file, the choice of what is
   read from offsets of the caret and the selection, the cutting into pieces, the phonemes, the engine and its models
   and voices, the reading speed, the audio made ahead, played and resumed, the estimate of the word heard and the
   reading time, and the reading of requirement 3.
3. The reading of a text, from its spoken parts to the end of its audio, with pause, resume, stop, the change of the
   speed and of the model, the state of what is heard and the times, is a class of `core`. It knows what is read only
   as a value of a type its caller chooses, and it takes the thread pool, the scheduler, the settings, the reports and
   the notice of a change from its caller, as `SpeechService` takes them now. The IntelliJ service of the plugin hands
   it those parts and holds no reading logic of its own.
4. A test of `core` reads a Markdown text through the class of requirement 3 to its end, with stand-ins for the engine
   and the player, pauses and resumes it, and runs with no class of the IntelliJ Platform on its classpath.
5. The code of `core` is in the package `com.ritenoar.aloudrite.core`. The plugin keeps `com.ritenoar.aloudrite`.
6. What the plugin does does not change. The tests of the logic that moves to `core` move with it and run there. Every
   test of the plugin, the tests with the real engine among them, run with the model in place, and not skipped,
   passes, and `./gradlew test` runs the tests of both modules. The plugin archive stays within the limit of 30 MB of
   unit 0004.
7. `README.md` describes the two modules: what `core` holds, that it has no dependency on the IntelliJ Platform, what
   the plugin adds to it, and how `core` is built and tested alone.
8. The work is committed in commits that each either move code or change how it is written, and none that does both,
   so that a reviewer can check each move for keeping the behavior as it was.

## Decisions

- **A module of the build, not a library of its own.** The request is that the part can be taken out. A module with a
  classpath of its own makes the boundary a rule the build checks, which a package does not. Rejected: a package only,
  which the compiler does not stop from importing the platform; and a library published to a Maven repository or a
  repository of its own, which has no second user yet, see item 1 of "Beyond the request".
- **On the JVM, in Kotlin, before a port is chosen.** The second application and its language are not known. The
  boundary, the class of requirement 3 and the tests on plain values are what any later step builds on: a JVM
  application uses `core` as it is, and a port to another language follows the same boundary and can take its tests
  as the cases to match. Rejected: porting to TypeScript now, which the human did not ask for, and whose engine and
  audio depend on where the second application runs.
- **The thing read as a type of the caller's choosing.** The plugin keeps `ReadingTarget` and the equality it relies
  on, the same file in another project being another reading, without `core` knowing a project or a file. Rejected: a
  string that names what is read, which would make the plugin keep a map from strings back to its targets.
- **The download of the model stays in the plugin.** It is built on the HTTP client, the unpacker and the progress of
  the platform, and moving it means replacing all three. Another application needs a model folder, and can be given
  one, see item 2 of "Beyond the request".
- **The highlight in the editor and the preview stays in the plugin**, with `PreviewText`, `PreviewPlaces`,
  `TextEdits` and `Following`. They serve the editor and the preview of the IDE, and `core` gives the ranges of the
  file that any other application would highlight.
- **The package `com.ritenoar.aloudrite.core`.** A reader of the plugin sees from an import which side a class is on,
  and a split package across two jars is avoided. Decided by the specifier and open to the human.

## Non-goals

- A port of any part to TypeScript or another language.
- A second application, a command line program or a server that uses `core`.
- An interface of `core` kept stable for users outside this repository, with versions of its own.
- Any change to what the plugin does, how it sounds or what it shows.
- Other platforms than the two the plugin supports.

## Beyond the request

1. Publish `core` as a library to a Maven repository with a version of its own. It costs the publishing setup, the
   signing and a versioning rule. Without it, another JVM application takes `core` from this repository as a jar or as
   an included build, which is cheap while there is one such application.
2. Move the download and installation of the model to `core`, on the HTTP client of Java and an unpacker of bzip2 and
   tar. It costs a dependency for the unpacking, a progress reported without the platform, and the tests of the
   download moved and changed. Without it, another application downloads and unpacks the model itself, as the README
   describes, or is pointed to the folder the plugin installs.
