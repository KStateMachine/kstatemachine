plugins {
    kotlin("multiplatform")
    `java-library`
    ru.nsk.`maven-publish`
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
    iosArm64()
    iosX64()
    iosSimulatorArm64()

    sourceSets {
        commonMain {
            dependencies {
                api(project(":kstatemachine"))

                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutinesCore}")
            }
        }
    }
}
