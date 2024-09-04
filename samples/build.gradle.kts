plugins {
    kotlin("multiplatform")
    application
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
            }
        }
    }
}
