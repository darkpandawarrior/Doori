plugins {
    id("shared.cmp.feature")
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
            // api: AttachmentItem/OcrResult/UploadState/AttachmentSource are re-exported below via
            // typealias, so consumers of feature:media (e.g. feature:tracking) need core:media's
            // real classes on their compile classpath too (V25 P25.A1.1).
            api(project(":core:media"))
        }
        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.play.services)
            implementation(libs.mlkit.document.scanner)
            implementation(libs.mlkit.text.recognition)
            implementation(libs.camera.core)
            implementation(libs.camera.camera2)
            implementation(libs.camera.lifecycle)
            implementation(libs.camera.view)
            // V26 P26.AND.4: EXIF orientation correction ahead of OCR (RealMediaRepository).
            implementation(libs.androidx.exifinterface)
            implementation(project(":stub"))
        }
    }
}
