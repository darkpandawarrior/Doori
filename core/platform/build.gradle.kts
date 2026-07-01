plugins {
    id("mileway.kmp.library")
}

kotlin {
    android {
        namespace = "com.mileway.core.platform"
        compileSdk = 37
        minSdk = 30
        // Run commonTest on the JVM host so the LocationNameResolver tests count toward the
        // ./gradlew test gate (the AGP KMP library plugin disables host tests by default).
        withHostTest {}
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.koin.core)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            // CF.2: LoggingAnalyticsHelper logs events via Napier (noGms/iOS analytics impl).
            implementation(libs.napier)
        }
        androidMain.dependencies {
            implementation(libs.koin.android)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.core.ktx)
            // LocationTracker (Android): fused location + Task.await()
            implementation(libs.play.services.location)
            implementation(libs.kotlinx.coroutines.play.services)
            // TextRecognizer (Android): ML Kit on-device OCR
            implementation(libs.mlkit.text.recognition)
            // BackgroundScheduler (Android): WorkManager
            implementation(libs.workmanager.runtime)
        }
        iosMain.dependencies {
            // V15 UP.3: IosAppUpdateManager queries the public iTunes Lookup API (no backend).
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
