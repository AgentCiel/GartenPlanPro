package com.gartenplan.pro.data.local.dao

import androidx.room.*
import com.gartenplan.pro.core.constants.CompanionType
import com.gartenplan.pro.core.constants.PlantCategory
import com.gartenplan.pro.data.local.entity.PlantCompanionEntity
import com.gartenplan.pro.data.local.entity.PlantEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Plant operations
 */
@Dao
interface PlantDao {
    
    // ==================== PLANTS ====================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlant(plant: PlantEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlants(plants: List<PlantEntity>)
    
    @Update
    suspend fun updatePlant(plant: PlantEntity)
    
    @Delete
    suspend fun deletePlant(plant: PlantEntity)
    
    @Query("SELECT * FROM plants ORDER BY nameDE ASC")
    fun getAllPlants(): Flow<List<PlantEntity>>
    
    @Query("SELECT * FROM plants ORDER BY nameDE ASC")
    suspend fun getAllPlantsOnce(): List<PlantEntity>
    
    @Query("SELECT * FROM plants WHERE id = :id")
    suspend fun getPlantById(id: String): PlantEntity?
    
    @Query("SELECT * FROM plants WHERE id = :id")
    fun observePlantById(id: String): Flow<PlantEntity?>
    
    @Query("SELECT * FROM plants WHERE category = :category ORDER BY nameDE ASC")
    fun getPlantsByCategory(category: PlantCategory): Flow<List<PlantEntity>>
    
    @Query("SELECT * FROM plants WHERE nameDE LIKE '%' || :query || '%' OR nameEN LIKE '%' || :query || '%' OR latinName LIKE '%' || :query || '%' ORDER BY nameDE ASC")
    fun searchPlants(query: String): Flow<List<PlantEntity>>
    
    @Query("SELECT * FROM plants WHERE isFavorite = 1 ORDER BY nameDE ASC")
    fun getFavoritePlants(): Flow<List<PlantEntity>>
    
    @Query("UPDATE plants SET isFavorite = :isFavorite WHERE id = :plantId")
    suspend fun updateFavoriteStatus(plantId: String, isFavorite: Boolean)
    
    @Query("SELECT * FROM plants WHERE isProOnly = 0 ORDER BY nameDE ASC")
    fun getFreePlants(): Flow<List<PlantEntity>>
    
    @Query("SELECT COUNT(*) FROM plants")
    suspend fun getPlantCount(): Int
    
    // Filter by sowing month
    @Query("""
        SELECT * FROM plants 
        WHERE (sowIndoorStart IS NOT NULL AND sowIndoorStart <= :month AND sowIndoorEnd >= :month)
           OR (sowOutdoorStart IS NOT NULL AND sowOutdoorStart <= :month AND sowOutdoorEnd >= :month)
        ORDER BY nameDE ASC
    """)
    fun getPlantsBySowingMonth(month: Int): Flow<List<PlantEntity>>
    
    // Filter by harvest month
    @Query("SELECT * FROM plants WHERE harvestStart <= :month AND harvestEnd >= :month ORDER BY nameDE ASC")
    fun getPlantsByHarvestMonth(month: Int): Flow<List<PlantEntity>>
    
    // ==================== COMPANIONS ====================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompanion(companion: PlantCompanionEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompanions(companions: List<PlantCompanionEntity>)
    
    @Query("DELETE FROM plant_companions")
    suspend fun deleteAllCompanions()
    
    @Query("""
        SELECT * FROM plant_companions 
        WHERE plantId1 = :plantId OR plantId2 = :plantId
    """)
    fun getCompanionsForPlant(plantId: String): Flow<List<PlantCompanionEntity>>
    
    @Query("""
        SELECT * FROM plant_companions 
        WHERE (plantId1 = :plantId OR plantId2 = :plantId) AND relationship = :type
    """)
    fun getCompanionsForPlantByType(plantId: String, type: CompanionType): Flow<List<PlantCompanionEntity>>
    
    @Query("""
        SELECT * FROM plant_companions 
        WHERE (plantId1 = :plantId1 AND plantId2 = :plantId2) 
           OR (plantId1 = :plantId2 AND plantId2 = :plantId1)
        LIMIT 1
    """)
    suspend fun getCompanionRelationship(plantId1: String, plantId2: String): PlantCompanionEntity?
    
    @Query("SELECT * FROM plant_companions")
    suspend fun getAllCompanions(): List<PlantCompanionEntity>
}
