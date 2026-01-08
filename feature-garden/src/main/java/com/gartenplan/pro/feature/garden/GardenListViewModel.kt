package com.gartenplan.pro.feature.garden

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gartenplan.pro.domain.model.Garden
import com.gartenplan.pro.domain.usecase.garden.DeleteGardenUseCase
import com.gartenplan.pro.domain.usecase.garden.GetAllGardensUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface GardenListUiState {
    data object Loading : GardenListUiState
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
                    _uiState.value = GardenListUiState.Error(e.message ?: "Unbekannter Fehler")
                }
                .collect { gardens ->
                    _uiState.value = GardenListUiState.Success(gardens)
                }
        }
    }

    fun deleteGarden(gardenId: String) {
        viewModelScope.launch {
            deleteGardenUseCase(gardenId)
        }
    }
}
