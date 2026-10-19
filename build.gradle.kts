import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.4.21"
    id("org.jetbrains.intellij.platform") version "2.19.0"
}

group = "com.ritenoar"
version = "0.0.1"

val sherpaOnnxVersion = "1.13.8"
val commonmarkVersion = "0.30.1"
val archiveSizeLimitInBytes = 30_000_000L

repositories {
    mavenCentral()
    // The jars of sherpa-onnx are assets of its GitHub releases and are not on Maven Central
    exclusiveContent {
        forRepository {
            ivy {
                url = uri("https://github.com/k2-fsa/sherpa-onnx/releases/download")
                patternLayout {
                    artifact("v[revision]/[artifact]-[revision].[ext]")
                }
                metadataSources {
                    artifact()
                }
            }
        }
        filter {
            includeGroup("com.k2fsa.sherpa.onnx")
        }
    }
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdea("2026.2.3")
        testFramework(TestFrameworkType.Platform)
        testBundledPlugin("org.intellij.plugins.markdown")
    }
    implementation("com.k2fsa.sherpa.onnx:sherpa-onnx-jvm:$sherpaOnnxVersion")
    implementation("com.k2fsa.sherpa.onnx:sherpa-onnx-native-lib-linux-x64:$sherpaOnnxVersion")
    implementation("com.k2fsa.sherpa.onnx:sherpa-onnx-native-lib-osx-aarch64:$sherpaOnnxVersion")
    implementation("org.commonmark:commonmark:$commonmarkVersion")
    implementation("org.commonmark:commonmark-ext-gfm-strikethrough:$commonmarkVersion")
    implementation("org.commonmark:commonmark-ext-gfm-tables:$commonmarkVersion")
    implementation("org.commonmark:commonmark-ext-task-list-items:$commonmarkVersion")
    implementation("org.commonmark:commonmark-ext-autolink:$commonmarkVersion")
    testImplementation("junit:junit:4.13.2")
}

// The plugin runs in the JDK 25 of IntelliJ IDEA 2026.2, so the bytecode stays at Java 25 whichever JDK runs the build.
java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "262"
        }
    }
}

tasks {
    prepareSandbox {
        from(layout.projectDirectory.files("LICENSE", "THIRD-PARTY.md")) {
            into(pluginName)
        }
    }

    buildPlugin {
        doLast {
            val archiveSizeInBytes = archiveFile.get().asFile.length()
            check(archiveSizeInBytes <= archiveSizeLimitInBytes) {
                "The plugin archive is $archiveSizeInBytes bytes, more than the limit of $archiveSizeLimitInBytes bytes"
            }
        }
    }
}
