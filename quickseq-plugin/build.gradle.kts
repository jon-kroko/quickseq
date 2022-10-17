import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("kapt")
    id("kotlin-publish")
    id("com.gradle.plugin-publish")
    id("java-gradle-plugin")
    id("com.github.gmazzo.buildconfig")
    id("org.jlleitschuh.gradle.ktlint") version "10.1.0"
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    api("org.jetbrains.kotlin:kotlin-compiler-embeddable")

    kapt("com.google.auto.service:auto-service:1.0-rc7")
    compileOnly("com.google.auto.service:auto-service-annotations:1.0-rc7")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.0")
    api("org.jetbrains.kotlin:kotlin-compiler-embeddable")
    testImplementation("com.github.tschuchortdev:kotlin-compile-testing:1.2.6")
    testImplementation("org.bitbucket.mstrobel:procyon-compilertools:0.5.36")

    implementation(kotlin("gradle-plugin-api"))
}

apply(plugin = "org.jlleitschuh.gradle.ktlint")

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

tasks.test {
    useJUnitPlatform()
}

tasks.compileTestKotlin {
    kotlinOptions {
        useIR = true
    }
}

tasks.kotlinSourcesJar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

buildConfig {
    val compilerPluginProject = project(":quickseq-plugin") // project(":plugin")
    packageName(compilerPluginProject.group.toString())
    buildConfigField("String", "KOTLIN_PLUGIN_ID", "\"${compilerPluginProject.group}.${compilerPluginProject.name}\"")
    buildConfigField("String", "KOTLIN_PLUGIN_GROUP", "\"${compilerPluginProject.group}\"")
    buildConfigField("String", "KOTLIN_PLUGIN_NAME", "\"${compilerPluginProject.name}\"")
    buildConfigField("String", "KOTLIN_PLUGIN_VERSION", "\"${compilerPluginProject.version}\"")
}

gradlePlugin {
    plugins {
        create("quickseq") {
            id = "com.kiwi.quickseq"
            displayName = "Kotlin QuickSeq compiler plugin"
            description = "Kotlin compiler plugin to easily create sequence diagrams from your code"
            implementationClass = "com.kiwi.quickseq.QuickSeqGradlePlugin"
        }
    }
}

pluginBundle {
    website = "https://github.com/Kira-We/quickseq"
    vcsUrl = "https://github.com/Kira-We/quickseq.git"
    tags = listOf("kotlin", "compiler-plugin", "uml", "plantuml", "sequence-diagram")
}

publishing {
    publications {
        create<MavenPublication>("default") {
            from(components["java"])
            artifact(org.gradle.api.internal.artifacts.dsl.LazyPublishArtifact(tasks.kotlinSourcesJar))
        }
    }
}
