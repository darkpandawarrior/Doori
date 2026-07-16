plugins {
    id("shared.cmp.feature")
}

// PLAN_V36 P5: bundled media (commonMain/composeResources/files/whatsnew/<entryId>/…), resolved
// via Res.getUri() in the AsyncImage call sites. Internal-only — nothing outside this module
// resolves feature:whatsnew's Res, so publicResClass stays at its default (false), unlike core:ui.
compose.resources {
    packageOfResClass = "com.mileway.feature.whatsnew.resources"
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
            // PLAN_V36 P5: CMP's resource-class generator only detects "Auto" mode via a DIRECT
            // dependency on this artifact — inheriting it transitively through core:ui (below)
            // does not trigger generateComposeResClass for THIS module's own files/ resources.
            implementation(compose.components.resources)
            implementation(project(":core:common"))
            implementation(project(":core:ui"))
            // PLAN_V36 P2: WhatsNewVersionProvider — the badge contract Settings (feature:profile)
            // consumes without a feature-to-feature dependency.
            implementation(project(":core:data"))
            implementation(project(":core:platform"))
            // PLAN_V36 P3: hero image on the list card (empty for every catalog entry until P5's
            // media pipeline lands, but the card must compile against a real AsyncImage now).
            implementation(libs.coil3.compose)
            // PLAN_V36 P7: engagement-event payloads (JsonObject only — no @Serializable classes,
            // so the kotlin.plugin.serialization compiler plugin isn't needed here).
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
