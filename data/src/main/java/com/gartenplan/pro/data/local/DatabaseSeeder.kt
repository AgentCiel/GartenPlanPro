package com.gartenplan.pro.data.local

import android.content.Context
import com.gartenplan.pro.core.constants.*
import com.gartenplan.pro.data.local.dao.PlantDao
import com.gartenplan.pro.data.local.entity.PlantCompanionEntity
import com.gartenplan.pro.data.local.entity.PlantEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Seeds the database with initial plant data on first launch
 */
@Singleton
class DatabaseSeeder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val plantDao: PlantDao
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun seedIfEmpty() {
        withContext(Dispatchers.IO) {
            val plantCount = plantDao.getPlantCount()
            if (plantCount == 0) {
                seedDatabase()
            }
        }
    }

    private suspend fun seedDatabase() {
        try {
            val jsonString = context.assets.open("plants_database.json")
                .bufferedReader()
                .use { it.readText() }

            val data = json.decodeFromString<PlantDatabaseJson>(jsonString)

            // Insert plants
            val plants = data.plants.map { it.toEntity() }
            plantDao.insertPlants(plants)

            // Insert companions
            val companions = data.companions.map { it.toEntity() }
            plantDao.insertCompanions(companions)

        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback: Insert minimal data
            seedMinimalData()
        }
    }

    private suspend fun seedMinimalData() {
        // Insert a few essential plants if JSON loading fails
        val essentialPlants = listOf(
            PlantEntity(
                id = "tomato",
                nameDE = "Tomate",
                nameEN = "Tomato",
                category = PlantCategory.VEGETABLE,
                sowIndoorStart = 2,
                sowIndoorEnd = 4,
                harvestStart = 7,
                harvestEnd = 10,
                spacingInRowCm = 50,
                spacingBetweenRowsCm = 80,
                nutrientDemand = NutrientLevel.HIGH,
                waterDemand = WaterLevel.HIGH,
                sunRequirement = SunLevel.FULL_SUN
            ),
            PlantEntity(
                id = "carrot",
                nameDE = "Karotte",
                nameEN = "Carrot",
                category = PlantCategory.VEGETABLE,
                sowOutdoorStart = 3,
                sowOutdoorEnd = 7,
                harvestStart = 6,
                harvestEnd = 11,
                spacingInRowCm = 5,
                spacingBetweenRowsCm = 30,
                nutrientDemand = NutrientLevel.MEDIUM,
                waterDemand = WaterLevel.MEDIUM,
                sunRequirement = SunLevel.FULL_SUN
            ),
            PlantEntity(
                id = "lettuce",
                nameDE = "Kopfsalat",
                nameEN = "Lettuce",
                category = PlantCategory.VEGETABLE,
                sowIndoorStart = 2,
                sowIndoorEnd = 3,
                sowOutdoorStart = 3,
                sowOutdoorEnd = 8,
                harvestStart = 5,
                harvestEnd = 10,
                spacingInRowCm = 25,
                spacingBetweenRowsCm = 30,
                nutrientDemand = NutrientLevel.MEDIUM,
                waterDemand = WaterLevel.HIGH,
                sunRequirement = SunLevel.PARTIAL_SHADE
            )
        )
        plantDao.insertPlants(essentialPlants)
    }
}

// JSON data classes for parsing
@Serializable
data class PlantDatabaseJson(
    val plants: List<PlantJson>,
    val companions: List<CompanionJson>
)

@Serializable
data class PlantJson(
    val id: String,
    val nameDE: String,
    val nameEN: String,
    val latinName: String? = null,
    val category: String,
    val sowIndoorStart: Int? = null,
    val sowIndoorEnd: Int? = null,
    val sowOutdoorStart: Int? = null,
    val sowOutdoorEnd: Int? = null,
    val harvestStart: Int,
    val harvestEnd: Int,
    val daysToGermination: Int? = null,
    val daysToHarvest: Int? = null,
    val spacingInRowCm: Int,
    val spacingBetweenRowsCm: Int,
    val plantDepthCm: Int? = null,
    val nutrientDemand: String,
    val waterDemand: String,
    val sunRequirement: String,
    val description: String? = null,
    val careTips: String? = null,
    val diseases: String? = null,
    val pests: String? = null,
    val isProOnly: Boolean = false
) {
    fun toEntity(): PlantEntity = PlantEntity(
        id = id,
        nameDE = nameDE,
        nameEN = nameEN,
        latinName = latinName,
        category = PlantCategory.valueOf(category),
        sowIndoorStart = sowIndoorStart,
        sowIndoorEnd = sowIndoorEnd,
        sowOutdoorStart = sowOutdoorStart,
        sowOutdoorEnd = sowOutdoorEnd,
        harvestStart = harvestStart,
        harvestEnd = harvestEnd,
        daysToGermination = daysToGermination,
        daysToHarvest = daysToHarvest,
        spacingInRowCm = spacingInRowCm,
        spacingBetweenRowsCm = spacingBetweenRowsCm,
        plantDepthCm = plantDepthCm,
        nutrientDemand = NutrientLevel.valueOf(nutrientDemand),
        waterDemand = WaterLevel.valueOf(waterDemand),
        sunRequirement = SunLevel.valueOf(sunRequirement),
        description = description,
        careTips = careTips,
        diseases = diseases,
        pests = pests,
        isProOnly = isProOnly
    )
}

@Serializable
data class CompanionJson(
    val plantId1: String,
    val plantId2: String,
    val relationship: String,
    val reasonDE: String? = null,
    val reasonEN: String? = null
) {
    fun toEntity(): PlantCompanionEntity = PlantCompanionEntity(
        plantId1 = plantId1,
        plantId2 = plantId2,
        relationship = CompanionType.valueOf(relationship),
        reasonDE = reasonDE,
        reasonEN = reasonEN
    )
}
