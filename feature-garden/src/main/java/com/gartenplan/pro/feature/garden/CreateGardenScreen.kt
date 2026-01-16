package com.gartenplan.pro.feature.garden

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Screen zum Erstellen eines neuen Gartens.
 * Einfach: Name + Größe eingeben oder Preset wählen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGardenScreen(
    onGardenCreated: (name: String, widthM: Float, heightM: Float) -> Unit,
    onNavigateBack: () -> Unit
) {
    var gardenName by remember { mutableStateOf("") }
    var selectedPreset by remember { mutableStateOf<GardenPreset?>(null) }
    var customWidth by remember { mutableStateOf("") }
    var customHeight by remember { mutableStateOf("") }
    var useCustomSize by remember { mutableStateOf(false) }

    val canCreate = gardenName.isNotBlank() && (
        selectedPreset != null || 
        (useCustomSize && customWidth.toFloatOrNull() != null && customHeight.toFloatOrNull() != null)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Neuer Garten") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, "Abbrechen")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
        ) {
            // Name
            OutlinedTextField(
                value = gardenName,
                onValueChange = { gardenName = it },
                label = { Text("Gartenname") },
                placeholder = { Text("z.B. Hintergarten") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(24.dp))

            Text(
                "Gartengröße",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(12.dp))

            // Presets
            GardenPreset.entries.forEach { preset ->
                PresetCard(
                    preset = preset,
                    isSelected = selectedPreset == preset && !useCustomSize,
                    onClick = {
                        selectedPreset = preset
                        useCustomSize = false
                    }
                )
                Spacer(Modifier.height(8.dp))
            }

            // Eigene Größe
            Card(
                onClick = { useCustomSize = true; selectedPreset = null },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (useCustomSize) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
                border = if (useCustomSize) 
                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary) 
                else null
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("✏️", fontSize = 24.sp)
                        Spacer(Modifier.width(16.dp))
                        Text(
                            "Eigene Größe",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.weight(1f))
                        if (useCustomSize) {
                            Icon(
                                Icons.Default.CheckCircle,
                                null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    if (useCustomSize) {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = customWidth,
                                onValueChange = { customWidth = it.filter { c -> c.isDigit() || c == '.' } },
                                label = { Text("Breite (m)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = customHeight,
                                onValueChange = { customHeight = it.filter { c -> c.isDigit() || c == '.' } },
                                label = { Text("Länge (m)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Erstellen Button
            Button(
                onClick = {
                    val (width, height) = if (useCustomSize) {
                        (customWidth.toFloatOrNull() ?: 5f) to (customHeight.toFloatOrNull() ?: 4f)
                    } else {
                        selectedPreset?.let { it.widthM to it.heightM } ?: (5f to 4f)
                    }
                    onGardenCreated(gardenName, width, height)
                },
                enabled = canCreate,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Check, null)
                Spacer(Modifier.width(8.dp))
                Text("Garten erstellen", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

enum class GardenPreset(
    val label: String,
    val description: String,
    val widthM: Float,
    val heightM: Float,
    val emoji: String
) {
    BALCONY("Balkon", "2 × 1 m", 2f, 1f, "🌿"),
    SMALL("Klein", "3 × 2 m", 3f, 2f, "🥬"),
    MEDIUM("Mittel", "5 × 4 m", 5f, 4f, "🥕"),
    LARGE("Groß", "8 × 6 m", 8f, 6f, "🌻"),
    ALLOTMENT("Schrebergarten", "12 × 8 m", 12f, 8f, "🏡")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresetCard(
    preset: GardenPreset,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isSelected) 
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary) 
        else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(preset.emoji, fontSize = 28.sp)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    preset.label,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    preset.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
