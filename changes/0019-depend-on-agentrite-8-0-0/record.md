---
kind: task
status: approved
timestamp: 2026-10-21T21:03:00Z
---

# 0019 Depend on AgentRite 8.0.0

## Description

This project installs AgentRite with the range `^7.3.0`, at 7.3.0. Release 8.0.0 is published, and every command
of it that reads `agentrite.json` refuses a file that lacks its new required key `orchestrator`, the path of the
definition its new command `npx agentrite orchestrate` runs. Move the project to it:

* Run `npm install --save-dev agentrite@^8.0.0`. `package.json` has the devDependency `agentrite` with the range
  `^8.0.0`, and `package-lock.json` an entry `node_modules/agentrite` at version 8.0.0
* Add `"orchestrator": "node_modules/agentrite/orchestrator.json",` to `agentrite.json`, after `standards`, as
  "Worth knowing when you update" of 8.0.0 gives it. The file is then the default file of 8.0.0, byte for byte, and
  the project uses the kit's default definition
* Run `npx agentrite refresh`. It refreshes the reviewer's wrappers of Claude Code and OpenCode, whose description
  8.0.0 changed, and prints `up to date` for every other file it writes
* Run `npm run check-changes` and `./sandbox.sh --in-place true`

Afterwards `npx agentrite refresh` prints `up to date` for every file it writes, `npm run check-changes` exits 0,
running the check of the installed 8.0.0, and `./sandbox.sh --in-place true` exits 0.

The record says how each instruction of "Worth knowing when you update" of 8.0.0 was settled. As the dry run below
found them: the key of `agentrite.json` and the `refresh` are done by the steps above; the status of
`./sandbox.sh` asks nothing, since nothing of the project reads it; the directory `.orchestrator` that a unit run
makes, and that git does not track while it is empty, asks nothing; the reviewer's own commit asks nothing; no branch
of the project has a name that holds `--`; and no rebuild of the sandbox image is needed.

Not part of this task: a definition of the project's own, and running the orchestrator.

## Log

- 2026-10-21T21:03:00Z [human] in a session of Claude Code in the kit's repository, after the kit's release 8.0.0, asked
  for a unit here that moves this project to it
- 2026-10-21T21:03:00Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in a session on the Linux host started
  in the kit's repository, on this project's `master` at `4e5be2e`, switched to from `feature-0016-reading-speed` with
  no change uncommitted, and switched back after the commit. Made the unit as a task: one dependency, one line of the
  settings and the files `refresh` writes, with no decision left open. Numbered 0019: the highest number in `changes/`
  is 0018. Checked before writing, in a clone of `master` outside the project with 8.0.0 from the registry unpacked
  under `node_modules/agentrite`: with the line added, `agentrite.json` was the default file of 8.0.0 byte for byte;
  `refresh` exited 0, refreshed `.claude/agents/reviewer.md` and `.opencode/agents/reviewer.md`, printed `up to date`
  for every other file, and changed nothing else; `init` with no argument skipped every file and printed the sections of
  `AGENTS.md`, as 7.3.0's does. Not run there: the install, the change check and the sandbox. The status is `draft`
- 2026-10-21T21:03:00Z [human] approved the record. The status is `approved`

## Amendments
