# AgentRite 6.0.0 from the npm registry

## Approach

The steps are the ones the release notes of 5.0.0 and 6.0.0 give, taken in one pass and in an order that keeps
`package.json` as requirement 1 wants it. Nothing is written by `init` in this project. `init` of release 6.0.0 is
run once more in an empty directory outside the repository, and what it writes there is the reference that the
files of this project are compared with at the end.

This plan is written against the branch `feature-0008-agentrite-6-from-the-npm-registry`, cut from `master` at
`03f7cf2`.

### What was tried while planning

On the Linux host, with node 26.9.0, npm 11.19.1 and Docker 28.4.0, on copies of `package.json` and
`package-lock.json` in a scratch directory. Nothing of it was run in this project.

* **The order of the two npm commands matters.** `npm uninstall @ritenoar/agentrite` followed by
  `npm install --save-dev agentrite`, the order of the release notes, removes the key `devDependencies` and adds it
  again after `scripts`, which requirement 1 does not allow. `npm install --save-dev agentrite` followed by
  `npm uninstall @ritenoar/agentrite` leaves the key where it is. The diff of `package.json` against `master` is
  then the one line of the devDependency, and `package-lock.json` is byte for byte the one `init` leaves in the
  reference directory. So the install comes first here
* Neither npm command touches the `check-changes` script. It is changed by hand
* `refresh` of release 6.0.0, run a second time, prints `up to date` for each of its nine files. That is the check
  of requirement 3
* The eight role wrappers of this project hold no `model:` line, so `refresh` has nothing of the project's to keep
  in them
* `agent/` holds `compose.project.yml` and nothing else, tracked or not, so `git mv agent sandbox` leaves no
  directory `agent/`
* `docker info` exits 0 in the session on the Linux host, so requirement 10 can be run there by the implementer
* `sandbox.sh` is written by `refresh` with the executable bit, as `agent.sh` has it now

Not tried: `refresh` in a project that holds the wrappers of 4.2.0, the build of the image, and the start of the
sandbox.

### What the implementer has to know before the first step

* The role files move with the install. From task 2 on, `roles/implementer.md` and `process.md` are at
  `node_modules/agentrite/`, and between task 2 and task 3 the wrappers under `.claude/agents/` and
  `.opencode/agents/` point to a path that is gone. Read both files before task 2, and take tasks 2 and 3 in one
  go
* `process.md` and the role files of release 6.0.0 are the rule from task 2 on. Where they differ from 4.2.0 in
  how a record entry is written, the entries of this unit from then on follow 6.0.0
* The `--rebuild true` of requirement 10 is the option `--rebuild` and the command `true`, which the sandbox runs
  and exits from

### The README section

It goes at the end of `README.md`, after "Installing in an IDE", with the heading "Development". The README runs
from what the plugin is, through what a user needs, to how it is built and installed, and how the project is
worked on comes after all of that. The section is, with one empty line before its heading:

```markdown
## Development

New features of this project are built with [AgentRite](https://github.com/norareit/agentrite). The details are in
[AGENTS.md](AGENTS.md).
```

## Files

- `package.json`: the devDependency `agentrite` at `^6.0.0` in the place of `@ritenoar/agentrite`, and the new
  path in the `check-changes` script
- `package-lock.json`: as npm writes it, with `node_modules/agentrite` at `6.0.0` from `registry.npmjs.org`
- `.claude/agents/specifier.md`, `planner.md`, `implementer.md`, `reviewer.md`: refreshed by `refresh`
- `.opencode/agents/specifier.md`, `planner.md`, `implementer.md`, `reviewer.md`: refreshed by `refresh`
- `sandbox.sh`: new, written by `refresh`
- `agent.sh`: removed
- `agent/compose.project.yml`: moved to `sandbox/compose.project.yml`, then edited
- `.dockerignore`: the path of the kit on line 4
- `CLAUDE.md`: the path of the kit on line 8
- `AGENTS.md`: the name of the package and the path of the kit, and the first paragraph wrapped as `init` wraps it
- `README.md`: the section "Development" at the end
- `changes/0008-agentrite-6-from-the-npm-registry/record.md`: the log

## Tasks

- [x] 1. Make the reference. In a new empty directory outside the repository, called `$REF` below, write a
  `package.json` that holds `"name": "aloudrite-intellij"` and `"private": true`, then run
  `npm install --save-dev agentrite` and `npx agentrite init claude opencode` there. Check that
  `$REF/node_modules/agentrite/package.json` gives the version `6.0.0`. When the registry gives a newer version,
  stop and log it: the spec names 6.0.0 (R3, R5, R6, R7)
- [x] 2. In the project root, run `npm install --save-dev agentrite`, then `npm uninstall @ritenoar/agentrite`, in
  that order. Check that `git diff -- package.json` shows the one line of the devDependency and nothing else
  (R1, R2)
- [x] 2a. Run `npm ci`. Added while implementing: both packages declare the bin `agentrite`, and the uninstall of
  task 2 removes `node_modules/.bin/agentrite`, the link of the package that stays, so `npx agentrite` of task 3
  exits 127 with `agentrite: not found`. `npm ci` installs from the `package-lock.json` that task 2 left and
  writes the link again. It changes no tracked file (R2, R3)
- [x] 3. Run `npx agentrite refresh claude opencode`. It prints the eight role wrappers as refreshed and
  `wrote sandbox.sh` (R3)
- [x] 4. In `package.json`, change the `check-changes` script to
  `node node_modules/agentrite/scripts/check-changes.mjs` (R1, R9)
- [x] 5. Run `git rm agent.sh` and `git mv agent sandbox` (R4)
- [x] 6. Edit `sandbox/compose.project.yml` until `cmp sandbox/compose.project.yml $REF/sandbox/compose.project.yml`
  is silent. The changes are the five that requirement 5 lists: the path of the kit and `container/sandbox.sh` in
  the comment at the top, the service `sandbox`, the name and the image `aloudrite-intellij-sandbox`, the limits
  from `SANDBOX_MEM_LIMIT` and `SANDBOX_CPUS`, and the comment above `ALLOWLIST`. Make them by editing and do not
  copy the file from `$REF`, so that a difference nobody expects is seen and not overwritten (R5)
- [x] 7. In `.dockerignore` line 4 and in `CLAUDE.md` line 8, replace `node_modules/@ritenoar/agentrite` with
  `node_modules/agentrite`. Each is then identical to its file in `$REF` (R6)
- [x] 8. In `AGENTS.md`, replace `node_modules/@ritenoar/agentrite` with `node_modules/agentrite` everywhere and
  `@ritenoar/agentrite` with `agentrite` in the first paragraph, and wrap that paragraph as `$REF/AGENTS.md` has
  it. The section "Building, running and testing" at the end is not touched. No sentence is reworded (R7)
- [x] 9. Run `git grep -n -I -e '@ritenoar/agentrite' -e 'agentrite.ritenoar.com' -e 'agent\.sh' -e
  'agent/compose.project.yml' -- ':!changes'`. It finds nothing. Whatever it finds is changed to the new name, and
  logged in `record.md` when it is in a file this plan does not list (R8)
- [x] 10. Run the checks of requirements 1 to 9 and 12 under "Verification" (R1, R2, R3, R4, R5, R6, R7, R8, R9,
  R12)
- [x] 11. On the host, run `./sandbox.sh --in-place --rebuild true` and log in `record.md` what was seen: that the
  image `aloudrite-intellij-sandbox` was built, that the sandbox started, and the exit status. Where it does not
  exit 0, the unit is not done: find the cause among the steps above, and stop and log it when the cause is not
  there (R10)
- [x] 12. Commit the move, as the first commit point
- [x] 13. Add the section "Development" at the end of `README.md`, as "The README section" above gives it, and run
  the check of requirement 11 under "Verification" (R11)
- [x] 14. Commit the README, as the second commit point

## Verification

- [x] `git diff master -- package.json` shows two changed lines, the devDependency and the `check-changes`
  script, and they read as requirement 1 gives them (R1)
- [x] `cmp package-lock.json $REF/package-lock.json` is silent. Then `rm -rf node_modules && npm ci` exits 0, there
  is no `node_modules/@ritenoar`, and `node_modules/agentrite/package.json` gives the version `6.0.0` (R2)
- [x] `npx agentrite refresh claude opencode` prints `up to date` for the eight role wrappers and for `sandbox.sh`,
  and `git ls-files -s sandbox.sh` gives the mode `100755` (R3)
- [x] `test ! -e agent.sh && test ! -e agent` exits 0, and `git ls-files agent agent.sh` prints nothing (R4)
- [x] `cmp sandbox/compose.project.yml $REF/sandbox/compose.project.yml` is silent, and the `ALLOWLIST` in it lists
  no host (R5)
- [x] `cmp .dockerignore $REF/.dockerignore` and `cmp CLAUDE.md $REF/CLAUDE.md` are silent (R6)
- [x] `diff <(cat $REF/AGENTS.md; git show master:AGENTS.md | tail -n 4) AGENTS.md` prints nothing. The four lines
  are the empty line before the section "Building, running and testing" and the section itself (R7)
- [x] The `git grep` of task 9 prints nothing and exits 1 (R8)
- [x] `npm run check-changes` exits 0, and the command it echoes names `node_modules/agentrite/scripts/` (R9)
- [x] `./sandbox.sh --in-place --rebuild true` exited 0 on the host, and the record says what was seen (R10)
- [x] `git diff master -- README.md` shows added lines only, all of them at the end of the file: one empty line,
  the heading, one empty line and the two lines of text. They name no command, no path under `node_modules/`, no
  version and nothing of the sandbox (R11)
- [x] `git diff --stat master -- src gradle build.gradle.kts settings.gradle.kts gradle.properties gradlew
  gradlew.bat` prints nothing (R12)
- [x] `git diff --stat master -- ':!changes'` reports sixteen files changed and no other than those under "Files":
  fourteen changed in place, and two that git shows as renames, `agent.sh` to `sandbox.sh` and
  `agent/compose.project.yml` to `sandbox/compose.project.yml`. `git diff --name-status --no-renames master --
  ':!changes'` lists the same as eighteen paths, with each of the two as one deletion and one addition
- [x] `./gradlew test` passes. It is no requirement of the spec, which changes nothing that Gradle reads. It is
  what `process.md` asks after every change

## Commit Points

- [x] `feature-0008 Move to AgentRite 6.0.0 from the npm registry`: tasks 1 to 11, which is every file under
  "Files" but `README.md`. The tree is not coherent at any point between task 2 and task 9, so it is one commit
- [x] `feature-0008 Say in the README that the project is built with AgentRite`: task 13
- [x] The entries of `record.md` and the ticks in this plan, committed with the work they describe or directly
  after it, and before `npm run check-changes` is trusted, since it reads committed content
