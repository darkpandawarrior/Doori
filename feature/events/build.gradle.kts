plugins {
    id("shared.cmp.feature")
}

kotlin {
    android {
        namespace = "com.mileway.feature.events"
        compileSdk {
            version = release(37) { minorApiLevel = 1 }
        }
        minSdk = 30
        // V29 P29.E: enable JVM host execution of commonTest for EventsRepository/EventDetailViewModel tests.
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
