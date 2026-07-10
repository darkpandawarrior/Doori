plugins {
    id("shared.kmp.compose")
    id("mileway.kmp.desktop")
}

kotlin {
    android {
        namespace = "com.mileway.core.forms"
        compileSdk = 37
        minSdk = 30
        // Run commonTest on the Android JVM host (not desktopTest — now that this module depends on
        // core:ui, desktopTest drags in the JCEF webview desktop artifact, which doesn't resolve).
        withHostTest {}
    }

    sourceSets {
        commonMain.dependencies {
            // UiText (validation error messages) lives in core:common.
            api(project(":core:common"))
            implementation(libs.runtime)
            implementation(libs.foundation)
            implementation(libs.material3)
            implementation(libs.ui)
            implementation(libs.material.icons.extended)
            implementation(libs.ui.tooling.preview.mp)
            implementation(libs.kotlinx.datetime)
            // DesignTokens, the shared string table (Res.string.*), SearchablePickerSheet,
            // WheelDatePickerDialog/WheelTimePickerDialog, PreviewLightDark/PreviewSurface.
            implementation(project(":core:ui"))
            // V27 P27.F.2: FILE_PDF field wiring — rememberMediaCaptureLauncher for the attachment
            // control.
            implementation(project(":core:media"))
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
