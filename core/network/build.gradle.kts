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
            // PLAN_V33 A3: KtorMilewayNetworkApi's HttpClient/BaseUrlProvider (createHttpClient +
            // the platform OkHttp/Darwin/CIO engine live in the toolkit module, not duplicated here).
            implementation("com.siddharth.kmp:network:1.0.0")
            // PLAN_V34 P2/A6: AuthTokenStore wraps the toolkit's encrypted Settings (refresh token).
            implementation("com.siddharth.kmp:settings:1.0.0")
            // PLAN_V34 P2/A6: Ktor's bearer{} Auth provider drives the 401 -> refresh -> retry-once
            // flow (withBearerAuth) — the multiplatform artifact, KtorMilewayNetworkApi itself is
            // built once per platform target from this same commonMain source set.
            implementation(libs.ktor.client.auth)
        }
        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            // PLAN_V33 A3: MockEngine backing KtorMilewayNetworkApiTest — no real :server needed.
            implementation(libs.ktor.client.mock)
            // PLAN_V34 P2/A6: MapSettings — the official in-memory Settings fake for AuthTokenStoreTest.
            implementation("com.russhwolf:multiplatform-settings-test:1.3.0")
        }
    }
}
