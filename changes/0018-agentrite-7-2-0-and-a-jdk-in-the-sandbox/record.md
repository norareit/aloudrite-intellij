---
kind: feature
status: planned
timestamp: 2026-10-20T18:03:56Z
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

## Amendments
