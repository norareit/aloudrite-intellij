# AgentRite 7.2.0, and a JDK in the sandbox

## Context

This project installs AgentRite with the range `^6.0.0`, at 6.0.0. Releases 7.0.0, 7.1.0 and 7.2.0 have come out
since, and each asks a project for steps beyond the install, in "Worth knowing when you update" of the kit's
`CHANGELOG.md`:

* **7.0.0** reads a project's settings from `agentrite.json`, written by `npx agentrite config`, and has the
  harnesses load the kit's `process.md` through `CLAUDE.md` and `opencode.json`
* **7.1.0** moves the rules on text, code and tests into `standards.md`, which the harnesses load beside `process.md`
* **7.2.0** requires the keys `bell`, `standards` and `roles` in `agentrite.json`, writes the list
  `.agentrite/instructions.md` of the instruction files in use, and has `AGENTS.md` name that list in the place of the
  kit's files. The sandbox's image is built again once, for the bell

7.2.0 also lets a project add software to the sandbox's image, with a file `sandbox/Dockerfile` built on the kit's
image. The kit added it for this project: an agent in its sandbox cannot build or test it, since the image has no
JDK. The build compiles to Java 25, as `build.gradle.kts` sets it, and `README.md` asks for JDK 25 or newer. The kit's
image is Debian bookworm, whose packages hold JDK 17 at the newest, which is what the example in the kit's
`SANDBOX.md` installs.

This project's `CLAUDE.md`, and the sections Development Guidelines, Roles and Changes in this project of its
`AGENTS.md`, are the 6.0.0 kit's text, with nothing of this project's in them. Its allowlist in
`sandbox/compose.project.yml` admits no host besides the kit's.

What the build reaches is in `build.gradle.kts` and the Gradle wrapper: the Gradle distribution from
`services.gradle.org`, Maven Central, the release assets of sherpa-onnx on GitHub, and the IntelliJ Platform's
default repositories, from which the build takes IntelliJ IDEA 2026.2.3. Which hosts those come from, redirects
included, was not established.

## Requirements

The kit:

1. `package.json` has the devDependency `agentrite` with the range `^7.2.0`, and `package-lock.json` an entry
   `node_modules/agentrite` at version 7.2.0, resolved from `registry.npmjs.org`. Nothing else in `package.json`
   differs
2. `agentrite.json` is the default file that `npx agentrite config` of 7.2.0 writes: the default branch `master`, the
   harnesses `claude` and `opencode`, the sandbox, the bell, and the kit's standards and role files
3. `.agentrite/instructions.md` and the eight role wrappers are what `npx agentrite refresh` of 7.2.0 writes, and
   `refresh` prints `up to date` for every file it writes and no notice
4. `opencode.json` is what `npx agentrite init` of 7.2.0 writes, with an `instructions` list of
   `node_modules/agentrite/process.md` and `node_modules/agentrite/standards.md`, and with one key besides, `$schema`,
   set to `https://opencode.ai/config.json`, as its first key
5. `CLAUDE.md` is the 7.2.0 kit's `templates/project/CLAUDE.md`, with the kit's `standards.md` filled in, byte for
   byte
6. The sections Development Guidelines, Roles and Changes in this project of `AGENTS.md` are those that
   `npx agentrite init` of 7.2.0 prints. The other sections are unchanged but for what requirement 12 adds
7. `npm run check-changes` exits 0, running the check of the installed 7.2.0

The JDK:

8. `sandbox/Dockerfile` opens with the two lines the kit's `SANDBOX.md` gives, and adds Amazon Corretto 25 to the
   image under `/opt`, with `JAVA_HOME` set to it and its `bin` on the `PATH`. It changes nothing of the entrypoint,
   the command, the user or the working directory
9. In the sandbox, `java -version` and `javac -version` print version 25 for the agent, and `java -version` names
   Corretto. The entrypoint's checks pass
   at start-up, and the agent holds no capability
10. In the sandbox, started with `./sandbox.sh --in-place`, `./gradlew buildPlugin` and `./gradlew test` succeed, as
    on the host, with a home volume made after the image was built
11. The `ALLOWLIST` of `sandbox/compose.project.yml` holds every host that the two commands of requirement 10 reach,
    each with a comment that says what of the build reaches it, and no other host besides the kit's
12. `README.md` says that `./gradlew buildPlugin` and `./gradlew test` run in the sandbox, which has the JDK, and that
    `./gradlew runIde` does not, as it opens a window. `AGENTS.md`, under "Building, running and testing", says the
    same for an agent, with a link to `README.md`

## Decisions

- **One unit for the move and the JDK.** The human asked for it. `sandbox/Dockerfile` needs 7.2.0, and the image is
  built again for 7.2.0 in any case
- **A feature, and not a task.** Which JDK, from where, and which hosts the build may reach need deciding
- **JDK 25, and not newer.** The build compiles to Java 25, and the plugin runs in the JDK 25 of IntelliJ IDEA
  2026.2, so the build's JDK is the plugin's own. Rejected: the newest JDK there is, which `README.md` allows but
  which changes under the project when it is built again
- **Amazon Corretto 25, copied from its official image.** The human chose Corretto. The developer's host builds
  with Corretto, installed with SDKMAN, and Gradle picks its Corretto 25 for the toolchain of `build.gradle.kts`, so
  the sandbox builds with the same JDK as the host. The file copies the JDK from the image `amazoncorretto:25-jdk`
  into `/opt`, which is one line, has a build for `amd64` and `arm64`, and needs no package source of its own. That
  image is built on Amazon Linux and not on Debian, but a JDK is a folder of its own that runs on the newer C library
  of bookworm. Rejected: Eclipse Temurin from the image `eclipse-temurin:25-jdk`, which the specifier first chose and
  which is as easy to copy, but is another JDK than the host's; Debian's packages, which hold JDK 17 at the newest in
  bookworm; Amazon's package source for Corretto, which adds a key and a source to the image for one package; and a
  toolchain that Gradle downloads at run time, which needs its hosts in the allowlist, and has the agent fetch and
  run a JDK in every new home volume
- **`opencode.json` keeps the `$schema` that OpenCode adds.** The file appeared with the key in the working tree
  after OpenCode was started in this project, and the human takes it to be OpenCode's own. Keeping it leaves the file
  as OpenCode wants it, so a start of OpenCode leaves the tree clean, and `refresh` prints no notice for it. Rejected:
  the file exactly as `init` writes it, which a start of OpenCode is taken to change again. Adding the key to what
  `init` writes is a change to the kit, made in its own repository
- **The hosts of the allowlist are found by running the build.** A host in the allowlist is one the agent can send
  data to, and the hosts of Gradle and of the IntelliJ Platform include redirects that the build files do not name.
  So the list is what the build was seen to reach, and no host is added in advance. GitHub is among them, for the
  assets of sherpa-onnx: the sandbox holds no credential of GitHub's, so the agent cannot push there, but it can
  reach whatever GitHub serves without one
- **What the build downloads at run time stays in the home volume.** Gradle keeps its distribution, the
  dependencies and IntelliJ IDEA under `~/.gradle`, in the home volume, so they are downloaded once per volume.
  Rejected: downloading them into the image, which ties the image to versions the build files change

## Non-goals

- `./gradlew runIde` in the sandbox, which opens a window, and the sandbox has no display
- A role or a standards file of this project's own, which 7.2.0 makes possible
- The other changes of the kit's three releases that ask nothing of this project: no unit is `planned`, no script or
  document runs `refresh` or `init` with arguments, no CI runs the change check, and the kit is under
  `devDependencies`

## Beyond the request

- None
