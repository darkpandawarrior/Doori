package com.miletracker.core.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Android biometric prompt helper. Android-only (BiometricPrompt requires a FragmentActivity).
 * The cross-platform [com.miletracker.core.platform.BiometricAuthenticator] abstraction is wired in
 * Phase 3.4 (feature:profile); this remains the Android implementation detail.
 */
object BiometricGuard {
    enum class Availability { Available, NoHardware, NoneEnrolled, Unavailable }

    fun checkAvailability(context: Context): Availability {
        val manager = BiometricManager.from(context)
        return when (manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> Availability.Available
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> Availability.NoHardware
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> Availability.NoneEnrolled
            else -> Availability.Unavailable
        }
    }

    fun showPrompt(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit,
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt =
            BiometricPrompt(
                activity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        onSuccess()
                    }

                    override fun onAuthenticationError(
                        errorCode: Int,
                        errString: CharSequence,
                    ) {
                        onFailure(errString.toString())
                    }

                    override fun onAuthenticationFailed() {
                        onFailure("Authentication failed — try again.")
                    }
                },
            )
        val promptInfo =
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButtonText("Cancel")
                .build()
        prompt.authenticate(promptInfo)
    }
}
