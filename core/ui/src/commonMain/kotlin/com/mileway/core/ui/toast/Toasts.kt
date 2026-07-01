package com.mileway.core.ui.toast

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** A toast's severity, used to pick the leading icon + accent colour. */
enum class ToastType { Success, Error, Info, Warning }

/** Optional action button shown on a toast. */
data class ToastAction(val label: String, val onClick: () -> Unit)

/** A toast payload rendered by [AppToast] / [AppToastHost]. */
data class ToastData(
    val title: String,
    val description: String,
    val type: ToastType = ToastType.Info,
    val primaryAction: ToastAction? = null,
    val secondaryAction: ToastAction? = null,
)

/** Process-wide toast event channel. [AppToastHost] collects from it and shows the rich [AppToast] card. */
internal object ToastController {
    private val _events = MutableSharedFlow<ToastData>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()

    fun emit(data: ToastData) {
        _events.tryEmit(data)
    }
}

/**
 * Cross-platform toast facade. Replaces the former koffee-based system (koffee has no iOS target) with a
 * fully multiplatform rich-card toast. `show(...)` emits to [ToastController]; [AppToastHost] renders it.
 * The public API is unchanged so existing call sites keep working.
 */
object Toasts {
    enum class ToastScenario { Gps, Battery, Permissions, Success, Error, Info, Warning }

    fun show(
        scenario: ToastScenario,
        title: String,
        description: String,
        primaryAction: ToastAction? = null,
        secondaryAction: ToastAction? = null,
    ) = show(title, description, typeFor(scenario), primaryAction, secondaryAction)

    fun show(
        title: String,
        description: String,
        type: ToastType = ToastType.Info,
        primaryAction: ToastAction? = null,
        secondaryAction: ToastAction? = null,
    ) {
        ToastController.emit(ToastData(title, description, type, primaryAction, secondaryAction))
    }

    private fun typeFor(scenario: ToastScenario): ToastType =
        when (scenario) {
            ToastScenario.Gps, ToastScenario.Battery, ToastScenario.Warning -> ToastType.Warning
            ToastScenario.Permissions, ToastScenario.Error -> ToastType.Error
            ToastScenario.Success -> ToastType.Success
            ToastScenario.Info -> ToastType.Info
        }
}
