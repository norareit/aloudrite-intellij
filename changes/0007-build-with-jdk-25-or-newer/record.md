---
kind: task
status: in-progress
timestamp: 2026-10-19T14:25:00Z
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

## Amendments
