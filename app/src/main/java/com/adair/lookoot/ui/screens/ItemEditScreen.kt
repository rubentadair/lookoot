package com.adair.lookoot.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adair.lookoot.models.Item
import com.adair.lookoot.viewmodels.LookootViewModel

// Displays a screen to edit item details such as name, description, price, tags, and categories.
// It fetches the current item details and updates the ViewModel with any changes.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemEditScreen(
    itemId: String,
    storeId: String,
    onNavigateBack: () -> Unit,
    viewModel: LookootViewModel = viewModel()
) {
    var item by remember { mutableStateOf<Item?>(null) }
    var editedName by remember { mutableStateOf("") }
    var editedDescription by remember { mutableStateOf("") }
    var editedPrice by remember { mutableStateOf("") }
    var editedTags by remember { mutableStateOf<List<String>>(emptyList()) }
    var editedCategories by remember { mutableStateOf<List<String>>(emptyList()) }
    var expanded by remember { mutableStateOf(false) }  // Used to control dropdown expansion

    // Example categories and tags (this should come from a data source)
    val allCategories = listOf(
        "Electronics", "Clothing", "Home", "Books", "Tools", "Food", "Beauty", "Sports"
    )
    val allTags = listOf(
        "Sale", "New", "Popular", "Limited", "Exclusive", "Organic", "Premium", "Budget"
    )

    // Loads item data when the screen is first displayed.
    LaunchedEffect(itemId) {
        item = viewModel.getItem(itemId)
        item?.let {
            editedName = it.name
            editedDescription = it.description
            editedPrice = it.price.toString()
            editedTags = it.tagsList
            editedCategories = it.categories
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Item") },
                navigationIcon = {
                    // Back button to navigate back to the previous screen.
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        // Main content layout with input fields for editing item details.
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            // Item name input
            OutlinedTextField(
                value = editedName,
                onValueChange = { editedName = it },
                label = { Text("Item Name") },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )

            // Description input
            OutlinedTextField(
                value = editedDescription,
                onValueChange = { editedDescription = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )

            // Price input
            OutlinedTextField(
                value = editedPrice,
                onValueChange = {
                    if (it.isEmpty() || it.toDoubleOrNull() != null) {
                        editedPrice = it
                    }
                },
                label = { Text("Price") },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )

            // Tags input with suggestions
            TagInputForEditScreen(
                tags = editedTags,
                onTagsChanged = { newTags ->
                    editedTags = newTags
                },
                allTags = allTags
            )

            // Category input with suggestions
            CategoryInputForEditScreen(
                categories = editedCategories,
                onCategoriesChanged = { newCategories ->
                    editedCategories = newCategories
                },
                allCategories = allCategories
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Save button
            Button(
                onClick = {
                    item?.let {
                        val updatedItem = it.copy(
                            name = editedName,
                            description = editedDescription,
                            price = editedPrice.toDoubleOrNull() ?: it.price,
                            tags = editedTags,
                            categories = editedCategories
                        )
                        viewModel.updateItem(updatedItem) { success ->
                            if (success) {
                                onNavigateBack()
                            }
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
// Displays a tag input field with suggestions and the ability to add or remove tags.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagInputForEditScreen(
    tags: List<String>,
    onTagsChanged: (List<String>) -> Unit,
    allTags: List<String>
) {
    var newTag by remember { mutableStateOf("") }
    var showTagSuggestions by remember { mutableStateOf(false) }

    Column {
        Text("Tags", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newTag,
                onValueChange = {
                    newTag = it.replace(Regex("[ \\n]"), "")
                    showTagSuggestions = newTag.isNotEmpty()
                },
                label = { Text("Add Tag") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (newTag.isNotEmpty() && newTag !in tags) {
                        onTagsChanged(tags + newTag)
                        newTag = ""
                        showTagSuggestions = false
                    }
                })
            )
            IconButton(onClick = {
                if (newTag.isNotEmpty() && newTag !in tags) {
                    onTagsChanged(tags + newTag)
                    newTag = ""
                    showTagSuggestions = false
                }
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Tag")
            }
        }

        // Suggestions for tags based on user input.
        if (showTagSuggestions) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                items(allTags.filter { it.contains(newTag, ignoreCase = true) }) { tag ->
                    Text(
                        text = tag,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (tag !in tags) {
                                    onTagsChanged(tags + tag)
                                    newTag = ""
                                    showTagSuggestions = false
                                }
                            }
                            .padding(vertical = 8.dp)
                    )
                }
            }
        }

        // Displays the added tags with the ability to remove them.
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tags.forEach { tag ->
                FilterChip(
                    selected = true,
                    onClick = { onTagsChanged(tags - tag) },
                    label = { Text(tag) },
                    trailingIcon = {
                        Icon(Icons.Default.Close, contentDescription = "Remove tag")
                    }
                )
            }
        }
    }
}
// Displays a category input field with suggestions and the ability to add or remove categories.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryInputForEditScreen(
    categories: List<String>,
    onCategoriesChanged: (List<String>) -> Unit,
    allCategories: List<String>
) {
    var newCategory by remember { mutableStateOf("") }
    var showCategorySuggestions by remember { mutableStateOf(false) }

    Column {
        Text("Categories", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newCategory,
                onValueChange = {
                    newCategory = it.trim()
                    showCategorySuggestions = newCategory.isNotEmpty()
                },
                label = { Text("Add Category") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (newCategory.isNotEmpty() && newCategory !in categories) {
                        onCategoriesChanged(categories + newCategory)
                        newCategory = ""
                        showCategorySuggestions = false
                    }
                })
            )
            IconButton(onClick = {
                if (newCategory.isNotEmpty() && newCategory !in categories) {
                    onCategoriesChanged(categories + newCategory)
                    newCategory = ""
                    showCategorySuggestions = false
                }
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Category")
            }
        }

        // Category suggestions
        if (showCategorySuggestions) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                items(allCategories.filter { it.contains(newCategory, ignoreCase = true) }) { category ->
                    Text(
                        text = category,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (category !in categories) {
                                    onCategoriesChanged(categories + category)
                                    newCategory = ""
                                    showCategorySuggestions = false
                                }
                            }
                            .padding(vertical = 8.dp)
                    )
                }
            }
        }

        // Display added categories
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { category ->
                FilterChip(
                    selected = true,
                    onClick = { onCategoriesChanged(categories - category) },
                    label = { Text(category) },
                    trailingIcon = {
                        Icon(Icons.Default.Close, contentDescription = "Remove category")
                    }
                )
            }
        }
    }
}
