// File: Models.kt
package com.adair.lookoot.models

import com.google.firebase.firestore.GeoPoint
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Data class representing a User in the system.
 * @param id Unique identifier for the user.
 * @param email User's email address.
 * @param username User's chosen username.
 * @param bio Short biography or description provided by the user.
 * @param role Role assigned to the user (e.g., USER, ADMIN, STORE_OWNER).
 * @param createdAt Timestamp when the user was created in the system.
 * @param lastUpdated Timestamp of the last update made to the user's information.
 * @param followers List of user IDs who are following this user.
 * @param following List of user IDs that this user is following.
 * @param storeOwnerRequestStatus Status of the user's request to become a store owner (e.g., PENDING, APPROVED).
 * @param profilePictureUrl URL to the user's profile picture stored in Firebase Storage.
 * @param followedStores List of store IDs that the user follows.
 */
data class User(
    val id: String = "",
    val email: String = "",
    val username: String = "N/A",
    val bio: String = "",
    val role: String = "USER",
    val createdAt: Timestamp? = null,
    val lastUpdated: Timestamp? = null,
    val followers: List<String> = listOf(),
    val following: List<String> = listOf(),
    val storeOwnerRequestStatus: String? = null,
    val profilePictureUrl: String? = null,
    val followedStores: List<String> = listOf(),
    val phoneNumber: String? = null,
    val dateOfBirth: Long? = null,  // Using Long to represent timestamp
    val isDarkMode: Boolean = false,
    )

/**
 * Data class representing a Store in the system.
 * @param id Unique identifier for the store.
 * @param ownerId User ID of the store owner.
 * @param name Name of the store.
 * @param description Description of the store.
 * @param location GeoPoint representing the store's location.
 * @param categories List of categories the store belongs to.
 * @param tags List of tags associated with the store for searchability.
 * @param rating Average rating of the store based on user reviews.
 * @param reviewCount Number of reviews the store has received.
 * @param createdAt Timestamp when the store was created in the system.
 * @param lastUpdated Timestamp of the last update made to the store's information.
 * @param followers List of user IDs who follow this store.
 */
data class Store(
    val id: String = "",
    val ownerId: String = "",
    val name: String = "",
    val description: String = "",
    val location: GeoPoint? = null,
    val categories: List<String> = listOf(),
    val tags: List<String> = listOf(),
    val rating: Double = 0.0,
    val reviewCount: Int = 0,
    val createdAt: Timestamp? = null,
    val lastUpdated: Timestamp? = null,
    val followers: List<String> = listOf(),
    val openingTimes: Map<String, OpeningHours> = mapOf()
)


data class OpeningHours(
    val open: String = "",  // Provide default values
    val close: String = ""  // Provide default values
)

/**
 * Data class representing an Item in the system.
 * @param id Unique identifier for the item.
 * @param storeId ID of the store to which this item belongs.
 * @param name Name of the item.
 * @param description Description of the item.
 * @param price Price of the item.
 * @param categories List of categories the item belongs to.
 * @param tags List of tags associated with the item for searchability.
 * @param inStock Boolean indicating whether the item is currently in stock.
 * @param createdAt Timestamp when the item was created in the system.
 * @param lastUpdated Timestamp of the last update made to the item's information.
 * @param wishlistedBy List of user IDs who have added this item to their wishlist.
 */
data class Item(
    val id: String = "",
    val storeId: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val categories: List<String> = listOf(),
    val tags: Any? = null,  // Change this to Any? to accept both String and List<String>
    val inStock: Boolean = true,
    val createdAt: Timestamp? = null,
    val lastUpdated: Timestamp? = null,
    val wishlistedBy: List<String> = listOf()
){
val tagsList: List<String>
    get() = when (tags) {
        is String -> tags.split(",").map { it.trim() }
        is List<*> -> tags as List<String>
        else -> emptyList()
    }
}
/**
 * Data class representing a Review in the system.
 * @param id Unique identifier for the review.
 * @param userId ID of the user who wrote the review.
 * @param storeId ID of the store that the review is for.
 * @param itemId Optional ID of the item that the review is for (null if the review is for the store).
 * @param rating Rating given by the user, typically from 1 to 5.
 * @param comment Text of the review written by the user.
 * @param timestamp Timestamp when the review was created.
 * @param likes Number of likes this review has received.
 */
data class Review(
    val id: String = "",
    val userId: String = "",
    val storeId: String = "",
    val itemId: String? = null,
    val rating: Int = 0,
    val comment: String = "",
    val timestamp: Timestamp? = null,
    val likes: Int = 0
)

/**
 * Data class representing an action history item, tracking significant user actions.
 * @param id Unique identifier for the history item.
 * @param userId ID of the user who performed the action.
 * @param action Description of the action performed.
 * @param details Additional details about the action.
 * @param actionType Enum representing the type of action (e.g., CREATE, UPDATE, DELETE).
 * @param targetType Enum representing the type of entity the action was performed on (e.g., STORE, ITEM).
 * @param targetId ID of the target entity the action was performed on.
 * @param timestamp Timestamp when the action was performed.
 * @param changes Map of any changes made during the action, with keys as field names and values as the new data.
 * @param additionalInfo Map of any additional information related to the action.
 */
data class HistoryItem(
    val id: String = "",
    val userId: String,
    val action: String = "",
    val details: String = "",
    val actionType: ActionType,
    val targetType: TargetType,
    val targetId: String,
    val timestamp: Timestamp = Timestamp.now(),
    val changes: Map<String, Any?> = mapOf(),
    val additionalInfo: Map<String, Any?> = mapOf()
) {

    /**
     * Enum class representing different types of actions that can be tracked in the history.
     */
    enum class ActionType {
        CREATE, UPDATE, DELETE, VIEW, LIKE, REVIEW, ADD_TO_WISHLIST, REMOVE_FROM_WISHLIST, FOLLOW, UNFOLLOW
    }

    /**
     * Enum class representing different types of entities that actions can be performed on.
     */
    enum class TargetType {
        STORE, ITEM, REVIEW, USER, WISHLIST
    }

    /**
     * Returns a DocumentReference pointing to the target entity in Firestore.
     * @return DocumentReference to the target entity based on its type.
     */
    fun getTargetReference(): DocumentReference {
        return when (targetType) {
            TargetType.STORE -> FirebaseFirestore.getInstance().collection("stores").document(targetId)
            TargetType.ITEM -> FirebaseFirestore.getInstance().collection("items").document(targetId)
            TargetType.REVIEW -> FirebaseFirestore.getInstance().collection("reviews").document(targetId)
            TargetType.USER -> FirebaseFirestore.getInstance().collection("users").document(targetId)
            TargetType.WISHLIST -> FirebaseFirestore.getInstance().collection("wishlists").document(targetId)
        }
    }

    /**
     * Provides a human-readable description of the action performed.
     * @return Formatted string describing the action.
     */
    fun getFormattedDescription(): String {
        return when (actionType) {
            ActionType.CREATE -> "Created a new ${targetType.name.toLowerCase()}"
            ActionType.UPDATE -> "Updated ${targetType.name.toLowerCase()} details"
            ActionType.DELETE -> "Deleted a ${targetType.name.toLowerCase()}"
            ActionType.VIEW -> "Viewed a ${targetType.name.toLowerCase()}"
            ActionType.LIKE -> "Liked a ${targetType.name.toLowerCase()}"
            ActionType.REVIEW -> "Left a review for a ${targetType.name.toLowerCase()}"
            ActionType.ADD_TO_WISHLIST -> "Added an item to wishlist"
            ActionType.REMOVE_FROM_WISHLIST -> "Removed an item from wishlist"
            ActionType.FOLLOW -> "Followed a ${targetType.name.toLowerCase()}"
            ActionType.UNFOLLOW -> "Unfollowed a ${targetType.name.toLowerCase()}"
        }
    }
}
