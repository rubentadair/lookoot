package com.adair.lookoot.viewmodels

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adair.lookoot.data.FirestoreRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneMultiFactorGenerator
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import java.util.concurrent.TimeUnit

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val username: String = "N/A",
    val bio: String = "",
    val profilePictureUrl: String? = null,
    val followedStores: List<String> = listOf(),
    val isDarkMode: Boolean = false,
    val phoneNumber: String? = null,
    val dateOfBirth: Long? = null,
    val role: String = "USER"
)

class UserProfileViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val repository = FirestoreRepository()

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    var verificationId: String? = null

    // Initializes FirebaseAppCheck, loads dark mode preferences, and loads the user profile.
    init {
        FirebaseApp.getInstance().let { firebaseApp ->
            FirebaseAppCheck.getInstance(firebaseApp).getToken(false)
                .addOnSuccessListener { result ->
                    if (result == null) {
                        Log.w(
                            "UserProfileViewModel",
                            "App Check not initialized. Some operations may fail."
                        )
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("UserProfileViewModel", "Failed to get App Check token", e)
                }
            loadDarkModePreference()
            loadUserProfile(auth.currentUser?.uid ?: "")
        }
    }

    // Loads the user's dark mode preference from Firestore and updates the state.
    private fun loadDarkModePreference() {
        viewModelScope.launch {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId != null) {
                try {
                    val document = db.collection("users").document(userId).get().await()
                    _isDarkMode.value = document.getBoolean("isDarkMode") ?: false
                    Log.d("UserProfileViewModel", "Dark Mode Preference loaded: ${_isDarkMode.value}")
                } catch (e: Exception) {
                    Log.e("UserProfileViewModel", "Error loading dark mode preference", e)
                    _isDarkMode.value = false
                }
            } else {
                _isDarkMode.value = false
            }
        }
    }
    // Updates the user's dark mode preference in Firestore and updates the state.
    fun updateDarkModePreference(userId: String, isDarkMode: Boolean, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val updates = mapOf("isDarkMode" to isDarkMode)

                db.collection("users").document(userId)
                    .set(updates, SetOptions.merge())
                    .await()

                _isDarkMode.value = isDarkMode
                Log.d("UserProfileViewModel", "Dark mode preference updated: $isDarkMode")
                onResult(true)
            } catch (e: Exception) {
                Log.e("UserProfileViewModel", "Error updating dark mode preference", e)
                onResult(false)
            }
        }
    }

    // Loads the user's profile from Firestore and updates the state.
    fun loadUserProfile(userId: String) {
        viewModelScope.launch {
            try {
                val userDoc = db.collection("users").document(userId).get().await()
                val user = userDoc.toObject(UserProfile::class.java)

                if (user != null) {
                    _userProfile.value = user
                } else {
                    Log.e("UserProfileViewModel", "User profile is null or not found")
                }
            } catch (e: Exception) {
                Log.e("UserProfileViewModel", "Error loading user profile", e)
            }
        }
    }

    // Deletes the user's account from Firebase Auth and Firestore, after re-authenticating.
    fun deleteAccount(userId: String, currentPassword: String, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                Log.e("UserProfileViewModel", "No authenticated user found")
                onComplete(false, "No authenticated user found")
                return@launch
            }

            // Re-authenticate the user before deletion
            if (!reAuthenticateUser(currentPassword)) {
                Log.e("UserProfileViewModel", "Failed to re-authenticate user")
                onComplete(false, "Failed to re-authenticate user")
                return@launch
            }

            // Delete the user account from Firebase Auth
            try {
                user.delete().await()

                // Delete user document from Firestore after successful Firebase Auth deletion
                db.collection("users").document(userId).delete().await()

                // Clear the in-memory user profile data
                clearUserProfile()
                onComplete(true, null)  // Success
            } catch (e: Exception) {
                Log.e("UserProfileViewModel", "Error deleting user", e)
                onComplete(false, e.message)
            }
        }
    }
    // Clears the user's profile data from the ViewModel state.
    private fun clearUserProfile() {
        _userProfile.value = null
        _isDarkMode.value = false
    }
    // Re-authenticates the user using their current password.
    suspend fun reAuthenticateUser(currentPassword: String): Boolean {
        return try {
            val user = FirebaseAuth.getInstance().currentUser ?: return false
            val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)
            user.reauthenticate(credential).await()
            true
        } catch (e: Exception) {
            Log.e("UserProfileViewModel", "Re-authentication failed", e)
            false
        }
    }
    // Checks if the given username is available in Firestore.
    fun isUsernameAvailable(username: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val trimmedUsername = username.trim()
                Log.d("UserProfileViewModel", "Checking availability for username: $trimmedUsername")
                val querySnapshot = db.collection("users")
                    .whereEqualTo("username", trimmedUsername)
                    .limit(1)
                    .get()
                    .await()
                val isAvailable = querySnapshot.isEmpty
                Log.d("UserProfileViewModel", "Username '$trimmedUsername' availability: $isAvailable")
                if (!isAvailable) {
                    Log.d("UserProfileViewModel", "Conflicting document ID: ${querySnapshot.documents.firstOrNull()?.id}")
                }
                onResult(isAvailable)
            } catch (e: Exception) {
                Log.e("UserProfileViewModel", "Error checking username availability", e)
                onResult(false)
            }
        }
    }

    // Updates the user's profile information (username, bio) in Firestore.
    fun updateProfile(userId: String, username: String, bio: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val updates = mapOf(
                    "username" to username,
                    "bio" to bio
                )
                db.collection("users").document(userId)
                    .set(updates, SetOptions.merge())
                    .await()
                _userProfile.value = _userProfile.value?.copy(
                    username = username,
                    bio = bio
                )
                Log.d("UserProfileViewModel", "Profile updated: ${_userProfile.value}")
                onResult(true)
            } catch (e: Exception) {
                Log.e("UserProfileViewModel", "Error updating profile", e)
                onResult(false)
            }
        }
    }
    // Uploads a new profile picture to Firebase Storage and updates Firestore with the URL.
    fun uploadProfilePicture(imageUri: Uri, userId: String, onComplete: (Boolean, String?) -> Unit) {
        val imageFileName = "profile_${userId}_${UUID.randomUUID()}.jpg"
        val imageRef = storage.reference.child("profilePictures/$userId/$imageFileName")

        imageRef.putFile(imageUri)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    updateProfilePictureUrl(userId, downloadUri.toString()) { success ->
                        onComplete(success, downloadUri.toString())
                    }
                }
            }
            .addOnFailureListener {
                onComplete(false, null)
            }
    }
    // Updates the user's profile picture URL in Firestore.
    private fun updateProfilePictureUrl(userId: String, imageUrl: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val updates = mapOf("profilePictureUrl" to imageUrl)
                db.collection("users").document(userId)
                    .set(updates, SetOptions.merge())
                    .await()
                _userProfile.value = _userProfile.value?.copy(profilePictureUrl = imageUrl)
                onComplete(true)
            } catch (e: Exception) {
                Log.e("UserProfileViewModel", "Error updating profile picture URL", e)
                onComplete(false)
            }
        }
    }

    // Updates the user's phone number and date of birth in Firestore.
    fun updatePersonalDetails(phoneNumber: String, dateOfBirth: Long, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
                val updates = mapOf(
                    "phoneNumber" to phoneNumber,
                    "dateOfBirth" to dateOfBirth
                )
                db.collection("users").document(userId)
                    .update(updates)
                    .await()
                _userProfile.value = _userProfile.value?.copy(
                    phoneNumber = phoneNumber,
                    dateOfBirth = dateOfBirth
                )
                onResult(true, null)
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }

    // Changes the user's password after re-authenticating.
    fun changePassword(currentPassword: String, newPassword: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val user = auth.currentUser ?: throw Exception("User not authenticated")
                val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)
                user.reauthenticate(credential).await()
                user.updatePassword(newPassword).await()
                Log.d("UserProfileViewModel", "Password changed successfully")
                onResult(true, null)
            } catch (e: Exception) {
                Log.e("UserProfileViewModel", "Error changing password", e)
                onResult(false, e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Changes the user's email after re-authenticating.
    fun changeEmail(currentPassword: String, newEmail: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val user = auth.currentUser ?: throw Exception("User not authenticated")
                val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)
                user.reauthenticate(credential).await()
                user.updateEmail(newEmail).await()
                repository.updateUserEmail(user.uid, newEmail)
                onResult(true, null)
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }

    // Sets up multi-factor authentication (MFA) using the user's phone number.
    fun setupMFA(phoneNumber: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val user = auth.currentUser ?: throw Exception("User not authenticated")
                val multiFactorSession = user.multiFactor.session.await()

                val phoneAuthOptions = PhoneAuthOptions.newBuilder()
                    .setPhoneNumber(phoneNumber)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setMultiFactorSession(multiFactorSession)
                    .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                            completeMFAEnrollment(credential, onResult)
                        }

                        override fun onVerificationFailed(e: FirebaseException) {
                            Log.e("MFA", "Verification failed: ${e.message}", e)
                            onResult(false, "Verification failed: ${e.message}")
                        }

                        override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                            this@UserProfileViewModel.verificationId = verificationId
                            onResult(true, null)
                        }
                    })
                    .build()

                PhoneAuthProvider.verifyPhoneNumber(phoneAuthOptions)
            } catch (e: Exception) {
                Log.e("MFA", "Failed to setup MFA: ${e.message}", e)
                onResult(false, e.message)
            }
        }
    }

    // Completes MFA enrollment by verifying the phone number.
    fun completeMFAEnrollment(credential: PhoneAuthCredential, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val multiFactorAssertion = PhoneMultiFactorGenerator.getAssertion(credential)
                val user = auth.currentUser
                user?.multiFactor?.enroll(multiFactorAssertion, "Phone number")?.await()
                onResult(true, "MFA setup successful")
            } catch (e: Exception) {
                onResult(false, "Failed to enroll MFA: ${e.message}")
            }
        }
    }

    // Refreshes the user's data from Firebase Auth.
    suspend fun refreshUser() {
        auth.currentUser?.reload()?.await()
    }

    // Checks if the user's email is verified.
    fun isEmailVerified(): Boolean {
        return auth.currentUser?.isEmailVerified ?: false
    }

    // Returns the user's email.
    fun getUserEmail(): String? {
        return auth.currentUser?.email
    }

    // Sends a verification email to the user.
    fun sendEmailVerification(callback: (Boolean, String?) -> Unit) {
        auth.currentUser?.sendEmailVerification()
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback(true, null)
                } else {
                    callback(false, task.exception?.message)
                }
            }
    }

    // Sends a password reset email to the specified email address.
    fun sendPasswordResetEmail(email: String, onResult: (Boolean, String?) -> Unit) {
        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    onResult(false, task.exception?.message ?: "Failed to send reset email")
                }
            }
    }
}