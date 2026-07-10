plugins {
    id("shared.kmp.library")
    id("mileway.kmp.desktop")
}

kotlin {
    android {
        namespace = "com.mileway.core.forms"
        compileSdk = 37
        minSdk = 30
    }

    sourceSets {
        commonMain.dependencies {
            // UiText (validation error messages) lives in core:common.
            api(project(":core:common"))
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
