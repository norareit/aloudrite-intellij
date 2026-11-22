import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.4.21"
    id("org.jetbrains.intellij.platform") version "2.19.0"
}

val sherpaOnnxVersion = "2.0.2"
val piperPhonemizeVersion = "1.6.1"
val commonmarkVersion = "0.30.1"
val archiveSizeLimitInBytes = 30_000_000L

allprojects {
    group = "com.ritenoar"
    version = "0.0.1"

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
        // The same for piper-phonemize, which is published under another account
        exclusiveContent {
            forRepository {
                ivy {
                    url = uri("https://github.com/csukuangfj/piper-phonemize/releases/download")
                    patternLayout {
                        artifact("v[revision]/[artifact]-[revision].[ext]")
                    }
                    metadataSources {
                        artifact()
                    }
                }
            }
            filter {
                includeGroup("com.github.csukuangfj.piper.phonemize")
            }
        }
    }

    pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
        // The plugin runs in the JDK 25 of IntelliJ IDEA 2026.2, so the bytecode stays at Java 25 whichever JDK runs the build.
        extensions.configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_25
            targetCompatibility = JavaVersion.VERSION_25
        }
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
            compilerOptions {
                jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25
            }
        }
        dependencies {
            add("testImplementation", "junit:junit:4.13.2")
        }
    }
}

repositories {
    intellijPlatform {
        defaultRepositories()
    }
}

// The Java archive of piper-phonemize as published, with the data of eSpeak NG for every language. The plugin reads the
// data of the model folder instead, so the archive it carries is made without it, see stripPiperPhonemizeJvm
val publishedPiperPhonemizeJvm: Configuration = configurations.create("publishedPiperPhonemizeJvm") {
    isTransitive = false
}

val stripPiperPhonemizeJvm = tasks.register<Zip>("stripPiperPhonemizeJvm") {
    archiveFileName = "piper-phonemize-jvm-$piperPhonemizeVersion-without-espeak-ng-data.jar"
    destinationDirectory = layout.buildDirectory.dir("piper-phonemize")
    from(provider { zipTree(publishedPiperPhonemizeJvm.singleFile) }) {
        exclude("piper-phonemize/espeak-ng-data/**")
    }
}

dependencies {
    intellijPlatform {
        intellijIdea("2026.2.3")
        testFramework(TestFrameworkType.Platform)
        bundledPlugin("org.intellij.plugins.markdown")
    }
    implementation(project(":core"))
    testImplementation(testFixtures(project(":core")))
    implementation("com.k2fsa.sherpa.onnx:sherpa-onnx-jvm:$sherpaOnnxVersion")
    implementation("com.k2fsa.sherpa.onnx:sherpa-onnx-native-lib-linux-x64:$sherpaOnnxVersion")
    implementation("com.k2fsa.sherpa.onnx:sherpa-onnx-native-lib-osx-aarch64:$sherpaOnnxVersion")
    publishedPiperPhonemizeJvm("com.github.csukuangfj.piper.phonemize:piper-phonemize-jvm:$piperPhonemizeVersion")
    implementation(files(stripPiperPhonemizeJvm.map { it.archiveFile }))
    implementation("com.github.csukuangfj.piper.phonemize:piper-phonemize-native-lib-linux-x64:$piperPhonemizeVersion")
    implementation("com.github.csukuangfj.piper.phonemize:piper-phonemize-native-lib-osx-aarch64:$piperPhonemizeVersion")
    implementation("org.commonmark:commonmark:$commonmarkVersion")
    implementation("org.commonmark:commonmark-ext-gfm-strikethrough:$commonmarkVersion")
    implementation("org.commonmark:commonmark-ext-gfm-tables:$commonmarkVersion")
    implementation("org.commonmark:commonmark-ext-task-list-items:$commonmarkVersion")
    implementation("org.commonmark:commonmark-ext-autolink:$commonmarkVersion")
    // Runs model.onnx itself for the durations of its tokens, in HighlightFollowingMeasurement, and ships with nothing
    testImplementation("com.microsoft.onnxruntime:onnxruntime:1.22.0")
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "262"
        }
    }
}

tasks {
    // The path of the published archive of piper-phonemize, with the data of eSpeak NG, for the test that compares the
    // phonemes with that data and with the data of the model folder. It is resolved only when the tests run
    test {
        jvmArgumentProviders += CommandLineArgumentProvider {
            listOf("-Daloudrite.test.publishedPiperPhonemizeJvm=${publishedPiperPhonemizeJvm.singleFile}")
        }
    }

    prepareSandbox {
        from(layout.projectDirectory.files("LICENSE", "THIRD-PARTY.md")) {
            into(pluginName)
        }
    }

    // Only the sandbox IDE shows the lines that the native code of sherpa-onnx writes per piece of a reading
    runIde {
        systemProperty("aloudrite.engine.debug", "true")
        // Lets the developer set by ear how long after its start the word is shown, without a build per value
        providers.gradleProperty("wordDelayMilliseconds").orNull?.let { systemProperty("aloudrite.word.delay.milliseconds", it) }
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
