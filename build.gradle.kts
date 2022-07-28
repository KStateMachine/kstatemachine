plugins {
    kotlin("jvm") version Versions.kotlin apply false
}

group = Versions.libraryGroup
version = Versions.libraryVersion

allprojects {
    repositories {
        mavenCentral()
    }
}