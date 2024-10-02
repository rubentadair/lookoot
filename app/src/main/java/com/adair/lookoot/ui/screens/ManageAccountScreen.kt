package com.adair.lookoot.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.adair.lookoot.viewmodels.UserProfileViewModel

// Displays a screen for managing user account settings and activity.
// Allows navigation to personal details, password security, and email settings screens.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageAccountScreen(
    viewModel: UserProfileViewModel,
    onBackClick: () -> Unit,
    onNavigateToPersonalDetails: () -> Unit,
    onNavigateToPasswordSecurity: () -> Unit,
    onNavigateToEmailChange: () -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings and activity") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            // Displays "Your account" section and various settings options.
            item {
                Text(
                    "Your account",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
            // Personal details settings item
            item {
                SettingsItem(
                    icon = Icons.Default.AccountCircle,
                    title = "Personal details",
                    subtitle = "Edit your personal information",
                    onClick = onNavigateToPersonalDetails
                )
            }
            // Password and security settings item
            item {
                SettingsItem(
                    icon = Icons.Default.Lock,
                    title = "Password and security",
                    subtitle = "Change password, setup 2FA",
                    onClick = onNavigateToPasswordSecurity
                )
            }
            // Email settings item, showing the current email or "Not set" if not available.
            item {
                SettingsItem(
                    icon = Icons.Default.Email,
                    title = "Email",
                    subtitle = userProfile?.email ?: "Not set",
                    onClick = onNavigateToEmailChange
                )
            }
            // Add more settings items as needed
        }
    }
}
// A reusable component that displays a row with an icon, title, subtitle, and a chevron for navigation.
@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            // Displays title and subtitle (e.g., "Personal details" and "Edit your personal information").
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium)
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Navigate"
            )
        }
    }
}