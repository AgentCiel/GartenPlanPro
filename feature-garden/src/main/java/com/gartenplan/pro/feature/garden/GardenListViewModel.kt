package com.gartenplan.pro.feature.garden

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gartenplan.pro.domain.model.Garden
import com.gartenplan.pro.domain.usecase.garden.DeleteGardenUseCase
import com.gartenplan.pro.domain.usecase.garden.GetAllGardensUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface GardenListUiState {
    data object Loading : GardenListUiState
    data object Empty : GardenListUiState
    data class Success(val gardens: List<Garden>) : GardenListUiState
    data class Error(val message: String) : GardenListUiState
}

@HiltViewModel
class GardenListViewModel @Inject constructor(
    private val getAllGardensUseCase: GetAllGardensUseCase,
    private val deleteGardenUseCase: DeleteGardenUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<GardenListUiState>(GardenListUiState.Loading)
    val uiState: StateFlow<GardenListUiState> = _uiState.asStateFlow()

    init {
        loadGardens()
    }

    private fun loadGardens() {
        viewModelScope.launch {
            getAllGardensUseCase()
                .catch { e ->
                    _uiState.value = GardenListUiState.Error(e.message ?: "Fehler beim Laden")
                }
                .collect { gardens ->
                    _uiState.value = if (gardens.isEmpty()) {
                        GardenListUiState.Empty
                    } else {
                        GardenListUiState.Success(gardens)
                    }
                }
        }
    }

    fun deleteGarden(gardenId: String) {
        viewModelScope.launch {
            try {
                deleteGardenUseCase(gardenId)
            } catch (e: Exception) {
                // Error wird durch Flow-Refresh angezeigt
            }
        }
    }
}
