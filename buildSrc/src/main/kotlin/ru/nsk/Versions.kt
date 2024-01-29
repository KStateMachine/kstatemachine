import org.gradle.api.JavaVersion

object Versions {
    // library
    const val libraryMavenCentralGroup = "io.github.nsk90"
    const val libraryJitPackGroup = "com.github.nsk90"
    const val libraryVersion = "0.25.0"

    // tools
    const val kotlin = "1.8.10"
    const val kotlinDokka = "1.7.20"
    const val jacocoTool = "0.8.8"

    // compatibility
    const val jdkVersion = 8
    const val languageVersion = "1.5"
    const val apiVersion = "1.5"

    // dependencies
    const val coroutinesCore = "1.6.4"

    // test dependencies   
    const val mockk = "1.13.4"
    const val kotest = "5.5.5"
}