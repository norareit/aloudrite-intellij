---
kind: task
status: draft
timestamp: 2026-10-08T22:14:08Z
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

## Amendments
