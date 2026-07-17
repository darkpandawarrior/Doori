import org.jetbrains.compose.desktop.application.dsl.TargetFormat

/**
 * PLAN_V23 D.2: `:desktopApp` — a thin Compose Desktop dashboard over the `core:{common,data,
 * platform,ui}` desktop targets opted in by D.1 (Option b: no `feature:tracking`/maps).
 *
 * A bare `jvm("desktop")` KMP module (not `mileway.kmp.desktop`/`mileway.kmp.library` — those apply
 * `com.android.kotlin.multiplatform.library`, irrelevant to a JVM-only launcher) so it resolves the
 * same Compose-desktop-attributed variant as `core:ui`'s `jvm("desktop")` target.
 */
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

// Wave-2 §A: same computed MARKETING as :app/:wear (gradle/versioning.gradle.kts) — the native
// installer's version string, not hand-typed.
apply(from = rootProject.file("gradle/versioning.gradle.kts"))

kotlin {
    jvm("desktop")

    sourceSets {
        val desktopMain by getting {
            dependencies {
                implementation(project(":core:common"))
                implementation(project(":core:data"))
                implementation(project(":core:platform"))
                // Excludes kcef/jogamp: core:ui's desktop webview backend pulls JogAmp artifacts
                // that aren't resolvable from this project's configured repositories (pre-existing
                // gap, not a D.2 concern — the thin dashboard never renders a webview).
                implementation(project(":core:ui")) {
                    exclude(group = "dev.datlag", module = "kcef")
                }
                implementation(compose.desktop.currentOs)
                implementation(libs.material3)
                // showcase/T.2: gallery screens build QuickAction/TimelineStep/ProfileGridItem mock
                // data with ImageVector icons directly — core:ui pulls this in as `implementation`
                // (not `api`), so it isn't visible on this module's compile classpath transitively.
                implementation(libs.material.icons.extended)
                implementation(libs.koin.core)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        val desktopTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
                // showcase/T.1: renders the desktop dashboard to a PNG via Compose Multiplatform's
                // ImageComposeScene — pure-JVM, no Robolectric/emulator needed.
                implementation(compose.desktop.uiTestJUnit4)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.mileway.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Deb, TargetFormat.Msi)
            packageName = "Mileway"
            packageVersion = extra["mileway.marketing"] as String
        }
    }
}
