---
kind: feature
status: in-progress
timestamp: 2026-10-20T18:07:41Z
---

# AgentRite 7.2.0, and a JDK in the sandbox

## Log

- 2026-10-20T16:09:25Z [human] in a session of Claude Code in the kit's repository, after the kit's release 7.2.0,
  asked for a unit here that moves this project to it, and then that the unit add a JDK to the sandbox as well
- 2026-10-20T16:09:25Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in a session on the Linux host
  started in the kit's repository, on this project's `master` at `3f4c129`. Made the unit as a feature: which JDK,
  from where, and which hosts the build may reach need deciding. Numbered 0018: the highest number in `changes/` is
  0017. Read `roles/specifier.md` and `process.md` of the kit's installed 7.1.0, "Worth knowing when you update" of
  7.0.0, 7.1.0 and 7.2.0, "Adding software to the sandbox" of the kit's `SANDBOX.md`, and this project's
  `build.gradle.kts`, `gradle/wrapper/gradle-wrapper.properties`, `sandbox/compose.project.yml`, `README.md`,
  `AGENTS.md` and `CLAUDE.md`. Checked before writing, in a clone of `master` outside the project with 7.2.0 from the
  registry unpacked under `node_modules/agentrite`: `config` wrote the default file; `refresh` exited 0, refreshed
  the two implementer wrappers, wrote the list, found the rest up to date, and printed the notices of `CLAUDE.md`,
  `opencode.json` and `AGENTS.md`; `init` with no argument wrote `opencode.json` as requirement 4 gives it, skipped
  every other file, and printed the sections of `AGENTS.md`. Not run there: the install, `npm run check-changes`, the
  sandbox and the build. Not established: the hosts the build reaches, which requirement 11 leaves to the run of the
  build. Wrote `spec.md`. It decides, without asking the human: JDK 25 rather than the newest, Temurin copied from its
  official image, the allowlist found by running the build, and the downloads of the build kept in the home volume.
  Approving the unit approves them. The status is `draft`
- 2026-10-20T17:53:52Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux host,
  on the human's choice, made the JDK of the sandbox Amazon Corretto 25 in place of Eclipse Temurin 25 in `spec.md`:
  requirement 8 adds Corretto, copied from the image `amazoncorretto:25-jdk`, which Docker Hub lists for `amd64` and
  `arm64`; requirement 9 has `java -version` name Corretto; and the decision on the JDK says that the host builds
  with Corretto 25 from SDKMAN, and rejects Temurin. The status stays `draft`
- 2026-10-20T17:55:52Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux host,
  at the human's word, took the number out of the titles of `spec.md` and `record.md`, as no other unit has it, and
  removed the non-goal on unit 0015, which called it in progress on its branch, while it is implemented on `master`. The
  status stays `draft`
- 2026-10-20T17:56:16Z [human] approved the spec as it stands, in the main session on the Linux host, with Amazon
  Corretto 25 as the JDK of the sandbox, and with the choices the specifier made without asking: JDK 25 rather than the
  newest, the allowlist found by running the build, and the downloads of the build kept in the home volume. The status
  is `approved`. The entry was written by Claude Code, anthropic/claude-opus-5-5, on the human's word
- 2026-10-20T18:03:56Z [Claude Code, anthropic/claude-opus-5-5] as the planner, in the main session on the Linux host,
  cut `feature-0018-agentrite-7-2-0-and-a-jdk-in-the-sandbox` from `master` at `78448f5` and planned against it, under
  `roles/planner.md` and `process.md` of the installed 6.0.0. Tried the kit's steps of 7.2.0 end to end in a clone in a
  scratch directory, and the copy of Corretto from `amazoncorretto:25-jdk` in a throwaway image on bookworm, on amd64;
  "What was tried while planning" in `plan.md` gives what was seen. Found that the kit's entrypoint sets the `PATH`
  anew before it drops to the agent, so a `PATH` set in `sandbox/Dockerfile` does not reach the agent and requirement
  9 would fail. The human chose to put the programs of Corretto's `bin` on the agent's `PATH` as links in
  `/usr/local/bin`, read as meeting requirement 8, with `spec.md` left as it is. Wrote `plan.md`. The status is
  `planned`
- 2026-10-20T18:07:41Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, in the main session on the Linux
  host, began the implementation under `roles/implementer.md` and `process.md` of the installed 6.0.0. The branch
  stands on `master` at `f20bfa5`, and not at `78448f5` as `plan.md` says: `78448f5` is on no branch, and `f20bfa5`
  holds the same documents of the unit with the approval on top. The status is `in-progress`
- 2026-10-20T18:09:05Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, did tasks 1 to 9 of `plan.md`.
  `npm ci`, then `npm install --save-dev agentrite@7.2.0`, changed the one line of the devDependency and gave
  `node_modules/agentrite` the version 7.2.0 from `registry.npmjs.org`. From then on worked under `process.md`,
  `standards.md` and `roles/implementer.md` of 7.2.0, read right after the install. `npx agentrite config` wrote
  `agentrite.json` byte for byte as the plan gives it. The first `refresh` refreshed the two implementer wrappers, wrote
  `.agentrite/instructions.md`, and printed the notices of `CLAUDE.md`, `opencode.json` and `AGENTS.md`. `init` wrote
  `opencode.json`, with the key `instructions` alone, listing `process.md` and `standards.md`, and skipped every other
  file. `CLAUDE.md` and the three sections of `AGENTS.md` were made as tasks 6 and 7 say, and the checks of
  requirements 5 and 6 under "Verification" print nothing. The second `refresh` printed `up to date` for its ten files
  and no other line. After `rm -rf node_modules && npm ci`, which exited 0, the installed version is 7.2.0
- 2026-10-20T18:11:36Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, did tasks 10 and 11.
  `sandbox/Dockerfile` opens with the two lines of `SANDBOX.md`, and holds no `ENTRYPOINT`, `CMD`, `USER` or
  `WORKDIR`. `./sandbox.sh --in-place --rebuild true` exited 0: it built the kit's image, every step from Docker's
  cache, which held a build of the same 7.2.0 Dockerfile made 41 minutes before, outside this session, and built the
  project's image on it; the wrapper started it, and the entrypoint verified the firewall. The check of requirement 9
  printed `openjdk version "25.0.4.1"`, `OpenJDK Runtime Environment Corretto-25.0.4.11.1`, `javac 25.0.4.1`,
  `/opt/corretto-25`, `/usr/local/bin/java`, and `0000000000000000` for `CapPrm`, `CapEff` and `CapAmb`. Then, in task
  12, the first run was refused `services.gradle.org`, which went into the `ALLOWLIST`, and the second failed in the
  wrapper's download with `the trustAnchors parameter must be non-empty`: the JDK's `cacerts` is a link to
  `/etc/pki/java/cacerts`, which the copy of the JDK does not bring along. Added to `plan.md`, under
  "sandbox/Dockerfile", a second `COPY` of the trust store of `amazoncorretto:25-jdk` to the path the link names, with
  the reason. This is within requirement 8, which the file still meets, and needed for requirement 10
- 2026-10-20T18:19:54Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, did task 12 in fifteen runs of its
  command, with the second `COPY` of `sandbox/Dockerfile` in from the third. Each run but the last was refused one
  host, which went into the `ALLOWLIST` with a comment, in this order: `services.gradle.org`, `github.com`,
  `release-assets.githubusercontent.com`, `plugins.gradle.org`, `plugins-artifacts.gradle.org`,
  `repo.maven.apache.org`, `download.jetbrains.com`, `download-cdn.jetbrains.com`, `cache-redirector.jetbrains.com`,
  `www.jetbrains.com`, `d2cico3c979uwg.cloudfront.net`, `d2s4y8xcwt8bet.cloudfront.net` and
  `artifacts-caching-proxy.aws.intellij.net`. What reaches each was read from the URL in Gradle's error. The
  IntelliJ Platform Gradle Plugin looks for `idea:idea` in each of its default repositories in turn, and a host it
  cannot reach fails the build where a repository without the artifact would not, so the hosts of its releases,
  snapshots and dependencies repositories are all needed. The comments on `github.com` and
  `release-assets.githubusercontent.com` name the jars of sherpa-onnx as well, which `build.gradle.kts` takes from
  GitHub, and which the Gradle cache of the home volume holds since these runs. The fifteenth run exited 0:
  `buildPlugin` built `build/distributions/aloudrite-intellij-0.0.1.zip`, and `test` ran 283 tests, 0 failures, 0
  errors and 6 skipped, read from `build/test-results/test/`. Its only refused line is `example.com`, the canary
  that the kit's entrypoint checks at every start, and not the build. These runs used the home volume that was there
  before, so task 13 is what proves the list
- 2026-10-20T18:31:49Z [human] allowed the removal of the home volume of the sandbox for task 13. The entry was written
  by Claude Code, anthropic/claude-opus-5-5, on the human's word
- 2026-10-20T18:31:49Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, did task 13. Removed
  `aloudrite-intellij-sandbox_agent-home`, and ran the command of the task with `./gradlew --version` put in front,
  for the version of the JVM the task asks to log. Compose made the volume anew at 18:29:13Z, after the image, built
  at 18:11:59Z. The command exited 0. `./gradlew --version` names Gradle 9.8.1 and the launcher JVM
  `25.0.4.1 (Amazon.com Inc. 25.0.4.1+11-LTS)`, with `/opt/corretto-25` for the daemon. `buildPlugin` and `test`
  both printed `BUILD SUCCESSFUL`, and `build/test-results/test/` holds 283 tests, 0 failures, 0 errors and 6
  skipped. The proxy's lines of the run are all `allowed`, none `refused`, and name exactly the thirteen hosts of the
  `ALLOWLIST` and no other, none of the kit's own among them, so no host was taken out. The login of Claude Code in
  the sandbox went with the volume
- 2026-10-20T18:34:02Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, did tasks 15 and 16. Added the
  paragraph to `README.md` and the bullet to `AGENTS.md` as `plan.md` gives them. The plan puts the bullet both first
  and before the one on macOS, which is the second; it went before the one on macOS, after the bullet that points to
  `README.md`. Ran every check under "Verification" on the tree of the third commit, `33f565e`: after
  `rm -rf node_modules && npm ci`, which exited 0, the installed kit is 7.2.0; `agentrite.json` is the planned file
  byte for byte; `refresh` printed `up to date` ten times and no other line; `opencode.json` holds `instructions`
  alone, with `process.md` and `standards.md`; the diffs of `CLAUDE.md` and of `AGENTS.md` are as the checks of
  requirements 5 and 6 ask; `sandbox/Dockerfile` opens with the kit's two lines and holds none of the four
  instructions; every host of the `ALLOWLIST` has a comment above it; `README.md` gains one empty line and three; the
  build files, `src`, `.dockerignore` and `sandbox.sh` are unchanged; and the files changed outside `changes/` are
  those under "Files". On the host, with Corretto 27 as `java` and Gradle's toolchain of Java 25,
  `./gradlew clean test` exited 0, with 283 tests, 0 failures, 0 errors and 2 skipped. In the sandbox,
  `./gradlew clean buildPlugin && ./gradlew test` exited 0 on the same tree, with 283 tests, 0 failures, 0 errors and
  6 skipped, and no refused host but the canary. `npm run check-changes` exited 0 on `33f565e` with this entry
  committed
- 2026-10-20T18:34:02Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, logged a concern, not worked
  around. Both runs skip `EngineThreadsMeasurement.measureOneToEightThreads` and
  `RealModelPackageTest.theRealPackageInstallsToTheSameModelAsTheOneInPlaceByHand`, whose environment variables are
  not set. The sandbox skips four more, the cases `aSentenceIsMadeAsAudioOfAPlausibleLength`,
  `anEmojiInASentenceAddsNoBabble`, `everyPieceOfLongTextsWithoutSentenceEndsIsMadeWithoutACrash` and
  `aCallbackThatAnswersStopGetsOneCall` of `KokoroSpeechEngineTest`, which assume the speech model in
  `~/.local/share/aloudrite/`. The host has it there and the home volume of the sandbox does not. `./gradlew test`
  succeeds in the sandbox as requirement 10 asks, but it covers four tests fewer than on the host. Putting the model
  in the sandbox is not in this unit. Not checked: anything on arm64, and the sandbox on macOS. The status stays
  `in-progress` until the review's findings are decided

## Amendments
