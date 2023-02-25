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

    testImplementation("io.kotest:kotest-assertions-core:${Versions.kotest}")
    testImplementation("io.kotest:kotest-framework-datatest:${Versions.kotest}")
    testImplementation("io.kotest:kotest-runner-junit5:${Versions.kotest}")
    testImplementation("io.mockk:mockk:${Versions.mockk}")
}