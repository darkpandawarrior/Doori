// G2: baseline-profile generator module (macrobenchmark). Drives the critical path on a device/GMD via
// `./gradlew :app:generateNoGmsReleaseBaselineProfile`, producing the profile that profileinstaller bundles.
// This module is NOT exercised by the unit-test gate (generation needs a device); it just configures cleanly.
plugins {
    // AGP 9 has built-in Kotlin, so no kotlin.android plugin. com.android.test is already on the
    // classpath (via build-logic) → apply versionless. baselineprofile is new → catalog version.
    id("com.android.test")
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "com.miletracker.baselineprofile"
    compileSdk = 37

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    defaultConfig {
        minSdk = 30
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Mirror the app's flavor dimension so each generator variant targets the matching app variant.
    flavorDimensions += "maps"
    productFlavors {
        create("gms") { dimension = "maps" }
        create("noGms") { dimension = "maps" }
    }

    targetProjectPath = ":app"
}

// Generate against the noGms variant (JVM/Robolectric-safe; gms maps can SIGSEGV in headless runs).
baselineProfile {
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.uiautomator)
    implementation(libs.benchmark.macro.junit4)
}
