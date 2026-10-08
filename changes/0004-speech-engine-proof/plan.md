# Speech engine proof of concept

## Approach

The plugin gets three jars of sherpa-onnx 1.13.8 as dependencies: the Java binding and the native libraries for Linux
x64 and for macOS on Apple Silicon. The build puts them in the `lib/` folder of the plugin archive, next to the
plugin's own jar, and the binding loads the native library of the running platform out of its jar by itself. The
"Read Aloud" button of unit 0003 stays where it is and is shown when it was shown before. What changes is what a
press does: the action checks the platform, then checks that the model is in its folder, and only then hands the
test sentence to an application service that makes the audio on a background thread and plays it through Java Sound,
which is part of the JDK the IDE runs on. The dialog that showed the content of the file is deleted, with the tests
that asserted it, and the plugin registers no action besides the button.

This plan is written against the branch `feature-0004-speech-engine-proof` at `3b10a03`, which holds unit 0003: the
action `ReadAloudAction`, its dialog `FileContentDialog`, the eight tests of `ReadAloudActionTest`, and the Markdown
plugin as a dependency of the tests only. It replaces an earlier plan of the same day, written against a tree without
unit 0003 and for a spec that started the sentence from an action in the Tools menu. `master` is two commits ahead of
the base of the branch, `4b68502` and `0026d5b`. They touch only `changes/0005-model-download/` and
`changes/0006-read-markdown-aloud/`, which this unit does not edit, so the branch is left as it is and nothing here
depends on them.

### Versions and artifacts

Checked on 2026-10-08 against the release `v1.13.8` and the release `tts-models` of `k2-fsa/sherpa-onnx` on GitHub.
Use them as written, and log it in `record.md` when one of them turns out not to work.

| What                      | File                                            | Bytes       |
|---------------------------|-------------------------------------------------|-------------|
| Java binding              | `sherpa-onnx-jvm-1.13.8.jar`                    | 187,490     |
| Native library, Linux x64 | `sherpa-onnx-native-lib-linux-x64-1.13.8.jar`   | 10,515,871  |
| Native library, macOS ARM | `sherpa-onnx-native-lib-osx-aarch64-1.13.8.jar` | 9,460,832   |
| Model package             | `kokoro-int8-multi-lang-v1_0.tar.bz2`           | 132,303,094 |
| License text              | `gpl-3.0.txt`                                   | 35,149      |

Their SHA-256 checksums, in the order of the table:

* `77b7b047fade4eadada96b568eb92615049aaf1dc317c7244e46c1ea38b9a63b`
* `30c93b59381113f9c20aedbbf9fc1ad399158f6bc03dddc0f8934a6e28e069ba`
* `42e272180c8836127f024f3335d7afcdfb30fe0164b78330d5d449034e34ce34`
* `4c3052abaa60943a341f193888cf6abd68787dae6ab8ae5c925a706caa247e4e`
* `3972dc9744f6499f0f9b2dbf76696f2ae7ad8af9b23dde66d6af86c9dfb36986`

The jars are at `https://github.com/k2-fsa/sherpa-onnx/releases/download/v1.13.8/<file>` and the model package is at
`https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-int8-multi-lang-v1_0.tar.bz2`. The license
text is at `https://www.gnu.org/licenses/gpl-3.0.txt`. The model package was last replaced by its publisher on
2026-09-08, so its address does not fix its content, and the checksum above is the one this plan was tried with.

### What the first trial established

A throwaway trial, made in a scratch directory outside the repository and deleted afterwards, ran the three jars
and the unpacked model package on the developer's Linux machine, first in a plain JDK 25 and then in a copy of this
repository under `./gradlew buildPlugin test`. That copy was of the tree before unit 0003. Nothing below depends on
how the sentence is started, and unit 0003 changed nothing these findings rest on, so they were not run again.

* **The jars are not on Maven Central.** They are assets of the GitHub release, so the build declares that release
  as an Ivy repository of its own. With the declaration of the build task below, Gradle resolved all three. The
  repackaging `net.dreamlu:mica-sherpa-onnx` on Maven Central was rejected: it is a third party's, and it holds the
  native libraries of five platforms.
* **The archive came to 20,036,743 bytes**, with `aloudrite-intellij/lib/` holding the plugin's jar and the three
  jars of the table, and `LICENSE` and `THIRD-PARTY.md` directly under `aloudrite-intellij/`. The plugin's own jar
  is a few kilobytes of that and differs by as little in the current tree.
* **Each native jar holds two libraries**, `libsherpa-onnx-jni` and `libonnxruntime`, under
  `sherpa-onnx/native/<platform>/`. ONNX Runtime is therefore shipped as well, inside those jars, and espeak-ng is
  linked into `libsherpa-onnx-jni`.
* **The binding loads the native library by itself**, the first time one of its classes that needs it is created.
  It finds the two libraries as resources next to its own classes, copies them to a new directory
  `sherpa-onnx-java<number>` under `java.io.tmpdir`, and loads them from there. The plugin calls nothing to make
  that happen. Two things follow that this unit accepts and does not repair: each run leaves an empty directory
  behind in the temporary folder, and a process that is killed leaves the two libraries there, 31 MB.
* **The model package unpacks to one directory, `kokoro-int8-multi-lang-v1_0/`**, of 189,882,497 bytes in 377
  files. The engine was given five of its entries and needed no other: `model.int8.onnx`, `voices.bin`,
  `tokens.txt`, `lexicon-us-en.txt` and the directory `espeak-ng-data`.
* **The voice `af_heart` is speaker 3.** The metadata inside `model.int8.onnx` lists 54 speakers and maps
  `3->af_heart`, and gives a sample rate of 24,000 Hz. The binding takes the speaker as a number and has no lookup
  by name.
* **The test sentence comes out as 3.67 seconds of audio**, in two pieces, one per sentence, of 31,544 and 56,523
  samples. With 2 threads the engine was created in 0.67 s, the first piece arrived 0.64 s after asking and the
  second after 1.66 s. With 1 thread that was 0.78 s and 2.14 s, with 4 threads 0.59 s and 1.50 s. These are the
  planner's figures from a plain JVM and are not the measurements of requirement 13.
* **Some mistakes end the process and others throw.** With a model file that is absent the binding throws
  `IllegalArgumentException`, and with one cut off after 1 MB it throws `RuntimeException`. With no lexicon given
  for this multi-language model, the native code ends the whole process with exit status 255, which inside an IDE
  would close the IDE without a word. The plugin therefore always gives the lexicon, and never creates the engine
  before it has seen that all five entries exist.
* **Kotlin cannot choose between two overloads of `generateWithCallback`** when it is given a lambda: one takes an
  `OfflineTtsCallback` and the other a `Consumer<float[]>`. The call names the type, as
  `OfflineTtsCallback { samples -> ...; 1 }`. The callback returns 1 to go on.
* **JDK 25 prints a warning** on the standard error stream that `System.load` is a restricted method, and the engine
  prints its configuration there. Neither goes through the IDE's logger, so neither is an error report.
* **In the test task, `user.home` is the developer's own home directory**, and a test that created the engine
  and made the sentence passed there, next to a test under the `ApplicationRule`. A JUnit 4 assumption that fails
  is reported by Gradle as skipped.
* **Java Sound opened an output line of 24,000 Hz, 16 bits, mono** on the developer's Linux machine. Nothing was
  written to it, so nothing was heard.

### What a second trial established

The part that did change, the button as the trigger, was tried on 2026-10-08 in a copy of the tree at `3b10a03`,
made in a scratch directory outside the repository and deleted afterwards, without sherpa-onnx in it. The action had
the constructor of this plan and showed its messages as this plan says. Under `./gradlew test`, four tests passed:

* **An action made by the test with its own arguments can be pressed** like the registered one, with
  `ActionUtil.performAction` and the event `ReadAloudActionTest` already builds for a Markdown file. The registered
  action is not needed for the tests of requirements 8 and 9.
* **A message shown with `Messages.showInfoMessage` reaches the `TestDialog`** set through `TestDialogManager`,
  under the `ApplicationRule` and with an event that holds no project. That is how the test of unit 0001 read the
  greeting, and it still holds beside the Markdown plugin.
* **`ActionManager.getActionIdList("com.ritenoar.aloudrite.")` gives exactly `com.ritenoar.aloudrite.ReadAloud`**,
  which is what the test that the plugin adds no other action asserts.
* **A function handed to the action in place of the speech service** received the test sentence once per press, for
  a file with text and for an empty one, with no message shown.

Not established by either trial, and left to the checks in an IDE: that the binding finds its native libraries
through the class loader of a plugin, that anything is audible, how it sounds, and all of it on macOS. The two
libraries for macOS carry a code signature and find each other through `@loader_path`, which is all that could be
read from Linux.

### Decisions the spec leaves to the plan

* **The model folder.** On macOS it is `~/Library/Application Support/AloudRite/kokoro-int8-multi-lang-v1_0`. On
  Linux it is `$XDG_DATA_HOME/aloudrite/kokoro-int8-multi-lang-v1_0` where `XDG_DATA_HOME` is set to an absolute
  path, and `~/.local/share/aloudrite/kokoro-int8-multi-lang-v1_0` otherwise, as the XDG Base Directory
  Specification says. The folder is the directory the package unpacks to, so unpacking the package into the
  `aloudrite` or `AloudRite` directory puts the model in place, and a later model gets a folder of its own.
* **"In place" means the five entries exist**: the four files and the directory named above. It is no check of
  their content, which the spec leaves to unit 0005.
* **Which platform.** Speech is supported where `os.name` starts with `Linux` and `os.arch` is `amd64` or
  `x86_64`, and where `os.name` starts with `Mac` and `os.arch` is `aarch64` or `arm64`. Those are the values the
  binding itself maps to the two native jars this plugin ships.
* **Order of the checks.** The platform first, then the model, then the engine. On an unsupported platform no class
  of sherpa-onnx is ever touched, which is what lets the plugin load there, and a machine that has neither a
  supported platform nor the model is told about the platform.
* **When the button is shown does not change.** `update` of `ReadAloudAction`, its registration in `plugin.xml` and
  its icons stay as unit 0003 left them. The platform and the model are looked at when the button is pressed and
  never when it is drawn, so the button is the same on every platform.
* **A press no longer reads the file.** `actionPerformed` takes neither the file nor its document, because the
  sentence is the same whatever the file holds. Unit 0006 brings the text back.
* **The dialog is deleted, not kept unused.** `FileContentDialog.kt` goes, and with it the three tests of
  `ReadAloudActionTest` that assert it and the helpers only they use. The five other tests of unit 0003 stay as
  they are: the Markdown file type, the place, text and icon of the action, the button being offered for a
  Markdown file and not for another, and "Say Hello" being gone.
* **The two messages** are shown with `Messages.showInfoMessage`, the project of the event and the title
  `AloudRite`:
    * `Speech is not supported on this platform. AloudRite speaks on Linux x64 and on macOS on Apple Silicon.`
    * `The speech model is missing. AloudRite looks for it in <folder>. The README of AloudRite says which model
      package to get and how to put it there.`, with the absolute path of the model folder for `<folder>`
* **A second press while one is running is ignored.** Nothing is queued and nothing is shown, and the button looks
  the same while the sentence plays. Stopping is a non-goal, so the sentence that is being spoken ends by itself.
* **The audio is played piece by piece as the engine delivers it**, and not after the whole sentence is made. It
  is the same amount of code, and it makes the time to the first sound, which requirement 13 asks for, the time
  unit 0006 can expect.
* **The engine is created at the first press and kept** until the IDE closes, so that the second press of
  requirement 13 shows what a press costs without loading the model.
* **The engine runs on 2 threads**, which leaves the rest of the machine to the IDE.
* **The time to the first sound is written to the IDE's log**, as one line at level INFO each time the button
  speaks: the milliseconds from the start of `actionPerformed` to the first write to the audio line. That is where
  requirements 13 and 14 are read from. It is the moment the audio is handed to the operating system, which is as
  near to the sound as the plugin can see.
* **A failure of the engine or of the audio output is shown as a message**, with the text of the exception, and
  is logged as a warning. A warning is not an error report of the IDE, and an error would be one.
* **The action is handed what speaks.** `ReadAloudAction` gets the operating system name, the architecture name,
  the model folder and a function that speaks, and its constructor without arguments, which the platform uses,
  fills them in from the running machine and the speech service. It is what lets the tests press the button for
  another platform, for a folder of their own, and without an engine or an audio device.
* **The archive limit is 30,000,000 bytes**, checked by the build itself at the end of `buildPlugin`, so that a
  model or a third platform in the archive fails the build.
* **The list of third-party software is `THIRD-PARTY.md`** in the repository root.
* **Names.** No class is named after the test sentence, which is a constant of `ReadAloudAction.kt` and leaves
  with unit 0006. No parameter or variable has a one-letter name, which the human asked for in the record of unit
  0003.

### Where the work is done

* **Build on the host.** The build fetches the three jars from `github.com`, which the agent sandbox does not
  admit: `agent/compose.project.yml` lists no host at all, and unit 0002, which would let the plugin build there,
  is a draft. Extending the allowlist traces to no requirement.
* **The model on the developer's Linux machine.** On 2026-10-08 `~/.local/share/aloudrite` did not exist there and
  `XDG_DATA_HOME` was not set. The test of requirement 12 is skipped until the model is in its folder. Putting it
  there writes 190 MB into the developer's home directory, outside the repository, so the implementer asks the
  human before doing it, or the human does it. Either way it is done with the commands of the README as written,
  which checks them.
* **What needs ears or a Mac** is the human's, as the spec says, and is under Verification.
* **The developer's Gradle daemon is left running.** No task here needs `./gradlew --stop`.

## Files

- `build.gradle.kts`: the Ivy repository for the release assets of sherpa-onnx, the three dependencies, `LICENSE`
  and `THIRD-PARTY.md` in the plugin archive, and the limit on the size of the archive
- `LICENSE`: new, the text of the GNU General Public License version 3
- `THIRD-PARTY.md`: new, the third-party software the plugin ships or uses, with the license of each
- `src/main/kotlin/com/ritenoar/aloudrite/SpeechPlatform.kt`: new, whether speech is supported for an operating
  system name and an architecture name
- `src/main/kotlin/com/ritenoar/aloudrite/KokoroModel.kt`: new, the name of the model package, the five entries
  the engine needs, where the model folder is, and whether the model is in place
- `src/main/kotlin/com/ritenoar/aloudrite/PcmAudio.kt`: new, the conversion of the engine's samples to 16-bit PCM
- `src/main/kotlin/com/ritenoar/aloudrite/OneAtATime.kt`: new, the guard that lets one piece of work run at a time
- `src/main/kotlin/com/ritenoar/aloudrite/KokoroSpeechEngine.kt`: new, the only file that names a class of
  sherpa-onnx
- `src/main/kotlin/com/ritenoar/aloudrite/AudioPlayer.kt`: new, the output line of Java Sound
- `src/main/kotlin/com/ritenoar/aloudrite/SpeechService.kt`: new, the application service that speaks in the
  background
- `src/main/kotlin/com/ritenoar/aloudrite/ReadAloudAction.kt`: changed, a press speaks the test sentence or shows
  one of the two messages, and the test sentence is a constant here
- `src/main/kotlin/com/ritenoar/aloudrite/FileContentDialog.kt`: deleted
- `src/main/resources/META-INF/plugin.xml`: not changed. It is named here because the plan depends on that: one
  action, one `<depends>`
- `src/test/kotlin/com/ritenoar/aloudrite/SpeechPlatformTest.kt`: new
- `src/test/kotlin/com/ritenoar/aloudrite/KokoroModelTest.kt`: new
- `src/test/kotlin/com/ritenoar/aloudrite/KokoroModelStandIn.kt`: new, the one place the tests make a folder that
  counts as a model in place
- `src/test/kotlin/com/ritenoar/aloudrite/PcmAudioTest.kt`: new
- `src/test/kotlin/com/ritenoar/aloudrite/OneAtATimeTest.kt`: new
- `src/test/kotlin/com/ritenoar/aloudrite/KokoroSpeechEngineTest.kt`: new
- `src/test/kotlin/com/ritenoar/aloudrite/ReadAloudActionTest.kt`: changed, the three tests of the dialog go and
  four tests of the press come
- `README.md`: the license, the model and its folder, what the button does, the platforms, and what no longer holds
- `changes/0004-speech-engine-proof/record.md`: the log, the measurements and the human's findings

## Tasks

- [x] Write `LICENSE` in the repository root with the text of `https://www.gnu.org/licenses/gpl-3.0.txt`,
      unchanged: 674 lines, 35,149 bytes, SHA-256
      `3972dc9744f6499f0f9b2dbf76696f2ae7ad8af9b23dde66d6af86c9dfb36986` (R15)
- [x] Write `THIRD-PARTY.md`. It has one entry for each piece of third-party software the plugin ships in its
      archive or uses at run time, and each entry gives the name, the version, where it comes from, its license by
      SPDX identifier, and whether it is in the archive or fetched by the user. It holds at the least: sherpa-onnx
      1.13.8 (Apache-2.0, in the archive), ONNX Runtime in the version `VersionInfo.getOnnxruntimeVersion()` of
      the binding reports (MIT, in the archive, inside the native jars), espeak-ng (GPL-3.0-or-later, in the
      archive, linked into `libsherpa-onnx-jni`), and the Kokoro v1.0 model package of the table above (fetched by
      the user, with the license its own `LICENSE` file states and a note that the package holds the data of
      espeak-ng as well). Beyond those four, read the `cmake/` directory of sherpa-onnx at the tag `v1.13.8` and
      add every library that the build of the JNI library with text-to-speech links in, each with the license its
      own repository states. Leave out what that build does not link, and state no license that was not read at
      its source. The file opens with one sentence saying that the plugin itself is under GPL-3.0-or-later and
      points to `LICENSE` (R16)
- [x] In `build.gradle.kts`, add to `repositories` an `exclusiveContent` block whose repository is
      `ivy { url = uri("https://github.com/k2-fsa/sherpa-onnx/releases/download") }` with
      `patternLayout { artifact("v[revision]/[artifact]-[revision].[ext]") }` and
      `metadataSources { artifact() }`, and whose filter is `includeGroup("com.k2fsa.sherpa.onnx")`. Add the
      version `1.13.8` once, as a value, and three `implementation` dependencies of that group and version:
      `sherpa-onnx-jvm`, `sherpa-onnx-native-lib-linux-x64` and `sherpa-onnx-native-lib-osx-aarch64`. No other
      native jar is added, and the `testBundledPlugin("org.intellij.plugins.markdown")` of unit 0003 stays (R1, R9)
- [x] In `build.gradle.kts`, make `prepareSandbox` copy `LICENSE` and `THIRD-PARTY.md` from the repository root
      into the plugin's directory, as `from(layout.projectDirectory.files("LICENSE", "THIRD-PARTY.md")) {
      into(pluginName) }`, so that the archive holds both directly under `aloudrite-intellij/` (R16)
- [x] In `build.gradle.kts`, make `buildPlugin` fail in a `doLast` when its archive is larger than 30,000,000
      bytes, with a message that states the size found and the limit. Run `./gradlew clean buildPlugin`, see it
      succeed, and see with `unzip -l` that the archive holds the plugin's jar, the three jars of sherpa-onnx,
      `LICENSE` and `THIRD-PARTY.md`, and nothing else. Then lower the limit to 1,000,000 for one run, see the
      build fail with that message, and put the limit back. Run `./gradlew test` and see the eight tests of unit
      0003 pass as before (R1, R2, R3)
- [x] Write `SpeechPlatformTest`, a plain JUnit 4 test of a function `isSpeechSupported(operatingSystemName,
      architectureName)`: true for `Linux` with `amd64`, `Linux` with `x86_64`, `Mac OS X` with `aarch64` and
      `Mac OS X` with `arm64`; false for `Linux` with `aarch64`, `Mac OS X` with `x86_64`, `Windows 11` with
      `amd64` and `FreeBSD` with `amd64`. Then write the function in `SpeechPlatform.kt`, a pure function of its
      two arguments, and see the test pass (R1, R9)
- [x] Write `KokoroModelStandIn.kt` in the test sources, with one function that fills a given folder with empty
      stand-ins for the five entries of a model in place, taking their names from the constants of
      `KokoroModel.kt`. Write `KokoroModelTest`, a plain JUnit 4 test, for two functions.
      `userModelFolder(operatingSystemName, userHome, xdgDataHome)` gives
      `<userHome>/Library/Application Support/AloudRite/kokoro-int8-multi-lang-v1_0` for `Mac OS X` whatever
      `xdgDataHome` is; `<xdgDataHome>/aloudrite/kokoro-int8-multi-lang-v1_0` for `Linux` with an absolute
      `xdgDataHome`; and `<userHome>/.local/share/aloudrite/kokoro-int8-multi-lang-v1_0` for `Linux` where
      `xdgDataHome` is null, empty or a relative path. `isModelInPlace(modelFolder)` is false for a folder that
      does not exist, false for an empty folder, false for each of the five cases in which exactly one of
      `model.int8.onnx`, `voices.bin`, `tokens.txt`, `lexicon-us-en.txt` and the directory `espeak-ng-data` is
      absent, and true for a folder under a `TemporaryFolder` that the stand-in function has filled. The test
      names the five entries itself, as text, so that a constant that is wrong fails it. Then write
      `KokoroModel.kt` with the package name and the five entry names as constants, stated once, and the two
      functions. `userModelFolder` is pure and touches no file; `isModelInPlace` only looks whether the entries
      exist. See the test pass (R4, R8)
- [x] Write `PcmAudioTest`, a plain JUnit 4 test of `toPcm16LittleEndian(samples)`: the result is twice as long
      as the input; `0f` gives the bytes `00 00`; `1f` gives `FF 7F`, which is 32,767; `-1f` gives `01 80`, which
      is -32,767; `2f` and `-2f` give the same as `1f` and `-1f`; and the input array is not changed. Then write
      the function in `PcmAudio.kt`: each sample is limited to the range -1 to 1, multiplied by 32,767, rounded,
      and written low byte first. See the test pass (R5)
- [x] Write `OneAtATimeTest`, a plain JUnit 4 test of a class `OneAtATime`. Its constructor takes the function
      that runs a piece of work in the background, and the test hands it one that keeps the work without running
      it, so that the test decides when the work runs and needs no thread. `tryRun(work)` returns true and hands
      the work to that function when nothing is running, and `isRunning` is true from then until the work has
      ended. A second `tryRun` in that time returns false and its work is never handed over. After the first work
      has been run to its end, `isRunning` is false and `tryRun` returns true again. After a work that throws, and
      after a background function that itself throws, `isRunning` is false and the next `tryRun` returns true.
      Then write `OneAtATime.kt`, with the state in an `AtomicBoolean`, and see the test pass (R7)
- [x] Write `KokoroSpeechEngineTest`, a plain JUnit 4 test with one test. It takes the model folder from
      `userModelFolder` with the `os.name`, `user.home` and `XDG_DATA_HOME` of the running machine, and opens with
      two assumptions, `assumeTrue` on `isSpeechSupported` for the running machine and `assumeTrue` on
      `isModelInPlace` for that folder, so that it is reported as skipped where either does not hold. It then
      creates a `KokoroSpeechEngine` for the folder, makes the audio of a text of its own, the two sentences of
      requirement 5 written out, adds up the samples of the pieces it is handed, closes the engine in a `finally`,
      and asserts that the samples divided by the engine's sample rate lie between 2.0 and 15.0 seconds. It opens
      no audio line. Run `./gradlew test` and see it fail to compile, because the engine does not exist (R12)
- [x] Write `KokoroSpeechEngine.kt`, the only file that imports from `com.k2fsa.sherpa.onnx`. The class takes the
      model folder, builds an `OfflineTtsKokoroModelConfig` from the five entries of `KokoroModel.kt` with
      `setModel`, `setVoices`, `setTokens`, `setLexicon` and `setDataDir`, an `OfflineTtsModelConfig` with 2
      threads, and creates the `OfflineTts`. It offers the sample rate the engine reports, a function that makes
      the audio of a text and hands each piece of samples to a function of the caller as it arrives, through
      `generateWithCallback(text, 3, 1.0f, OfflineTtsCallback { ... })`, and a `close` that calls `release`.
      Speaker 3 and speed 1.0 are constants with the names `af_heart` and normal speed in them, and a comment
      says that the number comes from the `id2speaker` metadata of the model, because the binding has no lookup
      by name. A second comment says why the lexicon is always given: without it the native code ends the
      process. The engine reads the model from that folder alone: nothing in the plugin fetches a file or opens a
      connection, and a model that is absent is reported and never downloaded. Run `./gradlew test` and see
      `KokoroSpeechEngineTest` reported as skipped while the model is not in its folder, and as passed once it is
      (R1, R5, R10, R12)
- [x] Write `AudioPlayer.kt`: a class that opens and starts a `SourceDataLine` of `javax.sound.sampled` for a given
      sample rate, signed 16-bit PCM, mono, little endian; a `play(samples)` that writes
      `toPcm16LittleEndian(samples)` to the line; and a `close` that drains the line and closes it. It uses the
      default output of the machine, which is what `AudioSystem.getSourceDataLine` gives, and names no mixer
      (R1, R5)
- [x] Write `SpeechService.kt`, an application-level light service, `@Service(Service.Level.APP)`, that is
      `Disposable`. Its one function, `speak`, takes the text, the model folder and the moment the button was
      pressed, as a value of `System.nanoTime()`, and returns at once: it hands the work to a `OneAtATime` whose
      background function is `ApplicationManager.getApplication().executeOnPooledThread`, and does nothing more
      when that returns false. The work creates the `KokoroSpeechEngine` if the service has none yet and keeps
      it, opens an `AudioPlayer` at the engine's sample rate, plays each piece as the engine hands it over, and
      closes the player when the engine is done, in a `finally`. At the first piece of each press it logs, at
      level INFO, `First sound <n> ms after the button was pressed`. Anything thrown by the engine or the player
      is caught, logged with `warn` and never with `error`, and shown from the event dispatch thread, through
      `invokeLater`, with `Messages.showErrorDialog` under the title `AloudRite` and the text
      `AloudRite could not speak: <message of the exception>`. `dispose` closes the engine where `isRunning` of
      the guard is false, and leaves it where a sentence is still being made, with a comment that says why:
      releasing an engine that another thread is using is a fault in native code, and the service is disposed
      only when the IDE closes or the plugin is unloaded. Run `./gradlew test` and see every test pass, the eight
      of unit 0003 included, since the button still shows its dialog at this point (R5, R6, R7, R13)
- [ ] Rework `ReadAloudActionTest` for the press, before the action is changed. Remove the three tests that assert
      the dialog, `pressingTheButtonShowsTheTextAsTheEditorHoldsItWithoutSavingIt`,
      `pressingTheButtonOnAnEmptyFileShowsAnEmptyDialog` and `pressingTheButtonOnALongFileShowsAllOfItsText`, and
      what only they use: `pressTheButtonOn` as it is, `ShownDialog`, `markdownFileOnDisk`, `clearInterceptors`
      and the imports that become unused. Leave the five other tests and `eventFor`, `isOfferedFor`,
      `readAloudAction` and `markdownFileType` as they are. Add a helper that makes a
      `ReadAloudAction(operatingSystemName, architectureName, modelFolder, speak)` whose `speak` adds the text it
      is given to a list of the test, and a helper that presses a given action on a given file with
      `ActionUtil.performAction` and the event of `eventFor` on the event dispatch thread, and returns the
      messages shown, captured through `TestDialogManager.setTestDialog` with the previous dialog restored in a
      `finally`. The file pressed on is a `LightVirtualFile` of the Markdown file type. Add the four tests
      below. None of them needs the model or an audio device, and none reaches the engine. Run `./gradlew test`
      and see it fail to compile, because the action has no such constructor (R5, R8, R9, R11)
    - the ids `ActionManager.getActionIdList("com.ritenoar.aloudrite.")` gives are exactly
      `com.ritenoar.aloudrite.ReadAloud`
    - with `Linux`, `amd64` and an empty folder under the `TemporaryFolder`, a press shows exactly one message,
      which contains `model is missing`, the absolute path of that folder and `README`, and nothing is spoken
    - with `Windows 11`, `amd64` and a folder filled by the stand-in function of `KokoroModelStandIn.kt`, the
      button is offered for a Markdown file as `update` leaves it, a press shows exactly the message
      `Speech is not supported on this platform. AloudRite speaks on Linux x64 and on macOS on Apple Silicon.`,
      and nothing is spoken
    - with `Linux`, `amd64` and a filled folder, a press on a Markdown file with text and a press on an empty one
      show no message, and what was spoken is twice the sentence
      `AloudRite can speak. This sentence was made on your own machine.`, which the test writes out
- [ ] Change `ReadAloudAction.kt` and delete `FileContentDialog.kt`. The class gets a primary constructor that
      takes the operating system name, the architecture name, the model folder and the function that speaks, of
      the type `(text: String, modelFolder: Path, pressedAtNanos: Long) -> Unit`, and a constructor without
      arguments, which the platform uses and which passes `os.name`, `os.arch`, the `userModelFolder` of
      `os.name`, `user.home` and the environment variable `XDG_DATA_HOME`, and a function that calls `speak` of
      the `SpeechService`. That function looks the service up when it is called and not when the action is made.
      `getActionUpdateThread`, `update` and `MARKDOWN_FILE_TYPE_NAME` stay as they are. `actionPerformed` takes
      the time, then returns with the first message of the Approach where `isSpeechSupported` is false, then
      returns with the second where `isModelInPlace` is false, and otherwise hands the test sentence, the model
      folder and the time to the function that speaks. It no longer reads the file or its document, and the
      imports of `FileDocumentManager` go. The sentence is a constant of this file,
      `AloudRite can speak. This sentence was made on your own machine.`, with a comment that it is a placeholder
      for the text of the file. `plugin.xml` is not changed. Run `./gradlew test` and see every test pass
      (R5, R8, R9, R11)
- [ ] Rewrite the parts of `README.md` that this unit makes untrue, and add what it brings. The introduction no
      longer says that the plugin reads nothing aloud, and no longer describes the dialog: it says that pressing
      the "Read Aloud" button on a Markdown file speaks one fixed sentence, whatever the file holds, as a
      placeholder for reading the file, on Linux x64 and on macOS on Apple Silicon, and that on any other
      platform the plugin loads, shows the button and says that speech is not supported there. It names the two
      messages a press can show in place of the sentence, and says that a second press while it speaks is
      ignored. The sentence that the plugin depends on nothing beyond the platform itself is replaced by what
      holds now: it requires no other plugin, so it loads in any IDE built on the IntelliJ Platform; the archive
      carries sherpa-onnx with its native libraries for those two platforms, about 20 MB; and nothing has to be
      installed besides the model. The sentence that the button appears only where the IDE's Markdown plugin is
      enabled stays. The "Requirements" section says that the first build also fetches the three jars of
      sherpa-onnx from GitHub. A new section on the model names the package
      `kokoro-int8-multi-lang-v1_0.tar.bz2`, its address, its size of 132 MB packed and 190 MB unpacked and its
      SHA-256, says that it is neither in the plugin nor in the repository, names the model folder on each of
      the two platforms, including the rule for `XDG_DATA_HOME`, and gives for each platform the commands that
      create the directory, fetch the package, check it against the SHA-256 and unpack it there. It says that the
      sandbox IDE, an installed IDE and the tests all use that one copy, and that the test which makes audio is
      skipped while the model is absent. A new section on the license says that the plugin is licensed under
      GPL-3.0-or-later, and why: it ships sherpa-onnx, whose native library contains espeak-ng, which is under
      GPL-3.0-or-later. It points to `LICENSE` and to `THIRD-PARTY.md`. Everything else in the README stays as it
      is (R1, R3, R4, R15, R16, R18)
- [ ] With the human's agreement, put the model in its folder on the developer's Linux machine with the commands
      of the README, exactly as they are written there, and correct the README where they do not work. Check the
      package against the SHA-256 of the table before unpacking. Then run `./gradlew test` and see
      `KokoroSpeechEngineTest` counted as passed and not as skipped (R3, R4, R12)
- [ ] Measure on the developer's Linux machine and write into `record.md`: the size of the archive in bytes, from
      `stat -c %s build/distributions/aloudrite-intellij-0.0.1.zip`; the size of the model folder, from
      `du -sb` on it; and the two times of the line `First sound <n> ms after the button was pressed` in the
      `idea.log` of the sandbox IDE, for the first press after `./gradlew runIde` started the IDE and for the one
      after it, each made after the sentence before it has ended. The log is
      `.intellijPlatform/sandbox/aloudrite-intellij/IU-2026.2.3/log_runIde/idea.log`. Where the implementer cannot
      press the button in a window, the human opens a Markdown file, presses it twice and the implementer reads
      the two lines. State with the figures which machine and how many processor cores (R13)
- [ ] Hand the human what they need for their checks and write their answers into `record.md` as they give them:
      the archive to install on both machines, the commands of the README for the model on macOS, where the IDE
      keeps its log (Help, then Show Log), and the line to look for. The record then holds the human's judgment
      of how the sentence sounds on Linux x64 and on macOS on Apple Silicon, and the two times of the first and
      the second press as the human found them on macOS (R14)
- [ ] Ask the human for the address of the GitHub repository and the day it was made public, and write both into
      `record.md`. Creating the repository and pushing to it is the human's step and no agent does it (R17)
- [ ] Before the status is set to `implemented`, write into `record.md` a list of what this unit established that
      the specs of units 0005 and 0006 rest on, at the least: the model folder on each platform and what "in
      place" means; that the package unpacks to 190 MB; the address and the checksum of the package and that the
      address does not fix its content; that some mistakes in the engine's configuration end the process, so that
      a model has to be complete before the engine sees it; the times measured; that the audio arrives sentence
      by sentence; what the binding leaves in the temporary folder; that the message for a missing model is a
      dialog of the action, shown before the speech service is reached, which is where the question of unit 0005
      goes; and that a press no longer reads the file, that a second press is ignored and not a stop, and that
      the test sentence is a constant of `ReadAloudAction.kt`, which are the three things unit 0006 changes. Ask
      the human to have the specifier revise both specs against that list, on `master` as the process says. The
      revisions `4b68502` and `0026d5b`, which followed the change of the trigger, were made before anything was
      built and are not the revision this asks for. Log in `record.md` that the human has seen the revisions, and
      for each of the two specs whether it changed (R19)

## Verification

- [ ] `./gradlew clean buildPlugin` succeeds, and `./gradlew test` passes. In `ReadAloudActionTest` the five tests
      of unit 0003 that do not assert the dialog are there unchanged and pass, and the three that asserted it are
      gone (R9, R11)
- [ ] With the model in its folder, `build/test-results/test/TEST-com.ritenoar.aloudrite.KokoroSpeechEngineTest.xml`
      reports 1 test, 0 skipped, 0 failures. With the model folder renamed for one run, the same file reports
      1 skipped and `ReadAloudActionTest` still passes. The folder is renamed back (R11, R12)
- [ ] The archive is no larger than 30,000,000 bytes, and `unzip -l build/distributions/aloudrite-intellij-0.0.1.zip`
      lists `aloudrite-intellij/LICENSE`, `aloudrite-intellij/THIRD-PARTY.md`, and under `aloudrite-intellij/lib/`
      the plugin's jar, `sherpa-onnx-jvm-1.13.8.jar`, `sherpa-onnx-native-lib-linux-x64-1.13.8.jar` and
      `sherpa-onnx-native-lib-osx-aarch64-1.13.8.jar`, and nothing else: no model, no voices, no native jar of
      another platform (R1, R2, R3, R16)
- [ ] The three jars of sherpa-onnx in the archive have the SHA-256 checksums of the table in the Approach (R1)
- [ ] `git ls-files` shows no file ending in `.onnx`, `.bin` or `.tar.bz2`, and nothing of the model package (R3)
- [ ] `sha256sum LICENSE` gives the checksum of the table in the Approach (R15)
- [ ] `grep -rn 'com.k2fsa' src/main` finds only lines in `KokoroSpeechEngine.kt`, and
      `grep -rn -E 'java\.net|HttpClient|HttpRequests|URL\(' src/main` finds nothing (R9, R10)
- [ ] `grep -rn 'FileContentDialog' src` finds nothing, `grep -n -i 'dialog' README.md` finds nothing, and
      `git diff master -- src/main/resources` is empty: the registration of the button and its icons are as unit
      0003 left them, and `plugin.xml` holds one `<action>` (R5, R18)
- [ ] The `META-INF/plugin.xml` inside the plugin's jar still holds `com.intellij.modules.platform` as its only
      `<depends>` (R9)
- [ ] In the sandbox IDE started with `./gradlew runIde` on Linux, with the model in place: the "Read Aloud"
      button is in the tab row of a Markdown file as before, and pressing it plays the sentence and opens no
      dialog. The sentence is the same for a second Markdown file with other content. While it is being made and
      played, typing in an editor goes on without a pause and the menus open. Pressing the button again while
      the sentence plays gives no second voice. Tools holds no entry of the plugin. This needs a display and
      ears. Where the implementer has neither, log that in `record.md` and leave the check to the human
      (R5, R6, R7)
- [ ] In the same IDE, with the model folder renamed: pressing the button shows the message that the model is
      missing, with the folder and the README in it, and no error indicator appears in the status bar of the
      IDE. The folder is renamed back (R8)
- [ ] In the same IDE, with the network of the machine switched off: the button speaks as before (R10)
- [ ] The archive installs through "Install Plugin from Disk" in the developer's IDE on Linux x64 and on macOS on
      Apple Silicon, and on both the button plays the sentence. These are the human's checks, made at the review
      (R1, R14)
- [ ] `record.md` holds the three measurements of requirement 13 from Linux, the human's judgment of the sound on
      both platforms and the two times from macOS, the address of the GitHub repository and the day it was made
      public, and what became of the specs of units 0005 and 0006 (R13, R14, R17, R19)
- [ ] `npm run check-changes` passes on the committed branch

## Commit Points

- [ ] `feature-0004 License the plugin under GPL-3.0-or-later`: `LICENSE` and `THIRD-PARTY.md`. The license is in
      the tree before the code it is there for
- [ ] `feature-0004 Bundle sherpa-onnx in the plugin archive`: `build.gradle.kts` with the repository, the three
      dependencies, the two license files in the archive and the limit on its size. The tree builds an archive
      that carries the engine and does not use it yet
- [ ] `feature-0004 Add the speech engine and the service that plays it`: the seven new source files from
      `SpeechPlatform.kt` to `SpeechService.kt`, with their tests and `KokoroModelStandIn.kt`. The tests pass and
      the button still shows its dialog
- [ ] `feature-0004 Make the Read Aloud button speak the test sentence`: `ReadAloudAction.kt`,
      `ReadAloudActionTest.kt` and the deletion of `FileContentDialog.kt`. The tests pass
- [ ] `feature-0004 Describe speech, the model and the license in the README`: `README.md`
- [ ] `feature-0004 Record the implementation`: `record.md` with the measurements, and `plan.md` with the boxes
      that are done ticked
