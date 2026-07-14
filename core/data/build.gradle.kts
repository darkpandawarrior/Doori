plugins {
    id("mileway.kmp.library.watchos")
    id("mileway.kmp.desktop")
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.kotlinSerialization)
}

room {
    schemaDirectory("$projectDir/schemas")
}

kotlin {
    android {
        namespace = "com.mileway.core.data"
        compileSdk = 37
        minSdk = 30
        withHostTest {}
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.room.runtime)
            implementation(libs.datastore.preferences.core)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.koin.core)
            // Durable outbox + change-bus, extracted to kmp-toolkit (own separate Room DB — see
            // OutboxDatabase in the toolkit module; RoomSubmitOutbox/RoomChangeBus no longer live here).
            api("com.siddharth.kmp:offline-outbox:1.0.0")
        }
        androidMain.dependencies {
            implementation(libs.sqlite.bundled)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.datastore.preferences)
            implementation(libs.koin.android)
        }
        // appleMain is the applyDefaultHierarchyTemplate() intermediate source set shared by
        // iosMain + watchosMain — actuals needed on both platforms (e.g. epochMillis(),
        // buildMilewayDatabase()) live here so watchos targets resolve them too (P3.2).
        appleMain.dependencies {
            implementation(libs.sqlite.bundled)
        }
        val desktopMain by getting {
            dependencies {
                implementation(libs.sqlite.bundled)
            }
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

dependencies {
    add("kspCommonMainMetadata", libs.room.compiler)
    add("kspAndroid", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
    add("kspWatchosArm64", libs.room.compiler)
    add("kspWatchosSimulatorArm64", libs.room.compiler)
    add("kspWatchosDeviceArm64", libs.room.compiler)
    add("kspDesktop", libs.room.compiler)
}
