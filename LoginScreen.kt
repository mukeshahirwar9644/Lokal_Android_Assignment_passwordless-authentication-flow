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
 * Login screen for email input and OTP generation.
 * 
 * Compose Concepts Used:
 * - @Composable: Function-based UI
 * - State hoisting: Email state managed in ViewModel
 * - remember: Not needed here as state is in ViewModel
 * - Recomposition: UI automatically updates when StateFlow changes
 */
@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val emailState by viewModel.emailInputState.collectAsState()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Passwordless Authentication",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "Enter your email to receive OTP",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        OutlinedTextField(
            value = emailState.email,
            onValueChange = { viewModel.updateEmail(it) },
            label = { Text("Email") },
            placeholder = { Text("example@email.com") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            enabled = !emailState.isLoading,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email
            )
        )
        
        // Error message
        emailState.errorMessage?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
        }
        
        // Success message
        if (emailState.otpSent) {
            Text(
                text = "OTP sent to your email",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
        }
        
        Button(
            onClick = { viewModel.sendOtp() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !emailState.isLoading && emailState.email.isNotBlank()
        ) {
            if (emailState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Send OTP")
            }
        }
    }
}

