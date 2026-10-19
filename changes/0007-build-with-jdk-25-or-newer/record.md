---
kind: task
status: implemented
timestamp: 2026-10-19T14:36:22Z
---

# Build with JDK 25 or newer

## Task

The project builds with JDK 25 or any newer JDK instead of exactly JDK 25.

Today `build.gradle.kts` declares `kotlin { jvmToolchain(25) }`. Gradle then requires a JDK 25 and fails on a newer
one, because no toolchain download resolver is configured. The human's Mac has JDK 27 and the build fails there.

The plugin runs inside the JDK of IntelliJ IDEA 2026.2, which is 25, so the bytecode the build produces stays at
Java 25 whichever JDK runs the build.

`README.md` says "JDK 25 is the only thing that has to be installed". It is reworded to say that JDK 25 or newer is
needed.

### Not yet known

* Whether Gradle 9.8.1 and Kotlin 2.4.21 build on JDK 27. Building on JDK 27 has not been tried.
* The exact failure on the Mac. The human has not shown the error yet. The task starts from the error text, and not
  from the assumption that the toolchain requirement is the only cause.

### Verification

The Linux host has JDK 25 only, so a build there shows that JDK 25 still works and that the output is still Java 25
bytecode, and nothing about JDK 27. The only machine with JDK 27 known is the human's Mac. The task is done when the
human, or an agent on the Mac, has run the README build and test commands there with JDK 27 and they pass. If they
do not pass, the cause goes into this record and the task is promoted to a bug or feature if it needs decisions.

## Log

- 2026-10-08T22:14:08Z [Claude Code, anthropic/claude-sonnet-5-5] as the specifier, in a subagent session on the
  Linux host, wrote the task description in this record. The unit is a task, because the request is a configuration
  tweak and a wording change. Status left at `draft` for the human to approve.
- 2026-10-08T22:47:30Z [human] approved the task, in the main session on the Linux host. The human said "Approve
  004", and answered "yes 0007" when asked whether they meant this unit. Status changed from `draft` to `approved`.
  The status change and this entry were written by Claude Code, anthropic/claude-sonnet-5-5, on the human's word.
- 2026-10-19T14:25:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, in a subagent session on the
  Linux host (JDK 25.0.2 only), began and did the work. Status changed from `approved` to `in-progress`. Replaced
  `kotlin { jvmToolchain(25) }` in `build.gradle.kts` with `java { sourceCompatibility/targetCompatibility = 25 }`
  and Kotlin `compilerOptions { jvmTarget = JVM_25 }`, so Gradle uses the JDK that runs it and the bytecode stays at
  Java 25. Reworded `README.md` Requirements to "JDK 25 or newer". Verified on JDK 25 only: `./gradlew buildPlugin`
  succeeds, and a class in `build/classes/kotlin/main` has class file major version 69 (Java 25). Not verified:
  anything on JDK 27, and whether the Mac failure has the toolchain as its only cause, since the error text was not
  available. `./gradlew test` reports 188 tests, 4 failed, 1 skipped, and the same 4 fail with the same count on
  unmodified `master` (`InstallAttemptTest` x3 and `ModelDownloadFlowTest` x1, `Cannot run program ... jbr/bin/java:
  Exec failed, error: 7 (Argument list too long)`, from `LockProbe` in this sandbox). They are unrelated to this task
  and left alone. The task stays `in-progress` until the human, or an agent on the Mac, runs the README build and
  test commands with JDK 27 and they pass.
- 2026-10-19T15:10:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, in the main session on the
  Linux host, now with JDK 27 as the default (`java -version` reports 27, installed by the human). On the branch of
  this task, `./gradlew clean buildPlugin test`: `buildPlugin` succeeds, and a freshly compiled class has class file
  major version 69 (Java 25). `test` reports 188 tests, 4 failed, 1 skipped, the same four as on JDK 25 and on
  `master`, which the human says are unit 0009 and still to be fixed there. The task stays `in-progress`: the Mac
  has not been tried, and the tests do not pass until 0009 is fixed.
- 2026-10-19T14:33:45Z [OpenCode, openai/gpt-6.1-sol] as the reviewer, reviewed unit 0007 at commit
  `d96b5a303d9c27dfc930c9426a82ecf54dbb16ef`, a full review against the merge base with `master`,
  `f908d373fe5053525ac46ce212f42b5cc7dc25f6`. Returned no findings on spec conformance, plan fidelity or code
  quality. Saved the complete review in
  `.reviews/0007-build-with-jdk-25-or-newer/20261019T143240Z-opencode-gpt-6.1-sol-full.md`. Independently built on
  Linux with JDK 27 and JDK 25.0.2, inspected Java 25 bytecode from each, and ran the full suite under the JDK 27
  build: 188 tests, the same four lock-probe failures recorded for unit 0009, and one skipped. The change check
  passes on the reviewed commit. Could not verify the Mac with JDK 27 or its original error; did not rerun the
  historical baseline runs on `master`.
- 2026-10-19T14:36:22Z [human] decided that the builds on this Linux host, with JDK 25.0.2 and with JDK 27, are
  enough, and that the Mac need not be tried. This waives the verification the task set, which was the README build
  and test commands passing on the Mac with JDK 27. The four lock-probe test failures stay with unit 0009. The
  status change and this entry were written by Claude Code, anthropic/claude-sonnet-5-5, in the main session on the
  Linux host, on the human's word. Status changed from `in-progress` to `implemented`.

## Amendments
