// File: FirestoreRepository.kt
package com.adair.lookoot.data

import android.content.ContentValues.TAG
import android.net.Uri
import android.util.Log
import com.adair.lookoot.models.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import com.google.firebase.firestore.Filter

// Manages Firestore and Firebase Storage operations related to users, stores, items, and reviews.
class FirestoreRepository {

    // Initialising Firestore and Firebase Storage instances
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")
    private val storesCollection = firestore.collection("stores")
    private val itemsCollection = firestore.collection("items")
    private val reviewsCollection = firestore.collection("reviews")
    private val storage = FirebaseStorage.getInstance()
    private val storageRef = storage.reference

    // Creates a new user document in Firestore and returns the user ID
    suspend fun createUser(user: User): String {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("FirestoreRepository", "Attempting to create user document: ${user.id}")
                val userDocument = FirebaseFirestore.getInstance().collection("users").document(user.id)
                userDocument.set(user).await()
                Log.d("FirestoreRepository", "User document created successfully: ${user.id}")
                user.id
            } catch (e: Exception) {
                Log.e("FirestoreRepository", "Error creating user: ${e.message}", e)
                throw e
            }
        }
    }
    fun sanitizeStoreId(id: String): String {
        return URLEncoder.encode(id, "UTF-8")
    }
    // Fetches a user document from Firestore based on the user ID and returns a User object
    suspend fun getUser(userId: String): User? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("FirestoreRepository", "Fetching user document: $userId")
                val snapshot = usersCollection.document(userId).get().await()
                Log.d("FirestoreRepository", "User document fetched successfully: $userId")
                snapshot.toObject(User::class.java)
            } catch (e: FirebaseFirestoreException) {
                Log.e("FirestoreRepository", "Error fetching user: ${e.message}", e)
                null
            }
        }
    }

    // Updates the role of a user in Firestore based on the provided user ID and new role
    suspend fun updateUserRole(userId: String, newRole: String) {
        withContext(Dispatchers.IO) {
            try {
                Log.d("FirestoreRepository", "Updating user role for user: $userId to $newRole")
                usersCollection.document(userId).update("role", newRole).await()
                Log.d("FirestoreRepository", "User role updated successfully: $userId")
            } catch (e: FirebaseFirestoreException) {
                Log.e("FirestoreRepository", "Error updating user role: ${e.message}", e)
                throw e
            }
        }
    }
    // Creates a new store document in Firestore, adds timestamps, and returns the store ID.
    suspend fun createStore(store: Store): String {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("FirestoreRepository", "Creating store: ${store.name}")
                val storeWithTimestamp = store.copy(
                    createdAt = Timestamp.now(),
                    lastUpdated = Timestamp.now()
                )
                val documentReference = storesCollection.add(storeWithTimestamp.toMap()).await()
                val storeId = documentReference.id
                storesCollection.document(storeId).update("id", storeId).await()
                Log.d("FirestoreRepository", "Store created successfully with ID: $storeId")
                storeId
            } catch (e: FirebaseFirestoreException) {
                Log.e("FirestoreRepository", "Error creating store: ${e.message}", e)
                throw e
            }
        }
    }
    // Fetches a store document from Firestore based on the store ID and returns a Store object
    suspend fun getStore(storeId: String): Store? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("FirestoreRepository", "Fetching store document: $storeId")
                val snapshot = storesCollection.document(storeId).get().await()
                Log.d("FirestoreRepository", "Store document fetched successfully: $storeId")

                if (snapshot.exists()) {
                    val data = snapshot.data
                    if (data != null) {
                        val openingTimesMap = data["openingTimes"] as? Map<String, Map<String, String>> ?: mapOf()
                        val openingTimes = openingTimesMap.mapValues { (_, value) ->
                            OpeningHours(value["open"] ?: "", value["close"] ?: "")
                        }
                        Store(
                            id = snapshot.id,
                            ownerId = data["ownerId"] as? String ?: "",
                            name = data["name"] as? String ?: "",
                            description = data["description"] as? String ?: "",
                            location = data["location"] as? GeoPoint,
                            categories = (data["categories"] as? List<String>) ?: listOf(),
                            tags = (data["tags"] as? List<String>) ?: listOf(),
                            rating = (data["rating"] as? Number)?.toDouble() ?: 0.0,
                            reviewCount = (data["reviewCount"] as? Number)?.toInt() ?: 0,
                            createdAt = data["createdAt"] as? Timestamp,
                            lastUpdated = data["lastUpdated"] as? Timestamp,
                            followers = (data["followers"] as? List<String>) ?: listOf(),
                            openingTimes = openingTimes
                        )
                    } else {
                        null
                    }
                } else {
                    null
                }
            } catch (e: FirebaseFirestoreException) {
                Log.e("FirestoreRepository", "Error fetching store: ${e.message}", e)
                null
            }
        }
    }
    // Updates an existing store document in Firestore
    suspend fun updateStore(store: Store) {
        withContext(Dispatchers.IO) {
            try {
                Log.d("FirestoreRepository", "Updating store: ${store.id}")
                val updates = store.toMap().toMutableMap()
                updates["lastUpdated"] = Timestamp.now()
                storesCollection.document(store.id).update(updates).await()
                Log.d("FirestoreRepository", "Store updated successfully: ${store.id}")
            } catch (e: FirebaseFirestoreException) {
                Log.e("FirestoreRepository", "Error updating store: ${e.message}", e)
                throw e
            }
        }
    }
    // Deletes a store document and its associated items and reviews from Firestore
    suspend fun deleteStore(storeId: String) {
        withContext(Dispatchers.IO) {
            try {
                Log.d("FirestoreRepository", "Deleting store document: $storeId")
                storesCollection.document(storeId).delete().await()

                // Deleting associated items and reviews
                val itemsToDelete = itemsCollection.whereEqualTo("storeId", storeId).get().await()
                itemsToDelete.documents.forEach { it.reference.delete().await() }

                val reviewsToDelete = reviewsCollection.whereEqualTo("storeId", storeId).get().await()
                reviewsToDelete.documents.forEach { it.reference.delete().await() }

                Log.d("FirestoreRepository", "Store and associated items/reviews deleted successfully: $storeId")
            } catch (e: FirebaseFirestoreException) {
                Log.e("FirestoreRepository", "Error deleting store: ${e.message}", e)
                throw e
            }
        }
    }

    private fun Store.toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "ownerId" to ownerId,
            "name" to name,
            "description" to description,
            "location" to location,
            "categories" to categories,
            "tags" to tags,
            "rating" to rating,
            "reviewCount" to reviewCount,
            "createdAt" to createdAt,
            "lastUpdated" to lastUpdated,
            "followers" to followers,
            "openingTimes" to openingTimes.mapValues { (_, hours) ->
                mapOf("open" to hours.open, "close" to hours.close)
            }
        )
    }

    private fun Map<String, Any>.toStore(): Store {
        return Store(
            id = this["id"] as? String ?: "",
            ownerId = this["ownerId"] as? String ?: "",
            name = this["name"] as? String ?: "",
            description = this["description"] as? String ?: "",
            location = this["location"] as? GeoPoint,
            categories = (this["categories"] as? List<*>)?.filterIsInstance<String>() ?: listOf(),
            tags = (this["tags"] as? List<*>)?.filterIsInstance<String>() ?: listOf(),
            rating = (this["rating"] as? Number)?.toDouble() ?: 0.0,
            reviewCount = (this["reviewCount"] as? Number)?.toInt() ?: 0,
            createdAt = this["createdAt"] as? Timestamp,
            lastUpdated = this["lastUpdated"] as? Timestamp,
            followers = (this["followers"] as? List<*>)?.filterIsInstance<String>() ?: listOf(),
            openingTimes = (this["openingTimes"] as? Map<*, *>)?.mapNotNull { (day, hours) ->
                if (day is String && hours is Map<*, *>) {
                    day to OpeningHours(
                        open = hours["open"] as? String ?: "",
                        close = hours["close"] as? String ?: ""
                    )
                } else null
            }?.toMap() ?: mapOf()
        )
    }
    // Fetches all stores associated with a specific user ID from Firestore.
    suspend fun getStoresForUser(userId: String): List<Store> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("FirestoreRepository", "Fetching stores for user: $userId")
                val snapshot = storesCollection
                    .whereEqualTo("ownerId", userId)
                    .get()
                    .await()
                Log.d("FirestoreRepository", "Stores fetched successfully for user: $userId")
                snapshot.documents.mapNotNull { it.toObject(Store::class.java)?.copy(id = it.id) }
            } catch (e: FirebaseFirestoreException) {
                Log.e("FirestoreRepository", "Error fetching stores for user: ${e.message}", e)
                emptyList()
            }
        }
    }

    // Creates a new item document in Firestore and returns the item ID
    suspend fun createItem(item: Item): String {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("FirestoreRepository", "Starting item creation: ${item.name}")
                val itemWithTimestamp = item.copy(
                    createdAt = Timestamp.now(),
                    lastUpdated = Timestamp.now()
                )
                Log.d("FirestoreRepository", "Adding item to Firestore")
                val documentReference = itemsCollection.add(itemWithTimestamp).await()
                val itemId = documentReference.id
                Log.d("FirestoreRepository", "Item added. Updating with ID: $itemId")
                itemsCollection.document(itemId).update("id", itemId).await()
                Log.d("FirestoreRepository", "Item creation completed successfully. ID: $itemId")
                itemId
            } catch (e: FirebaseFirestoreException) {
                Log.e("FirestoreRepository", "Error creating item: ${e.message}", e)
                throw e
            }
        }
    }

    // Fetches an item document from Firestore based on the item ID and returns an Item object
    suspend fun getItem(itemId: String): Item? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("FirestoreRepository", "Fetching item document: $itemId")
                val snapshot = itemsCollection.document(itemId).get().await()
                Log.d("FirestoreRepository", "Item document fetched successfully: $itemId")
                snapshot.toObject(Item::class.java)
            } catch (e: FirebaseFirestoreException) {
                Log.e("FirestoreRepository", "Error fetching item: ${e.message}", e)
                null
            }
        }
    }
    // Fetches all items for a specific store from Firestore and emits them as a Flow.
    fun getItemsForStore(storeId: String): Flow<List<Item>> = flow {
        try {
            Log.d("FirestoreRepository", "Fetching items for store: $storeId")
            val snapshot = itemsCollection
                .whereEqualTo("storeId", storeId)
                .orderBy("lastUpdated", Query.Direction.DESCENDING)
                .get()
                .await()
            val items = snapshot.documents.mapNotNull { document ->
                document.toObject(Item::class.java)?.copy(id = document.id)
            }
            Log.d("FirestoreRepository", "Items fetched successfully for store: $storeId")
            items.forEach { item ->
                Log.d("FirestoreRepository", "Item: ${item.name}, ID: ${item.id}")
            }
            emit(items)
        } catch (e: FirebaseFirestoreException) {
            Log.e("FirestoreRepository", "Error fetching items for store: ${e.message}", e)
            emit(emptyList())
        }
    }
    // Updates an existing item document in Firestore
    suspend fun updateItem(item: Item) {
        withContext(Dispatchers.IO) {
            try {
                Log.d("FirestoreRepository", "Updating item: ${item.id}")
                val updates = item.toMap().toMutableMap()
                updates["lastUpdated"] = Timestamp.now()
                itemsCollection.document(item.id).update(updates).await()
                Log.d("FirestoreRepository", "Item updated successfully: ${item.id}")
            } catch (e: FirebaseFirestoreException) {
                Log.e("FirestoreRepository", "Error updating item: ${e.message}", e)
                throw e
            }
        }
    }

    // Deletes an item document and its associated reviews from Firestore
    suspend fun deleteItem(itemId: String) {
        withContext(Dispatchers.IO) {
            try {
                Log.d("FirestoreRepository", "Deleting item document: $itemId")
                itemsCollection.document(itemId).delete().await()
                val reviewsToDelete = reviewsCollection.whereEqualTo("itemId", itemId).get().await()
                reviewsToDelete.documents.forEach { it.reference.delete().await() }
                Log.d("FirestoreRepository", "Item and associated reviews deleted successfully: $itemId")
            } catch (e: FirebaseFirestoreException) {
                Log.e("FirestoreRepository", "Error deleting item: ${e.message}", e)
                throw e
            }
        }
    }

    // Fetches all reviews for a specific store and returns a Flow of a list of Review objects
    fun getReviewsForStore(storeId: String): Flow<List<Review>> = flow {
        try {
            Log.d("FirestoreRepository", "Fetching reviews for store: $storeId")
            val snapshot = reviewsCollection
                .whereEqualTo("storeId", storeId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            val reviews = snapshot.toObjects(Review::class.java)
            Log.d("FirestoreRepository", "Reviews fetched successfully for store: $storeId")
            emit(reviews)
        } catch (e: FirebaseFirestoreException) {
            Log.e("FirestoreRepository", "Error fetching reviews for store: ${e.message}", e)
            emit(emptyList())
        }
    }

    // Fetches all reviews for a specific item and returns a Flow of a list of Review objects
    fun getReviewsForItem(itemId: String): Flow<List<Review>> = flow {
        try {
            Log.d("FirestoreRepository", "Fetching reviews for item: $itemId")
            val snapshot = reviewsCollection
                .whereEqualTo("itemId", itemId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            val reviews = snapshot.toObjects(Review::class.java)
            Log.d("FirestoreRepository", "Reviews fetched successfully for item: $itemId")
            emit(reviews)
        } catch (e: FirebaseFirestoreException) {
            Log.e("FirestoreRepository", "Error fetching reviews for item: ${e.message}", e)
            emit(emptyList())
        }
    }

    // Converts Item object to a map for Firestore operations
    private fun Item.toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "storeId" to storeId,
            "name" to name,
            "description" to description,
            "price" to price,
            "categories" to categories,
            "tags" to tags,
            "inStock" to inStock,
            "createdAt" to createdAt,
            "lastUpdated" to lastUpdated
        )
    }

    // Converts Review object to a map for Firestore operations
    private fun Review.toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "userId" to userId,
            "storeId" to storeId,
            "itemId" to itemId,
            "rating" to rating,
            "comment" to comment,
            "timestamp" to timestamp
        )
    }

    // Fetches all users in the Firestore database and returns a list of User objects
    suspend fun getAllUsers(): List<User> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("FirestoreRepository", "Fetching all users")
                val snapshot = firestore.collection("users").get().await()
                Log.d("FirestoreRepository", "All users fetched successfully")
                snapshot.toObjects(User::class.java)
            } catch (e: Exception) {
                Log.e("FirestoreRepository", "Error getting all users", e)
                emptyList()
            }
        }
    }

    // Requests store ownership for a user by updating their Firestore document
    suspend fun requestStoreOwnership(userId: String) {
        withContext(Dispatchers.IO) {
            try {
                Log.d("FirestoreRepository", "Requesting store ownership for user: $userId")
                usersCollection.document(userId).update("storeOwnerRequestStatus", "PENDING").await()
                Log.d("FirestoreRepository", "Store ownership requested successfully for user: $userId")
            } catch (e: FirebaseFirestoreException) {
                Log.e("FirestoreRepository", "Error requesting store ownership: ${e.message}", e)
                throw e
            }
        }
    }

    // Approves a store ownership request for a user by updating their Firestore document
    suspend fun approveStoreOwnerRequest(userId: String) {
        withContext(Dispatchers.IO) {
            try {
                Log.d("FirestoreRepository", "Approving store ownership request for user: $userId")
                usersCollection.document(userId).update(
                    mapOf(
                        "storeOwnerRequestStatus" to "APPROVED",
                        "role" to "STORE_OWNER"
                    )
                ).await()
                Log.d("FirestoreRepository", "Store ownership request approved for user: $userId")
            } catch (e: FirebaseFirestoreException) {
                Log.e("FirestoreRepository", "Error approving store ownership request: ${e.message}", e)
                throw e
            }
        }
    }

    // Declines a store ownership request for a user by updating their Firestore document
    suspend fun declineStoreOwnerRequest(userId: String) {
        withContext(Dispatchers.IO) {
            try {
                Log.d("FirestoreRepository", "Declining store ownership request for user: $userId")
                usersCollection.document(userId).update("storeOwnerRequestStatus", null).await()
                Log.d("FirestoreRepository", "Store ownership request declined for user: $userId")
            } catch (e: FirebaseFirestoreException) {
                Log.e("FirestoreRepository", "Error declining store ownership request: ${e.message}", e)
                throw e
            }
        }
    }
    // Fetches all stores from Firestore and returns a list of Store objects.
    suspend fun getAllStores(): List<Store> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("FirestoreRepository", "Fetching all stores")
                val snapshot = storesCollection.get().await()
                val stores = snapshot.documents.mapNotNull { document ->
                    val store = document.toObject(Store::class.java)
                    store?.copy(id = document.id)
                }
                Log.d("FirestoreRepository", "Fetched ${stores.size} stores")
                stores.forEach { store ->
                    Log.d("FirestoreRepository", "Store: ${store.name}, ID: ${store.id}")
                }
                stores
            } catch (e: Exception) {
                Log.e("FirestoreRepository", "Error fetching all stores", e)
                emptyList()
            }
        }
    }
    // Searches stores based on a query string and returns matching stores.
    suspend fun searchStores(query: String): List<Store> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("FirestoreRepository", "Searching stores with query: $query")
                val snapshot = storesCollection
                    .whereArrayContains("searchKeywords", query.lowercase())
                    .get()
                    .await()
                val stores = snapshot.toObjects(Store::class.java)
                Log.d("FirestoreRepository", "Found ${stores.size} stores for query: $query")
                stores.forEach { store ->
                    Log.d("FirestoreRepository", "Store: ${store.name}, ID: ${store.id}")
                }
                stores
            } catch (e: Exception) {
                Log.e("FirestoreRepository", "Error searching stores", e)
                emptyList()
            }
        }
    }
    // Fetches all items from Firestore and returns a list of Item objects.
    suspend fun getAllItems(): List<Item> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("FirestoreRepository", "Fetching all items")
                val snapshot = itemsCollection.get().await()
                val items = snapshot.documents.mapNotNull { document ->
                    val item = document.toObject(Item::class.java)
                    item?.copy(id = document.id)
                }
                Log.d("FirestoreRepository", "Fetched ${items.size} items")
                items.forEach { item ->
                    Log.d("FirestoreRepository", "Item: ${item.name}, ID: ${item.id}, Tags: ${item.tagsList}")
                }
                items
            } catch (e: Exception) {
                Log.e("FirestoreRepository", "Error fetching all items", e)
                emptyList()
            }
        }
    }
    // Searches items based on a query string and returns matching items.
    suspend fun searchItems(query: String): List<Item> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Searching items with query: $query")
                val lowercaseQuery = query.lowercase()
                val exactMatches = itemsCollection
                    .whereArrayContains("searchKeywords", lowercaseQuery)
                    .get()
                    .await()
                    .documents
                    .mapNotNull { it.toObject(Item::class.java)?.copy(id = it.id) }

                val partialMatches = itemsCollection
                    .get()
                    .await()
                    .documents
                    .mapNotNull { it.toObject(Item::class.java)?.copy(id = it.id) }
                    .filter { item ->
                        item.name.lowercase().contains(lowercaseQuery) ||
                                item.description.lowercase().contains(lowercaseQuery) ||
                                item.categories.any { it.lowercase().contains(lowercaseQuery) } ||
                                (item.tags as? List<String>)?.any { it.lowercase().contains(lowercaseQuery) } == true
                    }

                val combinedResults = (exactMatches + partialMatches).distinctBy { it.id }
                Log.d(TAG, "Found ${combinedResults.size} items for query: $query")
                combinedResults
            } catch (e: Exception) {
                Log.e(TAG, "Error searching items", e)
                emptyList()
            }
        }
    }
    // Updates a user's email address in Firestore.
    suspend fun updateUserEmail(userId: String, newEmail: String) {
        try {
            Log.d("FirestoreRepository", "Updating email for user: $userId")
            usersCollection.document(userId).update("email", newEmail).await()
            Log.d("FirestoreRepository", "Email updated successfully for user: $userId")
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Error updating user email", e)
            throw e
        }
    }
}