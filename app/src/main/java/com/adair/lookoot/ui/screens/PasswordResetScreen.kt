// File: PasswordResetScreen.kt
package com.adair.lookoot.ui.screens

import android.util.Log  // Import for logging
import androidx.compose.foundation.layout.*  // Import layout components for spacing and alignment
import androidx.compose.material3.*  // Import Material Design 3 components for UI elements
import androidx.compose.runtime.*  // Import Compose runtime for state management
import androidx.compose.ui.Alignment  // Import for UI alignment options
import androidx.compose.ui.Modifier  // Import Modifier for UI customization
import androidx.compose.ui.graphics.Color  // Import for setting color values
import androidx.compose.ui.unit.dp  // Import unit for defining dimensions in UI
import com.adair.lookoot.viewmodels.UserProfileViewModel

// Displays a password reset screen where the user can input their email to receive a password reset link.
@Composable
fun PasswordResetScreen(
    viewModel: UserProfileViewModel,
    onNavigateToLogin: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Input field for the user to enter their email.
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Button to initiate the password reset process.
        Button(
            onClick = {
                Log.d("PasswordResetScreen", "Reset Password button clicked")
                isLoading = true

                viewModel.sendPasswordResetEmail(email) { success, message ->
                    isLoading = false
                    isSuccess = success
                    if (success) {
                        Log.d("PasswordResetScreen", "Password reset email sent successfully")
                    } else {
                        errorMessage = message ?: "Failed to send password reset email. Please try again."
                        Log.e("PasswordResetScreen", errorMessage!!)
                    }
                }
            },
            enabled = !isLoading  // Button is disabled while the reset request is being processed.
        ) {
        }

        // Shows a loading spinner while the reset request is being processed.
        if (isLoading) {
            CircularProgressIndicator()
        }

        // Displays a success message when the password reset email is sent.
        if (isSuccess) {
            Text("Password reset email sent. Please check your inbox.", color = Color.Green)
        }

        // Displays an error message if there is an issue with the request.
        errorMessage?.let {
            Text(it, color = Color.Red)
        }

        Spacer(modifier = Modifier.height(16.dp))  // Adds space between components.

        // Button to navigate back to the login screen.
        TextButton(onClick = onNavigateToLogin) {
            Text("Back to Login")
        }
    }
}