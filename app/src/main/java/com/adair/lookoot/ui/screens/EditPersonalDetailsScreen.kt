package com.adair.lookoot.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.adair.lookoot.viewmodels.UserProfileViewModel
import java.text.SimpleDateFormat
import java.util.*

// Displays a screen for editing personal details, including phone number and date of birth.
// Fetches and displays the current user's details, allowing the user to update them.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPersonalDetailsScreen(
    viewModel: UserProfileViewModel,
    onBackClick: () -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsState()

    var phoneNumber by remember { mutableStateOf("") }
    var day by remember { mutableStateOf("") }
    var month by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    // Initializes the fields with the current user profile information when it is available.
    LaunchedEffect(userProfile) {
        phoneNumber = userProfile?.phoneNumber ?: ""
        userProfile?.dateOfBirth?.let { dob ->
            val calendar = Calendar.getInstance().apply { timeInMillis = dob }
            day = calendar.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
            month = (calendar.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
            year = calendar.get(Calendar.YEAR).toString()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Personal Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        // Back button to navigate to the previous screen.
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        // Main content layout for editing fields.
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            // Input field for phone number.
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("Phone Number") },
                placeholder = { Text("+44 7123 456 789") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )
            // Fields for date of birth (day, month, year).
            Spacer(modifier = Modifier.height(16.dp))
            Text("Date of Birth", style = MaterialTheme.typography.bodyLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = day,
                    onValueChange = { if (it.length <= 2) day = it },
                    label = { Text("Day") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = month,
                    onValueChange = { if (it.length <= 2) month = it },
                    label = { Text("Month") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = year,
                    onValueChange = { if (it.length <= 4) year = it },
                    label = { Text("Year") },
                    modifier = Modifier.weight(1.5f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            // Displays an error message if any.
            if (error != null) {
                Text(
                    text = error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            // Save button to update personal details.
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    // Validates the phone number and date of birth before updating.
                    if (!isValidUKPhoneNumber(phoneNumber)) {
                        error = "Invalid UK phone number format"
                        return@Button
                    }
                    // Parses and validates the date of birth.
                    val dateOfBirth = try {
                        val dateString = "$year-$month-$day"
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.UK)
                        sdf.parse(dateString)?.time ?: throw Exception("Invalid date")
                    } catch (e: Exception) {
                        error = "Invalid date"
                        return@Button
                    }
                    // Updates the user profile with the new phone number and date of birth.
                    viewModel.updatePersonalDetails(phoneNumber, dateOfBirth) { success, errorMsg ->
                        if (success) {
                            onBackClick()
                        } else {
                            error = errorMsg ?: "Error updating personal details"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Changes")
            }
        }
    }
}
// Validates the UK phone number format.
private fun isValidUKPhoneNumber(phoneNumber: String): Boolean {
    val phoneRegex = """^(\+44\s?7\d{3}|\(?07\d{3}\)?)\s?\d{3}\s?\d{3}$""".toRegex()
    return phoneRegex.matches(phoneNumber)
}