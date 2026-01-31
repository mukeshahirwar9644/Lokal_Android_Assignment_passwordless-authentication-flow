package com.passwordlessauth.app.data

/**
 * Data class to store OTP information for each email
 * @param otp The 6-digit OTP code
 * @param generatedAt Timestamp when OTP was generated (in milliseconds)
 * @param attemptsRemaining Number of validation attempts remaining
 */
data class OtpData(
    val otp: String,
    val generatedAt: Long,
    val attemptsRemaining: Int
) {
    companion object {
        const val OTP_LENGTH = 6
        const val OTP_EXPIRY_SECONDS = 60L
        const val MAX_ATTEMPTS = 3
    }
    
    /**
     * Check if OTP has expired
     */
    fun isExpired(currentTime: Long = System.currentTimeMillis()): Boolean {
        val elapsedSeconds = (currentTime - generatedAt) / 1000
        return elapsedSeconds >= OTP_EXPIRY_SECONDS
    }
    
    /**
     * Check if OTP has attempts remaining
     */
    fun hasAttemptsRemaining(): Boolean {
        return attemptsRemaining > 0
    }
}

