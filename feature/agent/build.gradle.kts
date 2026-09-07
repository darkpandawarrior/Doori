plugins {
    id("shared.cmp.feature")
}

kotlin {
    android {
        namespace = "com.mileway.feature.agent"
        compileSdk = 37
        minSdk = 30
        withHostTest {}
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.markdown.renderer)
            implementation(libs.markdown.renderer.m3)
            implementation(project(":core:ui"))
            implementation(project(":core:data"))
            implementation(project(":core:platform"))
            implementation(project(":stub"))
        }
        iosMain.dependencies {
            // FoundationModelsLlmGateway actual: kmp-toolkit's :ai OnDeviceLlm seam
            // (FoundationModelsOnDeviceLlm) — same coordinate core:ai/build.gradle.kts uses for
            // FoundationModelsAnalyzer, both sharing ONE Swift bridge registration.
            implementation("com.siddharth.kmp:ai:1.0.0")
        }
        androidMain.dependencies {
            implementation(libs.datastore.preferences)
            // LlmGateway actual: kmp-toolkit's :ai OnDeviceLlm seam (MlKitGenAiOnDeviceLlm), same
            // engine core:ai's MlKitGenAiAnalyzer uses for document extraction. EXPERIMENTAL —
            // see MlKitLlmGateway. Same coordinate core:ai/build.gradle.kts already uses.
            //
            // F-Droid build only: drop com.google.mediapipe:tasks-genai, whose
            // libllm_inference_engine_jni.so is ~44MB across the two shipped ABIs. Safe to
            // exclude: MediaPipeOnDeviceLlm and MediaPipeModelManager never touch mediapipe
            // types in their constructors or in isAvailable(); generate() is the only call
            // site that does, and it is wrapped in runCatching{}.getOrNull(), so a missing
            // class degrades to null and CompositeOnDeviceLlm falls through to
            // MlKitGenAiOnDeviceLlm. Both this app's analyzers use the ML Kit seam anyway,
            // which stays intact.
            val fdroidBuild = providers.gradleProperty("fdroid").isPresent
            implementation("com.siddharth.kmp:ai:1.0.0") {
                if (fdroidBuild) {
                    exclude(group = "com.google.mediapipe", module = "tasks-genai")
                }
            }
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
