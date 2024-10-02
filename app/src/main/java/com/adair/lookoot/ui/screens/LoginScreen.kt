// File: LoginScreen.kt
package com.adair.lookoot.ui.screens

import android.util.Log  // Import for logging
import androidx.compose.foundation.Image  // Import for displaying images
import androidx.compose.foundation.layout.*  // Import layout components for spacing and alignment
import androidx.compose.material3.*  // Import Material Design 3 components for UI elements
import androidx.compose.runtime.*  // Import Compose runtime for state management
import androidx.compose.ui.Alignment  // Import for UI alignment options
import androidx.compose.ui.Modifier  // Import Modifier for UI customization
import androidx.compose.ui.res.painterResource  // Import for loading resources like images
import androidx.compose.ui.text.input.PasswordVisualTransformation  // Import for password input transformation
import androidx.compose.ui.unit.dp  // Import unit for defining dimensions in UI
import androidx.lifecycle.viewmodel.compose.viewModel  // Import for using ViewModel in Compose
import com.adair.lookoot.viewmodels.LoginViewModel  // Import custom ViewModel for login logic
import com.adair.lookoot.R  // Import resource identifiers
import com.adair.lookoot.viewmodels.LookootViewModel  // Import ViewModel for handling user-related data

/**
 * Composable function that provides the UI for the login screen.
 *
 * @param onLoginSuccess Callback function that is triggered when login is successful.
 * @param onNavigateToRegister Callback function that is triggered to navigate to the registration screen.
 * @param onNavigateToPasswordReset Callback function that is triggered to navigate to the password reset screen.
 * @param loginViewModel The ViewModel that handles the login logic.
 * @param lookootViewModel The ViewModel that handles user-related data for the app.
 */
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToPasswordReset: () -> Unit,
    loginViewModel: LoginViewModel = viewModel(),
    lookootViewModel: LookootViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }  // State to hold the email input
    var password by remember { mutableStateOf("") }  // State to hold the password input
    var errorMessage by remember { mutableStateOf<String?>(null) }  // State to hold any error messages

    // Log the initialization of the LoginScreen
    Log.d("LoginScreen", "Login screen initialized")

    Column(
        modifier = Modifier
            .fillMaxSize()  // Make the column fill the available screen space
            .padding(16.dp),  // Add padding around the column for spacing
        horizontalAlignment = Alignment.CenterHorizontally,  // Center content horizontally
        verticalArrangement = Arrangement.Center  // Center content vertically
    ) {
        // Display a login image
        Image(
            painter = painterResource(id = R.drawable.login_image),
            contentDescription = "Login image",  // Describe the image for accessibility
            modifier = Modifier
                .size(200.dp)  // Set the size of the image
                .padding(bottom = 32.dp)  // Add bottom padding to separate it from the text fields
        )

        // Display the login title
        Text(
            text = "Login",
            style = MaterialTheme.typography.headlineMedium  // Apply headline style from the theme
        )
        Spacer(modifier = Modifier.height(16.dp))  // Add vertical space between elements

        // Input field for the email address
        TextField(
            value = email,
            onValueChange = {
                email = it
                Log.d("LoginScreen", "Email input changed: $email")  // Log email input changes
            },
            label = { Text("Email") },  // Label for the text field
            modifier = Modifier.fillMaxWidth()  // Make the text field fill the available width
        )
        Spacer(modifier = Modifier.height(8.dp))  // Add vertical space between elements

        // Input field for the password
        TextField(
            value = password,
            onValueChange = {
                password = it
                Log.d("LoginScreen", "Password input changed")  // Log password input changes
            },
            label = { Text("Password") },  // Label for the text field
            visualTransformation = PasswordVisualTransformation(),  // Mask the password input
            modifier = Modifier.fillMaxWidth()  // Make the text field fill the available width
        )
        Spacer(modifier = Modifier.height(16.dp))  // Add vertical space between elements

        // Button for submitting the login form
        Button(
            onClick = {
                Log.d("LoginScreen", "Login button clicked with email: $email")
                try {
                    loginViewModel.login(email, password) { success, error, user ->
                        if (success && user != null) {
                            Log.d("LoginScreen", "Login successful. User ID: ${user.id}")
                            lookootViewModel.setCurrentUser(user)
                            onLoginSuccess()
                        } else {
                            Log.e("LoginScreen", "Login failed: $error")
                            errorMessage = error ?: "Login failed. Please try again."
                        }
                    }
                } catch (e: Exception) {
                    Log.e("LoginScreen", "Exception during login: ${e.message}", e)
                    errorMessage = "An unexpected error occurred. Please try again."
                }
            },
            modifier = Modifier.fillMaxWidth()  // Make the button fill the available width
        ) {
            Text("Login")  // Display text on the button
        }

        // Button to navigate to the password reset screen
        TextButton(
            onClick = {
                Log.d("LoginScreen", "Navigating to password reset screen")
                onNavigateToPasswordReset()
            },
            modifier = Modifier.padding(top = 8.dp)  // Add padding above the button
        ) {
            Text("Forgot Password?")  // Display text on the button
        }

        // Display an error message if there is one
        errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,  // Apply error color from the theme
                modifier = Modifier.padding(top = 8.dp)  // Add top padding for spacing
            )
        }

        // Button to navigate to the registration screen
        TextButton(
            onClick = {
                Log.d("LoginScreen", "Navigating to registration screen")
                onNavigateToRegister()
            },
            modifier = Modifier.padding(top = 8.dp)  // Add padding above the button
        ) {
            Text("Don't have an account? Sign Up")  // Display text on the button
        }
    }
}
