plugins {
    kotlin("multiplatform")
    ru.nsk.`maven-publish`
    id("org.jetbrains.dokka")
    kotlin("plugin.serialization") version Versions.kotlin
    id("org.jetbrains.kotlinx.kover")
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
    macosX64()
    macosArm64()
    linuxX64()
    linuxArm64()
    mingwX64()
    js {
        browser()
        nodejs()
    }
    @Suppress("OPT_IN_USAGE") // this is alpha feature
    wasmJs {
        browser()
        nodejs()
    }

    applyDefaultHierarchyTemplate()
    sourceSets {
        commonMain {
            dependencies {
                api(project(":kstatemachine"))

                api("org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.serialization}")
            }
        }

    }
}