package com.gartenplan.pro.feature.garden.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * 5.3 Bed Detail Screen – Inhaltliche Planung
 * 
 * Zweck: "Was passiert IN diesem Beet?"
 * - Beetname & Größe
 * - Geplante Pflanzen
 * - Warnungen (schlechte Nachbarn, Fruchtfolge)
 * 
 * KEIN Zeichnen hier! Reines Planen & Entscheiden.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BedDetailScreen(
    bedId: String,
    onNavigateBack: () -> Unit,
    onOpenPlantPicker: () -> Unit,
    viewModel: BedDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    
    LaunchedEffect(bedId) {
        viewModel.loadBed(bedId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.bed?.displayName() ?: "Beet") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleEditName() }) {
                        Icon(Icons.Default.Edit, "Umbenennen")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onOpenPlantPicker,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Pflanze hinzufügen") }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Beet-Info Card
                item {
                    BedInfoCard(
                        bed = state.bed,
                        isEditingName = state.isEditingName,
                        editedName = state.editedName,
                        onNameChange = { viewModel.updateName(it) },
                        onSaveName = { viewModel.saveName() },
                        onCancelEdit = { viewModel.cancelEditName() }
                    )
                }
                
                // Warnungen (wenn vorhanden)
                if (state.warnings.isNotEmpty()) {
                    item {
                        WarningsCard(warnings = state.warnings)
                    }
                }
                
                // Geplante Pflanzen
                item {
                    Text(
                        "Geplante Pflanzen",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                if (state.plants.isEmpty()) {
                    item {
                        EmptyPlantsHint(onAddPlant = onOpenPlantPicker)
                    }
                } else {
                    items(state.plants) { plant ->
                        PlantCard(
                            plant = plant,
                            onRemove = { viewModel.removePlant(plant.id) }
                        )
                    }
                }
                
                // Platz für FAB
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

// ==================== BED INFO CARD ====================

@Composable
private fun BedInfoCard(
    bed: EditorBed?,
    isEditingName: Boolean,
    editedName: String,
    onNameChange: (String) -> Unit,
    onSaveName: () -> Unit,
    onCancelEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            if (isEditingName) {
                // Edit Mode
                OutlinedTextField(
                    value = editedName,
                    onValueChange = onNameChange,
                    label = { Text("Beetname") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onCancelEdit) {
                        Text("Abbrechen")
                    }
                    Button(onClick = onSaveName) {
                        Text("Speichern")
                    }
                }
            } else {
                // View Mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            bed?.displayName() ?: "Beet",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(Modifier.height(4.dp))
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            InfoChip(
                                icon = Icons.Default.Straighten,
                                text = bed?.sizeText() ?: ""
                            )
                            InfoChip(
                                icon = Icons.Default.SquareFoot,
                                text = bed?.areaText() ?: ""
                            )
                        }
                    }
                    
                    // Farb-Indikator
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(bed?.let { BedColors.parse(it.colorHex) } ?: Color.Gray)
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon, null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ==================== WARNINGS CARD ====================

data class BedWarning(
    val type: WarningType,
    val message: String,
    val plantNames: List<String> = emptyList()
)

enum class WarningType {
    BAD_NEIGHBORS,      // Schlechte Nachbarn
    CROP_ROTATION,      // Fruchtfolge-Problem
    OVERCROWDED         // Zu viele Pflanzen
}

@Composable
private fun WarningsCard(warnings: List<BedWarning>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Warning,
                    null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Hinweise",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            Spacer(Modifier.height(8.dp))
            
            warnings.forEach { warning ->
                WarningItem(warning)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun WarningItem(warning: BedWarning) {
    val (icon, iconColor) = when (warning.type) {
        WarningType.BAD_NEIGHBORS -> Icons.Default.HeartBroken to MaterialTheme.colorScheme.error
        WarningType.CROP_ROTATION -> Icons.Default.Loop to MaterialTheme.colorScheme.tertiary
        WarningType.OVERCROWDED -> Icons.Default.Groups to MaterialTheme.colorScheme.secondary
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                warning.message,
                style = MaterialTheme.typography.bodyMedium
            )
            if (warning.plantNames.isNotEmpty()) {
                Text(
                    warning.plantNames.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ==================== PLANTS ====================

data class BedPlant(
    val id: String,
    val name: String,
    val emoji: String? = null,
    val quantity: Int = 1
)

@Composable
private fun EmptyPlantsHint(onAddPlant: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Eco,
                null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Noch keine Pflanzen geplant",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                "Füge Pflanzen hinzu, um dein Beet zu planen",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onAddPlant) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Pflanze auswählen")
            }
        }
    }
}

@Composable
private fun PlantCard(
    plant: BedPlant,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji oder Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (plant.emoji != null) {
                    Text(plant.emoji, style = MaterialTheme.typography.headlineSmall)
                } else {
                    Icon(Icons.Default.Eco, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            
            Spacer(Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    plant.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "Menge: ${plant.quantity}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Close,
                    "Entfernen",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
