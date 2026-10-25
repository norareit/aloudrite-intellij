---
kind: task
status: approved
timestamp: 2026-10-24T10:57:58Z
---

# 0023 Submit the Gradle dependencies to GitHub

## Description

GitHub builds the dependency graph of a repository by reading its manifest files, and it does not read
`build.gradle.kts`, because the dependencies of a Gradle build are only known once the build resolves them. The
dependency graph of `norareit/aloudrite-intellij` on GitHub therefore holds only the npm devDependencies, and none of
the dependencies of the plugin, such as sherpa-onnx and commonmark. Add a workflow of GitHub Actions that resolves them
and submits them to GitHub:

* Add `.github/workflows/dependency-submission.yml`, the first workflow of the project, which runs on every push to
  `master` and can be started by hand
* It checks the repository out with `actions/checkout@v7`, installs a JDK 25 of Temurin with `actions/setup-java@v6`,
  because the build compiles for Java 25, and runs `gradle/actions/dependency-submission@v6`, which resolves the
  dependencies with the project's Gradle wrapper and submits them
* The workflow is given the permission `contents: write`, the one the submission needs, and no other

Afterwards a run of the workflow on `master` succeeds, and the dependency graph of the repository on GitHub, under
Insights, lists `com.k2fsa.sherpa.onnx:sherpa-onnx-jvm` among the dependencies of the plugin.

Worth knowing before approving:

* Resolving every configuration also resolves the IntelliJ Platform that the plugin is built against, so each run
  downloads it on the runner. The runners of GitHub Actions are free for a public repository, so this costs time on
  the runner and nothing else
* The repository is also pushed to Bitbucket, where the workflow does nothing
* Whether sherpa-onnx then lists the project under "Used by" is not something the task can make true, and it is not
  among its outcomes. GitHub counts a repository there when it depends on a package of a registry that GitHub links
  to `k2-fsa/sherpa-onnx`, and the jars come from the release assets of that repository through an Ivy repository,
  not from such a registry

Not part of this task: Dependabot alerts or updates for the Gradle dependencies, and a workflow that builds or tests
the plugin.

## Log

- 2026-10-24T10:57:58Z [human] asked for a workflow that submits the dependencies of the Gradle build to the
  dependency graph of GitHub, after asking why the project is missing from "Used by" of sherpa-onnx
- 2026-10-24T10:57:58Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in a session on the Linux host, on
  `master` at `a8653fe`, switched to from `feature-0017-reading-shortcut` with no change uncommitted. Made the unit as a
  task: one workflow file whose description and diff say the same thing. Numbered 0023: the highest number in
  `changes/` is 0022. The latest releases, read from GitHub, are `gradle/actions` v6.4.0, `actions/checkout` v7.0.1
  and `actions/setup-java` v6.1.0, and the workflow names their major versions. The status is `draft`
- 2026-10-25T11:10:51Z [human] approved the record. The status is `approved`
- 2026-10-25T11:11:31Z [Claude Code, anthropic/claude-opus-5-5] as the planner, in a session on the Linux host, cut
  `task-0023-submit-the-gradle-dependencies-to-github` from `master` at `9b6e25e`. The task needs no plan: it is one
  new workflow file, whose triggers, actions, JDK and permission the description names, with nothing left to decide.
  The description still fits the tree: `build.gradle.kts` compiles for Java 25, the wrapper is Gradle 9.8.1, sherpa-onnx
  comes from the Ivy repository of its GitHub releases, the default branch is `master`, and `.github/` does not exist
  yet. The status stays `approved`

## Amendments
