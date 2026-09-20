plugins {
    id("shared.kmp.compose")
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

            // Mandatory since maplibre-compose 0.15.0 and previously missing. maplibre-compose
            // carries the MapLibre Native FFI bindings but no renderer implementing them, and the
            // MapLibre Android SDK is no longer pulled in transitively -- without this artifact the
            // module compiles and links, then has no native renderer at runtime. Android-only AAR,
            // so it belongs here rather than in commonMain; iOS gets its renderer from the native
            // MapLibre SDK the iOS app already links.
            implementation(libs.maplibre.compose.runtime.opengl.android)
        }
    }
}
