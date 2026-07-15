plugins {
    id("shared.kmp.compose")
    id("mileway.kmp.desktop")
}

// Compose Multiplatform string resources — the shared, localizable string table for the whole app.
// commonMain/composeResources/values/strings.xml → generated `Res` (public so every module uses it).
compose.resources {
    publicResClass = true
    packageOfResClass = "com.mileway.core.ui.resources"
}

kotlin {
    android {
        namespace = "com.mileway.core.ui"
        compileSdk = 37
        minSdk = 30
        // V31 Z.5a: run commonTest on the JVM host so it counts toward the quality-gate's
        // ./gradlew testAndroidHostTest aggregate (AGP KMP library plugin disables host tests by default).
        withHostTest {}
    }

    // The `Mileway` iOS framework is produced by the `:shared` umbrella module (which exports both
    // core:ui and feature:tracking). core:ui keeps its iOS targets (declared by the convention plugin)
    // so its iosMain — MainViewController + the Referral/Push/DeepLink bridges — compiles and is exportable.

    sourceSets {
        commonMain.dependencies {
            implementation(libs.runtime)
            implementation(libs.foundation)
            // api (not implementation) so every module depending on core:ui can call
            // stringResource(Res.string.…) against the shared string table without re-declaring this.
            api(compose.components.resources)
            implementation(libs.material3)
            implementation(libs.ui)
            implementation(libs.material.icons.extended)
            // Unified @Preview in commonMain (CMP 1.11.1), enables platform-agnostic previews (Phase 9)
            implementation(libs.ui.tooling.preview.mp)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            // P31.MISC.1: koinViewModel() for BugReportViewModel (ShakeReportHost/BugReportSheet).
            implementation(libs.koin.compose.viewmodel)
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
            // V26 P26.SHEET: OcrConfirmationSheet/OcrReviewSheet/OcrBatchResultsSheet render
            // core:ai's DocumentAnalysis directly, so every OCR call site reaches them without a
            // feature module in between.
            implementation(project(":core:ai"))
            // BaseViewModel (MVI base class) uses androidx.lifecycle.ViewModel/viewModelScope directly
            // in commonMain; the artifact publishes common/android/ios/desktop targets.
            implementation(libs.lifecycle.viewmodel)
            // Consolidation backlog #1: BaseViewModel/StateViewModel now come from the toolkit's
            // :mvi-core (CAS-safe setState via MutableStateFlow.update — Mileway's own fork used a
            // non-atomic `_state.value = _state.value.reducer()` race). api() so every downstream
            // feature module resolving com.mileway.core.ui.mvi... transitively sees the new package.
            api("com.siddharth.kmp:mvi-core:1.0.0")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
        androidMain.dependencies {
            implementation(libs.core.ktx)
            implementation(libs.activity.compose)
            implementation(libs.jb.navigation.compose)
            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.koin.android)
            implementation(libs.kotlinx.coroutines.android)

            implementation(libs.datastore.preferences)
            implementation(libs.coil3.compose)
        }
    }
}
