import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    application
}
group = rootProject.group
version = rootProject.version

repositories {
    mavenCentral()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

application {
    mainClass.set("FullSyntaxSample")
}

dependencies {
    implementation(project(":kstatemachine"))
}

