package com.gartenplan.pro.feature.garden

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Yard
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gartenplan.pro.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GardenListScreen(
    onGardenClick: (String) -> Unit,
    onCreateGarden: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meine Gärten") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateGarden) {
                Icon(Icons.Default.Add, "Garten erstellen")
            }
        }
    ) { padding ->
        // TODO: Implement garden list
        EmptyState(
            icon = Icons.Default.Yard,
            title = "Noch keine Gärten",
            message = "Erstelle deinen ersten Garten um loszulegen!",
            actionLabel = "Garten erstellen",
            onAction = onCreateGarden,
            modifier = Modifier.padding(padding)
        )
    }
}
