package com.passwordlessauth.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.passwordlessauth.app.analytics.AnalyticsLogger
import com.passwordlessauth.app.data.OtpData
import com.passwordlessauth.app.data.OtpManager
import com.passwordlessauth.app.data.OtpValidationResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.regex.Pattern

/**
 * ViewModel managing authentication flow state.
 * 
 * Architecture Principles:
 * - One-way data flow: UI observes StateFlow, ViewModel updates state
 * - Business logic separation: All OTP logic in OtpManager, ViewModel orchestrates
 * - Coroutines for async operations
 * - Immutable state updates using copy/update
 */
class AuthViewModel : ViewModel() {
    
    private val otpManager = OtpManager()
    
    // Main authentication state
    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    // Email input screen state
    private val _emailInputState = MutableStateFlow(EmailInputState())
    val emailInputState: StateFlow<EmailInputState> = _emailInputState.asStateFlow()
    
    // OTP input screen state
    private val _otpInputState = MutableStateFlow(OtpInputState())
    val otpInputState: StateFlow<OtpInputState> = _otpInputState.asStateFlow()
    
    // Session screen state
    private val _sessionState = MutableStateFlow<SessionState?>(null)
    val sessionState: StateFlow<SessionState?> = _sessionState.asStateFlow()
    
    // Countdown job for OTP expiry
    private var countdownJob: Job? = null
    
    /**
     * Updates email input field
     */
    fun updateEmail(email: String) {
        _emailInputState.update { it.copy(email = email, errorMessage = null) }
    }
    
    /**
     * Validates email and generates OTP
     */
    fun sendOtp() {
        val email = _emailInputState.value.email.trim()
        
        if (!isValidEmail(email)) {
            _emailInputState.update { 
                it.copy(errorMessage = "Please enter a valid email") 
            }
            return
        }
        
        viewModelScope.launch {
            _emailInputState.update { it.copy(isLoading = true, errorMessage = null) }
            
            // Simulate network delay (in real app, this would be API call)
            delay(500)
            
            // Generate OTP
            val otp = otpManager.generateOtp(email)
            
            // Log analytics event
            AnalyticsLogger.logOtpGenerated(email)
            
            // Update state
            _emailInputState.update { 
                it.copy(
                    isLoading = false,
                    otpSent = true
                ) 
            }
            
            _authState.value = AuthState.OtpSent(email)
            _otpInputState.value = OtpInputState(
                attemptsRemaining = OtpData.MAX_ATTEMPTS,
                canResend = true,
                countdownSeconds = OtpData.OTP_EXPIRY_SECONDS.toInt()
            )
            
            // Start countdown timer
            startOtpCountdown(email)
        }
    }
    
    /**
     * Updates OTP input field
     */
    fun updateOtp(otp: String) {
        // Only allow numeric input and limit to 6 digits
        val filteredOtp = otp.filter { it.isDigit() }.take(6)
        _otpInputState.update { 
            it.copy(otp = filteredOtp, errorMessage = null) 
        }
    }
    
    /**
     * Validates the entered OTP
     */
    fun verifyOtp() {
        val currentAuthState = _authState.value
        if (currentAuthState !is AuthState.OtpSent) return
        
        val email = currentAuthState.email
        val inputOtp = _otpInputState.value.otp
        
        if (inputOtp.length != 6) {
            _otpInputState.update { 
                it.copy(errorMessage = "Please enter 6-digit OTP") 
            }
            return
        }
        
        viewModelScope.launch {
            _otpInputState.update { it.copy(isLoading = true, errorMessage = null) }
            
            // Simulate validation delay
            delay(300)
            
            val result = otpManager.validateOtp(email, inputOtp)
            
            when (result) {
                is OtpValidationResult.SUCCESS -> {
                    // Log success
                    AnalyticsLogger.logOtpValidationSuccess(email)
                    
                    // Clear OTP input state
                    _otpInputState.update { 
                        it.copy(
                            isLoading = false,
                            otp = "",
                            errorMessage = null
                        ) 
                    }
                    
                    // Stop countdown
                    countdownJob?.cancel()
                    
                    // Start session
                    val sessionStartTime = System.currentTimeMillis()
                    _authState.value = AuthState.Authenticated(email, sessionStartTime)
                    _sessionState.value = SessionState(
                        email = email,
                        sessionStartTime = sessionStartTime
                    )
                }
                
                is OtpValidationResult.EXPIRED -> {
                    AnalyticsLogger.logOtpValidationFailure(email, "expired")
                    _otpInputState.update { 
                        it.copy(
                            isLoading = false,
                            errorMessage = "OTP has expired. Please request a new one.",
                            canResend = true
                        ) 
                    }
                    countdownJob?.cancel()
                }
                
                is OtpValidationResult.MAX_ATTEMPTS_EXCEEDED -> {
                    AnalyticsLogger.logOtpValidationFailure(email, "max_attempts_exceeded")
                    _otpInputState.update { 
                        it.copy(
                            isLoading = false,
                            errorMessage = "Maximum attempts exceeded. Please request a new OTP.",
                            canResend = true
                        ) 
                    }
                    countdownJob?.cancel()
                }
                
                is OtpValidationResult.INVALID -> {
                    AnalyticsLogger.logOtpValidationFailure(email, "invalid")
                    _otpInputState.update { 
                        it.copy(
                            isLoading = false,
                            errorMessage = "Invalid OTP. Attempts remaining: ${result.attemptsRemaining}",
                            attemptsRemaining = result.attemptsRemaining
                        ) 
                    }
                }
                
                is OtpValidationResult.NOT_FOUND -> {
                    AnalyticsLogger.logOtpValidationFailure(email, "not_found")
                    _otpInputState.update { 
                        it.copy(
                            isLoading = false,
                            errorMessage = "OTP not found. Please request a new one.",
                            canResend = true
                        ) 
                    }
                }
            }
        }
    }
    
    /**
     * Resends OTP (invalidates old OTP and generates new one)
     */
    fun resendOtp() {
        val currentAuthState = _authState.value
        if (currentAuthState !is AuthState.OtpSent) return
        
        val email = currentAuthState.email
        
        // Cancel existing countdown
        countdownJob?.cancel()
        
        // Generate new OTP (this invalidates the old one)
        viewModelScope.launch {
            _otpInputState.update { 
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    otp = "",
                    canResend = false
                ) 
            }
            
            delay(500)
            
            val otp = otpManager.generateOtp(email)
            AnalyticsLogger.logOtpGenerated(email)
            
            _otpInputState.update { 
                it.copy(
                    isLoading = false,
                    attemptsRemaining = OtpData.MAX_ATTEMPTS,
                    canResend = true,
                    countdownSeconds = OtpData.OTP_EXPIRY_SECONDS.toInt()
                ) 
            }
            
            startOtpCountdown(email)
        }
    }
    
    /**
     * Logs out the user and resets to initial state
     */
    fun logout() {
        val sessionState = _sessionState.value ?: return
        val email = sessionState.email
        val sessionDuration = (System.currentTimeMillis() - sessionState.sessionStartTime) / 1000
        
        // Log analytics
        AnalyticsLogger.logLogout(email, sessionDuration)
        
        // Reset all state
        _authState.value = AuthState.Initial
        _emailInputState.value = EmailInputState()
        _otpInputState.value = OtpInputState()
        _sessionState.value = null
        countdownJob?.cancel()
    }
    
    /**
     * Starts countdown timer for OTP expiry
     */
    private fun startOtpCountdown(email: String) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            var remainingSeconds = OtpData.OTP_EXPIRY_SECONDS.toInt()
            
            while (remainingSeconds > 0) {
                _otpInputState.update { it.copy(countdownSeconds = remainingSeconds) }
                delay(1000)
                remainingSeconds--
                
                // Check if OTP is still valid (user might have resend)
                if (!otpManager.hasValidOtp(email)) {
                    break
                }
            }
            
            // OTP expired
            if (remainingSeconds == 0) {
                _otpInputState.update { 
                    it.copy(
                        countdownSeconds = 0,
                        canResend = true,
                        errorMessage = "OTP has expired. Please request a new one."
                    ) 
                }
            }
        }
    }
    
    /**
     * Validates email format using regex
     */
    private fun isValidEmail(email: String): Boolean {
        val emailPattern = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
        )
        return emailPattern.matcher(email).matches()
    }
    
    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}

