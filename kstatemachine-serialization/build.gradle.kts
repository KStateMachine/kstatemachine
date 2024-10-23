plugins {
    kotlin("multiplatform")
    `java-library`
    ru.nsk.`maven-publish`
    id("org.jetbrains.dokka")
    kotlin("plugin.serialization") version Versions.kotlin
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
    js {
        browser()
        nodejs()
    }
    @Suppress("OPT_IN_USAGE") // this is alpha feature
    wasmJs()

    applyDefaultHierarchyTemplate()
    sourceSets {
        commonMain {
            dependencies {
                api(project(":kstatemachine"))

                api("org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.serialization}")
            }
        }

        // contains blocking APIs which are not supported on JS
        val blockingMain by creating { dependsOn(commonMain.get()) }
        val jsCommonMain by creating { dependsOn(commonMain.get()) }

        jvmMain.get().dependsOn(blockingMain)
        iosMain.get().dependsOn(blockingMain)

        val wasmJsMain by getting
        wasmJsMain.dependsOn(jsCommonMain)
        jsMain.get().dependsOn(jsCommonMain)
    }
}