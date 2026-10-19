# Engine threads from the machine

## Approach

The rule of requirement 1 becomes one pure function, `engineThreadCount(availableProcessors: Int): Int`, which
returns half the number given, rounded down, kept between 1 and 6 (4 until the human set the cap from the measurements, see `record.md`). It is the only place that holds the halving, the
floor and the cap. `KokoroSpeechEngine` stops holding a number of its own: its constant `THREAD_COUNT = 2` goes, and
it takes the number of threads as a second constructor parameter, which it hands to `setNumThreads` as before.

The plugin creates the engine in one place, the factory that the constructor without arguments of `SpeechService`
hands to the primary constructor. That factory becomes a function of the companion of `SpeechService`,
`createKokoroEngine(modelFolder)`, which asks `Runtime.getRuntime().availableProcessors()` at that moment (R2), works
out the number with `engineThreadCount`, creates the engine with it, and then logs one line at INFO through the
`LOG` the service already has (R3). The line is written after the engine is created, so that it is never written for
an engine that failed to come into being, and it is written each time the factory runs, which is each time the
service creates an engine. The tests of `SpeechService` hand the primary constructor stand-ins and are not touched.

`KokoroSpeechEngineTest`, which runs the real engine, creates it with the number of requirement 1 for the machine it
runs on, as the non-goals of the spec say.

The measurements of requirement 5 are made by a test class of their own, `EngineThreadsMeasurement`, which runs only
where the environment variable `ALOUDRITE_MEASURE_ENGINE_THREADS` is set and the model is in its folder, and is
reported as skipped otherwise, after the example of `RealModelPackageTest` and `ALOUDRITE_MODEL_PACKAGE`. It creates
`KokoroSpeechEngine` directly with 1, 2, 4, 6 and 8 threads, one after the other, so that the developer needs no
build of the plugin and no setting, as the spec's "Beyond the request" expects. It is committed, so that the same
code measures on the Linux host and on the Mac. Its result is a small table written to `build/engine-threads.txt`,
which the implementer copies into `record.md`.

This plan is written against the branch `feature-0011-engine-threads-from-the-machine`, cut from `master` at
`423c8d7`. On that tree the number 2 is set in `KokoroSpeechEngine.kt` as `THREAD_COUNT`, the engine is created in
the constructor without arguments of `SpeechService` as `{ modelFolder -> KokoroSpeechEngine(modelFolder) }`, and in
`KokoroSpeechEngineTest` in `engine()`. Nothing else creates the engine or names the number. The service creates
its engine at the first reading and keeps it until it is disposed, so the engine is created once per IDE session. The
draft units 0012, 0013 and 0014 change none of this on `master`. `./gradlew test` on that tree on 2026-10-19 on the
Linux host, with the model in its folder, ran 188 tests: none failed, and one, `RealModelPackageTest`, was skipped.

### How the measurement is made

So that the figures of the two machines can be compared, the measurement is fixed here:

* **The number of processors** is `Runtime.getRuntime().availableProcessors()` of the test JVM, written at the top
  of the table with `os.name` and `os.arch`.
* **For each number of threads** in 1, 2, 4, 6 and 8, in this order: create `KokoroSpeechEngine` with the model
  folder of the machine and that number, make the short sentence once and throw the result away, so that the first
  call after the model is loaded does not count, then make the two measurements below, then close the engine before
  the next one is created. Only one engine exists at a time.
* **Time to the first audio** of the short sentence: the text of `samples/one-sentence.md`, trimmed, "The keeper
  climbed the stairs of the lighthouse every evening at sunset." From just before `speak` to the first call of its
  callback, measured with `System.nanoTime()`, the callback answering false so that the engine stops after it. Made 5
  times; the table gives the median in milliseconds.
* **Seconds of audio per second of work** for a long passage of prose: the first 3,000 characters of
  `samples/ten-thousand-words.md`, cut with `piecesOf` as the plugin cuts a reading, the pieces that are white space
  only left out, each piece handed to `speak` in turn with a callback that adds up the samples and answers true. The
  seconds of audio, the sum of the samples divided by `sampleRate`, divided by the seconds from before the first
  `speak` to after the last. Made once; the table gives it with two decimals, together with the seconds of audio.
* The samples are read from the project directory, which is the working directory of `./gradlew test`.

The table has one row per number of threads, with the columns threads, first audio in ms, seconds of audio, and
seconds of audio per second of work. On the Linux host it takes about five minutes, most of it at 1 thread.

## Files

- `src/main/kotlin/com/ritenoar/aloudrite/EngineThreads.kt`: new, `engineThreadCount` with the constants of the
  floor, the cap and the halving, and a comment that points to the spec of this unit for why
- `src/test/kotlin/com/ritenoar/aloudrite/EngineThreadsTest.kt`: new, the plain-number cases of requirement 4
- `src/main/kotlin/com/ritenoar/aloudrite/KokoroSpeechEngine.kt`: the number of threads becomes a constructor
  parameter, `THREAD_COUNT` goes
- `src/main/kotlin/com/ritenoar/aloudrite/SpeechService.kt`: the factory `createKokoroEngine` in the companion, with
  the log line
- `src/test/kotlin/com/ritenoar/aloudrite/KokoroSpeechEngineTest.kt`: `engine()` creates the engine with the number
  of requirement 1
- `src/test/kotlin/com/ritenoar/aloudrite/EngineThreadsMeasurement.kt`: new, the measurement of requirement 5
- `README.md`: the number of threads and that it follows from the machine
- `changes/0011-engine-threads-from-the-machine/record.md`: the log, the measurements and the human's findings

## Tasks

- [x] Write `EngineThreadsTest`, a plain JUnit 4 test with one case per pair, asserting that
      `engineThreadCount` gives 1 for 1, 1 for 2, 1 for 3, 2 for 4, 2 for 5, 3 for 6, 4 for 8, 4 for 9, 4 for 16 and
      4 for 32 processors. The cases of the spec are 1, 2, 3, 8, 9 and 32; 4, 5 and 6 show the halving between the
      floor and the cap, and 16 is the count of a machine of 8 cores and 16 threads. Each case names the processors
      and the threads in its name, as `eightProcessorsGiveFourThreads`. See it fail to compile (R1, R4)
- [x] Write `EngineThreads.kt` with `fun engineThreadCount(availableProcessors: Int): Int`, as
      `(availableProcessors / PROCESSORS_PER_THREAD).coerceIn(MINIMUM_THREADS, MAXIMUM_THREADS)` with the constants 2,
      1 and 4 named in the file. It takes no value from the running machine, so that it is a function of its argument
      alone. Its comment says in one or two lines why half: it stands for the physical cores, which Java cannot read,
      and the cap keeps the plugin a guest in the IDE, with a pointer to the spec of unit 0011 for the rest. No check
      of the argument: the JVM never reports fewer than 1 processor, and the floor holds for any smaller value anyway.
      Run `./gradlew test --tests com.ritenoar.aloudrite.EngineThreadsTest` and see it pass (R1, R4)
- [x] Change `KokoroSpeechEngine` to `class KokoroSpeechEngine(modelFolder: Path, threadCount: Int)`, hand
      `threadCount` to `setNumThreads`, and delete the constant `THREAD_COUNT`. Change nothing else of the class: the
      speaker `AF_HEART_SPEAKER_ID`, the speed `NORMAL_SPEED`, the files of the model and the callback stay as they
      are (R1, R8)
- [x] Change `engine()` of `KokoroSpeechEngineTest` to create the engine as
      `KokoroSpeechEngine(MODEL_FOLDER, engineThreadCount(Runtime.getRuntime().availableProcessors()))`. Its four
      cases stay as they are; they are what shows that the audio is made as before (R8)
- [x] In `SpeechService`, add to the companion
      `fun createKokoroEngine(modelFolder: Path): SpeechEngine`. It reads `Runtime.getRuntime().availableProcessors()`,
      computes the threads with `engineThreadCount`, creates `KokoroSpeechEngine(modelFolder, threads)`, and then logs
      `LOG.info("Created the speech engine with $threads threads, of $processors processors available")`, and
      returns the engine. A comment above it says that the processors are asked at each creation, so that a limit
      on the IDE's process, such as that of a container, is followed. The constructor without arguments hands
      `::createKokoroEngine` to the primary constructor in place of the lambda it has. The companion is `private`
      today and stays so (R2, R3)
- [x] Run `./gradlew test` and see every test pass that passed before, `KokoroSpeechEngineTest` passing where the
      model is in its folder, and the count of tests grown by the ten cases of `EngineThreadsTest` (R1, R4, R8)
- [x] Write `EngineThreadsMeasurement`, a plain JUnit 4 class with one test, `measureOneToEightThreads`, as described
      under "How the measurement is made". It opens with three assumptions, each with a message: `assumeTrue` on
      `System.getenv("ALOUDRITE_MEASURE_ENGINE_THREADS") != null`, on `isSpeechSupported` for the running machine and
      on `isModelInPlace` for the model folder, taken as in `KokoroSpeechEngineTest`. It writes the table to
      `build/engine-threads.txt`, replacing an earlier one, and prints it as well. It asserts only that each number
      of threads gave a first piece of audio and more than 0 seconds of audio. A comment at the top says what it
      measures, for which requirement, and how it is run. Run `./gradlew test` without the variable and see it
      reported as skipped (R5)
- [x] On the Linux host, run
      `ALOUDRITE_MEASURE_ENGINE_THREADS=1 ./gradlew cleanTest test --tests com.ritenoar.aloudrite.EngineThreadsMeasurement`
      with the IDE and other heavy programs closed or idle, and log the table of `build/engine-threads.txt` in
      `record.md`, with the date, the machine, and what else was running (R5)
- [x] Ask the developer to run the same command on the Mac on Apple Silicon, from this branch, and to hand over the
      table. Log it in `record.md` in the same way (R5)
- [x] Put the two tables in front of the human, with the step from each number of threads to the next, and ask
      whether the cap stays at 4. Log the answer in `record.md`. When the human keeps 4, go on. When the human sets
      another cap, stop: log it, and the unit goes back to the specifier for requirement 1 and requirement 4, whose
      numbers would change, before anything more is built (R5)
- [x] Raise the cap to 6, added after the human set it from the measurements (the cap was 4 in the tasks above, which
      were done as written). In `EngineThreadsTest` first add the cases `twelveProcessorsGiveSixThreads` and
      `thirteenProcessorsGiveSixThreads`, change `sixteenProcessorsGiveFourThreads` and
      `thirtyTwoProcessorsGiveFourThreads` to expect 6, and see the four fail. Then set `MAXIMUM_THREADS` to 6 in
      `EngineThreads.kt`. Run `./gradlew test` (R1, R4)
- [x] Add to `README.md`, in its first section, after the sentence that ends "the first sound comes before the whole
      file is turned into audio": that the engine makes the audio on half the processors the IDE may use, rounded
      down, at least 1 and at most 6 threads, so that the number follows from the machine and leaves the rest of it to
      the IDE, and that the IDE's log gives the number each time the engine is created. In "Testing", add a paragraph
      that `EngineThreadsMeasurement` runs only with `ALOUDRITE_MEASURE_ENGINE_THREADS` set and the model in its
      folder, with the command of the task above, and writes its table to `build/engine-threads.txt` (R7)
- [x] Build the plugin with `./gradlew buildPlugin` and start the sandbox IDE with `./gradlew runIde` on the Linux
      host. Ask the developer to read `samples/ten-thousand-words.md` aloud for at least a minute and to say what they
      find of the time to the first sound and of the load on the machine, the fans and the temperature, compared with
      the readings of unit 0006 on 2 threads. Take the line "Created the speech engine with ..." and the line "First
      sound ... ms" from `idea.log` of the sandbox, under `.intellijPlatform/sandbox/`, and log both lines and the
      developer's words in `record.md` (R3, R6, R8)
- [x] Run `./gradlew test` and `npm run check-changes` on the committed branch and log the counts in `record.md`

## Verification

- [x] `./gradlew test` passes, with `EngineThreadsTest` run and `EngineThreadsMeasurement` reported as skipped
- [x] `KokoroSpeechEngineTest` passes on the Linux host, where the model is in its folder
- [x] `grep -rn "setNumThreads\|THREAD_COUNT\|KokoroSpeechEngine(" src/main` shows the engine created only in
      `createKokoroEngine` and the number handed to `setNumThreads` only from its parameter
- [x] The sandbox IDE's `idea.log` holds one line "Created the speech engine with 6 threads, of 32 processors
      available" on the Linux host for the first reading after the IDE starts, and none for a second reading, since
      the service keeps its engine
- [x] `record.md` holds the tables of requirement 5 of both machines, with the number of processors each reported, the
      human's answer on the cap, and the findings of requirement 6 of both machines
- [x] `npm run check-changes` passes

## Commit Points

- [x] `feature-0011 Plan to take the engine's threads from the machine`: `plan.md` and `record.md`
- [x] `feature-0011 Work out the engine's threads from the processors`: `EngineThreads.kt` and
      `EngineThreadsTest.kt`
- [x] `feature-0011 Create the engine with the threads of the machine and log them`: `KokoroSpeechEngine.kt`,
      `SpeechService.kt` and `KokoroSpeechEngineTest.kt`. One commit, because the new constructor of the engine and
      its two callers build only together
- [x] `feature-0011 Measure the engine at one to eight threads`: `EngineThreadsMeasurement.kt`
- [x] `feature-0011 Record the measurements of both machines`: `record.md`
- [x] `feature-0011 Raise the engine's thread cap to 6`: `EngineThreads.kt`, `EngineThreadsTest.kt` and `plan.md`
- [x] `feature-0011 Describe the engine's threads in the README`: `README.md`
- [ ] `feature-0011 Record the implementation`: `record.md` and the ticked boxes of this plan
