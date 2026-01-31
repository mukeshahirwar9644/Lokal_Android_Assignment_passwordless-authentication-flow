package com.passwordlessauth.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.passwordlessauth.app.viewmodel.AuthViewModel

/**
 * OTP input screen for verification.
 * 
 * Compose Concepts Used:
 * - @Composable: Function-based UI
 * - State hoisting: OTP state managed in ViewModel
 * - rememberSaveable: Not needed as state survives via ViewModel
 * - Recomposition: UI updates when StateFlow changes
 */
@Composable
fun OtpScreen(
    email: String,
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val otpState by viewModel.otpInputState.collectAsState()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Enter OTP",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "We sent a 6-digit code to\n$email",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        // OTP Input Field
        OutlinedTextField(
            value = otpState.otp,
            onValueChange = { viewModel.updateOtp(it) },
            label = { Text("OTP") },
            placeholder = { Text("000000") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            enabled = !otpState.isLoading,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            maxLines = 1
        )
        
        // Countdown timer
        if (otpState.countdownSeconds > 0) {
            Text(
                text = "OTP expires in ${otpState.countdownSeconds}s",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        // Error message
        otpState.errorMessage?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
        }
        
        // Attempts remaining
        if (otpState.attemptsRemaining < 3 && otpState.attemptsRemaining > 0) {
            Text(
                text = "Attempts remaining: ${otpState.attemptsRemaining}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        // Verify Button
        Button(
            onClick = { viewModel.verifyOtp() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(bottom = 16.dp),
            enabled = !otpState.isLoading && otpState.otp.length == 6
        ) {
            if (otpState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Verify OTP")
            }
        }
        
        // Resend Button
        TextButton(
            onClick = { viewModel.resendOtp() },
            enabled = otpState.canResend && !otpState.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Resend OTP")
        }
    }
}

