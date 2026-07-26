plugins {
    id("shared.cmp.feature")
}

kotlin {
    android {
        namespace = "com.mileway.feature.travel"
        compileSdk = 37
        minSdk = 30
        // Run this module's commonTest (repository + SearchProvider suites) on the JVM host,
        // same as :feature:approvals and :feature:events. Without it the commonTest source set
        // compiles for no test target and the tests silently never run.
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
