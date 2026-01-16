package com.gartenplan.pro.feature.garden.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gartenplan.pro.core.constants.PlantCategory
import com.gartenplan.pro.domain.model.Plant

/**
 * 5.4 Plant Picker Dialog – Auswahlhilfe
 *
 * Zweck: "Welche Pflanze will ich in dieses Beet setzen?"
 * - Suche
 * - Filter (Gemüse, Kräuter, etc.)
 * - Favoriten
 * - Auswahl → Pflanze landet im Beet
 *
 * Noch keine Zeitplanung hier - nur Auswahl & Zuordnung
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantPickerDialog(
    plants: List<Plant>,
    onPlantSelected: (Plant) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<PlantCategory?>(null) }
    var showFavoritesOnly by remember { mutableStateOf(false) }

    // Filter Pflanzen
    val filteredPlants = remember(plants, searchQuery, selectedCategory, showFavoritesOnly) {
        plants.filter { plant ->
            val matchesSearch = searchQuery.isEmpty() ||
                    plant.nameDE.contains(searchQuery, ignoreCase = true) ||
                    plant.nameEN.contains(searchQuery, ignoreCase = true)

            val matchesCategory = selectedCategory == null || plant.category == selectedCategory

            // TODO: Favoriten aus DB laden
            val matchesFavorites = !showFavoritesOnly // || plant.isFavorite

            matchesSearch && matchesCategory && matchesFavorites
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column {
                // Header
                TopAppBar(
                    title = { Text("Pflanze auswählen") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, "Schließen")
                        }
                    }
                )

                // Suchfeld
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Suchen...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, "Löschen")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                // Filter Chips
                FilterChipsRow(
                    selectedCategory = selectedCategory,
                    showFavoritesOnly = showFavoritesOnly,
                    onCategorySelected = { selectedCategory = it },
                    onFavoritesToggle = { showFavoritesOnly = !showFavoritesOnly },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(Modifier.height(8.dp))

                // Pflanzen-Liste
                if (filteredPlants.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.SearchOff,
                                null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Keine Pflanzen gefunden",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredPlants, key = { it.id }) { plant ->
                            PlantPickerItem(
                                plant = plant,
                                onClick = { onPlantSelected(plant) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChipsRow(
    selectedCategory: PlantCategory?,
    showFavoritesOnly: Boolean,
    onCategorySelected: (PlantCategory?) -> Unit,
    onFavoritesToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Favoriten
        FilterChip(
            selected = showFavoritesOnly,
            onClick = onFavoritesToggle,
            label = { Text("⭐ Favoriten") },
            leadingIcon = if (showFavoritesOnly) {
                { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
            } else null
        )

        // Alle
        FilterChip(
            selected = selectedCategory == null,
            onClick = { onCategorySelected(null) },
            label = { Text("Alle") }
        )

        // Kategorien
        PlantCategory.entries.forEach { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                label = { Text("${getCategoryEmoji(category)} ${getCategoryName(category)}") }
            )
        }
    }
}

@Composable
private fun PlantPickerItem(
    plant: Plant,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(getCategoryColor(plant.category).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    getPlantEmoji(plant.nameDE) ?: getCategoryEmoji(plant.category),
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    plant.nameDE,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    plant.nameEN,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Kategorie-Badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = getCategoryColor(plant.category).copy(alpha = 0.2f)
            ) {
                Text(
                    getCategoryName(plant.category),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = getCategoryColor(plant.category)
                )
            }

            Spacer(Modifier.width(8.dp))

            Icon(
                Icons.Default.Add,
                "Hinzufügen",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// ==================== HELPER ====================

private fun getCategoryName(category: PlantCategory): String = when (category) {
    PlantCategory.VEGETABLE -> "Gemüse"
    PlantCategory.FRUIT -> "Obst"
    PlantCategory.HERB -> "Kräuter"
    PlantCategory.FLOWER -> "Blumen"
    PlantCategory.GREEN_MANURE -> "Gründüngung"
}

private fun getCategoryEmoji(category: PlantCategory): String = when (category) {
    PlantCategory.VEGETABLE -> "🥬"
    PlantCategory.FRUIT -> "🍓"
    PlantCategory.HERB -> "🌿"
    PlantCategory.FLOWER -> "🌸"
    PlantCategory.GREEN_MANURE -> "🌾"
}

@Composable
private fun getCategoryColor(category: PlantCategory): Color = when (category) {
    PlantCategory.VEGETABLE -> Color(0xFF4CAF50)
    PlantCategory.FRUIT -> Color(0xFFE91E63)
    PlantCategory.HERB -> Color(0xFF8BC34A)
    PlantCategory.FLOWER -> Color(0xFF9C27B0)
    PlantCategory.GREEN_MANURE -> Color(0xFF795548)
}

private fun getPlantEmoji(name: String): String? {
    val lower = name.lowercase()
    return when {
        lower.contains("tomate") -> "🍅"
        lower.contains("karotte") || lower.contains("möhre") -> "🥕"
        lower.contains("salat") -> "🥬"
        lower.contains("gurke") -> "🥒"
        lower.contains("paprika") || lower.contains("chili") -> "🌶️"
        lower.contains("zwiebel") -> "🧅"
        lower.contains("knoblauch") -> "🧄"
        lower.contains("kartoffel") -> "🥔"
        lower.contains("mais") -> "🌽"
        lower.contains("kürbis") -> "🎃"
        lower.contains("erdbeere") -> "🍓"
        lower.contains("bohne") -> "🫘"
        lower.contains("erbse") -> "🫛"
        lower.contains("brokkoli") -> "🥦"
        lower.contains("blumenkohl") -> "🥬"
        lower.contains("aubergine") -> "🍆"
        lower.contains("sonnenblume") -> "🌻"
        lower.contains("basilikum") -> "🌿"
        lower.contains("petersilie") -> "🌿"
        lower.contains("minze") -> "🌿"
        else -> null
    }
}