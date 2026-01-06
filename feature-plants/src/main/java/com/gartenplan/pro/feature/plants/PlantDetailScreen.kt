package com.gartenplan.pro.feature.plants

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gartenplan.pro.core.constants.CompanionType
import com.gartenplan.pro.core.constants.NutrientLevel
import com.gartenplan.pro.domain.model.CompanionInfo
import com.gartenplan.pro.domain.model.Plant
import com.gartenplan.pro.ui.components.*
import com.gartenplan.pro.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantDetailScreen(
    plantId: String,
    onBack: () -> Unit,
    viewModel: PlantDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(plantId) {
        viewModel.loadPlant(plantId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text((uiState as? PlantDetailUiState.Success)?.plant?.nameDE ?: "Pflanze") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück")
                    }
                },
                actions = {
                    (uiState as? PlantDetailUiState.Success)?.plant?.let { plant ->
                        IconButton(onClick = { viewModel.toggleFavorite() }) {
                            Icon(
                                imageVector = if (plant.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = "Favorit",
                                tint = if (plant.isFavorite) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is PlantDetailUiState.Loading -> LoadingScreen(modifier = Modifier.padding(padding))
            is PlantDetailUiState.Success -> PlantDetailContent(state.plant, state.goodCompanions, state.badCompanions, Modifier.padding(padding))
            is PlantDetailUiState.Error -> ErrorScreen(state.message, { viewModel.loadPlant(plantId) }, Modifier.padding(padding))
        }
    }
}

@Composable
private fun PlantDetailContent(plant: Plant, goodCompanions: List<CompanionInfo>, badCompanions: List<CompanionInfo>, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { PlantHeaderCard(plant) }
        item { TimingCard(plant) }
        item { RequirementsCard(plant) }
        item { SpacingCard(plant) }
        if (plant.description != null || plant.careTips != null) item { DescriptionCard(plant) }
        if (goodCompanions.isNotEmpty()) item { CompanionsSection("Gute Nachbarn", goodCompanions, CompanionType.GOOD) }
        if (badCompanions.isNotEmpty()) item { CompanionsSection("Schlechte Nachbarn", badCompanions, CompanionType.BAD) }
        if (plant.diseases.isNotEmpty() || plant.pests.isNotEmpty()) item { ProblemsCard(plant) }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun PlantHeaderCard(plant: Plant) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(80.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Eco, null, Modifier.size(48.dp), MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(plant.nameDE, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    plant.latinName?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)) }
                    Spacer(Modifier.height(4.dp))
                    AssistChip(onClick = {}, label = { Text(plant.category.getDisplayName()) })
                }
            }
        }
    }
}

@Composable
private fun TimingCard(plant: Plant) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Aussaat & Ernte", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            val indoorStart = plant.sowIndoorStart
            val indoorEnd = plant.sowIndoorEnd
            if (indoorStart != null && indoorEnd != null) {
                InfoRow(
                    Icons.Default.Home,
                    "Vorziehen",
                    "${monthName(indoorStart)} - ${monthName(indoorEnd)}"
                )
            }
            val outdoorStart = plant.sowOutdoorStart
            val outdoorEnd = plant.sowOutdoorEnd
            if (outdoorStart != null && outdoorEnd != null) {
                InfoRow(
                    Icons.Default.WbSunny,
                    "Direktsaat",
                    "${monthName(outdoorStart)} - ${monthName(outdoorEnd)}"
                )
            }
            InfoRow(Icons.Default.Agriculture, "Ernte", "${monthName(plant.harvestStart)} - ${monthName(plant.harvestEnd)}")
            plant.daysToHarvest?.let { InfoRow(Icons.Default.Timer, "Kulturzeit", "$it Tage") }
        }
    }
}

@Composable
private fun RequirementsCard(plant: Plant) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Anforderungen", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                RequirementItem(Icons.Default.Compost, "Nährstoffe", plant.nutrientDemand.getDisplayName(), when (plant.nutrientDemand) { NutrientLevel.HIGH -> NutrientHigh; NutrientLevel.MEDIUM -> NutrientMedium; NutrientLevel.LOW -> NutrientLow })
                RequirementItem(Icons.Default.WaterDrop, "Wasser", plant.waterDemand.getDisplayName(), Teal60)
                RequirementItem(Icons.Default.WbSunny, "Sonne", plant.sunRequirement.getDisplayName(), Color(0xFFFFC107))
            }
        }
    }
}

@Composable
private fun RequirementItem(icon: ImageVector, label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, Modifier.size(32.dp), color)
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SpacingCard(plant: Plant) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Platzbedarf", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            InfoRow(Icons.Default.SwapHoriz, "Abstand in Reihe", "${plant.spacingInRowCm} cm")
            InfoRow(Icons.Default.SwapVert, "Reihenabstand", "${plant.spacingBetweenRowsCm} cm")
            val depth = plant.plantDepthCm
            if (depth != null && depth > 0) {
                InfoRow(Icons.Default.Layers, "Pflanztiefe", "$depth cm")
            }
        }
    }
}

@Composable
private fun DescriptionCard(plant: Plant) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            plant.description?.let { Text("Beschreibung", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(8.dp)); Text(it) }
            plant.careTips?.let { if (plant.description != null) Spacer(Modifier.height(16.dp)); Text("Pflegetipps", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(8.dp)); Text(it) }
        }
    }
}

@Composable
private fun CompanionsSection(title: String, companions: List<CompanionInfo>, type: CompanionType) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CompanionIndicator(type, size = CompanionIndicatorSize.Medium)
            Spacer(Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(companions) { AssistChip(onClick = {}, label = { Text(it.plant.nameDE) }) }
        }
    }
}

@Composable
private fun ProblemsCard(plant: Plant) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Krankheiten & Schädlinge", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            if (plant.diseases.isNotEmpty()) Text("Krankheiten: ${plant.diseases.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
            if (plant.pests.isNotEmpty()) Text("Schädlinge: ${plant.pests.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(20.dp), MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

private fun monthName(month: Int) = listOf("Jan", "Feb", "Mär", "Apr", "Mai", "Jun", "Jul", "Aug", "Sep", "Okt", "Nov", "Dez").getOrElse(month - 1) { "?" }

sealed interface PlantDetailUiState {
    data object Loading : PlantDetailUiState
    data class Success(val plant: Plant, val goodCompanions: List<CompanionInfo>, val badCompanions: List<CompanionInfo>) : PlantDetailUiState
    data class Error(val message: String) : PlantDetailUiState
}
