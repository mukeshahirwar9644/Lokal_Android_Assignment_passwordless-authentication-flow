package com.passwordlessauth.app.data

import kotlin.random.Random

/**
 * Manages OTP generation and validation per email address.
 * Uses a Map to store OTP data per email, allowing multiple users
 * to have active OTPs simultaneously.
 * 
 * Key Design Decisions:
 * - Map<String, OtpData>: Allows storing OTP per email address
 * - Thread-safe operations: All methods should be called from ViewModel
 * - Immutable OtpData: Prevents accidental state mutations
 */
class OtpManager {
    // Map to store OTP data per email address
    // Key: email address, Value: OtpData
    private val otpStore: MutableMap<String, OtpData> = mutableMapOf()
    
    /**
     * Generates a new 6-digit OTP for the given email.
     * If an OTP already exists for this email, it is invalidated and replaced.
     * This resets the attempt count as per requirements.
     */
    fun generateOtp(email: String): String {
        val otp = generateRandomOtp()
        val otpData = OtpData(
            otp = otp,
            generatedAt = System.currentTimeMillis(),
            attemptsRemaining = OtpData.MAX_ATTEMPTS
        )
        otpStore[email.lowercase()] = otpData
        return otp
    }
    
    /**
     * Validates the provided OTP for the given email.
     * Returns ValidationResult indicating success, failure reason, or expiry.
     */
    fun validateOtp(email: String, inputOtp: String): OtpValidationResult {
        val emailKey = email.lowercase()
        val otpData = otpStore[emailKey] ?: return OtpValidationResult.NOT_FOUND
        
        // Check if OTP has expired
        if (otpData.isExpired()) {
            otpStore.remove(emailKey)
            return OtpValidationResult.EXPIRED
        }
        
        // Check if attempts are exhausted
        if (!otpData.hasAttemptsRemaining()) {
            otpStore.remove(emailKey)
            return OtpValidationResult.MAX_ATTEMPTS_EXCEEDED
        }
        
        // Validate OTP
        if (otpData.otp == inputOtp) {
            otpStore.remove(emailKey) // Remove OTP after successful validation
            return OtpValidationResult.SUCCESS
        } else {
            // Decrement attempts and update store
            val updatedOtpData = otpData.copy(
                attemptsRemaining = otpData.attemptsRemaining - 1
            )
            otpStore[emailKey] = updatedOtpData
            return OtpValidationResult.INVALID(updatedOtpData.attemptsRemaining)
        }
    }
    
    /**
     * Gets the remaining attempts for an email's OTP
     */
    fun getRemainingAttempts(email: String): Int {
        return otpStore[email.lowercase()]?.attemptsRemaining ?: 0
    }
    
    /**
     * Checks if an OTP exists and is valid (not expired) for the given email
     */
    fun hasValidOtp(email: String): Boolean {
        val otpData = otpStore[email.lowercase()] ?: return false
        return !otpData.isExpired()
    }
    
    /**
     * Generates a random 6-digit OTP
     */
    private fun generateRandomOtp(): String {
        return (100000..999999).random().toString()
    }
}

/**
 * Sealed class representing OTP validation results
 */
sealed class OtpValidationResult {
    object SUCCESS : OtpValidationResult()
    object EXPIRED : OtpValidationResult()
    object NOT_FOUND : OtpValidationResult()
    object MAX_ATTEMPTS_EXCEEDED : OtpValidationResult()
    data class INVALID(val attemptsRemaining: Int) : OtpValidationResult()
}

