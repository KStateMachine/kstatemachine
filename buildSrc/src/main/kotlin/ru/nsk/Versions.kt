import org.gradle.api.JavaVersion

object Versions {
    // library
    const val libraryGroup = "io.github.nsk90"
    const val libraryVersion = "0.18.1"

    // tools
    const val kotlin = "1.8.10"
    const val kotlinDokka = "1.7.20"
    const val gradle = "7.1.0"

    // compatibility
    const val jvmTarget = "1.8"
    val javaCompatibilityVersion = JavaVersion.VERSION_1_8
    const val languageVersion = "1.5"
    const val apiVersion = "1.5"

    // dependencies
    const val coroutinesCore = "1.6.4"

    // test dependencies   
    const val mockk = "1.13.4"
    const val kotest = "5.5.5"
}