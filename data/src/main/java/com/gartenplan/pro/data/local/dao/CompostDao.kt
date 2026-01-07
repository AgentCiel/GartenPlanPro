package com.gartenplan.pro.data.local.dao

import androidx.room.*
import com.gartenplan.pro.data.local.entity.CompostEntity
import com.gartenplan.pro.data.local.entity.CompostEntryEntity
import com.gartenplan.pro.data.local.entity.CompostStatus
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Compost operations
 */
@Dao
interface CompostDao {
    
    // ==================== COMPOSTS ====================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompost(compost: CompostEntity)
    
    @Update
    suspend fun updateCompost(compost: CompostEntity)
    
    @Delete
    suspend fun deleteCompost(compost: CompostEntity)
    
    @Query("DELETE FROM composts WHERE id = :compostId")
    suspend fun deleteCompostById(compostId: String)
    
    @Query("SELECT * FROM composts ORDER BY startedAt DESC")
    fun getAllComposts(): Flow<List<CompostEntity>>
    
    @Query("SELECT * FROM composts WHERE status = :status ORDER BY startedAt DESC")
    fun getCompostsByStatus(status: CompostStatus): Flow<List<CompostEntity>>
    
    @Query("SELECT * FROM composts WHERE status = 'ACTIVE' ORDER BY startedAt DESC")
    fun getActiveComposts(): Flow<List<CompostEntity>>
    
    @Query("SELECT * FROM composts WHERE id = :id")
    suspend fun getCompostById(id: String): CompostEntity?
    
    @Query("SELECT * FROM composts WHERE id = :id")
    fun observeCompostById(id: String): Flow<CompostEntity?>
    
    @Query("UPDATE composts SET status = :status, updatedAt = :updatedAt WHERE id = :compostId")
    suspend fun updateCompostStatus(compostId: String, status: CompostStatus, updatedAt: Long = System.currentTimeMillis())
    
    // ==================== COMPOST ENTRIES ====================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: CompostEntryEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<CompostEntryEntity>)
    
    @Update
    suspend fun updateEntry(entry: CompostEntryEntity)
    
    @Delete
    suspend fun deleteEntry(entry: CompostEntryEntity)
    
    @Query("DELETE FROM compost_entries WHERE id = :entryId")
    suspend fun deleteEntryById(entryId: String)
    
    @Query("SELECT * FROM compost_entries WHERE compostId = :compostId ORDER BY addedAt DESC")
    fun getEntriesByCompost(compostId: String): Flow<List<CompostEntryEntity>>
    
    @Query("SELECT * FROM compost_entries WHERE compostId = :compostId ORDER BY addedAt DESC")
    suspend fun getEntriesByCompostOnce(compostId: String): List<CompostEntryEntity>
    
    @Query("SELECT * FROM compost_entries WHERE id = :id")
    suspend fun getEntryById(id: String): CompostEntryEntity?
    
    // Calculate green/brown ratio
    @Query("""
        SELECT COUNT(*) FROM compost_entries 
        WHERE compostId = :compostId AND isGreen = 1
    """)
    suspend fun countGreenEntries(compostId: String): Int
    
    @Query("""
        SELECT COUNT(*) FROM compost_entries 
        WHERE compostId = :compostId AND isGreen = 0
    """)
    suspend fun countBrownEntries(compostId: String): Int
    
    // Get total liters by type
    @Query("""
        SELECT COALESCE(SUM(amountLiters), 0) FROM compost_entries 
        WHERE compostId = :compostId AND isGreen = 1
    """)
    suspend fun getTotalGreenLiters(compostId: String): Float
    
    @Query("""
        SELECT COALESCE(SUM(amountLiters), 0) FROM compost_entries 
        WHERE compostId = :compostId AND isGreen = 0
    """)
    suspend fun getTotalBrownLiters(compostId: String): Float
    
    // ==================== COMBINED QUERIES ====================
    
    @Transaction
    @Query("SELECT * FROM composts WHERE id = :compostId")
    fun getCompostWithEntries(compostId: String): Flow<CompostWithEntries?>
    
    @Transaction
    @Query("SELECT * FROM composts ORDER BY startedAt DESC")
    fun getAllCompostsWithEntries(): Flow<List<CompostWithEntries>>
}

/**
 * Data class for Compost with its entries
 */
data class CompostWithEntries(
    @Embedded val compost: CompostEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "compostId"
    )
    val entries: List<CompostEntryEntity>
)
