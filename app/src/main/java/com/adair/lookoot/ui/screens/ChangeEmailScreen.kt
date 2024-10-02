package com.adair.lookoot.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adair.lookoot.viewmodels.UserProfileViewModel

// Displays a screen for users to change their email address.
// Collects the current password and new email, validates the input, and triggers email change via the ViewModel.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeEmailScreen(
    viewModel: UserProfileViewModel,
    onBackClick: () -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newEmail by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Change Email") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") // Back button to navigate back to the previous screen

                    }
                }
            )
        }
    ) { innerPadding ->
        // Layout for input fields and buttons
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Input field for current password
            OutlinedTextField(
                value = currentPassword,
                onValueChange = { currentPassword = it },
                label = { Text("Current Password") },
                singleLine = true,
                isError = errorMessage != null,
                modifier = Modifier.fillMaxWidth()
            )
            // Input field for new email
            OutlinedTextField(
                value = newEmail,
                onValueChange = { newEmail = it },
                label = { Text("New Email") },
                singleLine = true,
                isError = errorMessage != null,
                modifier = Modifier.fillMaxWidth()
            )
            // Button to trigger email change
            Button(
                onClick = {
                    isLoading = true
                    errorMessage = null
                    successMessage = null
                    // Calls the ViewModel's changeEmail function with input values
                    viewModel.changeEmail(currentPassword, newEmail) { success, message ->
                        isLoading = false
                        if (success) {
                            successMessage = "Email changed successfully"
                            currentPassword = ""
                            newEmail = ""
                        } else {
                            errorMessage = message
                        }
                    }
                },
                enabled = !isLoading && currentPassword.isNotBlank() && newEmail.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Change Email")
            }
            // Shows a loading indicator if the operation is in progress
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            // Displays an error message if one exists
            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            // Displays a success message if the email was changed successfully
            successMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}