plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    id("org.jetbrains.kotlinx.kover")
}

group = rootProject.group
version = rootProject.version

kotlin {
    jvmToolchain(libs.versions.jdk.get().toInt())
    jvm {
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    sourceSets {
        commonTest {
            dependencies {
                implementation(project(":kstatemachine-coroutines"))
                implementation(project(":kstatemachine-serialization"))

                implementation(libs.kotest.assertions.core)
            }
        }
        jvmTest {
            dependencies {
                implementation(libs.kotest.runner.junit5)
                implementation(libs.mockk)
            }
        }
    }
}

dependencies {
    kover(project(":kstatemachine"))
    kover(project(":kstatemachine-coroutines"))
    kover(project(":kstatemachine-serialization"))
}

kover.reports.verify.rule {
    minBound(88)
}
