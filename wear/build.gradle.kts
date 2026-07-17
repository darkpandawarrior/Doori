import org.gradle.api.artifacts.component.ModuleComponentIdentifier

plugins {
    id("shared.android.application")
    id("shared.test")
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.roborazzi)
}

// Wave-2 §A: same computed MARKETING/BUILDCODE as :app (gradle/versioning.gradle.kts) — a watch
// build ships alongside a phone build under the same release tag, so they carry the same versions.
apply(from = rootProject.file("gradle/versioning.gradle.kts"))

android {
    namespace = "com.mileway.wear"
    defaultConfig {
        applicationId = "com.mileway.wear"
        minSdk = 30
        targetSdk = 36
        versionCode = extra["mileway.buildCode"] as Int
        versionName = extra["mileway.marketing"] as String
        buildConfigField("String", "FINGERPRINT", "\"${extra["mileway.fingerprint"]}\"")
    }

    buildFeatures {
        buildConfig = true
    }

    // P2.1: mirror :app's maps-flavor split so the Wear app builds the same FOSS/proprietary story
    // (P2.2 adds the FOSS-purity guard on top of this dimension).
    flavorDimensions += "tier"
    productFlavors {
        create("gms") {
            dimension = "tier"
        }
        create("noGms") {
            dimension = "tier"
        }
    }

    // showcase/Wear.1: mirrors :app's testOptions — without isIncludeAndroidResources, Robolectric
    // can't resolve the merged manifest's package at unit-test time (falls back to
    // "org.robolectric.default", breaking createComposeRule()'s ActivityScenario launch).
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:platform"))
    implementation(project(":feature:tracking"))
    implementation(project(":stub"))
    // Napier: KMP logging used by the gms-flavor DataLayer bridges (the core modules expose it via
    // api, but that's hidden through their implementation() edges, so declare it directly).
    implementation(libs.napier)

    // Compose for Wear OS.
    implementation(platform(libs.compose.bom))
    implementation(libs.wear.compose.foundation)
    implementation(libs.wear.compose.material3)
    implementation(libs.wear.compose.navigation)
    implementation(libs.activity.compose)
    // P2.3: WearMilewayTheme's @Preview (round-watch device preview).
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    // P2.4: WearViewModel (androidx.lifecycle.ViewModel) + collectAsStateWithLifecycle in WearRootScreen.
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.runtime.compose)

    // Tile / complication / ongoing-activity surfaces (already present before this task).
    implementation(libs.wear.protolayout)
    implementation(libs.wear.tiles)
    implementation(libs.wear.tiles.material)
    implementation(libs.wear.complications.datasource)
    implementation(libs.wear.ongoing)
    implementation(libs.core.ktx)
    implementation("com.google.guava:guava:33.6.0-android")

    // Koin — WearAppGraph boots the same coreDataModule/trackingModule/stubModule graph the phone uses.
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)

    // P2.9: phone->watch snapshot Data Layer sync (WearDataLayerSyncBridge), gms flavor ONLY.
    // noGms binds WatchSyncBridge to NoopWatchSyncBridge instead (wear/src/noGms) — the
    // FOSS-purity guard above enforces this never leaks onto noGmsReleaseRuntimeClasspath.
    "gmsImplementation"(libs.play.services.wearable)
    "gmsImplementation"(libs.kotlinx.coroutines.play.services)

    // showcase/Wear.1: Roborazzi host-render screenshots for docs/screenshots (mirrors :app's wiring).
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi.core)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    // createComposeRule()'s ActivityScenarioRule needs a resolvable androidx.activity.ComponentActivity
    // in the merged test manifest — this artifact provides it (mirrors :app's debugImplementation).
    debugImplementation(libs.compose.ui.test.manifest)
}

// ─── P2.2, FOSS-purity dependency guard for the wear noGms flavor ───────────────────────────────────
// Mirrors :app's verifyNoGmsDependencyPrefixes (FLFD.2): fails if a GMS/Play/Firebase coordinate
// leaks onto noGmsReleaseRuntimeClasspath. The wear surfaces (tile/complication/ongoing-activity)
// are all AndroidX (androidx.wear.*), never play-services — but :wear depends on :core:platform,
// whose androidMain unconditionally implements the SAME pre-existing FusedLocation + ML Kit OCR
// prime-feature chain :app already allowlists (LocationTracker / TextRecognizer, not flavor-split
// at the core:platform level — out of P2.2 scope to change). Allowlist mirrors :app's exactly so
// this guard's real job — catching a FUTURE GMS leak, e.g. an unguarded play-services-wearable ref
// outside wear/src/gms (P2.9/P2.10) — still works.
// Inside afterEvaluate so the AGP-created variant configuration exists when we look it up.
afterEvaluate {
    val forbiddenPrefixes =
        listOf(
            "com.google.android.gms",
            "com.google.android.play",
            "com.google.firebase",
            "com.google.maps.android",
            "com.android.installreferrer",
        )
    val allowlist =
        setOf(
            // Pre-existing FusedLocation chain (core:platform AndroidLocationTracker).
            "com.google.android.gms:play-services-location",
            "com.google.android.gms:play-services-base",
            "com.google.android.gms:play-services-basement",
            "com.google.android.gms:play-services-tasks",
            // Pre-existing ML Kit OCR (core:platform AndroidTextRecognizer / doc scanner).
            "com.google.android.gms:play-services-mlkit-document-scanner",
            "com.google.android.gms:play-services-mlkit-text-recognition",
            "com.google.android.gms:play-services-mlkit-text-recognition-common",
            // Transitive Firebase infra pulled by the play-services libs above (NOT messaging/analytics/crashlytics).
            "com.google.firebase:firebase-annotations",
            "com.google.firebase:firebase-components",
            "com.google.firebase:firebase-encoders",
            "com.google.firebase:firebase-encoders-json",
        )
    val verifyTask =
        tasks.register("verifyNoGmsDependencyPrefixes") {
            group = "verification"
            description = "Fails if a proprietary dependency leaks into the wear noGms (F-Droid) release classpath."
            // Resolves the variant classpath at execution time via the component graph (not artifacts, which
            // would hit variant-attribute ambiguity for project deps).
            notCompatibleWithConfigurationCache("Resolves the noGmsReleaseRuntimeClasspath component graph")
            doLast {
                val deps =
                    configurations.getByName("noGmsReleaseRuntimeClasspath")
                        .incoming.resolutionResult.allComponents
                        .mapNotNull { it.id as? ModuleComponentIdentifier }
                        .map { "${it.group}:${it.module}" }
                val violations =
                    deps
                        .filter { dep -> forbiddenPrefixes.any { dep.startsWith(it) } && dep !in allowlist }
                        .distinct()
                if (violations.isNotEmpty()) {
                    throw GradleException(
                        "wear noGms (F-Droid) release leaks proprietary dependencies: $violations",
                    )
                }
            }
        }
    tasks.named("check").configure { dependsOn(verifyTask) }
    tasks.matching { it.name == "assembleNoGmsRelease" }.configureEach { dependsOn(verifyTask) }
}
