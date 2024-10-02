// File: AdminManagementScreen.kt
package com.adair.lookoot.ui.screens

import android.util.Log  // Import for logging
import androidx.compose.foundation.layout.*  // Import layout components for spacing and alignment
import androidx.compose.foundation.lazy.LazyColumn  // Import LazyColumn for displaying lists
import androidx.compose.foundation.lazy.items  // Import items for LazyColumn to handle list items
import androidx.compose.material3.*  // Import Material Design 3 components for UI elements
import androidx.compose.runtime.*  // Import Compose runtime for state management
import androidx.compose.ui.Alignment  // Import for UI alignment options
import androidx.compose.ui.Modifier  // Import Modifier for UI customization
import androidx.compose.ui.unit.dp  // Import unit for defining dimensions in UI
import androidx.lifecycle.viewmodel.compose.viewModel  // Import for using ViewModel in Compose
import com.adair.lookoot.models.User  // Import User model
import com.adair.lookoot.viewmodels.LookootViewModel  // Import ViewModel to handle business logic

// The main screen for admin management, displaying all users and pending store owner requests.
// It also includes a search bar for filtering users by username or email.
// Uses a Scaffold with a TopAppBar and LazyColumn to list users and pending requests.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminManagementScreen(
    viewModel: LookootViewModel = viewModel()
) {
    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    var pendingRequests by remember { mutableStateOf<List<User>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Search query state
    var searchQuery by remember { mutableStateOf("") }

    // Filter users and pending requests by search query
    val filteredPendingRequests = pendingRequests.filter {
        it.username.contains(searchQuery, ignoreCase = true) ||
                it.email.contains(searchQuery, ignoreCase = true)
    }
    val filteredUsers = users.filter {
        it.username.contains(searchQuery, ignoreCase = true) ||
                it.email.contains(searchQuery, ignoreCase = true)
    }

    // Fetch users when the screen is launched
    LaunchedEffect(Unit) {
        viewModel.getAllUsers { allUsers ->
            users = allUsers
            pendingRequests = allUsers.filter { it.storeOwnerRequestStatus == "PENDING" }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Management") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search Users") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Section for pending store owner requests
                item {
                    Text(
                        "Pending Store Owner Requests",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                // Check if there are any pending requests to display
                if (filteredPendingRequests.isEmpty()) {
                    item {
                        Text(
                            "No pending store owner requests",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    // Display filtered pending store owner requests
                    items(filteredPendingRequests) { user ->
                        PendingRequestItem(
                            user = user,
                            onApprove = {
                                viewModel.approveStoreOwnerRequest(user.id)
                                refreshUserList(viewModel) { allUsers ->
                                    users = allUsers
                                    pendingRequests = allUsers.filter {
                                        it.storeOwnerRequestStatus == "PENDING"
                                    }
                                }
                            },
                            onDecline = {
                                viewModel.declineStoreOwnerRequest(user.id)
                                refreshUserList(viewModel) { allUsers ->
                                    users = allUsers
                                    pendingRequests = allUsers.filter {
                                        it.storeOwnerRequestStatus == "PENDING"
                                    }
                                }
                            }
                        )
                    }
                }

                // Section for all users
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "All Users",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                // Display filtered users
                items(filteredUsers) { user ->
                    UserListItem(
                        user = user,
                        onRoleChange = { newRole ->
                            viewModel.updateUserRole(user.id, newRole)
                            refreshUserList(viewModel) { allUsers ->
                                users = allUsers
                                pendingRequests = allUsers.filter { it.storeOwnerRequestStatus == "PENDING" }
                            }
                        }
                    )
                }
            }

            // Display an error message if any
            errorMessage?.let { message ->
                Snackbar(
                    action = {
                        Button(onClick = { errorMessage = null }) {
                            Text("Dismiss")
                        }
                    },
                    content = { Text(text = message) },
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}
// Helper function to refresh the list of users by fetching them from the ViewModel.
// Updates the user list and pending store owner requests accordingly.
private fun refreshUserList(viewModel: LookootViewModel, onUsersLoaded: (List<User>) -> Unit) {
    viewModel.getAllUsers { allUsers ->
        onUsersLoaded(allUsers)
    }
}


// Displays a card for each pending store owner request, with "Approve" and "Decline" buttons.
// Approves or declines the request and logs actions when buttons are clicked.
/**
 * Composable function for displaying an individual pending request item.
 *
 * @param user The User object representing the user with a pending store owner request.
 * @param onApprove Callback function triggered when the request is approved.
 * @param onDecline Callback function triggered when the request is declined.
 */
@Composable
fun PendingRequestItem(
    user: User,
    onApprove: () -> Unit,
    onDecline: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = user.username, style = MaterialTheme.typography.titleMedium)
            Text(text = user.email, style = MaterialTheme.typography.bodyMedium)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(onClick = {
                    try {
                        onApprove()
                    } catch (e: Exception) {
                        Log.e("PendingRequestItem", "Error approving request: ${e.message}", e)
                    }
                }) {
                    Text("Approve")
                    Log.d("PendingRequestItem", "Approve button clicked for user: ${user.id}")
                }
                Button(
                    onClick = {
                        try {
                            onDecline()
                        } catch (e: Exception) {
                            Log.e("PendingRequestItem", "Error declining request: ${e.message}", e)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Decline")
                    Log.d("PendingRequestItem", "Decline button clicked for user: ${user.id}")
                }
            }
        }
    }
}
// Displays a user card, showing the user's username, email, and current role.
// Allows the admin to change the user's role using a dropdown menu and confirms the role change with a dialog.
/**
 * Composable function for displaying an individual user item with role management.
 *
 * @param user The User object representing the user.
 * @param onRoleChange Callback function triggered when the role is changed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserListItem(user: User, onRoleChange: (String) -> Unit) {
    // State to hold the selected role
    var selectedRole by remember { mutableStateOf(user.role) }
    // State to manage dropdown expansion
    var expanded by remember { mutableStateOf(false) }
    // State to manage confirmation dialog visibility
    var showConfirmationDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = user.username, style = MaterialTheme.typography.titleMedium)
            Text(text = user.email, style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Role: ", style = MaterialTheme.typography.bodySmall)

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    TextField(
                        value = selectedRole,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        listOf("USER", "STORE_OWNER", "ADMIN").forEach { role ->
                            DropdownMenuItem(
                                text = { Text(role) },
                                onClick = {
                                    selectedRole = role
                                    expanded = false
                                    showConfirmationDialog = true
                                    Log.d("UserListItem", "Role selected: $role for user: ${user.id}")
                                }
                            )
                        }
                    }
                }
            }

            if (showConfirmationDialog) {
                AlertDialog(
                    onDismissRequest = { showConfirmationDialog = false },
                    title = { Text("Confirm Role Change") },
                    text = { Text("Are you sure you want to change the role to $selectedRole?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                try {
                                    Log.d("UserListItem", "Role change confirmed to $selectedRole for user: ${user.id}")
                                    onRoleChange(selectedRole)
                                } catch (e: Exception) {
                                    Log.e("UserListItem", "Error confirming role change: ${e.message}", e)
                                } finally {
                                    showConfirmationDialog = false
                                }
                            }
                        ) {
                            Text("Confirm")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showConfirmationDialog = false
                            Log.d("UserListItem", "Role change cancelled for user: ${user.id}")
                        }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}
