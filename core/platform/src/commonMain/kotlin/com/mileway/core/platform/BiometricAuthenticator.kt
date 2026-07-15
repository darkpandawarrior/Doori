package com.mileway.core.platform

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
