plugins {
    id("mileway.kmp.library")
}

kotlin {
    android {
        namespace = "com.mileway.core.security"
        compileSdk = 37
        minSdk = 30
        // Enable JVM host execution of commonTest so RootDetectorTest runs in the gradle gate.
        withHostTest {}
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.core.ktx)
            implementation(libs.biometric)
            implementation(libs.kotlinx.coroutines.android)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
