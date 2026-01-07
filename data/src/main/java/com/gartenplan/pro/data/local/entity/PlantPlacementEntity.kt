package com.gartenplan.pro.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing a plant placement within a bed
 * This tracks where specific plants are planted
 */
@Entity(
    tableName = "plant_placements",
    foreignKeys = [
        ForeignKey(
            entity = BedEntity::class,
            parentColumns = ["id"],
            childColumns = ["bedId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PlantEntity::class,
            parentColumns = ["id"],
            childColumns = ["plantId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["bedId"]),
        Index(value = ["plantId"])
    ]
)
data class PlantPlacementEntity(
    @PrimaryKey
    val id: String,
    
    val bedId: String,
    val plantId: String,
    
    // Position within the bed (in cm from bed's top-left)
    val positionX: Int,
    val positionY: Int,
    
    // Area covered (in cm)
    val widthCm: Int,
    val heightCm: Int,
    
    // Quantity of plants in this area
    val quantity: Int = 1,
    
    // Planting year (for crop rotation tracking)
    val year: Int,
    
    // Culture type (for succession planting)
    val cultureType: CultureType = CultureType.MAIN,
    
    // Actual dates (optional, for tracking)
    val sowDate: Long? = null,
    val transplantDate: Long? = null,
    val harvestDate: Long? = null,
    
    // Status
    val status: PlantingStatus = PlantingStatus.PLANNED,
    
    // Notes
    val notes: String? = null,
    
    // Metadata
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Culture type for succession planting
 */
enum class CultureType {
    PRE,    // Vorkultur (before main crop)
    MAIN,   // Hauptkultur (main crop)
    POST    // Nachkultur (after main crop)
}

/**
 * Status of a plant placement
 */
enum class PlantingStatus {
    PLANNED,        // Geplant
    SOWN,           // Ausgesät
    TRANSPLANTED,   // Ausgepflanzt
    GROWING,        // Wächst
    HARVESTING,     // In Ernte
    HARVESTED,      // Abgeerntet
    FAILED          // Fehlgeschlagen
}
