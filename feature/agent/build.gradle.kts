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
            // LlmGateway actual: kmp-toolkit's :ai OnDeviceLlm seam (MlKitGenAiOnDeviceLlm), same
            // engine core:ai's MlKitGenAiAnalyzer uses for document extraction. EXPERIMENTAL —
            // see MlKitLlmGateway. Same coordinate core:ai/build.gradle.kts already uses.
            implementation("com.siddharth.kmp:ai:1.0.0")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
