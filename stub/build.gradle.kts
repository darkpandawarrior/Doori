plugins {
    id("shared.kmp.library")
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    android {
        namespace = "com.mileway.stub"
        compileSdk = 37
        minSdk = 30
        withHostTest {}
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.koin.core)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(project(":core:data"))
            implementation(project(":core:network"))
            // V15 PF.5: DemoConfigManager returns UpdateConfig (defined in core:platform).
            implementation(project(":core:platform"))
            // Typed Result<D, DataError.Network> for DemoConfigManager.configState — api so
            // consumers of :stub (e.g. :app tests) see it transitively.
            api("com.siddharth.kmp:result:1.0.0")
            // PLAN_V33 A3: StubModule's NetworkBackendFlags branch builds a real HttpClient
            // (createHttpClient) when the flag is flipped. ktor-client-core is `implementation`
            // (not `api`) inside the toolkit module, so the HttpClient type needs declaring here too.
            implementation("com.siddharth.kmp:network:1.0.0")
            implementation(libs.ktor.client.core)
            // PLAN_V34 P2/A6: StubModule constructs AuthTokenStore's SecureSettingsFactory directly
            // (androidMain needs a Context, iosMain doesn't — same expect/actual split as network).
            implementation("com.siddharth.kmp:settings:1.0.0")
        }
        androidMain.dependencies {
            implementation(libs.koin.android)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.datastore.preferences)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
