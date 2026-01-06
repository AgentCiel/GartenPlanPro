package com.gartenplan.pro.feature.plants

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gartenplan.pro.core.constants.PlantCategory
import com.gartenplan.pro.domain.model.Plant
import com.gartenplan.pro.feature.plants.components.PlantCard
import com.gartenplan.pro.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantListScreen(
    onPlantClick: (String) -> Unit,
    viewModel: PlantListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pflanzen") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            GartenSearchBar(
                query = searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                placeholder = "Pflanze suchen...",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            // Category Filter
            CategoryChips(
                selectedCategory = selectedCategory,
                onCategorySelected = viewModel::onCategorySelected,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            // Content
            when (val state = uiState) {
                is PlantListUiState.Loading -> {
                    LoadingScreen(message = "Pflanzen werden geladen...")
                }
                
                is PlantListUiState.Success -> {
                    if (state.plants.isEmpty()) {
                        EmptyState(
                            icon = Icons.Default.Eco,
                            title = "Keine Pflanzen gefunden",
                            message = if (searchQuery.isNotEmpty()) 
                                "Versuche einen anderen Suchbegriff" 
                            else 
                                "Es sind noch keine Pflanzen in der Datenbank"
                        )
                    } else {
                        PlantList(
                            plants = state.plants,
                            onPlantClick = onPlantClick,
                            onFavoriteClick = viewModel::onFavoriteToggle
                        )
                    }
                }
                
                is PlantListUiState.Error -> {
                    ErrorScreen(
                        message = state.message,
                        onRetry = viewModel::loadPlants
                    )
                }
            }
        }
    }
}

@Composable
private fun PlantList(
    plants: List<Plant>,
    onPlantClick: (String) -> Unit,
    onFavoriteClick: (String, Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = plants,
            key = { it.id }
        ) { plant ->
            PlantCard(
                plant = plant,
                onClick = { onPlantClick(plant.id) },
                onFavoriteClick = { isFavorite -> 
                    onFavoriteClick(plant.id, isFavorite) 
                }
            )
        }
    }
}

// ==================== UI STATE ====================

sealed interface PlantListUiState {
    data object Loading : PlantListUiState
    data class Success(val plants: List<Plant>) : PlantListUiState
    data class Error(val message: String) : PlantListUiState
}
