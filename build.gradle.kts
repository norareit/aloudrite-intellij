import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.4.21"
    id("org.jetbrains.intellij.platform") version "2.19.0"
}

val archiveSizeLimitInBytes = 30_000_000L

allprojects {
    group = "com.ritenoar"
    version = "1.0.0"

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

dependencies {
    intellijPlatform {
        intellijIdea("2026.2.3")
        testFramework(TestFrameworkType.Platform)
        bundledPlugin("org.intellij.plugins.markdown")
    }
    implementation(project(":core"))
    testImplementation(testFixtures(project(":core")))
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "262"
        }
    }
    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }
    pluginVerification {
        ides {
            recommended()
        }
    }
}

tasks {
    prepareSandbox {
        from(layout.projectDirectory.files("LICENSE", "THIRD-PARTY.md", "EULA.md", "PRIVACY.md")) {
            into(pluginName)
        }
    }

    signPlugin {
        // The signer takes the key and its password as arguments, which Gradle logs at these two levels
        doFirst {
            check(!logger.isInfoEnabled) { "signPlugin refuses --info and --debug, which would log the key and its password" }
        }
    }

    // Without the dependency, Gradle refuses to run signPlugin and verifyPluginSignature in one invocation
    verifyPluginSignature {
        dependsOn(signPlugin)
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
