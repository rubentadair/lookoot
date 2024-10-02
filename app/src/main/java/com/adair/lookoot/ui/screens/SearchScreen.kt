package com.adair.lookoot.ui.screens

import android.annotation.SuppressLint
import android.os.Build
import android.content.Context
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.adair.lookoot.models.Store
import com.adair.lookoot.ui.components.ItemCard
import com.adair.lookoot.ui.components.StoreCard
import com.adair.lookoot.ui.components.AdvancedSearchManager
import com.adair.lookoot.ui.components.FilterOption
import com.adair.lookoot.viewmodels.LookootViewModel
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapEffect
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.Circle

// Main screen for searching stores and items with options to toggle between list and map views.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateToStoreProfile: (String) -> Unit,
    onNavigateToItemDetailScreen: (String) -> Unit,
    viewModel: LookootViewModel = viewModel(),
    navController: NavController
) {
    val searchManager = viewModel.searchManager
    val searchText by searchManager.searchText.collectAsState()
    val isSearching by remember { mutableStateOf(false) }
    val searchResults by searchManager.searchResults.collectAsState()
    val stores by viewModel.stores.collectAsState()
    var showFilters by remember { mutableStateOf(false) }
    var showMapView by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var searchRadius by remember { mutableStateOf(searchManager.radius) }

    val scope = rememberCoroutineScope()
    var searchJob: Job? = null

    LaunchedEffect(Unit) {
        viewModel.loadAllStores()
        viewModel.loadAllItems()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search") },
                actions = {
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filters")
                    }
                    IconButton(onClick = { showMapView = !showMapView }) {
                        Icon(
                            if (showMapView) Icons.Default.List else Icons.Default.Map,
                            contentDescription = if (showMapView) "List View" else "Map View"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            SearchBar(
                query = searchText,
                onQueryChange = {
                    scope.launch {
                        searchManager.onSearchTextChange(it)
                    }
                },
                onSearch = {
                    scope.launch {
                        searchManager.onSearchTextChange(it)
                        searchManager.searchStoresAndItems(it)
                    }
                },
                active = isSearching,
                onActiveChange = { /* Implement onToggleSearch if available */ },
                placeholder = { Text("Search stores and items") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchText.isNotEmpty()) {
                        IconButton(onClick = {
                            scope.launch {
                                searchManager.onSearchTextChange("")
                            }
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {}

            AnimatedVisibility(visible = showFilters) {
                FilterOptions(
                    onFilterChange = { filter ->
                        searchManager.updateFilter(filter)
                        scope.launch {
                            searchManager.searchStoresAndItems(searchText)
                        }
                    }
                )
            }

            if (!isSearching && !showMapView) {
                CategoryAndNavigationSection(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { category ->
                        selectedCategory = category
                        scope.launch {
                            searchManager.searchStoresAndItems(category)
                        }
                    }
                )
            }

            if (showMapView) {
                GoogleMapView(
                    stores = searchManager.getFilteredStores(stores),
                    onNavigateToStoreProfile = onNavigateToStoreProfile,
                    userLocation = searchManager.userLocation,
                    radius = searchRadius,
                    searchResults = searchResults,
                    onRadiusChange = { newRadius ->
                        searchRadius = newRadius
                        scope.launch {
                            searchManager.updateRadius(newRadius)
                            searchJob?.cancel()
                            searchJob = launch {
                                delay(300) // Wait for 300ms before performing the search
                                searchManager.searchStoresAndItems(searchText)
                            }
                        }
                    }
                )
            } else {
                SearchResultsList(
                    searchResults = searchResults,
                    onNavigateToStoreProfile = onNavigateToStoreProfile,
                    onNavigateToItemDetailScreen = onNavigateToItemDetailScreen
                )
            }
        }
    }
}
// Displays a Google Map with store clusters and handles cluster item interactions.
@SuppressLint("PotentialBehaviorOverride")
@OptIn(MapsComposeExperimentalApi::class)
@Composable
fun GoogleMapView(
    stores: List<Store>,
    onNavigateToStoreProfile: (String) -> Unit,
    userLocation: LatLng,
    radius: Float,
    searchResults: List<AdvancedSearchManager.SearchResult>,
    onRadiusChange: (Float) -> Unit
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(userLocation, 12f)
    }

    val mapUiSettings by remember { mutableStateOf(MapUiSettings(mapToolbarEnabled = false)) }
    val mapProperties by remember { mutableStateOf(MapProperties(isMyLocationEnabled = true)) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var clusterManager by remember { mutableStateOf<ClusterManager<StoreClusterItem>?>(null) }
    var clusterRenderer by remember { mutableStateOf<CustomClusterRenderer?>(null) }

    Column {
        Slider(
            value = radius,
            onValueChange = { newRadius ->
                onRadiusChange(newRadius)
            },
            valueRange = 1f..20f,
            steps = 19,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
        Text(
            text = "Search Radius: ${radius.toInt()} km",
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        GoogleMap(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = mapUiSettings,
            onMapLoaded = {
                // Perform actions when the map is fully loaded
            }
        ) {
            Circle(
                center = userLocation,
                radius = radius * 1000.0,
                strokeColor = MaterialTheme.colorScheme.primary,
                fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            )

            MapEffect(stores) { map ->
                if (clusterManager == null) {
                    clusterManager = ClusterManager<StoreClusterItem>(context, map)
                    clusterRenderer = CustomClusterRenderer(context, map, clusterManager!!)
                    clusterManager?.renderer = clusterRenderer
                    clusterManager?.setOnClusterItemInfoWindowClickListener { item ->
                        onNavigateToStoreProfile(item.store.id)
                    }
                }

                val relevantStores = stores.filter { store ->
                    searchResults.any { result ->
                        when (result) {
                            is AdvancedSearchManager.SearchResult.StoreResult -> result.store.id == store.id
                            is AdvancedSearchManager.SearchResult.ItemResult -> result.item.storeId == store.id
                        }
                    }
                }

                clusterManager?.clearItems()
                relevantStores.forEach { store ->
                    store.location?.let { geoPoint ->
                        val position = LatLng(geoPoint.latitude, geoPoint.longitude)
                        val clusterItem = StoreClusterItem(
                            store = store,
                            latLng = position,
                            title = store.name,
                            snippet = store.description
                        )
                        clusterManager?.addItem(clusterItem)
                    }
                }

                scope.launch {
                    clusterManager?.cluster()
                }

                // Set up the info window click listener
                map.setOnInfoWindowClickListener { marker ->
                    marker.tag?.let { storeId ->
                        onNavigateToStoreProfile(storeId.toString())
                    }
                }
            }

            MapEffect(Unit) { map ->
                clusterManager?.let { manager ->
                    map.setOnCameraIdleListener(manager)
                    map.setOnMarkerClickListener(manager)
                    map.setOnInfoWindowClickListener(manager)
                }
            }
        }
    }
}
// Custom cluster renderer for showing store markers on the map.
data class StoreClusterItem(
    val store: Store,
    private val latLng: LatLng,
    private val title: String,
    private val snippet: String
) : ClusterItem {
    override fun getPosition(): LatLng = latLng
    override fun getTitle(): String = title
    override fun getSnippet(): String = snippet
    override fun getZIndex(): Float? = null
}
class CustomClusterRenderer(
    private val context: Context,
    map: GoogleMap,
    clusterManager: ClusterManager<StoreClusterItem>
) : DefaultClusterRenderer<StoreClusterItem>(context, map, clusterManager) {
    override fun onBeforeClusterItemRendered(item: StoreClusterItem, markerOptions: MarkerOptions) {
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
        markerOptions.title(item.title)
        markerOptions.snippet(item.snippet)
    }

    override fun onClusterItemRendered(clusterItem: StoreClusterItem, marker: Marker) {
        super.onClusterItemRendered(clusterItem, marker)
        marker.tag = clusterItem.store.id
    }
}
// Displays search results in a list format (stores and items).
@Composable
fun SearchResultsList(
    searchResults: List<AdvancedSearchManager.SearchResult>,
    onNavigateToStoreProfile: (String) -> Unit,
    onNavigateToItemDetailScreen: (String) -> Unit
) {
    LazyColumn {
        items(searchResults) { result ->
            when (result) {
                is AdvancedSearchManager.SearchResult.StoreResult -> StoreCard(
                    store = result.store,
                    onStoreClick = { onNavigateToStoreProfile(result.store.id) }
                )
                is AdvancedSearchManager.SearchResult.ItemResult -> ItemCard(
                    item = result.item,
                    onItemClick = { onNavigateToItemDetailScreen(result.item.id) }
                )
            }
        }
    }
}
// Displays a list of categories for filtering the search results.
@Composable
fun CategoryAndNavigationSection(
    selectedCategory: String?,
    onCategorySelected: (String) -> Unit
) {
    val categories = listOf("Clothing", "Electronics", "Books", "Home & Garden", "Sports", "Beauty")

    Column {
        Text("Categories", style = MaterialTheme.typography.headlineSmall)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(categories) { category ->
                FilterChip(
                    selected = category == selectedCategory,
                    onClick = { onCategorySelected(category) },
                    label = { Text(category) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Recent Searches", style = MaterialTheme.typography.headlineSmall)
        // Add recent searches list here
    }
}
// Displays filter options like date added, price, rating, and reviews.
@Composable
fun FilterOptions(onFilterChange: (FilterOption) -> Unit) {
    Column {
        FilterChip(
            selected = false,
            onClick = { onFilterChange(FilterOption.DateAdded) },
            label = { Text("Date Added") }
        )
        FilterChip(
            selected = false,
            onClick = { onFilterChange(FilterOption.PriceLowToHigh) },
            label = { Text("Price: Low to High") }
        )
        FilterChip(
            selected = false,
            onClick = { onFilterChange(FilterOption.PriceHighToLow) },
            label = { Text("Price: High to Low") }
        )
        FilterChip(
            selected = false,
            onClick = { onFilterChange(FilterOption.Rating) },
            label = { Text("Rating") }
        )
        FilterChip(
            selected = false,
            onClick = { onFilterChange(FilterOption.MostReviews) },
            label = { Text("Most Reviews") }
        )
    }
}