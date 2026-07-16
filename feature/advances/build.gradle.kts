plugins {
    id("shared.cmp.feature")
}

kotlin {
    android {
        namespace = "com.mileway.feature.advances"
        compileSdk = 37
        minSdk = 30
        // PLAN_V35.P3: run commonTest (validators/UPI parser/mock repos) on the JVM host.
        withHostTest {}
    }

    sourceSets {
        // PLAN_V35.P3 is domain+data only (no screens yet — that's P4), so no :core:ui/:core:common
        // dependency until a composable actually needs one.
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
