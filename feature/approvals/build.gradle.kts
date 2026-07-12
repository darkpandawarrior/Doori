plugins {
    id("shared.cmp.feature")
}

kotlin {
    android {
        namespace = "com.mileway.feature.approvals"
        compileSdk = 37
        minSdk = 30
        // PLAN_V28 P28.2: run commonTest (ClarificationRepository/ViewModel tests) on the JVM host.
        withHostTest {}
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(project(":core:data"))
            implementation(project(":core:ui"))
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
