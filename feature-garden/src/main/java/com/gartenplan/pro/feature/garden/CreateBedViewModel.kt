package com.gartenplan.pro.feature.garden

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gartenplan.pro.core.constants.BedShape
import com.gartenplan.pro.domain.usecase.garden.CreateBedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface CreateBedUiState {
    data object Idle : CreateBedUiState
    data object Loading : CreateBedUiState
    data class Success(val bedId: String) : CreateBedUiState
    data class Error(val message: String) : CreateBedUiState
}

@HiltViewModel
class CreateBedViewModel @Inject constructor(
    private val createBedUseCase: CreateBedUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<CreateBedUiState>(CreateBedUiState.Idle)
    val uiState: StateFlow<CreateBedUiState> = _uiState.asStateFlow()

    private var gardenId: String = ""

    // Form fields
    var name by mutableStateOf("")
    var widthCm by mutableStateOf("100")
    var heightCm by mutableStateOf("200")
    var positionX by mutableStateOf("0")
    var positionY by mutableStateOf("0")
    var colorHex by mutableStateOf("#8D6E63")

    // Validation errors
    var nameError by mutableStateOf<String?>(null)
    var sizeError by mutableStateOf<String?>(null)

    fun setGardenId(id: String) {
        gardenId = id
    }

    fun createBed() {
        // Validate
        nameError = null
        sizeError = null

        if (name.isBlank()) {
            nameError = "Bitte gib einen Namen ein"
            return
        }

        val width = widthCm.toIntOrNull()
        val height = heightCm.toIntOrNull()

        if (width == null || width <= 0) {
            sizeError = "Bitte gib eine gültige Breite ein"
            return
        }
        if (height == null || height <= 0) {
            sizeError = "Bitte gib eine gültige Länge ein"
            return
        }

        _uiState.value = CreateBedUiState.Loading

        viewModelScope.launch {
            try {
                val bedId = createBedUseCase(
                    gardenId = gardenId,
                    name = name.trim(),
                    positionX = positionX.toIntOrNull() ?: 0,
                    positionY = positionY.toIntOrNull() ?: 0,
                    widthCm = width,
                    heightCm = height,
                    shape = BedShape.RECTANGLE,
                    colorHex = colorHex
                )
                _uiState.value = CreateBedUiState.Success(bedId)
            } catch (e: Exception) {
                _uiState.value = CreateBedUiState.Error(e.message ?: "Fehler beim Erstellen")
            }
        }
    }
}
