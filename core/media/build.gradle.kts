plugins {
    // V26 P26.AND: real launcher actuals need @Composable (rememberLauncherForActivityResult,
    // Peekaboo's rememberImagePickerLauncher) — upgraded from shared.kmp.library, modelled on
    // core/ui/build.gradle.kts.
    id("shared.kmp.compose")
    id("mileway.kmp.desktop")
}

kotlin {
    android {
        namespace = "com.mileway.core.media"
        compileSdk = 37
        minSdk = 30
        // V26 P26.CONV: enable JVM host execution of commonTest so the odometer OCR
        // characterization tests (ocr/) run in the gradle gate.
        withHostTest {}
    }

    sourceSets {
        commonMain.dependencies {
            // The launcher contract is @Composable as of V26 P26.AND (see MediaCaptureLauncher.kt).
            implementation(libs.runtime)
            // V26 P26.CONV: the sole odometer OCR pipeline (ocr/ package) wraps core:ai's
            // TextRecognizer/DocumentIntelligence — the real ML Kit/Vision recognizers — instead of
            // each feature reimplementing its own platform actual.
            implementation(project(":core:ai"))
            // V26 P26.SHEET: rememberMediaCaptureLauncher renders core:ui's OcrResultHost/
            // OcrBatchResultsSheet directly after capture when config.enableOcr is set — no
            // feature module needed as a go-between. No cycle: core:ui doesn't depend on core:media.
            implementation(project(":core:ui"))
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        androidMain.dependencies {
            implementation(libs.activity.compose)
            implementation(libs.peekaboo.image.picker)
            implementation(libs.mlkit.document.scanner)
            // V26 P26.CONV: MlKitGalleryMultiPassRecognizer's real multi-pass gallery-image OCR.
            implementation(libs.mlkit.text.recognition)
            implementation(libs.kotlinx.coroutines.play.services)
        }
        // V26 P26.IOS.1: Peekaboo gallery picker + PeekabooCamera (camera preview composable).
        // ui/foundation are needed directly here for the Dialog+Modifier.fillMaxSize() host that
        // presents PeekabooCamera (see MediaCaptureLauncher.ios.kt) — iOS-only use, Android's
        // CaptureMode.Camera stays on feature:media's own CameraCaptureScreen.
        iosMain.dependencies {
            implementation(libs.peekaboo.image.picker)
            implementation(libs.peekaboo.ui)
            implementation(libs.ui)
            implementation(libs.foundation)
        }
    }
}
