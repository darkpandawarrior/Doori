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
        androidMain.dependencies {
            implementation(libs.datastore.preferences)
            // LlmGateway actual: ML Kit GenAI Prompt API (Gemini Nano), same engine core:ai's
            // MlKitGenAiAnalyzer uses for document extraction. EXPERIMENTAL — see MlKitLlmGateway.
            implementation(libs.mlkit.genai.prompt)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
