# Say that the plugin is made with AgentRite

## Approach

The record of this task states its requirements as a list and a paragraph. The plan numbers them so that each task
can name what it delivers:

* R1: `README.md` has a short section at its top, directly after the logo and the rule below it and before the first
  paragraph of the description, that says the plugin is made with AgentRite, links "AgentRite" to
  https://agentrite.dev, says in one sentence what the kit does as its site states it, and points to `changes/` and to
  `AGENTS.md`
* R2: the `<description>` of `plugin.xml` keeps its present sentence as its first paragraph and has a second that says
  the plugin is made with AgentRite, with "AgentRite" linked to https://agentrite.dev, written as HTML within a `CDATA`
  section
* R3: the paragraph under "Development" in `README.md` keeps its link to the repository of the kit and has the link to
  https://agentrite.dev beside it
* R4: afterwards `./gradlew buildPlugin` succeeds, and the `plugin.xml` in the built archive holds the link

The tree fits the record. `README.md` opens with `![lockup.svg](assets/lockup.svg)`, a blank line and `---`, followed
by the paragraph that begins "AloudRite is an IntelliJ IDEA plugin". The paragraph under "## Development" names
AgentRite with a link to https://github.com/norareit/agentrite. The `<description>` of `plugin.xml` is the one line
"Reads Markdown and agentic coding documents aloud inside the IDE.", and `build.gradle.kts` sets no description in
`pluginConfiguration`, so the one in `plugin.xml` is the one the archive carries.

The sentence on what the kit does follows the site, which describes AgentRite as running coding agents by a fixed
process, where the agents specify, plan, build and review every change and the human approves and decides. The change
touches only text and no code, so no unit test is added; `./gradlew test` is run to show that nothing else moved.

## Files

- `README.md`: a new section between the rule below the logo and the first paragraph of the description, and the
  link to https://agentrite.dev in the paragraph under "Development"
- `src/main/resources/META-INF/plugin.xml`: the `<description>` as a `CDATA` section of two HTML paragraphs

## Tasks

- [x] In `README.md`, after the `---` below the logo and before the paragraph that begins "AloudRite is an IntelliJ IDEA
  plugin", add a section with a level-two heading "Made with AgentRite". Its text says that the plugin is made with
  [AgentRite](https://agentrite.dev); says in one sentence that AgentRite runs coding agents by a fixed process, in
  which the agents specify, plan, build and review every change and the human approves and decides; and says that the
  change units of the plugin are kept in [`changes/`](changes/) and that [AGENTS.md](AGENTS.md) holds how the agents
  work here. Keep it to these sentences, wrap lines at 120 characters as the rest of the file does, and use no em
  dash. Follow the section with a `---` rule, so that the description below it starts as it did after the logo (R1)
- [x] In the paragraph under "## Development" in `README.md`, keep
  `[AgentRite](https://github.com/norareit/agentrite)` and add the site beside it, as "New features of this project
  are built with [AgentRite](https://agentrite.dev), whose repository is
  [norareit/agentrite](https://github.com/norareit/agentrite)." Leave the sentence that points to `AGENTS.md` and the
  paragraph on the sandbox as they are (R3)
- [x] In `src/main/resources/META-INF/plugin.xml`, replace the one-line `<description>` with a `CDATA` section holding
  two paragraphs: `<p>Reads Markdown and agentic coding documents aloud inside the IDE.</p>`, the present sentence
  unchanged, and `<p>Made with <a href="https://agentrite.dev">AgentRite</a>, by which coding agents specify, plan,
  build and review every change and the human approves and decides.</p>`. Keep the indentation of the other elements
  of the file (R2)

## Verification

- [x] `./gradlew buildPlugin` succeeds (R4)
- [x] `unzip -p build/distributions/aloudrite-intellij-0.0.1.zip 'aloudrite-intellij/lib/aloudrite-intellij-0.0.1.jar'
  > /tmp/aloudrite-0034.jar && unzip -p /tmp/aloudrite-0034.jar META-INF/plugin.xml | grep -n 'agentrite.dev'` shows
  the link in the `<description>`. Where the archive or the jar has another name, take the names from `unzip -l` of
  the archive and record them in `record.md` (R2, R4)
- [x] `grep -n 'agentrite.dev\|github.com/norareit/agentrite' README.md` shows the link to the site in the section at
  the top and both links in the paragraph under "Development", and `sed -n 1,15p README.md` shows the section between
  the rule below the logo and the first paragraph of the description (R1, R3)
- [x] `grep -c '—' README.md` gives the same count as on `master`, so the new text adds no em dash (R1)
- [x] `./gradlew test` passes. The tests with the real engine need not run, since the task changes neither the speech
  engine, sherpa-onnx nor the text handed to the engine
- [ ] `npm run check-changes` passes once the work is committed

## Commit Points

- [x] `task-0034 Plan the task`: `plan.md` and the log entry in `record.md`
- [ ] `task-0034 Say that the plugin is made with AgentRite`: `README.md` and `plugin.xml`, with the ticked tasks and
  the log entry in `record.md`
