package com.mileway.core.ui.mvi

import com.siddharth.kmp.common.UiText

/** Sealed state for any screen that loads data. Drives the UI without boolean soup. */
sealed interface ScreenState<out T> {
    data object Loading : ScreenState<Nothing>

    data object Empty : ScreenState<Nothing>

    data object NoNetwork : ScreenState<Nothing>

    data class Error(val message: UiText, val cause: Throwable? = null) : ScreenState<Nothing>

    /** Loaded content. [isStale] marks data shown from cache while a refresh is in flight. */
    data class Content<T>(val data: T, val isStale: Boolean = false) : ScreenState<T>
}

val <T> ScreenState<T>.dataOrNull: T?
    get() = (this as? ScreenState.Content)?.data

val ScreenState<*>.isLoading: Boolean
    get() = this is ScreenState.Loading

/** Maps the content payload while preserving the non-content branches and [Content.isStale]. */
inline fun <T, R> ScreenState<T>.map(transform: (T) -> R): ScreenState<R> =
    when (this) {
        is ScreenState.Content -> ScreenState.Content(transform(data), isStale)
        is ScreenState.Loading -> ScreenState.Loading
        is ScreenState.Empty -> ScreenState.Empty
        is ScreenState.NoNetwork -> ScreenState.NoNetwork
        is ScreenState.Error -> this
    }

/** Runs [block] only when content is present; returns the state unchanged for chaining. */
inline fun <T> ScreenState<T>.onContent(block: (T) -> Unit): ScreenState<T> {
    if (this is ScreenState.Content) block(data)
    return this
}

/** Content payload or [default] for every other branch. */
fun <T> ScreenState<T>.contentOrElse(default: T): T = dataOrNull ?: default

/** Convenience constructors. */
fun <T> T.asContent(isStale: Boolean = false): ScreenState<T> = ScreenState.Content(this, isStale)

fun errorState(message: String): ScreenState<Nothing> = ScreenState.Error(UiText.Dynamic(message))
