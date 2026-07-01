package com.mileway.core.data.state

sealed class UiState<out T> {
    object Initial : UiState<Nothing>()

    object Loading : UiState<Nothing>()

    data class Success<T>(val data: T) : UiState<T>()

    data class Error(val message: String) : UiState<Nothing>()
}
