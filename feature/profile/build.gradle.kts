plugins {
    id("shared.cmp.feature")
}

kotlin {
    android {
        namespace = "com.mileway.feature.profile"
        compileSdk = 37
        minSdk = 30
        // P4.2: Enable JVM host execution of commonTest for the AdvanceViewModel detail-load test.
        withHostTest {}
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(project(":core:ui"))
            implementation(project(":core:data"))
            implementation(project(":core:network"))
            implementation(project(":core:security"))
            // V15 RF.4: ReferralManager + ShareSheet (LocalShareSheet) types live in core:platform.
            implementation(project(":core:platform"))
            // P6.4: ActiveSessionsRepository seeds from ProfileMockData.sessions() on first run.
            implementation(project(":stub"))
            // V26 P26.SITE.3: avatar/KYC-doc/self-audit/vehicle-photo pickers route through the
            // shared core:media launcher instead of hand-rolled ActivityResultContracts.
            implementation(project(":core:media"))
            // PLAN_V24 P3.3: render the picked profile photo (same loader other feature modules use).
            implementation(libs.coil3.compose)
        }
        androidMain.dependencies {
            implementation("androidx.appcompat:appcompat:1.8.0")
            // BiometricGuard (BiometricPrompt helper) now lives in the toolkit :security module.
            implementation("com.siddharth.kmp:security:1.0.0")
            // AiSettingsSection/AiSettingsState (consent, on-device model download, BYOK cloud
            // key) — SettingsScreen's new AI card, wired via ProfileModule.
            //
            // F-Droid build only: drop com.google.mediapipe:tasks-genai (~44MB), matching
            // core:ai/build.gradle.kts's own exclude on this coordinate — every Android edge that
            // pulls in com.siddharth.kmp:ai must apply it, or the transitive dependency leaks back
            // in through whichever edge omits it.
            val fdroidBuild = providers.gradleProperty("fdroid").isPresent
            implementation("com.siddharth.kmp:ai:1.0.0") {
                if (fdroidBuild) {
                    exclude(group = "com.google.mediapipe", module = "tasks-genai")
                }
            }
            implementation("com.siddharth.kmp:llm-chat:1.0.0")
            implementation("com.siddharth.kmp:designsystem:1.0.0")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
