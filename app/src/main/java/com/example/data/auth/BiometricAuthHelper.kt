package com.example.data.auth

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

enum class BiometricAvailability {
    AVAILABLE,
    NOT_ENROLLED,
    HARDWARE_UNAVAILABLE,
    UNSUPPORTED,
    ;

    val isReady: Boolean
        get() = this == AVAILABLE
}

object BiometricAuthHelper {

    fun allowedAuthenticators(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        }
    }

    fun checkAvailability(context: Context): BiometricAvailability {
        val manager = BiometricManager.from(context)
        return when (manager.canAuthenticate(allowedAuthenticators())) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NOT_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricAvailability.HARDWARE_UNAVAILABLE
            else -> BiometricAvailability.UNSUPPORTED
        }
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        negativeButtonText: String,
        onPromptVisibilityChanged: (Boolean) -> Unit = {},
        onSuccess: () -> Unit,
        onError: (errorCode: Int, message: String) -> Unit,
        onFailedAttempt: () -> Unit = {},
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onPromptVisibilityChanged(false)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onPromptVisibilityChanged(false)
                onError(errorCode, errString.toString())
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onFailedAttempt()
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)
        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setConfirmationRequired(false)
            .setAllowedAuthenticators(allowedAuthenticators())

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            promptInfoBuilder.setNegativeButtonText(negativeButtonText)
        }

        try {
            onPromptVisibilityChanged(true)
            prompt.authenticate(promptInfoBuilder.build())
        } catch (e: Exception) {
            onPromptVisibilityChanged(false)
            onError(
                BiometricPrompt.ERROR_UNABLE_TO_PROCESS,
                e.localizedMessage ?: "Gagal membuka dialog biometrik."
            )
        }
    }

    fun createEnrollIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, allowedAuthenticators())
            }
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }
    }
}

tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}
