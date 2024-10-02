package com.adair.lookoot.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adair.lookoot.viewmodels.UserProfileViewModel
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.launch

data class SecurityState(
    val phoneNumber: String = "",
    val verificationCode: String = "",
    val verificationId: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isVerificationSent: Boolean = false,
    val isEnrollmentComplete: Boolean = false,
    val isEmailVerified: Boolean = false,
    val userEmail: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordSecurityScreen(viewModel: UserProfileViewModel, onBackClick: () -> Unit) {
    // Manages the state of the security settings screen, including email verification and multi-factor authentication (MFA).
    var state by remember { mutableStateOf(SecurityState()) }
    val scope = rememberCoroutineScope()

    // Function to refresh the email verification status from Firebase.
    fun refreshEmailVerificationStatus() {
        scope.launch {
            state = state.copy(isLoading = true)
            viewModel.refreshUser()  // Refresh user data.
            state = state.copy(
                isLoading = false,
                isEmailVerified = viewModel.isEmailVerified(),// Check if email is verified.
                userEmail = viewModel.getUserEmail() ?: "" // Get the user's email.
            )
        }
    }

    // Trigger email verification status refresh when the screen is first displayed.
    LaunchedEffect(Unit) {
        refreshEmailVerificationStatus()
    }

    // Main layout of the screen.
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Password and Security") },
                navigationIcon = {
                    // Back button to navigate to the previous screen.
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // Email Verification Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Email Verification",
                            style = MaterialTheme.typography.titleMedium
                        )
                        // Refresh email verification status button.
                        IconButton(onClick = { refreshEmailVerificationStatus() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Current email: ${state.userEmail}")
                    Spacer(modifier = Modifier.height(8.dp))

                    // Check if email is verified or prompt the user to verify it.
                    if (state.isEmailVerified) {
                        Text(
                            "Email is verified",
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {

                        // Button to send email verification.
                        Button(
                            onClick = {
                                state = state.copy(isLoading = true)
                                viewModel.sendEmailVerification { success, message ->
                                    state = state.copy(
                                        isLoading = false,
                                        errorMessage = if (success) null else message
                                    )
                                }
                            },
                            enabled = !state.isLoading // Button disabled while loading.
                        ) {
                            Text("Send Verification Email")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section for multi-factor authentication (MFA) setup.
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Multi-Factor Authentication",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Phone number input field for MFA setup.
                    OutlinedTextField(
                        value = state.phoneNumber,
                        onValueChange = { state = state.copy(phoneNumber = it) },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isLoading && !state.isVerificationSent
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Button to enroll in SMS MFA if verification is not sent yet.
                    if (!state.isVerificationSent) {
                        Button(
                            onClick = {
                                state = state.copy(isLoading = true)
                                viewModel.setupMFA(state.phoneNumber) { success, message ->
                                    state = if (success) {
                                        state.copy(
                                            isLoading = false,
                                            isVerificationSent = true,
                                            verificationId = viewModel.verificationId
                                        )
                                    } else {
                                        state.copy(
                                            isLoading = false,
                                            errorMessage = message ?: "Failed to setup MFA"
                                        )
                                    }
                                }
                            },
                            enabled = !state.isLoading && state.phoneNumber.isNotBlank() && state.isEmailVerified
                        ) {
                            Text("Enroll in SMS MFA")
                        }
                    }

                    // If verification is sent but not completed, show the verification code input.
                    if (state.isVerificationSent && !state.isEnrollmentComplete) {
                        OutlinedTextField(
                            value = state.verificationCode,
                            onValueChange = { state = state.copy(verificationCode = it) },
                            label = { Text("Verification Code") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.isLoading
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Button to verify the code and complete MFA enrollment.
                        Button(
                            onClick = {
                                state = state.copy(isLoading = true)
                                val credential = PhoneAuthProvider.getCredential(
                                    state.verificationId!!,
                                    state.verificationCode
                                )
                                viewModel.completeMFAEnrollment(credential) { success, message ->
                                    state = if (success) {
                                        state.copy(
                                            isLoading = false,
                                            isEnrollmentComplete = true,
                                            errorMessage = null
                                        )
                                    } else {
                                        state.copy(
                                            isLoading = false,
                                            errorMessage = message ?: "Failed to complete MFA enrollment"
                                        )
                                    }
                                }
                            },
                            enabled = !state.isLoading && state.verificationCode.isNotBlank()
                        ) {
                            Text("Verify Code and Complete Enrollment")
                        }
                    }
                }
            }

            // Loading indicator when actions are in progress.
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            }

            // Display error messages if any.
            state.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            // Display success message if MFA enrollment is completed.
            if (state.isEnrollmentComplete) {
                Text(
                    "MFA enrollment completed successfully",
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Inform the user to verify their email before enrolling in MFA.
            if (!state.isEmailVerified) {
                Text(
                    "Please verify your email before enrolling in MFA",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}