package com.gartenplan.pro.feature.garden.canvas

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gartenplan.pro.domain.model.Plant

// ==================== PLANT PICKER BAR ====================

@Composable
fun PlantPickerBar(
    plants: List<Plant>,
    selectedPlantId: String?,
    onPlantDragStart: (String, Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Eco,
                    null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Pflanze ins Beet ziehen:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(8.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(plants, key = { it.id }) { plant ->
                    DraggablePlantChip(
                        plant = plant,
                        isSelected = plant.id == selectedPlantId,
                        onDragStart = { offset -> onPlantDragStart(plant.id, offset) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DraggablePlantChip(
    plant: Plant,
    isSelected: Boolean,
    onDragStart: (Offset) -> Unit
) {
    val emoji = PlantEmojis.forPlant(plant.nameDE)
    val backgroundColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surface,
        label = "chipBg"
    )

    Surface(
        modifier = Modifier
            .pointerInput(plant.id) {
                detectDragGestures(
                    onDragStart = { offset -> onDragStart(offset) },
                    onDrag = { _, _ -> },
                    onDragEnd = { }
                )
            },
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor,
        shadowElevation = 2.dp,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (emoji != null) {
                Text(emoji, fontSize = 16.sp)
                Spacer(Modifier.width(6.dp))
            }
            Text(
                plant.nameDE,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ==================== BED LABEL OVERLAY ====================

@Composable
fun BedLabelOverlay(
    currentName: String,
    currentEmoji: String?,
    onSave: (String, String?) -> Unit,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var selectedEmoji by remember { mutableStateOf(currentEmoji) }
    val focusManager = LocalFocusManager.current

    val emojis = listOf("🥕", "🍅", "🥬", "🥒", "🌶️", "🧅", "🥔", "🌽", "🌻", "🫛", "🥦", "🍆")

    Surface(
        modifier = Modifier
            .width(280.dp)
            .shadow(16.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Beet benennen",
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Close, "Schließen", modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            // Name input
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("z.B. Tomaten-Beet") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        onSave(name, selectedEmoji)
                    }
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(12.dp))

            // Emoji picker
            Text(
                "Symbol (optional)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(Modifier.height(8.dp))

            // Emoji grid (2 rows)
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    emojis.take(6).forEach { emoji ->
                        EmojiButton(
                            emoji = emoji,
                            isSelected = selectedEmoji == emoji,
                            onClick = {
                                selectedEmoji = if (selectedEmoji == emoji) null else emoji
                            }
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    emojis.drop(6).forEach { emoji ->
                        EmojiButton(
                            emoji = emoji,
                            isSelected = selectedEmoji == emoji,
                            onClick = {
                                selectedEmoji = if (selectedEmoji == emoji) null else emoji
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Delete button
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Löschen")
                }

                // Save button
                Button(
                    onClick = { onSave(name, selectedEmoji) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Fertig")
                }
            }
        }
    }
}

@Composable
private fun EmojiButton(
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        modifier = Modifier
            .size(40.dp)
            .then(
                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                else Modifier
            )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(emoji, fontSize = 20.sp)
        }
    }
}

// ==================== CANVAS HINT BAR ====================

@Composable
fun CanvasHintBar(
    mode: CanvasMode,
    hasSelection: Boolean,
    onEditLabel: () -> Unit,
    onDeselect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            when {
                mode == CanvasMode.DRAWING_BED -> {
                    Icon(
                        Icons.Default.TouchApp,
                        null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Finger heben um Beet zu erstellen",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                hasSelection -> {
                    // Edit label button
                    FilledTonalButton(
                        onClick = onEditLabel,
                        modifier = Modifier.height(40.dp)
                    ) {
                        Icon(Icons.Default.Label, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Benennen")
                    }

                    Spacer(Modifier.width(12.dp))

                    // Deselect button
                    OutlinedButton(
                        onClick = onDeselect,
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text("Fertig")
                    }
                }
                else -> {
                    Icon(
                        Icons.Default.Draw,
                        null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Zeichne mit dem Finger ein Beet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ==================== QUICK GARDEN SETUP ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickGardenSetupScreen(
    onGardenCreated: (name: String, widthCm: Int, heightCm: Int) -> Unit,
    onNavigateBack: () -> Unit
) {
    var gardenName by remember { mutableStateOf("") }
    var selectedPreset by remember { mutableStateOf<GardenPreset?>(null) }

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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Name input
            OutlinedTextField(
                value = gardenName,
                onValueChange = { gardenName = it },
                label = { Text("Name (optional)") },
                placeholder = { Text("Mein Garten") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(32.dp))

            Text(
                "Gartengröße wählen",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(16.dp))

            // Preset buttons
            GardenPreset.entries.forEach { preset ->
                PresetCard(
                    preset = preset,
                    isSelected = selectedPreset == preset,
                    onClick = { selectedPreset = preset }
                )
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.weight(1f))

            // Start button
            Button(
                onClick = {
                    selectedPreset?.let { preset ->
                        onGardenCreated(
                            gardenName.ifBlank { "Mein Garten" },
                            preset.widthCm,
                            preset.heightCm
                        )
                    }
                },
                enabled = selectedPreset != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Draw, null)
                Spacer(Modifier.width(8.dp))
                Text("Garten anlegen", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

enum class GardenPreset(
    val label: String,
    val description: String,
    val widthCm: Int,
    val heightCm: Int,
    val emoji: String
) {
    BALCONY("Balkon", "2×1 m", 200, 100, "🌿"),
    SMALL("Klein", "3×2 m", 300, 200, "🥬"),
    MEDIUM("Mittel", "5×4 m", 500, 400, "🥕"),
    LARGE("Groß", "8×6 m", 800, 600, "🌻"),
    CUSTOM("Eigene Größe", "Frei zeichnen", 1000, 1000, "✏️")
}

@Composable
private fun PresetCard(
    preset: GardenPreset,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
        border = if (isSelected) 
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
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
