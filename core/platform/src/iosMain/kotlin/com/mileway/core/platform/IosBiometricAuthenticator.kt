package com.mileway.core.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSError
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthenticationWithBiometrics
import kotlin.coroutines.resume

/**
 * iOS biometric auth via LocalAuthentication, the Face/Touch ID counterpart to Android's BiometricPrompt
 * (F). Checks policy availability, then bridges LAContext.evaluatePolicy's completion handler into a
 * cancellable suspend call. Compiles + links against the simulator framework; runtime needs a real device.
 */
class IosBiometricAuthenticator : BiometricAuthenticator {
    @OptIn(ExperimentalForeignApi::class)
    override fun isAvailable(): Boolean = LAContext().canEvaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics, null)

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun authenticate(reason: String): BiometricResult {
        val context = LAContext()
        if (!context.canEvaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics, null)) {
            return BiometricResult.Unavailable
        }
        return suspendCancellableCoroutine { continuation ->
            context.evaluatePolicy(
                LAPolicyDeviceOwnerAuthenticationWithBiometrics,
                localizedReason = reason,
            ) { success: Boolean, error: NSError? ->
                val result =
                    when {
                        success -> BiometricResult.Success
                        error != null -> BiometricResult.Error(error.localizedDescription)
                        else -> BiometricResult.Failed
                    }
                continuation.resume(result)
            }
        }
    }
}
