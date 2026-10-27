package com.mileway.feature.profile.data

/**
 * PLAN_V22 P6.8: a single "Video Tutorials" card on `HelpScreen` — Mileway's own local equivalent
 * of a reference-app video-tutorial library, minus any bundled video asset or codec dependency
 * (no `Media3`/`ExoPlayer` in this app's dependency graph yet, and pulling one in for a demo-data
 * feature would be exactly the kind of unnecessary dependency the project's guardrails call out).
 * [durationLabel] is the card's static runtime readout; actual "playback" is a locally simulated
 * progress state (see `VideoTutorialPlaybackState` in `HelpScreen.kt`), not a real video stream.
 */
data class VideoTutorial(
    val id: String,
    val title: String,
    val category: String,
    val durationLabel: String,
)

val VIDEO_TUTORIALS =
    listOf(
        VideoTutorial("vt1", "Starting your first journey", "Track Miles", "1:42"),
        VideoTutorial("vt2", "Submitting a mileage claim", "Expenses", "2:15"),
        VideoTutorial("vt3", "Creating a voucher from approved trips", "Expenses", "1:58"),
        VideoTutorial("vt4", "Switching between demo accounts", "Account", "1:10"),
    )
