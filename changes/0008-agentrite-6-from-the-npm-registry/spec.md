# AgentRite 6.0.0 from the npm registry

## Context

This project follows AgentRite 4.2.0. It installs the kit as the devDependency `@ritenoar/agentrite`, from the URL
of one release on `agentrite.ritenoar.com`. The kit has had two major releases since, and each renamed what a
project holds:

* **5.0.0** named the sandbox `sandbox` where it was named `agent`. What starts it is `sandbox.sh` and no longer
  `agent.sh`, the project's half of the configuration is `sandbox/compose.project.yml` and no longer
  `agent/compose.project.yml`, and the Compose service in it is `sandbox`. No old name keeps working
* **6.0.0** made the kit the package `agentrite` on the npm registry. Its files are in a project at
  `node_modules/agentrite/`, where they were at `node_modules/@ritenoar/agentrite/`. No old path keeps working,
  and the sandbox no longer admits `agentrite.ritenoar.com` by itself

The kit's own repository made the second move in its unit 0044. This project makes both at once. The host
`agentrite.ritenoar.com` is taken down once every project that uses the kit has moved, which is human step 8 of
the kit's unit 0043, and this project is one of them: after that, `npm ci` fails on every commit of this project
from before the move.

The release notes of the kit give the steps, under "Worth knowing when you update" of 5.0.0 and of 6.0.0 in its
`CHANGELOG.md`. The steps of 6.0.0 start from a project on 5.0.0, so a project on 4.2.0 takes those of 5.0.0 as
well.

What `init` of release 6.0.0 writes for this project was looked at while this spec was written: in an empty
directory with a `package.json` named `aloudrite-intellij`, `npm install --save-dev agentrite` and then
`npx agentrite init claude opencode`. The requirements below that name `init` refer to what it wrote there.

## Requirements

1. `package.json` has the devDependency `agentrite` with the version `^6.0.0`, and no devDependency
   `@ritenoar/agentrite`. Its `check-changes` script is `node node_modules/agentrite/scripts/check-changes.mjs`.
   Nothing else in the file differs
2. `package-lock.json` has an entry `node_modules/agentrite` at version `6.0.0`, resolved from
   `registry.npmjs.org` with an integrity hash, and no entry `node_modules/@ritenoar/agentrite`. `npm ci` exits 0
   with it and installs the kit at `node_modules/agentrite/`
3. The eight role wrappers under `.claude/agents/` and `.opencode/agents/`, and `sandbox.sh` at the root, are what
   `agentrite refresh claude opencode` of release 6.0.0 writes
4. `agent.sh` and the directory `agent/` are gone
5. `sandbox/compose.project.yml` is, byte for byte, what `init` of release 6.0.0 writes for this project. Against
   `agent/compose.project.yml` as it is now, that is:
    * the path of the kit in the comment at the top, and `container/sandbox.sh` where the comment named
      `container/agent.sh`
    * the service `sandbox` where it was `agent`
    * the Compose project and the image named `aloudrite-intellij-sandbox` where they were
      `aloudrite-intellij-agent`
    * the limits read from `SANDBOX_MEM_LIMIT` and `SANDBOX_CPUS` where they were read from `AGENT_MEM_LIMIT` and
      `AGENT_CPUS`, with the defaults `8g` and `6` unchanged
    * the comment above `ALLOWLIST`, which no longer names the kit's release host, and sends the reader to
      "Reaching more hosts" of the kit's `SANDBOX.md` where it sent them to "Egress" of its `README.md`

   The `ALLOWLIST` itself is unchanged, and lists no host
6. `.dockerignore` and `CLAUDE.md` name `node_modules/agentrite` where they named
   `node_modules/@ritenoar/agentrite`, and nothing else in them differs. Each is then what `init` of release 6.0.0
   writes
7. `AGENTS.md` is what `init` of release 6.0.0 writes, followed by the section "Building, running and testing"
   that the file ends with now, unchanged. So it names the package `agentrite` and the path `node_modules/agentrite`
   everywhere, its first paragraph is wrapped as `init` wraps it, and no sentence of the file says anything else
   than it says now
8. No tracked file names `@ritenoar/agentrite`, `agentrite.ritenoar.com`, `agent.sh` or
   `agent/compose.project.yml`, apart from the files under `changes/`
9. `npm run check-changes` exits 0, and runs the check of the installed release 6.0.0
10. On the host, `./sandbox.sh --in-place --rebuild true` exits 0: the image is built and the sandbox starts. The
    implementer runs it where the session has Docker, or asks the human to, and logs what was seen
11. Nothing of the plugin differs: no file under `src/` or `gradle/`, none of the Gradle files at the root, and
    not `README.md`

## Decisions

* **The move goes to 6.0.0 in one unit, not to 5.0.0 first.** Every release up to 5.0.0 is installed from the host
  that is about to go, so a stop at 5.0.0 would be a state nobody keeps. The two sets of steps do not get in each
  other's way: those of 5.0.0 rename the sandbox, those of 6.0.0 rename the path of the kit, and
  `refresh` of 6.0.0 writes a `sandbox.sh` that holds both
* **The steps are the kit's.** `npm uninstall @ritenoar/agentrite` and the install, then
  `npx agentrite refresh claude opencode`, the removal of `agent.sh`, the move of `agent/` to `sandbox/`, the
  rename of the service, the replacement of the path and of the package's name in the project's own files, and
  the rebuild of the sandbox. They are what the release notes of 5.0.0 and 6.0.0 give. The order in which they are
  taken is the planner's
* **The files are judged against what `init` writes, and are not written by `init`.** `init` is for a project
  that has none of these files. What it writes is the one definition of what a project of release 6.0.0 holds, and
  it makes requirements 5, 6 and 7 checkable with `diff`. That it lets every file of this project come out
  identical is seen: the project changed nothing in what `init` of 4.2.0 wrote, but for the section at the end of
  `AGENTS.md`
* **The range `^6.0.0`, as npm writes it.** `npm install --save-dev agentrite` is the command the release notes
  give for a project that holds the kit as a devDependency. `package-lock.json` still pins the one version with
  its integrity hash, and `npm ci` installs exactly that. The kit's own repository pins `6.0.0` exactly in
  `package.json`, for a reason of its own: its release process checks a one-line diff of that file. This project
  has no such step
* **The Compose project and the image take the name `aloudrite-intellij-sandbox`.** The release notes of 5.0.0
  allow either: keeping `aloudrite-intellij-agent` keeps the home volume, which holds Claude Code's login, the
  image and the clones of earlier unit runs, and the new name costs all three. On the Linux host there is nothing
  to keep: `docker volume ls` and `docker image ls` show no volume and no image of this project, so its sandbox
  was never started there. With the new name the file is what `init` writes, and what `docker` lists says
  `sandbox` like everything else. Not established: whether the sandbox of this project was ever started on the
  human's Mac. If it was, the cost there is one more login of Claude Code, and a volume, an image and a network
  under the old name that stay until removed by hand, as "Where things are kept on the host" of the kit's
  `SANDBOX.md` gives
* **The limits are read from `SANDBOX_MEM_LIMIT` and `SANDBOX_CPUS`.** The release notes leave the old names
  working, since the file reads whatever it names. The kit's documents describe only the new names, so with the
  old ones a limit set as `SANDBOX.md` says would be ignored without a word. Nothing tracked in this project sets
  either. A shell on a computer that sets `AGENT_MEM_LIMIT` or `AGENT_CPUS` has to set the new names, and nothing
  finds that
* **The comments of the compose file are corrected with the rest.** The release notes say that the old paths in
  the comments harm nothing. Left as they are, they would name a script, a host and a section of a README that no
  longer exist, in a file a reader opens to find out what the sandbox may reach
* **The records under `changes/` are left as they are.** The plans of units 0001 and 0004 are frozen. The record
  of unit 0002, which is `draft`, names `agent/compose.project.yml` and says what AgentRite 4.2.0 lets a project
  add to the image. Bringing it up to the release this project then follows is a revision of that unit, by its
  own specifier
* **The suites of the plugin are not a requirement.** The unit changes no file that Gradle reads, which
  requirement 11 states. What `process.md` asks after every change still holds for the implementer
* **It is a feature and not a task.** It changes sixteen files in an order that matters, has a step that
  needs Docker on the host, and decides a name and two variables, so it gets a plan and a review

## Non-goals

* Building the plugin inside the sandbox. That is unit 0002, and it needs hosts in `ALLOWLIST` and a JDK in the
  image. After this unit the sandbox starts, and reaches what the kit admits and nothing more
* Listing `agentrite.ritenoar.com` in `ALLOWLIST`. The installed release comes from the npm registry, which the
  kit admits
* Removing the volume, the image or the network that a sandbox under the old name left on any computer
* A change to the records of other units under `changes/`
* A change to `README.md`. It names neither the kit nor the agent sandbox. The "sandbox IDE" it describes is the
  one of the IntelliJ Platform Gradle plugin
* Taking down the host `agentrite.ritenoar.com`, and moving any other project

## Beyond the request

* A section in `README.md` on working with the agent sandbox: that `./sandbox.sh` starts it and that the kit's
  `SANDBOX.md` describes it. It would cost a few lines. Without it a reader finds `sandbox.sh` at the root, whose
  comment names the two documents of the kit
* A unit run in the sandbox as part of the verification, such as `./sandbox.sh claude 0007`. It would show that a
  harness finds its role after the move. It would cost a login of Claude Code in a new home volume and a run that
  cannot build the plugin, for the reasons unit 0002 gives. Without it requirement 10 shows that the image builds
  and the sandbox starts, and the first unit run after the move shows the rest
