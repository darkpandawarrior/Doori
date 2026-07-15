plugins {
    id("shared.kmp.library")
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    android {
        namespace = "com.mileway.core.network"
        compileSdk = 37
        minSdk = 30
        // V31 Z.5a: run commonTest on the JVM host so NetworkLogStore/NetworkLogEntry tests count
        // toward the ./gradlew testAndroidHostTest quality-gate aggregate (AGP KMP library plugin
        // disables host tests by default).
        withHostTest {}
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.koin.core)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.konnection)
            // V21 §3 Wave 4: NetworkLogPlugin is a createClientPlugin for whatever HttpClient the
            // app installs it on (see core/network/netlog); no HTTP calls made from this module.
            implementation(libs.ktor.client.core)
            implementation(project(":core:data"))
            // V15 PF.5: ConfigProvider exposes UpdateConfig (defined in core:platform).
            implementation(project(":core:platform"))
            // PLAN_V33 A1: ContextModels/VendorModels/ProfileModels/OfficeEntityModels physically
            // moved to :contract but kept their `com.mileway.core.network.model` package name —
            // `api` so every module that already depends on core:network keeps resolving them.
            // (core:data already re-exports :contract too, transitively, via the line above.)
            api(project(":contract"))
        }
        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
