plugins {
    kotlin("multiplatform")
    `java-library`
    ru.nsk.`maven-publish`
    ru.nsk.jacoco
    id("org.jetbrains.dokka") version Versions.kotlinDokka
}

group = rootProject.group
version = rootProject.version

kotlin {
    jvmToolchain(Versions.jdkVersion)
    sourceSets.all {
        languageSettings.apply {
            languageVersion = Versions.languageVersion
            apiVersion = Versions.apiVersion
        }
    }

    jvm {}
    ios()
    iosSimulatorArm64()
}
