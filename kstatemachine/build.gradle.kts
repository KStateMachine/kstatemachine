import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    dependencies {
        classpath("org.jacoco:org.jacoco.core:0.8.7")
    }
}

plugins {
    kotlin("jvm") version Versions.kotlin
    `java-library`
    jacoco
}
group = "ru.nsk"
version = "0.9.1"

repositories {
    mavenCentral()
}

jacoco {
    toolVersion = "0.8.7"
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport) // report is always generated after tests run
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        csv.required.set(false)
        html.outputLocation.set(layout.buildDirectory.dir("jacocoHtml"))
    }
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit { minimum = "0.7".toBigDecimal() }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

dependencies {
    testImplementation("io.kotest:kotest-assertions-core:${Versions.kotestAssertions}")
    testImplementation("io.kotest:kotest-runner-junit5:${Versions.kotestRunner}")
    testImplementation("io.mockk:mockk:${Versions.mockk}")
}