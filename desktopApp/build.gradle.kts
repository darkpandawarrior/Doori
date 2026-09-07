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
    // Reads the compose.desktop block below and turns it into signed .dmg/.msix/.deb with delta
    // auto-update. `packageDistributionForCurrentOS` builds an installer; it does not give the
    // user a way to receive the NEXT one, and that gap is the whole reason this is here.
    alias(libs.plugins.conveyor)
}

// Wave-2 §A: native-installer version, computed by gradle/versioning.gradle.kts. NOT MARKETING —
// Compose Desktop validates packageVersion at configure time as MAJOR.MINOR.BUILD with MAJOR ≤ 255,
// and MARKETING's MAJOR is the year (>255). desktopPackageVersion (MILESTONE.0.commitCount) is the
// desktop-legal form; see that file's ponytail comment.
apply(from = rootProject.file("gradle/versioning.gradle.kts"))

// Read once at project scope — inside the compose.desktop { } DSL, `extra[...]` would resolve
// against the DSL receiver, not the project's extra.
val desktopPackageVersion = extra["mileway.desktopPackageVersion"] as String

// Conveyor reads project.version, and every package format requires one. This is the same
// desktop-legal MAJOR.MINOR.BUILD that Compose Desktop already validates against - NOT the
// MARKETING version, whose MAJOR is the year and would fail both.
version = desktopPackageVersion

kotlin {
    jvm("desktop")

    sourceSets {
        // `by getting`, not the `desktopMain { }` typed accessor: Kotlin generates those accessors
        // only for standard targets (commonMain/androidMain/iosMain). This module declares a
        // CUSTOM-named target via `jvm("desktop")`, so no accessor exists and the typed form fails
        // with "Unresolved reference 'desktopMain'". Gradle 9.6 deprecates `by getting` generally,
        // but for a custom-named target this remains the working form — revisit when Kotlin ships
        // accessors for custom target names, not before.
        val desktopMain by getting {
            dependencies {
                implementation(project(":core:common"))
                implementation(project(":core:data"))
                implementation(project(":core:platform"))
                // buildCloudFallback (the BYOK cloud-provider chain) — see Assistant.kt.
                implementation(project(":core:ai"))
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
                // Assistant.kt: AiSettingsSection/AiSettingsState (consent + BYOK key card) and the
                // SecureKeyStore/ModelManager/OnDeviceLlm seam behind it — desktop's on-device tier
                // is always UnavailableOnDeviceLlm, so the cloud key is the only way this card ever
                // answers a prompt.
                implementation("com.siddharth.kmp:ai:1.0.0")
                implementation("com.siddharth.kmp:llm-chat:1.0.0")
                implementation("com.siddharth.kmp:designsystem:1.0.0")
                // AiResult<T>/AiFailure/fold — CloudOnDeviceLlm.generate()'s return type.
                implementation("com.siddharth.kmp:result:1.0.0")
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
            packageVersion = desktopPackageVersion
        }
    }
}
