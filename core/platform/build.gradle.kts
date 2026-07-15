plugins {
    id("shared.kmp.library")
    id("mileway.kmp.desktop")
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
            // api, not implementation: PlatformBindings (this module's public facade) exposes
            // app-shell types (LocationTracker, NotificationScheduler, AppUpdateManager, …) directly
            // as public field types, so every consumer of core:platform needs them on its classpath too.
            api("com.siddharth.kmp:app-shell:1.0.0")
            implementation(libs.koin.core)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            // CF.4: NapierCrashReporter logs breadcrumbs/exceptions via Napier (noGms/iOS crash impl).
            implementation(libs.napier)
        }
        androidMain.dependencies {
            implementation(libs.koin.android)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.core.ktx)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
