package com.gartenplan.pro.feature.garden

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gartenplan.pro.domain.model.Bed
import com.gartenplan.pro.domain.model.Garden

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GardenDetailScreen(
    gardenId: String,
    onNavigateBack: () -> Unit,
    onBedClick: (String) -> Unit,
    onAddBed: () -> Unit,
    viewModel: GardenDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(gardenId) {
        viewModel.loadGarden(gardenId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        when (val state = uiState) {
                            is GardenDetailUiState.Success -> state.garden.name
                            else -> "Garten"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Edit garden */ }) {
                        Icon(Icons.Default.Edit, "Bearbeiten")
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState is GardenDetailUiState.Success) {
                FloatingActionButton(onClick = onAddBed) {
                    Icon(Icons.Default.Add, "Beet hinzufügen")
                }
            }
        }
    ) { padding ->
        when (val state = uiState) {
            is GardenDetailUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is GardenDetailUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is GardenDetailUiState.Success -> {
                GardenDetailContent(
                    garden = state.garden,
                    beds = state.beds,
                    onBedClick = onBedClick,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun GardenDetailContent(
    garden: Garden,
    beds: List<Bed>,
    onBedClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Garden Info Card
        item {
            GardenInfoCard(garden)
        }

        // Garden Preview (simple visual)
        item {
            GardenPreview(garden, beds, onBedClick)
        }

        // Beds Section
        item {
            Text(
                text = "Beete (${beds.size})",
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (beds.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.GridOn,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Noch keine Beete angelegt")
                        Text(
                            "Tippe auf + um ein Beet hinzuzufügen",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        } else {
            items(beds, key = { it.id }) { bed ->
                BedListItem(bed = bed, onClick = { onBedClick(bed.id) })
            }
        }
    }
}

@Composable
private fun GardenInfoCard(garden: Garden) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoChip(
                    icon = Icons.Default.Straighten,
                    label = "${garden.widthCm / 100.0}m × ${garden.heightCm / 100.0}m"
                )
                InfoChip(
                    icon = Icons.Default.SquareFoot,
                    label = "%.1f m²".format((garden.widthCm * garden.heightCm) / 10000.0)
                )
                InfoChip(
                    icon = Icons.Default.Thermostat,
                    label = "Zone ${garden.climateZone}"
                )
            }
        }
    }
}

@Composable
private fun InfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun GardenPreview(
    garden: Garden,
    beds: List<Bed>,
    onBedClick: (String) -> Unit
) {
    Card {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(garden.widthCm.toFloat() / garden.heightCm.toFloat().coerceAtLeast(1f))
                .padding(8.dp)
                .background(
                    color = Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(4.dp)
                )
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(4.dp)
                )
        ) {
            // Render beds as colored rectangles
            beds.forEach { bed ->
                val xPercent = bed.positionX.toFloat() / garden.widthCm
                val yPercent = bed.positionY.toFloat() / garden.heightCm
                val widthPercent = bed.widthCm.toFloat() / garden.widthCm
                val heightPercent = bed.heightCm.toFloat() / garden.heightCm

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = (xPercent * 100).dp,
                            top = (yPercent * 100).dp
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(widthPercent)
                            .fillMaxHeight(heightPercent)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                color = try {
                                    Color(android.graphics.Color.parseColor(bed.colorHex))
                                } catch (e: Exception) {
                                    MaterialTheme.colorScheme.primary
                                }
                            )
                            .clickable { onBedClick(bed.id) }
                    )
                }
            }

            if (beds.isEmpty()) {
                Text(
                    text = "Tippe + um Beete hinzuzufügen",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BedListItem(
    bed: Bed,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color indicator
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        try {
                            Color(android.graphics.Color.parseColor(bed.colorHex))
                        } catch (e: Exception) {
                            MaterialTheme.colorScheme.primary
                        }
                    )
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bed.name,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "${bed.widthCm}cm × ${bed.heightCm}cm",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
