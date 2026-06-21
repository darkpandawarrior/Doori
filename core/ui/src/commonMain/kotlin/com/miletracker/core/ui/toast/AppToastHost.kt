package com.miletracker.core.ui.toast

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

private const val TOAST_DURATION_MS = 4_000L

/**
 * Hosts app-wide toasts. Collects [ToastController] events and renders the rich [AppToast] card with a
 * slide+fade animation, auto-dismissing after [TOAST_DURATION_MS]. Wrap the app content in this once at
 * the root. Fully multiplatform (Android + iOS), no koffee, no platform host coordination.
 */
@Composable
fun AppToastHost(content: @Composable () -> Unit) {
    var current by remember { mutableStateOf<ToastData?>(null) }
    // Retain the last payload through the exit animation (current goes null before the slide-out finishes).
    var lastShown by remember { mutableStateOf<ToastData?>(null) }
    if (current != null) lastShown = current

    LaunchedEffect(Unit) {
        ToastController.events.collect { data ->
            current = data
            delay(TOAST_DURATION_MS)
            current = null
        }
    }

    Box(Modifier.fillMaxSize()) {
        content()
        AnimatedVisibility(
            visible = current != null,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            lastShown?.let { data ->
                AppToast(
                    data = data,
                    onDismiss = { current = null },
                    onPrimary = { current = null },
                    onSecondary = { current = null },
                )
            }
        }
    }
}
