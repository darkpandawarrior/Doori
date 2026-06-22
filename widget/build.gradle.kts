// G11: Android-only Glance home-screen widget. Uses the shared android.library convention (com.android.library
// + Compose compiler + compose=true) so the Glance @Composable content compiles. Consumes the platform-neutral
// SurfaceSnapshot model/producer from :core:data (the producer already exists; this is the missing consumer).
plugins {
    id("miletracker.android.library")
}

android {
    namespace = "com.miletracker.widget"
}

dependencies {
    implementation(libs.glance.appwidget)
    implementation(project(":core:data"))
    // Room base types (RoomDatabase/close) — core:data keeps Room as implementation.
    implementation(libs.room.runtime)
    implementation(libs.kotlinx.coroutines.core)
}
