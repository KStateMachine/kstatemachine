plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("org.jetbrains.dokka")
    alias(libs.plugins.maven.publish) // must be applied after dokka
    id("org.jetbrains.kotlinx.kover")
}

group = rootProject.group
version = rootProject.version

kotlin {
    jvmToolchain(libs.versions.jdk.get().toInt())
    sourceSets.all {
        languageSettings.apply {
            languageVersion = libs.versions.kotlin.language.get()
            apiVersion = libs.versions.kotlin.language.get()
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

                api(libs.kotlinx.coroutines.core)
            }
        }

        val blockingMain = create("blockingMain") {
            dependsOn(commonMain.get())
        }
        jvmMain {
            dependsOn(blockingMain)
        }
        nativeMain {
            dependsOn(blockingMain)
        }
    }
}
