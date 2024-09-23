plugins {
    kotlin("multiplatform")
    application
    kotlin("plugin.serialization") version Versions.kotlin
}

group = rootProject.group
version = rootProject.version

kotlin {
    jvmToolchain(Versions.jdkVersion)
    jvm {}

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":kstatemachine-coroutines"))
                implementation(project(":kstatemachine-serialization"))

                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.serialization}")
            }
        }
    }
}
