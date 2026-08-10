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

    // A JVM target that exists ONLY to screenshot this shell. Every screen here
    // (Dashboard/Tracking/Expenses) lives in commonMain — plain Compose with no wasm-specific
    // code; only Main.kt and one theme actual are wasmJs-only. So the same composables render on
    // the JVM and can be captured with ImageIO, exactly as :desktopApp does.
    //
    // Be clear about what this does and does not prove: it covers the shell's UI, and it does NOT
    // prove the wasm binary runs in a browser. Verifying that needs a real browser harness
    // (Playwright against the built distribution), which is a separate piece of work. A capture
    // that silently implied wasm-runtime coverage would be worse than none.
    jvm("screenshot")

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
                // Layer 2 (semantic roles) + the HCT maths it derives them with. MilewayTheme.kt
                // provides LocalMilewayRoleColors unconditionally, so these are not optional
                // extras — without them this target does not compile at all.
                "com/mileway/core/ui/theme/MilewayRoles.kt",
                "com/mileway/core/ui/theme/MilewayDomain.kt",
                "com/mileway/core/ui/theme/ColorMath.kt",
                // Every design direction, by directory rather than by name: this allowlist silently
                // rotted the moment the five directions landed, and naming each file would set the
                // same trap for the sixth.
                "com/mileway/core/ui/theme/direction/**",
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

        val screenshotTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(compose.desktop.uiTestJUnit4)
                implementation(compose.desktop.currentOs)
            }
        }
    }
}
