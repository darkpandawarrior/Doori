plugins {
    id("shared.kmp.library")
    id("mileway.kmp.desktop")
}

kotlin {
    android {
        namespace = "com.mileway.core.ai"
        compileSdk = 37
        minSdk = 30
    }

    sourceSets {
        commonMain.dependencies {
            // DocumentIntelligence.analyze() runs aiAnalyzer/textRecognizer concurrently via
            // coroutineScope { async {} }.
            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            // TextRecognizer actual: ML Kit on-device Latin text recognition (same artifact
            // core:platform's AndroidTextRecognizer uses for feature:tracking's odometer OCR).
            implementation(libs.mlkit.text.recognition)
            // DocumentAiAnalyzer actual: ML Kit GenAI Prompt API (Gemini Nano). EXPERIMENTAL —
            // see MlKitGenAiAnalyzer.
            implementation(libs.mlkit.genai.prompt)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
