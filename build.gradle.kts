plugins {
    kotlin("multiplatform") version "1.4.10"
    application
}
group = "ru.nsk"
version = "0.1"

repositories {
    mavenCentral()
}
kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "11"
        }
        withJava()
    }
    sourceSets {
        val jvmMain by getting
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
            }
        }
    }
}
application {
    mainClassName = "MainKt"
}