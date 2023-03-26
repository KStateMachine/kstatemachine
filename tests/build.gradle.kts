plugins {
    kotlin("jvm")
}

group = rootProject.group
version = rootProject.version

kotlin {
    jvmToolchain(Versions.jdkVersion)
}

tasks.test {
    useJUnitPlatform()
}

dependencies {
    testImplementation(project(":kstatemachine-coroutines"))

    testImplementation("io.kotest:kotest-assertions-core:${Versions.kotest}")
    testImplementation("io.kotest:kotest-framework-datatest:${Versions.kotest}")
    testImplementation("io.kotest:kotest-runner-junit5:${Versions.kotest}")
    testImplementation("io.mockk:mockk:${Versions.mockk}")
}