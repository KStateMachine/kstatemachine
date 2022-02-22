import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    `java-library`
    ru.nsk.`maven-publish`
    ru.nsk.jacoco
}

group = rootProject.group
version = rootProject.version

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.test {
    useJUnitPlatform()
}

dependencies {
    testImplementation("io.kotest:kotest-assertions-core:${Versions.kotestAssertions}")
    testImplementation("io.kotest:kotest-runner-junit5:${Versions.kotestRunner}")
    testImplementation("io.mockk:mockk:${Versions.mockk}")
}