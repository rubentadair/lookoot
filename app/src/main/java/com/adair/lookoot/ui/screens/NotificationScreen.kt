// File: NotificationScreen.kt
package com.adair.lookoot.ui.screens

import android.util.Log  // Import for logging
import androidx.compose.foundation.layout.*  // Import layout components for spacing and alignment
import androidx.compose.foundation.lazy.LazyColumn  // Import LazyColumn for displaying lists
import androidx.compose.foundation.lazy.items  // Import items for LazyColumn to handle list items
import androidx.compose.material.icons.Icons  // Import for using icons
import androidx.compose.material.icons.filled.ArrowBack  // Import specific icon for back navigation
import androidx.compose.material3.*  // Import Material Design 3 components for UI elements
import androidx.compose.runtime.*  // Import Compose runtime for state management
import androidx.compose.ui.Modifier  // Import Modifier for UI customization
import androidx.compose.ui.unit.dp  // Import unit for defining dimensions in UI
import androidx.lifecycle.viewmodel.compose.viewModel  // Import for using ViewModel in Compose
import com.adair.lookoot.viewmodels.LookootViewModel  // Import ViewModel for handling business logic

/**
 * Data class representing a notification item.
 *
 * @param id The unique identifier for the notification.
 * @param title The title of the notification.
 * @param message The message content of the notification.
 * @param timestamp The time the notification was received or created.
 */
data class Notification(
    val id: String,
    val title: String,
    val message: String,
    val timestamp: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    onBackClick: () -> Unit,  // Callback function to handle back navigation
    viewModel: LookootViewModel = viewModel()  // ViewModel instance for managing notifications
) {
    // In a real (deployed) app, you would fetch notifications from the ViewModel
    val notifications = remember {
        // Placeholder notifications for demonstration
        listOf(
            Notification("1", "New Store", "A new store has been added near you!", "2 hours ago"),
            Notification("2", "Sale Alert", "Don't miss out on our big sale!", "1 day ago"),
            Notification("3", "New Item", "Check out the latest item in your favorite store", "3 days ago")
        )
    }

    // Log that the NotificationScreen has been initialised
    Log.d("NotificationScreen", "Notification screen initialized with ${notifications.size} notifications")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                navigationIcon = {
                    IconButton(onClick = {
                        Log.d("NotificationScreen", "Back button clicked")
                        onBackClick()
                    }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            items(notifications) { notification ->
                NotificationItem(notification)
            }
        }
    }
}

/**
 * Composable function to display an individual notification item.
 *
 * @param notification The Notification object containing the details to be displayed.
 */
@Composable
fun NotificationItem(notification: Notification) {
    Log.d("NotificationItem", "Displaying notification: ${notification.title}")
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            Text(
                text = notification.title,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = notification.message,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = notification.timestamp,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
