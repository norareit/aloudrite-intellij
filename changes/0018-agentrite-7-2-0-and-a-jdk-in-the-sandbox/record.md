---
kind: feature
status: draft
timestamp: 2026-10-20T16:09:25Z
---

# 0018 AgentRite 7.2.0, and a JDK in the sandbox

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

## Amendments
