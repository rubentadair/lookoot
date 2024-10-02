package com.adair.lookoot

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adair.lookoot.ui.theme.LookootTheme
import com.adair.lookoot.viewmodels.AppViewModel
import com.adair.lookoot.viewmodels.LoginViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.messaging.FirebaseMessaging

// Main activity that initializes Firebase services, handles permissions, and sets up the UI.
class MainActivity : ComponentActivity() {
    // Handles the result of the notification permission request.
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // TO DO
        } else {
            // TO DO
        }
    }
    // Handles the result of location permission requests (both fine and coarse location).
    private val requestLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                // Precise location access granted.
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                // Only approximate location access granted.
            }
            else -> {
                // No location access granted.
            }
        }
    }
    // Initializes Firebase services (Firestore, Messaging, Crashlytics, AppCheck)
    // and sets up the content using Compose UI.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance()
        )
        FirebaseApp.initializeApp(this)
        FirebaseFirestore.getInstance()
        FirebaseFirestore.getInstance().firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
        FirebaseFirestore.setLoggingEnabled(true)
        FirebaseMessaging.getInstance().isAutoInitEnabled = true

        // Sets the theme and loads the main application content using Compose.
        setContent {
            LookootTheme {
                val loginViewModel: LoginViewModel = viewModel()
                val appViewModel: AppViewModel = viewModel()
                LookootApp(appViewModel = appViewModel, loginViewModel = loginViewModel)
            }
        }

        // Fetches the Firebase Cloud Messaging (FCM) token for push notifications.
        // Logs the FCM token or handles the failure if fetching fails.
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
            Log.d("FCM", "FCM Token: $token")
        }

        askNotificationPermission()// Requests notification permission.
        checkLocationPermission()// Checks location permission and requests if needed.
    }

    // Requests notification permission if not already granted.
    // Checks if the POST_NOTIFICATIONS permission is granted, shows rationale or requests permission.
    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
            //TO DO
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
             //TO DO
            } else {
                // Directly ask for the permission
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Checks if location permissions (fine and coarse) are granted, shows rationale or requests permissions.
    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
            //TO DO
            }
            // Displays a rationale for requesting location permissions to the user.
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
            //TO DO
                showLocationPermissionRationale()
            }
            else -> {
          // directly ask for the permission.
                requestLocationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    private fun showLocationPermissionRationale() {
        //TO DO
    }
}