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
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
