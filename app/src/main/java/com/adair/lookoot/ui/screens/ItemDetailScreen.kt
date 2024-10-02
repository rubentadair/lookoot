package com.adair.lookoot.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adair.lookoot.DisplayItemDetails
import com.adair.lookoot.models.Item
import com.adair.lookoot.models.Store
import com.adair.lookoot.viewmodels.LookootViewModel

// Displays details of a specific item, including tags, reviews, and the option to edit the item if the user is the store owner.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailScreen(
    itemId: String,
    viewModel: LookootViewModel,
    onNavigateToEditItem: (String) -> Unit,
    onBackClick: () -> Unit
) {
    var item by remember { mutableStateOf<Item?>(null) }
    val itemReviews by viewModel.itemReviews.collectAsState()
    var showAddTagDialog by remember { mutableStateOf(false) }
    var newTag by remember { mutableStateOf("") }
    val currentUser by viewModel.currentUser.collectAsState()
    var store by remember { mutableStateOf<Store?>(null) }

    val isStoreOwner = currentUser?.id == store?.ownerId
    // Checks if the current user is the owner of the store that the item belongs to.
    LaunchedEffect(itemId) {
        // Loads the item and its reviews when the composable is first rendered.
        Log.d("ItemDetailScreen", "Loading item details for itemId: $itemId")
        try {
            item = viewModel.getItem(itemId)
            viewModel.getItemReviews(itemId)
            store = item?.storeId?.let { viewModel.getStore(it) }
            Log.d("ItemDetailScreen", "Loaded item: $item")
        } catch (e: Exception) {
            Log.e("ItemDetailScreen", "Error loading item details: ${e.message}", e)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(store?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        // Back button to navigate to the previous screen.
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isStoreOwner) {
                        // If the user is the store owner, show the edit button.
                        IconButton(onClick = { onNavigateToEditItem(itemId) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Item")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        // Main content displaying item details.
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            item {
                // Displays the item details and tags.
                item?.let { currentItem ->
                    DisplayItemDetails(currentItem)

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Tags:",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    // Replace ItemTags with a simple display of tags
                    Text(currentItem.tagsList.joinToString(", "))


                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Reviews", style = MaterialTheme.typography.titleLarge)
                } ?: Text("Item not found") // Displays a message if the item is not found.
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                // Button to add a review for the item.
                Button(
                    onClick = {
                        Log.d(
                            "ItemDetailScreen",
                            "Navigating to add review screen for itemId: $itemId"
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add Review")
                }
            }
        }
    }
}