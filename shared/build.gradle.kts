plugins {
    id("shared.kmp.compose")
}

/**
 * Shared app-shell + iOS umbrella module.
 *
 * Dual-target (Android + iOS): it holds the app-shell UI (home dashboard, navigation, auth, search)
 * in `commonMain` so **both** the Android `:app` and the iOS `MainViewController` render the same
 * `MilewayApp()` — full KMP/CMP parity (see AgentHarness/plans/mileway/app-shell-ios-parity.md).
 *
 * It also produces the single `Mileway.framework` the Xcode app links against. The framework can't
 * live in `core:ui`, because the shell calls into feature modules and a core module must not depend
 * on a feature. This module sits *above* core + features and re-exports the iOS entrypoints.
 *
 * Exported API surfaced to Swift:
 *  - core:ui          → MainViewController, ReferralBridge, PushBridge, DeepLinkBridge
 *  - feature:tracking → MilwayViewController (IosTrackingEntry), IosBgTaskDispatcher
 *  - feature:agent    → iosAgentModule (IosAgentEntry)
 *  - feature:logging  → IosIntentEntry (iOS App Intents start/stop/log-expense bridge)
 *  - core:ai          → DocumentAiAnalyzer, FoundationModelsBridge, AiExtraction/DocPrompt models
 *    (V26 AI: lets Swift conform to DocumentAiAnalyzer and register a Foundation Models bridge —
 *    see `iosApp/iosApp/ai/FoundationModelsDocumentAnalyzer.swift`)
 */
kotlin {
    android {
        namespace = "com.mileway.shared"
        compileSdk = 37
        minSdk = 30
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Mileway"
            export(project(":core:ui"))
            export(project(":feature:tracking"))
            export(project(":feature:agent"))
            export(project(":feature:logging"))
            export(project(":core:ai"))
        }
    }

    sourceSets {
        commonMain.dependencies {
            // Compose runtime + UI libs — this module now hosts Compose shell UI (Compose compiler is
            // applied via shared.kmp.compose), and core:ui exposes these as `implementation`, so they
            // aren't transitive. Mirror core:ui's set.
            implementation(libs.runtime)
            implementation(libs.foundation)
            implementation(libs.material3)
            implementation(libs.ui)
            implementation(libs.material.icons.extended)
            implementation(libs.ui.tooling.preview.mp)
            implementation(compose.components.resources)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.lifecycle.viewmodel)
            // V29 P29.H.6: header avatar image (falls back to the terminal glyph when unset).
            implementation(libs.coil3.compose)

            // Core + stub deps the hoisted app-shell (home/nav/auth/search) uses directly.
            implementation(project(":core:common"))
            implementation(project(":core:data"))
            implementation(project(":core:network"))
            implementation(project(":core:platform"))
            implementation(project(":stub"))

            // api(...) is required for export(...) above to surface these modules' public API in the framework.
            api(project(":core:ui"))
            api(project(":feature:tracking"))
            api(project(":feature:agent"))
            api(project(":feature:logging"))
            api(project(":core:ai"))

            // Remaining feature deps — the hoisted app-shell (home/nav/auth/search) composes every
            // feature at the composition root, so this module depends on all of them (as :app did).
            api(project(":feature:cards"))
            api(project(":feature:events"))
            api(project(":feature:payments"))
            api(project(":feature:payables"))
            api(project(":feature:travel"))
            api(project(":feature:profile"))
            api(project(":feature:approvals"))
            api(project(":feature:media"))
        }
        iosMain.dependencies {
            // No extra deps — MainViewController() uses api deps already imported above.
        }
    }
}
