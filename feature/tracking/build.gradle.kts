plugins {
    id("miletracker.cmp.feature")
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    android {
        namespace = "com.miletracker.feature.tracking"
        compileSdk = 37
        minSdk = 30
        // Enable JVM host execution of commonTest so pipeline/policy tests run in the gradle gate.
        withHostTest {}
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kmpworkmanager)
            // G1: Paging 3 — paging-common + paging-compose are KMP since 3.3.0+.
            implementation(libs.paging.common)
            implementation(libs.paging.compose)
            implementation(project(":core:common"))
            implementation(project(":core:data"))
            implementation(project(":core:network"))
            implementation(project(":core:ui"))
            implementation(project(":core:platform"))
            implementation(project(":core:maps"))
            // P-E.1: Coil3 is multiplatform; moved from androidMain so submission components can live in commonMain.
            implementation(libs.coil3.compose)
        }
        androidMain.dependencies {
            // api() so dependents (feature:logging, :app) can resolve Material theme parent
            api(libs.material)
            implementation(libs.koin.androidx.workmanager)
            implementation(libs.kotlinx.coroutines.play.services)
            implementation(libs.play.services.location)
            implementation(libs.workmanager.runtime)
            implementation(libs.mlkit.document.scanner)
            implementation(libs.mlkit.text.recognition)
            implementation(project(":feature:media"))
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
