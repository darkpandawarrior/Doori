plugins {
    id("shared.kmp.library")
    id("mileway.kmp.desktop")
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
            // DocumentIntelligence.analyze() runs aiAnalyzer/textRecognizer concurrently via
            // coroutineScope { async {} }.
            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            // TextRecognizer actual: ML Kit on-device Latin text recognition.
            implementation(libs.mlkit.text.recognition)
            // DocumentAiAnalyzer actual delegates the model call to kmp-toolkit's :ai OnDeviceLlm
            // seam (MlKitGenAiOnDeviceLlm) — this module owns prompt building + JSON parsing only,
            // not the ML Kit GenAI client itself. See MlKitGenAiAnalyzer.
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
