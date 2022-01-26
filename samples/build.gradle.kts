import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version Versions.kotlin
    application
}
group = "ru.nsk"

repositories {
    mavenCentral()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

dependencies {
    implementation(project(":kstatemachine"))
}

application {
    mainClassName = "FullSyntaxSample"
}