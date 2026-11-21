---
kind: task
status: approved
timestamp: 2026-11-21T11:40:00Z
---

# 0034 Say that the plugin is made with AgentRite

## Description

The plugin is built with AgentRite, the kit by which coding agents specify, plan, build and review every change and
the human approves and decides. `README.md` says so only under "Development", near its end, with a link to the
repository of the kit, and the description the JetBrains Marketplace shows says nothing of it. Make it prominent in
both, with a link to https://agentrite.dev:

* `README.md` gets a short section at its top, directly after the logo and the rule below it and before the first
  paragraph of the description, that says the plugin is made with AgentRite, links "AgentRite" to
  https://agentrite.dev, says in one sentence what the kit does as its site states it, and points to `changes/`,
  where the change units of the plugin are kept, and to `AGENTS.md`
* The `<description>` of `src/main/resources/META-INF/plugin.xml`, which the Marketplace shows as the description of
  the plugin, keeps its present sentence as its first paragraph and gets a second that says the plugin is made with
  AgentRite, with "AgentRite" linked to https://agentrite.dev. The description is written as HTML within a `CDATA`
  section, which the Marketplace takes for a description
* The paragraph under "Development" in `README.md` keeps its link to the repository of the kit and gets the link to
  https://agentrite.dev beside it, so that both places name the same address for the kit

Afterwards `./gradlew buildPlugin` succeeds, and the `plugin.xml` in the built archive holds the link. How the
Marketplace renders the description is seen only once a release is uploaded, which is not part of this task.

Not part of this task: a change to the name, the vendor or the logo of the plugin, the change notes of a release, and
the page of the plugin on the Marketplace itself, which is made when the plugin is first published.

## Log

- 2026-11-21T11:40:00Z [human] asked for a prominent part at the top of `README.md` saying that the plugin is made with
  AgentRite, with a link to agentrite.dev, and for the description the Marketplace shows to mention AgentRite as well
- 2026-11-21T11:40:00Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `bef121b`, read `process.md`, the standards and `roles/specifier.md` of the installed kit,
  8.4.0. Made the unit a task: two passages of text and a link, in the two places the human named. Numbered it 0034,
  after 0033. Found that the description the Marketplace shows is the `<description>` of `plugin.xml`, as the build
  sets no other, and that https://agentrite.dev answers and describes the kit as running coding agents by a fixed
  process, where the agents specify, plan, build and review every change and the human approves and decides. The
  status is `draft`
- 2026-11-21T12:52:02Z [human] approved the record. The status is `approved`
- 2026-11-21T13:05:00Z [Claude Code, anthropic/claude-opus-5-5] as the planner, on
  `task-0034-say-that-the-plugin-is-made-with-agentrite` at `bd39500`, read `process.md`, the standards and
  `roles/planner.md` of the installed kit, and found that the tree fits the record. Wrote `plan.md`, which numbers the
  requirements of the record R1 to R4 and gives each a task or a check. The status stays `approved`

## Amendments
