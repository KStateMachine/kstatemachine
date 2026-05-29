plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
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
