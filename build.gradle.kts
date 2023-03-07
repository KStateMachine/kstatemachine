plugins {
    kotlin("jvm") version Versions.kotlin apply false
}

group = Versions.libraryMavenCentralGroup
version = Versions.libraryVersion

allprojects {
    repositories {
        mavenCentral()
    }
}