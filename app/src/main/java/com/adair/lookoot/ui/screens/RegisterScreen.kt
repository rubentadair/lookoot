// File: RegisterScreen.kt
package com.adair.lookoot.ui.screens

import android.util.Log  // Import for logging
import androidx.compose.foundation.Image  // Import for displaying images
import androidx.compose.foundation.layout.*  // Import for layout management
import androidx.compose.material.icons.Icons  // Import for using icons
import androidx.compose.material.icons.filled.Check  // Import Check icon
import androidx.compose.material.icons.filled.Error  // Import Error icon
import androidx.compose.material3.*  // Import Material Design 3 components for UI elements
import androidx.compose.runtime.*  // Import Compose runtime for state management
import androidx.compose.ui.Alignment  // Import for alignment options
import androidx.compose.ui.Modifier  // Import Modifier for UI customization
import androidx.compose.ui.graphics.Color  // Import Color class
import androidx.compose.ui.res.painterResource  // Import for loading resources like images
import androidx.compose.ui.text.input.PasswordVisualTransformation  // Import for password input masking
import androidx.compose.ui.unit.dp  // Import unit for defining dimensions in UI
import androidx.lifecycle.viewmodel.compose.viewModel  // Import for using ViewModel in Compose
import com.adair.lookoot.viewmodels.LoginViewModel  // Import LoginViewModel to handle login logic
import com.adair.lookoot.R  // Import for accessing resources
import com.adair.lookoot.viewmodels.UserProfileViewModel  // Import UserProfileViewModel to handle user profile logic
import kotlinx.coroutines.*  // Import for coroutine support

/**
 * Composable function for the registration screen.
 * This screen allows users to create a new account by providing a username, email, and password.
 *
 * @param onRegisterSuccess Callback function triggered when registration is successful.
 * @param onNavigateToLogin Callback function triggered to navigate to the login screen.
 * @param loginViewModel ViewModel for handling registration logic.
 * @param userProfileViewModel ViewModel for managing user profile data, including username availability.
 */
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    loginViewModel: LoginViewModel = viewModel(),
    userProfileViewModel: UserProfileViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }  // State to hold the email input
    var password by remember { mutableStateOf("") }  // State to hold the password input
    var confirmPassword by remember { mutableStateOf("") }  // State to hold the confirm password input
    var username by remember { mutableStateOf("") }  // State to hold the username input
    var errorMessage by remember { mutableStateOf<String?>(null) }  // State to hold error messages
    var isUsernameAvailable by remember { mutableStateOf(true) }  // State to track username availability
    var isCheckingUsername by remember { mutableStateOf(false) }  // State to track if username check is in progress

    val scope = rememberCoroutineScope()  // Coroutine scope for managing coroutines
    var debouncedJob by remember { mutableStateOf<Job?>(null) }  // State to hold the debounced job for username checking

    // Function to check username availability with a debounce mechanism
    val checkUsernameAvailability: (String) -> Unit = { input ->
        debouncedJob?.cancel()  // Cancel the previous job if it exists
        debouncedJob = scope.launch {
            delay(300)  // Debounce delay of 300ms
            isCheckingUsername = true
            Log.d("RegisterScreen", "Checking availability for username: $input")
            userProfileViewModel.isUsernameAvailable(input) { available ->
                Log.d("RegisterScreen", "Username '$input' availability result: $available")
                isUsernameAvailable = available
                isCheckingUsername = false
                if (!available) {
                    errorMessage = "This username is not available. Please try another."
                } else {
                    errorMessage = null
                }
            }
        }
    }

    // UI layout for the registration screen
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Display a login image
        Image(
            painter = painterResource(id = R.drawable.login_image),
            contentDescription = "Login image",
            modifier = Modifier
                .size(200.dp)
                .padding(bottom = 32.dp)
        )

        // Headline text for the registration title
        Text(
            text = "Register",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Username input field with availability check
        OutlinedTextField(
            value = username,
            onValueChange = { newUsername ->
                username = newUsername
                errorMessage = null  // Clear error message when username changes
                if (newUsername.startsWith('@') && newUsername.length > 1) {
                    checkUsernameAvailability(newUsername)  // Check if the username is available
                } else {
                    isUsernameAvailable = true
                    isCheckingUsername = false
                }
            },
            label = { Text("Username") },
            isError = !isUsernameAvailable || !username.startsWith('@'),  // Show error if username is not available or doesn't start with '@'
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                when {
                    isCheckingUsername -> CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    !isUsernameAvailable -> Icon(Icons.Default.Error, "Username not available", tint = MaterialTheme.colorScheme.error)
                    username.startsWith('@') && username.length > 1 -> Icon(Icons.Default.Check, "Username available", tint = Color.Green)
                }
            }
        )
        if (!username.startsWith('@')) {
            Text(
                "Username must start with '@'",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 16.dp)
            )
        } else if (!isUsernameAvailable) {
            Text(
                "Username is already taken",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Email input field
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Password input field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),  // Mask the password input
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Confirm password input field
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            visualTransformation = PasswordVisualTransformation(),  // Mask the confirm password input
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Register button with validation checks
        Button(
            onClick = {
                val trimmedUsername = username.trim()
                Log.d("RegisterScreen", "Register button clicked. Username: $trimmedUsername, IsAvailable: $isUsernameAvailable")
                when {
                    password != confirmPassword -> {
                        errorMessage = "Passwords do not match"
                    }
                    !trimmedUsername.startsWith('@') || trimmedUsername.length <= 1 -> {
                        errorMessage = "Invalid username format"
                    }
                    !isUsernameAvailable -> {
                        errorMessage = "This username is not available. Please try another."
                    }
                    isCheckingUsername -> {
                        errorMessage = "Please wait, checking username availability"
                    }
                    else -> {
                        errorMessage = null
                        // Attempt to sign up the user
                        loginViewModel.signUp(email, password, trimmedUsername) { success, error, user ->
                            if (success && user != null) {
                                Log.d("RegisterScreen", "Registration successful for user: ${user.email}")
                                onRegisterSuccess()  // Navigate to the next screen on successful registration
                            } else {
                                errorMessage = error ?: "Registration failed. Please try again."
                                Log.e("RegisterScreen", "Registration failed: $error")
                            }
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            // Enable the button only if all fields are valid
            enabled = !isCheckingUsername && isUsernameAvailable && username.startsWith('@') && username.length > 1
                    && email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty()
        ) {
            Text("Register")
        }

        // Display error message if any
        errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Button to navigate to the login screen
        TextButton(
            onClick = onNavigateToLogin,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Already have an account? Login")
        }
    }
}
