plugins {
    kotlin("jvm") version Versions.kotlin apply false
    id("org.jetbrains.dokka") version Versions.kotlinDokka
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version Versions.kotlinBinaryCompatibilityValidatorPlugin
}

group = Versions.libraryMavenCentralGroup
version = Versions.libraryVersion

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