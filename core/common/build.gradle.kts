plugins {
    id("shared.kmp.library")
    id("mileway.kmp.desktop")
}

kotlin {
    android {
        namespace = "com.mileway.core.common"
        compileSdk = 37
        minSdk = 30
        // V31 Z.5a: run commonTest on the JVM host so it counts toward the quality-gate's
        // ./gradlew testAndroidHostTest aggregate (AGP KMP library plugin disables host tests by default).
        withHostTest {}
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
