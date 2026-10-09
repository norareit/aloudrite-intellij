---
kind: feature
status: planned
timestamp: 2026-10-09T21:13:00Z
---

# AgentRite 6.0.0 from the npm registry

## Log

- 2026-10-09T20:51:16Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `22550e6`. The human pointed to unit 0044 of the kit's repository and asked for the spec of
  the same move here, naming the package's move to the npm registry and `sandbox.sh` in the place of `agent.sh`.
  Read `process.md` and `roles/specifier.md` of the installed release 4.2.0, units 0043 and 0044 of the kit, and
  the sections 5.0.0 and 6.0.0 of its `CHANGELOG.md`. Made the unit as a feature, for the reason its spec gives,
  and numbered it 0008. Wrote `spec.md`. Looked at, to write it:
  * `npm view agentrite` gives `6.0.0` as the latest version on the registry
  * In an empty scratch directory with a `package.json` named `aloudrite-intellij`, with npm 11.19.1 and node
    26.9.0, `npm install --save-dev agentrite` wrote `^6.0.0` and `npx agentrite init claude opencode` wrote its
    files. `diff` of each against this project's file shows what requirements 5, 6 and 7 list and nothing else:
    the project changed nothing in what `init` of 4.2.0 wrote, but for the section at the end of `AGENTS.md`
  * With Docker 28.4.0 on this host, `docker volume ls` and `docker image ls` show no volume and no image of this
    project, which the decision on the name of the Compose project rests on

  Decided by the specifier and open to the human at approval: the range `^6.0.0` and not an exact version, the name
  `aloudrite-intellij-sandbox`, and the limits read from `SANDBOX_MEM_LIMIT` and `SANDBOX_CPUS`. Not established:
  whether the sandbox of this project was ever started on the human's Mac, and whether a shell anywhere sets
  `AGENT_MEM_LIMIT` or `AGENT_CPUS`. Not tried: any step of the move in this project or a copy of it, and anything
  that builds or starts a container. The status is `draft`
- 2026-10-09T21:06:44Z [human] read the spec and took part of what it left as beyond the request: the README says
  that AgentRite is used to build new features and that `AGENTS.md` holds the details, and not much more than that
- 2026-10-09T21:06:44Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, revised `spec.md` on that word. Requirement 11 is the section of `README.md`, and what was requirement 11
  is requirement 12, without `README.md` among the files that do not differ. Added the decision, reworded the
  non-goal on `README.md`, and removed the section on the agent sandbox from "Beyond the request", which the
  human's smaller one replaces. The link to the kit is its repository on GitHub, which is the specifier's choice.
  The status stays `draft`
- 2026-10-09T21:10:34Z [human] approved the spec. The status is `approved`
- 2026-10-09T21:13:00Z [Claude Code, anthropic/claude-opus-5-5] as the planner, in the main session on the Linux
  host. Read `process.md` and `roles/planner.md` of the installed release 4.2.0, the spec and this record, and the
  sections 5.0.0 and 6.0.0 of the `CHANGELOG.md` of release 6.0.0. Cut the branch
  `feature-0008-agentrite-6-from-the-npm-registry` from `master` at `03f7cf2` and wrote `plan.md` against it.
  Decided, as the spec leaves to the planner:
  * The order of the steps. `npm install --save-dev agentrite` comes before `npm uninstall @ritenoar/agentrite`,
    the other way round from the release notes. Tried on copies of `package.json` and `package-lock.json` in a
    scratch directory, with npm 11.19.1 and node 26.9.0: the order of the release notes moves the key
    `devDependencies` behind `scripts`, which requirement 1 does not allow, and the order of the plan leaves it in
    place. `package-lock.json` comes out byte for byte the same either way
  * The section of `README.md` is the last one of the file, with the heading "Development", and the plan gives its
    two lines of text

  The files are compared with what `init` of release 6.0.0 writes in an empty directory, which the implementer
  makes again as task 1 of the plan. Seen besides: `refresh` run a second time prints `up to date` for its nine
  files, the role wrappers of this project hold no `model:` line, `agent/` holds nothing but the compose file, and
  `docker info` exits 0 in this session, so the implementer can run requirement 10 on this host. Found nothing in
  the spec that is unclear or that does not fit the tree. Not tried: any step in this project itself, `refresh`
  over the wrappers of 4.2.0, the build of the image and the start of the sandbox. The status is `planned`

## Amendments
