/**
 * `:app-web-preview` — a wasmJs "preview shell" for the portfolio site (embedded as an iframe the
 * same way Kursi's `cmp-web` build is). Room KMP publishes no wasm target, so `:core:data` (and
 * everything above it: `:core:ui`, the feature modules) can never compile to wasm — this module
 * instead compiles the REAL design system straight from `core/ui`'s commonMain sources (theme
 * package only, via srcDir + include filter) and rebuilds a curated demo subset of screens
 * (dashboard / tracking / expense log) over in-memory fakes.
 *
 * ponytail: source-inclusion over a module split — extracting a `:core:designsystem` module just
 * for wasm would touch 20+ consumers; the theme package is dependency-clean (compose +
 * materialkolor only), so an explicit file allowlist below buys the same reuse for zero refactor.
 * Upgrade path: if a real designsystem module ever gets split out, swap the srcDir for a project
 * dependency.
 */
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        commonMain {
            // The real Mileway theme, compiled from core:ui's sources. Allowlist (not the whole
            // dir): the controller files pull DataStore/Koin, which have no place in a demo shell.
            kotlin.srcDir(rootDir.resolve("core/ui/src/commonMain/kotlin"))
            kotlin.include(
                "com/mileway/webpreview/**",
                "com/mileway/core/ui/theme/Color.kt",
                "com/mileway/core/ui/theme/DesignTokens.kt",
                "com/mileway/core/ui/theme/MapProvider.kt",
                "com/mileway/core/ui/theme/MilewaySemanticColors.kt",
                "com/mileway/core/ui/theme/MilewayTheme.kt",
                "com/mileway/core/ui/theme/MilewayThemes.kt",
                "com/mileway/core/ui/theme/ThemeController.kt",
                "com/mileway/core/ui/theme/ThemeDefaults.kt",
                "com/mileway/core/ui/theme/Type.kt",
            )
            dependencies {
                implementation(libs.runtime)
                implementation(libs.foundation)
                implementation(libs.material3)
                implementation(libs.ui)
                implementation(libs.material.icons.extended)
                implementation(libs.materialkolor)
                // ThemeController.kt (AccentPalette + persistence) compiles against DataStore;
                // the preview never instantiates it — no DataStore is ever created on wasm.
                implementation(libs.datastore.preferences.core)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)
                // Real tracking math (Kalman smoothing, path simplification) — the toolkit module
                // already publishes a wasmJs target, so the demo drive runs the production pipeline.
                implementation("com.siddharth.kmp:location:1.0.0")
            }
        }
    }
}
