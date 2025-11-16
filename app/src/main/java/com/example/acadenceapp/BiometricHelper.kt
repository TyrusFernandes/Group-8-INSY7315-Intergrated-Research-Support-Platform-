package com.example.acadenceapp

import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Small helper around BiometricPrompt.
 * Call authenticateOrContinue(), and when auth succeeds (or is not available)
 * it will run the onAuthenticated() lambda you pass in.
 */
class BiometricHelper(
    private val activity: FragmentActivity,
    private val onAuthenticated: () -> Unit
) {

    private val executor = ContextCompat.getMainExecutor(activity)

    private val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Unlock Acadence")
        .setSubtitle("Use your fingerprint or device credentials")
        // Allow strong biometrics OR device PIN/pattern/password
        .setAllowedAuthenticators(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        .build()

    private val biometricPrompt = BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {

            override fun onAuthenticationSucceeded(
                result: BiometricPrompt.AuthenticationResult
            ) {
                super.onAuthenticationSucceeded(result)
                onAuthenticated()
            }

            override fun onAuthenticationError(
                errorCode: Int,
                errString: CharSequence
            ) {
                super.onAuthenticationError(errorCode, errString)
                Toast.makeText(activity,
                    "Authentication error: $errString", Toast.LENGTH_SHORT).show()
                // Block access if they cancel or error
                activity.finish()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Toast.makeText(activity,
                    "Fingerprint not recognised", Toast.LENGTH_SHORT).show()
            }
        }
    )

    /** Returns true if biometrics / device credential can be used on this device. */
    private fun canUseBiometrics(): Boolean {
        val manager = BiometricManager.from(activity)
        return when (manager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Toast.makeText(
                    activity,
                    "No biometrics enrolled on this device",
                    Toast.LENGTH_LONG
                ).show()
                false
            }
            else -> false
        }
    }

    /** If biometrics available → show prompt; otherwise just continue normally. */
    fun authenticateOrContinue() {
        if (canUseBiometrics()) {
            biometricPrompt.authenticate(promptInfo)
        } else {
            // Device can’t use biometrics → just continue into dashboard
            onAuthenticated()
        }
    }
}
