plugins {
    id("shared.cmp.feature")
}

kotlin {
    android {
        namespace = "com.mileway.feature.whatsnew"
        compileSdk = 37
        minSdk = 30
        // PLAN_V36 P1: catalog invariant tests run on the JVM host.
        withHostTest {}
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(project(":core:ui"))
            // PLAN_V36 P2: WhatsNewVersionProvider — the badge contract Settings (feature:profile)
            // consumes without a feature-to-feature dependency.
            implementation(project(":core:data"))
            implementation(project(":core:platform"))
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
