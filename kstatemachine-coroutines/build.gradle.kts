plugins {
    kotlin("jvm")
    `java-library`
}

group = rootProject.group
version = rootProject.version

tasks {
    compileKotlin {
        kotlinOptions {
            jvmTarget = Versions.jvmTarget
        }
    }
}

java {
    sourceCompatibility = Versions.javaCompatibilityVersion
    targetCompatibility = Versions.javaCompatibilityVersion
}

dependencies {
    implementation(project(":kstatemachine"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutinesCore}")
}