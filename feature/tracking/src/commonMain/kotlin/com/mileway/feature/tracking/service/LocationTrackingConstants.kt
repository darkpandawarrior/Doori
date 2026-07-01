package com.mileway.feature.tracking.service

object LocationTrackingConstants {
    // P-A.1: hard coordinate bounds — any reading outside these is a sensor error.
    const val COORD_LAT_MIN = -90.0
    const val COORD_LAT_MAX = 90.0
    const val COORD_LNG_MIN = -180.0
    const val COORD_LNG_MAX = 180.0

    // P-A.1: accuracy hard gates — impossibly precise (<= 0.1 m) or hopelessly noisy (> 250 m).
    const val ACCURACY_MIN_M = 0.1f
    const val ACCURACY_MAX_M = 250f

    // P-A.1: soft accuracy contribution gate (default 50 m); tunable via TrackingConfigManager.
    // Fixes above this threshold are persisted but excluded from cleanedDistance.
    const val MAX_ACCURACY_THRESHOLD_M = 50.0

    // P-A.1: exceptional-stationary allowance thresholds.
    // When speed ≤ this AND accuracy < EXCEPTIONAL_STATIONARY_ACCURACY_M AND there is recent movement,
    // the fix is allowed to contribute even if accuracy > MAX_ACCURACY_THRESHOLD_M.
    const val EXCEPTIONAL_STATIONARY_SPEED_MPS = 0.1f
    const val EXCEPTIONAL_STATIONARY_ACCURACY_M = 20f

    const val NOTIF_UPDATE_MIN_INTERVAL_MS = 5_000L
    const val MIN_DISTANCE_DELTA_FOR_UPDATE_M = 10.0
    const val MIN_SPEED_DELTA_FOR_UPDATE_MPS = 0.5f

    const val MILESTONE_INTERVAL_SHORT_M = 5000.0
    const val MILESTONE_INTERVAL_MEDIUM_M = 10000.0
    const val MILESTONE_INTERVAL_LONG_M = 25000.0
    const val MILESTONE_SHORT_THRESHOLD_M = 20000.0
    const val MILESTONE_MEDIUM_THRESHOLD_M = 100000.0
    const val MILESTONE_MIN_TIME_INTERVAL_MS = 120000L

    const val STATIONARY_SPEED_THRESHOLD_MPS = 0.8f
    const val MAX_ALLOWED_ACCURACY_WHEN_STATIONARY = 35f
    const val STATIONARY_SUPPRESSION_WINDOW = 5
    const val MIN_MOVE_TO_BREAK_SUPPRESSION_M = 15.0

    const val JITTER_DISTANCE_M = 8.0
    const val JITTER_IMPLIED_SPEED_MPS = 0.7

    const val PERMISSION_CHECK_MIN_INTERVAL_MS: Long = 15_000L

    const val WAKE_LOCK_TAG = "LocationTrackingService::WakeLock"
    const val WAKE_LOCK_MAX_DURATION_MS = 10 * 60 * 1000L

    const val NOTIFICATION_CHANNEL_ID = "mileway_tracking"
    const val NOTIFICATION_ID = 1001
    const val NOTIFICATION_CHANNEL_NAME = "Live Tracking"

    const val ACTION_PAUSE = "com.mileway.ACTION_PAUSE"
    const val ACTION_RESUME = "com.mileway.ACTION_RESUME"
    const val ACTION_STOP = "com.mileway.ACTION_STOP"
    const val ACTION_FIX_GPS = "com.mileway.ACTION_FIX_GPS"
    const val ACTION_TRACKING_STARTED = "com.mileway.TRACKING_STARTED"
    const val ACTION_TRACKING_STOPPED = "com.mileway.TRACKING_STOPPED"
    const val ACTION_TRACKING_PAUSED = "com.mileway.TRACKING_PAUSED"
    const val ACTION_TRACKING_RESUMED = "com.mileway.TRACKING_RESUMED"

    const val EXTRA_TOKEN = "extra_token"
    const val EXTRA_DISTANCE = "extra_distance"
}
