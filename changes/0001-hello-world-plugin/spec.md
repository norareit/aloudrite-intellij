# Hello world plugin

## Context

The project is meant to become an IntelliJ IDEA plugin that reads Markdown and agentic coding documents aloud. So far
the repository holds only the AgentRite setup, a `package.json`, the notes in `idea.md` and a Python experiment. There
is no plugin, no build and no `README.md`, although `AGENTS.md` names the README as the source of truth for the
project.

This unit adds the smallest plugin that proves the whole chain works: it builds with Gradle, loads in an IDE, shows
something on screen, and can be installed in the developer's own IntelliJ IDEA without being released. Everything
later is built on that skeleton.

The developer runs IntelliJ IDEA 2026.2 and has JDK 25 installed. The plugin is intended to be open sourced and
published later, which is why the officially supported Gradle build was chosen over Maven.

## Requirements

1. The repository contains a Gradle build, using the IntelliJ Platform Gradle Plugin, that produces an installable
   plugin archive from a fresh clone. The Gradle wrapper is committed, so that a JDK is the only thing that has to be
   installed beforehand.
2. The build compiles against IntelliJ IDEA 2026.2, and the archive it produces installs in IntelliJ IDEA 2026.2
   through "Install Plugin from Disk" without a compatibility error.
3. The plugin declares the id `com.ritenoar.aloudrite`, the name `AloudRite` and the vendor `Ritenoar`, and depends
   on nothing beyond the platform itself, so that it loads in any IDE built on the IntelliJ Platform.
4. With the plugin loaded, the Tools menu contains an action labelled "Say Hello". Invoking it shows an information
   dialog with the text "Hello, world!".
5. The version of the built plugin is `0.0.1`. It is stated once, in the Gradle build configuration, and
   `package.json` has no `version` field.
6. The plugin can be started in a sandbox IDE with one documented command, without touching the developer's own IDE
   installation or settings.
7. An automated test invokes the action and asserts the text of the message it shows. The test runs with one
   documented command and passes.
8. Nothing the Gradle build generates or downloads is tracked by git or reported by `git status` after a build, a
   sandbox run and a test run.
9. The contents of the plugin archive come from the plugin's own sources only. Nothing from `node_modules`, the
   AgentRite files, `idea.md` or the Python experiment is in it.
10. A `README.md` exists and states what the project is, what is needed to build it, and the commands that build the
    archive, start the sandbox IDE, and run the tests, together with how to install the archive in an IDE.
11. `AGENTS.md` states, under its project specific section, how the tests of this project are run, by pointing to
    the README and not by repeating the commands.

## Decisions

- **Kotlin, not Java.** `idea.md` already describes the plugin as a Kotlin plugin, Kotlin is what the platform's own
  documentation and examples use, and the Gradle build script is Kotlin either way. Java was rejected because it
  would put two languages in a project that needs one.
- **Kotlin DSL for the build script.** It is what the IntelliJ Platform Gradle Plugin documents, so examples can be
  followed without translation. Groovy was rejected for that reason alone.
- **The target platform is IntelliJ IDEA 2026.2.** It is the version the developer runs, and requirement 2 is only
  checkable against an IDE that is actually installed. The Java level the code is compiled for follows from what
  that platform version requires and is not chosen separately. Targeting an older version to reach more users was
  rejected: there are no users yet, and the range is cheap to widen when the plugin is published.
- **There is no upper bound on the compatible IDE versions.** The plugin declares 2026.2 as the oldest version it
  supports and no newest one, which is what the IntelliJ Platform Gradle Plugin does when the build states no upper
  bound. The plugin therefore keeps installing after the developer upgrades the IDE. Limiting it to the 2026.2 line
  was rejected: the IDE would refuse the plugin after the next major upgrade, and a placeholder that uses one action
  and one dialog gives no reason to expect an incompatibility. An earlier version of this spec listed the missing
  upper bound under "Beyond the request", on the assumption that the build limits the plugin to the 2026.2 line by
  default. The built archive showed that assumption to be false, and the human then chose to keep the range
  unbounded.
- **The version lives in the Gradle build, and `package.json` has none.** The Gradle build is what produces the
  plugin, and it is where the platform's tooling for building and later publishing expects the version. The
  `package.json` exists only to install AgentRite, is marked private and is never published with npm, and npm needs
  a `version` only for a package that is published. Reading the version from `package.json` in the Gradle build was
  rejected: it makes a file that is about the agent kit decide something about the plugin, and adds JSON parsing to
  the build for no gain. Keeping the number in both files was rejected because the two would drift.
- **The name is "AloudRite".** It puts the plugin in one family with AgentRite and Ritenoar, it contains the word
  that says what the plugin does, and it reads as "a loud rite", which is a pun the developer wants. "TTS Reader"
  was rejected because it names the technology and not what the user gets, and "Read Aloud" because it is generic
  and belongs to no family. Markdown is deliberately not in the name: handling Markdown well is a quality of the
  reading and not its boundary, so the description carries it and the name stays right whatever the plugin reads
  later.
- **The id `com.ritenoar.aloudrite` and the base package of the same name.** The id follows the reverse domain
  convention with the developer's domain, and ends in the plugin's name so that it stays right if Ritenoar publishes
  a second plugin. It is the one value here that cannot be changed once the plugin is published on the Marketplace,
  so it is fixed now rather than left to the implementation. Nothing is published in this unit, so it can still be
  changed until then.
- **The vendor is "Ritenoar".** It is the developer's company, and it is the name the plugin id, the domain and the
  AgentRite package already carry, so the plugin shows one party and not two. The developer's own name and the
  pseudonym "Nora Reit" were rejected: either one next to a `com.ritenoar` id reads as a different party, and a
  company name stays right whichever of the two the plugin is later promoted under. The vendor can be changed at any
  time.
- **The hello action is a placeholder.** It exists to prove that the plugin loads and can register something
  visible. An action in the Tools menu was chosen over a tool window, a notification at startup or a status bar
  widget because it is the smallest contribution that the user triggers deliberately, which makes both the manual
  check and the automated test unambiguous. The first unit that adds real behavior removes it.
- **The automated test invokes the action inside the platform's test environment.** That exercises the registration
  in the plugin descriptor, the action class and the message together, which is everything this unit adds. The
  dialog appearing in a real IDE window is checked by hand in the sandbox IDE once, as part of the review. See
  "Beyond the request" for the alternative.
- **Existing files are not moved or removed.** `idea.md`, the Python experiment and the AgentRite files stay where
  they are. The unit adds to the repository and reorganizes nothing.

## Non-goals

- Any text-to-speech behavior: no audio, no model, no voices, no Markdown processing, no highlighting.
- Publishing: no Marketplace listing, no plugin signing, no release workflow, no license choice.
- A plugin icon, a settings page, a keyboard shortcut for the action, or translations.
- Continuous integration.
- Support for IDE versions older than 2026.2.
- A Maven build.

## Beyond the request

- **Running the plugin verifier as part of the build.** It checks the archive against a set of IDE versions for
  binary compatibility problems. It costs a download of each IDE version checked and several minutes per run. Without
  it, an incompatibility shows up as a compile error or an error in the sandbox IDE, which is where this unit is
  checked anyway. It becomes worth having when the plugin is published.
- **A test that drives a real IDE window.** It would start a full IDE, click the menu item and read the dialog. It
  costs an additional test framework, a display in the environment that runs it, and a run time of a minute or more.
  Without it, the one thing left unchecked by automation is that the dialog is drawn, which the manual check in the
  sandbox IDE covers for a placeholder that the next unit removes.
- **A license file and repository metadata.** Both are needed before the plugin is open sourced. They cost a choice
  that only the developer can make. Without them nothing in this unit fails.
