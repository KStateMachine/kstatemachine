plugins {
    kotlin("multiplatform")
    application
}

group = rootProject.group
version = rootProject.version

kotlin {
    jvmToolchain(Versions.jdkVersion)
    jvm {}
//    js(IR) {
//        browser()
//        nodejs()
//    }
//    iosArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":kstatemachine-coroutines"))
            }
        }
    }
}
