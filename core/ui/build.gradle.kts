plugins {
    id("miletracker.kmp.compose")
}

kotlin {
    android {
        namespace = "com.miletracker.core.ui"
        compileSdk = 37
        minSdk = 30
    }

    // The `MileTracker` iOS framework is produced by the `:shared` umbrella module (which exports both
    // core:ui and feature:tracking). core:ui keeps its iOS targets (declared by the convention plugin)
    // so its iosMain — MainViewController + the Referral/Push/DeepLink bridges — compiles and is exportable.

    sourceSets {
        commonMain.dependencies {
            implementation(libs.runtime)
            implementation(libs.foundation)
            implementation(libs.material3)
            implementation(libs.ui)
            implementation(libs.material.icons.extended)
            // Unified @Preview in commonMain (CMP 1.11.1), enables platform-agnostic previews (Phase 9)
            implementation(libs.ui.tooling.preview.mp)

            implementation(libs.koin.core)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.datastore.preferences.core)
            implementation(libs.materialkolor)
            implementation(libs.colorpicker.compose)
            // Multiplatform wheel date/time picker (Phase 2.4b, replaces Android-only PickTime)
            implementation(libs.datetime.wheel.picker)
            implementation(libs.kotlinx.datetime)
            implementation(libs.webview.multiplatform)
            implementation(project(":core:common"))
            implementation(project(":core:data"))
            // V15 PF.3: LocalManagerProvider Compose layer over the core:platform service interfaces.
            implementation(project(":core:platform"))
        }
        androidMain.dependencies {
            implementation(libs.core.ktx)
            implementation(libs.activity.compose)
            implementation(libs.jb.navigation.compose)
            implementation(libs.lifecycle.viewmodel)
            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.koin.android)
            implementation(libs.koin.compose)
            implementation(libs.kotlinx.coroutines.android)

            implementation(libs.datastore.preferences)
            implementation(libs.coil3.compose)
        }
    }
}
