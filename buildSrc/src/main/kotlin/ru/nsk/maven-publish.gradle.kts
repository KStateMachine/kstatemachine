package ru.nsk

import Versions
import java.util.*

plugins {
    java
    `maven-publish`
    signing
}

/**
 * Local configuration with credentials is stored in local.properties file that is not under vcs.
 * local.properties file structure sample:
 *
 * signing.gnupg.executable=gpg2
 * signing.gnupg.keyName=AABBCCDD # last 8 digits of key ID (gpg2 --list-keys)
 * signing.gnupg.passphrase=secret1
 * mavenUsername=accountName
 * mavenPassword=secret2
 */
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) load(file.reader())
}

afterEvaluate {
    tasks.create<Jar>("javadocJar") {
        archiveClassifier.set("javadoc")
        from(tasks.named("dokkaHtml"))
    }
}

publishing {
    val resolvedGroupId = if (project.group == Versions.libraryJitPackGroup)
        project.group.toString() // JitPack passes this as arguments
    else
        rootProject.group.toString()

    // Publication is created by multiplatform plugin itself
    // this code references it and configures
    publications.withType<MavenPublication> {
        afterEvaluate {
            artifact(tasks.named("javadocJar"))
        }

        pom {
            name.set(project.name)
            description.set(
                "KStateMachine is a Kotlin DSL library for creating state machines and " +
                        "hierarchical state machines (statecharts)."
            )
            url.set("https://github.com/KStateMachine/kstatemachine")
            inceptionYear.set("2020")

            issueManagement {
                system.set("GitHub")
                url.set("https://github.com/KStateMachine/kstatemachine/issues")
            }
            licenses {
                license {
                    name.set("Boost Software License 1.0")
                    url.set("https://raw.githubusercontent.com/KStateMachine/kstatemachine/master/LICENSE")
                    distribution.set("repo")
                }
            }
            developers {
                developer {
                    id.set("nsk")
                    name.set("Mikhail Fedotov")
                    email.set("nosik90@gmail.com")
                    url.set("https://github.com/nsk90")
                }
            }
            scm {
                url.set("https://github.com/KStateMachine/kstatemachine")
                connection.set("scm:git:git://github.com/KStateMachine/kstatemachine.git")
                developerConnection.set("scm:git:ssh://git@github.com/KStateMachine/kstatemachine.git")
            }
        }
    }

    if (resolvedGroupId == Versions.libraryMavenCentralGroup) {
        repositories {
            maven {
                credentials {
                    val mavenUsername: String? by project
                    val mavenPassword: String? by project
                    username = localProperties.getProperty("mavenUsername", mavenUsername)
                    password = localProperties.getProperty("mavenPassword", mavenPassword)
                }
                url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2")
            }
        }
    }
}

val executable = localProperties.getProperty("signing.gnupg.executable")
if (executable != null) {
    ext.set("signing.gnupg.executable", executable)

    localProperties.getProperty("signing.gnupg.keyName")?.let { keyName ->
        ext.set("signing.gnupg.keyName", keyName)

        localProperties.getProperty("signing.gnupg.passphrase")?.let { passphrase ->
            ext.set("signing.gnupg.passphrase", passphrase)

            signing {
                useGpgCmd()
                sign(publishing.publications)
            }
        }
    }
} else { // try getting from environment (GitHub flow)
    val signingKey: String? by project
    val signingPassword: String? by project

    if (signingKey != null && signingPassword != null) {
        signing {
            useInMemoryPgpKeys(signingKey, signingPassword)
            sign(publishing.publications)
        }
    }
}

// workaround for gradle warning about task order. should be removed with gradle 8
val signingTasks = tasks.withType<Sign>()
tasks.withType<AbstractPublishToMaven>().configureEach {
    dependsOn(signingTasks)
}