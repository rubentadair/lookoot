// File: AboutUsScreen.kt
package com.adair.lookoot.ui.screens

import android.util.Log  // Import for logging
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.foundation.layout.*  // Import layout components for spacing and alignment
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*  // Import Material Design 3 components for UI elements
import androidx.compose.runtime.Composable  // Import for defining Composable functions
import androidx.compose.ui.Alignment  // Import for UI alignment options
import androidx.compose.ui.Modifier  // Import Modifier for UI customization
import androidx.compose.ui.text.style.TextAlign  // Import for text alignment
import androidx.compose.ui.unit.dp  // Import unit for defining dimensions in UI

/**
 * Composable function to display the About Us screen.
 * This screen provides information about the Lookoot app, its mission, and version.
 *
 * @param onBackClick Function to be called when the back arrow is clicked.
 */
@OptIn(ExperimentalMaterial3Api::class)  // Opt-in to use experimental Material 3 APIs
@Composable
fun AboutUsScreen(
    onBackClick: () -> Unit  // Callback function for back button click
) {
    // Log when the About Us screen is displayed
    Log.d("AboutUsScreen", "Displaying About Us Screen")

    // Scaffold component provides the basic structure for the screen, including the top bar
    Scaffold(
        topBar = {
            // TopAppBar provides a standard app bar with title and navigation icon
            TopAppBar(
                title = { Text("About Us") },  // Title displayed in the app bar
                navigationIcon = {
                    IconButton(onClick = {
                        Log.d("AboutUsScreen", "Back button clicked")  // Log when the back button is clicked
                        onBackClick()  // Trigger the callback function when the back button is clicked
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")  // Back arrow icon
                    }
                }
            )
        }
    ) { innerPadding ->
        // Column layout to arrange the content vertically
        Column(
            modifier = Modifier
                .fillMaxSize()  // Make the column fill the available screen space
                .padding(innerPadding)  // Apply padding to the column based on the scaffold's inner padding
                .padding(16.dp),  // Additional padding around the content
            horizontalAlignment = Alignment.CenterHorizontally,  // Center content horizontally
            verticalArrangement = Arrangement.Center  // Center content vertically
        ) {
            // Display the welcome message
            Text(
                text = "Welcome to Lookoot",
                style = MaterialTheme.typography.headlineMedium,  // Apply a medium headline style
                textAlign = TextAlign.Center  // Center the text horizontally
            )
            Spacer(modifier = Modifier.height(16.dp))  // Add vertical space between elements

            // Display the app description
            Text(
                text = "Lookoot is a local store discovery app that helps you find and connect with businesses in your area. Our mission is to support local communities by making it easier for people to discover and engage with nearby stores.",
                style = MaterialTheme.typography.bodyLarge,  // Apply a large body text style
                textAlign = TextAlign.Center  // Center the text horizontally
            )
            Spacer(modifier = Modifier.height(16.dp))  // Add vertical space between elements

            // Display the app version
            Text(
                text = "Version 1.0",
                style = MaterialTheme.typography.bodyMedium,  // Apply a medium body text style
                textAlign = TextAlign.Center  // Center the text horizontally
            )
        }
    }
}
