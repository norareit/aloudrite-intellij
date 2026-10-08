# AloudRite

AloudRite is meant to become an IntelliJ IDEA plugin that reads Markdown and agentic coding documents aloud.

It does not read anything yet. What is here is the smallest plugin that proves the whole chain works: it builds with
Gradle, loads in an IDE, and adds one action, "Say Hello" in the Tools menu, which shows a dialog with the text
"Hello, world!". The action is a placeholder that the first real feature removes.

The plugin is written in Kotlin, is built with the [IntelliJ Platform Gradle Plugin][gradle-plugin], and targets
IntelliJ IDEA 2026.2. It depends on nothing beyond the platform itself, so it loads in any IDE built on the IntelliJ
Platform.

[gradle-plugin]: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html

## Requirements

JDK 25 is the only thing that has to be installed. The Gradle wrapper in the repository downloads Gradle itself.

The first build also downloads IntelliJ IDEA, about 1.5 GB, to compile against and to run the plugin in. It is kept
in the Gradle user home and in `.intellijPlatform/`, which git ignores.

## Building

```sh
./gradlew buildPlugin
```

This builds the installable plugin archive, `build/distributions/aloudrite-intellij-0.0.1.zip`.

## Running in a sandbox IDE

```sh
./gradlew runIde
```

This starts a separate IntelliJ IDEA with the plugin loaded. It keeps its settings, caches and plugins in a sandbox
under `.intellijPlatform/sandbox/`, which git ignores, so the IDE installed on the machine and its settings are left
alone.

## Testing

```sh
./gradlew test
```

This runs the automated tests inside the test environment of the platform, without opening an IDE window.

## Installing in an IDE

1. Build the archive with `./gradlew buildPlugin`
2. In the IDE, open Settings, then Plugins
3. Click the gear icon and choose "Install Plugin from Disk..."
4. Select `build/distributions/aloudrite-intellij-0.0.1.zip` and restart the IDE when it asks

The archive installs in IDEs of version 2026.2 and later.
