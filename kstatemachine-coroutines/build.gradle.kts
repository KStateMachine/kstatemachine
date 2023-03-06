plugins {
    kotlin("jvm")
    `java-library`
    ru.nsk.`maven-publish`
    id("org.jetbrains.dokka") version Versions.kotlinDokka
}

group = rootProject.group
version = rootProject.version

tasks {
    compileKotlin {
        kotlinOptions {
            jvmTarget = Versions.jvmTarget
            languageVersion = Versions.languageVersion
            apiVersion = Versions.apiVersion
        }
    }
}

java {
    sourceCompatibility = Versions.javaCompatibilityVersion
    targetCompatibility = Versions.javaCompatibilityVersion
}

dependencies {
    api(project(":kstatemachine"))
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutinesCore}")
}