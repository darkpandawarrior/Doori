plugins {
    id("shared.kmp.library")
    id("mileway.kmp.desktop")
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    android {
        namespace = "com.mileway.core.ai"
        compileSdk = 37
        minSdk = 30
        // V31 Z.5a: run commonTest on the JVM host so it counts toward the quality-gate's
        // ./gradlew testAndroidHostTest aggregate (AGP KMP library plugin disables host tests by default).
        withHostTest {}
    }

    sourceSets {
        commonMain.dependencies {
            // suspend-fun plumbing throughout this module; MlKitTextRecognizer's androidMain
            // actual also needs it directly.
            implementation(libs.kotlinx.coroutines.core)
            // DocumentAiAnalyzer.extract returns kmp-toolkit's typed AiResult<AiExtraction> —
            // :result is otherwise dependency-free (same coordinate :stub already carries).
            implementation("com.siddharth.kmp:result:1.0.0")
            // DocumentExtractionFields (StructuredOutput<T>'s decode target) is @Serializable —
            // already used elsewhere in this repo (core:network, :contract, ...), just not in
            // this module until now.
            implementation(libs.kotlinx.serialization.json)
            // Single commonMain declaration (moved from separate android/iosMain declarations —
            // AiAssistantSettings.kt now needs ModelManager/OnDeviceLlm/CloudOnDeviceLlm on every
            // target this module compiles for, including the new desktop one) so the F-Droid
            // exclude below applies once instead of leaking mediapipe back in through a second,
            // unexcluded per-platform edge — same pattern feature:agent/build.gradle.kts already
            // uses for this identical coordinate; see that file's own comment.
            val fdroidBuild = providers.gradleProperty("fdroid").isPresent
            implementation("com.siddharth.kmp:ai:1.0.0") {
                if (fdroidBuild) {
                    exclude(group = "com.google.mediapipe", module = "tasks-genai")
                }
            }
            // buildCloudFallback (AiAssistantSettings.kt) — the cloud BYOK provider chain the
            // desktop assistant runs prompts through; AiProvider/ProviderId/AiConfig/AiMessage/
            // buildProviderChain/loadAiProviderConfig all live here.
            implementation("com.siddharth.kmp:llm-chat:1.0.0")
        }
        androidMain.dependencies {
            // TextRecognizer actual: ML Kit on-device Latin text recognition.
            implementation(libs.mlkit.text.recognition)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
