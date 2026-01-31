package com.passwordlessauth.app.analytics

import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase

/**
 * Analytics logger using Firebase Analytics.
 * 
 * Why Firebase Analytics:
 * - Industry standard for mobile analytics
 * - Easy integration with Android
 * - Provides event tracking, user properties, and detailed insights
 * - Free tier is sufficient for this assignment
 * 
 * This class wraps Firebase Analytics to provide a clean interface
 * for logging authentication events.
 */
object AnalyticsLogger {
    private val firebaseAnalytics: FirebaseAnalytics by lazy {
        Firebase.analytics
    }
    
    /**
     * Log when an OTP is generated
     */
    fun logOtpGenerated(email: String) {
        firebaseAnalytics.logEvent("otp_generated") {
            param("email", email)
            param("timestamp", System.currentTimeMillis())
        }
        Log.d("Analytics", "OTP generated for: $email")
    }
    
    /**
     * Log successful OTP validation
     */
    fun logOtpValidationSuccess(email: String) {
        firebaseAnalytics.logEvent("otp_validation_success") {
            param("email", email)
            param("timestamp", System.currentTimeMillis())
        }
        Log.d("Analytics", "OTP validation success for: $email")
    }
    
    /**
     * Log failed OTP validation
     */
    fun logOtpValidationFailure(email: String, reason: String) {
        firebaseAnalytics.logEvent("otp_validation_failure") {
            param("email", email)
            param("reason", reason)
            param("timestamp", System.currentTimeMillis())
        }
        Log.d("Analytics", "OTP validation failure for: $email, reason: $reason")
    }
    
    /**
     * Log user logout
     */
    fun logLogout(email: String, sessionDuration: Long) {
        firebaseAnalytics.logEvent("logout") {
            param("email", email)
            param("session_duration_seconds", sessionDuration)
            param("timestamp", System.currentTimeMillis())
        }
        Log.d("Analytics", "Logout for: $email, session duration: ${sessionDuration}s")
    }
}

