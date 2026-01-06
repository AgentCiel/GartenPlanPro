package com.gartenplan.pro.domain.usecase.plant

import com.gartenplan.pro.core.constants.CompanionType
import com.gartenplan.pro.core.constants.PlantCategory
import com.gartenplan.pro.domain.model.CompanionInfo
import com.gartenplan.pro.domain.model.Plant
import com.gartenplan.pro.domain.repository.PlantRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Get all plants from database
 */
class GetAllPlantsUseCase @Inject constructor(
    private val repository: PlantRepository
) {
    operator fun invoke(): Flow<List<Plant>> = repository.getAllPlants()
}

/**
 * Get plants filtered by category
 */
class GetPlantsByCategoryUseCase @Inject constructor(
    private val repository: PlantRepository
) {
    operator fun invoke(category: PlantCategory): Flow<List<Plant>> = 
        repository.getPlantsByCategory(category)
}

/**
 * Search plants by name
 */
class SearchPlantsUseCase @Inject constructor(
    private val repository: PlantRepository
) {
    operator fun invoke(query: String): Flow<List<Plant>> = 
        repository.searchPlants(query)
}

/**
 * Get a single plant by ID
 */
class GetPlantByIdUseCase @Inject constructor(
    private val repository: PlantRepository
) {
    suspend operator fun invoke(plantId: String): Plant? = 
        repository.getPlantById(plantId)
}

/**
 * Get favorite plants
 */
class GetFavoritePlantsUseCase @Inject constructor(
    private val repository: PlantRepository
) {
    operator fun invoke(): Flow<List<Plant>> = repository.getFavoritePlants()
}

/**
 * Toggle plant favorite status
 */
class TogglePlantFavoriteUseCase @Inject constructor(
    private val repository: PlantRepository
) {
    suspend operator fun invoke(plantId: String, isFavorite: Boolean) {
        repository.updateFavoriteStatus(plantId, isFavorite)
    }
}

/**
 * Get plants that can be sown in a specific month
 */
class GetPlantsBySowingMonthUseCase @Inject constructor(
    private val repository: PlantRepository
) {
    operator fun invoke(month: Int): Flow<List<Plant>> = 
        repository.getPlantsBySowingMonth(month)
}

/**
 * Get plants that can be harvested in a specific month
 */
class GetPlantsByHarvestMonthUseCase @Inject constructor(
    private val repository: PlantRepository
) {
    operator fun invoke(month: Int): Flow<List<Plant>> = 
        repository.getPlantsByHarvestMonth(month)
}

/**
 * Get companion plants (good and bad neighbors)
 */
class GetCompanionPlantsUseCase @Inject constructor(
    private val repository: PlantRepository
) {
    operator fun invoke(plantId: String): Flow<List<CompanionInfo>> = 
        repository.getCompanionsForPlant(plantId)
    
    fun getGoodCompanions(plantId: String): Flow<List<CompanionInfo>> = 
        repository.getGoodCompanions(plantId)
    
    fun getBadCompanions(plantId: String): Flow<List<CompanionInfo>> = 
        repository.getBadCompanions(plantId)
}

/**
 * Check companion relationship between two plants
 */
class CheckCompanionRelationshipUseCase @Inject constructor(
    private val repository: PlantRepository
) {
    suspend operator fun invoke(plantId1: String, plantId2: String): CompanionType? = 
        repository.getCompanionRelationship(plantId1, plantId2)
}
