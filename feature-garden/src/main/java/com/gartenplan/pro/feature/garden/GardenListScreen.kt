package com.gartenplan.pro.feature.garden

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GardenListScreen(
    onGardenClick: (String) -> Unit,
    onCreateGarden: () -> Unit,
    viewModel: GardenListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meine Gärten") }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateGarden,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Neuer Garten") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is GardenListUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is GardenListUiState.Empty -> {
                    EmptyGardenState(
                        onCreateGarden = onCreateGarden,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is GardenListUiState.Success -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.gardens, key = { it.id }) { garden ->
                            GardenCard(
                                garden = garden,
                                onClick = { onGardenClick(garden.id) },
                                onDelete = { viewModel.deleteGarden(garden.id) }
                            )
                        }
                        // Space for FAB
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
                is GardenListUiState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyGardenState(
    onCreateGarden: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Yard,
            null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Noch keine Gärten",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Erstelle deinen ersten Garten und\nzeichne deine Beete direkt ein!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onCreateGarden,
            modifier = Modifier.height(48.dp)
        ) {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.width(8.dp))
            Text("Garten erstellen")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GardenCard(
    garden: com.gartenplan.pro.domain.model.Garden,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Garden icon
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.GridOn,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    garden.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${garden.getWidthM()} × ${garden.getHeightM()} m  •  ${garden.beds.size} Beete",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Delete button
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    Icons.Default.MoreVert,
                    "Optionen",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.Delete, null) },
            title = { Text("Garten löschen?") },
            text = { Text("\"${garden.name}\" und alle Beete werden gelöscht.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Löschen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}
