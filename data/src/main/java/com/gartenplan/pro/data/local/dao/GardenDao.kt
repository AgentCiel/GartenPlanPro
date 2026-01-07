package com.gartenplan.pro.data.local.dao

import androidx.room.*
import com.gartenplan.pro.data.local.entity.BedEntity
import com.gartenplan.pro.data.local.entity.GardenEntity
import com.gartenplan.pro.data.local.entity.PlantPlacementEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Garden, Bed, and PlantPlacement operations
 */
@Dao
interface GardenDao {
    
    // ==================== GARDENS ====================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGarden(garden: GardenEntity)
    
    @Update
    suspend fun updateGarden(garden: GardenEntity)
    
    @Delete
    suspend fun deleteGarden(garden: GardenEntity)
    
    @Query("DELETE FROM gardens WHERE id = :gardenId")
    suspend fun deleteGardenById(gardenId: String)
    
    @Query("SELECT * FROM gardens ORDER BY updatedAt DESC")
    fun getAllGardens(): Flow<List<GardenEntity>>
    
    @Query("SELECT * FROM gardens ORDER BY updatedAt DESC")
    suspend fun getAllGardensOnce(): List<GardenEntity>
    
    @Query("SELECT * FROM gardens WHERE id = :id")
    suspend fun getGardenById(id: String): GardenEntity?
    
    @Query("SELECT * FROM gardens WHERE id = :id")
    fun observeGardenById(id: String): Flow<GardenEntity?>
    
    @Query("SELECT COUNT(*) FROM gardens")
    suspend fun getGardenCount(): Int
    
    @Query("SELECT COUNT(*) FROM gardens")
    fun observeGardenCount(): Flow<Int>
    
    // ==================== BEDS ====================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBed(bed: BedEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBeds(beds: List<BedEntity>)
    
    @Update
    suspend fun updateBed(bed: BedEntity)
    
    @Delete
    suspend fun deleteBed(bed: BedEntity)
    
    @Query("DELETE FROM beds WHERE id = :bedId")
    suspend fun deleteBedById(bedId: String)
    
    @Query("SELECT * FROM beds WHERE gardenId = :gardenId ORDER BY name ASC")
    fun getBedsByGarden(gardenId: String): Flow<List<BedEntity>>
    
    @Query("SELECT * FROM beds WHERE gardenId = :gardenId ORDER BY name ASC")
    suspend fun getBedsByGardenOnce(gardenId: String): List<BedEntity>
    
    @Query("SELECT * FROM beds WHERE gardenId = :gardenId AND isPath = 0 ORDER BY name ASC")
    fun getActualBedsByGarden(gardenId: String): Flow<List<BedEntity>>
    
    @Query("SELECT * FROM beds WHERE gardenId = :gardenId AND isPath = 1 ORDER BY name ASC")
    fun getPathsByGarden(gardenId: String): Flow<List<BedEntity>>
    
    @Query("SELECT * FROM beds WHERE id = :id")
    suspend fun getBedById(id: String): BedEntity?
    
    @Query("SELECT * FROM beds WHERE id = :id")
    fun observeBedById(id: String): Flow<BedEntity?>
    
    // ==================== PLANT PLACEMENTS ====================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlacement(placement: PlantPlacementEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlacements(placements: List<PlantPlacementEntity>)
    
    @Update
    suspend fun updatePlacement(placement: PlantPlacementEntity)
    
    @Delete
    suspend fun deletePlacement(placement: PlantPlacementEntity)
    
    @Query("DELETE FROM plant_placements WHERE id = :placementId")
    suspend fun deletePlacementById(placementId: String)
    
    @Query("SELECT * FROM plant_placements WHERE bedId = :bedId ORDER BY positionY, positionX")
    fun getPlacementsByBed(bedId: String): Flow<List<PlantPlacementEntity>>
    
    @Query("SELECT * FROM plant_placements WHERE bedId = :bedId ORDER BY positionY, positionX")
    suspend fun getPlacementsByBedOnce(bedId: String): List<PlantPlacementEntity>
    
    @Query("SELECT * FROM plant_placements WHERE bedId = :bedId AND year = :year ORDER BY positionY, positionX")
    fun getPlacementsByBedAndYear(bedId: String, year: Int): Flow<List<PlantPlacementEntity>>
    
    @Query("SELECT * FROM plant_placements WHERE id = :id")
    suspend fun getPlacementById(id: String): PlantPlacementEntity?
    
    @Query("SELECT * FROM plant_placements WHERE plantId = :plantId")
    fun getPlacementsByPlant(plantId: String): Flow<List<PlantPlacementEntity>>
    
    // Get all placements for a garden (via beds)
    @Query("""
        SELECT pp.* FROM plant_placements pp
        INNER JOIN beds b ON pp.bedId = b.id
        WHERE b.gardenId = :gardenId
        ORDER BY b.name, pp.positionY, pp.positionX
    """)
    fun getPlacementsByGarden(gardenId: String): Flow<List<PlantPlacementEntity>>
    
    // Get placements for crop rotation check (same bed, previous years)
    @Query("""
        SELECT * FROM plant_placements 
        WHERE bedId = :bedId AND year < :currentYear
        ORDER BY year DESC
    """)
    suspend fun getPreviousYearPlacements(bedId: String, currentYear: Int): List<PlantPlacementEntity>
    
    // ==================== COMBINED QUERIES ====================
    
    @Transaction
    @Query("SELECT * FROM gardens WHERE id = :gardenId")
    fun getGardenWithBeds(gardenId: String): Flow<GardenWithBeds?>
}

/**
 * Data class for Garden with its Beds
 */
data class GardenWithBeds(
    @Embedded val garden: GardenEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "gardenId"
    )
    val beds: List<BedEntity>
)
