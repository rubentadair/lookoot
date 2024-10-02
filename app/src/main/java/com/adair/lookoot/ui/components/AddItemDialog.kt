// File: AddItemDialog.kt
package com.adair.lookoot.ui.components

import android.util.Log  // Import for logging
import androidx.compose.foundation.layout.*  // Import layout components for spacing and alignment
import androidx.compose.material3.*  // Import Material Design components for UI elements
import androidx.compose.runtime.*  // Import Compose runtime for state management
import androidx.compose.ui.Modifier  // Import Modifier for UI customization
import androidx.compose.ui.unit.dp  // Import unit for defining dimensions in UI
import com.adair.lookoot.models.Item  // Import the Item model for creating new items

/**
 * Composable function to display a dialog for adding a new item.
 * This dialog is used within the UI to allow users to input details for a new item they want to add.
 *
 * @param onDismiss Function to be called when the dialog is dismissed.
 * @param onAddItem Function to be called when the "Add" button is clicked, passing the new Item object.
 */
@Composable
fun AddItemDialog(onDismiss: () -> Unit, onAddItem: (Item) -> Unit) {
    // State variables to hold user input for item name, description, and price
    var itemName by remember { mutableStateOf("") }  // Holds the name of the item
    var itemDescription by remember { mutableStateOf("") }  // Holds the description of the item
    var itemPrice by remember { mutableStateOf("") }  // Holds the price of the item as a string

    // Log when the dialog is displayed
    Log.d("AddItemDialog", "Displaying Add Item Dialog")

    // AlertDialog is a Material Design dialog component that displays the input fields and buttons
    AlertDialog(
        onDismissRequest = {
            Log.d("AddItemDialog", "Dialog dismissed by user action")  // Log when the dialog is dismissed
            onDismiss()
        },  // Callback to handle the dialog dismissal when clicked outside or dismissed
        title = {
            Log.d("AddItemDialog", "Displaying dialog title")  // Log when the title is displayed
            Text("Add New Item")
        },  // The title of the dialog, displayed at the top
        text = {
            // The main content of the dialog where input fields are placed
            Column {
                // Input field for the item name
                OutlinedTextField(
                    value = itemName,
                    onValueChange = {
                        itemName = it
                        Log.d("AddItemDialog", "Item name updated: $itemName")  // Log the item name as it is updated
                    },  // Updates itemName state whenever the user types something
                    label = { Text("Item Name") },  // Label for the text field, displayed as a placeholder and on focus
                    modifier = Modifier.fillMaxWidth()  // Modifier to make the text field occupy the full width of the dialog
                )
                Spacer(modifier = Modifier.height(8.dp))  // Spacer to add vertical space between input fields
                // Input field for the item description
                OutlinedTextField(
                    value = itemDescription,
                    onValueChange = {
                        itemDescription = it
                        Log.d("AddItemDialog", "Item description updated: $itemDescription")  // Log the item description as it is updated
                    },  // Updates itemDescription state whenever the user types something
                    label = { Text("Description") },  // Label for the text field
                    modifier = Modifier.fillMaxWidth()  // Make the text field fill the available width
                )
                Spacer(modifier = Modifier.height(8.dp))  // Spacer to add vertical space between input fields
                // Input field for the item price
                OutlinedTextField(
                    value = itemPrice,
                    onValueChange = {
                        itemPrice = it
                        Log.d("AddItemDialog", "Item price updated: $itemPrice")  // Log the item price as it is updated
                    },  // Updates itemPrice state whenever the user types something
                    label = { Text("Price") },  // Label for the text field
                    modifier = Modifier.fillMaxWidth()  // Make the text field fill the available width
                )
            }
        },
        confirmButton = {
            // Button for confirming the addition of the new item
            TextButton(onClick = {
                // When the "Add" button is clicked, create a new Item object with the user's input data
                val newItem = Item(
                    name = itemName,  // Assign the input item name
                    description = itemDescription,  // Assign the input item description
                    price = itemPrice.toDoubleOrNull() ?: 0.0  // Attempt to convert the price to a Double, defaulting to 0.0 if conversion fails
                )
                Log.d("AddItemDialog", "New item created: $newItem")  // Log the creation of the new item
                // Pass the new item back to the parent component through the onAddItem callback function
                onAddItem(newItem)
            }) {
                Text("Add")  // The text displayed on the "Add" button
                Log.d("AddItemDialog", "Add button clicked")  // Log when the Add button is clicked
            }
        },
        dismissButton = {
            // Button for cancelling the dialog without adding a new item
            TextButton(onClick = {
                Log.d("AddItemDialog", "Cancel button clicked")  // Log when the Cancel button is clicked
                onDismiss()
            }) {
                Text("Cancel")  // The text displayed on the "Cancel" button
            }
        }
    )
}


