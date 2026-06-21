-dontwarn **

# ── Kotlin / Coroutines ───────────────────────────────────────────────────────
# Without these, StateFlow/SharedFlow and continuation classes get obfuscated,
# causing StackOverflowErrors and lost coroutine debug info.
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-keep class kotlin.coroutines.Continuation

# ── OkHttp / Ktor ─────────────────────────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
# Ktor internal reflection via ServiceLoader
-keep class io.ktor.** { *; }
-keep interface io.ktor.** { *; }

# ── Napier logging ────────────────────────────────────────────────────────────
# Strip Napier in release, log calls are dead code after minification with this keep.
-assumenosideeffects class io.github.aakira.napier.Napier {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# ── MapLibre ─────────────────────────────────────────────────────────────────
-keep class org.maplibre.** { *; }
-keep interface org.maplibre.** { *; }

# ── Room ──────────────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-keep interface * extends androidx.room.RoomDatabase$Callback
-keep class * extends androidx.room.migration.Migration

# ── Koin ──────────────────────────────────────────────────────────────────────
-keepnames class * extends org.koin.core.module.Module
-keep class com.miletracker.** {
    @org.koin.core.annotation.* <fields>;
}

# ── Kotlin serialization ──────────────────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}

# ── OSMdroid ─────────────────────────────────────────────────────────────────
-keep class org.osmdroid.** { *; }
-keep class org.osmdroid.tileprovider.** { *; }

# ── Coil ─────────────────────────────────────────────────────────────────────
-keep class coil.** { *; }
-keep interface coil.** { *; }

# ── DataStore / Protobuf ─────────────────────────────────────────────────────
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
}

# ── WorkManager ───────────────────────────────────────────────────────────────
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ── SQLCipher ─────────────────────────────────────────────────────────────────
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }

# ── Application classes ───────────────────────────────────────────────────────
-keep class com.miletracker.** extends android.app.Application
-keep class com.miletracker.**.dao.** { *; }
-keep class com.miletracker.**.model.** { *; }
-keep class com.miletracker.**.di.** { *; }
