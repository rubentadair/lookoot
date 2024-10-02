package com.adair.lookoot.ui.screens

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adair.lookoot.models.Item
import com.adair.lookoot.models.OpeningHours
import com.adair.lookoot.models.Review
import com.adair.lookoot.models.Store
import com.adair.lookoot.ui.components.AddItemDialog
import com.adair.lookoot.ui.components.ErrorDialog
import com.adair.lookoot.ui.components.ItemCard
import com.adair.lookoot.viewmodels.LookootViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.withTimeout

// Main composable function to display store profile details including items and reviews
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreProfileScreen(
    viewModel: LookootViewModel = viewModel(),
    isOwner: Boolean,
    onNavigateToItemDetail: (String) -> Unit,
    onBackClick: () -> Unit,
    storeId: String?
) {
    val currentStore by viewModel.currentStore.collectAsState()
    val storeItems by viewModel.storeItems.collectAsState()
    val storeReviews by viewModel.storeReviews.collectAsState()
    var showErrorMessage by remember { mutableStateOf<String?>(null) }
    var showAddItemDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var showStreetView by remember { mutableStateOf(false) }
    var streetViewLocation by remember { mutableStateOf<LatLng?>(null) }

LaunchedEffect(storeId) {
    if (storeId != null) {
        Log.d("StoreProfileScreen", "Loading store with ID: $storeId")
        isLoading = true
        try {
            withTimeout(10000) {
                viewModel.loadStore(storeId)
                viewModel.loadStoreItems(storeId)
                viewModel.loadStoreReviews(storeId)
            }
            Log.d("StoreProfileScreen", "Store data loaded successfully. Store: ${currentStore?.name}, Items: ${storeItems.size}, Reviews: ${storeReviews.size}")
        } catch (e: Exception) {
            Log.e("StoreProfileScreen", "Error loading store data: ${e.message}", e)
            showErrorMessage = "Failed to load store data. Please try again."
        } finally {
            isLoading = false
        }
    } else {
        Log.w("StoreProfileScreen", "No store ID provided")
        showErrorMessage = "Store ID is missing. Please try again."
        isLoading = false
    }
}

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentStore?.name ?: "Store Details") },
                navigationIcon = {
                    IconButton(onClick = {
                        Log.d("StoreProfileScreen", "Back button clicked")
                        onBackClick()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            currentStore?.location?.let { location ->
                                streetViewLocation = LatLng(location.latitude, location.longitude)
                                showStreetView = true
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Street View"
                        )
                    }
                }
            )
        }
    ) { padding ->
        when {
            isLoading -> LoadingView()
            showErrorMessage != null -> ErrorView(showErrorMessage!!)
            currentStore == null -> StoreNotFoundView()
            else -> {
                val store = currentStore
                if (store != null) {
                    StoreContent(
                        currentStore = store,
                        storeItems = storeItems,
                        storeReviews = storeReviews,
                        isOwner = isOwner,
                        onNavigateToItemDetail = { itemId ->
                            Log.d("StoreProfileScreen", "Navigating to item detail. ItemId: $itemId")
                            onNavigateToItemDetail(itemId)
                        },
                        padding = padding
                    )
                } else {
                    StoreNotFoundView()
                }
            }
        }
    }

    if (showErrorMessage != null) {
        ErrorDialog(
            errorMessage = showErrorMessage!!,
            onDismiss = { showErrorMessage = null }
        )
    }


    if (showAddItemDialog) {
        AddItemDialog(
            onDismiss = {
                Log.d("StoreProfileScreen", "Add Item Dialog dismissed")
                showAddItemDialog = false
            },
            onAddItem = { newItem ->
                Log.d("StoreProfileScreen", "Adding new item: ${newItem.name}")
                viewModel.createItem(newItem, currentStore?.id ?: "") { success ->
                    if (success) {
                        Log.d("StoreProfileScreen", "New item added successfully: ${newItem.name}")
                        showAddItemDialog = false
                    } else {
                        Log.e("StoreProfileScreen", "Failed to add new item: ${newItem.name}")
                        showErrorMessage = "Failed to add item. Please try again."
                    }
                }
            }
        )
    }

    if (showStreetView) {
        streetViewLocation?.let { location ->
            StreetViewDialog(
                location = location,
                onDismiss = { showStreetView = false }
            )
        }
    }
}

// Displays a dialog asking whether to open Google Maps Street View for the store's location
@Composable
fun StreetViewDialog(location: LatLng, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Open in Street View?") },
        text = { Text("This will open Google Maps in Street View mode.") },
        confirmButton = {
            Button(onClick = {
                val gmmIntentUri = Uri.parse("google.streetview:cbll=${location.latitude},${location.longitude}")
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                mapIntent.setPackage("com.google.android.apps.maps")
                try {
                    context.startActivity(mapIntent)
                } catch (e: Exception) {
                    Log.e("StoreProfileScreen", "Error opening Street View: ${e.message}", e)
                }
                onDismiss()
            }) {
                Text("Open Street View")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Displays a loading spinner and message when store data is being loaded
@Composable
fun LoadingView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Text("Loading store...", modifier = Modifier.padding(top = 8.dp))
        }
    }
}

// Displays an error message when store data fails to load
@Composable
fun ErrorView(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message)
    }
}

// Displays a message when the store is not found or fails to load
@Composable
fun StoreNotFoundView() {
    Log.w("StoreProfileScreen", "Store not found or failed to load")
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Store not found or failed to load.")
    }
}

// Displays the main content of the store profile including items and reviews
@Composable
fun StoreContent(
    currentStore: Store,
    storeItems: List<Item>,
    storeReviews: List<Review>,
    isOwner: Boolean,
    onNavigateToItemDetail: (String) -> Unit,
    padding: PaddingValues
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        item {
            Log.d("StoreProfileScreen", "Displaying store header")
            StoreHeader(currentStore.name, currentStore.description)
        }

        item {
            Log.d("StoreProfileScreen", "Displaying opening times")
            OpeningTimesDisplay(currentStore.openingTimes)
        }

        item {
            Log.d("StoreProfileScreen", "Displaying store location on map")
            StoreLocationMap(currentStore)
        }

        item {
            Log.d("StoreProfileScreen", "Displaying items section")
            SectionTitle("Items")
        }

        items(storeItems) { item ->
            Log.d("StoreProfileScreen", "Displaying item: ${item.name}, ID: ${item.id}")
            ItemCard(
                item = item,
                onItemClick = { clickedItem ->
                    Log.d("StoreProfileScreen", "Item clicked: ${clickedItem.name}, ID: ${clickedItem.id}")
                    if (clickedItem.id.isNotEmpty()) {
                        onNavigateToItemDetail(clickedItem.id)
                    } else {
                        Log.e("StoreProfileScreen", "Attempted to navigate with empty item ID for item: ${clickedItem.name}")
                        // You might want to show an error message to the user here
                    }
                }
            )
        }
        item {
            Log.d("StoreProfileScreen", "Displaying reviews section")
            SectionTitle("Reviews")
        }

        items(storeReviews) { review ->
            Log.d("StoreProfileScreen", "Displaying review: ${review.id}")
            ReviewCard(review)
        }
    }
}

// Displays the store's name and description
@Composable
fun StoreHeader(name: String?, description: String?) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = name ?: "Store",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = description ?: "",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

// Displays section titles such as "Items" and "Reviews"
@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(16.dp)
    )
}

// Displays the store's opening hours for each day of the week
@Composable
fun OpeningTimesDisplay(openingTimes: Map<String, OpeningHours>?) {
    if (openingTimes != null) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Opening Times", style = MaterialTheme.typography.titleMedium)
            openingTimes.forEach { (day, hours) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(day, modifier = Modifier.width(100.dp))
                    Text("${hours.open} - ${hours.close}")
                }
            }
        }
    }
}

// Displays the store's location on a Google Map with a marker
@Composable
fun StoreLocationMap(store: Store) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(store.location?.latitude ?: 0.0, store.location?.longitude ?: 0.0),
            15f
        )
    }

    LaunchedEffect(store.location) {
        store.location?.let { location ->
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(
                    LatLng(location.latitude, location.longitude),
                    15f
                ),
                durationMs = 1000
            )
        }
    }

    GoogleMap(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        cameraPositionState = cameraPositionState
    ) {
        store.location?.let { location ->
            Marker(
                state = MarkerState(position = LatLng(location.latitude, location.longitude)),
                title = store.name
            )
        }
    }
}

// Displays individual store reviews including the user's comment and rating
@Composable
fun ReviewCard(review: Review) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "User: ${review.userId}", style = MaterialTheme.typography.bodyMedium)
                Text(text = "Rating: ${review.rating}/5", style = MaterialTheme.typography.bodyMedium)
            }
            Text(text = review.comment, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp))
        }
    }
}
