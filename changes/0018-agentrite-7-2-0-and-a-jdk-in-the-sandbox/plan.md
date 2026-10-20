# AgentRite 7.2.0, and a JDK in the sandbox

## Approach

The work has three parts, each with its own commit: the move to the kit's 7.2.0, the JDK and the allowlist of the
sandbox, and what `README.md` and `AGENTS.md` say about the build in the sandbox. The move comes first, because
`sandbox/Dockerfile` is built only by the wrapper of 7.2.0.

The move follows "Worth knowing when you update" of 7.0.0, 7.1.0 and 7.2.0, taken in one pass: the install, `config`,
`refresh`, `init`, `CLAUDE.md` and the three sections of `AGENTS.md` by hand, and `refresh` once more, which then finds
everything up to date and prints no notice.

The JDK is copied from Amazon's image `amazoncorretto:25-jdk`, which Docker pulls from Docker Hub on the host when the
sandbox's image is built. The allowlist is found by running the build in the sandbox and reading the egress proxy's
log, and is proved with a home volume made after the image.

This plan is written against the branch `feature-0018-agentrite-7-2-0-and-a-jdk-in-the-sandbox`, cut from `master`
at `78448f5`.

### What was tried while planning

On the Linux host, amd64, with Docker 28.4.0, in a clone of this branch in a scratch directory and in throwaway
images. Nothing of it was run in this project.

* **The kit's steps, end to end.** `npm ci`, then `npm install --save-dev agentrite@7.2.0`, changes the one line of
  the devDependency in `package.json` and gives `node_modules/agentrite` the version 7.2.0, resolved from
  `https://registry.npmjs.org/agentrite/-/agentrite-7.2.0.tgz`. `npx agentrite config` writes the file that
  "agentrite.json" below gives. `npx agentrite refresh` exits 0, refreshes the two implementer wrappers, writes
  `.agentrite/instructions.md`, finds the other six wrappers and `sandbox.sh` up to date, and prints the notices of
  `CLAUDE.md`, `opencode.json` and `AGENTS.md`. `npx agentrite init` exits 0, writes `opencode.json` as requirement 4
  gives it, skips every other file, `.dockerignore` among them, and prints the three sections of `AGENTS.md`. With
  `CLAUDE.md` and `AGENTS.md` then made as tasks 6 and 7 say, a second `refresh` prints `up to date` for its ten
  files and no notice, and `npm run check-changes` exits 0 once that is committed
* **The JDK, copied.** In a Dockerfile on `debian:bookworm-slim`, the one line
  `COPY --from=amazoncorretto:25-jdk /usr/lib/jvm/java-25-amazon-corretto.* /opt/corretto-25/` gives a JDK whose
  `java -version` prints `openjdk version "25.0.4.1"` and `OpenJDK Runtime Environment Corretto-25.0.4.11.1`, whose
  `javac -version` prints `javac 25.0.4.1`, and whose `bin/java` lacks no library under `ldd`. The wildcard is
  there because the folder of Corretto's image is named after the machine, `java-25-amazon-corretto.x86_64` on amd64
  and `.aarch64` on arm64, and `/usr/lib/jvm/java-25-amazon-corretto` is a link through `/etc/alternatives` that a
  `COPY` would not follow. A wildcard that matches one folder copies what is in it into `/opt/corretto-25/`
* **What reaches the agent.** The kit's `container/entrypoint.sh` sets
  `PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin` on line 11 and drops to the agent with
  `setpriv`, which keeps the environment. So `JAVA_HOME` from `ENV` reaches the agent, and a `PATH` from `ENV` does
  not. The programs of Corretto's `bin` are put on the `PATH` as links in `/usr/local/bin`. The human chose that
  while the plan was written
* **What the proxy logs.** The kit's `container/egress-proxy.mjs` writes `allowed <host> -> <address>` for a
  connection it admits and `refused <host>: not allowlisted` for one it refuses, to `/run/egress-proxy.log`, which
  the agent can read
* **The home volume** is `aloudrite-intellij-sandbox_agent-home`, from the compose name of
  `sandbox/compose.project.yml` and the volume `agent-home` of the kit's `container/compose.yml`

Not tried: the start of the sandbox with `sandbox/Dockerfile`, the build and the tests in it, and anything on arm64.

### What the implementer has to know before the first step

* The kit's files change with the install of task 2. From then on `process.md`, `standards.md` and
  `roles/implementer.md` of 7.2.0 are the rule, and the entries of this unit's `record.md` follow them. Read the
  three of 7.2.0 right after task 2. 7.1.0 removed the status `planned`: this unit is `in-progress` from the start of
  the implementation, so no status of it needs changing for that
* Removing the home volume in task 13 removes the login of Claude Code in the sandbox, which is kept once per home
  volume, and whatever else the developer keeps there. Ask the human before running `docker volume rm`, and say
  that Claude Code is logged in again at its next start in the sandbox
* `./gradlew clean` in the sandbox, in place, removes the host's `build/` as well. That is meant: an up-to-date
  `test` task would pass without running a test
* Each start of the sandbox reads the `ALLOWLIST` anew and fails when a listed host cannot be reached, so a host is
  added only once it was seen refused, with its name exactly as the proxy logged it

### agentrite.json

```json
{
  "defaultBranch": "master",
  "harnesses": ["claude", "opencode"],
  "sandbox": true,
  "bell": true,
  "standards": "node_modules/agentrite/standards.md",
  "roles": {
    "specifier": "node_modules/agentrite/roles/specifier.md",
    "planner": "node_modules/agentrite/roles/planner.md",
    "implementer": "node_modules/agentrite/roles/implementer.md",
    "reviewer": "node_modules/agentrite/roles/reviewer.md"
  }
}
```

### sandbox/Dockerfile

```dockerfile
ARG AGENTRITE_IMAGE
FROM ${AGENTRITE_IMAGE}

# Amazon Corretto 25, the JDK the developer's host builds with. The folder in Corretto's image is named after the
# machine, x86_64 or aarch64, hence the wildcard.
COPY --from=amazoncorretto:25-jdk /usr/lib/jvm/java-25-amazon-corretto.* /opt/corretto-25/
# The JDK's lib/security/cacerts is a link to /etc/pki/java/cacerts, Amazon Linux's trust store, which bookworm does
# not have. Without it the JDK trusts no certificate, and the Gradle wrapper fails at its first download.
COPY --from=amazoncorretto:25-jdk /etc/pki/ca-trust/extracted/java/cacerts /etc/pki/java/cacerts
ENV JAVA_HOME=/opt/corretto-25
# The kit's entrypoint sets the PATH anew before it drops to the agent, so the JDK's programs are linked into a
# directory on that PATH rather than added to it here.
RUN ln -s /opt/corretto-25/bin/* /usr/local/bin/
```

`ln -s` fails when a name is in `/usr/local/bin` already, so a clash with the kit's image fails the build and is not
hidden.

The second `COPY` was added by the implementer, after the first run of task 12 failed with `the trustAnchors parameter
must be non-empty`. In `amazoncorretto:25-jdk`, `lib/security/cacerts` of the JDK is a link to `/etc/pki/java/cacerts`,
which resolves to `/etc/pki/ca-trust/extracted/java/cacerts`. The first `COPY` copies the link and not what it points
to, so the JDK in the sandbox had no trust store. The file is copied to the path the link names, so the JDK is left as
Corretto's image has it, and its trust store is the one Corretto's image ships. Bookworm has no `/etc/pki`, so nothing
of the kit's image is replaced.

### The text for requirement 12

In `README.md`, at the end of the section "Development", after its paragraph and one empty line:

```markdown
The sandbox of AgentRite, started with `./sandbox.sh`, holds JDK 25, Amazon Corretto, so `./gradlew buildPlugin` and
`./gradlew test` run there as they do on the host. `./gradlew runIde` does not, as it opens a window and the sandbox
has no display.
```

In `AGENTS.md`, as the first bullet of "Building, running and testing", before the one on macOS:

```markdown
* In the sandbox, `./gradlew buildPlugin` and `./gradlew test` run as on the host, and `./gradlew runIde` does not,
  as it opens a window. [README.md](README.md) says why
```

## Files

- `package.json`: the devDependency `agentrite` at `^7.2.0`
- `package-lock.json`: as npm writes it, with `node_modules/agentrite` at 7.2.0 from `registry.npmjs.org`
- `agentrite.json`: new, written by `config`
- `.agentrite/instructions.md`: new, written by `refresh`
- `.claude/agents/implementer.md`, `.opencode/agents/implementer.md`: refreshed by `refresh`. The other six
  wrappers and `sandbox.sh` stay as they are
- `opencode.json`: new, written by `init`, with the key `$schema` that OpenCode adds, as the corrected requirement 4
  allows
- `CLAUDE.md`: the 7.2.0 kit's `templates/project/CLAUDE.md`, with the standards filled in
- `AGENTS.md`: the sections Development Guidelines, Roles and Changes in this project as `init` prints them, and one
  bullet under "Building, running and testing"
- `sandbox/Dockerfile`: new, as "sandbox/Dockerfile" above gives it
- `sandbox/compose.project.yml`: the hosts of the build in its `ALLOWLIST`
- `README.md`: one paragraph at the end of "Development"
- `changes/0018-agentrite-7-2-0-and-a-jdk-in-the-sandbox/record.md`: the log

## Tasks

- [x] 1. Run `npm ci`, so that `node_modules/` holds what `package-lock.json` of `master` gives (R1)
- [x] 2. Run `npm install --save-dev agentrite@7.2.0`. Check that `git diff -- package.json` shows the one line of
  the devDependency, now `"agentrite": "^7.2.0"`, and that `node_modules/agentrite/package.json` gives the version
  `7.2.0`. Then read `process.md`, `standards.md` and `roles/implementer.md` under `node_modules/agentrite/` (R1)
- [x] 3. Run `npx agentrite config`. Check that `agentrite.json` is the one under "agentrite.json" above (R2)
- [x] 4. Run `npx agentrite refresh`. It refreshes the two implementer wrappers and writes
  `.agentrite/instructions.md` (R3)
- [x] 5. Run `npx agentrite init > "$INIT" 2>&1`, where `$INIT` is a file outside the repository. It writes
  `opencode.json` and skips every other file. Check `opencode.json` against requirement 4 (R4)
- [x] 6. Write `CLAUDE.md` as
  `sed 's#{{standards}}#node_modules/agentrite/standards.md#' node_modules/agentrite/templates/project/CLAUDE.md`
  gives it (R5)
- [x] 7. In `AGENTS.md`, replace everything above the line `## Specific to this project`, and not the empty line
  before it, with what `sed -n '/^# Development Guidelines$/,/^The change check is/p' "$INIT"` prints. Nothing from
  `## Specific to this project` on changes (R6)
- [x] 8. Run `npx agentrite refresh` again. It prints `up to date` for each of its ten files and no notice. A notice
  means one of tasks 5 to 7 is not as the kit wants it: fix that task, not the notice (R3)
- [x] 9. Run the checks of requirements 1 to 6 under "Verification", then commit, as the first commit point, then
  run `npm run check-changes`. It exits 0 (R1, R2, R3, R4, R5, R6, R7)
- [x] 10. Write `sandbox/Dockerfile` as "sandbox/Dockerfile" above gives it, and check it against requirement 8 under
  "Verification" (R8)
- [x] 11. On the host, run `./sandbox.sh --in-place --rebuild true`. It builds the kit's image, which 7.2.0 asks for
  once, and the project's on it, and starts. Then run the check of requirement 9 under "Verification". Where the
  build of the file fails, or the wrapper refuses the image, stop and log what Docker or the wrapper printed (R8, R9)
- [x] 12. Find the hosts. Run
  `./sandbox.sh --in-place bash -c './gradlew clean buildPlugin && ./gradlew test; status=$?; grep "not allowlisted" /run/egress-proxy.log; exit $status'`.
  For each host it prints refused, add it to the `ALLOWLIST` of `sandbox/compose.project.yml`, below the comment
  that is there, as it was logged, and with a comment on the line above that says what of the build reaches it, as
  "the Gradle distribution, which services.gradle.org redirects to". Find what reaches it from Gradle's output, or by
  running the same command with `--info`. Run the command again until it exits 0 and prints no refused host. A
  failure that names no refused host, as a test that fails for want of an audio device, is not this unit's to work
  around: stop and log it, since requirement 10 says the two commands succeed (R10, R11)
- [x] 13. Prove the list with a new home volume. Ask the human first, as "What the implementer has to know" says.
  Then, on the host, run `docker volume rm aloudrite-intellij-sandbox_agent-home`, and
  `./sandbox.sh --in-place bash -c 'start=$(wc -l < /run/egress-proxy.log); ./gradlew clean buildPlugin && ./gradlew test; status=$?; tail -n +$((start + 1)) /run/egress-proxy.log; exit $status'`.
  It exits 0. Log in `record.md` the exit status, the version that `./gradlew --version` names for the JVM in the
  sandbox, and the hosts of the `allowed` and `refused` lines it printed. None is `refused`, and the `allowed` hosts
  outside the kit's own are exactly those of the `ALLOWLIST`. A listed host that no line names is taken out, and the
  command run once more (R9, R10, R11)
- [x] 14. Commit the JDK and the allowlist, as the second commit point
- [x] 15. Add the paragraph to `README.md` and the bullet to `AGENTS.md`, as "The text for requirement 12" above gives
  them, and run the check of requirement 12 under "Verification" (R12)
- [x] 16. Run every check under "Verification", then commit, as the third commit point, and run
  `npm run check-changes` once more (R7)
- [x] 17. Added by the implementer, after the correction of requirement 4: commit `opencode.json` with the key
  `$schema` that OpenCode added, run the check of requirement 4 and `npx agentrite refresh` under "Verification",
  and run `npm run check-changes` once that is committed (R3, R4, R7)

## Verification

- [x] `git diff master -- package.json` shows one changed line, `"agentrite": "^7.2.0"`, and
  `grep -A2 '"node_modules/agentrite"' package-lock.json` shows the version `7.2.0` and the `resolved` URL on
  `registry.npmjs.org`. Then `rm -rf node_modules && npm ci` exits 0, and `node_modules/agentrite/package.json` gives
  the version `7.2.0` (R1)
- [x] `agentrite.json` is byte for byte the file "agentrite.json" above gives, with the newline at the end that
  `config` writes (R2)
- [x] `npx agentrite refresh` prints `up to date` for the eight role wrappers, `.agentrite/instructions.md` and
  `sandbox.sh`, prints no notice, and leaves `git status --short` empty (R3)
- [x] `opencode.json` holds the keys `$schema`, first, set to `https://opencode.ai/config.json`, and `instructions`,
  and no other, with `node_modules/agentrite/process.md` and `node_modules/agentrite/standards.md` in that order
  (R4)
- [x] `diff <(sed 's#{{standards}}#node_modules/agentrite/standards.md#' node_modules/agentrite/templates/project/CLAUDE.md) CLAUDE.md`
  prints nothing (R5)
- [x] `diff <(sed -n '/^# Development Guidelines$/,/^The change check is/p' "$INIT"; echo; git show master:AGENTS.md | sed -n '/^## Specific to this project$/,$p') AGENTS.md`
  prints nothing at the first commit point. At the third, `git diff <first commit> -- AGENTS.md` shows the bullet of
  task 15 and nothing else (R6, R12)
- [x] `npm run check-changes` exits 0, run after the `npm ci` of the check of requirement 1, so that the script it
  runs is that of the installed 7.2.0 (R7)
- [x] `head -n 2 sandbox/Dockerfile` prints `ARG AGENTRITE_IMAGE` and `FROM ${AGENTRITE_IMAGE}`, and
  `grep -nE '^(ENTRYPOINT|CMD|USER|WORKDIR)' sandbox/Dockerfile` prints nothing. The wrapper starting the image in
  task 11 shows that it found none of the four changed (R8)
- [x] `./sandbox.sh --in-place sh -c 'java -version; javac -version; echo "$JAVA_HOME"; command -v java; grep -E "^Cap(Prm|Eff|Amb)" /proc/self/status'`
  starts with no complaint from the entrypoint, and prints version 25 for both programs, `Corretto` in the output of
  `java -version`, `/opt/corretto-25`, `/usr/local/bin/java`, and `0000000000000000` for each of the three sets of
  capabilities (R9)
- [x] The run of task 13 exited 0 with a home volume made after the image, and the record says what was seen (R10)
- [x] Every host in the `ALLOWLIST` has a comment on the line above it, is one task 13 saw allowed, and no host task
  13 saw outside the kit's own is missing from it (R11)
- [x] `git diff master -- README.md` shows one empty line and the three lines of the paragraph, added at the end of
  the file, and nothing else (R12)
- [x] `git diff --stat master -- src gradle build.gradle.kts settings.gradle.kts gradle.properties gradlew
  gradlew.bat .dockerignore sandbox.sh` prints nothing
- [x] `git diff --name-status master -- ':!changes'` lists the files under "Files" and no other
- [x] `./gradlew test` passes on the host, which `process.md` asks after every change

## Commit Points

- [x] `feature-0018 Move to AgentRite 7.2.0`: tasks 1 to 8. `package.json`, `package-lock.json`, `agentrite.json`,
  `.agentrite/instructions.md`, the two wrappers, `opencode.json`, `CLAUDE.md` and `AGENTS.md` in one commit, as the
  kit's notes ask
- [x] `feature-0018 Add Amazon Corretto 25 to the sandbox, and the hosts of the build to its allowlist`: tasks 10 to
  13
- [x] `feature-0018 Say that the build and the tests run in the sandbox`: task 15
- [x] The entries of `record.md` and the ticks in this plan, committed with the work they describe or directly after
  it, and before `npm run check-changes` is trusted, since it reads committed content
