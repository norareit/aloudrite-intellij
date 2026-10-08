# Speech engine proof of concept

## Approach

The plugin gets three jars of sherpa-onnx 1.13.8 as dependencies: the Java binding and the native libraries for Linux
x64 and for macOS on Apple Silicon. The build puts them in the `lib/` folder of the plugin archive, next to the
plugin's own jar, and the binding loads the native library of the running platform out of its jar by itself. A new
action in the Tools menu checks the platform, then checks that the model is in its folder, and only then hands the
test sentence to an application service that makes the audio on a background thread and plays it through Java Sound,
which is part of the JDK the IDE runs on.

This plan is written against the head of `master` at `9299d4e`. Unit 0003 is implemented on its own branch and is
not in that tree: there is no "Read Aloud" button here, and the "Say Hello" action of unit 0001 is still there. This
unit leaves "Say Hello" alone, because removing it is unit 0003's. Where unit 0003 is merged before this unit, the
two meet in three files, `build.gradle.kts`, `plugin.xml` and `README.md`, and in each of them the changes of both
are kept.

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

### What a trial established

A throwaway trial, made in a scratch directory outside the repository and deleted afterwards, ran the three jars
and the unpacked model package on the developer's Linux machine, first in a plain JDK 25 and then in a copy of this
repository under `./gradlew buildPlugin test`.

* **The jars are not on Maven Central.** They are assets of the GitHub release, so the build declares that release
  as an Ivy repository of its own. With the declaration of the first task below, Gradle resolved all three. The
  repackaging `net.dreamlu:mica-sherpa-onnx` on Maven Central was rejected: it is a third party's, and it holds the
  native libraries of five platforms.
* **The archive came to 20,036,743 bytes**, with `aloudrite-intellij/lib/` holding the plugin's jar and the three
  jars of the table, and `LICENSE` and `THIRD-PARTY.md` directly under `aloudrite-intellij/`.
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
  and made the sentence passed there, next to the existing test under the `ApplicationRule`. A JUnit 4 assumption
  that fails is reported by Gradle as skipped.
* **Java Sound opened an output line of 24,000 Hz, 16 bits, mono** on the developer's Linux machine. Nothing was
  written to it, so nothing was heard.

Not established, and left to the checks in an IDE: that the binding finds its native libraries through the class
loader of a plugin, that anything is audible, how it sounds, and all of it on macOS. The two libraries for macOS
carry a code signature and find each other through `@loader_path`, which is all that could be read from Linux.

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
  of sherpa-onnx is ever touched, which is what lets the plugin load there.
* **The two messages** are shown with `Messages.showInfoMessage` and the title `AloudRite`, the call unit 0001
  showed to reach the test dialog:
    * `Speech is not supported on this platform. AloudRite speaks on Linux x64 and on macOS on Apple Silicon.`
    * `The speech model is missing. AloudRite looks for it in <folder>. The README of AloudRite says which model
      package to get and how to put it there.`, with the absolute path of the model folder for `<folder>`
* **A second invocation while one is running is ignored.** Nothing is queued and nothing is shown. Stopping is a
  non-goal, so the sentence that is being spoken ends by itself.
* **The audio is played piece by piece as the engine delivers it**, and not after the whole sentence is made. It
  is the same amount of code, and it makes the time to the first sound, which requirement 13 asks for, the time
  unit 0006 can expect.
* **The engine is created at the first invocation and kept** until the IDE closes, so that the second invocation
  of requirement 13 shows what an invocation costs without loading the model.
* **The engine runs on 2 threads**, which leaves the rest of the machine to the IDE.
* **The time to the first sound is written to the IDE's log**, as one line at level INFO each time the action
  speaks: the milliseconds from the start of `actionPerformed` to the first write to the audio line. That is where
  requirements 13 and 14 are read from. It is the moment the audio is handed to the operating system, which is as
  near to the sound as the plugin can see.
* **A failure of the engine or of the audio output is shown as a message**, with the text of the exception, and
  is logged as a warning. A warning is not an error report of the IDE, and an error would be one.
* **The archive limit is 30,000,000 bytes**, checked by the build itself at the end of `buildPlugin`, so that a
  model or a third platform in the archive fails the build.
* **The list of third-party software is `THIRD-PARTY.md`** in the repository root.
* **Names.** The action class is `SpeakTestSentenceAction` and its id is `com.ritenoar.aloudrite.SpeakTestSentence`.
  No parameter or variable has a one-letter name, which the human asked for in the record of unit 0003.

### Where the work is done

* **Build on the host.** The build fetches the three jars from `github.com`, which the agent sandbox does not
  admit, as it does not admit the hosts unit 0001 named. Extending the allowlist traces to no requirement.
* **The model on the developer's Linux machine.** The test of requirement 12 is skipped until the model is in its
  folder. Putting it there writes 190 MB into the developer's home directory, outside the repository, so the
  implementer asks the human before doing it, or the human does it. Either way it is done with the commands of
  the README as written, which checks them.
* **What needs ears or a Mac** is the human's, as the spec says, and is under Verification.

## Files

- `build.gradle.kts`: the Ivy repository for the release assets of sherpa-onnx, the three dependencies, `LICENSE`
  and `THIRD-PARTY.md` in the plugin archive, and the limit on the size of the archive
- `LICENSE`: new, the text of the GNU General Public License version 3
- `THIRD-PARTY.md`: new, the third-party software the plugin ships or uses, with the license of each
- `src/main/resources/META-INF/plugin.xml`: register the action
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
- `src/main/kotlin/com/ritenoar/aloudrite/SpeakTestSentenceAction.kt`: new, the action and the test sentence
- `src/test/kotlin/com/ritenoar/aloudrite/SpeechPlatformTest.kt`: new
- `src/test/kotlin/com/ritenoar/aloudrite/KokoroModelTest.kt`: new
- `src/test/kotlin/com/ritenoar/aloudrite/PcmAudioTest.kt`: new
- `src/test/kotlin/com/ritenoar/aloudrite/OneAtATimeTest.kt`: new
- `src/test/kotlin/com/ritenoar/aloudrite/SpeakTestSentenceActionTest.kt`: new
- `src/test/kotlin/com/ritenoar/aloudrite/KokoroSpeechEngineTest.kt`: new
- `README.md`: the license, the model and its folder, the action, the platforms, and what no longer holds
- `changes/0004-speech-engine-proof/record.md`: the log, the measurements and the human's findings

## Tasks

- [ ] Write `LICENSE` in the repository root with the text of `https://www.gnu.org/licenses/gpl-3.0.txt`,
      unchanged: 674 lines, 35,149 bytes, SHA-256
      `3972dc9744f6499f0f9b2dbf76696f2ae7ad8af9b23dde66d6af86c9dfb36986` (R15)
- [ ] Write `THIRD-PARTY.md`. It has one entry for each piece of third-party software the plugin ships in its
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
- [ ] In `build.gradle.kts`, add to `repositories` an `exclusiveContent` block whose repository is
      `ivy { url = uri("https://github.com/k2-fsa/sherpa-onnx/releases/download") }` with
      `patternLayout { artifact("v[revision]/[artifact]-[revision].[ext]") }` and
      `metadataSources { artifact() }`, and whose filter is `includeGroup("com.k2fsa.sherpa.onnx")`. Add the
      version `1.13.8` once, as a value, and three `implementation` dependencies of that group and version:
      `sherpa-onnx-jvm`, `sherpa-onnx-native-lib-linux-x64` and `sherpa-onnx-native-lib-osx-aarch64`. No other
      native jar is added (R1, R9)
- [ ] In `build.gradle.kts`, make `prepareSandbox` copy `LICENSE` and `THIRD-PARTY.md` from the repository root
      into the plugin's directory, as `from(layout.projectDirectory.files("LICENSE", "THIRD-PARTY.md")) {
      into(pluginName) }`, so that the archive holds both directly under `aloudrite-intellij/` (R16)
- [ ] In `build.gradle.kts`, make `buildPlugin` fail in a `doLast` when its archive is larger than 30,000,000
      bytes, with a message that states the size found and the limit. Run `./gradlew clean buildPlugin`, see it
      succeed, and see with `unzip -l` that the archive holds the plugin's jar, the three jars of sherpa-onnx,
      `LICENSE` and `THIRD-PARTY.md`, and nothing else. Then lower the limit to 1,000,000 for one run, see the
      build fail with that message, and put the limit back (R1, R2, R3)
- [ ] Write `SpeechPlatformTest`, a plain JUnit 4 test of a function `isSpeechSupported(operatingSystemName,
      architectureName)`: true for `Linux` with `amd64`, `Linux` with `x86_64`, `Mac OS X` with `aarch64` and
      `Mac OS X` with `arm64`; false for `Linux` with `aarch64`, `Mac OS X` with `x86_64`, `Windows 11` with
      `amd64` and `FreeBSD` with `amd64`. Then write the function in `SpeechPlatform.kt`, a pure function of its
      two arguments, and see the test pass (R1, R9)
- [ ] Write `KokoroModelTest`, a plain JUnit 4 test, for two functions. `userModelFolder(operatingSystemName,
      userHome, xdgDataHome)` gives `<userHome>/Library/Application Support/AloudRite/kokoro-int8-multi-lang-v1_0`
      for `Mac OS X` whatever `xdgDataHome` is; `<xdgDataHome>/aloudrite/kokoro-int8-multi-lang-v1_0` for `Linux`
      with an absolute `xdgDataHome`; and `<userHome>/.local/share/aloudrite/kokoro-int8-multi-lang-v1_0` for
      `Linux` where `xdgDataHome` is null, empty or a relative path. `isModelInPlace(modelFolder)` is false for a
      folder that does not exist, false for an empty folder, false for each of the five cases in which exactly one
      of `model.int8.onnx`, `voices.bin`, `tokens.txt`, `lexicon-us-en.txt` and the directory `espeak-ng-data` is
      absent, and true for a folder under a `TemporaryFolder` that holds empty stand-ins for all five. Then write
      `KokoroModel.kt` with the package name and the five entry names as constants, stated once, and the two
      functions. `userModelFolder` is pure and touches no file; `isModelInPlace` only looks whether the entries
      exist. See the test pass (R4, R8)
- [ ] Write `PcmAudioTest`, a plain JUnit 4 test of `toPcm16LittleEndian(samples)`: the result is twice as long
      as the input; `0f` gives the bytes `00 00`; `1f` gives `FF 7F`, which is 32,767; `-1f` gives `01 80`, which
      is -32,767; `2f` and `-2f` give the same as `1f` and `-1f`; and the input array is not changed. Then write
      the function in `PcmAudio.kt`: each sample is limited to the range -1 to 1, multiplied by 32,767, rounded,
      and written low byte first. See the test pass (R5)
- [ ] Write `OneAtATimeTest`, a plain JUnit 4 test of a class `OneAtATime` with one function `tryRun(work)`. While
      a first `tryRun` is held inside its work by a latch on another thread, a second `tryRun` returns false and
      does not run its work. After the first has returned, `tryRun` returns true and runs its work. After a work
      that throws, the exception reaches the caller and the next `tryRun` runs again. Then write `OneAtATime.kt`
      and see the test pass (R7)
- [ ] Write `SpeakTestSentenceActionTest`, a JUnit 4 test under the `ApplicationRule`, in the manner of
      `SayHelloActionTest`: it opens no project and captures the messages through `TestDialogManager`, restoring
      the previous dialog in a `finally`. Three tests. The first looks up `com.ritenoar.aloudrite.SpeakTestSentence`
      through `ActionManager` and asserts that it is a child of the group `ToolsMenu` and that its template text
      is `Speak Test Sentence`. The second creates `SpeakTestSentenceAction("Linux", "amd64", <an empty folder
      under a TemporaryFolder>)`, invokes it on the event dispatch thread with a test event that holds no project,
      and asserts that exactly one message was shown, that it contains `model is missing`, the absolute path of
      that folder and `README`. The third creates the action with `"Windows 11"`, `"amd64"` and a folder that
      holds stand-ins for all five model entries, invokes it the same way, and asserts that the one message shown
      is `Speech is not supported on this platform. AloudRite speaks on Linux x64 and on macOS on Apple Silicon.`
      Neither of the two needs the model or an audio device, and neither reaches the engine. Run `./gradlew test`
      and see the three fail to compile or fail, because the action does not exist (R5, R8, R9, R11)
- [ ] Write `KokoroSpeechEngineTest`, a plain JUnit 4 test with one test. It takes the model folder from
      `userModelFolder` with the `os.name`, `user.home` and `XDG_DATA_HOME` of the running machine, and opens with
      two assumptions, `assumeTrue` on `isSpeechSupported` for the running machine and `assumeTrue` on
      `isModelInPlace` for that folder, so that it is reported as skipped where either does not hold. It then
      creates a `KokoroSpeechEngine` for the folder, makes the audio of the test sentence, which it takes from
      the constant of the action and does not repeat, adds up the samples of the pieces it is handed, closes the
      engine in a `finally`, and asserts that the samples divided by the engine's sample rate lie between 2.0 and
      15.0 seconds. It opens no audio line (R12)
- [ ] Write `KokoroSpeechEngine.kt`, the only file that imports from `com.k2fsa.sherpa.onnx`. The class takes the
      model folder, builds an `OfflineTtsKokoroModelConfig` from the five entries of `KokoroModel.kt` with
      `setModel`, `setVoices`, `setTokens`, `setLexicon` and `setDataDir`, an `OfflineTtsModelConfig` with 2
      threads, and creates the `OfflineTts`. It offers the sample rate the engine reports, a function that makes
      the audio of a text and hands each piece of samples to a function of the caller as it arrives, through
      `generateWithCallback(text, 3, 1.0f, OfflineTtsCallback { ... })`, and a `close` that calls `release`.
      Speaker 3 and speed 1.0 are constants with the names `af_heart` and normal speed in them, and a comment
      says that the number comes from the `id2speaker` metadata of the model, because the binding has no lookup
      by name. A second comment says why the lexicon is always given: without it the native code ends the
      process. The engine reads the model from that folder alone: nothing in the plugin fetches a file or opens a
      connection, and a model that is absent is reported and never downloaded. With the model in its folder, run
      `./gradlew test` and see `KokoroSpeechEngineTest` pass, and see in `build/test-results/test/` that it is
      counted as passed and not as skipped (R1, R5, R10, R12)
- [ ] Write `AudioPlayer.kt`: a class that opens and starts a `SourceDataLine` of `javax.sound.sampled` for a given
      sample rate, signed 16-bit PCM, mono, little endian; a `play(samples)` that writes
      `toPcm16LittleEndian(samples)` to the line; and a `close` that drains the line and closes it. It uses the
      default output of the machine, which is what `AudioSystem.getSourceDataLine` gives, and names no mixer
      (R1, R5)
- [ ] Write `SpeechService.kt`, an application-level light service, `@Service(Service.Level.APP)`, that is
      `Disposable`. Its one function takes the text, the model folder and the moment the action was invoked, as
      a value of `System.nanoTime()`, and returns at once: through a `OneAtATime` guard that is taken on the
      calling thread and released when the work ends, it runs the work on a pooled thread with
      `ApplicationManager.getApplication().executeOnPooledThread`. The work creates the `KokoroSpeechEngine` if
      the service has none yet and keeps it, opens an `AudioPlayer` at the engine's sample rate, plays each piece
      as the engine hands it over, and closes the player when the engine is done. At the first piece of each
      invocation it logs, at level INFO, `First sound <n> ms after the action was invoked`. Anything thrown by
      the engine or the player is caught, logged with `warn` and never with `error`, and shown from the event
      dispatch thread, through `invokeLater`, with `Messages.showErrorDialog` under the title `AloudRite` and the
      text `AloudRite could not speak: <message of the exception>`. When the guard is already taken, the function
      does nothing. `dispose` closes the engine (R5, R6, R7, R13)
- [ ] Write `SpeakTestSentenceAction.kt`. The class has a constructor that takes the operating system name, the
      architecture name and the model folder, which the tests use, and a constructor without arguments, which the
      platform uses and which passes `os.name`, `os.arch` and the `userModelFolder` of `os.name`, `user.home` and
      the environment variable `XDG_DATA_HOME`. `actionPerformed` takes the time, then returns with the first
      message of the Approach where `isSpeechSupported` is false, then returns with the second where
      `isModelInPlace` is false, and otherwise hands the test sentence to the `SpeechService`. The sentence is a
      constant of this file, `AloudRite can speak. This sentence was made on your own machine.`, and the event
      parameter is named `event`. Register the action in `plugin.xml` with the id
      `com.ritenoar.aloudrite.SpeakTestSentence`, the text `Speak Test Sentence` and
      `<add-to-group group-id="ToolsMenu"/>`. The existing action and its test are not changed. Run
      `./gradlew test` and see every test pass (R5, R8, R9, R11)
- [ ] Rewrite the parts of `README.md` that this unit makes untrue, and add what it brings. The introduction says
      that the plugin can speak one fixed sentence, on Linux x64 and on macOS on Apple Silicon, and that on any
      other platform it loads and says that speech is not supported there. The sentence that the plugin depends on
      nothing beyond the platform itself is replaced by what holds now: the archive carries sherpa-onnx with its
      native libraries for those two platforms, about 20 MB, and nothing has to be installed besides the model.
      A new section on the model names the package `kokoro-int8-multi-lang-v1_0.tar.bz2`, its address, its size
      of 132 MB packed and 190 MB unpacked and its SHA-256, says that it is neither in the plugin nor in the
      repository, names the model folder on each of the two platforms, including the rule for `XDG_DATA_HOME`,
      and gives for each platform the commands that create the directory, fetch the package and unpack it there.
      It says that the sandbox IDE, an installed IDE and the tests all use that one copy, and that the test which
      makes audio is skipped while the model is absent. A new section describes the "Speak Test Sentence" action
      in the Tools menu: what it says, that it is a placeholder, the two messages, and that a second press while
      it speaks is ignored. A new section on the license says that the plugin is licensed under
      GPL-3.0-or-later, and why: it ships sherpa-onnx, whose native library contains espeak-ng, which is under
      GPL-3.0-or-later. It points to `LICENSE` and to `THIRD-PARTY.md`. Everything else in the README stays as it
      is, the description of "Say Hello" included (R3, R4, R15, R16, R18)
- [ ] With the human's agreement, put the model in its folder on the developer's Linux machine with the commands
      of the README, exactly as they are written there, and correct the README where they do not work. Check the
      package against the SHA-256 of the table before unpacking (R3, R4, R12)
- [ ] Measure on the developer's Linux machine and write into `record.md`: the size of the archive in bytes, from
      `stat -c %s build/distributions/aloudrite-intellij-0.0.1.zip`; the size of the model folder, from
      `du -sb` on it; and the two times of the line `First sound <n> ms after the action was invoked` in the
      `idea.log` of the sandbox IDE, for the first invocation after `./gradlew runIde` started the IDE and for the
      one after it. The log is under `.intellijPlatform/sandbox/`, in the `log` directory next to `log-test`.
      Where the implementer cannot press the action in a window, the human presses it twice and the implementer
      reads the two lines. State with the figures which machine and how many processor cores (R13)
- [ ] Hand the human what they need for their checks and write their answers into `record.md` as they give them:
      the archive to install on both machines, the commands of the README for the model on macOS, where the IDE
      keeps its log (Help, then Show Log), and the line to look for. The record then holds the human's judgment
      of how the sentence sounds on Linux x64 and on macOS on Apple Silicon, and the two times of the first and
      the second invocation as the human found them on macOS (R14)
- [ ] Ask the human for the address of the GitHub repository and the day it was made public, and write both into
      `record.md`. Creating the repository and pushing to it is the human's step and no agent does it (R17)
- [ ] Before the status is set to `implemented`, write into `record.md` a list of what this unit established that
      the specs of units 0005 and 0006 rest on, at the least: the model folder on each platform and what "in
      place" means; that the package unpacks to 190 MB; the address and the checksum of the package and that the
      address does not fix its content; that some mistakes in the engine's configuration end the process, so that
      a model has to be complete before the engine sees it; the times measured; that the audio arrives sentence
      by sentence; and what the binding leaves in the temporary folder. Ask the human to have the specifier
      revise both specs against that list, on `master` as the process says. Log in `record.md` that the human
      has seen the revisions, and for each of the two specs whether it changed (R19)

## Verification

- [ ] `./gradlew clean buildPlugin` succeeds, and `./gradlew test` passes with `SayHelloActionTest` unchanged and
      every other test that was in the tree before this unit unchanged, which includes the tests of the
      "Read Aloud" button where unit 0003 has been merged into the branch (R9, R11)
- [ ] With the model in its folder, `build/test-results/test/TEST-com.ritenoar.aloudrite.KokoroSpeechEngineTest.xml`
      reports 1 test, 0 skipped, 0 failures. With the model folder renamed for one run, the same file reports
      1 skipped and `SpeakTestSentenceActionTest` still passes. The folder is renamed back (R11, R12)
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
- [ ] The `META-INF/plugin.xml` inside the plugin's jar still holds `com.intellij.modules.platform` as its only
      `<depends>` (R9)
- [ ] In the sandbox IDE started with `./gradlew runIde` on Linux, with the model in place: Tools contains
      "Speak Test Sentence", and choosing it plays the sentence. While it is being made and played, typing in an
      editor goes on without a pause and the menus open. Choosing the action again while the sentence plays
      gives no second voice. This needs a display and ears. Where the implementer has neither, log that in
      `record.md` and leave the check to the human (R5, R6, R7)
- [ ] In the same IDE, with the model folder renamed: choosing the action shows the message that the model is
      missing, with the folder and the README in it, and no error indicator appears in the status bar of the
      IDE. The folder is renamed back (R8)
- [ ] In the same IDE, with the network of the machine switched off: the action speaks as before (R10)
- [ ] The archive installs through "Install Plugin from Disk" in the developer's IDE on Linux x64 and on macOS on
      Apple Silicon, and on both the action plays the sentence. These are the human's checks, made at the review
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
- [ ] `feature-0004 Add the Speak Test Sentence action`: the sources, the tests and the registration in
      `plugin.xml`. The tests pass
- [ ] `feature-0004 Describe speech, the model and the license in the README`: `README.md`
- [ ] `feature-0004 Record the implementation`: `record.md` with the measurements, and `plan.md` with the boxes
      that are done ticked
