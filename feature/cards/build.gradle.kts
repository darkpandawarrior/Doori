plugins {
    id("shared.cmp.feature")
}

kotlin {
    android {
        namespace = "com.mileway.feature.cards"
        compileSdk = 37
        minSdk = 30
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(project(":core:ui"))
            // PLAN_V24 P4.3: LocalOtpEngine (CARD_KYC) + PluginRegistry for the KYC wizard + gating.
            implementation(project(":core:data"))
        }
        androidMain.dependencies {
            implementation(project(":stub"))
        }
    }
}
