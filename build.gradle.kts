plugins {
    kotlin("jvm") version Versions.kotlin apply false
    id("org.jetbrains.dokka") version Versions.kotlinDokka
}

group = Versions.libraryMavenCentralGroup
version = Versions.libraryVersion

allprojects {
    repositories {
        mavenCentral()
    }
}