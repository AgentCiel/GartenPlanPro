package com.gartenplan.pro.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gartenplan.pro.domain.model.Garden
import com.gartenplan.pro.domain.model.Plant
import com.gartenplan.pro.domain.usecase.garden.GetAllGardensUseCase
import com.gartenplan.pro.domain.usecase.plant.GetAllPlantsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class SowingCalendarState(
    val plants: List<Plant> = emptyList(),
    val gardens: List<Garden> = emptyList(),
    val climateZone: String = "7a",
    val currentMonth: LocalDate = LocalDate.now(),
    val isLoading: Boolean = true
)

@HiltViewModel
class SowingCalendarViewModel @Inject constructor(
    private val getAllPlantsUseCase: GetAllPlantsUseCase,
    private val getAllGardensUseCase: GetAllGardensUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(SowingCalendarState())
    val state: StateFlow<SowingCalendarState> = _state.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            // Lade Gärten für Klimazone
            getAllGardensUseCase().first().let { gardens ->
                val climateZone = gardens.firstOrNull()?.climateZone ?: "7a"
                _state.value = _state.value.copy(
                    gardens = gardens,
                    climateZone = climateZone
                )
            }

            // Lade alle Pflanzen mit Aussaat-Daten
            getAllPlantsUseCase().collect { plants ->
                // Filtere Pflanzen mit gültigen Aussaat-Daten und sortiere nach Kategorie/Name
                val plantsWithSowingData = plants
                    .filter { plant ->
                        plant.sowIndoorStart != null ||
                        plant.sowOutdoorStart != null ||
                        plant.harvestStart != null
                    }
                    .sortedWith(
                        compareBy<Plant> { it.category.ordinal }
                            .thenBy { it.nameDE }
                    )

                _state.value = _state.value.copy(
                    plants = plantsWithSowingData,
                    isLoading = false
                )
            }
        }
    }

    /**
     * Filtert Pflanzen nach aktueller Saison (was jetzt ausgesät werden kann)
     */
    fun getPlantsForCurrentSeason(): List<Plant> {
        val currentMonth = _state.value.currentMonth.monthValue
        return _state.value.plants.filter { plant ->
            isInRange(currentMonth, plant.sowIndoorStart, plant.sowIndoorEnd) ||
            isInRange(currentMonth, plant.sowOutdoorStart, plant.sowOutdoorEnd)
        }
    }

    /**
     * Filtert Pflanzen nach Ernte-Saison
     */
    fun getPlantsForHarvest(): List<Plant> {
        val currentMonth = _state.value.currentMonth.monthValue
        return _state.value.plants.filter { plant ->
            isInRange(currentMonth, plant.harvestStart, plant.harvestEnd)
        }
    }

    private fun isInRange(month: Int, start: Int?, end: Int?): Boolean {
        if (start == null || end == null) return false
        return if (start <= end) {
            month in start..end
        } else {
            month >= start || month <= end
        }
    }
}
