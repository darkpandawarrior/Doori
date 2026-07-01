plugins {
    id("mileway.kmp.compose")
}

kotlin {
    android {
        namespace = "com.mileway.core.maps.maplibre"
        compileSdk = 37
        minSdk = 30
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":core:maps"))

            // MapLibre Compose, open-source KMP map (Android + iOS).
            // No API key required; uses configurable tile server (default: OpenFreeMap).
            implementation(libs.maplibre.compose)

            // Koin
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
        }
        androidMain.dependencies {
            implementation(libs.koin.android)
        }
    }
}
