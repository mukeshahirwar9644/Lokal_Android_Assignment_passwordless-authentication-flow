package com.passwordlessauth.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.passwordlessauth.app.viewmodel.AuthViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

/**
 * Session screen showing login duration and logout option.
 * 
 * Compose Concepts Used:
 * - @Composable: Function-based UI
 * - LaunchedEffect: For timer that updates every second
 * - remember: For timer state that survives recomposition
 * - rememberSaveable: Not needed as ViewModel handles state persistence
 * - Recomposition: Timer triggers recomposition every second
 */
@Composable
fun SessionScreen(
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val sessionState by viewModel.sessionState.collectAsState()
    
    if (sessionState == null) {
        return
    }
    
    // Timer state that survives recomposition
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    
    // LaunchedEffect to update timer every second
    LaunchedEffect(sessionState.sessionStartTime) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(1000)
        }
    }
    
    // Calculate duration
    val durationMillis = currentTime - sessionState.sessionStartTime
    val durationSeconds = (durationMillis / 1000).toInt()
    val minutes = durationSeconds / 60
    val seconds = durationSeconds % 60
    val durationString = String.format("%02d:%02d", minutes, seconds)
    
    // Format start time
    val startTimeFormatted = remember(sessionState.sessionStartTime) {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        sdf.format(Date(sessionState.sessionStartTime))
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome!",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = sessionState.email,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 48.dp)
        )
        
        // Session Start Time Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Session Started",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = startTimeFormatted,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
        
        // Session Duration Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Session Duration",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = durationString,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
        
        // Logout Button
        Button(
            onClick = { viewModel.logout() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("Logout")
        }
    }
}

