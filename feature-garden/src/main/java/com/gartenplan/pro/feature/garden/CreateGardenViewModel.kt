package com.gartenplan.pro.feature.garden

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gartenplan.pro.domain.usecase.garden.CreateGardenUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface CreateGardenUiState {
    data object Idle : CreateGardenUiState
    data object Loading : CreateGardenUiState
    data class Success(val gardenId: String) : CreateGardenUiState
    data class Error(val message: String) : CreateGardenUiState
}

@HiltViewModel
class CreateGardenViewModel @Inject constructor(
    private val createGardenUseCase: CreateGardenUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<CreateGardenUiState>(CreateGardenUiState.Idle)
    val uiState: StateFlow<CreateGardenUiState> = _uiState.asStateFlow()

    // Form fields
    var name by mutableStateOf("")
    var widthMeter by mutableStateOf("")
    var heightMeter by mutableStateOf("")
    var climateZone by mutableStateOf("7a (-17.8 bis -15.0°C)")
    var notes by mutableStateOf("")

    // Validation errors
    var nameError by mutableStateOf<String?>(null)
    var sizeError by mutableStateOf<String?>(null)

    fun createGarden() {
        // Validate
        nameError = null
        sizeError = null

        if (name.isBlank()) {
            nameError = "Bitte gib einen Namen ein"
            return
        }

        val width = widthMeter.toFloatOrNull()
        val height = heightMeter.toFloatOrNull()

        if (width == null || width <= 0) {
            sizeError = "Bitte gib eine gültige Breite ein"
            return
        }
        if (height == null || height <= 0) {
            sizeError = "Bitte gib eine gültige Länge ein"
            return
        }

        _uiState.value = CreateGardenUiState.Loading

        viewModelScope.launch {
            try {
                val gardenId = createGardenUseCase(
                    name = name.trim(),
                    widthCm = (width * 100).toInt(),
                    heightCm = (height * 100).toInt(),
                    climateZone = climateZone.substringBefore(" ")
                )
                _uiState.value = CreateGardenUiState.Success(gardenId)
            } catch (e: Exception) {
                _uiState.value = CreateGardenUiState.Error(e.message ?: "Fehler beim Erstellen")
            }
        }
    }
}
