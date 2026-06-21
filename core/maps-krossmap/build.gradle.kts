plugins {
    id("miletracker.android.library")
}

android {
    namespace = "com.miletracker.core.maps.krossmap"
}

dependencies {
    api(project(":core:maps"))

    // KrossMap: wraps Google Maps Compose (Android) + MapKit (iOS).
    // play-services-maps and maps-compose are transitive from KrossMap's POM.
    implementation(libs.krossmap)

    // Koin binding
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
}
