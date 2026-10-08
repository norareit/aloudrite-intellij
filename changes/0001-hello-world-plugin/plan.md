# Hello world plugin

## Approach

The repository root becomes a single-module Gradle project next to the files that are already there. Nothing existing
is moved. The build is the standard one of the IntelliJ Platform Gradle Plugin 2.x: the Kotlin JVM plugin, the
platform plugin, the IntelliJ IDEA 2026.2 distribution as the platform dependency, and the platform test framework
with JUnit 4.

The versions below were checked on 2026-10-08 against the plugin portal, Maven Central, the Gradle and JetBrains
release services, and the developer's installed IDE. Use them as written, and log it in `record.md` when one of them
turns out not to work.

| What                           | Version     | Why this one                                                         |
|--------------------------------|-------------|----------------------------------------------------------------------|
| Gradle (wrapper)               | `9.8.1`     | Current release. Runs on JDK 25, which needs 9.1 or later            |
| IntelliJ Platform Gradle Plugin | `2.19.0`   | Current release                                                      |
| Kotlin Gradle plugin           | `2.4.21`    | Latest patch of the 2.4 line. IntelliJ IDEA 2026.2 bundles 2.4.0     |
| IntelliJ IDEA                  | `2026.2.3`  | Build `262.10968.63`, the build the developer runs                   |
| Java level                     | `25`        | The platform classes of 2026.2 are class file version 69             |
| JUnit                          | `4.13.2`    | What the platform test framework is built on                         |

Things the implementer would otherwise have to find out:

* **Java 25 through the toolchain.** `kotlin { jvmToolchain(25) }` picks up the installed JDK 25. No toolchain
  resolver plugin is added, so Gradle never downloads a JDK and requirement 1 holds as written.
* **There is no Gradle on this machine.** The wrapper is generated with a Gradle 9.8.1 distribution that is downloaded
  and unpacked outside the repository, and the distribution checksum is pinned in the wrapper properties.
* **IntelliJ IDEA is one distribution since 2025.3.** The dependency is `intellijIdea("2026.2.3")`. There is no
  separate Community artifact to choose.
* **The first build downloads the IDE, about 1.5 GB.** It lands in the Gradle user home and in `.intellijPlatform/`,
  not in anything tracked.
* **The build needs hosts the agent sandbox does not admit.** `agent/compose.project.yml` has an empty allowlist, and
  the build reaches the Gradle distribution host, the plugin portal, Maven Central and the JetBrains download and
  repository hosts. Build this unit on the host. Extending the allowlist traces to no requirement and is not part of
  this unit.
* **The version is stated once.** `version = "0.0.1"` is in `build.gradle.kts`. `plugin.xml` has no `<version>`
  element and no `<idea-version>` element: the build writes both into the descriptor it packages.
* **The compatibility range.** `sinceBuild` is set to `262` explicitly. `untilBuild` is left at the plugin's default.
  With IntelliJ Platform Gradle Plugin 2.19.0 that default is no upper bound: the packaged descriptor holds
  `since-build="262"` and no `until-build`.
* **The Kotlin standard library is not bundled.** `kotlin.stdlib.default.dependency=false` in `gradle.properties`
  keeps it out of the archive, because the platform provides it.
* **The archive name follows the project name.** `rootProject.name` is `aloudrite-intellij`, the name of the
  repository and of `package.json`, so the archive is `build/distributions/aloudrite-intellij-0.0.1.zip`.
* **How the test sees the dialog.** In the test environment `Messages.showInfoMessage` shows nothing and hands the
  message to the `TestDialog` set through `TestDialogManager`. The test installs one that captures the message,
  invokes the action, and restores the default in a `finally` block. It looks the action up by id through
  `ActionManager` and never names the action class, so that it exercises the registration in `plugin.xml` and
  compiles before the class exists.
* **The test opens no project.** Opening one in the test environment of IntelliJ IDEA 2026.2.3 fails before the test
  body runs: the licensing startup activity of the IDE's own commercial module cannot be created there, the test
  logger turns that logged error into a failure, and a `BasePlatformTestCase` opens a project in its `setUp`. The
  test therefore starts the application alone, with the test framework's `ApplicationRule`, and passes the action
  an event without a project. The action needs none: the project is only the parent of its dialog.
* **Names that the spec leaves to the implementation.** The action class is `SayHelloAction`, its id is
  `com.ritenoar.aloudrite.SayHello`, it is added to the group `ToolsMenu`, its label is the `text` attribute in
  `plugin.xml`, and the dialog title is `AloudRite`. There is no resource bundle, because translations are a non-goal.

## Files

- `.gitignore`: add `.gradle/`, `build/`, `.intellijPlatform/` and `.kotlin/`
- `settings.gradle.kts`: new, sets `rootProject.name`
- `build.gradle.kts`: new, the whole build configuration including the version
- `gradle.properties`: new, `kotlin.stdlib.default.dependency=false`
- `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties`: new,
  generated
- `src/main/resources/META-INF/plugin.xml`: new, the plugin descriptor
- `src/main/kotlin/com/ritenoar/aloudrite/SayHelloAction.kt`: new, the action
- `src/test/kotlin/com/ritenoar/aloudrite/SayHelloActionTest.kt`: new, the test
- `package.json`: remove the `version` field
- `package-lock.json`: remove the two `version` fields of the root package, at the top and under `packages[""]`
- `README.md`: new
- `AGENTS.md`: add the pointer to the tests under "Specific to this project"

## Tasks

- [x] Add `.gradle/`, `build/`, `.intellijPlatform/` and `.kotlin/` to `.gitignore`, before anything is built (R8)
- [x] Download the Gradle 9.8.1 binary distribution to a directory outside the repository, check it against the
      SHA-256 `dce76f55f8e251a3a1f130eb120f30b3d271de2b76c9b0729d316b5a1b6dc01f`, and use it once to run
      `gradle wrapper --gradle-version 9.8.1 --gradle-distribution-sha256-sum <that checksum>` in the repository
      root, with a `settings.gradle.kts` that sets `rootProject.name = "aloudrite-intellij"` already in place.
      `gradlew` is executable in git (R1)
- [x] Write `gradle.properties` and `build.gradle.kts`: the plugins `org.jetbrains.kotlin.jvm` `2.4.21` and
      `org.jetbrains.intellij.platform` `2.19.0`; `group = "com.ritenoar"` and `version = "0.0.1"`; the repositories
      `mavenCentral()` and `intellijPlatform { defaultRepositories() }`; the dependencies
      `intellijIdea("2026.2.3")`, `testFramework(TestFrameworkType.Platform)` and
      `testImplementation("junit:junit:4.13.2")`; `kotlin { jvmToolchain(25) }`; and
      `intellijPlatform { pluginConfiguration { ideaVersion { sinceBuild = "262" } } }`. The build takes its
      sources from `src/` only, and `gradle.properties` keeps the Kotlin standard library out of the archive
      (R1, R2, R5, R9)
- [x] Write `src/main/resources/META-INF/plugin.xml` with `<id>com.ritenoar.aloudrite</id>`,
      `<name>AloudRite</name>`, `<vendor>Ritenoar</vendor>`, a `<description>` of one or two sentences saying that
      the plugin reads Markdown and agentic coding documents aloud, and
      `<depends>com.intellij.modules.platform</depends>` as its only dependency. No action yet (R3)
- [x] Remove the `version` field from `package.json` and the root package's two `version` fields from
      `package-lock.json`, then run `npm ci` and `npm run check-changes` to see that both still work (R5)
- [x] Run `./gradlew buildPlugin` and see it produce `build/distributions/aloudrite-intellij-0.0.1.zip` (R1, R2)
- [x] Write `SayHelloActionTest`, a JUnit 4 test under the `ApplicationRule` with one test that looks up the action
      `com.ritenoar.aloudrite.SayHello` through `ActionManager`, asserts that it is a child of the `ToolsMenu` group
      and that its template text is `Say Hello`, invokes it on the event dispatch thread with a test event that
      holds no project, and asserts that the one message handed to the test dialog is exactly `Hello, world!`. Run
      `./gradlew test` and see it fail because the action is not registered (R4, R7)
- [x] Write `SayHelloAction`, an `AnAction` whose `actionPerformed` calls
      `Messages.showInfoMessage(e.project, "Hello, world!", "AloudRite")`, and register it in `plugin.xml` with the
      id `com.ritenoar.aloudrite.SayHello`, the text `Say Hello` and `<add-to-group group-id="ToolsMenu"/>`. Run
      `./gradlew test` and see it pass (R4, R7)
- [x] Write `README.md`: what the project is, in a few sentences taken from the spec's context and not from
      `idea.md`'s roadmap; what is needed to build it, which is JDK 25 and nothing else, with a note that the first
      build downloads IntelliJ IDEA; the commands `./gradlew buildPlugin`, `./gradlew runIde` and `./gradlew test`
      with what each does, that `runIde` uses a sandbox under `.intellijPlatform/sandbox/` and leaves the installed
      IDE and its settings alone; and how to install `build/distributions/aloudrite-intellij-0.0.1.zip` through
      Settings, Plugins, "Install Plugin from Disk" (R6, R10)
- [x] Add one item under "Specific to this project" in `AGENTS.md` saying that the commands that build the plugin,
      start the sandbox IDE and run the tests are in `README.md`. It names no command (R11)

## Verification

- [x] `./gradlew clean buildPlugin` succeeds and `./gradlew test` passes, both started from the wrapper with only
      JDK 25 on the path (R1, R7)
- [x] `unzip -l build/distributions/aloudrite-intellij-0.0.1.zip` lists one jar of the plugin under
      `aloudrite-intellij/lib/` and nothing else: no Kotlin standard library, nothing from `node_modules`,
      `changes/`, `agent/`, `idea.md` or `speak.py` (R9)
- [x] The `META-INF/plugin.xml` inside that jar holds the id `com.ritenoar.aloudrite`, the name `AloudRite`, the
      vendor `Ritenoar`, the version `0.0.1`, `since-build="262"`, and `com.intellij.modules.platform` as its only
      `<depends>` (R2, R3, R5)
- [x] `grep -n '"version"' package.json` finds nothing, and `0.0.1` appears in no tracked file outside
      `build.gradle.kts`, `README.md` and `changes/` (R5)
- [x] `./gradlew runIde` starts a sandbox IDE, Tools contains "Say Hello", and choosing it shows a dialog reading
      "Hello, world!". This needs a display. Where the implementer has none, log that in `record.md` and leave the
      check to the review, as the spec assigns it (R4, R6)
- [x] After the build, the test run and the sandbox run, `git status --porcelain` is empty on a committed tree and
      `git ls-files` shows nothing under `.gradle/`, `build/`, `.intellijPlatform/` or `.kotlin/` (R8)
- [x] The archive installs in the developer's IntelliJ IDEA 2026.2 through "Install Plugin from Disk" without a
      compatibility error. This is the human's check, made at the review (R2)
- [x] `npm run check-changes` passes on the committed branch

## Commit Points

- [x] `feature-0001 Add the Gradle build of an empty plugin`: the ignore rules, the wrapper, the build files, the
      descriptor without the action, and the removal of the version from `package.json` and `package-lock.json`.
      The tree builds an installable plugin that does nothing
- [x] `feature-0001 Add the Say Hello action`: the test, the action and its registration. The tests pass
- [x] `feature-0001 Document how the plugin is built and run`: `README.md` and `AGENTS.md`
