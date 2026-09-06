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
            // Real data sources for OfflineAssistantEngine's grounded replies — the same
            // repositories the approvals/cards/advances/expense screens themselves read, so the
            // assistant never states a number those screens wouldn't also show.
            implementation(project(":feature:approvals"))
            implementation(project(":feature:cards"))
            implementation(project(":feature:advances"))
            implementation(project(":feature:logging"))
            // LlmGateway/FoundationModelsLlmGateway actuals: kmp-toolkit's :ai OnDeviceLlm seam,
            // same engine core:ai's analyzers use for document extraction/FoundationModelsAnalyzer,
            // one Swift bridge registration shared with core:ai. Declared in commonMain (not per
            // platform) because it also carries KeywordClassifier<T>, used from commonMain to
            // route chat intents instead of a hand-rolled per-feature matcher.
            //
            // F-Droid build only: drop com.google.mediapipe:tasks-genai, whose
            // libllm_inference_engine_jni.so is ~44MB across the two shipped ABIs. Safe to
            // exclude: MediaPipeOnDeviceLlm and MediaPipeModelManager never touch mediapipe
            // types in their constructors or in isAvailable(); generate() is the only call
            // site that does, and it is wrapped in runCatching{}.getOrNull(), so a missing
            // class degrades to null and CompositeOnDeviceLlm falls through to
            // MlKitGenAiOnDeviceLlm. Both this app's analyzers use the ML Kit seam anyway,
            // which stays intact. Android-only concern, but the exclude must live on this single
            // commonMain declaration — a second per-platform declaration of the same coordinate
            // would pull mediapipe back in through its own unexcluded edge.
            val fdroidBuild = providers.gradleProperty("fdroid").isPresent
            implementation("com.siddharth.kmp:ai:1.0.0") {
                if (fdroidBuild) {
                    exclude(group = "com.google.mediapipe", module = "tasks-genai")
                }
            }
        }
        androidMain.dependencies {
            implementation(libs.datastore.preferences)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
