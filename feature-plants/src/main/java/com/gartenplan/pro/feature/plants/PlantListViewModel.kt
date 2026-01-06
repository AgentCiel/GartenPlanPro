package com.gartenplan.pro.feature.plants

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gartenplan.pro.core.constants.PlantCategory
import com.gartenplan.pro.domain.model.Plant
import com.gartenplan.pro.domain.usecase.plant.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlantListViewModel @Inject constructor(
    private val getAllPlantsUseCase: GetAllPlantsUseCase,
    private val getPlantsByCategoryUseCase: GetPlantsByCategoryUseCase,
    private val searchPlantsUseCase: SearchPlantsUseCase,
    private val togglePlantFavoriteUseCase: TogglePlantFavoriteUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<PlantCategory?>(null)
    val selectedCategory: StateFlow<PlantCategory?> = _selectedCategory.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<PlantListUiState> = combine(
        _searchQuery,
        _selectedCategory
    ) { query, category ->
        Pair(query, category)
    }.flatMapLatest { (query, category) ->
        when {
            query.isNotBlank() -> searchPlantsUseCase(query)
            category != null -> getPlantsByCategoryUseCase(category)
            else -> getAllPlantsUseCase()
        }
    }.map<List<Plant>, PlantListUiState> { plants ->
        PlantListUiState.Success(plants)
    }.catch { e ->
        emit(PlantListUiState.Error(e.message ?: "Unbekannter Fehler"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlantListUiState.Loading
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelected(category: PlantCategory?) {
        _selectedCategory.value = category
        // Clear search when category changes
        if (category != null) {
            _searchQuery.value = ""
        }
    }

    fun onFavoriteToggle(plantId: String, isFavorite: Boolean) {
        viewModelScope.launch {
            togglePlantFavoriteUseCase(plantId, isFavorite)
        }
    }

    fun loadPlants() {
        // Trigger reload by resetting filters
        _searchQuery.value = ""
        _selectedCategory.value = null
    }
}
