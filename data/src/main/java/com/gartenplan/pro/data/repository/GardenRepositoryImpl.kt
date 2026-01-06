package com.gartenplan.pro.data.repository

import com.gartenplan.pro.data.local.dao.GardenDao
import com.gartenplan.pro.data.local.dao.PlantDao
import com.gartenplan.pro.data.mapper.*
import com.gartenplan.pro.domain.model.Bed
import com.gartenplan.pro.domain.model.Garden
import com.gartenplan.pro.domain.model.PlantPlacement
import com.gartenplan.pro.domain.repository.GardenRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GardenRepositoryImpl @Inject constructor(
    private val gardenDao: GardenDao,
    private val plantDao: PlantDao
) : GardenRepository {

    override fun getAllGardens(): Flow<List<Garden>> {
        return gardenDao.getAllGardens().map { gardens ->
            gardens.map { it.toDomain() }
        }
    }

    override fun observeGardenById(id: String): Flow<Garden?> {
        return gardenDao.observeGardenById(id).map { it?.toDomain() }
    }

    override fun observeGardenCount(): Flow<Int> {
        return gardenDao.observeGardenCount()
    }

    override fun getBedsByGarden(gardenId: String): Flow<List<Bed>> {
        return gardenDao.getBedsByGarden(gardenId).map { beds ->
            beds.map { it.toDomain() }
        }
    }

    override fun getPlacementsByBed(bedId: String): Flow<List<PlantPlacement>> {
        return gardenDao.getPlacementsByBed(bedId).map { placements ->
            placements.mapNotNull { placement ->
                val plant = plantDao.getPlantById(placement.plantId)
                plant?.let { placement.toDomain(it.toDomain()) }
            }
        }
    }

    override fun getPlacementsByBedAndYear(bedId: String, year: Int): Flow<List<PlantPlacement>> {
        return gardenDao.getPlacementsByBedAndYear(bedId, year).map { placements ->
            placements.mapNotNull { placement ->
                val plant = plantDao.getPlantById(placement.plantId)
                plant?.let { placement.toDomain(it.toDomain()) }
            }
        }
    }

    override suspend fun getGardenById(id: String): Garden? {
        return gardenDao.getGardenById(id)?.toDomain()
    }

    override suspend fun getGardenCount(): Int {
        return gardenDao.getGardenCount()
    }

    override suspend fun getBedById(id: String): Bed? {
        return gardenDao.getBedById(id)?.toDomain()
    }

    override suspend fun createGarden(garden: Garden): String {
        gardenDao.insertGarden(garden.toEntity())
        return garden.id
    }

    override suspend fun updateGarden(garden: Garden) {
        gardenDao.updateGarden(garden.toEntity().copy(updatedAt = System.currentTimeMillis()))
    }

    override suspend fun deleteGarden(gardenId: String) {
        gardenDao.deleteGardenById(gardenId)
    }

    override suspend fun createBed(bed: Bed): String {
        gardenDao.insertBed(bed.toEntity())
        return bed.id
    }

    override suspend fun updateBed(bed: Bed) {
        gardenDao.updateBed(bed.toEntity())
    }

    override suspend fun deleteBed(bedId: String) {
        gardenDao.deleteBedById(bedId)
    }

    override suspend fun createPlacement(placement: PlantPlacement): String {
        gardenDao.insertPlacement(placement.toEntity())
        return placement.id
    }

    override suspend fun updatePlacement(placement: PlantPlacement) {
        gardenDao.updatePlacement(placement.toEntity())
    }

    override suspend fun deletePlacement(placementId: String) {
        gardenDao.deletePlacementById(placementId)
    }

    override suspend fun getPreviousYearPlacements(bedId: String, currentYear: Int): List<PlantPlacement> {
        return gardenDao.getPreviousYearPlacements(bedId, currentYear).mapNotNull { placement ->
            val plant = plantDao.getPlantById(placement.plantId)
            plant?.let { placement.toDomain(it.toDomain()) }
        }
    }
}
