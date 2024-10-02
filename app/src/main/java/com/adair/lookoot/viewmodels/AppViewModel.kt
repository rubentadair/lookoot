package com.adair.lookoot.viewmodels

import androidx.lifecycle.ViewModel  // Import the ViewModel class to manage UI-related data in a lifecycle-conscious way
import kotlinx.coroutines.flow.MutableStateFlow  // Import MutableStateFlow for managing state that can be observed and modified
import kotlinx.coroutines.flow.asStateFlow  // Import asStateFlow to expose a read-only StateFlow

class AppViewModel : ViewModel() {
    // MutableStateFlow to hold the login state, initially set to false
    private val _isLoggedIn = MutableStateFlow(false)

    // Expose the login state as a read-only StateFlow
    val isLoggedIn = _isLoggedIn.asStateFlow()

    // Function to update the login state
    fun setLoggedIn(value: Boolean) {
        _isLoggedIn.value = value  // Update the MutableStateFlow with the new value
    }
}
