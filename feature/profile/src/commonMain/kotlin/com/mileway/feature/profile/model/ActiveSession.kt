package com.mileway.feature.profile.model

/**
 * PLAN_V22 P6.4: how recently a device session was active, relative to "now" — derived at read
 * time from [ActiveSession.lastActiveMillis] via [deriveSessionStatus] rather than stored, so it
 * never goes stale.
 */
enum class SessionStatus { ACTIVE, RECENT, IDLE }

private const val ACTIVE_THRESHOLD_MS = 5 * 60 * 1_000L // 5 minutes
private const val RECENT_THRESHOLD_MS = 24 * 60 * 60 * 1_000L // 24 hours

/**
 * Active: last seen within 5 minutes of [nowMillis]. Recent: within 24 hours. Idle: older than
 * that (or, defensively, a [lastActiveMillis] that is somehow in the future).
 */
fun deriveSessionStatus(
    lastActiveMillis: Long,
    nowMillis: Long,
): SessionStatus {
    val age = nowMillis - lastActiveMillis
    return when {
        age in 0..ACTIVE_THRESHOLD_MS -> SessionStatus.ACTIVE
        age in 0..RECENT_THRESHOLD_MS -> SessionStatus.RECENT
        else -> SessionStatus.IDLE
    }
}

/**
 * A device session this account is signed in on, promoted from the read-only `SessionsDialog`
 * list to [com.mileway.feature.profile.ui.screens.ActiveSessionsScreen]'s full revoke-capable
 * surface. [isCurrent] marks the one row [ActiveSessionsViewModel][com.mileway.feature.profile
 * .viewmodel.ActiveSessionsViewModel] refuses to revoke (the device driving this app instance).
 */
data class ActiveSession(
    val id: String,
    val deviceName: String,
    val platform: String,
    val lastActiveMillis: Long,
    val isCurrent: Boolean,
    // PLAN_V24 P7.2: session detail enrichment (source: the reference app DeviceDetails{os, appVersion, ip}).
    // Defaulted so pre-P7.2 construction sites (tests, mappers) compile unchanged.
    val os: String = "",
    val appVersion: String = "",
    val ip: String = "",
) {
    fun status(nowMillis: Long): SessionStatus = deriveSessionStatus(lastActiveMillis, nowMillis)

    /** PLAN_V24 P7.2: device family, derived from [platform] (not stored) — source `DeviceType`. */
    val deviceType: SessionDeviceType get() = deriveDeviceType(platform)
}

/** PLAN_V24 P7.2: the device family a session runs on (source: the reference app `DeviceType{IOS, ANDROID, WEB}`). */
enum class SessionDeviceType { ANDROID, IOS, WEB, UNKNOWN }

/** Classifies a [platform] label (e.g. "Android 15", "iPadOS 18", "Web") into a [SessionDeviceType]. */
fun deriveDeviceType(platform: String): SessionDeviceType {
    val p = platform.lowercase()
    return when {
        "android" in p -> SessionDeviceType.ANDROID
        "ios" in p || "ipad" in p || "iphone" in p -> SessionDeviceType.IOS
        "web" in p || "chrome" in p || "windows" in p || "mac" in p -> SessionDeviceType.WEB
        else -> SessionDeviceType.UNKNOWN
    }
}
