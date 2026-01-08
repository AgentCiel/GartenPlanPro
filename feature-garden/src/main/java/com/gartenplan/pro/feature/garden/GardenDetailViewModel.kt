package com.gartenplan.pro.feature.garden

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gartenplan.pro.domain.model.Bed
import com.gartenplan.pro.domain.model.Garden
import com.gartenplan.pro.domain.usecase.garden.GetBedsByGardenUseCase
import com.gartenplan.pro.domain.usecase.garden.ObserveGardenUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface GardenDetailUiState {
    data object Loading : GardenDetailUiState
    data class Success(val garden: Garden, val beds: List<Bed>) : GardenDetailUiState
    data class Error(val message: String) : GardenDetailUiState
}

@HiltViewModel
class GardenDetailViewModel @Inject constructor(
    private val observeGardenUseCase: ObserveGardenUseCase,
    private val getBedsByGardenUseCase: GetBedsByGardenUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<GardenDetailUiState>(GardenDetailUiState.Loading)
    val uiState: StateFlow<GardenDetailUiState> = _uiState.asStateFlow()

    fun loadGarden(gardenId: String) {
        viewModelScope.launch {
            _uiState.value = GardenDetailUiState.Loading
            
            try {
                combine(
                    observeGardenUseCase(gardenId),
                    getBedsByGardenUseCase(gardenId)
                ) { garden, beds ->
                    if (garden != null) {
                        GardenDetailUiState.Success(garden, beds)
                    } else {
                        GardenDetailUiState.Error("Garten nicht gefunden")
                    }
                }
                .catch { e ->
                    _uiState.value = GardenDetailUiState.Error(e.message ?: "Fehler beim Laden")
                }
                .collect { state ->
                    _uiState.value = state
                }
            } catch (e: Exception) {
                _uiState.value = GardenDetailUiState.Error(e.message ?: "Fehler beim Laden")
            }
        }
    }
}
