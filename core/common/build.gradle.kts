plugins {
    id("mileway.kmp.library")
}

kotlin {
    android {
        namespace = "com.mileway.core.common"
        compileSdk = 37
        minSdk = 30
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.coroutines.core)
            // Napier: KMP logging usable from commonMain (android.util.Log cannot go here).
            api(libs.napier)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
