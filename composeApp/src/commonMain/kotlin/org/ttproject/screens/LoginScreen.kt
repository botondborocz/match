package org.ttproject.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import org.koin.core.logger.Logger
import org.ttproject.viewmodel.LoginState
import org.ttproject.viewmodel.LoginViewModel

@Composable
fun LoginScreen(
    // 🪄 Magic! Koin finds the ViewModel and gives it to us.
    viewModel: LoginViewModel = koinInject()
) {
    // 1. Observe the state from the ViewModel
    val uiState by viewModel.uiState.collectAsState()

    // 2. Local UI State for the text fields
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Welcome to Match App", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(16.dp))

        // Text Fields
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 3. React to the ViewModel's StateFlow!
        when (uiState) {
            is LoginState.Idle -> {
                Button(
                    onClick = { viewModel.login(email, password) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Login")
                }
            }
            is LoginState.Loading -> {
                CircularProgressIndicator()
                Text("Connecting to server...")
            }
            is LoginState.Success -> {
                Text("✅ Logged in successfully! JWT Saved!", color = (Color.Green))
                // Here is where you would navigate to your FeedScreen!
            }
            is LoginState.Error -> {
                val errorMessage = (uiState as LoginState.Error).message
                Text("❌ Error: $errorMessage", color = (Color.Red))

                Button(onClick = { viewModel.login(email, password) }) {
                    Text("Try Again")
                }
            }
        }
    }
}