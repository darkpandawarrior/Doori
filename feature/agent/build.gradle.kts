plugins {
    id("miletracker.cmp.feature")
}

kotlin {
    android {
        namespace = "com.miletracker.feature.agent"
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
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
