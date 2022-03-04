package ru.nsk

import java.util.*

plugins {
    java
    `maven-publish`
}

java {
    withSourcesJar()
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) load(file.reader())
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = "io.github.nsk90"
            artifactId = "kstatemachine"
            version = rootProject.version as String

            from(components["java"])

            pom {
                name.set(rootProject.name)
                description.set(
                    "KStateMachine is a Kotlin DSL library for creating finite state machines (FSM) " +
                            "and hierarchical state machines (HSM)."
                )
                url.set("https://github.com/nsk90/kstatemachine")
                inceptionYear.set("2020")

                issueManagement {
                    system.set("GitHub")
                    url.set("https://github.com/nsk90/kstatemachine/issues")
                }
                licenses {
                    license {
                        name.set("Boost Software License 1.0")
                        url.set("https://github.com/nsk90/kstatemachine/blob/master/LICENSE")
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
                    url.set("https://github.com/nsk90/kstatemachine")
                    connection.set("scm:git:git://github.com/nsk90/kstatemachine.git")
                    developerConnection.set("scm:git:ssh://git@github.com/nsk90/kstatemachine.git")
                }
            }

            repositories {
                maven {
                    credentials {
                        username = localProperties.getProperty("mavenUsername", "")
                        password = localProperties.getProperty("mavenPassword", "")
                    }
                    url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2")
                }
            }
        }
    }
}