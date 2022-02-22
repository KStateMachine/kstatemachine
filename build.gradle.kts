plugins {
    kotlin("jvm") version Versions.kotlin apply false
}

buildscript {
    dependencies {
        classpath("org.jacoco:org.jacoco.core:0.8.7")
    }
}

group = "ru.nsk"
version = "0.9.1"

allprojects {
    repositories {
        mavenCentral()
    }
}