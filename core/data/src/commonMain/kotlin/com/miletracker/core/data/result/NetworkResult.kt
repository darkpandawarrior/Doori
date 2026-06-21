package com.miletracker.core.data.result

// Typed result for any async operation, avoids raw try/catch propagating to UI.
// D = data type, E = domain error type (default NetworkError for network ops).
sealed class NetworkResult<out D, out E> {
    data class Success<D>(val data: D) : NetworkResult<D, Nothing>()

    data class Failure<E>(val error: E) : NetworkResult<Nothing, E>()

    data object Loading : NetworkResult<Nothing, Nothing>()
}

// Common network-level errors; avoids passing raw Throwable through layers.
enum class NetworkError {
    /** No connectivity (airplane mode, wifi off). */
    NoNetwork,

    /** HTTP 4xx (bad request, unauthorized, not found, etc.). */
    ClientError,

    /** HTTP 5xx. */
    ServerError,

    /** Response arrived but was not parseable. */
    ParseError,

    /** Request timed out. */
    Timeout,

    /** Catch-all for unexpected exceptions. */
    Unknown,
}

fun NetworkError.toUiText(): String =
    when (this) {
        NetworkError.NoNetwork -> "No internet connection. Check your network and try again."
        NetworkError.ClientError -> "Something went wrong with your request."
        NetworkError.ServerError -> "Server error. Please try again later."
        NetworkError.ParseError -> "Received unexpected data from server."
        NetworkError.Timeout -> "Request timed out. Check your connection and retry."
        NetworkError.Unknown -> "An unexpected error occurred."
    }

fun NetworkError.toThrowable(): Throwable = IllegalStateException(toUiText())

// Convenience aliases for the common case where E = NetworkError.
typealias DataResult<D> = NetworkResult<D, NetworkError>

fun <D> D.asSuccess(): DataResult<D> = NetworkResult.Success(this)

fun NetworkError.asFailure(): DataResult<Nothing> = NetworkResult.Failure(this)
