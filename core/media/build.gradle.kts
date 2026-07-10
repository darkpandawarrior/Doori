plugins {
    id("shared.kmp.library")
    id("mileway.kmp.desktop")
}

kotlin {
    android {
        namespace = "com.mileway.core.media"
        compileSdk = 37
        minSdk = 30
    }

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
