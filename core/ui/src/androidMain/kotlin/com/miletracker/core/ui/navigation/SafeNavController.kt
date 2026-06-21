package com.miletracker.core.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder

fun NavController.navigateSafely(
    route: String,
    builder: NavOptionsBuilder.() -> Unit = {},
) {
    try {
        navigate(route, builder)
    } catch (_: IllegalArgumentException) {
        // destination not in the current graph — ignore silently (avoids crashes from duplicate taps)
    }
}

fun NavController.popBackStackSafely(): Boolean = if (previousBackStackEntry != null) popBackStack() else false
