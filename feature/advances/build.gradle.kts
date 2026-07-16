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
        // PLAN_V35.P4: screens/ViewModels land here, so :core:ui (DesignTokens, scaffolds,
        // resources) and :core:common (UiText) are now real dependencies.
        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(project(":core:ui"))
            implementation(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
