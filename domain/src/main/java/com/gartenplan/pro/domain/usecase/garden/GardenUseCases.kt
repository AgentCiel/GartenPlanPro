package com.gartenplan.pro.domain.usecase.garden

import com.gartenplan.pro.core.constants.BedShape
import com.gartenplan.pro.core.constants.CompanionType
import com.gartenplan.pro.domain.model.*
import com.gartenplan.pro.domain.repository.GardenRepository
import com.gartenplan.pro.domain.repository.PlantRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject

/**
 * Get all gardens
 */
class GetAllGardensUseCase @Inject constructor(
    private val repository: GardenRepository
) {
    operator fun invoke(): Flow<List<Garden>> = repository.getAllGardens()
}

/**
 * Get garden by ID with live updates
 */
class ObserveGardenUseCase @Inject constructor(
    private val repository: GardenRepository
) {
    operator fun invoke(gardenId: String): Flow<Garden?> = 
        repository.observeGardenById(gardenId)
}

/**
 * Get garden count (for free version limit check)
 */
class GetGardenCountUseCase @Inject constructor(
    private val repository: GardenRepository
) {
    operator fun invoke(): Flow<Int> = repository.observeGardenCount()
    
    suspend fun getCount(): Int = repository.getGardenCount()
}

/**
 * Create a new garden
 */
class CreateGardenUseCase @Inject constructor(
    private val repository: GardenRepository
) {
    suspend operator fun invoke(
        name: String,
        widthCm: Int,
        heightCm: Int,
        climateZone: String = "7a"
    ): String {
        val garden = Garden(
            id = UUID.randomUUID().toString(),
            name = name,
            widthCm = widthCm,
            heightCm = heightCm,
            climateZone = climateZone,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        return repository.createGarden(garden)
    }
}

/**
 * Update an existing garden
 */
class UpdateGardenUseCase @Inject constructor(
    private val repository: GardenRepository
) {
    suspend operator fun invoke(garden: Garden) {
        repository.updateGarden(garden)
    }
}

/**
 * Delete a garden
 */
class DeleteGardenUseCase @Inject constructor(
    private val repository: GardenRepository
) {
    suspend operator fun invoke(gardenId: String) {
        repository.deleteGarden(gardenId)
    }
}

/**
 * Get beds for a garden
 */
class GetBedsByGardenUseCase @Inject constructor(
    private val repository: GardenRepository
) {
    operator fun invoke(gardenId: String): Flow<List<Bed>> = 
        repository.getBedsByGarden(gardenId)
}

/**
 * Create a new bed in a garden
 */
class CreateBedUseCase @Inject constructor(
    private val repository: GardenRepository
) {
    suspend operator fun invoke(
        gardenId: String,
        name: String,
        positionX: Int,
        positionY: Int,
        widthCm: Int,
        heightCm: Int,
        shape: BedShape = BedShape.RECTANGLE,
        isPath: Boolean = false,
        colorHex: String = if (isPath) "#9E9E9E" else "#8D6E63"
    ): String {
        val bed = Bed(
            id = UUID.randomUUID().toString(),
            gardenId = gardenId,
            name = name,
            positionX = positionX,
            positionY = positionY,
            widthCm = widthCm,
            heightCm = heightCm,
            shape = shape,
            isPath = isPath,
            colorHex = colorHex
        )
        return repository.createBed(bed)
    }
}

/**
 * Update a bed
 */
class UpdateBedUseCase @Inject constructor(
    private val repository: GardenRepository
) {
    suspend operator fun invoke(bed: Bed) {
        repository.updateBed(bed)
    }
}

/**
 * Delete a bed
 */
class DeleteBedUseCase @Inject constructor(
    private val repository: GardenRepository
) {
    suspend operator fun invoke(bedId: String) {
        repository.deleteBed(bedId)
    }
}

/**
 * Get plant placements for a bed
 */
class GetPlacementsByBedUseCase @Inject constructor(
    private val repository: GardenRepository
) {
    operator fun invoke(bedId: String): Flow<List<PlantPlacement>> = 
        repository.getPlacementsByBed(bedId)
    
    fun forYear(bedId: String, year: Int): Flow<List<PlantPlacement>> = 
        repository.getPlacementsByBedAndYear(bedId, year)
}

/**
 * Place a plant in a bed
 */
class PlacePlantInBedUseCase @Inject constructor(
    private val repository: GardenRepository,
    private val plantRepository: PlantRepository
) {
    suspend operator fun invoke(
        bedId: String,
        plantId: String,
        positionX: Int,
        positionY: Int,
        widthCm: Int,
        heightCm: Int,
        year: Int,
        cultureType: CultureType = CultureType.MAIN
    ): String {
        val plant = plantRepository.getPlantById(plantId) 
            ?: throw IllegalArgumentException("Plant not found: $plantId")
        
        val placement = PlantPlacement(
            id = UUID.randomUUID().toString(),
            bedId = bedId,
            plant = plant,
            positionX = positionX,
            positionY = positionY,
            widthCm = widthCm,
            heightCm = heightCm,
            quantity = 1,
            year = year,
            cultureType = cultureType,
            status = PlantingStatus.PLANNED
        )
        return repository.createPlacement(placement)
    }
}

/**
 * Remove a plant placement
 */
class RemovePlacementUseCase @Inject constructor(
    private val repository: GardenRepository
) {
    suspend operator fun invoke(placementId: String) {
        repository.deletePlacement(placementId)
    }
}

/**
 * Analyze companion planting in a bed
 * Returns list of conflicts (bad neighbors)
 */
class AnalyzeBedCompanionsUseCase @Inject constructor(
    private val gardenRepository: GardenRepository,
    private val plantRepository: PlantRepository
) {
    data class CompanionConflict(
        val plant1: Plant,
        val plant2: Plant,
        val reasonDE: String?,
        val reasonEN: String?
    )
    
    data class CompanionBenefit(
        val plant1: Plant,
        val plant2: Plant,
        val reasonDE: String?,
        val reasonEN: String?
    )
    
    data class BedAnalysisResult(
        val conflicts: List<CompanionConflict>,
        val benefits: List<CompanionBenefit>,
        val score: Int // 0-100, higher is better
    )
    
    suspend operator fun invoke(bedId: String, year: Int): BedAnalysisResult {
        val placements = gardenRepository.getPlacementsByBedAndYear(bedId, year).first()
        val plantIds = placements.map { it.plant.id }.distinct()
        
        val conflicts = mutableListOf<CompanionConflict>()
        val benefits = mutableListOf<CompanionBenefit>()
        
        // Check all pairs of plants
        for (i in plantIds.indices) {
            for (j in i + 1 until plantIds.size) {
                val plant1 = placements.find { it.plant.id == plantIds[i] }?.plant ?: continue
                val plant2 = placements.find { it.plant.id == plantIds[j] }?.plant ?: continue
                
                val relationship = plantRepository.getCompanionRelationship(plantIds[i], plantIds[j])
                
                when (relationship) {
                    CompanionType.BAD -> {
                        // Get reason from companion info
                        val companionInfo = plantRepository.getBadCompanions(plantIds[i]).first()
                            .find { it.plant.id == plantIds[j] }
                        conflicts.add(CompanionConflict(
                            plant1 = plant1,
                            plant2 = plant2,
                            reasonDE = companionInfo?.reasonDE,
                            reasonEN = companionInfo?.reasonEN
                        ))
                    }
                    CompanionType.GOOD -> {
                        val companionInfo = plantRepository.getGoodCompanions(plantIds[i]).first()
                            .find { it.plant.id == plantIds[j] }
                        benefits.add(CompanionBenefit(
                            plant1 = plant1,
                            plant2 = plant2,
                            reasonDE = companionInfo?.reasonDE,
                            reasonEN = companionInfo?.reasonEN
                        ))
                    }
                    else -> { /* neutral, ignore */ }
                }
            }
        }
        
        // Calculate score (simple formula)
        val totalPairs = if (plantIds.size > 1) (plantIds.size * (plantIds.size - 1)) / 2 else 1
        val score = if (totalPairs > 0) {
            val benefitPoints = benefits.size * 10
            val conflictPenalty = conflicts.size * 20
            (100 + benefitPoints - conflictPenalty).coerceIn(0, 100)
        } else 100
        
        return BedAnalysisResult(
            conflicts = conflicts,
            benefits = benefits,
            score = score
        )
    }
}

/**
 * Check crop rotation (Fruchtfolge)
 * Returns plants that were planted in same bed in previous years
 */
class CheckCropRotationUseCase @Inject constructor(
    private val repository: GardenRepository
) {
    data class RotationWarning(
        val plant: Plant,
        val lastPlantedYear: Int,
        val yearsAgo: Int
    )
    
    suspend operator fun invoke(
        bedId: String,
        plantId: String,
        currentYear: Int
    ): RotationWarning? {
        val previousPlacements = repository.getPreviousYearPlacements(bedId, currentYear)
        
        val samePlantPlacement = previousPlacements
            .filter { it.plant.id == plantId }
            .maxByOrNull { it.year }
        
        return samePlantPlacement?.let {
            RotationWarning(
                plant = it.plant,
                lastPlantedYear = it.year,
                yearsAgo = currentYear - it.year
            )
        }
    }
}
