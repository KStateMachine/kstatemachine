import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import java.util.*

plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.maven.publish) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.dokka)
    alias(libs.plugins.binary.compatibility.validator)
    alias(libs.plugins.kover)
}

group = "io.github.nsk90"
version = libs.versions.library.get()

allprojects {
    repositories {
        mavenCentral()
    }
}

apiValidation {
    /**
     * Sub-projects that are excluded from API validation
     */
    ignoredProjects.addAll(listOf("samples", "tests"))

    /**
     * Set of annotations that exclude API from being public.
     * Typically, it is all kinds of `@InternalApi` annotations that mark
     * effectively private API that cannot be actually private for technical reasons.
     */
    nonPublicMarkers.add("ru.nsk.kstatemachine.VisibleForTesting")
}

dependencies {
    dokka(project(":kstatemachine"))
    dokka(project(":kstatemachine-coroutines"))
    dokka(project(":kstatemachine-serialization:"))
}

/**
 * Local configuration for signing is stored in local.properties file that is not under vcs.
 * local.properties file structure sample:
 *
 * signing.gnupg.executable=gpg2
 * # last 8 digits of key ID (gpg2 --list-keys)
 * signing.gnupg.keyName=AABBCCDD
 * signing.gnupg.passphrase=secret1
 *
 * Maven Central Portal user token is passed as gradle properties mavenCentralUsername / mavenCentralPassword
 * (~/.gradle/gradle.properties or ORG_GRADLE_PROJECT_ env variables).
 * In CI signing key is passed as signingInMemoryKey / signingInMemoryKeyPassword gradle properties.
 */
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) load(file.reader())
}

subprojects {
    plugins.withId("com.vanniktech.maven.publish") {
        val resolvedGroupId = if (project.group == "com.github.nsk90")
            project.group.toString() // JitPack passes this as arguments
        else
            rootProject.group.toString()

        configure<MavenPublishBaseExtension> {
            configure(KotlinMultiplatform(javadocJar = JavadocJar.Dokka("dokkaGeneratePublicationHtml")))
            coordinates(resolvedGroupId, project.name, rootProject.version.toString())

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

            if (resolvedGroupId == "io.github.nsk90") {
                // Uploads all publications as a single bundle via Central Portal API.
                // publishAndReleaseToMavenCentral task (used in CI) also releases it automatically,
                // publishToMavenCentral leaves the deployment for manual release/drop in the UI.
                publishToMavenCentral()
            }

            val executable: String? = localProperties.getProperty("signing.gnupg.executable")
            if (executable != null) {
                extra["signing.gnupg.executable"] = executable
                localProperties.getProperty("signing.gnupg.keyName")?.let { extra["signing.gnupg.keyName"] = it }
                localProperties.getProperty("signing.gnupg.passphrase")?.let { extra["signing.gnupg.passphrase"] = it }
                signAllPublications() // applies signing plugin
                configure<SigningExtension> { useGpgCmd() }
            } else if (project.findProperty("signingInMemoryKey") != null) { // GitHub flow
                signAllPublications()
            }
        }
    }
}
