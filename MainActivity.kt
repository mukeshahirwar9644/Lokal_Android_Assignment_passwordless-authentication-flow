package com.passwordlessauth.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.passwordlessauth.app.ui.LoginScreen
import com.passwordlessauth.app.ui.OtpScreen
import com.passwordlessauth.app.ui.PasswordlessAuthTheme
import com.passwordlessauth.app.ui.SessionScreen
import com.passwordlessauth.app.viewmodel.AuthState
import com.passwordlessauth.app.viewmodel.AuthViewModel

/**
 * Main Activity hosting the authentication flow.
 * Uses ViewModel to manage state across configuration changes.
 */
class MainActivity : ComponentActivity() {
    
    private val viewModel: AuthViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            PasswordlessAuthTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    // Observe auth state and show appropriate screen
                    val authState by viewModel.authState.collectAsState()
                    
                    when (val state = authState) {
                        is AuthState.Initial -> {
                            LoginScreen(viewModel = viewModel)
                        }
                        is AuthState.OtpSent -> {
                            OtpScreen(
                                email = state.email,
                                viewModel = viewModel
                            )
                        }
                        is AuthState.Authenticated -> {
                            SessionScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

