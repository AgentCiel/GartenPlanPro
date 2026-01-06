package com.gartenplan.pro.feature.plants

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gartenplan.pro.domain.model.CompanionInfo
import com.gartenplan.pro.domain.model.Plant
import com.gartenplan.pro.domain.usecase.plant.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlantDetailViewModel @Inject constructor(
    private val getPlantByIdUseCase: GetPlantByIdUseCase,
    private val getCompanionPlantsUseCase: GetCompanionPlantsUseCase,
    private val togglePlantFavoriteUseCase: TogglePlantFavoriteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<PlantDetailUiState>(PlantDetailUiState.Loading)
    val uiState: StateFlow<PlantDetailUiState> = _uiState.asStateFlow()

    private var currentPlantId: String? = null

    fun loadPlant(plantId: String) {
        currentPlantId = plantId
        viewModelScope.launch {
            _uiState.value = PlantDetailUiState.Loading
            try {
                val plant = getPlantByIdUseCase(plantId)
                if (plant != null) {
                    val goodCompanions = getCompanionPlantsUseCase.getGoodCompanions(plantId).first()
                    val badCompanions = getCompanionPlantsUseCase.getBadCompanions(plantId).first()
                    _uiState.value = PlantDetailUiState.Success(plant, goodCompanions, badCompanions)
                } else {
                    _uiState.value = PlantDetailUiState.Error("Pflanze nicht gefunden")
                }
            } catch (e: Exception) {
                _uiState.value = PlantDetailUiState.Error(e.message ?: "Unbekannter Fehler")
            }
        }
    }

    fun toggleFavorite() {
        val state = _uiState.value
        if (state is PlantDetailUiState.Success) {
            viewModelScope.launch {
                val newFavorite = !state.plant.isFavorite
                togglePlantFavoriteUseCase(state.plant.id, newFavorite)
                _uiState.value = state.copy(plant = state.plant.copy(isFavorite = newFavorite))
            }
        }
    }
}
