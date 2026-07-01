plugins {
    id("mileway.cmp.feature")
}

kotlin {
    android {
        namespace = "com.mileway.feature.profile"
        compileSdk = 37
        minSdk = 30
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(project(":core:ui"))
            implementation(project(":core:data"))
            implementation(project(":core:network"))
            implementation(project(":core:security"))
            // V15 RF.4: ReferralManager + ShareSheet (LocalShareSheet) types live in core:platform.
            implementation(project(":core:platform"))
        }
        androidMain.dependencies {
            implementation("androidx.appcompat:appcompat:1.7.0")
            implementation(project(":stub"))
        }
    }
}
