plugins {
    id("shared.kmp.compose")
}

kotlin {
    android {
        namespace = "com.mileway.core.maps"
        compileSdk = 37
        minSdk = 30
        // Enable JVM host execution of commonTest (CanvasRouteSurface's pure projection math)
        // via testAndroidHostTest — mirrors core:security's setup.
        withHostTest {}
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.runtime)
            implementation(libs.ui)
            implementation(libs.foundation)
            // MaterialTheme.colorScheme for CanvasRouteSurface — already-cataloged, used by
            // core:ui/core:forms/etc; no new coordinate added.
            implementation(libs.material3)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
