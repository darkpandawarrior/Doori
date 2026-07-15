plugins {
    id("shared.cmp.feature")
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    android {
        namespace = "com.mileway.feature.tracking"
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
            // V26 P26.CONV: the sole odometer OCR pipeline (OdometerOcrService/OdometerReconciler)
            // now lives in core:media so it's reachable without a feature-to-feature dependency.
            implementation(project(":core:media"))
            // V27 P27.F.6: TrackSubmissionScreen's "Additional Details" form routes through the
            // shared core:forms FormRenderer/validationErrors instead of a hand-rolled duplicate.
            implementation(project(":core:forms"))
            // P-E.1: Coil3 is multiplatform; moved from androidMain so submission components can live in commonMain.
            implementation(libs.coil3.compose)
            // V21 §3 Wave 4: NetworkLogViewModel's API tester takes an optional HttpClient.
            implementation(libs.ktor.client.core)
            // kmp-toolkit adoption: pure GPS-math (KalmanSmoother, PathSimplifier, DynamicIntervalCalculator,
            // TrackingQualityScorer) extracted to the monorepo's :location leaf.
            implementation("com.siddharth.kmp:location:1.0.0")
        }
        androidMain.dependencies {
            // api() so dependents (feature:logging, :app) can resolve Material theme parent
            api(libs.material)
            // PLAN_V33 A6: VehiclePricingCacheStore's DataStore-blob cache (mirrors core/data's
            // SnapshotCacheStore idiom).
            implementation(libs.datastore.preferences)
            implementation(libs.koin.androidx.workmanager)
            implementation(libs.kotlinx.coroutines.play.services)
            implementation(libs.play.services.location)
            implementation(libs.workmanager.runtime)
            implementation(libs.mlkit.document.scanner)
            implementation(project(":feature:media"))
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            // PLAN_V33 A4: RealLocationSendTest drives real Ktor exceptions (ClientRequestException/
            // ServerResponseException) through a MockEngine, matching KtorMilewayNetworkApiTest's
            // convention in core:network, instead of hand-constructing them (their constructors
            // eagerly read `response.call.request`, which needs a real HttpClientCall).
            implementation(libs.ktor.client.mock)
            implementation("com.siddharth.kmp:network:1.0.0")
        }
    }
}
