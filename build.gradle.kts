plugins {
    kotlin("jvm") version Versions.kotlin apply false
}

group = "ru.nsk"
version = "0.9.1"

allprojects {
    repositories {
        mavenCentral()
    }
}