plugins {
    id("shared.cmp.feature")
}

kotlin {
    android {
        namespace = "com.mileway.feature.cards"
        compileSdk = 37
        minSdk = 30
        // P29.C.1: run commonTest (CardDetailViewModel KYC-completion test) on the JVM host.
        withHostTest {}
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(project(":core:ui"))
            // PLAN_V24 P4.3: LocalOtpEngine (CARD_KYC) + PluginRegistry for the KYC wizard + gating.
            implementation(project(":core:data"))
            // V26 P26.SITE.4: AttachDocument gets a real picker via the shared core:media launcher.
            implementation(project(":core:media"))
        }
        androidMain.dependencies {
            implementation(project(":stub"))
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
