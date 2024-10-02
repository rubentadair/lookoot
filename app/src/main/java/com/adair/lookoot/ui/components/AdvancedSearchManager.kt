package com.adair.lookoot.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.adair.lookoot.data.FirestoreRepository
import com.adair.lookoot.models.Item
import com.adair.lookoot.models.Store
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.math.*

enum class FilterOption {
    DateAdded, PriceLowToHigh, PriceHighToLow, Rating, MostReviews
}

class AdvancedSearchManager(private val repository: FirestoreRepository) {

    // StateFlows for search text, results, loading state, and error messages
    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Holds the current set of selected filters
    private val _currentFilters = MutableStateFlow<Set<FilterOption>>(emptySet())
    val currentFilters: StateFlow<Set<FilterOption>> = _currentFilters.asStateFlow()

    // Holds the selected categories for filtering
    private val _selectedCategories = MutableStateFlow<Set<String>>(emptySet())
    val selectedCategories: StateFlow<Set<String>> = _selectedCategories.asStateFlow()

    // Defines the search radius (in kilometers) for store proximity
    var radius by mutableStateOf(5f)

    // Holds the user's current location (default is set to Glasgow)
    var userLocation by mutableStateOf(LatLng(55.8642, -4.2518))

    // Job for debouncing search input
    private var searchJob: Job? = null

    // Coroutine scope for launching coroutines
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Defines the possible search results (either an Item or a Store)
    sealed class SearchResult {
        data class ItemResult(val item: Item) : SearchResult()
        data class StoreResult(val store: Store) : SearchResult()
    }

    // Updates the search text when the user types something new
    fun onSearchTextChange(text: String) {
        _searchText.value = text
        debounceSearch(text)
    }

    // Implements debouncing to delay search execution until the user stops typing
    private fun debounceSearch(query: String) {
        searchJob?.cancel()
        searchJob = coroutineScope.launch {
            delay(300) // Wait for 300ms before performing the search
            searchStoresAndItems(query)
        }
    }

    // Executes a search for both stores and items based on the query and filters the results
    suspend fun searchStoresAndItems(query: String) {
        _isLoading.value = true
        _errorMessage.value = null

        try {
            // Fetch items and stores using repository methods
            val itemResultsDeferred = coroutineScope.async(Dispatchers.IO) {
                repository.searchItems(query)
            }

            val storeResultsDeferred = coroutineScope.async(Dispatchers.IO) {
                repository.searchStores(query)
            }

            val itemResults = itemResultsDeferred.await()
            val storeResults = storeResultsDeferred.await()

            // Combine and filter results
            val combinedResults = (itemResults.map { SearchResult.ItemResult(it) } +
                    storeResults.map { SearchResult.StoreResult(it) })
                .filter { result ->
                    // Apply category filters
                    _selectedCategories.value.isEmpty() || when (result) {
                        is SearchResult.ItemResult -> result.item.categories.any { it in _selectedCategories.value }
                        is SearchResult.StoreResult -> result.store.categories.any { it in _selectedCategories.value }
                    }
                }

            // Apply sorting filters
            _searchResults.value = applyFilters(combinedResults)
        } catch (e: Exception) {
            _errorMessage.value = "An error occurred: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    // Applies the currently selected filters to the search results
    private fun applyFilters(results: List<SearchResult>): List<SearchResult> {
        var filteredResults = results

        // Apply sorting filters
        _currentFilters.value.forEach { filterOption ->
            filteredResults = when (filterOption) {
                FilterOption.DateAdded -> filteredResults.sortedByDescending {
                    when (it) {
                        is SearchResult.ItemResult -> it.item.createdAt
                        is SearchResult.StoreResult -> it.store.createdAt
                    }
                }
                FilterOption.PriceLowToHigh -> filteredResults.sortedBy {
                    when (it) {
                        is SearchResult.ItemResult -> it.item.price ?: Double.MAX_VALUE
                        is SearchResult.StoreResult -> Double.MAX_VALUE // Stores don't have a price
                    }
                }
                FilterOption.PriceHighToLow -> filteredResults.sortedByDescending {
                    when (it) {
                        is SearchResult.ItemResult -> it.item.price ?: Double.MIN_VALUE
                        is SearchResult.StoreResult -> Double.MIN_VALUE
                    }
                }

                FilterOption.Rating -> TODO()
                FilterOption.MostReviews -> TODO()
            }
        }

        return filteredResults
    }

    // Updates the selected filter options
    fun updateFilters(filterOptions: Set<FilterOption>) {
        _currentFilters.value = filterOptions
        // Re-apply filters to the current results
        _searchResults.value = applyFilters(_searchResults.value)
    }

    // Updates the selected categories
    fun updateCategories(categories: Set<String>) {
        _selectedCategories.value = categories
        // Re-execute the search with updated categories
        coroutineScope.launch {
            searchStoresAndItems(_searchText.value)
        }
    }

    // Updates the search radius for filtering stores by proximity
    fun updateRadius(newRadius: Float) {
        radius = newRadius
        // Re-execute the search with updated radius
        coroutineScope.launch {
            searchStoresAndItems(_searchText.value)
        }
    }

    // Cleans up resources when the manager is no longer needed
    fun onCleared() {
        coroutineScope.cancel()
    }
}
