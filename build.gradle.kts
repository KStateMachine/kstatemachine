plugins {
    kotlin("jvm") version Versions.kotlin apply false
}

group = "ru.nsk"
version = "0.9.3"

allprojects {
    repositories {
        mavenCentral()
    }
}