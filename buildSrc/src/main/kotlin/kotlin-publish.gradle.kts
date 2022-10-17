import org.gradle.api.plugins.JavaBasePlugin.DOCUMENTATION_GROUP

plugins {
    id("org.jetbrains.dokka")
    signing
    `maven-publish`
}

val projectName = "QuickSeq"
val projectDescription = "QuickSeq"
val projectUrl = "github.com/jon-kroko/quickseq"

val dokkaJar by tasks.creating(Jar::class) {
  group = DOCUMENTATION_GROUP
  description = "Assembles Kotlin docs with Dokka"
  archiveClassifier.set("javadoc")
  from(tasks["dokkaHtml"])
}

val copyJavadocToTopLevel by tasks.registering {
    group = DOCUMENTATION_GROUP
    description = "Copies code documentation into a top-level folder of the project (\"javadoc\")"
    doFirst {
        val sourceDir = File(file(".").absolutePath + "/build/dokka")
        val targetDir = File(file(".").absolutePath + "/../javadoc")
        if (!targetDir.exists()) targetDir.mkdirs()
        else targetDir.apply {
            deleteRecursively()
            mkdirs()
        }
        sourceDir.copyRecursively(targetDir)
    }
}

tasks.dokkaHtml {
    setFinalizedBy(listOf(copyJavadocToTopLevel))
}

signing {
    val signingKey = findProperty("signingKey") as? String
    val signingPassword = (findProperty("signingPassword") as? String).orEmpty()
    if (signingKey != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
    }

    setRequired(provider { gradle.taskGraph.hasTask("publish") })
    sign(publishing.publications)
}

publishing {
    publications.configureEach {
        if (this !is MavenPublication) return@configureEach

        artifact(dokkaJar)

        pom {
            name.set(projectName)
            description.set(projectDescription)
            url.set("https://$projectUrl")

            licenses {
                license {
                    name.set("The Apache License, Version 2.0")
                    url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }
            scm {
                url.set("https://$projectUrl")
                connection.set("scm:git:git://$projectUrl.git")
            }
            developers {
                developer {
                    name.set("Kira Weinlein")
                    url.set("https://github.com/jon-kroko")
                }
            }
        }
    }

    repositories {
        if (hasProperty("sonatypeUsername") && hasProperty("sonatypePassword")) {
            maven {
                setUrl(
                    if ("SNAPSHOT" in version.toString()) "https://oss.sonatype.org/content/repositories/snapshots"
                    else "https://oss.sonatype.org/service/local/staging/deploy/maven2"
                )
                credentials {
                    username = property("sonatypeUsername") as String
                    password = property("sonatypePassword") as String
                }
            }
        }
    }
}
