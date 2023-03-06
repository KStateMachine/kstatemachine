plugins {
    kotlin("jvm")
    application
}

group = rootProject.group
version = rootProject.version

application {
    mainClass.set("FullSyntaxSample")
}

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
    implementation(project(":kstatemachine-coroutines"))
}