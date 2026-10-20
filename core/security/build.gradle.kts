plugins {
    id("shared.kmp.library")
}

kotlin {
    android {
        namespace = "com.mileway.core.security"
        compileSdk {
            version = release(37) { minorApiLevel = 1 }
        }
        minSdk = 30
        // Enable JVM host execution of commonTest so RootDetectorTest runs in the gradle gate.
        withHostTest {}
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.core.ktx)
            implementation(libs.kotlinx.coroutines.android)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
