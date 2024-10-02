package com.adair.lookoot.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adair.lookoot.models.Item
import com.adair.lookoot.models.OpeningHours
import com.adair.lookoot.models.Store
import com.adair.lookoot.ui.components.AddItemDialog
import com.adair.lookoot.viewmodels.LookootViewModel
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.firebase.firestore.GeoPoint
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale

// Main screen for editing store details, location, categories, tags, and items
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StoreEditScreen(
    storeId: String,
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
    onNavigateToItemEdit: (String) -> Unit,
    onStoreDeleted: () -> Unit,
    viewModel: LookootViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val store by viewModel.currentStore.collectAsState()
    val items by viewModel.storeItems.collectAsState()
    var addressSearchQuery by remember { mutableStateOf("") }
    var itemSearchQuery by remember { mutableStateOf("") }
    var editedStore by remember { mutableStateOf(store) }
    var showAddItemDialog by remember { mutableStateOf(false) }
    var showErrorSnackbar by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var storeLocation by remember { mutableStateOf<LatLng?>(null) }
    var searchPredictions by remember { mutableStateOf<List<String>>(emptyList()) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var showCategorySuggestions by remember { mutableStateOf(false) }
    var newCategory by remember { mutableStateOf("") }
    var newTag by remember { mutableStateOf("") }
    var showTagSuggestions by remember { mutableStateOf(false) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(55.8642, -4.2518), 10f)
    }
    val placesClient = remember {
        val applicationInfo = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
        val apiKey = applicationInfo.metaData.getString("com.google.android.geo.API_KEY")
        Places.initialize(context, apiKey ?: "")
        Places.createClient(context)
    }

    LaunchedEffect(storeId) {
        viewModel.loadStore(storeId)
        viewModel.loadStoreItems(storeId)
    }

    LaunchedEffect(store) {
        editedStore = store
        store?.location?.let { geoPoint ->
            storeLocation = LatLng(geoPoint.latitude, geoPoint.longitude)
            cameraPositionState.position = CameraPosition.fromLatLngZoom(storeLocation!!, 15f)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit ${store?.name ?: "Store"}") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        editedStore?.let {
                            viewModel.updateStore(it) { success ->
                                if (success) onSaveClick()
                                else {
                                    showErrorSnackbar = true
                                    errorMessage = "Failed to save store changes"
                                }
                            }
                        }
                    }) {
                        Icon(Icons.Filled.Check, contentDescription = "Save Changes")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                // Store edit fields
                StoreEditFields(
                    store = editedStore,
                    onStoreChange = { updatedStore -> editedStore = updatedStore }
                )

                // Location search and map
                Text(
                    "Search for Store Location",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
                OutlinedTextField(
                    value = addressSearchQuery,
                    onValueChange = { newQuery ->
                        addressSearchQuery = newQuery
                        searchError = null
                        coroutineScope.launch {
                            performAutocompletePredictions(
                                placesClient,
                                newQuery,
                                { newPredictions -> searchPredictions = newPredictions },
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
                            storeLocation = geocodeAddress(context, addressSearchQuery)
                            storeLocation?.let { location ->
                                cameraPositionState.position =
                                    CameraPosition.fromLatLngZoom(location, 15f)
                                editedStore = editedStore?.copy(
                                    location = GeoPoint(
                                        location.latitude,
                                        location.longitude
                                    )
                                )
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

                if (searchPredictions.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    ) {
                        items(searchPredictions) { prediction ->
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
                                                editedStore = editedStore?.copy(
                                                    location = GeoPoint(
                                                        location.latitude,
                                                        location.longitude
                                                    )
                                                )
                                            }
                                            addressSearchQuery = prediction
                                            searchPredictions = emptyList()
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
                        .aspectRatio(1f),
                    cameraPositionState = cameraPositionState,
                    onMapClick = { latLng ->
                        storeLocation = latLng
                        editedStore = editedStore?.copy(
                            location = GeoPoint(
                                latLng.latitude,
                                latLng.longitude
                            )
                        )
                        coroutineScope.launch {
                            addressSearchQuery = reverseGeocode(context, latLng) ?: ""
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

                // Categories input
                Text(
                    "Categories",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newCategory,
                        onValueChange = {
                            newCategory = it.trim().replace(Regex("\\s+"), " ")
                            showCategorySuggestions = newCategory.isNotEmpty()
                        },
                        label = { Text("Add Category") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (newCategory.isNotEmpty() && newCategory !in (editedStore?.categories ?: emptyList())) {
                                editedStore = editedStore?.copy(
                                    categories = (editedStore?.categories ?: emptyList()) + newCategory
                                )
                                newCategory = ""
                                showCategorySuggestions = false
                            }
                        })
                    )
                    IconButton(onClick = {
                        if (newCategory.isNotEmpty() && newCategory !in (editedStore?.categories ?: emptyList())) {
                            editedStore = editedStore?.copy(
                                categories = (editedStore?.categories ?: emptyList()) + newCategory
                            )
                            newCategory = ""
                            showCategorySuggestions = false
                        }
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Category")
                    }
                }

                if (showCategorySuggestions) {
                    val allCategories = listOf(
                        "Tools", "Food", "Garden", "Electronics", "Home", "Clothing", "Toys",
                        "Books", "Beauty", "Sports", "Automotive", "Health", "Office Supplies",
                        "Music", "Pet Supplies", "Jewelry", "Furniture", "Outdoor", "Baby",
                        "Movies", "Video Games", "Groceries", "Bags & Accessories", "Art & Crafts"
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    ) {
                        items(allCategories.filter { it.contains(newCategory, ignoreCase = true) }) { category ->
                            Text(
                                text = category,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (category !in (editedStore?.categories ?: emptyList())) {
                                            editedStore = editedStore?.copy(
                                                categories = (editedStore?.categories ?: emptyList()) + category
                                            )
                                            newCategory = ""
                                            showCategorySuggestions = false
                                        }
                                    }
                                    .padding(vertical = 8.dp)
                            )
                        }
                    }
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    editedStore?.categories?.forEach { category ->
                        FilterChip(
                            selected = true,
                            onClick = {
                                editedStore = editedStore?.copy(
                                    categories = editedStore?.categories?.filter { it != category } ?: emptyList()
                                )
                            },
                            label = { Text(category) },
                            trailingIcon = {
                                Icon(Icons.Default.Close, contentDescription = "Remove category")
                            }
                        )
                    }
                }

                // Tags input with suggestions
                Text(
                    "Tags",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newTag,
                        onValueChange = {
                            newTag = it.trim().replace(Regex("\\s+"), " ")
                            showTagSuggestions = newTag.isNotEmpty()
                        },
                        label = { Text("Add Tag") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (newTag.isNotEmpty() && newTag !in (editedStore?.tags ?: emptyList())) {
                                editedStore = editedStore?.copy(
                                    tags = (editedStore?.tags ?: emptyList()) + newTag
                                )
                                newTag = ""
                                showTagSuggestions = false
                            }
                        })
                    )
                    IconButton(onClick = {
                        if (newTag.isNotEmpty() && newTag !in (editedStore?.tags ?: emptyList())) {
                            editedStore = editedStore?.copy(
                                tags = (editedStore?.tags ?: emptyList()) + newTag
                            )
                            newTag = ""
                            showTagSuggestions = false
                        }
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Tag")
                    }
                }

                if (showTagSuggestions) {
                    val allTags = listOf(
                        "Sale", "Discount", "Limited Edition", "New Arrival", "Popular", "Seasonal",
                        "Exclusive", "Premium", "Eco-friendly", "Handmade", "Vintage"
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    ) {
                        items(allTags.filter { it.contains(newTag, ignoreCase = true) }) { tag ->
                            Text(
                                text = tag,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (tag !in (editedStore?.tags ?: emptyList())) {
                                            editedStore = editedStore?.copy(
                                                tags = (editedStore?.tags ?: emptyList()) + tag
                                            )
                                            newTag = ""
                                            showTagSuggestions = false
                                        }
                                    }
                                    .padding(vertical = 8.dp)
                            )
                        }
                    }
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    editedStore?.tags?.forEach { tag ->
                        FilterChip(
                            selected = true,
                            onClick = {
                                editedStore = editedStore?.copy(
                                    tags = editedStore?.tags?.filter { it != tag } ?: emptyList()
                                )
                            },
                            label = { Text(tag) },
                            trailingIcon = {
                                Icon(Icons.Default.Close, contentDescription = "Remove tag")
                            }
                        )
                    }
                }

                // Opening times input
                Text(
                    "Opening Times",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 16.dp)
                )
                OpeningTimesInput(
                    openingTimes = editedStore?.openingTimes ?: emptyMap(),
                    onOpeningTimesChanged = { editedStore = editedStore?.copy(openingTimes = it) }
                )

                // Delete store button
                Button(
                    onClick = { showDeleteConfirmDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Delete Store")
                }
            }

            item {
                // Search bar for items
                SearchBar(
                    query = itemSearchQuery,
                    onQueryChange = { itemSearchQuery = it },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            item {
                // Add new item button
                Button(
                    onClick = { showAddItemDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add New Item")
                }
            }
            items(
                items = items.filter { item ->
                    item.name.contains(itemSearchQuery, ignoreCase = true) ||
                            item.description.contains(itemSearchQuery, ignoreCase = true)
                }
            ) { item ->
                EditableItemCard(
                    item = item,
                    onDelete = {
                        viewModel.deleteItem(item.id) { success ->
                            if (success) {
                                Log.d("StoreEditScreen", "Item deleted successfully: ${item.name}")
                            } else {
                                Log.e("StoreEditScreen", "Failed to delete item: ${item.name}")
                                showErrorSnackbar = true
                                errorMessage = "Failed to delete item"
                            }
                        }
                    },
                    onClick = { onNavigateToItemEdit(item.id) }
                )
            }
        }
    }

    // Add Item Dialog
    if (showAddItemDialog) {
        AddItemDialog(
            onDismiss = { showAddItemDialog = false },
            onAddItem = { newItem ->
                viewModel.createItem(newItem, storeId) { success ->
                    if (success) showAddItemDialog = false
                    else {
                        showErrorSnackbar = true
                        errorMessage = "Failed to add new item"
                    }
                }
            }
        )
    }

    // Error Snackbar
    if (showErrorSnackbar) {
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                TextButton(onClick = { showErrorSnackbar = false }) {
                    Text("Dismiss")
                }
            }
        ) {
            Text(errorMessage)
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Store") },
            text = { Text("Are you sure you want to delete this store? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCurrentStore { success ->
                            if (success) {
                                onStoreDeleted()
                            } else {
                                showErrorSnackbar = true
                                errorMessage = "Failed to delete store"
                            }
                            showDeleteConfirmDialog = false
                        }
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
// UI for editing store name and description
@Composable
fun StoreEditFields(
    store: Store?,
    onStoreChange: (Store) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        store?.let { currentStore ->
            OutlinedTextField(
                value = currentStore.name,
                onValueChange = { onStoreChange(currentStore.copy(name = it)) },
                label = { Text("Store Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = currentStore.description,
                onValueChange = { onStoreChange(currentStore.copy(description = it)) },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
// UI for displaying editable items with an option to delete
@Composable
fun EditableItemCard(
    item: Item,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "£%.2f".format(item.price),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(4.dp))

            if (expanded) {
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.clickable { expanded = false } // Collapse when clicked
                )
            } else {
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2, // Limit to 2 lines
                    overflow = TextOverflow.Ellipsis, // Show ellipsis at the end
                    modifier = Modifier.clickable { expanded = true } // Expand when clicked
                )

                Text(
                    text = "Read more",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.clickable { expanded = true } // Expand when clicked
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDelete) {
                    Text("Delete")
                }
            }
        }
    }
}
// Search bar UI for filtering items in the store
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("Search items...") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        singleLine = true
    )
}
// Generates a list of time options in HH:MM format for opening hours
@SuppressLint("DefaultLocale")
fun generateTimeOptions(): List<String> {
    val timeOptions = mutableListOf<String>()
    for (hour in 0..23) {
        for (minute in listOf(0, 30)) {
            val time = String.format("%02d:%02d", hour, minute)
            timeOptions.add(time)
        }
    }
    return timeOptions
}
// Fetches autocomplete predictions for addresses from Google Places API
suspend fun performAutocompletePredictions(placesClient: PlacesClient, query: String, callback: (List<String>) -> Unit, onError: (String) -> Unit) {
    val token = AutocompleteSessionToken.newInstance()
    val request = FindAutocompletePredictionsRequest.builder()
        .setSessionToken(token)
        .setQuery(query)
        .build()

    placesClient.findAutocompletePredictions(request).addOnSuccessListener { response ->
        val predictions = response.autocompletePredictions.map { it.getFullText(null).toString() }
        callback(predictions)
    }.addOnFailureListener { exception ->
        Log.e("PlacesAutocomplete", "Error fetching autocomplete predictions: ", exception)
        when (exception) {
            is ApiException -> {
                when (exception.statusCode) {
                    CommonStatusCodes.API_NOT_CONNECTED -> onError("The API is not connected. Please check your internet connection.")
                    CommonStatusCodes.NETWORK_ERROR -> onError("A network error occurred. Please check your internet connection.")
                    CommonStatusCodes.ERROR -> onError("An error occurred. Please try again.")
                    else -> onError("An unexpected error occurred (${exception.statusCode}). Please try again.")
                }
            }
            else -> onError("An unexpected error occurred. Please try again.")
        }
        callback(emptyList())
    }
}
// Converts an address string into a LatLng object using the Geocoder
suspend fun geocodeAddress(context: Context, address: String): LatLng? = withContext(Dispatchers.IO) {
    val geocoder = Geocoder(context, Locale.getDefault())
    try {
        val results = geocoder.getFromLocationName(address, 1)
        if (!results.isNullOrEmpty()) {
            val location = results[0]
            return@withContext LatLng(location.latitude, location.longitude)
        }
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return@withContext null
}
// Converts a LatLng object into an address string using the Geocoder
suspend fun reverseGeocode(context: Context, latLng: LatLng): String? = withContext(Dispatchers.IO) {
    val geocoder = Geocoder(context, Locale.getDefault())
    try {
        val results = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
        if (!results.isNullOrEmpty()) {
            return@withContext results[0].getAddressLine(0)
        }
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return@withContext null
}
// UI for setting and editing store opening hours
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpeningTimesInput(
    openingTimes: Map<String, OpeningHours>,
    onOpeningTimesChanged: (Map<String, OpeningHours>) -> Unit
) {
    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    val timeOptions = generateTimeOptions()

    Column {
        days.forEach { day ->
            var open by remember { mutableStateOf(openingTimes[day]?.open ?: "") }
            var close by remember { mutableStateOf(openingTimes[day]?.close ?: "") }
            var openExpanded by remember { mutableStateOf(false) }
            var closeExpanded by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(day, modifier = Modifier.width(80.dp))

                // Open time dropdown
                ExposedDropdownMenuBox(
                    expanded = openExpanded,
                    onExpandedChange = { openExpanded = !openExpanded }
                ) {
                    OutlinedTextField(
                        value = open,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Open") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = openExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .width(120.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = openExpanded,
                        onDismissRequest = { openExpanded = false }
                    ) {
                        timeOptions.forEach { time ->
                            DropdownMenuItem(
                                text = { Text(time) },
                                onClick = {
                                    open = time
                                    openExpanded = false
                                    onOpeningTimesChanged(openingTimes + (day to OpeningHours(open, close)))
                                }
                            )
                        }
                    }
                }

                // Close time dropdown
                ExposedDropdownMenuBox(
                    expanded = closeExpanded,
                    onExpandedChange = { closeExpanded = !closeExpanded }
                ) {
                    OutlinedTextField(
                        value = close,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Close") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = closeExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .width(120.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = closeExpanded,
                        onDismissRequest = { closeExpanded = false }
                    ) {
                        timeOptions.forEach { time ->
                            DropdownMenuItem(
                                text = { Text(time) },
                                onClick = {
                                    close = time
                                    closeExpanded = false
                                    onOpeningTimesChanged(openingTimes + (day to OpeningHours(open, close)))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
