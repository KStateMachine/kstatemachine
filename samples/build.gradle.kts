plugins {
    kotlin("jvm")
    application
}

group = rootProject.group
version = rootProject.version

application {
    mainClass.set("FullSyntaxSample")
}

dependencies {
    implementation(project(":kstatemachine"))
}
