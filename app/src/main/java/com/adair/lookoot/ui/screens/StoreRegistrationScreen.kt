package com.adair.lookoot.ui.screens

import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adair.lookoot.models.Store
import com.adair.lookoot.viewmodels.LookootViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

// Main composable function to display the store registration screen, allowing users to input store details and search for location.
@Composable
fun StoreRegistrationScreen(
    viewModel: LookootViewModel = viewModel(),
    onStoreRegistered: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var storeName by remember { mutableStateOf("") }
    var storeDescription by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var storeLocation by remember { mutableStateOf<LatLng?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var predictions by remember { mutableStateOf<List<String>>(emptyList()) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    val glasgow = LatLng(55.8642, -4.2518)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(glasgow, 10f)
    }

    // Initialize Places API
    val placesClient = remember {
        val applicationInfo = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
        val apiKey = applicationInfo.metaData.getString("com.google.android.geo.API_KEY")
        if (apiKey != null) {
            Places.initialize(context, apiKey)
        }
        Places.createClient(context)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
    ) {
        item {
            Text(
                text = "Register a Store",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = storeName,
                onValueChange = { storeName = it },
                label = { Text("Store Name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = storeDescription,
                onValueChange = { storeDescription = it },
                label = { Text("Store Description") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .padding(bottom = 16.dp)
            )

            Text("Search for Store Location", style = MaterialTheme.typography.bodyLarge)

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { newQuery ->
                    searchQuery = newQuery
                    searchError = null // Clear previous errors
                    coroutineScope.launch {
                        performAutocompletePredictions(
                            placesClient,
                            newQuery,
                            { newPredictions -> predictions = newPredictions },
                            { error -> searchError = error }
                        )
                    }
                },
                label = { Text("Search address") },
                isError = searchError != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    coroutineScope.launch {
                        storeLocation = geocodeAddress(context, searchQuery)
                        storeLocation?.let { location ->
                            cameraPositionState.position = CameraPosition.fromLatLngZoom(location, 15f)
                        }
                    }
                })
            )

            searchError?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )
            }

            if (predictions.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    items(predictions) { prediction ->
                        Text(
                            text = prediction,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    coroutineScope.launch {
                                        storeLocation = geocodeAddress(context, prediction)
                                        storeLocation?.let { location ->
                                            cameraPositionState.position =
                                                CameraPosition.fromLatLngZoom(location, 15f)
                                        }
                                        searchQuery = prediction
                                        predictions = emptyList()
                                    }
                                }
                                .padding(vertical = 8.dp)
                        )
                    }
                }
            }

            GoogleMap(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(top = 4.dp),
                cameraPositionState = cameraPositionState,
                onMapClick = { latLng ->
                    storeLocation = latLng
                    coroutineScope.launch {
                        searchQuery = reverseGeocode(context, latLng) ?: ""
                    }
                }
            ) {
                storeLocation?.let { location ->
                    Marker(
                        state = MarkerState(position = location),
                        title = "Store Location"
                    )
                }
            }

            Button(
                onClick = {
                    coroutineScope.launch {
                        isLoading = true
                        errorMessage = null
                        try {
                            val newStore = Store(
                                id = "", // This will be set by Firebase
                                ownerId = viewModel.currentUserId ?: throw IllegalStateException("User not logged in"),
                                name = storeName,
                                description = storeDescription,
                                location = storeLocation?.let { GeoPoint(it.latitude, it.longitude) },
                                categories = emptyList(),
                                tags = emptyList(),
                                rating = 0.0,
                                reviewCount = 0,
                                createdAt = Timestamp.now(),
                                lastUpdated = Timestamp.now(),
                                followers = listOf(),
                                openingTimes = emptyMap()
                            )
                            viewModel.registerStore(newStore)
                            showSuccessDialog = true
                        } catch (e: Exception) {
                            errorMessage = "Failed to register store: ${e.message}"
                            Log.e("StoreRegistrationScreen", "Error registering store", e)
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && storeName.isNotEmpty() && storeDescription.isNotEmpty() && storeLocation != null
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Register Store")
                }
            }

            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                onStoreRegistered()
            },
            title = { Text("Success") },
            text = { Text("Store successfully registered. You can now edit it in your store list.") },
            confirmButton = {
                Button(onClick = {
                    showSuccessDialog = false
                    onStoreRegistered()
                }) {
                    Text("OK")
                }
            }
        )
    }
}