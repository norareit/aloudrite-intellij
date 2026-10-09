---
kind: feature
status: draft
timestamp: 2026-10-09T20:51:16Z
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

## Amendments
