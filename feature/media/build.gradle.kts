plugins {
    id("mileway.cmp.feature")
}

kotlin {
    android {
        namespace = "com.mileway.feature.media"
        compileSdk = 37
        minSdk = 30
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.coil3.compose)
            implementation(project(":core:common"))
            implementation(project(":core:data"))
            implementation(project(":core:ui"))
        }
        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.play.services)
            implementation(libs.mlkit.document.scanner)
            implementation(libs.mlkit.text.recognition)
            implementation(libs.camera.core)
            implementation(libs.camera.camera2)
            implementation(libs.camera.lifecycle)
            implementation(libs.camera.view)
            implementation(project(":stub"))
        }
    }
}
