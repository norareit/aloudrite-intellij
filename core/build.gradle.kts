plugins {
    id("org.jetbrains.kotlin.jvm")
    `java-test-fixtures`
}

base {
    archivesName = "aloudrite-core"
}

dependencies {
    // The IntelliJ Platform provides the Kotlin standard library to the plugin, so the archive of the plugin carries none.
    // Its own tests, which run without the platform, need it on their classpath
    compileOnly(kotlin("stdlib"))
    testFixturesCompileOnly(kotlin("stdlib"))
    testImplementation(kotlin("stdlib"))
}

tasks.test {
    // The tests read the samples/ folder and write the build/ folder of the repository, by paths from its root
    workingDir = rootProject.projectDir
}
