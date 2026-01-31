package com.passwordlessauth.app.viewmodel

import com.passwordlessauth.app.data.OtpData

/**
 * Sealed class representing the authentication UI state.
 * Uses sealed classes for type-safe state management and exhaustive when expressions.
 */
sealed class AuthState {
    /**
     * Initial state - showing email input screen
     */
    data object Initial : AuthState()
    
    /**
     * OTP sent state - showing OTP input screen
     * @param email The email address OTP was sent to
     */
    data class OtpSent(val email: String) : AuthState()
    
    /**
     * Authenticated state - showing session screen
     * @param email The authenticated user's email
     * @param sessionStartTime Timestamp when session started
     */
    data class Authenticated(
        val email: String,
        val sessionStartTime: Long
    ) : AuthState()
}

/**
 * UI state for email input screen
 */
data class EmailInputState(
    val email: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val otpSent: Boolean = false
)

/**
 * UI state for OTP input screen
 */
data class OtpInputState(
    val otp: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val attemptsRemaining: Int = OtpData.MAX_ATTEMPTS,
    val canResend: Boolean = true,
    val countdownSeconds: Int = 0
)

/**
 * UI state for session screen
 */
data class SessionState(
    val email: String,
    val sessionStartTime: Long,
    val currentDuration: String = "00:00"
)

