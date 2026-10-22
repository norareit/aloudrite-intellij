# Choose the speech model

## Approach

The plugin knows one model today, named by constants in `KokoroModel.kt` and `ModelPackage.kt` that the download,
the installer, the lock, the engine and the action all use. This unit makes the model a value, `SpeechModel`, of which
the plugin knows a list, and hands that value to every place that names the int8 model now. Then it adds the fp32
entry, a setting for the model in use, the operations of the page, and the part of the page. The work comes in seven
steps, each a commit.

**1. The model as a value, with one entry (a refactoring).** A new file `SpeechModel.kt` holds:

```kotlin
data class SpeechModel(
    val displayName: String,     // "Kokoro 1.0, int8"
    val packageName: String,     // "kokoro-int8-multi-lang-v1_0", also the name of its folder and its stored id
    val modelFileName: String,   // "model.int8.onnx"
    val modelPackage: ModelPackage,
) {
    fun folderIn(modelsFolder: Path): Path = modelsFolder.resolve(packageName)
}

val SPEECH_MODELS: List<SpeechModel> = listOf(KOKORO_INT8)
```

with `KOKORO_INT8` a private `val` beside the list, and the address built from the package name as
`https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/<packageName>.tar.bz2`. `KOKORO_MODEL_PACKAGE`,
`MODEL_PACKAGE_NAME` and `MODEL_FILE_NAME` go. `KokoroModel.kt` keeps the names of the files every Kokoro 1.0 package
shares, `VOICES_FILE_NAME`, `TOKENS_FILE_NAME`, `LEXICON_FILE_NAME` and `MODEL_DATA_DIRECTORY_NAME`, and:

* `userModelsFolder(operatingSystemName, userHome, xdgDataHome): Path`, the folder that holds the model folders:
  `~/Library/Application Support/AloudRite` on macOS and `$XDG_DATA_HOME/aloudrite` or `~/.local/share/aloudrite`
  on Linux. It replaces `userModelFolder`, and the folder of the int8 model stays where unit 0005 put it
* `isModelInPlace(model: SpeechModel, modelFolder: Path)`, which looks for the model's own file beside the shared
  ones

The installation takes the model where it took the constants. `ModelInstaller(model: SpeechModel)` downloads
`model.modelPackage` and checks that the package unpacks to exactly one directory named `model.packageName`.
`InstallAttempt` calls the package in its directory `package.tar.bz2`, since the directory is the attempt's own. Its
lock file and the prefix of its directories keep their present names, `.kokoro-int8-multi-lang-v1_0.install.lock` and
`.kokoro-int8-multi-lang-v1_0.download-`, as two constants that name no entry of the list. They are one lock and one
prefix for every model, which is what makes one download run at a time across models and across IDE windows (R4).
They keep the old strings so that an IDE that still runs an earlier version of the plugin takes the same lock, and its
leftovers are found. A comment says so.

`ModelDownloads.claim(modelsFolder)` takes the lock in the folder of the models, and
`start(model, modelsFolder, attempt, onModelInPlace)` installs that model. The `install` collaborator of
`ModelDownloadService` takes the model as well. `InstallFailure.OutOfDiskSpace` carries the `ModelPackage` whose sizes
its message gives, in place of reading `KOKORO_MODEL_PACKAGE`, and `ioFailure` and the unpacking get the package from
the installer to put in it. `downloadQuestion(model, modelFolder)` names the model by its display name.

What `ReadAloudAction.offerTheDownload` does, claim the lock, look again whether the model is in place, ask, and hand
the attempt to the download, is what the page does too (R4, R5). It moves into a new class `ModelDownloadOffer` in
`ModelDownloadOffer.kt`, whose one method

```kotlin
fun offer(model: SpeechModel, question: String, onAlreadyInPlace: () -> Unit, onModelInPlace: () -> Unit,
          onFailureRetry: () -> Unit)
```

holds that sequence, with the downloads, the folder of the models and the dialogs (`ask(question): Boolean`,
`tell(message)`) handed in. `onModelInPlace` is called with the attempt handed over to the download. The action keeps
its behaviour and its messages, and calls `offer`.

`KokoroSpeechEngine(modelFolder, modelFileName, threadCount)` takes the file name without a default, and the comment
that the plugin uses only the int8 file goes. `EngineSilenceMeasurement` takes its model from
`ALOUDRITE_MEASURE_MODEL_FOLDER` and `ALOUDRITE_MEASURE_MODEL_FILE` as now, with the int8 entry as the default.

Nothing a user sees or a test asserts about behaviour changes in this step, so the existing tests change only where
they name a constant that went, and all of them pass.

**2. The debug output of sherpa-onnx (R15).** The configuration that `KokoroSpeechEngine` builds moves into a function
of its own, `kokoroTtsConfig(modelFolder, modelFileName, threadCount, debugOutput: Boolean): OfflineTtsConfig`, which
calls `setDebug(debugOutput)`. Building it loads no native code. The engine takes `debugOutput` and hands it on. A
function `isEngineDebugOutputOn(propertyValue: String?): Boolean` is true only for `"true"`, and
`SpeechService.createKokoroEngine` passes it the system property `aloudrite.engine.debug`. In `build.gradle.kts`,
`tasks.runIde` sets that property to `true` with `systemProperty`, so the sandbox IDE writes the lines as now and an
installed plugin writes none. The binding has no getter for `debug`, so the test reads the private field `debug` of
the built `OfflineTtsModelConfig` by reflection, and says in a comment why.

**3. The fp32 entry and the default (R1, R3).** `SPEECH_MODELS` gets its second entry:

| | int8 | fp32 |
|---|---|---|
| Display name | Kokoro 1.0, int8 | Kokoro 1.0, fp32 |
| Package and folder | `kokoro-int8-multi-lang-v1_0` | `kokoro-multi-lang-v1_0` |
| Model file | `model.int8.onnx` | `model.onnx` |
| Size in bytes | 132,303,094 | 349,906,910 |
| Unpacked size in bytes | 189,882,497 | 401,239,297 |
| SHA-256 | `4c3052abaa60943a341f193888cf6abd68787dae6ab8ae5c925a706caa247e4e` | `c5f7e2d2caf082bc1d20fb70334a61d99d20b484500aad32e7cf84c128ea3298` |

The fp32 size and checksum are those of unit 0016's record, and the planner checked both again on 2026-10-22 against
the release file. The unpacked size is the sum of the sizes of the files of the package, as the int8 one is, which
the planner read from a listing of the package. It is 401 MB in the plugin's megabytes. The 384 MB of the spec's
context is what the folder takes on the disk by `du`, as 183 MB is for the int8 model.

The default is one function in `SpeechModel.kt`:

```kotlin
fun defaultSpeechModel(operatingSystemName: String, architectureName: String): SpeechModel =
    if (operatingSystemName.startsWith("Mac")) KOKORO_FP32 else KOKORO_INT8
```

On a platform without speech, which reads nothing, it is the int8 model. A test pins the default of both platforms.

A new application service `SpeechModelSetting` in `SpeechModelSetting.kt` is a `PersistentStateComponent` with
`@State(name = "AloudRiteSpeechModel", storages = [Storage("aloudrite.xml")])`. Its state holds `packageName:
String?`, null until the user chooses. `modelInUse` is the entry of `SPEECH_MODELS` with the stored package name, or
the default where there is none or the name is of no entry, as from a file edited by hand. The default is never
stored: `getState` gives null while nothing was chosen. `choose(model)` stores the choice, also where it equals the
default, and calls its collaborator `onChange(model)` where the model in use changed. The no-argument constructor
takes the default from `os.name` and `os.arch` and makes `onChange` tell `SpeechService.modelChanged()` and publish on
a new topic `SPEECH_MODEL_CHANGED` with the listener `SpeechModelChangeListener`, as `ReadingSpeedSetting` does.

**4. A reading with the model in use (R7, R8, R10).** `Readings.read` and `readOnceTheModelIsInPlace` take a
`SpeechModel` in place of the model folder, and `Reading` keeps it. `createEngine` takes the model, and
`createKokoroEngine` finds its folder with `userModelsFolder`. A new interface method, in a new narrow interface
`ReadingModel` that `SpeechService` implements, `modelOfTheReading(): SpeechModel?`, gives the model of the current
reading, for the removal of step 5.

The service keeps at most one engine, with the model it was made for. In `withTheEngine`, which holds `engineInUse`,
an engine of another model than the reading's is closed before the reading's engine is made, so two engines never
exist at once. That is within R10, which allows a second one at the start of a reading. The engine of a model that is
neither the model in use nor that of the current reading is released by a new private
`releaseTheEngineNoReadingUses()`, which takes `engineInUse` with `tryAcquire` and does nothing where it is held. It
runs:

* in `modelChanged()`, which `SpeechModelSetting` calls, so that a choice made while nothing reads frees the memory
  at once
* when a reading ends, stops or fails, so that a choice made during a reading frees the old engine after it
* after `withTheEngine` releases `engineInUse`, so that an engine work that held it when one of the above ran does
  the release itself

To know the model in use, the service gets one collaborator more, `modelInUse: () -> SpeechModel`, which is the
setting in the IDE. The engine of the model in use is kept between readings, as now.

`ReadAloudAction` takes `modelsFolder` and `modelInUse: () -> SpeechModel` in place of `modelFolder`. At a press it
takes the model in use once, reads with it where it is in place, and otherwise offers its download through
`ModelDownloadOffer`, with the question of unit 0005 naming that model (R8).

**5. The operations of the page (R4, R5, R6).** A new application service `SpeechModelManagement` in
`SpeechModelManagement.kt` does what the page asks, with its collaborators handed in: the setting, the downloads, the
`ReadingModel`, the folder of the models, the dialogs, and `runInBackground`. It holds the pending choice, the model
chosen and being downloaded, under its monitor.

* `isInPlace(model)`, `isDownloadRunning`, `pendingChoice` and `canRemove(model)` answer the page
* `download(model)` offers the download with the question `downloadQuestionFromThePage(model, folder)`, which asks
  "Download Kokoro 1.0, fp32?" followed by the sentences of sizes and place of `downloadQuestion`, which the two share
  as one function. It does not change the model in use. Where a download runs in this IDE it says so, as the action
  does
* `choose(model)` sets the model at once where it is in place, and clears a pending choice. Otherwise it offers the
  download with the question `chooseQuestion(model, folder)`, which says that the model is not on the machine and
  asks whether to download it and use it once it is in place. On a yes the model becomes the pending choice, and the
  callback of the download makes it the model in use only while it is still the pending one, so that a later choice
  wins over a download that ends after it. On a no nothing changes, and `choose` returns false so that the page shows
  the model in use again (R5)
* `remove(model)` refuses, with a message, the model in use and the model of the current reading, since R7 keeps a
  reading on its model. Otherwise it asks `removeQuestion(model, folder)`, which names the model, its folder and the
  megabytes it frees, its unpacked size. On a yes it claims the lock, which also makes it wait for no download: where
  the lock is held it says that a download is running. Holding the lock it looks again: where the folder does not
  hold a complete model by `isModelInPlace`, it is not the plugin's, and the removal stops with a message that names
  the folder and changes nothing (R6). Otherwise it moves the folder with one `ATOMIC_MOVE` into the attempt's
  directory, so that no half removed model is ever in place, and closes the attempt in the background, which deletes
  it and releases the lock. A removal that a killed IDE left half done is a leftover that the next attempt removes
* After a download ends, either way, and after a removal, it publishes on a new topic `SPEECH_MODELS_CHANGED`, so that
  the page shows what is on the machine. `ModelDownloadService` takes a collaborator `onRunningChanged` that publishes
  on the same topic when a download starts and when it ends, so that every open page shows that one runs (R4)

The messages are functions in `ModelDownloadMessages.kt`, beside the existing ones.

**6. The part of the page (R2, R9).** `ReadingSpeedConfigurable` becomes the page of both, and keeps its class name
and its registration, since the id in `plugin.xml` is what the IDE knows the page by. Under the row of the speed it
gets a group "Speech model" with one row per entry of `SPEECH_MODELS`, built by iterating the list:

* a radio button with the display name, in one button group, selected for the model in use
* a comment: "<size> MB download, <unpacked> MB on disk", then "on this machine", "not on this machine" or
  "downloading", and "in use" for the model in use
* a button "Download", enabled where the model is not in place and no download runs
* a button "Remove", enabled where `canRemove` holds

The choice follows the page's apply, as the speed does. `isModified` is true where the selected model differs from the
model in use and from the pending choice. `apply` calls `choose`, and where it returns false selects the model in use
again. Download and Remove act at once, as they change files and not a setting. While the page is open it follows
`SPEECH_MODEL_CHANGED` and `SPEECH_MODELS_CHANGED`. Each topic resets only its own part: a change of the speed resets
the speed and leaves a model selected but not applied, and a change of the model does the reverse. So `reset` is split
into `resetSpeed` and `resetModel`, and `reset` calls both.

**7. The README (R14).**

## Files

- `src/main/kotlin/com/ritenoar/aloudrite/SpeechModel.kt`: new. `SpeechModel`, `SPEECH_MODELS`, `defaultSpeechModel`
- `src/main/kotlin/com/ritenoar/aloudrite/KokoroModel.kt`: the shared file names, `userModelsFolder`, and
  `isModelInPlace` of a model
- `src/main/kotlin/com/ritenoar/aloudrite/ModelPackage.kt`: loses `KOKORO_MODEL_PACKAGE`
- `src/main/kotlin/com/ritenoar/aloudrite/ModelInstaller.kt`, `InstallAttempt.kt`, `InstallFailure.kt`,
  `PackageDownload.kt`, `PackageUnpacking.kt`: take the model or its package
- `src/main/kotlin/com/ritenoar/aloudrite/ModelDownloadService.kt`: `claim` and `start` of a model, `onRunningChanged`
- `src/main/kotlin/com/ritenoar/aloudrite/ModelDownloadMessages.kt`: the questions of the page and of removal
- `src/main/kotlin/com/ritenoar/aloudrite/ModelDownloadOffer.kt`: new, the offer of a download
- `src/main/kotlin/com/ritenoar/aloudrite/ReadAloudAction.kt`: the model in use, the offer
- `src/main/kotlin/com/ritenoar/aloudrite/KokoroSpeechEngine.kt`: the model file, `kokoroTtsConfig`, the debug output
- `src/main/kotlin/com/ritenoar/aloudrite/SpeechService.kt`: the model of a reading, one engine per model, its release
- `src/main/kotlin/com/ritenoar/aloudrite/SpeechModelSetting.kt`: new, the model in use
- `src/main/kotlin/com/ritenoar/aloudrite/SpeechModelManagement.kt`: new, download, choice and removal from the page
- `src/main/kotlin/com/ritenoar/aloudrite/ReadingSpeedConfigurable.kt`: the part "Speech model"
- `build.gradle.kts`: the system property of `runIde`
- `src/test/kotlin/com/ritenoar/aloudrite/`: `KokoroModelTest.kt`, `ModelPackageTest.kt`, `ModelPackageStandIn.kt`,
  `KokoroModelStandIn.kt`, `InstallSupport.kt`, `RealModelPackageTest.kt`, `ModelInstallerTest.kt`,
  `ModelDownloadServiceTest.kt`, `ModelDownloadFlowTest.kt`, `ModelDownloadMessagesTest.kt`, `ReadAloudActionTest.kt`,
  `ButtonPressSupport.kt`, `SpeechStandIns.kt`, `SpeechServiceTest.kt`, `KokoroSpeechEngineTest.kt`,
  `EngineSilenceMeasurement.kt`, `EngineThreadsMeasurement.kt`, `ReadingSpeedConfigurableTest.kt`; new
  `SpeechModelTest.kt`, `SpeechModelSettingTest.kt`, `SpeechModelManagementTest.kt`, `KokoroTtsConfigTest.kt`
- `README.md`: the two models

## Tasks

### 1. The model as a value, with one entry

- [x] Add `SpeechModel.kt` with `SpeechModel` and `SPEECH_MODELS` holding the int8 entry, with the values of today,
      and move `isModelInPlace` and the folder to the model as the Approach says. In `KokoroModelTest`, the folder
      tests assert `userModelsFolder` and `folderIn`, which give the same int8 folder as today on both platforms, and
      the test of a missing entry takes the model file from the entry. `ModelPackageTest` asserts the int8 entry's
      package as it asserts `KOKORO_MODEL_PACKAGE` today (R1)
- [x] `ModelPackageStandIn` and `fillWithModelStandIn` take a `SpeechModel`, so that the stand-in package unpacks to
      the model's directory and holds its model file (R1, R11)
- [x] `ModelInstaller`, `InstallAttempt`, `InstallFailure.OutOfDiskSpace`, `ioFailure` and the unpacking take the
      model or its package, with the lock file and the prefix kept under their present names and a comment that says
      why. `ModelInstallerTest`, `ModelDownloadMessagesTest` and `RealModelPackageTest` follow (R1, R4)
- [x] `ModelDownloads.claim(modelsFolder)` and `start(model, modelsFolder, attempt, onModelInPlace)`, and the install
      collaborator of `ModelDownloadService` with the model. `ModelDownloadServiceTest` follows (R1)
- [x] Move the claim, the second look, the question and the start out of `ReadAloudAction` into `ModelDownloadOffer`,
      which the action calls with the int8 entry. `ReadAloudActionTest`, `ButtonPressSupport` and
      `ModelDownloadFlowTest` change only in the arguments they pass, and all their cases pass unchanged (R8)
- [x] `KokoroSpeechEngine` takes the model file without a default, `SpeechService.createKokoroEngine` passes the int8
      entry's, and `KokoroSpeechEngineTest`, `EngineThreadsMeasurement` and `EngineSilenceMeasurement` follow (R1)
- [x] `grep -rn "MODEL_PACKAGE_NAME\|MODEL_FILE_NAME\|KOKORO_MODEL_PACKAGE\|kokoro-int8" src/main` finds the int8 name
      only in the entry of the list and in the two constants of the lock and the prefix (R1)
- [x] `./gradlew test` passes with as many tests as before this step, and with the model in its folder the tests of
      the real engine run and pass rather than being skipped (R11)

### 2. The debug output of sherpa-onnx

- [x] Write `KokoroTtsConfigTest`: `kokoroTtsConfig` with `debugOutput` false gives a model configuration whose
      `debug` is false, and with true one whose `debug` is true; `isEngineDebugOutputOn` is true for `"true"` and
      false for null, `""`, `"false"` and `"yes"` (R15)
- [x] Add `kokoroTtsConfig` and `isEngineDebugOutputOn`, hand `debugOutput` through `KokoroSpeechEngine`, and have
      `createKokoroEngine` read the property `aloudrite.engine.debug` (R15)
- [x] In `build.gradle.kts`, set `systemProperty("aloudrite.engine.debug", "true")` on `tasks.runIde`, and nowhere
      else (R15)
- [ ] Start the sandbox IDE with `./gradlew runIde`, read a file, and see the lines of sherpa-onnx in the output of
      the task. Run `./gradlew test` with the model in place and see that the real engine writes none of them (R15)

### 3. The fp32 entry and the default

- [x] Write `SpeechModelTest`: the list holds the two entries of the table of the Approach, in that order, each with
      its display name, package, model file, address, sizes and SHA-256; each address is HTTPS; the folders of the
      two lie side by side in `userModelsFolder`, as `kokoro-int8-multi-lang-v1_0` and `kokoro-multi-lang-v1_0`;
      `isModelInPlace` of each needs that entry's model file and not the other's; the default is the fp32 entry for
      `"Mac OS X"` with `"aarch64"` and the int8 entry for `"Linux"` with `"amd64"` (R1, R3, R11)
- [x] Add the fp32 entry and `defaultSpeechModel` (R1, R3)
- [x] Write `SpeechModelSettingTest`: a new setting gives the default it was made with; its state is null while
      nothing was chosen, so that a setting loaded from it with another default gives that other default; a chosen
      model, also one equal to the default, is given back by a new setting that loaded the state, whatever that
      setting's default; a stored name of no entry gives the default; `choose` calls the collaborator once for a
      change of the model in use and not for the model already in use; the IDE's own service is found (R3, R11)
- [x] Add `SpeechModelSetting`, `SPEECH_MODEL_CHANGED` and `SpeechModelChangeListener` (R3, R9)

### 4. A reading with the model in use

- [x] In `SpeechServiceTest`, with `SpeechStandIns` recording the model of each engine it makes and a settable model
      in use: a reading reads with the model it was given; a choice of the other model during a reading leaves that
      reading on its engine to its end, and the next reading makes an engine of the new model after the old one was
      closed; after a choice made while nothing reads, the engine of the old model is closed at once; after a choice
      during a reading, the old engine is closed when that reading ends, when it is stopped, and when it fails; an
      engine work that holds the engine when the choice comes closes it itself once it ends; the engine of the model
      in use is kept between two readings, as `theEngineIsMadeAtTheFirstReadingAndKept` says now; at no moment do
      two stand-in engines exist that are not closed (R7, R10, R11)
- [x] Give `Readings.read`, `readOnceTheModelIsInPlace`, `Reading` and `createEngine` the model, add `modelInUse`,
      `modelChanged`, `ReadingModel.modelOfTheReading` and `releaseTheEngineNoReadingUses` to `SpeechService` as the
      Approach says (R7, R10)
- [x] In `ReadAloudActionTest` and `ModelDownloadFlowTest`: a press with the fp32 model in use and in place reads with
      the fp32 model; a press with the fp32 model in use and only the int8 model in place asks the question naming the
      fp32 model, and after the download reads with it; a declined question reads nothing and leaves the int8 model
      where it is (R8, R11)
- [x] `ReadAloudAction` takes the folder of the models and the model in use, and offers the download of that model
      (R8)

### 5. The operations of the page

- [ ] Write `SpeechModelManagementTest`, with stand-ins for the downloads, the dialogs and the reading, and the models
      in a temporary folder:
    * a download from the page asks the question naming the model, its sizes and its folder, and on a yes starts it
      and leaves the model in use as it was (R4)
    * a download is refused, without a question, while one runs in this IDE, and with the message of unit 0005 while
      another process holds the lock, which `LockProbeProcess` holds as in `ModelDownloadFlowTest` (R4)
    * a choice of a model in place makes it the model in use with no question (R5)
    * a choice of a model not in place asks; on a no the model in use is unchanged and nothing starts; on a yes the
      model in use is unchanged until the download ends, and is the chosen model after it; a failed download leaves
      the model in use as it was; a later choice of the model in place, made while the download runs, stays the model
      in use after the download ends (R5)
    * a removal of a model in place and not in use asks a question that names the megabytes it frees, and on a yes
      the folder is gone and the lock file stays; on a no the folder stays (R6)
    * a removal of the model in use, and of the model of the current reading, is refused and asks nothing (R6, R7)
    * a removal while a download runs is refused and the folder stays (R4, R6)
    * a folder that does not hold a complete model is not removed (R6)
    * `SPEECH_MODELS_CHANGED` is published when a download starts, when it ends either way, and after a removal (R4)
- [ ] Add `SpeechModelManagement`, `SPEECH_MODELS_CHANGED`, `onRunningChanged` of `ModelDownloadService`, and the
      questions and messages in `ModelDownloadMessages.kt`, with `ModelDownloadMessagesTest` covering each (R4, R5,
      R6)

### 6. The part of the page

- [ ] In `ReadingSpeedConfigurableTest`, with a stand-in management: the page shows a group "Speech model" below the
      speed with one row per entry of `SPEECH_MODELS`, in order, each with its display name and the sizes of its
      comment; the radio button of the model in use is selected and its comment says "in use"; the comments say which
      model is on the machine; Download is enabled only for a model not on the machine while no download runs, and
      every row says "downloading" for the model being downloaded while one runs; Remove is enabled only for a model
      in place that `canRemove` allows; selecting the other model makes the page modified, and apply chooses it; a
      choice declined at apply selects the model in use again and the page is not modified; a choice made on another
      page, published on `SPEECH_MODEL_CHANGED`, shows on this one; a change of the speed meanwhile leaves a model
      selected and not applied as it is; the radio buttons keep their own height on a tall page (R2, R4, R5, R6, R9,
      R11)
- [ ] Add the part to `ReadingSpeedConfigurable`, built by iterating `SPEECH_MODELS`, and split `reset` (R2, R9)
- [ ] Look at the page in the sandbox IDE as `AGENTS.md` says, capturing only the window of the IDE, with neither
      model in place, with one, and during a download, and correct what the capture shows to be out of line with the
      speed row above it (R2)

### 7. The README

- [ ] In `README.md`, under "The speech model": the two models with their sizes, 132 MB and 190 MB for int8, 350 MB
      and 401 MB for fp32; the default per platform and why, with the finding of unit 0016; where the model is
      chosen, downloaded and removed, on the page "AloudRite" under Tools; that a press without the model in use
      asks to download that model; the address and SHA-256 of each; the commands to put either model in place by
      hand; the folders of both. Under "Testing", the sentence that the plugin always uses the int8 model goes, and
      `EngineSilenceMeasurement` is described with its default. Under "Running in a sandbox IDE", that the sandbox IDE
      shows the debug output of sherpa-onnx in the output of the task, and an installed plugin does not (R14, R15)

## Verification

- [ ] `./gradlew test` passes, with the model of the int8 entry in its folder, so that the tests of the real engine
      run and none of them is counted as skipped (R11)
- [ ] `./gradlew buildPlugin` passes within the size limit of the archive
- [ ] Every requirement from 1 to 15 is named by at least one task above, and the record says where R12 and R13 stand
- [ ] On Linux x64, the developer downloads from the page the model that is not on the machine, switches between the
      two, reads a file with each, and removes one, and the record holds that each step did what requirements 2 to 8
      say (R13)
- [ ] On macOS on Apple Silicon, the developer downloads the fp32 model from the page, chooses it, and listens to
      `samples/markdown-features.md` to the end of its first chapter of prose at 1.0, 1.5 and 2.0, and the record
      holds for each speed whether a part of the text was not heard and whether a silence came in the middle of a
      sentence (R12)
- [ ] `npm run check-changes` passes

## Commit Points

- [ ] `feature-0020 Plan choosing the speech model`: this plan and the record's entry
- [ ] `feature-0020 Take the speech model from a list`: step 1, a refactoring with no change of behaviour
- [ ] `feature-0020 Show the debug output of sherpa-onnx only in the sandbox IDE`: step 2
- [ ] `feature-0020 Add the fp32 model and a model in use per platform`: step 3
- [ ] `feature-0020 Read with the model in use and keep one engine`: step 4
- [ ] `feature-0020 Download, choose and remove a model`: step 5
- [ ] `feature-0020 Show the speech model on the settings page`: step 6, with what the capture led to
- [ ] `feature-0020 Describe the two speech models in the README`: step 7
- [ ] The record's entries of the developer's checks, R12 and R13
