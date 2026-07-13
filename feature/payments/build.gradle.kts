plugins {
    id("shared.cmp.feature")
}

kotlin {
    android {
        namespace = "com.mileway.feature.payments"
        compileSdk = 37
        minSdk = 30
        // P29.C.6/C.7: run commonTest (payment state-machine + duplicate-detection tests) on the JVM host.
        withHostTest {}
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(project(":core:data"))
            implementation(project(":core:ui"))
            // P29.C.7: QR invoice attachment reuses the shared OCR pipeline (rememberMediaCaptureLauncher)
            // and DuplicateDetector/DuplicateVerdict for local duplicate-receipt detection.
            implementation(project(":core:media"))
            implementation(project(":core:ai"))
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
