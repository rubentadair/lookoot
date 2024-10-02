package com.adair.lookoot.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adair.lookoot.data.FirestoreRepository
import com.adair.lookoot.models.HistoryItem
import com.adair.lookoot.models.Item
import com.adair.lookoot.models.Review
import com.adair.lookoot.models.Store
import com.adair.lookoot.models.User
import com.adair.lookoot.ui.components.AdvancedSearchManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LookootViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val repository = FirestoreRepository()
    val searchManager = AdvancedSearchManager(repository)

    private val _currentStore = MutableStateFlow<Store?>(null)
    val currentStore = _currentStore.asStateFlow()

    private val _storeItems = MutableStateFlow<List<Item>>(emptyList())
    val storeItems = _storeItems.asStateFlow()

    private val _storeReviews = MutableStateFlow<List<Review>>(emptyList())
    val storeReviews = _storeReviews.asStateFlow()

    private val _itemReviews = MutableStateFlow<List<Review>>(emptyList())
    val itemReviews: StateFlow<List<Review>> = _itemReviews.asStateFlow()

    private val _userStores = MutableStateFlow<List<Store>>(emptyList())
    val userStores: StateFlow<List<Store>> = _userStores.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _stores = MutableStateFlow<List<Store>>(emptyList())
    val stores: StateFlow<List<Store>> = _stores.asStateFlow()

    private val _items = MutableStateFlow<List<Item>>(emptyList())
    val items: StateFlow<List<Item>> = _items.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn = _isLoggedIn.asStateFlow()

    private val _currentItem = MutableStateFlow<Item?>(null)
    val currentItem: StateFlow<Item?> = _currentItem.asStateFlow()

    private val _followers = MutableStateFlow<List<User>>(emptyList())
    val followers: StateFlow<List<User>> = _followers.asStateFlow()

    private val _wishlistItems = MutableStateFlow<List<Item>>(emptyList())
    val wishlistItems: StateFlow<List<Item>> = _wishlistItems.asStateFlow()

    private val _userHistory = MutableStateFlow<List<HistoryItem>>(emptyList())
    val userHistory: StateFlow<List<HistoryItem>> = _userHistory.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Any>>(emptyList())
    val searchResults: StateFlow<List<Any>> = _searchResults.asStateFlow()

    // Initializes the ViewModel by checking if a user is logged in and fetching their data.
    init {
        viewModelScope.launch {
            auth.currentUser?.let { firebaseUser ->
                val user = repository.getUser(firebaseUser.uid)
                _currentUser.value = user
                Log.d("LookootViewModel", "Current user set: ${user?.username}")
            }
        }
    }

    // Returns the current user's ID from Firebase Authentication, or null if not logged in.
    val currentUserId: String?
        get() = FirebaseAuth.getInstance().currentUser?.uid

    // Registers a new store in Firestore and returns the store ID.
    suspend fun registerStore(store: Store): String {
        return repository.createStore(store)
    }

    // Loads all stores from Firestore and updates the state.
    fun loadAllStores() {
        viewModelScope.launch {
            try {
                Log.d("LookootViewModel", "Loading all stores")
                val allStores = repository.getAllStores()
                allStores.forEach { store ->
                    Log.d("LookootViewModel", "Loaded store: ${store.name}, ID: ${store.id}")
                }
                _stores.value = allStores
                Log.d("LookootViewModel", "Loaded ${allStores.size} stores")
            } catch (e: Exception) {
                Log.e("LookootViewModel", "Error loading all stores", e)
            }
        }
    }

    // Loads all items from Firestore and updates the state.
    fun loadAllItems() {
        viewModelScope.launch {
            try {
                Log.d("LookootViewModel", "Loading all items")
                val allItems = repository.getAllItems()
                allItems.forEach { item ->
                    Log.d("LookootViewModel", "Loaded item: ${item.name}, ID: ${item.id}")
                }
                _items.value = allItems
                Log.d("LookootViewModel", "Loaded ${allItems.size} items")
            } catch (e: Exception) {
                Log.e("LookootViewModel", "Error loading all items", e)
            }
        }
    }

    // Fetches the current user's details from Firestore based on their Firebase Auth ID.
    private suspend fun getCurrentUserFromFirebase(): User? {
        val firebaseUser = auth.currentUser
        return firebaseUser?.let {
            val user = repository.getUser(it.uid)
            Log.d("LookootViewModel", "Fetched user: ${user?.username}, Role: ${user?.role}")
            user
        }
    }

    // Loads stores associated with a particular user and updates the state.
    fun loadUserStores(userId: String) {
        viewModelScope.launch {
            Log.d("LookootViewModel", "Attempting to load stores for user: $userId")
            if (userId.isEmpty()) {
                Log.d("LookootViewModel", "Cannot load stores: User ID is empty")
                _userStores.value = emptyList()
                return@launch
            }
            try {
                val stores = repository.getStoresForUser(userId)
                Log.d("LookootViewModel", "Loaded ${stores.size} stores for user $userId")
                _userStores.value = stores
            } catch (e: Exception) {
                Log.e("LookootViewModel", "Error loading user stores", e)
                _userStores.value = emptyList()
            }
        }
    }

    // Fetches a specific store by its ID from Firestore.
    suspend fun getStore(storeId: String): Store? {
        return withContext(Dispatchers.IO) {
            try {
                repository.getStore(storeId)
            } catch (e: Exception) {
                Log.e("LookootViewModel", "Error fetching store: ${e.message}", e)
                null
            }
        }
    }

    // Sets the current user in the ViewModel and loads their stores.
    fun setCurrentUser(user: User?) {
        Log.d("LookootViewModel", "Setting current user: ${user?.id ?: "null"}")
        _currentUser.value = user
        if (user != null) {
            loadUserStores(user.id)
            _isLoggedIn.value = true
        } else {
            clearCurrentUser()
        }
    }

    // Fetches a user by their ID from Firestore.
    suspend fun getUser(userId: String): User? {
        return repository.getUser(userId)
    }

    // Fetches an item by its ID from Firestore.
    suspend fun getItem(itemId: String): Item? {
        return repository.getItem(itemId)
    }

    // Fetches reviews for a specific item and updates the state.
    fun getItemReviews(itemId: String) {
        viewModelScope.launch {
            try {
                repository.getReviewsForItem(itemId).collect { reviews ->
                    _itemReviews.value = reviews
                }
            } catch (e: Exception) {
                Log.e("LookootViewModel", "Error getting item reviews", e)
            }
        }
    }

    // Loads a specific store by its ID and updates the current store state.
    fun loadStore(storeId: String) {
        viewModelScope.launch {
            try {
                _currentStore.value = null // Clear current store before loading
                val store = repository.getStore(storeId)
                _currentStore.value = store
            } catch (e: Exception) {
                Log.e("LookootViewModel", "Error loading store", e)
            }
        }
    }

    // Loads all items associated with a particular store and updates the state.
    fun loadStoreItems(storeId: String) {
        viewModelScope.launch {
            try {
                _storeItems.value = emptyList() // Clear items before loading
                repository.getItemsForStore(storeId).collect { items ->
                    _storeItems.value = items
                }
            } catch (e: Exception) {
                Log.e("LookootViewModel", "Error loading store items", e)
            }
        }
    }

    // Loads all reviews for a specific store and updates the state.
    fun loadStoreReviews(storeId: String) {
        viewModelScope.launch {
            try {
                _storeReviews.value = emptyList() // Clear reviews before loading
                repository.getReviewsForStore(storeId).collect { reviews ->
                    _storeReviews.value = reviews
                }
            } catch (e: Exception) {
                Log.e("LookootViewModel", "Error loading store reviews", e)
            }
        }
    }

    // Updates a store's details in Firestore and updates the current store state.
    fun updateStore(store: Store, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                repository.updateStore(store)
                _currentStore.value = store
                onComplete(true)
            } catch (e: Exception) {
                Log.e("LookootViewModel", "Error updating store", e)
                onComplete(false)
            }
        }
    }

    // Creates a new item in a store and updates the item list in the state.
    fun createItem(item: Item, storeId: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val itemId = repository.createItem(item.copy(storeId = storeId))
                _storeItems.value = _storeItems.value + item.copy(id = itemId)
                onComplete(true)
            } catch (e: Exception) {
                Log.e("LookootViewModel", "Error creating item", e)
                onComplete(false)
            }
        }
    }

    // Updates an item's details and updates the item list in the state.
    fun updateItem(item: Item, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                repository.updateItem(item)
                _storeItems.value = _storeItems.value.map { if (it.id == item.id) item else it }
                _currentItem.value = item
                onComplete(true)
            } catch (e: Exception) {
                Log.e("LookootViewModel", "Error updating item", e)
                onComplete(false)
            }
        }
    }

    // Deletes an item from a store and updates the item list in the state.
    fun deleteItem(itemId: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                repository.deleteItem(itemId)
                _storeItems.value = _storeItems.value.filter { it.id != itemId }
                onComplete(true)
            } catch (e: Exception) {
                Log.e("LookootViewModel", "Error deleting item", e)
                onComplete(false)
            }
        }
    }

    // Deletes the currently loaded store and clears the store-related state.
    fun deleteCurrentStore(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                _currentStore.value?.let { store ->
                    repository.deleteStore(store.id)
                    _currentStore.value = null
                    _storeItems.value = emptyList()
                    onComplete(true)
                } ?: onComplete(false)
            } catch (e: Exception) {
                Log.e("LookootViewModel", "Error deleting store", e)
                onComplete(false)
            }
        }
    }

    // Clears the current user data and resets the logged-in state.
    private fun clearCurrentUser() {
        _currentUser.value = null
        _isLoggedIn.value = false
        _userStores.value = emptyList()
    }

    // Refreshes the current user data from Firebase and updates the state.
    fun refreshCurrentUser() {
        viewModelScope.launch {
            try {
                val user = getCurrentUserFromFirebase()
                _currentUser.value = user
                Log.d("LookootViewModel", "Current user refreshed: ${user?.username}")
            } catch (e: Exception) {
                Log.e("LookootViewModel", "Error refreshing current user", e)
            }
        }
    }

    // Requests store ownership for the current user by calling the Firestore repository.
    fun requestStoreOwnership() {
        viewModelScope.launch {
            try {
                currentUser.value?.id?.let { userId ->
                    repository.requestStoreOwnership(userId)
                    refreshCurrentUser()
                }
            } catch (e: Exception) {
                Log.e("LookootViewModel", "Error requesting store ownership", e)
            }
        }
    }

    // Fetches a list of all users from Firestore.
    fun getAllUsers(onResult: (List<User>) -> Unit) {
        viewModelScope.launch {
            try {
                val users = repository.getAllUsers()
                onResult(users)
            } catch (e: Exception) {
                Log.e("LookootViewModel", "Error getting all users", e)
                onResult(emptyList())
            }
        }
    }

    // Approves a user's request to become a store owner and updates the user list.
    fun approveStoreOwnerRequest(userId: String) {
        viewModelScope.launch {
            try {
                repository.approveStoreOwnerRequest(userId)
                getAllUsers { users ->
                    // Update the UI or state as needed
                }
            } catch (e: Exception) {
                Log.e("LookootViewModel", "Error approving store owner request", e)
            }
        }
    }

    // Declines a user's request to become a store owner and updates the user list.
    fun declineStoreOwnerRequest(userId: String) {
        viewModelScope.launch {
            try {
                repository.declineStoreOwnerRequest(userId)
                getAllUsers { users ->
                    // Update the UI or state as needed
                }
            } catch (e: Exception) {
                Log.e("LookootViewModel", "Error declining store owner request", e)
            }
        }
    }

    // Updates a user's role in Firestore and refreshes the user list.
    fun updateUserRole(userId: String, newRole: String) {
        viewModelScope.launch {
            try {
                repository.updateUserRole(userId, newRole)
                getAllUsers { users ->
                    // Update state or UI as needed
                }
            } catch (e: Exception) {
                Log.e("LookootViewModel", "Error updating user role", e)
            }
        }
    }
}