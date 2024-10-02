package com.adair.lookoot.ui.components

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adair.lookoot.models.Item
import com.adair.lookoot.models.Store
import java.time.LocalDate
import java.util.Locale


// Displays an item card with the item's name, description, and price.
// Allows the user to click the card to trigger an action (e.g., view details).
@Composable
fun ItemCard(
    item: Item,
    onItemClick: (Item) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable {
                Log.d("ItemCard", "Item clicked: ${item.name}, ID: ${item.id}")
                onItemClick(item)
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = item.name, style = MaterialTheme.typography.titleMedium)
                Text(text = item.description, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                text = "£%.2f".format(item.price),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

// Shows a simple alert dialog for displaying an error message.
// Has an "OK" button to dismiss the dialog, which triggers onDismiss.
// Logs the dismissal of the error dialog for debugging.
@Composable
fun ErrorDialog(errorMessage: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Error") },
        text = { Text(errorMessage) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
                // Log error dialog dismissal
                Log.d("ErrorDialog", "Error dialog dismissed.")
            }
        }
    )
}


// Displays a store card with the store's name, description, and creation date.
// If the store has opening hours for the current day, they are displayed as well.
// The card is clickable, triggering onStoreClick with the store's ID.
@Composable
fun StoreCard(store: Store, onStoreClick: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable {
                if (store.id.isNotBlank()) {
                    onStoreClick(store.id)
                } else {
                    Log.e("StoreCard", "Attempted to click on store with blank ID: $store")
                }
            }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = store.name, style = MaterialTheme.typography.titleMedium)
            Text(text = store.description, style = MaterialTheme.typography.bodyMedium)
            Text(text = "Created: ${store.createdAt}", style = MaterialTheme.typography.bodySmall)

            val today = LocalDate.now().dayOfWeek.name.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(
                    Locale.ROOT
                ) else it.toString()
            }
            store.openingTimes[today]?.let { hours ->
                Text(
                    text = "Today's hours: ${hours.open} - ${hours.close}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}