package com.adair.lookoot.ui.screens

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.adair.lookoot.models.HistoryItem
import com.adair.lookoot.models.Item
import com.adair.lookoot.models.Store
import com.adair.lookoot.models.User
import com.adair.lookoot.ui.components.ErrorDialog
import com.adair.lookoot.viewmodels.LookootViewModel
import com.adair.lookoot.viewmodels.UserProfileViewModel
import kotlinx.coroutines.launch
import java.util.Locale

// Main screen that shows a user profile and includes profile picture, username and bio editing, dark mode toggle, and followers.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    onLogout: () -> Unit,
    onNavigateToRegisterStore: () -> Unit,
    onNavigateToEditStore: (String) -> Unit,
    onNavigateToUserManagement: () -> Unit,
    onNavigateToAboutUs: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToManageAccount: () -> Unit,
    viewModel: UserProfileViewModel = viewModel(),
    lookootViewModel: LookootViewModel = viewModel(),
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val currentUser by lookootViewModel.currentUser.collectAsState()
    val userStores by lookootViewModel.userStores.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    var showFollowers by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var editedUsername by remember { mutableStateOf(userProfile?.username ?: "") }
    var editedBio by remember { mutableStateOf(userProfile?.bio ?: "") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isUsernameValid by remember { mutableStateOf(editedUsername.startsWith('@')) }
    var isUsernameAvailable by remember { mutableStateOf(true) }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var followersList by remember { mutableStateOf<List<User>>(emptyList()) }
    var showImagePicker by remember { mutableStateOf(false) }
    var profilePictureUrl by remember { mutableStateOf<String?>(null) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Dark Mode", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.weight(1f))
        Switch(
            checked = isDarkMode,
            onCheckedChange = onToggleDarkMode
        )
    }

    LaunchedEffect(currentUser?.followers) {
        currentUser?.followers?.let { followerIds ->
            followersList = followerIds.mapNotNull {
                lookootViewModel.getUser(it)
            }
        }
    }
    LaunchedEffect(currentUser?.id) {
        currentUser?.id?.let { userId ->
            lookootViewModel.loadUserStores(userId)
        }
    }

    LaunchedEffect(Unit) {
        lookootViewModel.refreshCurrentUser()
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            currentUser?.id?.let { userId ->
                viewModel.uploadProfilePicture(it, userId) { success, newUrl ->
                    if (success) {
                        Log.d("ProfileScreen", "Profile picture uploaded successfully.")
                        profilePictureUrl = newUrl
                        lookootViewModel.refreshCurrentUser()
                    } else {
                        Log.e("ProfileScreen", "Failed to upload profile picture.")
                        showError = true
                        errorMessage = "Failed to upload profile picture. Please try again."
                    }
                }
            }
        } ?: run {
            Log.w("ProfileScreen", "Image selection cancelled or failed.")
        }
        showImagePicker = false
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(300.dp)
            ) {
                DrawerContent(
                    currentUser = currentUser,
                    onLogout = onLogout,
                    onNavigateToUserManagement = onNavigateToUserManagement,
                    onNavigateToAboutUs = onNavigateToAboutUs,
                    onNavigateToNotifications = onNavigateToNotifications,
                    onNavigateToManageAccount = onNavigateToManageAccount,
                    onDeleteAccount = { showDeleteConfirmation = true },
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = onToggleDarkMode
                )

            }
        },
        gesturesEnabled = drawerState.isOpen
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Profile", style = MaterialTheme.typography.headlineMedium) },
                    actions = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AsyncImage(
                    model = userProfile?.profilePictureUrl ?: profilePictureUrl,
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .clickable { showImagePicker = true }
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (isEditing) {
                    OutlinedTextField(
                        value = editedUsername,
                        onValueChange = { newUsername ->
                            editedUsername = newUsername
                            isUsernameValid = newUsername.startsWith('@')
                            if (isUsernameValid) {
                                viewModel.isUsernameAvailable(newUsername) { available ->
                                    isUsernameAvailable = available
                                    Log.d("ProfileScreen", "Username available: $isUsernameAvailable")
                                }
                            }
                        },
                        label = { Text("Username") },
                        isError = !isUsernameValid || !isUsernameAvailable,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (!isUsernameValid) {
                        Text(
                            "Username must start with '@'",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                        Log.w("ProfileScreen", "Invalid username entered: $editedUsername")
                    } else if (!isUsernameAvailable) {
                        Text(
                            "Username is already taken",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                        Log.w("ProfileScreen", "Username is taken: $editedUsername")
                    }
                } else {
                    Text("Username: ${userProfile?.username ?: "N/A"}")
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (isEditing) {
                    OutlinedTextField(
                        value = editedBio,
                        onValueChange = { editedBio = it },
                        label = { Text("Bio") },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text("Bio: ${userProfile?.bio ?: "N/A"}")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = {
                    if (isEditing) {
                        currentUser?.id?.let {
                            viewModel.updateProfile(it, editedUsername, editedBio) { success ->
                                if (success) {
                                    Log.d("ProfileScreen", "Profile updated successfully for user ID: $it.")
                                    isEditing = false
                                    lookootViewModel.refreshCurrentUser()
                                } else {
                                    Log.e("ProfileScreen", "Failed to update profile for user ID: $it.")
                                    showError = true
                                    errorMessage = "Failed to update profile. Please try again."
                                }
                            }
                        } ?: run {
                            Log.w("ProfileScreen", "Cannot update profile; currentUser is null.")
                        }
                    } else {
                        isEditing = true
                        editedUsername = userProfile?.username ?: ""
                        editedBio = userProfile?.bio ?: ""
                    }
                }) {
                    Text(if (isEditing) "Save" else "Edit")
                }
                if (isEditing) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        isEditing = false
                        editedUsername = userProfile?.username ?: ""
                        editedBio = userProfile?.bio ?: ""
                    }) {
                        Text("Cancel")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = { showFollowers = true }) {
                    Text("Followers: ${currentUser?.followers?.size ?: 0}")
                }

                Spacer(modifier = Modifier.height(16.dp))

                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                        Text("Stores")
                    }
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                        Text("Wishlist")
                    }
                    Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                        Text("History")
                    }
                }

                when (selectedTab) {
                    0 -> StoresTab(
                        stores = userStores,
                        onNavigateToEditStore = onNavigateToEditStore,
                        onNavigateToRegisterStore = onNavigateToRegisterStore,
                        userRole = currentUser?.role,
                        currentUser = currentUser,
                        viewModel = lookootViewModel
                    )
                    1 -> WishlistTab(lookootViewModel)
                    2 -> HistoryTab(lookootViewModel)
                }
            }

            if (showDeleteConfirmation) {
                DeleteAccountDialog(
                    onConfirmation = { password ->
                        currentUser?.id?.let { userId ->
                            viewModel.deleteAccount(userId, password) { success, errorMsg ->
                                if (success) {
                                    Log.d("ProfileScreen", "Account deleted successfully for user ID: $userId.")
                                    showDeleteConfirmation = false
                                    onLogout() // Call onLogout here to ensure the user is logged out after account deletion
                                } else {
                                    showError = true
                                    errorMessage = errorMsg ?: "Failed to delete account. Please try again or re-authenticate."
                                    showDeleteConfirmation = false
                                }
                            }
                        } ?: run {
                            Log.w("ProfileScreen", "Cannot delete account; currentUser ID is null.")
                            showError = true
                            errorMessage = "Unable to delete account. User ID not found."
                            showDeleteConfirmation = false
                        }
                    },
                    onDismiss = { showDeleteConfirmation = false }
                )
            }
            if (showError) {
                ErrorDialog(
                    errorMessage = errorMessage,
                    onDismiss = { showError = false }
                )
                Log.e("ProfileScreen", "Error shown: $errorMessage")
            }

            if (showImagePicker) {
                LaunchedEffect(showImagePicker) {
                    launcher.launch("image/*")
                    Log.d("ProfileScreen", "Image picker launched.")
                }
            }

            if (showFollowers) {
                FollowersDialog(
                    followers = followersList,
                    onDismiss = { showFollowers = false }
                )
            }
        }
    }
}

// Modal drawer menu for navigating to different sections like manage account, notifications, about, dark mode toggle, and logout.
@Composable
fun DrawerContent(
    currentUser: User?,
    onLogout: () -> Unit,
    onNavigateToUserManagement: () -> Unit,
    onNavigateToAboutUs: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToManageAccount: () -> Unit,
    onDeleteAccount: () -> Unit,
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(16.dp)
    ) {
        Text(
            "Menu",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        HorizontalDivider()

        currentUser?.let { user ->
            DrawerButton(
                text = "Manage Account (${user.email})",
                onClick = onNavigateToManageAccount,
                icon = Icons.Default.AccountCircle
            )
            Text(
                text = "Role: ${user.role.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(
                        Locale.getDefault()
                    ) else it.toString()
                }}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
            )
            HorizontalDivider()
        }
        DrawerButton(
            text = "Notifications",
            onClick = onNavigateToNotifications,
            icon = Icons.Default.Notifications
        )

        DrawerButton(
            text = "About Us",
            onClick = onNavigateToAboutUs,
            icon = Icons.Default.Info
        )

        if (currentUser?.role == "ADMIN") {
            DrawerButton(
                text = "Manage Users",
                onClick = {
                    Log.d("ProfileScreen", "Attempting to navigate to User Management")
                    onNavigateToUserManagement()
                },
                icon = Icons.Default.People
            )
        }

        DrawerButton(
            text = "Dark Mode",
            icon = Icons.Default.Brightness6,
            content = {
                Switch(
                    checked = isDarkMode,
                    onCheckedChange = { onToggleDarkMode(it) }
                )
            }
        )

        DrawerButton(
            text = "Delete Account",
            onClick = onDeleteAccount,
            icon = Icons.Default
                .Delete,
            textColor = MaterialTheme.colorScheme.error
        )

        DrawerButton(
            text = "Logout",
            onClick = onLogout,
            icon = Icons.AutoMirrored.Filled.ExitToApp
        )
    }
}
// A reusable button for the drawer, allowing navigation to different screens.
@Composable
fun DrawerButton(
    text: String,
    onClick: (() -> Unit)? = null,
    icon: ImageVector,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    content: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(enabled = onClick != null) {
                onClick?.invoke()
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        content?.invoke()
    }
}
// Displays a list of stores owned by the user, allows registering a new store or requesting store ownership.
@Composable
fun StoresTab(
    stores: List<Store>,
    onNavigateToEditStore: (String) -> Unit,
    onNavigateToRegisterStore: () -> Unit,
    userRole: String?,
    currentUser: User?,
    viewModel: LookootViewModel
) {
    LazyColumn {
        items(stores) { store ->
            StoreItem(store, onNavigateToEditStore)
        }
        item {
            if (userRole == "STORE_OWNER" || userRole == "ADMIN") {
                Button(
                    onClick = onNavigateToRegisterStore,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Register New Store")
                }
            } else if (userRole == "USER") {
                StoreOwnerRequestButton(currentUser, viewModel)
            }
        }
    }
}
// Allows a user to request store ownership if they are not already a store owner or have a pending request.
@Composable
fun StoreOwnerRequestButton(
    currentUser: User?,
    viewModel: LookootViewModel
) {
    if (currentUser?.role != "STORE_OWNER" && currentUser?.storeOwnerRequestStatus != "PENDING") {
        Button(
            onClick = {
                viewModel.requestStoreOwnership()
                Log.d("StoreOwnerRequestButton", "Store ownership requested by user ID: ${currentUser?.id}")
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("Request to be a Store Owner")
        }
    } else if (currentUser?.storeOwnerRequestStatus == "PENDING") {
        Text(
            "Store owner request pending",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
        Log.d("StoreOwnerRequestButton", "Store owner request is pending for user ID: ${currentUser.id}")
    }
}
// Displays individual store items in the store list.
@Composable
fun StoreItem(store: Store, onNavigateToEditStore: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable {
                onNavigateToEditStore(store.id)
                Log.d("StoreItem", "Navigating to edit store with ID: ${store.id}")
            }
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                store.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                onNavigateToEditStore(store.id)
                Log.d("StoreItem", "Edit button clicked for store ID: ${store.id}")
            }) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Store")
            }
        }
    }
}
// Displays the user's wishlist items.
@Composable
fun WishlistTab(viewModel: LookootViewModel) {
    val wishlistItems by viewModel.wishlistItems.collectAsState()

    LazyColumn {
        items(wishlistItems) { item ->
            WishlistItemCard(item)
        }
    }
}
// Displays individual wishlist items.
@Composable
fun WishlistItemCard(item: Item) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(item.name, style = MaterialTheme.typography.titleMedium)
                Text(item.description, style = MaterialTheme.typography.bodyMedium)
            }
            Text("£${item.price}", style = MaterialTheme.typography.titleMedium)
        }
    }
}
// Displays the user's action history.
@Composable
fun HistoryTab(viewModel: LookootViewModel) {
    val userHistory by viewModel.userHistory.collectAsState()

    LazyColumn {
        items(userHistory) { historyItem ->
            HistoryItemCard(historyItem)
        }
    }
}
// Displays individual history items in the user's action history.
@Composable
fun HistoryItemCard(historyItem: HistoryItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(historyItem.action, style = MaterialTheme.typography.titleMedium)
            Text(historyItem.timestamp.toString(), style = MaterialTheme.typography.bodyMedium)
            Text(historyItem.details, style = MaterialTheme.typography.bodySmall)
        }
    }
}
// A dialog that shows a list of users who follow the current user.
@Composable
fun FollowersDialog(followers: List<User>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Followers") },
        text = {
            LazyColumn {
                items(followers) { follower ->
                    Text(follower.username)
                    Log.d("FollowersDialog", "Displaying follower: ${follower.username}")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
                Log.d("FollowersDialog", "Followers dialog closed.")
            }
        }
    )
}
// A dialog for confirming account deletion, where the user enters their current password.
@Composable
fun DeleteAccountDialog(onConfirmation: (String) -> Unit, onDismiss: () -> Unit) {
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Account") },
        text = {
            Column {
                Text("Are you sure you want to delete your account? This action cannot be undone.")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Current Password") },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (showPassword) "Hide password" else "Show password"
                            )
                        }
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirmation(password) }) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}