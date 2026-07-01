plugins {
    `kotlin-dsl`
}

group = "com.mileway.buildlogic"

java {
    // Convention plugin code itself targets Java 17, not the Android modules it configures.
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    // Must be compileOnly, using implementation causes ClassCastException when the
    // same plugin class is loaded by two different classloaders at different versions.
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.composeCompiler.gradlePlugin)
}

tasks.validatePlugins {
    // Catches accidental use of internal Gradle APIs and missing @TaskAction annotations
    // before they fail at runtime on a different Gradle version.
    enableStricterValidation = true
    failOnWarning = true
}

gradlePlugin {
    plugins {
        register("kmpLibrary") {
            id = "mileway.kmp.library"
            implementationClass = "MilewayKmpLibraryConventionPlugin"
        }
        register("kmpCompose") {
            id = "mileway.kmp.compose"
            implementationClass = "MilewayKmpComposeConventionPlugin"
        }
        register("androidLibrary") {
            id = "mileway.android.library"
            implementationClass = "MilewayAndroidLibraryConventionPlugin"
        }
        register("androidApplication") {
            id = "mileway.android.application"
            implementationClass = "MilewayAndroidApplicationConventionPlugin"
        }
        register("cmpFeature") {
            id = "mileway.cmp.feature"
            implementationClass = "MilewayCmpFeatureConventionPlugin"
        }
        register("test") {
            id = "mileway.test"
            implementationClass = "MilewayTestConventionPlugin"
        }
    }
}
