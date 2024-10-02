package com.adair.lookoot.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adair.lookoot.data.FirestoreRepository
import com.adair.lookoot.models.User
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * ViewModel responsible for managing user authentication logic.
 * Handles sign-up, login, auto sign-in, and logout processes.
 */
class LoginViewModel : ViewModel() {
    // Firebase authentication instance
    private val auth = FirebaseAuth.getInstance()

    // Firestore repository for interacting with user data
    private val repository = FirestoreRepository()

    // MutableStateFlow to track if the user is signed in
    private val _isUserSignedIn = MutableStateFlow<Boolean?>(null)
    val isUserSignedIn = _isUserSignedIn.asStateFlow()

    init {
        Log.d("LoginViewModel", "Initialized. Current user: ${auth.currentUser?.uid ?: "null"}")
        checkUserSignInStatus()  // Check the sign-in status when the ViewModel is initialized
    }

    /**
     * Checks whether a user is currently signed in and updates the state accordingly.
     */
    private fun checkUserSignInStatus() {
        viewModelScope.launch {
            _isUserSignedIn.value = auth.currentUser != null
            Log.d("LoginViewModel", "User sign-in status checked: ${_isUserSignedIn.value}")
        }
    }

    /**
     * Attempts to automatically sign in the current user.
     * @param onResult Callback to return the result of the auto sign-in attempt.
     */
    fun autoSignIn(onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                Log.d("LoginViewModel", "Auto sign-in successful. User ID: ${currentUser.uid}")
                onResult(true, null)
            } else {
                Log.d("LoginViewModel", "Auto sign-in failed: No user currently signed in")
                onResult(false, "No user signed in")
            }
        }
    }

    /**
     * Handles user sign-up using email and password.
     * @param email The email address of the new user.
     * @param password The password for the new user.
     * @param username The desired username for the new user.
     * @param onResult Callback to return the result of the sign-up attempt.
     */
    fun signUp(email: String, password: String, username: String, onResult: (Boolean, String?, User?) -> Unit) {
        viewModelScope.launch {
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val firebaseUser = result.user
                if (firebaseUser != null) {
                    val userId = firebaseUser.uid
                    Log.d("LoginViewModel", "Firebase Auth user created with ID: $userId")

                    // Create a new User object and store it in Firestore
                    val newUser = User(
                        id = userId,
                        email = email,
                        username = username,
                        createdAt = Timestamp.now(),
                        lastUpdated = Timestamp.now()
                    )
                    repository.createUser(newUser)
                    Log.d("LoginViewModel", "New user data stored in Firestore. User ID: $userId")
                    onResult(true, null, newUser)
                } else {
                    Log.e("LoginViewModel", "Sign-up failed: Firebase user is null")
                    onResult(false, "Sign-up failed: user is null", null)
                }
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Sign-up failed: ${e.message}", e)
                onResult(false, e.message, null)
            }
        }
    }

    /**
     * Handles user login using email and password.
     * @param email The email address of the user.
     * @param password The password of the user.
     * @param onResult Callback to return the result of the login attempt.
     */
    fun login(email: String, password: String, onResult: (Boolean, String?, User?) -> Unit) {
        viewModelScope.launch {
            try {
                Log.d("LoginViewModel", "Attempting login for email: $email")
                val result = auth.signInWithEmailAndPassword(email, password).await()
                val firebaseUser = result.user
                if (firebaseUser != null) {
                    val userId = firebaseUser.uid
                    val user = repository.getUser(userId)
                    Log.d("LoginViewModel", "Login successful. User ID: $userId")
                    onResult(true, null, user)
                } else {
                    Log.e("LoginViewModel", "Login failed: Firebase user is null")
                    onResult(false, "Login failed: user is null", null)
                }
            } catch (e: FirebaseAuthException) {
                Log.e("LoginViewModel", "Login failed: ${getErrorMessage(e)}")
                onResult(false, getErrorMessage(e), null)
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Login failed: Unexpected error: ${e.message}")
                onResult(false, "An unexpected error occurred: ${e.message}", null)
            }
        }
    }

    /**
     * Maps FirebaseAuthException error codes to user-friendly error messages.
     * @param e The FirebaseAuthException that was caught.
     * @return A user-friendly error message.
     */
    private fun getErrorMessage(e: FirebaseAuthException): String {
        return when (e.errorCode) {
            "ERROR_WEAK_PASSWORD" -> "The password is too weak."
            "ERROR_INVALID_EMAIL" -> "The email address is badly formatted."
            "ERROR_EMAIL_ALREADY_IN_USE" -> "The email address is already in use by another account."
            "ERROR_WRONG_PASSWORD" -> "The password is incorrect."
            "ERROR_USER_NOT_FOUND" -> "There is no user account with this email."
            else -> "Authentication failed: ${e.message}"
        }
    }

    /**
     * Retrieves the current user information if logged in.
     * @return The current User object or null if no user is logged in.
     */
}
