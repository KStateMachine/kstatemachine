plugins {
    kotlin("jvm") version Versions.kotlin apply false
}

group = "ru.nsk"
version = "0.10.0"

allprojects {
    repositories {
        mavenCentral()
    }
}