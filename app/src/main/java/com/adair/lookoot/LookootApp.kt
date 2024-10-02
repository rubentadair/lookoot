package com.adair.lookoot

import android.util.Log
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.adair.lookoot.models.Item
import com.adair.lookoot.ui.screens.*
import com.adair.lookoot.viewmodels.LookootViewModel
import com.adair.lookoot.viewmodels.UserProfileViewModel
import java.util.Locale
import com.adair.lookoot.ui.theme.LookootTheme
import com.adair.lookoot.ui.screens.ItemDetailScreen
import com.adair.lookoot.viewmodels.AppViewModel
import com.adair.lookoot.viewmodels.LoginViewModel

@Composable
fun LookootApp(
    appViewModel: AppViewModel,
    loginViewModel: LoginViewModel,
    userProfileViewModel: UserProfileViewModel = viewModel()
) {
    // Sets up the main app layout, manages user dark mode preferences,
    // and applies the app's theme based on the dark mode setting.
    val lookootViewModel: LookootViewModel = viewModel()
    val systemInDarkTheme = isSystemInDarkTheme()
    val currentUser by lookootViewModel.currentUser.collectAsState()
    val isDarkMode by userProfileViewModel.isDarkMode.collectAsState()
    val effectiveDarkMode = isDarkMode ?: systemInDarkTheme

    LaunchedEffect(currentUser) {
        currentUser?.id?.let { userId ->
            userProfileViewModel.loadUserProfile(userId) // Pass userId here
        }
    }

    val toggleDarkMode: (Boolean) -> Unit = { newMode ->
        currentUser?.id?.let { userId ->
            userProfileViewModel.updateDarkModePreference(userId, newMode) { success ->
                if (!success) {
                    // Handle the error, maybe show a toast or snackbar
                }
            }
        }
    }

    LookootTheme(darkTheme = effectiveDarkMode) {
        LookootAppContent(
            appViewModel = appViewModel,
            loginViewModel = loginViewModel,
            lookootViewModel = lookootViewModel,
            userProfileViewModel = userProfileViewModel,
            isDarkMode = effectiveDarkMode,
            onToggleDarkMode = toggleDarkMode
        )
    }
}

@Composable
fun LookootAppContent(
    appViewModel: AppViewModel,
    loginViewModel: LoginViewModel,
    lookootViewModel: LookootViewModel,
    userProfileViewModel: UserProfileViewModel,
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit
) {
    // Handles the app's navigation and scaffold, including navigation
    // to screens like login, home, search, profile, and others.
    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStack?.destination
    val currentScreen by remember { mutableStateOf(currentDestination?.route ?: "login") }
    val isLoggedIn by appViewModel.isLoggedIn.collectAsState()
    val isUserSignedIn by loginViewModel.isUserSignedIn.collectAsState()
    val lookootViewModel: LookootViewModel = viewModel() // Must be inside a composable
    val currentUser by lookootViewModel.currentUser.collectAsState()

    LaunchedEffect(isUserSignedIn) {
        when (isUserSignedIn) {
            true -> {
                loginViewModel.autoSignIn { success, _ ->
                    if (success) {
                        appViewModel.setLoggedIn(true)
                        navController.navigate("home") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                }
            }
            false -> {
                if (currentScreen !in listOf("login", "register", "password_reset")) {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            null -> {} // Still checking, do nothing
        }
    }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn && currentScreen !in listOf("home", "search", "profile")) {
            navController.navigate("home") {
                popUpTo("login") { inclusive = true }
            }
        } else if (!isLoggedIn && currentScreen !in listOf("login", "register", "password_reset")) {
            navController.navigate("login") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (isLoggedIn) {
                NavigationBar {
                    listOf("home", "search", "profile").forEach { screen ->
                        NavigationBarItem(
                            selected = currentScreen == screen,
                            onClick = { navController.navigate(screen) },
                            icon = {
                                when (screen) {
                                    "home" -> Icon(Icons.Default.Home, "Home")
                                    "search" -> Icon(Icons.Default.Search, "Search")
                                    "profile" -> Icon(Icons.Default.Person, "Profile")
                                    else -> Icon(Icons.Default.Home, "Home")
                                }
                            },
                            label = {
                                Text(screen.replaceFirstChar {
                                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                                })
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "login",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("about") {
                AboutUsScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable("admin_management") {
                AdminManagementScreen(viewModel = lookootViewModel)
            }
            composable("notifications") {
                NotificationScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable("manage_account") {
                ManageAccountScreen(
                    viewModel = userProfileViewModel,
                    onBackClick = { navController.popBackStack() },
                    // Ensure proper navigation
                    onNavigateToPersonalDetails = { navController.navigate("edit_personal_details") },
                    onNavigateToPasswordSecurity = { navController.navigate("password_security") },
                    onNavigateToEmailChange = { navController.navigate("change_email") }
                )
            }
            composable("edit_personal_details") {
                val userProfileViewModel: UserProfileViewModel = viewModel()
                EditPersonalDetailsScreen(
                    viewModel = userProfileViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable("password_security") {
                val userProfileViewModel: UserProfileViewModel = viewModel()
                PasswordSecurityScreen(
                    viewModel = userProfileViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable("change_email") {
                val userProfileViewModel: UserProfileViewModel = viewModel()
                ChangeEmailScreen(
                    viewModel = userProfileViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable("login") {
                LoginScreen(
                    onLoginSuccess = {
                        appViewModel.setLoggedIn(true)
                        navController.navigate("home") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    onNavigateToRegister = {
                        navController.navigate("register")
                    },
                    onNavigateToPasswordReset = {
                        navController.navigate("password_reset")
                    }
                )
            }
            composable("register") {
                RegisterScreen(
                    onRegisterSuccess = {
                        appViewModel.setLoggedIn(true)
                        navController.navigate("home") {
                            popUpTo("register") { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.navigate("login")
                    }
                )
            }
            composable("password_reset") {
                PasswordResetScreen(
                    viewModel = userProfileViewModel,
                    onNavigateToLogin = {
                        navController.navigate("login")
                    }
                )
            }
            composable("home") {
                HomeScreen(
                    onNavigateToNotifications = { navController.navigate("notifications") },
                    onNavigateToSearch = { navController.navigate("search") }
                )
            }
            composable("search") {
                SearchScreen(
                    onNavigateToStoreProfile = { storeId ->
                        Log.d("LookootAppContent", "Navigating to store_view/$storeId")
                        navController.navigate("store_view/$storeId")
                    },
                    viewModel = lookootViewModel,
                    navController = navController,
                    onNavigateToItemDetailScreen = { itemId ->
                        navController.navigate("item_detail/$itemId")
                    }
                )
            }
            composable(
                route = "store_view/{storeId}",
                arguments = listOf(navArgument("storeId") { type = NavType.StringType })
            ) { backStackEntry ->
                val storeId = backStackEntry.arguments?.getString("storeId")
                Log.d("LookootAppContent", "Entered store_view with storeId: $storeId")
                StoreProfileScreen(
                    viewModel = lookootViewModel,
                    isOwner = currentUser?.role == "STORE_OWNER" || currentUser?.role == "ADMIN",
                    onNavigateToItemDetail = { itemId ->
                        Log.d("LookootAppContent", "Navigating to item_detail. StoreId: $storeId, ItemId: $itemId")
                        navController.navigate("item_detail/$storeId/$itemId")
                    },
                    onBackClick = { navController.popBackStack() },
                    storeId = storeId
                )
            }

        composable("profile") {
            ProfileScreen(
                navController = navController,
                onLogout = {
                    appViewModel.setLoggedIn(false)
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToRegisterStore = {
                    if (currentUser?.role == "STORE_OWNER" || currentUser?.role == "ADMIN") {
                        navController.navigate("register_store")
                    }
                },
                onNavigateToEditStore = { storeId ->
                    navController.navigate("edit_store/$storeId")
                },
                onNavigateToUserManagement = {
                    if (currentUser?.role == "ADMIN") {
                        Log.d("LookootAppContent", "Navigating to user_management")
                        navController.navigate("user_management")
                    } else {
                        Log.d("LookootAppContent", "User is not an admin, cannot navigate to user_management")
                    }
                },
                onNavigateToNotifications = {
                    navController.navigate("notifications")
                },
                onNavigateToAboutUs = {
                    navController.navigate("about")
                },
                onNavigateToManageAccount = {
                    navController.navigate("manage_account")
                },
                isDarkMode = isDarkMode,
                onToggleDarkMode = onToggleDarkMode
            )
        }
            composable(
                route = "item_detail/{itemId}",
                arguments = listOf(
                    navArgument("itemId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
                val currentUser by lookootViewModel.currentUser.collectAsState()

                ItemDetailScreen(
                    itemId = itemId,
                    viewModel = lookootViewModel,
                    onNavigateToEditItem = { /* Implement if needed */ },

                    onBackClick = {
                        navController.popBackStack()
                    }
                    // You may need to pass the actual storeId if available
                )
            }
            composable(
                "edit_store/{storeId}",
                arguments = listOf(navArgument("storeId") { type = NavType.StringType })
            ) { backStackEntry ->
                val storeId = backStackEntry.arguments?.getString("storeId") ?: ""
                StoreEditScreen(
                    storeId = storeId,
                    onBackClick = { navController.popBackStack() },
                    onSaveClick = { navController.popBackStack() },
                    onNavigateToItemEdit = { itemId: String ->
                        navController.navigate("item_edit/$storeId/$itemId")
                    },
                    onStoreDeleted = {
                        navController.navigate("profile") {
                            popUpTo("profile") { inclusive = true }
                        }
                    },
                    viewModel = lookootViewModel
                )
            }
            composable(
                route = "item_detail/{storeId}/{itemId}",
                arguments = listOf(
                    navArgument("storeId") { type = NavType.StringType },
                    navArgument("itemId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val storeId = backStackEntry.arguments?.getString("storeId") ?: ""
                val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
                val currentUser by lookootViewModel.currentUser.collectAsState()

                ItemDetailScreen(
                    itemId = itemId,
                    viewModel = lookootViewModel,
                    onNavigateToEditItem = { /* Implement if needed */ },
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(
                route = "store_edit/{storeId}",
                arguments = listOf(navArgument("storeId") { type = NavType.StringType })
            ) { backStackEntry ->
                val storeId = backStackEntry.arguments?.getString("storeId") ?: ""
                StoreEditScreen(
                    storeId = storeId,
                    onBackClick = { navController.popBackStack() },
                    onSaveClick = { navController.popBackStack() },
                    onNavigateToItemEdit = { itemId ->
                        navController.navigate("item_edit/$storeId/$itemId")
                    },
                    onStoreDeleted = {
                        navController.navigate("profile") {
                            popUpTo("profile") { inclusive = true }
                        }
                    },
                    viewModel = lookootViewModel
                )
            }
            composable(
                route = "item_edit/{storeId}/{itemId}",
                arguments = listOf(
                    navArgument("storeId") { type = NavType.StringType },
                    navArgument("itemId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val storeId = backStackEntry.arguments?.getString("storeId") ?: ""
                val itemId = backStackEntry.arguments?.getString("itemId") ?: ""

                ItemEditScreen(
                    itemId = itemId,
                    storeId = storeId,
                    onNavigateBack = { navController.popBackStack() },
                    viewModel = lookootViewModel
                )
            }
            composable("register_store") {
                StoreRegistrationScreen(
                    viewModel = lookootViewModel,
                    onStoreRegistered = {
                        navController.navigate("profile") {
                            popUpTo("profile") { inclusive = true }
                        }
                    }
                )
            }
            composable(
                "add_item/{storeId}",
                arguments = listOf(navArgument("storeId") { type = NavType.StringType })
            ) { backStackEntry ->
                val storeId = backStackEntry.arguments?.getString("storeId") ?: ""
                AddItemScreen(
                    storeId = storeId,
                    onItemAdded = {
                        navController.popBackStack()
                    },
                    viewModel = lookootViewModel
                )
            }
            composable("user_management") {
                if (currentUser?.role == "ADMIN") {
                    AdminManagementScreen(viewModel = lookootViewModel)
                } else {
                    Log.d("LookootAppContent", "User is not an admin, cannot access User Management")
                }
            }
        } // Close NavHost here
    }
}

// Defines the dark and light themes and applies them based on
// whether dark mode is enabled or not.
@Composable
fun LookootTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFF6200EE),
            secondary = Color(0xFF03DAC5),
            background = Color(0xFF121212)
            // Add more color definitions as needed
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF6200EE),
            secondary = Color(0xFF03DAC5),
            background = Color(0xFFFFFFFF)
            // Add more color definitions as needed
        )
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography(),
        content = content
    )
}
// Displays the UI for adding a new item to a store, tied to the given store ID.
@Composable
fun AddItemScreen(storeId: String, onItemAdded: () -> Unit, viewModel: LookootViewModel) {
    // Implementation for AddItemScreen
}
// Displays details of an individual item, such as name, description, and price.
@Composable
fun DisplayItemDetails(item: Item) {
    Text(
        text = item.name,
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    Text(
        text = item.description,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    Text(
        text = "Price: £${item.price}",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = 16.dp)
    )
}


