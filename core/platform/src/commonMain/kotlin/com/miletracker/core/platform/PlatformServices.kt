package com.miletracker.core.platform

import kotlinx.coroutines.flow.Flow

// Platform-neutral abstractions for device capabilities that have no single multiplatform library.
// Each interface is implemented per platform and bound through Koin (platformModule()).
// Camera capture and maps are NOT modelled here, they are Compose UI components (peekaboo/MapLibre)
// and live in the UI layer, not as background services.

/** A platform-neutral geographic sample. */
data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float = 0f,
    val timestampMillis: Long = 0L,
)

/** Continuous + one-shot location access. Android: FusedLocation/Compass; iOS: CoreLocation. */
interface LocationTracker {
    /** Hot stream of location updates while tracking is active. */
    val updates: Flow<GeoPoint>

    /** One-shot best-effort current location, or null if unavailable. */
    suspend fun current(): GeoPoint?

    fun start()

    fun stop()
}

/** OCR text recognition from raw image bytes. Android: ML Kit; iOS: Vision (VNRecognizeTextRequest). */
interface TextRecognizer {
    suspend fun recognize(imageBytes: ByteArray): String
}

/** Document scanning → captured page images as bytes. Android: ML Kit doc scanner; iOS: VisionKit. */
interface DocumentScanner {
    suspend fun scan(maxPages: Int = 1): List<ByteArray>
}

/** Local notifications. Android: NotificationManager + channels; iOS: UNUserNotificationCenter. */
interface NotificationScheduler {
    suspend fun ensurePermission(): Boolean

    fun notify(
        id: Int,
        title: String,
        body: String,
    )

    fun cancel(id: Int)
}

sealed interface BiometricResult {
    data object Success : BiometricResult

    data object Failed : BiometricResult

    data object Unavailable : BiometricResult

    data class Error(val message: String) : BiometricResult
}

/** Biometric auth. Android: BiometricPrompt; iOS: LocalAuthentication (LAContext). */
interface BiometricAuthenticator {
    fun isAvailable(): Boolean

    suspend fun authenticate(reason: String): BiometricResult
}

/**
 * A unit of periodic background work. Register an implementation in Koin under a *named* qualifier whose
 * name matches the `uniqueName` passed to [BackgroundScheduler.schedulePeriodic]; the platform worker
 * resolves and runs it. Keeps [BackgroundScheduler] decoupled from any concrete worker class.
 */
interface BackgroundTask {
    suspend fun run()
}

/** Background/periodic work. Android: WorkManager; iOS: BGTaskScheduler. */
interface BackgroundScheduler {
    fun schedulePeriodic(
        uniqueName: String,
        intervalMinutes: Long,
    )

    fun cancel(uniqueName: String)
}

/** Runtime permissions the app requests. Backed by moko-permissions where possible. */
enum class AppPermission { LOCATION, CAMERA, NOTIFICATIONS, STORAGE }

sealed interface PermissionResult {
    data object Granted : PermissionResult

    data object Denied : PermissionResult

    data object DeniedAlways : PermissionResult
}

/** Runtime permission requests. Android: moko-permissions / ActivityResult; iOS: moko-permissions. */
interface PermissionsProvider {
    suspend fun isGranted(permission: AppPermission): Boolean

    suspend fun request(permission: AppPermission): PermissionResult
}
