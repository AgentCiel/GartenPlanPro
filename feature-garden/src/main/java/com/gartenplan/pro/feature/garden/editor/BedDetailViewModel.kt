package com.gartenplan.pro.feature.garden.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gartenplan.pro.domain.model.Plant
import com.gartenplan.pro.domain.usecase.garden.GetBedByIdUseCase
import com.gartenplan.pro.domain.usecase.garden.UpdateBedUseCase
import com.gartenplan.pro.domain.usecase.plant.GetAllPlantsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BedDetailState(
    val bed: EditorBed? = null,
    val plants: List<BedPlant> = emptyList(),
    val warnings: List<BedWarning> = emptyList(),
    val isLoading: Boolean = false,
    val isEditingName: Boolean = false,
    val editedName: String = "",
    val availablePlants: List<Plant> = emptyList(),
    val showPlantPicker: Boolean = false
)

@HiltViewModel
class BedDetailViewModel @Inject constructor(
    private val getBedByIdUseCase: GetBedByIdUseCase,
    private val updateBedUseCase: UpdateBedUseCase,
    private val getAllPlantsUseCase: GetAllPlantsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(BedDetailState())
    val state: StateFlow<BedDetailState> = _state.asStateFlow()

    private var allPlants: List<Plant> = emptyList()

    init {
        loadAllPlants()
    }

    private fun loadAllPlants() {
        viewModelScope.launch {
            getAllPlantsUseCase().collect { plants ->
                allPlants = plants
                _state.value = _state.value.copy(availablePlants = plants)
                updatePlantWarnings()
            }
        }
    }

    fun showPlantPicker() {
        _state.value = _state.value.copy(showPlantPicker = true)
    }

    fun hidePlantPicker() {
        _state.value = _state.value.copy(showPlantPicker = false)
    }

    fun loadBed(bedId: String) {
        _state.value = _state.value.copy(isLoading = true)
        
        viewModelScope.launch {
            try {
                val bed = getBedByIdUseCase(bedId)
                if (bed != null) {
                    val editorBed = EditorBed(
                        id = bed.id,
                        name = bed.name,
                        x = bed.positionX / 100f,
                        y = bed.positionY / 100f,
                        width = bed.widthCm / 100f,
                        height = bed.heightCm / 100f,
                        colorHex = bed.colorHex,
                        plantIds = emptyList() // TODO: Aus DB laden wenn implementiert
                    )
                    
                    _state.value = _state.value.copy(
                        bed = editorBed,
                        isLoading = false,
                        editedName = bed.name
                    )
                    
                    loadBedPlants(editorBed.plantIds)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }

    private fun loadBedPlants(plantIds: List<String>) {
        val bedPlants = plantIds.mapNotNull { id ->
            allPlants.find { it.id == id }?.let { plant ->
                BedPlant(
                    id = plant.id,
                    name = plant.nameDE,
                    emoji = getPlantEmoji(plant.nameDE),
                    quantity = 1
                )
            }
        }
        
        _state.value = _state.value.copy(plants = bedPlants)
        updatePlantWarnings()
    }

    private fun updatePlantWarnings() {
        val currentPlants = _state.value.plants
        if (currentPlants.isEmpty()) {
            _state.value = _state.value.copy(warnings = emptyList())
            return
        }

        val warnings = mutableListOf<BedWarning>()
        
        // Prüfe auf schlechte Nachbarn
        val badNeighborPairs = findBadNeighbors(currentPlants)
        if (badNeighborPairs.isNotEmpty()) {
            warnings.add(BedWarning(
                type = WarningType.BAD_NEIGHBORS,
                message = "Diese Pflanzen vertragen sich nicht gut:",
                plantNames = badNeighborPairs.map { "${it.first} & ${it.second}" }
            ))
        }
        
        // Prüfe auf Überfüllung (vereinfacht)
        val bed = _state.value.bed
        if (bed != null && currentPlants.size > bed.areaSqM * 4) {
            warnings.add(BedWarning(
                type = WarningType.OVERCROWDED,
                message = "Das Beet könnte überfüllt sein",
                plantNames = emptyList()
            ))
        }
        
        _state.value = _state.value.copy(warnings = warnings)
    }

    private fun findBadNeighbors(plants: List<BedPlant>): List<Pair<String, String>> {
        // Vereinfachte schlechte Nachbarn (in Realität aus DB laden)
        val badPairs = mapOf(
            "Tomate" to listOf("Gurke", "Fenchel", "Erbse"),
            "Gurke" to listOf("Tomate", "Radieschen", "Kartoffel"),
            "Kartoffel" to listOf("Tomate", "Gurke", "Sonnenblume"),
            "Zwiebel" to listOf("Bohne", "Erbse"),
            "Bohne" to listOf("Zwiebel", "Knoblauch", "Lauch")
        )
        
        val found = mutableListOf<Pair<String, String>>()
        
        for (i in plants.indices) {
            for (j in i + 1 until plants.size) {
                val plant1 = plants[i].name
                val plant2 = plants[j].name
                
                val isBad = badPairs[plant1]?.any { it.equals(plant2, ignoreCase = true) } == true ||
                            badPairs[plant2]?.any { it.equals(plant1, ignoreCase = true) } == true
                
                if (isBad) {
                    found.add(plant1 to plant2)
                }
            }
        }
        
        return found
    }

    // ==================== NAME EDITING ====================

    fun toggleEditName() {
        _state.value = _state.value.copy(
            isEditingName = true,
            editedName = _state.value.bed?.name ?: ""
        )
    }

    fun updateName(name: String) {
        _state.value = _state.value.copy(editedName = name)
    }

    fun saveName() {
        val bed = _state.value.bed ?: return
        val newName = _state.value.editedName
        
        val updatedBed = bed.copy(name = newName)
        _state.value = _state.value.copy(
            bed = updatedBed,
            isEditingName = false
        )
        
        // Persistieren
        viewModelScope.launch {
            updateBedUseCase(com.gartenplan.pro.domain.model.Bed(
                id = bed.id,
                gardenId = "", // Wird vom UseCase ignoriert
                name = newName,
                positionX = (bed.x * 100).toInt(),
                positionY = (bed.y * 100).toInt(),
                widthCm = (bed.width * 100).toInt(),
                heightCm = (bed.height * 100).toInt(),
                colorHex = bed.colorHex
            ))
        }
    }

    fun cancelEditName() {
        _state.value = _state.value.copy(
            isEditingName = false,
            editedName = _state.value.bed?.name ?: ""
        )
    }

    // ==================== PLANT MANAGEMENT ====================

    fun addPlant(plantId: String) {
        val plant = allPlants.find { it.id == plantId } ?: return
        
        val bedPlant = BedPlant(
            id = plant.id,
            name = plant.nameDE,
            emoji = getPlantEmoji(plant.nameDE),
            quantity = 1
        )
        
        _state.value = _state.value.copy(
            plants = _state.value.plants + bedPlant
        )
        
        updatePlantWarnings()
        // TODO: Persistieren wenn BedPlant-Tabelle existiert
    }

    fun removePlant(plantId: String) {
        _state.value = _state.value.copy(
            plants = _state.value.plants.filter { it.id != plantId }
        )
        
        updatePlantWarnings()
        // TODO: Persistieren
    }

    private fun getPlantEmoji(name: String): String? {
        val lower = name.lowercase()
        return when {
            lower.contains("tomate") -> "🍅"
            lower.contains("karotte") || lower.contains("möhre") -> "🥕"
            lower.contains("salat") -> "🥬"
            lower.contains("gurke") -> "🥒"
            lower.contains("paprika") -> "🌶️"
            lower.contains("zwiebel") -> "🧅"
            lower.contains("knoblauch") -> "🧄"
            lower.contains("kartoffel") -> "🥔"
            lower.contains("mais") -> "🌽"
            lower.contains("kürbis") -> "🎃"
            lower.contains("erdbeere") -> "🍓"
            lower.contains("bohne") -> "🫘"
            lower.contains("erbse") -> "🫛"
            lower.contains("brokkoli") -> "🥦"
            lower.contains("aubergine") -> "🍆"
            lower.contains("sonnenblume") -> "🌻"
            else -> null
        }
    }
}
