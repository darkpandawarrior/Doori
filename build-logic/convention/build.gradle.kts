plugins {
    `kotlin-dsl`
}

group = "com.mileway.buildlogic"

java {
    // Convention plugin code itself targets Java 17, not the Android modules it configures.
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
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
        // mileway.kmp.library, mileway.kmp.compose, mileway.cmp.feature, mileway.android.application,
        // and mileway.test moved to the shared kmp-build-logic repo (shared.*) — see
        // external/kmp-build-logic. Their Mileway*ConventionPlugin.kt sources are left in place
        // (unregistered, not yet deleted) pending the follow-up cleanup commit.
        register("kmpLibraryWatchos") {
            id = "mileway.kmp.library.watchos"
            implementationClass = "MilewayKmpLibraryWatchosConventionPlugin"
        }
        register("kmpDesktop") {
            id = "mileway.kmp.desktop"
            implementationClass = "MilewayKmpDesktopConventionPlugin"
        }
        register("androidLibrary") {
            id = "mileway.android.library"
            implementationClass = "MilewayAndroidLibraryConventionPlugin"
        }
    }
}
