package com.adair.lookoot.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adair.lookoot.viewmodels.LookootViewModel

// Displays the main home screen, allowing users to toggle between map and list views, search, and view notifications.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToNotifications: () -> Unit,
    onNavigateToSearch: () -> Unit,
    viewModel: LookootViewModel = viewModel()
) {
    // State to toggle between map and list view
    var showMap by remember { mutableStateOf(false) }

    // Collecting the stores data from the ViewModel
    val stores by viewModel.stores.collectAsState()

    // Loading all stores when the composable enters the composition
    LaunchedEffect(Unit) {
        try {
            viewModel.loadAllStores()
            Log.d("HomeScreen", "Successfully loaded all stores")
        } catch (e: Exception) {
            Log.e("HomeScreen", "Error loading stores: ${e.message}", e) // Error handling with logging
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Lookoot")
                    }
                },
                actions = {
                    // Toggle button to switch between map and list views
                    IconToggleButton(
                        checked = showMap,
                        onCheckedChange = {
                            showMap = it
                            Log.d("HomeScreen", "ShowMap toggled: $showMap") // Log state changes
                        }
                    ) {
                        Icon(
                            if (showMap) Icons.Default.List else Icons.Default.Map,
                            contentDescription = if (showMap) "Show List" else "Show Map" // Accessibility improvement
                        )
                    }
                    // Button to navigate to notifications
                    IconButton(onClick = {
                        onNavigateToNotifications()
                        Log.d("HomeScreen", "Navigating to Notifications") // Log navigation
                    }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                    }
                }
            )
        }
    ) { padding ->
        // Main content column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Button to navigate to the search screen
            Button(
                onClick = {
                    onNavigateToSearch()
                    Log.d("HomeScreen", "Navigating to Search") // Log navigation
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Search Stores and Items")
            }

            // Conditional UI based on `showMap` state (e.g., list vs map)
            if (showMap) {
                // Display map view (not implemented here)
                Log.d("HomeScreen", "Displaying Map View")
                // TODO: Implement Map View here
            } else {
                // Display the list of stores
                LazyColumn {
                    items(stores) { store ->
                        // TODO: Implement StoreItem composable
                        // StoreItem(store = store, onClick = { onNavigateToStoreProfile(store.id) })
                        Log.d("HomeScreen", "Displaying store: ${store.name}")
                    }
                }
            }
        }
    }
}
