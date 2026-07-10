plugins {
    id("shared.cmp.feature")
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    android {
        namespace = "com.mileway.feature.logging"
        compileSdk = 37
        minSdk = 30
        // Enable JVM host execution of commonTest so catalog/validator tests run in the gradle gate.
        withHostTest {}
    }

    sourceSets {
        commonMain.dependencies {
            // P5.1: JSON-encodes the LocationStop list into LogMilesDraftEntity.locationsJson.
            implementation(libs.kotlinx.serialization.json)
            implementation(project(":core:common"))
            implementation(project(":core:data"))
            implementation(project(":core:network"))
            // Location switching: LocationTracker / LocationNameResolver for "use current location".
            implementation(project(":core:platform"))
            implementation(project(":core:ui"))
            // V26 P26.CONV: OdometerCaptureSheet runs real OCR on the attached photo via the one
            // shared odometer OCR pipeline instead of manual-only entry.
            implementation(project(":core:media"))
            // V27 P27.F.6: LogMilesStep2Screen's "Additional Details" card routes through the
            // shared core:forms FormRenderer/validationErrors instead of a hand-rolled duplicate.
            implementation(project(":core:forms"))
            implementation(project(":feature:tracking"))
            // P1.6: reuses PolicyMockData's tiered policy engine for expense-amount validation.
            implementation(project(":stub"))
            // P1.4: renders an optional local receipt photo on the entry form + detail screen.
            implementation(libs.coil3.compose)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            // P5.1: runTest for the draft-repository mapper round-trip test.
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
