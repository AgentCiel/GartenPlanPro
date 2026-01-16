package com.gartenplan.pro.feature.garden

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gartenplan.pro.domain.model.Garden

/**
 * 5.1 Garden Overview Screen
 * 
 * Zweck: "Welchen Garten möchte ich gerade bearbeiten?"
 * 
 * Was der Nutzer hier tun kann:
 * - Alle existierenden Gärten sehen
 * - Einen neuen Garten erstellen
 * - Einen Garten auswählen und in die Planung wechseln
 * 
 * WICHTIG: Dieser Screen ist rein organisatorisch, keine Planung!
 */
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
                        // Platz für FAB
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
                is GardenListUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Error,
                            null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            state.message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
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
            "Erstelle deinen ersten Garten und plane deine Beete!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onCreateGarden) {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.width(8.dp))
            Text("Garten erstellen")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GardenCard(
    garden: Garden,
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
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mini-Vorschau
            GardenMiniPreview(
                garden = garden,
                modifier = Modifier.size(72.dp)
            )

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
                    "${garden.getWidthM()} × ${garden.getHeightM()} m",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${garden.beds.size} Beete",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Mehr-Button
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

/**
 * Grobe Mini-Vorschau des Gartens (keine Details)
 */
@Composable
private fun GardenMiniPreview(
    garden: Garden,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFE8F5E9)
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            val gardenAspect = garden.widthCm.toFloat() / garden.heightCm.toFloat()
            val canvasAspect = size.width / size.height
            
            val (drawWidth, drawHeight) = if (gardenAspect > canvasAspect) {
                size.width to size.width / gardenAspect
            } else {
                size.height * gardenAspect to size.height
            }
            
            val offsetX = (size.width - drawWidth) / 2
            val offsetY = (size.height - drawHeight) / 2
            
            // Gartenfläche
            drawRoundRect(
                color = Color(0xFFA5D6A7),
                topLeft = Offset(offsetX, offsetY),
                size = Size(drawWidth, drawHeight),
                cornerRadius = CornerRadius(4f)
            )
            
            // Rahmen
            drawRoundRect(
                color = Color(0xFF4CAF50),
                topLeft = Offset(offsetX, offsetY),
                size = Size(drawWidth, drawHeight),
                cornerRadius = CornerRadius(4f),
                style = Stroke(width = 2f)
            )
            
            // Beete als kleine Rechtecke (grob, keine Details)
            garden.beds.forEach { bed ->
                val bedX = offsetX + (bed.positionX.toFloat() / garden.widthCm) * drawWidth
                val bedY = offsetY + (bed.positionY.toFloat() / garden.heightCm) * drawHeight
                val bedW = (bed.widthCm.toFloat() / garden.widthCm) * drawWidth
                val bedH = (bed.heightCm.toFloat() / garden.heightCm) * drawHeight
                
                drawRoundRect(
                    color = Color(0xFF795548).copy(alpha = 0.7f),
                    topLeft = Offset(bedX, bedY),
                    size = Size(bedW, bedH),
                    cornerRadius = CornerRadius(2f)
                )
            }
        }
    }
}
