plugins {
    id("org.jetbrains.kotlin.jvm")
    `java-test-fixtures`
}

val sherpaOnnxVersion = "2.0.2"
val piperPhonemizeVersion = "1.6.1"
val commonmarkVersion = "0.30.1"

base {
    archivesName = "aloudrite-core"
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
    // The IntelliJ Platform provides the Kotlin standard library to the plugin, so the archive of the plugin carries none.
    // Its own tests, which run without the platform, need it on their classpath
    compileOnly(kotlin("stdlib"))
    testFixturesCompileOnly(kotlin("stdlib"))
    testImplementation(kotlin("stdlib"))
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

tasks.test {
    // The path of the published archive of piper-phonemize, with the data of eSpeak NG, for the test that compares the
    // phonemes with that data and with the data of the model folder. It is resolved only when the tests run
    jvmArgumentProviders += CommandLineArgumentProvider {
        listOf("-Daloudrite.test.publishedPiperPhonemizeJvm=${publishedPiperPhonemizeJvm.singleFile}")
    }

    // The tests read the samples/ folder and write the build/ folder of the repository, by paths from its root
    workingDir = rootProject.projectDir
}

// The check of the sources, which asks Hugging Face whether it still serves the commits of the speech models. It is a source
// set of its own, so that the plugin archive does not carry it, and no task of `test` or `build` runs it, so that neither
// needs the network
val sourcesCheck: SourceSet = sourceSets.create("sourcesCheck") {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}

dependencies {
    "sourcesCheckCompileOnly"(kotlin("stdlib"))
    "sourcesCheckRuntimeOnly"(kotlin("stdlib"))
    testImplementation(sourcesCheck.output)
}

tasks.register<JavaExec>("checkModelSources") {
    group = "verification"
    description = "Checks that Hugging Face still serves the commits of the speech models that the plugin downloads"
    classpath = sourcesCheck.runtimeClasspath
    mainClass = "com.ritenoar.aloudrite.sources.ModelSourcesCheckKt"
}
