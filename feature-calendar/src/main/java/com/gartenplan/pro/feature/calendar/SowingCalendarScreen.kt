package com.gartenplan.pro.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gartenplan.pro.domain.model.Plant
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SowingCalendarScreen(
    onNavigateBack: () -> Unit,
    onPlantClick: (String) -> Unit,
    viewModel: SowingCalendarViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showInfoDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aussaatkalender") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück")
                    }
                },
                actions = {
                    IconButton(onClick = { showInfoDialog = true }) {
                        Icon(Icons.Default.Info, "Info")
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Klimazone-Info
                ClimateZoneHeader(
                    climateZone = state.climateZone,
                    currentMonth = state.currentMonth
                )

                // Legende
                SowingLegend()

                // Pflanzen-Liste mit Monatsübersicht
                if (state.plants.isEmpty()) {
                    EmptySowingState()
                } else {
                    PlantSowingList(
                        plants = state.plants,
                        currentMonth = state.currentMonth.monthValue,
                        onPlantClick = onPlantClick
                    )
                }
            }
        }
    }

    // Info Dialog
    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("Aussaatkalender") },
            text = {
                Column {
                    Text(
                        "Der Aussaatkalender zeigt dir die optimalen Zeiträume für:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(SowingColors.indoor)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Vorziehen (Indoor)")
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(SowingColors.outdoor)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Direktsaat (Outdoor)")
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(SowingColors.harvest)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Ernte")
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Die Zeiträume basieren auf deiner Klimazone (${state.climateZone}). " +
                        "Je nach lokalem Wetter können die tatsächlichen Termine abweichen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("Verstanden")
                }
            }
        )
    }
}

object SowingColors {
    val indoor = Color(0xFF81C784) // Hellgrün
    val outdoor = Color(0xFF4CAF50) // Grün
    val harvest = Color(0xFFFF9800) // Orange
    val current = Color(0xFF2196F3) // Blau für aktuellen Monat
}

@Composable
private fun ClimateZoneHeader(
    climateZone: String,
    currentMonth: LocalDate
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Klimazone: $climateZone",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Basierend auf deinen Garteneinstellungen",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Aktueller Monat
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = SowingColors.current
            ) {
                Text(
                    text = currentMonth.month.getDisplayName(TextStyle.SHORT, Locale.GERMAN),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun SowingLegend() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        LegendItem(color = SowingColors.indoor, label = "Vorziehen")
        LegendItem(color = SowingColors.outdoor, label = "Direktsaat")
        LegendItem(color = SowingColors.harvest, label = "Ernte")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptySowingState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Grass,
            null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Keine Pflanzen",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Füge Pflanzen zu deinen Beeten hinzu, um den Aussaatkalender zu sehen.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PlantSowingList(
    plants: List<Plant>,
    currentMonth: Int,
    onPlantClick: (String) -> Unit
) {
    val months = Month.entries.map {
        it.getDisplayName(TextStyle.SHORT, Locale.GERMAN).take(1).uppercase()
    }
    val scrollState = rememberScrollState()

    LazyColumn(
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Monats-Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(start = 120.dp, end = 8.dp, top = 8.dp, bottom = 4.dp)
            ) {
                months.forEachIndexed { index, month ->
                    val isCurrent = index + 1 == currentMonth
                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .then(
                                if (isCurrent) Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(SowingColors.current.copy(alpha = 0.2f))
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = month,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCurrent) SowingColors.current
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        items(plants, key = { it.id }) { plant ->
            PlantSowingRow(
                plant = plant,
                currentMonth = currentMonth,
                scrollState = scrollState,
                onClick = { onPlantClick(plant.id) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlantSowingRow(
    plant: Plant,
    currentMonth: Int,
    scrollState: androidx.compose.foundation.ScrollState,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pflanzenname
            Column(
                modifier = Modifier
                    .width(112.dp)
                    .padding(8.dp)
            ) {
                Text(
                    text = plant.nameDE,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = plant.category.getDisplayName(true),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Monats-Balken
            Row(
                modifier = Modifier
                    .horizontalScroll(scrollState)
                    .padding(end = 8.dp)
            ) {
                (1..12).forEach { month ->
                    val isCurrent = month == currentMonth
                    val cellColor = when {
                        isInRange(month, plant.sowIndoorStart, plant.sowIndoorEnd) -> SowingColors.indoor
                        isInRange(month, plant.sowOutdoorStart, plant.sowOutdoorEnd) -> SowingColors.outdoor
                        isInRange(month, plant.harvestStart, plant.harvestEnd) -> SowingColors.harvest
                        else -> Color.Transparent
                    }

                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .height(24.dp)
                            .then(
                                if (isCurrent) Modifier
                                    .background(SowingColors.current.copy(alpha = 0.1f))
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (cellColor != Color.Transparent) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .height(16.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(cellColor)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun isInRange(month: Int, start: Int?, end: Int?): Boolean {
    if (start == null || end == null) return false
    return if (start <= end) {
        month in start..end
    } else {
        // Wrapping case (e.g., Nov-Feb: 11-2)
        month >= start || month <= end
    }
}
