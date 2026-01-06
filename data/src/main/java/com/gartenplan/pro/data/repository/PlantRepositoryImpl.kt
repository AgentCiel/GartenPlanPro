package com.gartenplan.pro.data.repository

import com.gartenplan.pro.core.constants.CompanionType
import com.gartenplan.pro.core.constants.PlantCategory
import com.gartenplan.pro.data.local.dao.PlantDao
import com.gartenplan.pro.data.mapper.toDomain
import com.gartenplan.pro.data.mapper.toDomainList
import com.gartenplan.pro.domain.model.CompanionInfo
import com.gartenplan.pro.domain.model.Plant
import com.gartenplan.pro.domain.repository.PlantRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlantRepositoryImpl @Inject constructor(
    private val plantDao: PlantDao
) : PlantRepository {

    override fun getAllPlants(): Flow<List<Plant>> {
        return plantDao.getAllPlants().map { it.toDomainList() }
    }

    override fun getPlantsByCategory(category: PlantCategory): Flow<List<Plant>> {
        return plantDao.getPlantsByCategory(category).map { it.toDomainList() }
    }

    override fun searchPlants(query: String): Flow<List<Plant>> {
        return plantDao.searchPlants(query).map { it.toDomainList() }
    }

    override fun getFavoritePlants(): Flow<List<Plant>> {
        return plantDao.getFavoritePlants().map { it.toDomainList() }
    }

    override fun getPlantsBySowingMonth(month: Int): Flow<List<Plant>> {
        return plantDao.getPlantsBySowingMonth(month).map { it.toDomainList() }
    }

    override fun getPlantsByHarvestMonth(month: Int): Flow<List<Plant>> {
        return plantDao.getPlantsByHarvestMonth(month).map { it.toDomainList() }
    }

    override suspend fun getPlantById(id: String): Plant? {
        return plantDao.getPlantById(id)?.toDomain()
    }

    override suspend fun updateFavoriteStatus(plantId: String, isFavorite: Boolean) {
        plantDao.updateFavoriteStatus(plantId, isFavorite)
    }

    override suspend fun getPlantCount(): Int {
        return plantDao.getPlantCount()
    }

    override fun getCompanionsForPlant(plantId: String): Flow<List<CompanionInfo>> {
        return plantDao.getCompanionsForPlant(plantId).map { companions ->
            companions.mapNotNull { companion ->
                val otherPlantId = if (companion.plantId1 == plantId) companion.plantId2 else companion.plantId1
                val plant = plantDao.getPlantById(otherPlantId)
                plant?.let {
                    CompanionInfo(
                        plant = it.toDomain(),
                        relationship = companion.relationship,
                        reasonDE = companion.reasonDE,
                        reasonEN = companion.reasonEN
                    )
                }
            }
        }
    }

    override fun getGoodCompanions(plantId: String): Flow<List<CompanionInfo>> {
        return plantDao.getCompanionsForPlantByType(plantId, CompanionType.GOOD).map { companions ->
            companions.mapNotNull { companion ->
                val otherPlantId = if (companion.plantId1 == plantId) companion.plantId2 else companion.plantId1
                val plant = plantDao.getPlantById(otherPlantId)
                plant?.let {
                    CompanionInfo(
                        plant = it.toDomain(),
                        relationship = CompanionType.GOOD,
                        reasonDE = companion.reasonDE,
                        reasonEN = companion.reasonEN
                    )
                }
            }
        }
    }

    override fun getBadCompanions(plantId: String): Flow<List<CompanionInfo>> {
        return plantDao.getCompanionsForPlantByType(plantId, CompanionType.BAD).map { companions ->
            companions.mapNotNull { companion ->
                val otherPlantId = if (companion.plantId1 == plantId) companion.plantId2 else companion.plantId1
                val plant = plantDao.getPlantById(otherPlantId)
                plant?.let {
                    CompanionInfo(
                        plant = it.toDomain(),
                        relationship = CompanionType.BAD,
                        reasonDE = companion.reasonDE,
                        reasonEN = companion.reasonEN
                    )
                }
            }
        }
    }

    override suspend fun getCompanionRelationship(plantId1: String, plantId2: String): CompanionType? {
        return plantDao.getCompanionRelationship(plantId1, plantId2)?.relationship
    }
}
