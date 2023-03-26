plugins {
    kotlin("jvm")
    application
}

group = rootProject.group
version = rootProject.version

application {
    mainClass.set("FullSyntaxSample")
}

kotlin {
    jvmToolchain(Versions.jdkVersion)
}

dependencies {
    implementation(project(":kstatemachine-coroutines"))
}