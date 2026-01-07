package com.gartenplan.pro.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gartenplan.pro.core.constants.CompostMaterialType

/**
 * Entity representing a compost pile
 */
@Entity(tableName = "composts")
data class CompostEntity(
    @PrimaryKey
    val id: String,
    
    val name: String,
    
    // Start date
    val startedAt: Long = System.currentTimeMillis(),
    
    // Expected ready date (calculated)
    val expectedReadyAt: Long? = null,
    
    // Status
    val status: CompostStatus = CompostStatus.ACTIVE,
    
    // Notes
    val notes: String? = null,
    
    // Metadata
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Compost status
 */
enum class CompostStatus {
    ACTIVE,     // In Bearbeitung
    RESTING,    // Ruht (nicht mehr hinzufügen)
    READY,      // Fertig
    USED        // Verwendet
}

/**
 * Entity representing material added to a compost pile
 */
@Entity(
    tableName = "compost_entries",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = CompostEntity::class,
            parentColumns = ["id"],
            childColumns = ["compostId"],
            onDelete = androidx.room.ForeignKey.CASCADE
        )
    ],
    indices = [androidx.room.Index(value = ["compostId"])]
)
data class CompostEntryEntity(
    @PrimaryKey
    val id: String,
    
    val compostId: String,
    
    val materialType: CompostMaterialType,
    
    // Custom material name (if not from predefined list)
    val customMaterialName: String? = null,
    
    // Is this a green (nitrogen) or brown (carbon) material?
    val isGreen: Boolean,
    
    // Amount (estimated liters or kg)
    val amountLiters: Float? = null,
    
    // Date added
    val addedAt: Long = System.currentTimeMillis(),
    
    // Notes
    val notes: String? = null
)
