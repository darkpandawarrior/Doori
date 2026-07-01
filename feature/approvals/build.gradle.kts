plugins {
    id("mileway.cmp.feature")
}

kotlin {
    android {
        namespace = "com.mileway.feature.approvals"
        compileSdk = 37
        minSdk = 30
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(project(":core:ui"))
        }
    }
}
