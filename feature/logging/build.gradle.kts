plugins {
    id("mileway.cmp.feature")
}

kotlin {
    android {
        namespace = "com.mileway.feature.logging"
        compileSdk = 37
        minSdk = 30
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(project(":core:data"))
            implementation(project(":core:network"))
            implementation(project(":core:ui"))
            implementation(project(":feature:tracking"))
        }
    }
}
