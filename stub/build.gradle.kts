plugins {
    id("mileway.kmp.library")
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    android {
        namespace = "com.mileway.stub"
        compileSdk = 37
        minSdk = 30
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.koin.core)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(project(":core:data"))
            implementation(project(":core:network"))
            // V15 PF.5: DemoConfigManager returns UpdateConfig (defined in core:platform).
            implementation(project(":core:platform"))
        }
        androidMain.dependencies {
            implementation(libs.koin.android)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.datastore.preferences)
        }
    }
}
