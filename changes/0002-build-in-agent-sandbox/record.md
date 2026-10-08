---
kind: feature
status: draft
timestamp: 2026-10-08T13:50:36Z
---

# Build in the agent sandbox

## Wish

The plugin can be built, tested and worked on inside the AgentRite agent sandbox, so that a unit of this project can
be run there by number like a unit of any other project. Today it cannot: unit 0001 adds a Gradle build of an
IntelliJ Platform plugin, and its plan tells the implementer to build on the host. Two things are missing, and both
are needed. Either one alone leaves the build failing.

### The network allowlist

`agent/compose.project.yml` lists no hosts under `ALLOWLIST`, so the container reaches only what the kit admits
itself. The Gradle build has to reach, over HTTPS:

* the Gradle distribution host, for the wrapper's download of Gradle itself
* the Gradle plugin portal, for the Kotlin and IntelliJ Platform Gradle plugins
* Maven Central, for the libraries
* the JetBrains download and repository hosts, for the IntelliJ IDEA distribution of about 1.5 GB and the platform's
  test framework

This part is this project's own to change. The exact hostnames are not established yet. Some of these hosts redirect
to others, and the entrypoint refuses to start when a listed host is unreachable, so the list has to be found by
running the build and not by guessing.

### A JDK in the image

The sandbox image is built from the kit's `container/Dockerfile`, which starts from `node:26-bookworm` and installs
no JDK. The plugin needs JDK 25, because the platform classes of IntelliJ IDEA 2026.2 are compiled for Java 25. As of
AgentRite 4.2.0 the only thing a project can add to the image is the Playwright browser, through the
`PLAYWRIGHT_VERSION` build argument. There is no way for a project to add a JDK or any other toolchain.

This part cannot be done in this project. It needs an extension of AgentRite, made in the kit's repository, that lets
a project add a JDK to the sandbox image. This project then moves to the kit release that has it.

## State

This record holds the wish only, at the human's request. There is no `spec.md` yet: the requirements depend on the
shape of the kit's extension, which does not exist. The spec is written once the kit can do it.

## Log

- 2026-10-08T13:50:36Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, wrote this record at the human's request, after planning unit 0001 showed that its build cannot run in the
  sandbox. The kind is `feature` because the allowlist and the way the JDK is added both need decisions. No `spec.md`
  is written, because the unit waits for an AgentRite release that lets a project add a JDK to the image. Status left
  at `draft`.

## Amendments
