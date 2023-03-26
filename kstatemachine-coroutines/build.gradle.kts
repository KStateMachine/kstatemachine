plugins {
    kotlin("jvm")
    `java-library`
    ru.nsk.`maven-publish`
    id("org.jetbrains.dokka") version Versions.kotlinDokka
}

group = rootProject.group
version = rootProject.version

kotlin {
    jvmToolchain(Versions.jdkVersion)
}

tasks {
    compileKotlin {
        kotlinOptions {
            languageVersion = Versions.languageVersion
            apiVersion = Versions.apiVersion
        }
    }
}

dependencies {
    api(project(":kstatemachine"))
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutinesCore}")
}