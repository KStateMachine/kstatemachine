import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

group = rootProject.group
version = rootProject.version

kotlin {
    jvmToolchain(libs.versions.jdk.get().toInt())
    jvm {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        binaries {
            executable(KotlinCompilation.MAIN_COMPILATION_NAME, "ComplexSyntaxSample") {
                mainClass.set("ru.nsk.samples.ComplexSyntaxSampleKt")
            }
            executable(KotlinCompilation.MAIN_COMPILATION_NAME, "CrossLevelTransitionSample") {
                mainClass.set("ru.nsk.samples.CrossLevelTransitionSampleKt")
            }
            executable(KotlinCompilation.MAIN_COMPILATION_NAME, "FinishedEventDataStateSample") {
                mainClass.set("ru.nsk.samples.FinishedEventDataStateSampleKt")
            }
            executable(KotlinCompilation.MAIN_COMPILATION_NAME, "FinishedEventSample") {
                mainClass.set("ru.nsk.samples.FinishedEventSampleKt")
            }
            executable(KotlinCompilation.MAIN_COMPILATION_NAME, "FinishedStateSample") {
                mainClass.set("ru.nsk.samples.FinishedStateSampleKt")
            }
            executable(KotlinCompilation.MAIN_COMPILATION_NAME, "GuardedTransitionSample") {
                mainClass.set("ru.nsk.samples.GuardedTransitionSampleKt")
            }
            executable(KotlinCompilation.MAIN_COMPILATION_NAME, "InheritTransitionsSample") {
                mainClass.set("ru.nsk.samples.InheritTransitionsSampleKt")
            }
            executable(KotlinCompilation.MAIN_COMPILATION_NAME, "MermaidExportSample") {
                mainClass.set("ru.nsk.samples.MermaidExportSampleKt")
            }
            executable(KotlinCompilation.MAIN_COMPILATION_NAME, "MinimalSealedClassesSample") {
                mainClass.set("ru.nsk.samples.MinimalSealedClassesSampleKt")
            }
            executable(KotlinCompilation.MAIN_COMPILATION_NAME, "MinimalSyntaxSample") {
                mainClass.set("ru.nsk.samples.MinimalSyntaxSampleKt")
            }
            executable(KotlinCompilation.MAIN_COMPILATION_NAME, "PlantUmlExportSample") {
                mainClass.set("ru.nsk.samples.PlantUmlExportSampleKt")
            }
            executable(KotlinCompilation.MAIN_COMPILATION_NAME, "PlantUmlExportWithUmlMetaInfoSample") {
                mainClass.set("ru.nsk.samples.PlantUmlExportWithUmlMetaInfoSampleKt")
            }
            executable(KotlinCompilation.MAIN_COMPILATION_NAME, "PlantUmlUnsafeExportWithExportMetaInfoSample") {
                mainClass.set("ru.nsk.samples.PlantUmlUnsafeExportWithExportMetaInfoSampleKt")
            }
            executable(KotlinCompilation.MAIN_COMPILATION_NAME, "SerializationEventRecordingSample") {
                mainClass.set("ru.nsk.samples.SerializationEventRecordingSampleKt")
            }
            executable(KotlinCompilation.MAIN_COMPILATION_NAME, "StdLibMinimalSealedClassesSample") {
                mainClass.set("ru.nsk.samples.StdLibMinimalSealedClassesSampleKt")
            }
            executable(KotlinCompilation.MAIN_COMPILATION_NAME, "TypesafeTransitionSample") {
                mainClass.set("ru.nsk.samples.TypesafeTransitionSampleKt")
            }
            executable(KotlinCompilation.MAIN_COMPILATION_NAME, "UndoTransitionSample") {
                mainClass.set("ru.nsk.samples.UndoTransitionSampleKt")
            }
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":kstatemachine-coroutines"))
                implementation(project(":kstatemachine-serialization"))

                implementation(libs.kotlinx.serialization.json)
            }
        }
    }
}
